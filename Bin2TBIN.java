/****************************************************************************
 *
 *  Copyright (C) 2004 Dallas Semiconductor Corporation. All rights Reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 *  IN NO EVENT SHALL DALLAS SEMICONDUCTOR BE LIABLE FOR ANY CLAIM, DAMAGES
 *  OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 *  ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 *  OTHER DEALINGS IN THE SOFTWARE.
 *
 *  Except as contained in this notice, the name of Dallas Semiconductor
 *  shall not be used except as stated in the Dallas Semiconductor
 *  Branding Policy.
 *
 *  Author: KLA
 *  Original Version: March 11, 2004
 *
 *  Revision History:
 *     03-11-04 KLA 1.01 Initial version of software.  This tool allows you to convert any 
 *                       binary image into a TBIN file suitable for loading by TINI's
 *                       bootloader programs.
 *
 */
import java.io.*;

public class Bin2TBIN
{
	public static final String DATE = "March 11, 2004";
	public static final String VERSION = "1.01";

	public static void main(String[] args) throws Exception
	{
		System.out.println("Dallas Semiconductor Bin2TBIN program.  KLA "+DATE+" Version "+VERSION);
		if ((args.length < 2) || (args.length > 3))
		{
			System.out.println("Bin2TBIN");
			System.out.println("");
			System.out.println("Usage:");
			System.out.println("   java Bin2TBIN FILENAME STARTADDR [OUTPUTFILENAME]");
			System.out.println("");
			System.out.println("Where:");
			System.out.println("");
			System.out.println("   FILENAME       Name of the file that will be converted to TBIN format");
			System.out.println("   STARTADDR      Hex address that TBIN will load at with a TINI loader");
			System.out.println("   OUTPUTFILENAME Optional file name for output tbin.  Default is the");
			System.out.println("                  original FILENAME but with a tbin extension");
			return;
		}

		String filename = args[0];
		int address = Integer.parseInt(args[1], 16);
		String newfilename = filename;
		if (args.length == 3)
		{
			newfilename = args[2];
		}
		else if (newfilename.lastIndexOf('.') != -1)
		{
			newfilename = newfilename.substring(0, newfilename.lastIndexOf('.'));
			newfilename = newfilename + ".tbin";
		}

		System.out.println("Input file     : "+filename);
		System.out.println("Target address : "+Integer.toHexString(address));
		System.out.println("New file name  : "+newfilename);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fin = new FileInputStream(filename);
		byte[] buffer = new byte[1024];
		int amount = fin.read(buffer);
		while (amount != -1)
		{
			baos.write(buffer, 0, amount);
			amount = fin.read(buffer);
		}
		fin.close();




		byte[] output = baos.toByteArray();
		FileOutputStream fout = new FileOutputStream(newfilename);
		int offset = 0;
		int bytestogo = output.length;
		int lastaddress = address;
		int bytesnow;

		while (bytestogo > 0)
		{
			// Write the starting address for this block.
			fout.write(offset+address);
			fout.write((offset+address) >>> 8);
			fout.write((offset+address) >>> 16);
		
			if (bytestogo > (65536-(lastaddress & 0xFFFF)))
				bytesnow = (65536-(lastaddress & 0xFFFF));
			else
				bytesnow = bytestogo;

			// Write length (length - 1 is what we write)
			fout.write(bytesnow - 1);
			fout.write((bytesnow - 1) >>> 8);

			// Write data
			int crc = computeCRC(output,offset,bytesnow,0);
			fout.write(output,offset,bytesnow);

			fout.write(crc);
			fout.write(crc >>> 8);

			System.out.println("Segment start address: "+Integer.toHexString(offset+address));
			System.out.println("   length: "+bytesnow);
			System.out.println("      CRC: "+Integer.toHexString(crc & 0xFFFF));

			bytestogo -= bytesnow;
			offset += bytesnow;
			lastaddress += bytesnow;
		}

		
		
	}


	private static final int[] ODD_PARITY = { 0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1, 0 };

	static int computeCRC (int dataToCrc, int seed)
	{
		int dat = ((dataToCrc ^ (seed & 0xFF)) & 0xFF);
	
		seed = seed >>> 8;
		int indx1 = (dat & 0x0F);
		int indx2 = (dat >>> 4);
	
		if ((ODD_PARITY [indx1] ^ ODD_PARITY [indx2]) == 1)
			seed = seed ^ 0xC001;
	
		dat  = (dat << 6);
		seed = seed ^ dat;
		dat  = (dat << 1);
		seed = seed ^ dat;
		return seed;
	}

	static int computeCRC (byte dataToCrc [], int off, int len)
	{
		return computeCRC(dataToCrc, off, len, 0);
	}

	static int computeCRC (byte dataToCrc [], int off, int len, int seed)
	{
		// loop to do the crc on each data element
		for (int i = 0; i < len; i++)
			seed = computeCRC(dataToCrc [i + off], seed);
		return seed;
	}

}