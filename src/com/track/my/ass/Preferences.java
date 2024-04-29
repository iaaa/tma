package com.track.my.ass;

import java.util.Map;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.content.SharedPreferences.Editor;

// http://www.javacodegeeks.com/2011/01/android-quick-preferences-tutorial.html
public class Preferences extends Application
{
	public interface Saveable {
		public void save(Editor preferences);
	}

	public void onCreate(){
		super.onCreate();
		Preferences.context = getApplicationContext();
	}

	private static Context context;
	public static Context getContext() {
		return Preferences.context;
	}
	
	public static Bitmap loadBitmap(int id)
	{
		return BitmapFactory.decodeResource(context.getResources(), id);
	}

	public static boolean getBoolean(String key)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, false);
	}
	public static boolean getBoolean(String key, boolean defValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defValue);
	}

	public static float getFloat(String key, float defValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getFloat(key, defValue);
	}

	public static int getInt(String key, int defValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defValue);
	}

	public static long getLong(String key, long defValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getLong(key, defValue);
	}

	public static String getString(String key)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, null);
	}
	public static String getString(String key, String defValue)
	{
		return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defValue);
	}
	
	public static Point getPoint(String key, Point defValue)
	{
		return new Point(
				getInt(key + ".x", defValue.x),
				getInt(key + ".y", defValue.y)
		);
	}

	public static android.content.SharedPreferences.Editor edit()
	{
		return PreferenceManager.getDefaultSharedPreferences(context).edit();
	}

	public static class Activity extends PreferenceActivity
		implements SharedPreferences.OnSharedPreferenceChangeListener
	{
		
		@Override
		public void onCreate(Bundle savedInstanceState) {        
			super.onCreate(savedInstanceState);        
			addPreferencesFromResource(R.xml.preferences);
			getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

			Map<String,?> keys = PreferenceManager.getDefaultSharedPreferences(context).getAll();
			for (Map.Entry<String,?> entry : keys.entrySet()) {
				Log.d("Tma","Setting '" + entry.getKey() + "' = " + entry.getValue().toString());
				updatePreference(findPreference(entry.getKey()));
			}
		}
	
		@Override
		public void onBackPressed() {
			Intent intent = new Intent();
			intent.setAction("GPS_PREFERENCES_CHANGED");
	        sendBroadcast(intent);

			super.onBackPressed();
		}
		@Override
		public void onSharedPreferenceChanged (SharedPreferences sharedPreferences, String key) {
			updatePreference(findPreference(key));

		}
	
		private void updatePreference (Preference preference) {
			if (preference instanceof EditTextPreference) {
				EditTextPreference editTextPreference = (EditTextPreference)preference;
				editTextPreference.setSummary(editTextPreference.getText());
				return;
			}
			if (preference instanceof ListPreference) {
				ListPreference listPreference = (ListPreference)preference;
				listPreference.setSummary(listPreference.getEntry());
			}
		}
	}
}
