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

import java.awt.*;
import java.awt.image.*;
import java.lang.*;
import java.io.*;
import java.applet.*;
import java.net.*;
import java.util.Random;
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

/** This is a white noise generator.  It is used to emulate
 *  channel 4.
 */

class NoiseGenerator {
 /** Indicates sound is to be played on the left channel of a stereo sound */
 public static final int CHAN_LEFT = 1;

 /** Indictaes sound is to be played on the right channel of a stereo sound */
 public static final int CHAN_RIGHT = 2;

 /** Indicates that sound is mono */
 public static final int CHAN_MONO = 4;

 /** Indicates the length of the sound in frames */
 int totalLength;
 int cyclePos;

 /** The length of one cycle, in samples */
 int cycleLength;

 /** Amplitude of the wave function */
 int amplitude;

 /** Channel being played on.  Combination of CHAN_LEFT and CHAN_RIGHT, or CHAN_MONO */
 int channel;

 /** Sampling rate of the output channel */
 int sampleRate;

 /** Initial value of the envelope */
 int initialEnvelope;

 int numStepsEnvelope;

 /** Whether the envelope is an increase/decrease in amplitude */
 boolean increaseEnvelope;

 int counterEnvelope;

 /** Stores the random values emulating the polynomial generator (badly!) */
 boolean randomValues[];

 int dividingRatio;
 int polynomialSteps;
 int shiftClockFreq;
 int finalFreq;
 int cycleOffset;

 /** Creates a white noise generator with the specified wavelength, amplitude, channel, and sample rate */
 public NoiseGenerator(int waveLength, int ampl, int chan, int rate) {
  cycleLength = waveLength;
  amplitude = ampl;
  cyclePos = 0;
  channel = chan;
  sampleRate = rate;
  cycleOffset = 0;

   randomValues = new boolean[32767];

  Random rand = new java.util.Random();

  
  for (int r = 0; r < 32767; r++) {
	randomValues[r] = rand.nextBoolean();
  }

  cycleOffset = 0;
}

 /** Creates a white noise generator with the specified sample rate */
 public NoiseGenerator(int rate) {
  cyclePos = 0;
  channel = CHAN_LEFT | CHAN_RIGHT;
  cycleLength = 2;
  totalLength = 0;
  sampleRate = rate;
  amplitude = 32;

  randomValues = new boolean[32767];

  Random rand = new java.util.Random();

  
  for (int r = 0; r < 32767; r++) {
	randomValues[r] = rand.nextBoolean();
  }

  cycleOffset = 0;
 }


 public void setSampleRate(int sr) {
  sampleRate = sr;
 }

 /** Set the channel that the white noise is playing on */
 public void setChannel(int chan) {
  channel = chan;
 }

 /** Setup the envelope, and restart it from the beginning */
 public void setEnvelope(int initialValue, int numSteps, boolean increase) {
  initialEnvelope = initialValue;
  numStepsEnvelope = numSteps;
  increaseEnvelope = increase;
  amplitude = initialValue * 2;
 }

 /** Set the length of the sound */
 public void setLength(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (64 - gbLength) / 4;
  }
 }

 public void setParameters(float dividingRatio, boolean polynomialSteps, int shiftClockFreq) {
  this.dividingRatio = (int) dividingRatio;
  if (!polynomialSteps) {
   this.polynomialSteps = 32767;
   cycleLength = 32767 << 8;
   cycleOffset = 0;
  } else {
   this.polynomialSteps = 63;
   cycleLength = 63 << 8;

   java.util.Random rand = new java.util.Random();

   cycleOffset = (int) (rand.nextFloat() * 1000);
  }
  this.shiftClockFreq = shiftClockFreq;

  if (dividingRatio == 0) dividingRatio = 0.5f;

  finalFreq = ((int) (4194304 / 8 / dividingRatio)) >> (shiftClockFreq + 1);
//  System.out.println("dr:" + dividingRatio + "  steps: " + this.polynomialSteps + "  shift:" + shiftClockFreq + "  = Freq:" + finalFreq);
 }

 /** Output a single frame of samples, of specified length.  Start at position indicated in the
  *  output array.
  */
 public void play(byte[] b, int length, int offset) {
  int val;

  if (totalLength != 0) {
   totalLength--;

   counterEnvelope++;
   if (numStepsEnvelope != 0) {
    if (((counterEnvelope % numStepsEnvelope) == 0) && (amplitude > 0)) {
     if (!increaseEnvelope) {
      if (amplitude > 0) amplitude-=2;
     } else {
      if (amplitude < 16) amplitude+=2;
     }
    }
   }


   int step = ((finalFreq) / (sampleRate >> 8));
  // System.out.println("Step=" + step);

   for (int r = offset; r < offset + length; r++) {
	boolean value = randomValues[((cycleOffset ) + (cyclePos >> 8)) & 0x7FFF];
	int v = value? (amplitude / 2): (-amplitude / 2);

    if ((channel & CHAN_LEFT) != 0) b[r * 2] += v;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += v;
    if ((channel & CHAN_MONO) != 0) b[r] += v;
   
    cyclePos = (cyclePos + step) % cycleLength;
  }

   /*
   for (int r = offset; r < offset + length; r++) {
    val = (int) ((Math.random() * amplitude * 2) - amplitude);

    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
    if ((channel & CHAN_MONO) != 0) b[r] += val;

 //   System.out.print(val + " ");

    cyclePos = (cyclePos + 256) % cycleLength;

   }*/
  }
 }

}
