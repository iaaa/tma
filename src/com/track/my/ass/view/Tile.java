package com.track.my.ass.view;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.track.my.ass.Cache;
import com.track.my.ass.Database;
import com.track.my.ass.Preferences;
import com.track.my.ass.R;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.util.Log;


// Map Tile
public class Tile
{
	public static Bitmap Loading = Preferences.loadBitmap(R.drawable.loading);
	public static Bitmap Nocache = Preferences.loadBitmap(R.drawable.nocache);
	public static Bitmap Invalid = Preferences.loadBitmap(R.drawable.invalid);

	public static String UserAgent = "curl/7.81.0";

	protected static long Timestamp = 0;
	public static void Reserve(int step)
	{
		// reserve timestamps for downloaded tiles
		Lastuseds = Timestamp = Timestamp + step;
	}

	final static String TAG = "Gps";

	// 
	public final long Position;	// для кеша
	public long Lastused;	// для кеша
	static long Lastuseds = 0;

	public int x, y, z;		// мировые координаты
	public long key;
	public String url;
	public long map;		// ключ карты

	Bitmap bitmap = Loading;
	boolean disposable = false; // штучная ли текстура, или общая (надо ли ее убивать)

	public Tile()
	{
		this.Position = -1;
		this.bitmap = Nocache; // молоко
	}
	public Tile(int x, int y, int z, long position)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.key = Database.zxy(x, y, z);
		this.map = Database.Generation();
		
		Position = position;
		Lastused = Lastuseds;
	}
	
	static Paint red = new Paint();
	static Paint black = new Paint();
	
	static {
		red.setColor(0xffFF0000);
		red.setAntiAlias(true);
		red.setStyle(Style.FILL);
		red.setStrokeWidth(2);
		red.setTextSize(32);
		
		black.setColor(0xff000000);
		black.setAntiAlias(true);
		black.setStyle(Style.FILL);
		black.setStrokeWidth(1);
		black.setTextSize(32);
	}
	
	// free the tile
	public void Free()
	{
		if (bitmap != Invalid &&
			bitmap != Loading &&
			bitmap != Nocache)
		bitmap.recycle();
	}
	
	public void Download()
	{
		Log.i(TAG, "Downloading "+url);
		
		HttpURLConnection input = null;
		try
		{
			input = (HttpURLConnection)new URL(url).openConnection();
			input.setRequestProperty("User-Agent", UserAgent);
			input.setDoInput(true);
			input.setDoOutput(false);

			input.connect();
            
			// тайл скачан и еще не протух (карту не успели сменить)
			if (input.getResponseCode() / 100 == 2 && map == Database.Generation()) {
            	InputStream s = input.getInputStream();
            	byte[] bytes = new byte[65536];
            	ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int length = 0;
                while ((length = s.read(bytes)) > 0)
                	buffer.write(bytes, 0, length);
                bytes = buffer.toByteArray();
                
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) {
                	bitmap = Invalid;
					Log.e(TAG, "invalid bitmap");
				}
                else {
               		Database.Put(map, key, bytes);
                }
            }
			else {
				bitmap = Nocache;
				Log.e(TAG, "input.getResponseCode() = " + String.valueOf(input.getResponseCode()));
			}
        }
        catch (Exception ex) {
        	bitmap = Invalid;
			Log.e(TAG, ex.getMessage());
        }
        finally {
            if (input != null)
                input.disconnect();
        }
		// drawRoutes();
	}

	// single threaded
	public void Load()
	{
		Bitmap load = Database.Get(key);
		if (load == null)
			Update();
		else
			bitmap = load;
		
		// drawRoutes();
	}
	
	public void Update()
	{
		// TODO: don't try to download if no internet
		//if (!Configuration.networkConnected)
		//	bitmap = Nocache;
		//else {
			this.url = Database.Url(x, y, z);
			if (this.url.length() > 0)
				Cache.Download(this);	// put tile into downloader queue
			else
				bitmap = Nocache;
		//}
	}
	
	public void Draw(Canvas canvas, int x, int y)
	{
		canvas.drawBitmap(bitmap, x, y, null);
		Lastused = Lastuseds--;

/*
		// public static Boolean central = false;
		Paint color = central ? red : black;
		canvas.drawLine(x+1, y+1, x + 255, y+1, color);
		canvas.drawLine(x + 255, y+1, x + 255, y + 255, color);
		canvas.drawLine(x + 255, y + 255, x+1, y + 255, color);
		canvas.drawLine(x+1, y+1, x+1, y + 255, color);
		
		canvas.drawText(" " + x / 256 + ", " + y / 256 + ", z = " + z,
			x, y + 32, color);
		canvas.drawText(" x = " + x + ", y = " + y,
			x, y + 64, color);
//*/
	}
}