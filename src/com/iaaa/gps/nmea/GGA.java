package com.iaaa.gps.nmea;

import com.iaaa.gps.NMEA;

/**
 * Global Positioning System Fix Data
 * Информация о фиксированном решении
 *
 * Самое популярное и наиболее используемое NMEA сообщение с информацией о
 * текущем фиксированном решении – горизонтальные координаты, значение высоты,
 * количество используемых спутников и тип решения.
 */
public class GGA extends NMEA
{
//	     1         2       3 4        5 6 7  8   9  10 11 12 13  14   15
//	     |         |       | |        | | |  |   |   | |   | |   |    |
//	$--GGA,hhmmss.ss,llll.ll,a,yyyyy.yy,a,x,xx,x.x,x.x,M,x.x,M,x.x,xxxx*hh
//	$GPGGA,,,,,,0,,,,,,,,*66
//	$GPGGA,123519,4807.038,N,01131.000,E,1,08,0.9,545.4,M,46.9,M,,*47
//	$GPGGA,144959.0,4950.707883,N,02359.872843,E,1,04,1.6,354.0,M,,,,*0E
	
//	1 )	Time (UTC)
	public static float Time;

//	2 )	Latitude
//	3 )	N or S (North or South)
	public static float Latitude;

//	4 )	Longitude
//	5 )	E or W (East or West)
	public static float Longitude;

//	6 )	GPS Quality Indicator
	//	0 - fix not available,		нет решения
	//	1 - GPS fix					StandAlone
	//	2 - Differential GPS fix	DGPS
	//	3 - PPS
	//	4 - фиксированный RTK
	//	5 - не фиксированный RTK
	//	6 - использование данных инерциальных систем
	//	7 - ручной режим
	//	8 - режим симуляции
	public static char Quality;

//	7 )	Number of satellites in view, 00 - 12
	public static long Satellites;

//	8 )	Horizontal Dilution of precision (HDOP)
	public static float HDOP;

//	9 )	Antenna Altitude above/below mean-sea-level (geoid)
	public static float Altitude;

//	10)	Units of antenna altitude, meters 
	public static char Units;

//	11)	Geoidal separation, the difference between the WGS-84 earth ellipsoid and
//		mean-sea-level (geoid), "-" means mean-sea-level below ellipsoid
//	12)	Units of geoid separation, meters
//	13)	Age of differential GPS data, time in seconds since last SC104
//		type 1 or 9 update, null field when GPS is not used
//	14)	Differential reference station ID, 0000-1023
//	15)	Checksum
	
	public static void Parse()
	{
		Time = readFloat();
		Latitude = readLatitude()	* (readChar() == 'S' ? -1 : +1);
		Longitude = readLongitude()	* (readChar() == 'W' ? -1 : +1);
		Quality = readChar();
		Satellites = readInteger();
		HDOP = readFloat();
		Altitude = readFloat();
		Units = readChar();
	}
}
