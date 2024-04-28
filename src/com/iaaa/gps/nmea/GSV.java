package com.iaaa.gps.nmea;

import android.util.Log;

import com.iaaa.gps.NMEA;

/** Детальная информация о спутниках
 
	В этом сообщении отображается число видимых спутников(SV), PRN номера
	этих спутников, их высота над местным горизонтом, азимут и отношение
	сигнал/шум. В каждом сообщении может быть информация не более чем о
	четырех спутниках, остальные данные могут быть расположены в следующих
	по порядку $GPGSV сообщениях. Полное число отправляемых сообщений и
	номер текущего сообщения указаны в первых двух полях каждого сообщения.
 */
public class GSV extends NMEA {
	
	// Спутник
	public class Satellite {
		public int PRN;			// PRN номер спутника.
		public float Altitude;	// Высота, градусы, (90° - максимум).
		public float Azimuth;	// Азимут истинный, градусы, от 0° до 359°.
		public int Noise;		// Отношение сигнал/шум от 00 до 99 дБ, ноль - когда нет сигнала.

		public long Timestamp;
		public Satellite() {}
	}
	
//	$GPGSV, x, x, xx, xx, xx, xxx, xx..........., xx, xx, xxx, xx *hh
	
//	$GPGSV,3,1,12,02,86,172,,09,62,237,,22,39,109,,27, 37,301,*7A
//	$GPGSV,3,2,12,17,28,050,,29,21,314,,26,18,246,,08, 10,153,*7F
//	$GPGSV,3,3,12,07,08,231,,10,08,043,,04,06,170,,30, 00,281,*77

//	$GPGSV,3,3,12, 07,08,231,, 10,08,043,, 04,06,170,, 30,00,281,*77
//	$GPGSV,4,4,15, 27,75,253,, 30,00,000,, 32,17,153,, 1*50
	
	static int Total;	// Полное число сообщений, от 1 до 9.
	static int Number;	// Номер сообщения, от 1 до 9.
	public static int Seen;	// Полное число видимых спутников.

	// С первого!!! Всего 64! Нулевой не трогать!!!
	public static Satellite[] Satellites = new Satellite[1 + 96];
	// 1..32: GPS
	// 1..36: Galileo
	// 1..63: BeiDou
	// 1..14: NavIC
	// 1..10: QZSS
	// 33..64: SBAS
	// 65..96: GLONASS
	static {
		GSV parent = new GSV();
		for (int i = 0; i < Satellites.length; i++)
			Satellites[i] = parent.new Satellite();
	}
	
	public static void Parse()
	{
		Total = readInteger();
		Number = readInteger();
		Seen = readInteger();
		
		for (int sv = 0; sv < 4 && got() != '*'; sv++)	// Не более 4 спутников в сообщении
		{
			int number = readInteger();
			if (number <= 0)	// больше нету спутников в сообщении
				break;
			if (number < Satellites.length) {
				if (got() == '*') {
					// message is ended, number is a GNSS System
					// 1(GP) - GPS
					// 2(GL) - GLONASS
					// 3(GA) - Galileo
					// 4(GB/BD) - BDS (BeiDou System)
					// 5(GQ) - QZSS
					// 6(GI) - NavIC
					return; 
				}
				Satellites[number].Timestamp = Timestamp;	// отметка времени (когда последний раз видели спутник)
				Satellites[number].PRN = number;
				Satellites[number].Altitude = (float)Math.toRadians(readInteger());
				Satellites[number].Azimuth = (float)Math.toRadians(readInteger());
				Satellites[number].Noise = readInteger();
			}
		}
	}
}
