/*

JavaBoy
                                  
COPYRIGHT (C) 2001 Neil Millstone and The Victoria University of Manchester
                                                                        
This program is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the Free
Software Foundation; either version 2 of the License, or (at your option)
any later version.        

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
more details.


You should have received a copy of the GNU General Public License along with
this program; if not, write to the Free Software Foundation, Inc., 59 Temple
Place - Suite 330, Boston, MA 02111-1307, USA.

*/

import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import java.io.*;
import java.applet.*;
import java.net.*;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.ComponentListener;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.util.StringTokenizer;
import javax.sound.sampled.*;
/** This class represents a palette.  There can be three
 *  palettes, one for the background and window, and two
 *  for sprites. 
 */

class GameboyPalette { 

/** Data for which colour maps to which RGB value */
 short[] data = new short[4];

 int[] gbcData = new int[4];

 /** Default RGB colour values */
 int[] colours = {0xFFFFFFFF, 0xFFAAAAAA, 0xFF555555, 0xFF000000};

 /** Create a palette with the specified colour mappings */
 public GameboyPalette(int c1, int c2, int c3, int c4) {  data[0] = (short) c1;
  data[1] = (short) c2;
  data[2] = (short) c3;
  data[3] = (short) c4;
 }

 /** Create a palette from the internal Gameboy format */
 public GameboyPalette(int pal) {
  decodePalette(pal);
 } 

/** Change the colour mappings */
 public void setColours(int c1, int c2, int c3, int c4) {
  colours[0] = c1;
  colours[1] = c2;
  colours[2] = c3;
  colours[3] = c4;
 }

 /** Get the palette from the internal Gameboy Color format */
 public byte getGbcColours(int entryNo, boolean high) {
  if (high) {
   return (byte) (gbcData[entryNo] >> 8);

  } else {
   return (byte) (gbcData[entryNo] & 0x00FF);

  }
 }


 /** Set the palette from the internal Gameboy Color format */
 public void setGbcColours(int entryNo, boolean high, int dat) {
  if (high) {
   gbcData[entryNo] = (gbcData[entryNo] & 0x00FF) | (dat << 8);

  } else {
   gbcData[entryNo] = (gbcData[entryNo] & 0xFF00) | dat;

  }

  int red = (gbcData[entryNo] & 0x001F) << 3;

  int green = (gbcData[entryNo] & 0x03E0) >> 2;

  int blue = (gbcData[entryNo] & 0x7C00) >> 7;


  data[0] = 0;

  data[1] = 1;

  data[2] = 2;

  data[3] = 3;


  Color c = new Color(red, green, blue);

  colours[entryNo] = c.getRGB();


//  System.out.println("Colour " + entryNo + " set to " + red + ", " + green + ", " + blue);

 }
  
 /** Set the palette from the internal Gameboy format */
 public void decodePalette(int pal) {
  data[0] = (short) (pal & 0x03);
  data[1] = (short) ((pal & 0x0C) >> 2);
  data[2] = (short) ((pal & 0x30) >> 4);
  data[3] = (short) ((pal & 0xC0) >> 6);
 }

 /** Get the RGB colour value for a specific colour entry */ public int getRgbEntry(int e) {
  return colours[data[e]];
 }
 /** Get the colour number for a specific colour entry */
 public short getEntry(int e) {
  return data[e];
 }
}
