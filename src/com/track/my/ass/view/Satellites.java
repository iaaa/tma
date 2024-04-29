package com.track.my.ass.view;

import com.iaaa.Subscriber;
import com.iaaa.gps.nmea.GSA;
import com.iaaa.gps.nmea.GSV;
import com.iaaa.gps.nmea.RMC;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class Satellites extends View
{
	public float direction = 0;
	public Subscriber OnGPSChange = new Subscriber() {
		@Override
		public void Knock(Boolean urgent) {
			post(new Runnable() {
				public void run() {
					invalidate();
				}
			});
		}
	};
	
	public Satellites(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		int minDimension = Math.min(widthMeasureSpec, heightMeasureSpec);
//		super.onMeasure(minDimension, minDimension);
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }	
	
	// http://www.w3schools.com/html/html_colornames.asp
	Color Red		= new Color(0xffFF0000);
	Color Green		= new Color(0xff00FF00);
	Color Blue      = new Color(0xff0000FF);
//	Color Orange	= new Color(0xffFFA500);
//	Color Yellow	= new Color(0xffFFFF00);
//	Color Greeny	= new Color(0xff009900);

	Paint pen = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	@Override
	protected void onDraw(Canvas canvas)
	{
		int cx = this.getWidth() / 2;
		int cy = this.getHeight() / 2;
		int R = Math.min(cx, cy);
		cx = cy = R;
		
		float d2 = 2* 2;//cx * 2 / 75f;
		float d3 = 2* 3;//cx * 3 / 75f;
		
		canvas.save();
		canvas.rotate(-direction, cx, cy);

		char type = GSA.Type;
		double pdop = GSA.PDOP;
		
		// сетка
		pen.setStyle(Style.STROKE);
		pen.setStrokeWidth(d2);

		pen.setColor(0x77A5A5A5);
		canvas.drawCircle(cx, cy, R - 2, pen);
		canvas.drawLine(cx - R / 6, cy, cx - R + 1, cy, pen);
		canvas.drawLine(cx + R / 6, cy, cx + R - 1, cy, pen);
		canvas.drawCircle(cx, cy, R / 2, pen);
		
		// компас
		pen.setStrokeWidth(d3);
		pen.setColor(0xff0000FF);	// blue
		canvas.drawLine(cx, cy - R / 6, cx, cy - R + 1, pen);
		pen.setColor(0xffFF0000);	// red-
		canvas.drawLine(cx, cy + R / 6, cx, cy + R - 1, pen);
		
		// спутники
		pen.setStyle(Style.FILL);
		
		for (int i = 1; i < GSV.Satellites.length; i++) {
			int PRN = GSV.Satellites[i].PRN;
			if (GSV.Satellites[i].Timestamp >= GSV.Timestamp - 5) // 5 seconds history
			{	// if satellite visible
				int noise = GSV.Satellites[i].Noise;
				
				int r = noise < 11 ? (int) (    .0) :
						noise < 33 ? (int) ( 255.0) :
						             (int) ( 255.0        * (99 - noise) / (99 - 33)) ;
				int g = noise < 11 ? (int) (    .0) :
						noise < 33 ? (int) ( 255.0        * (noise - 11) / (33 - 11)) :
						noise < 66 ? (int) ((255.0 - 211) * (66 - noise) / (66 - 33)) + 211 :
						             (int) ((255.0 - 211) * (noise - 66) / (99 - 66)) + 211 ;
						
				pen.setColor ((0xdd << 24) |
						(r << 16) | (g << 8));
				pen.setStrokeWidth(1);
				for (int prn : GSA.PRN) // used in calculations
					if (prn == PRN)
						pen.setStrokeWidth(d3);
				
				double altitude = GSV.Satellites[i].Altitude;
				double azimuth = GSV.Satellites[i].Azimuth;
				
				if (altitude != 0 && azimuth != 0) {
					double x = (R - d2) * Math.cos(altitude) * Math.cos(azimuth);
					double y = (R - d2) * Math.cos(altitude) * Math.sin(azimuth);
				
					canvas.drawCircle((float)(cx + y), (float)(cy - x), d2, pen);
				}
			}
		}

		// todo: if "track position", draw central dot

		canvas.restore();
	}
	
	private static final double min(double a, double b) {
		return a < b ? a : b;
	}
}

class Color extends Paint
{
	public Color(int color) {
		super();
		this.setColor(color);
	}
}
