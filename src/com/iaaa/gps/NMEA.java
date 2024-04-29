package com.iaaa.gps;

import com.iaaa.gps.nmea.GGA;
import com.iaaa.gps.nmea.GSA;
import com.iaaa.gps.nmea.GSV;
import com.iaaa.gps.nmea.RMC;

import android.content.Intent;
import android.util.Log;

public class NMEA
{
	public enum Type
	{
		Invalid,	// Ошибочное сообщение
		GGA,		// Global Positioning System Fix Data
		GSV,		// GPS Satellites in view
		GSA,		// GPS DOP and active satellites
		RMC,		// Recommended minimum specific GPS/Transit data
		Unknown,	// Неизвестное сообщение
	}
	public static void Save(Intent storage) { }
	public static void Restore(Intent storage) { }

	// Messages	timestamp
	public static long Timestamp = 3600; // 1 hour stamp

	// low-level parsing
	private static char[] packet = null; // NMEA message
	private static int packetLength = 0;
	private static int packetPtr = 0;
	
	public static final Type Post(String message)
	{
		packet = message.toCharArray();
		packetLength = packet.length;
		packetPtr = 0;

		if (packetLength > 6) {	// Минимальный размер сообщения
		if (packet[0] == '$' && packet[6] == ',')
		{
			packetPtr = 7;
			switch ((packet[3] << 16) +
					(packet[4] << +8) +
					(packet[5] << +0))
			{
				// Time, position and fix type data
				case ('G' << 16) + ('G' << 8) + ('A'):	// GGA
					GGA.Parse();
					Timestamp++; // отметка, что пакет обработан
					return Type.GGA;
				// Time, date, position, course and speed data
				case ('R' << 16) + ('M' << 8) + ('C'):	// RMC
					RMC.Parse();
					return Type.RMC;
					
				// Общая информация о спутниках
				case ('G' << 16) + ('S' << 8) + ('A'):	// GSA
					GSA.Parse();
					return Type.GSA;
				// Детальная информация о спутниках
				case ('G' << 16) + ('S' << 8) + ('V'):	// GSV
					GSV.Parse();
					return Type.GSV;	
		// 		// PGLOR
		// 		// GNGSA
		// 		// QZGSA
				}
		 	}
		 	return Type.Unknown;
		 }
		return Type.Invalid;
	}
	
	protected static final char get()
	{
		return packet[packetPtr++];
	}
	protected static final char got()
	{
		return packet[packetPtr-1];
	}

	protected static final int readInteger()
	{
		// Log.e("GPS", "packetPtr = " + String.valueOf(packetPtr));
		// if (packetPtr >= packet.length) {
		// 	Log.e("GPS", "readInteger fail for '" + String.valueOf(packet) + "', packetPtr = " + String.valueOf(packetPtr));
		// }
		char var0;
		if ((var0 = get()) == ',' || var0 == '*')
			return 0;

		int var1 = 0;
		do {
			var1 = var1 * 10 + (var0 - '0');

			// if (packetPtr >= packet.length) {
			// 	Log.e("GPS", "readInteger fail for '" + String.valueOf(packet) + "', packetPtr = " + String.valueOf(packetPtr));
			// }
		}
		while ((var0 = get()) != ',' && var0 != '*');
		return var1;
	}
	
	protected static final float readFloat()
	{
		char var0;
		if ((var0 = get()) == ',' || var0 == '*')
			return +0.0f;

		float var1 = +0.0f;
		float var2 = 10.0f;
		do {
			var1 = var1 * 10 + (var0 - '0');
		}
		while ((var0 = get()) != '.' && var0 != '*' && var0 != ',');
		while (var0 != ',' && (var0 = get()) != ',' && var0 != '*') {
			var1 = var1 + (var0 - '0') / var2;
			var2 = var2 * 10.0f;
		}
		return var1;
	}
	protected static final char readChar()
	{
		char var0;
		if ((var0 = get()) == ',' || var0 == '*')
			return ('\0');
		char var1;				// ?
		do {
			var1 = (char)var0;
		}
		while ((var0 = get()) != ',' && var0 != '*');
		return var1;
	}
/*	static final double readTime()
	{
		char var0;
		if ((var0 = get()) == ',' || var0 == '*')
			return +0.0;

		double var1 = var0 - '0';
		var1 = var1 * 10.0 + (get() - '0'); var0 = get();	// hours
		double var2 = var0 - '0';
		var2 = var2 * 10.0 + (get() - '0');					// minutes

		return var1 * 24.0 * 60.0 + var2 * 60.0 + readDouble();
	}*/
	protected static final float readLatitude()
	{
		char var0;
		if ((var0 = get()) == ',' || var0 == '*')
			return +0.0f;
		float var1 = +0.0f;
		var1 = var1 * 10.f + (var0 - '0'); var0 = get();
		var1 = var1 * 10.f + (var0 - '0');
		return var1 + readFloat() / 60.0f;
	}
	protected static final float readLongitude()
	{
		char var0;
		if ((var0 = get()) == ',' || var0 == '*')
			return +0.0f;
		float var1 = +0.0f;
		var1 = var1 * 10.f + (var0 - '0'); var0 = get();
		var1 = var1 * 10.f + (var0 - '0'); var0 = get();
		var1 = var1 * 10.f + (var0 - '0');
		return var1 + readFloat() / 60.0f;
	}
}
