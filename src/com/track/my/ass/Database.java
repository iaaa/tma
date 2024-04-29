package com.track.my.ass;

import java.io.*;
import java.util.Random;

import com.track.my.ass.Preferences;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class Database
{
    private static long generation = 0;
    public static long Generation() { return generation; }
    
	public static String Name = "-1";
	static SQLiteDatabase database = null;
	static String databaseFileName = null;
	static String downloaderScript = null;
	static String projectionString = null;

	// todo: integrate Otus Lisp
	static BitmapFactory.Options options = new BitmapFactory.Options();
	static {
		options.inPreferredConfig = Bitmap.Config.RGB_565;
	}

	public static void Close()
	{
		if (database != null)
			database.close();
		database = null;
	}
	
	public static int projectionStyle1 = 1;
	public static void New(Context context, String name,
			String filename, String script, String projection)
	{
		generation++; // это единственное место где меняется дженерейшен.
		// с этого момента все прошлые запросы к БД объявляются недействительными

		if (database != null)
			database.close();

		File storageDirectory = new File(Environment.getExternalStorageDirectory(), "/Maps");
		try {
			Log.i("Gps", "Opening '" + filename + "' database");
			if (filename == null)
				throw new Exception("No database selected (first run?).");

			storageDirectory.mkdirs();
			database = SQLiteDatabase.openDatabase(
					filename.startsWith("/") ? filename : (storageDirectory.getAbsolutePath() + "/" + filename), null,
					SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			if (database.getVersion() != 101) {
				database.execSQL("CREATE TABLE IF NOT EXISTS tiles (z_xy INTEGER PRIMARY KEY, tile BLOB)");
				database.execSQL("PRAGMA USER_VERSION = 101");
			}
			database.rawQuery("PRAGMA journal_mode = MEMORY", null).close();
			databaseFileName = filename;
			downloaderScript = script;
			projectionString = projection;

			Name = name;

			projectionStyle1 = 1;
			if ("mercator-elliptical".equals(projection))
				projectionStyle1 = 2;
		}
		catch (Exception ex) {
			Log.e("Gps", ex.getMessage());

			Name = "-- demo --";

			final File file = context.getDatabasePath("demo.db");
			if (file.exists()) {
				Log.i("Gps", "Opening demo database");
				SQLiteDatabase database = null;
				try {
					database = SQLiteDatabase.openDatabase(
							context.getDatabasePath("demo.db").getAbsolutePath(), null,
							SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS); 
					Cursor cursor = database.rawQuery("SELECT COUNT(*) FROM tiles", new String[] { });
					if (cursor.moveToFirst())
						if (cursor.getInt(0) != 85)	// number of tiles in demo world.db
							throw new SQLiteException("Invalid demo database");
					cursor.close();
					Log.i("Gps", "Demo database opened");
				}
				catch (SQLiteException ex2) {
					Log.i("Gps", ex2.getMessage());
					file.delete();
				}
				finally {
					if (database != null)
						database.close();
				}
			}

			// если же карты нет (или она была удалена как невалидная) скопируем новую из архива
			if (!file.exists()) {
				Log.i("Gps", "Unpacking new demo database");
				try {
					file.getParentFile().mkdirs();
				
					// How to ship an Android application with a database?
					//	http://stackoverflow.com/questions/513084/how-to-ship-an-android-application-with-a-database
					//	http://www.reigndesign.com/blog/using-your-own-sqlite-database-in-android-applications/
					final InputStream i = context.getAssets().open("demo.db");
					final OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
				
					byte[] buffer = new byte[65536];
					try {
						int length;
						while ((length = i.read(buffer)) > 0)
							os.write(buffer, 0, length);
					}
					finally {
						try {
							os.flush();
						} finally {
							os.close();
						}
						i.close();
					}
				}
				catch (Exception ex2) {
					Log.e("Gps", ex2.getMessage());
				}
			}

			// demo database settings
			filename = context.getDatabasePath("demo.db").getAbsolutePath();
			database = SQLiteDatabase.openDatabase(
					filename, null,
					SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
			databaseFileName = null;
			downloaderScript = null;
			Log.i("Gps", "Demo database opened");
		}

		// // test downloader scripts
		// try {
		// 	if (downloaderScript != null && !downloaderScript.equals("-")) {
		// 		// test url script
		// 		/*lisp.eval(String.format("(defun @ (x y z) %s)", downloaderScript));
				
		// 		org.jatha.dynatype.LispValue value = lisp.eval(String.format("(@ %s %s %s)", 0, 0, 0));
		// 		Log.i("lisp", "test for (0, 0, 0): " + value.toString());
		// 		if (!(value instanceof org.jatha.dynatype.LispString) || !((org.jatha.dynatype.LispString)value).getValue().startsWith("http"))
		// 			downloaderScript = null;*/
		// 	}
		// 	else {
		// 		//lisp.eval(String.format("(defun @ (x y z) \"\")", downloaderScript));
		// 		downloaderScript = null;
		// 	}
		// } catch (Exception ex) {
		// 	Log.e("Lisp", ex.toString());
		// }

		Log.i("Gps", "Database switched to " + Name);

		// store settings
		Editor editor = Preferences.edit();
		editor.putString("database.name", Name);
		editor.putString("database.filename", databaseFileName);
		editor.putString("downloader.script", downloaderScript);
		editor.putString("projection.string", projectionString);
		editor.commit();
	}
	public static void New(Context context)
	{
		New(context, Preferences.getString("database.name"),
			Preferences.getString("database.filename", databaseFileName),
			Preferences.getString("downloader.script", downloaderScript),
			Preferences.getString("projection.string", projectionString));
	}

	// -------------------------------
	public static final long zxy(final int x, final int y, final int z)
	{
		return ((((long)z) << 48) |
				(((long)x) << 24) |
				(((long)y)      ));
	}

	// get image from database
	public static Bitmap Get(long key) {
		// К сожалению, API 10 не позволяет более эффективно юзать запросы к BLOB данным
		Cursor cursor = null;
		try {
			cursor = database.rawQuery("SELECT tile FROM tiles WHERE z_xy = ? LIMIT 1", new String[] { String.valueOf(key) });
			if (cursor.moveToFirst()) {
				byte[] bytes = cursor.getBlob(0);
				return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
//				Log.i("cache", "got(" + x + ", " + y + ", " + z + ")");
			}
		}
		catch (Exception ex) {}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
    
	// put the image into database
	public static boolean Put(long map, long key, byte[] blob)
	{
		if (map != generation)
			return false;
		Log.i("Gps", "put(" + String.valueOf(map) + ", " + String.valueOf(key) + ")");
		ContentValues values = new ContentValues();
		values.put("z_xy", key);
		values.put("tile", blob);
//		database.insert("tiles", null, values);
		database.insertWithOnConflict("tiles", null, values, SQLiteDatabase.CONFLICT_REPLACE);
		
		return true;
	}

	// generate url for downloading (todo: move to different place?)
	static Random rand = new Random();
	public static String Url(int x, int y, int z)
	{
		// todo: use Lisp
		//return ((org.jatha.dynatype.LispString)(lisp.eval(String.format("(@ %s %s %s)", x, y, z)))).getValue();
		int abc = rand.nextInt(3);
		return "http://" + (
			abc == 0 ? "a" :
			abc == 1 ? "b" :
			abc == 2 ? "c" : "q"
		)
		+ ".tile-cyclosm.openstreetmap.fr/cyclosm/" + String.valueOf(z) + "/" + String.valueOf(x) + "/" + String.valueOf(y) + ".png";
	}

}