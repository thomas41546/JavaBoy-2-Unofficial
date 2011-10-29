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


/** This class can mix a square wave signal with a sound buffer.
 *  It supports all features of the Gameboys sound channels 1 and 2.
 */
class SquareWaveGenerator {
 /** Sound is to be played on the left channel of a stereo sound */
 public static final int CHAN_LEFT = 1;

 /** Sound is to be played on the right channel of a stereo sound */
 public static final int CHAN_RIGHT = 2;

 /** Sound is to be played back in mono */
 public static final int CHAN_MONO = 4;

 /** Length of the sound (in frames) */
 int totalLength;

 /** Current position in the waveform (in samples) */
 int cyclePos;

 /** Length of the waveform (in samples) */
 int cycleLength;

 /** Amplitude of the waveform */
 int amplitude;

 /** Amount of time the sample stays high in a single waveform (in eighths) */
 int dutyCycle;

 /** The channel that the sound is to be played back on */
 int channel;

 /** Sample rate of the sound buffer */
 int sampleRate;

 /** Initial amplitude */
 int initialEnvelope;

 /** Number of envelope steps */
 int numStepsEnvelope;

 /** If true, envelope will increase amplitude of sound, false indicates decrease */
 boolean increaseEnvelope;

 /** Current position in the envelope */
 int counterEnvelope;

 /** Frequency of the sound in internal GB format */
 int gbFrequency;

 /** Amount of time between sweep steps. */
 int timeSweep;

 /** Number of sweep steps */
 int numSweep;

 /** If true, sweep will decrease the sound frequency, otherwise, it will increase */
 boolean decreaseSweep;

 /** Current position in the sweep */
 int counterSweep;

 /** Create a square wave generator with the supplied parameters */
 public SquareWaveGenerator(int waveLength, int ampl, int duty, int chan, int rate) {
  cycleLength = waveLength;
  amplitude = ampl;
  cyclePos = 0;
  dutyCycle = duty;
  channel = chan;
  sampleRate = rate;
 }

 /** Create a square wave generator at the specified sample rate */
 public SquareWaveGenerator(int rate) {
  dutyCycle = 4;
  cyclePos = 0;
  channel = CHAN_LEFT | CHAN_RIGHT;
  cycleLength = 2;
  totalLength = 0;
  sampleRate = rate;
  amplitude = 32;
  counterSweep = 0;
 }

 /** Set the sound buffer sample rate */
 public void setSampleRate(int sr) {
  sampleRate = sr;
 }

 /** Set the duty cycle */
 public void setDutyCycle(int duty) {
  switch (duty) {
   case 0 : dutyCycle = 1;
            break;
   case 1 : dutyCycle = 2;
            break;
   case 2 : dutyCycle = 4;
            break;
   case 3 : dutyCycle = 6;
            break;
  }
//  System.out.println(dutyCycle);
 }

 /** Set the sound frequency, in internal GB format */
 public void setFrequency(int gbFrequency) {
  try {
  float frequency = 131072 / 2048;

  if (gbFrequency != 2048) {
   frequency = ((float) 131072 / (float) (2048 - gbFrequency));
  }
//  System.out.println("gbFrequency: " + gbFrequency + "");
  this.gbFrequency = gbFrequency;
  if (frequency != 0) {
   cycleLength = (256 * sampleRate) / (int) frequency;
  } else {
   cycleLength = 65535;
  }
  if (cycleLength == 0) cycleLength = 1;
//  System.out.println("Cycle length : " + cycleLength + " samples");
  } catch (ArithmeticException e) {
   // Skip ip
  }
 }

 /** Set the channel for playback */
 public void setChannel(int chan) {
  channel = chan;
 }

 /** Set the envelope parameters */
 public void setEnvelope(int initialValue, int numSteps, boolean increase) {
  initialEnvelope = initialValue;
  numStepsEnvelope = numSteps;
  increaseEnvelope = increase;
  amplitude = initialValue * 2;
 }

 /** Set the frequency sweep parameters */
 public void setSweep(int time, int num, boolean decrease) {
  timeSweep = (time + 1) / 2;
  numSweep = num;
  decreaseSweep = decrease;
  counterSweep = 0;
//  System.out.println("Sweep: " + time + ", " + num + ", " + decrease);
 }

 public void setLength(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (64 - gbLength) / 4;
  }
 }

 public void setLength3(int gbLength) {
  if (gbLength == -1) {
   totalLength = -1;
  } else {
   totalLength = (256 - gbLength) / 4;
  }
 }

 public void setVolume3(int volume) {
  switch (volume) {
   case 0 : amplitude = 0;
            break;
   case 1 : amplitude = 32;
            break;
   case 2 : amplitude = 16;
            break;
   case 3 : amplitude = 8;
            break;
  }
//  System.out.println("A:"+volume);
 }

 /** Output a frame of sound data into the buffer using the supplied frame length and array offset. */
 public void play(byte[] b, int length, int offset) {
  int val = 0;

  if (totalLength != 0) {
   totalLength--;

   if (timeSweep != 0) {
    counterSweep++;
    if (counterSweep > timeSweep) {
     if (decreaseSweep) {
      setFrequency(gbFrequency - (gbFrequency >> numSweep));
     } else {
      setFrequency(gbFrequency + (gbFrequency >> numSweep));
     }
     counterSweep = 0;
    }
   }

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
   for (int r = offset; r < offset + length; r++) {

    if (cycleLength != 0) {
     if (((8 * cyclePos) / cycleLength) >= dutyCycle) {
      val = amplitude;
     } else {
      val = -amplitude;
     }
    }

/*    if (cyclePos >= (cycleLength / 2)) {
     val = amplitude;
    } else {
     val = -amplitude;
    }*/


    if ((channel & CHAN_LEFT) != 0) b[r * 2] += val;
    if ((channel & CHAN_RIGHT) != 0) b[r * 2 + 1] += val;
    if ((channel & CHAN_MONO) != 0) b[r] += val;

 //   System.out.print(val + " ");

    cyclePos = (cyclePos + 256) % cycleLength;
   }
  }
 }

}
