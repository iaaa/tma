package com.iaaa.gps.nmea;

import com.iaaa.gps.NMEA;

/// <summary>
/// Recommended minimum specific GPS/TRANSIT data
/// 
/// Рекомендованный минимальный набор GPS данных (position, velocity, time)
/// </summary>
public class RMC extends NMEA
{
	//	     1         2 3       4 5        6 7   8   9      10 11 12
	//	     |         | |       | |        | |   |   |      |   | |
	//$--RMC,hhmmss.ss,A,llll.ll,a,yyyyy.yy,a,x.x,x.x,xxxxxx,x.x,a*hh
	//$GPRMC,,V,,,,,,,,,,N*53
	//$GPRMC,081836,A,3751.65,S,14507.36,E,000.0,360.0,130998,011.3,E*62
	//$GPRMC,041300.0,A,4950.182833,N,02359.658320,E,,,040410,,,A*65
	
	//1 )	Time (UTC)
	public static float Time;
	
	//2 )	Status, V = Navigation receiver warning
	public static char Status;
	
	//3 )	Latitude
	//4 )	N or S
	public static float Latitude;
	
	//5 )	Longitude
	//6 )	E or W
	public static float Longitude;
	
	//7 )	Speed over ground, knots
	public static float Speed;
	
	//8 )	Track made good, degrees true
	public static float Course;
	
	//9 )	Date, ddmmyy
	public static float Date;
	
	//10)	Magnetic variation
	//11)	E or W
	//магнитное склонение
	//12)	Checksum
	
	public static void Parse()
	{
		Time = readFloat();
		Status = readChar();
		Latitude = readLatitude()	* (readChar() == 'S' ? -1 : +1);
		Longitude = readLongitude()	* (readChar() == 'W' ? -1 : +1);
		Speed = readFloat();
		Course = readFloat();
		Date = readFloat();
	}
}
