package com.iaaa.gps.nmea;

import com.iaaa.gps.NMEA;

/// <summary>
/// Общая информация о спутниках
/// </summary>
public class GSA extends NMEA
{
	// $GPGSA,A,3,04,05,,09,12,,,24,,,,,2.5,1.3,2.1*39 
	public static char Mode;	// 'A' or 'M'
	public static char Type;	// 1 - no solve, 2 - 2D, 3 - 3D
	public static int[] PRN = new int[12];	// PRN коды используемых в подсчете позиции спутников (12 полей)
	public static float PDOP;	// пространственный геометрический фактор, PDOP
	public static float HDOP;	// горизонтальный геометрический фактор, HDOP
	public static float VDOP;	// вертикальный геометрический фактор, VDOP
	
	public static void Parse()
	{
		Mode = readChar();
		Type = readChar();
		
		for (int i = 0; i < 12; i++)
			PRN[i] = readInteger();
		PDOP = readFloat();
		HDOP = readFloat();
		VDOP = readFloat();
	}
}
