package com.track.my.ass;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.iaaa.Gps;
import com.track.my.ass.MainActivity.Button;
import com.track.my.ass.view.Satellites;
import com.track.my.ass.view.TiledMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends Activity
	implements SensorEventListener
{
	final static String TAG = "Gps";

	// public MainActivity() {
	// 	final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler =
	// 			Thread.getDefaultUncaughtExceptionHandler();
	// 	Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
	// 		//@Override
	// 		public void uncaughtException(Thread thread, Throwable exception) {
	// 			// TODO: ErrorReport.Send(MainActivity.this, "Uncaught exception", exception);
    //             if (defaultUncaughtExceptionHandler != null)
    //             	defaultUncaughtExceptionHandler.uncaughtException(thread, exception);
	// 		}
	// 	});
	// }
	// void Fatal(final Object message, final Object exception)
	// {
	// 	final Activity activity = this;
	// 	runOnUiThread(new Runnable(){
	// 		public void run(){
	// 			try {
	// 		        setContentView(R.layout.error);
	// 				// http://stackoverflow.com/questions/9494037/how-to-set-text-size-of-textview-dynamically-for-diffrent-screens
	// 				TextView textView = (TextView)findViewById(R.id.textError);
	// 				// Так как нету рендеререра, то сменим фон с прозрачного на "ошибочный"
	// 				textView.setBackgroundColor(Color.rgb(153, 13, 15));
	// 				textView.setText("Извините, что-то случилось:\n" +
	// 						"\"" + message + "\"\n\n" +
	// 						"Отчет об ошибке будет отправлен. Ждите исправления в следующем апдейте."
	// 				);
	// 			}
	// 			catch (Exception ex) {
	// 				// TODO: ErrorReport.Send(activity, "Can't create BSOD", ex); // and ignore exception
	// 			}
	// 		}
	// 	});		
		
	// 	// TODO: ErrorReport.Send(activity, message, exception);
	// }

	// sensors
	SensorManager sensorManager;
	private Sensor sensorMagneticField;
	private Sensor sensorAccelerometer;

	class Button
	{
		@SuppressWarnings("deprecation")
		public Button(int id, OnClickListener handler)
		{
			View view = findViewById(id);
			view.setOnClickListener(handler);
			
			((ImageView)view).setAlpha(80);
		}
	}
	void checkScaleButtons()
	{
		int scale = tiledMapView.getScale();
		
		findViewById(R.id.imageViewMinus).setVisibility(scale < 2
				? View.INVISIBLE : View.VISIBLE);
		
		findViewById(R.id.imageViewPlus).setVisibility(scale >= TiledMap.SCALE_LIMIT
				? View.INVISIBLE : View.VISIBLE);
	}


	protected TiledMap tiledMapView;
	protected Satellites satellitesView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		// start services if not started
		this.startService(new Intent(this, Gps.class));
		// initialize map
		Database.New(this);

		// set fullscreen
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// init layout
        setContentView(R.layout.main);

		// setup local variables
		tiledMapView = (TiledMap)findViewById(R.id.mapView);

		satellitesView = (Satellites)findViewById(R.id.satellites);
		satellitesView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				tiledMapView.TrackPosition(true);
			}});

		// "+" and "-" buttons setup
		new Button(R.id.imageViewPlus, new OnClickListener() { // plus
			public void onClick(View v) {
				tiledMapView.rescale(+1);
				checkScaleButtons();
			}});
		new Button(R.id.imageViewMinus, new OnClickListener() { // minus
			public void onClick(View v) {
				tiledMapView.rescale(-1);
				checkScaleButtons();
			}});
		checkScaleButtons();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}	
	
	@Override
    protected void onDestroy()
	{
		super.onDestroy();
	}
	

	private float[] valuesAccelerometer = new float[3];
	private float[] valuesMagneticField = new float[3];
	
	private final float[] matrixR = new float[9];
	private final float[] matrixI = new float[9];
	private final float[] matrixValues = new float[3];
	
	private float direction;


	@Override
	protected void onResume()
	{
		super.onResume();
//		if (!Tileset.IsValid()) return;

// //		Gps.Subscribe(this);
		Gps.Subscribe(tiledMapView.OnGPSChange);
		Gps.Subscribe(satellitesView.OnGPSChange);

		sensorManager.registerListener(this,
				sensorAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorMagneticField,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	@Override
    protected void onPause()
	{
		super.onPause();
		
		sensorManager.unregisterListener(this,
				sensorMagneticField);
		sensorManager.unregisterListener(this,
				sensorAccelerometer);
//		Cache.Unsubscribe();
		Gps.UnsubscribeAll();
		
		// сохраним состояние
		save();
	}

	//	<!-- saving
	private void save()
	{
		Editor preferences = Preferences.edit();
		if (tiledMapView != null)
			tiledMapView.save(preferences);
		preferences.commit();
	}
	// -->
	
	
	final static DecimalFormat df = new DecimalFormat("00");
	final static DecimalFormat f1 = new DecimalFormat("0.0");
	final static DecimalFormat f2 = new DecimalFormat("#.##");
	final static DecimalFormat f3 = new DecimalFormat("#.###");
	final static DecimalFormat f7 = new DecimalFormat("#.#######");

	char savedQuality = '\0';
	void updateWindow()
	{
		// if (mapView != null)
		// 	mapView.invalidate();
		if (satellitesView != null)
			satellitesView.invalidate();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		menu.setGroupDividerEnabled(true);
		return true;
	}
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		
//		MenuItem dump = menu.findItem(R.id.dump);
//		if (Gps.isDumping()) {
//			dump.setIcon(R.drawable.player_stop);
//			dump.setTitle(R.string.stop);
//		}
//		else {
//			dump.setIcon(R.drawable.player_play);
//			dump.setTitle(R.string.record);
//		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		// Операции для выбранного пункта меню
		switch (item.getItemId()) 
		{
			case R.id.exit:
				// stop services
				stopService(new Intent(this, Gps.class));
				// exit application
				finish();
				return true;

			// Preferences dialog
			case R.id.preferences:
				startActivity(new Intent(this, Preferences.Activity.class));
				return true;

			// change the map
			case R.id.map_changer:
				showDialog(1);
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	@SuppressWarnings("serial")
	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id) {
			// http://developer.android.com/guide/topics/ui/dialogs.html
			case 1: {
				try {
					InputStream input = null;
					try {
						// TODO: load "maps.yaml" from server
						// URL url = new URL(urlString);
						throw new Exception("no internet map");
					}
					catch (Exception ex) {
						Log.e(TAG, ex.toString());
						input = this.getAssets().open("maps.yaml");
					}
					Yaml yaml = new Yaml();
					Iterable<Object> maps = yaml.loadAll(input);

					final List<String> names = new ArrayList<String>();
					final List<String> databases = new ArrayList<String>();
					final List<String> scripts = new ArrayList<String>();
					final List<String> projections = new ArrayList<String>();
					for (Object object : maps) {
						Map<String, Object> map = (Map<String, Object>) object;
						names.add((String)map.get("name"));

						databases.add((String)map.get("database"));
						scripts.add((String)map.get("script"));
						projections.add((String)map.get("projection"));
					}
	
					final Context context = this;
					AlertDialog dialog = new AlertDialog.Builder(this)
						.setTitle("Select Map")
						.setSingleChoiceItems(names.toArray(new String[0]), names.indexOf(Database.Name), new DialogInterface.OnClickListener() {
		                    @Override
		                    public void onClick(DialogInterface dialog, int id) {
								Log.i(TAG, "Selected map " + String.valueOf(id) + ": " + names.get(id));
								Database.New(context, names.get(id),
										databases.get(id), scripts.get(id), projections.get(id));
								Cache.Clear();
								if (tiledMapView != null)
									tiledMapView.Update();
								dialog.dismiss();
		                    }
		                })
		                .create();
			        return dialog;
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			}
		}
		return null;
	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// do nothing
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			for (int i = 0; i < 3; i++)
				valuesAccelerometer[i] = event.values[i];
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			for (int i = 0; i < 3; i++)
				valuesMagneticField[i] = event.values[i];
			break;
		}
		   
		if (SensorManager.getRotationMatrix(
				matrixR,
				matrixI,
				valuesAccelerometer,
				valuesMagneticField))
		{
			SensorManager.getOrientation(matrixR, matrixValues);
		    
			double azimuth = Math.toDegrees(matrixValues[0]);
			double pitch = Math.toDegrees(matrixValues[1]);
			double roll = Math.toDegrees(matrixValues[2]);

			if (satellitesView != null)
				satellitesView.direction = matrixValues[0] * 180.0f / 3.14159f;
		}
	}
	
	private String loadAssetTextAsString(Context context, String name) {
		BufferedReader in = null;
		try {
			StringBuilder buf = new StringBuilder();
			in = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));

			String line;
			while ((line = in.readLine()) != null) {
				buf.append(line);
				buf.append('\n');
			}
			return buf.toString();
		} catch (IOException e) {
			Log.e("gps", "Error opening asset " + name);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					Log.e("gps", "Error closing asset " + name);
				}
			}
		}

		return null;
	}
}

