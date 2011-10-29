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

/** This is the central controlling class for the sound.
 *  It interfaces with the Java Sound API, and handles the
 *  calsses for each sound channel.
 */
class SoundChip {
 /** The DataLine for outputting the sound */
 SourceDataLine soundLine;

 SquareWaveGenerator channel1;
 SquareWaveGenerator channel2;
 VoluntaryWaveGenerator channel3;
 NoiseGenerator channel4;
 boolean soundEnabled = false;

 /** If true, channel is enabled */
 boolean channel1Enable = true, channel2Enable = true,
         channel3Enable = true, channel4Enable = true;

 /** Current sampling rate that sound is output at */
 int sampleRate = 44100;

 /** Amount of sound data to buffer before playback */
 int bufferLengthMsec = 200;

 /** Initialize sound emulation, and allocate sound hardware */
 public SoundChip() {
  soundLine = initSoundHardware();
  channel1 = new SquareWaveGenerator(sampleRate);
  channel2 = new SquareWaveGenerator(sampleRate);
  channel3 = new VoluntaryWaveGenerator(sampleRate);
  channel4 = new NoiseGenerator(sampleRate);
 }

 /** Initialize sound hardware if available */
 public SourceDataLine initSoundHardware() {

  try {
   AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
    sampleRate, 8, 2, 2, sampleRate, true);
   DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, format);

   if (!AudioSystem.isLineSupported(lineInfo)) {
    System.out.println("Error: Can't find audio output system!");
    soundEnabled = false;
   } else {
    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(lineInfo);

    int bufferLength = (sampleRate / 1000) * bufferLengthMsec;
    line.open(format, bufferLength);
    line.start();
//    System.out.println("Initialized audio successfully.");
    soundEnabled = true;
    return line;
   }
  } catch (Exception e) {
   System.out.println("Error: Audio system busy!");
   soundEnabled = false;
  }

  return null;
 }

 /** Change the sample rate of the playback */
 public void setSampleRate(int sr) {
  sampleRate = sr;

  soundLine.flush();
  soundLine.close();

  soundLine = initSoundHardware();

  channel1.setSampleRate(sr);
  channel2.setSampleRate(sr);
  channel3.setSampleRate(sr);
  channel4.setSampleRate(sr);
 }

 /** Change the sound buffer length */
 public void setBufferLength(int time) {
  bufferLengthMsec = time;

  soundLine.flush();
  soundLine.close();

  soundLine = initSoundHardware();
 }

 /** Adds a single frame of sound data to the buffer */
 public void outputSound() {
  if (soundEnabled) {
   int numSamples;

   if (sampleRate / 28 >= soundLine.available() * 2) {
    numSamples = soundLine.available() * 2;
   } else {
    numSamples = (sampleRate / 28) & 0xFFFE;
   }

   byte[] b = new byte[numSamples];
   if (channel1Enable) channel1.play(b, numSamples / 2, 0);
   if (channel2Enable) channel2.play(b, numSamples / 2, 0);
   if (channel3Enable) channel3.play(b, numSamples / 2, 0);
   if (channel4Enable) channel4.play(b, numSamples / 2, 0);
   soundLine.write(b, 0, numSamples);
  }
 }

}


