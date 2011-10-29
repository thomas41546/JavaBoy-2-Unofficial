/*

JavaBoy
                                  
COPYRIGHT (C) 2001 Neil Millstone and The Victoria University of Manchester
                                                                         ;;;
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

class VoluntaryWaveGenerator {
 public static final int CHAN_LEFT = 1;
 public static final int CHAN_RIGHT = 2;
 public static final int CHAN_MONO = 4;

 int totalLength;
 int cyclePos;
 int cycleLength;
 int amplitude;
 int channel;
 int sampleRate;
 int volumeShift;

 byte[] waveform = new byte[32];

 public VoluntaryWaveGenerator(int waveLength, int ampl, int duty, int chan, int rate) {
  cycleLength = waveLength;
  amplitude = ampl;
  cyclePos = 0;
  channel = chan;
  sampleRate = rate;
 }

 public VoluntaryWaveGenerator(int rate) {
  cyclePos = 0;
  channel = CHAN_LEFT | CHAN_RIGHT;
  cycleLength = 2;
  totalLength = 0;
  sampleRate = rate;
  amplitude = 32;
 }

 public void setSampleRate(int sr) {
  sampleRate = sr;
 }

 public void setFrequency(int gbFrequency) {
//  cyclePos = 0;
  float frequency = (int) ((float) 65536 / (float) (2048 - gbFrequency));
//  System.out.println("gbFrequency: " + gbFrequency + "");
  cycleLength = (int) ((float) (256f * sampleRate) / (float) frequency);
  if (cycleLength == 0) cycleLength = 1;
//  System.out.println("Cycle length : " + cycleLength + " samples");
 }

 public void setChannel(int chan) {
  channel = chan;
 }

 public void setLength(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (256 - gbLength) / 4;
  }
 }

 public void setSamplePair(int address, int value) {
  waveform[address * 2] = (byte) ((value & 0xF0) >> 4);
  waveform[address * 2 + 1] = (byte) ((value & 0x0F)) ;
 }

 public void setVolume(int volume) {
  switch (volume) {
   case 0 : volumeShift = 5;
            break;
   case 1 : volumeShift = 0;
            break;
   case 2 : volumeShift = 1;
            break;
   case 3 : volumeShift = 2;
            break;
  }
//  System.out.println("A:"+volume);
 }

 public void play(byte[] b, int length, int offset) {
  int val;

  if (totalLength != 0) {
   totalLength--;

   for (int r = offset; r < offset + length; r++) {

    int samplePos = (31 * cyclePos) / cycleLength;
    val = JavaBoy.unsign(waveform[samplePos % 32]) >> volumeShift << 1;
//    System.out.print(" " + val);

    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
    if ((channel & CHAN_MONO) != 0) b[r] += val;

 //   System.out.print(val + " ");
    cyclePos = (cyclePos + 256) % cycleLength;
   }
  }
 }

}
