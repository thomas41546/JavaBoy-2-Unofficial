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

/** This class implements the serial communcation (Game Link) on the Gameboy using a socket connection
 *  to a remote host.  A server must bind to a port, and a client must connect to the port, then
 *  the two emulated Gameboys will appear to be connected via a serial link.
 */

class TCPGameLink extends GameLink implements Runnable {
 final int PORTNO = 1989;

 Dmgcpu dmgcpu;
 ServerSocket server;
 Socket client;
 BufferedInputStream inStream;
 BufferedOutputStream outStream;
 boolean terminate = false;


 /** A client has connected to the server */
 boolean clientConnected = false;

 /** Interface parent for error dialogs */
 Frame parent;

 /** Create a Game Link server, and bind to port 1989 */
 public TCPGameLink(Frame parent) {
  this.parent = parent;
  try {
   server = new ServerSocket(PORTNO);
   new ModalDialog(parent, "Server startup succeeded!", "Game Link server running", "A client can now connect.");
   Thread t = new Thread(this);
   t.start();
   serverRunning = true;
  } catch (Exception e) {
   new ModalDialog(parent, "Server startup failed", "Cannot start server on port " + PORTNO,
     "Check that the port is not in use.");
  }
 }

 /** Create a Game Link client, and connect to the specified IP address */
 public TCPGameLink(Frame parent, String ip) {
  this.parent = parent;
  try {
   client = new Socket(InetAddress.getByName(ip), PORTNO);
   client.setTcpNoDelay(true);
   outStream =
     new BufferedOutputStream(client.getOutputStream());
   inStream =
     new BufferedInputStream(client.getInputStream());
   new ModalDialog(parent, "Connect succeeded!", "Connected to Game Link server.", "You may now start a two player game.");
   Thread t = new Thread(this);
   t.start();
   clientConnected = true;
  } catch (Exception e) {
    new ModalDialog(parent, "Connect failed", "Cannot connect to host " + ip + ":" + PORTNO,
      "Ensure 'allow connections' is checked on the server.");
  }
 }

 /** Set the CPU that is using the Game Link connection */
 public void setDmgcpu(Dmgcpu d) {
  dmgcpu = d;
 }

 /** Stop the Game Link server/client connection */
 public void shutDown() {
  terminate = true;
  try {
   if (server != null) server.close();
  } catch (Exception e) {

  }
 }

 /** Output an int value */
 public void writeInt(OutputStream s, int i) {
  int b1, b2, b3, b4;

  b1 = i & 0x000000FF;
  b2 = (i & 0x0000FF00) >> 8;
  b3 = (i & 0x00FF0000) >> 16;
  b4 = (i & 0xFF000000) >> 24;

  try {
   s.write(b1);
   s.write(b2);
   s.write(b3);
   s.write(b4);
  } catch (IOException e) {

  }
 }

 public void run() {

  while (((serverRunning) || (clientConnected)) && (!terminate)) {

   try {

    if (serverRunning) {
     Socket conn = null;
     conn = server.accept();
     System.out.println("Connection established!");
     outStream =
       new BufferedOutputStream(conn.getOutputStream());
     inStream =
       new BufferedInputStream(conn.getInputStream());
    }


    int data = 0, clock, initial, b1, b2, b3, b4, v = 0;
    while ((data != -1) && (!terminate)) {           /* This needs to terminate */

     if (dmgcpu != null) {
      v = dmgcpu.instrCount;
     }

     initial = inStream.read();

     b1 = inStream.read();
     b2 = inStream.read();
     b3 = inStream.read();
     b4 = inStream.read();

//    System.out.println(b1+" "+b2+" "+b3+" "+b4);

    clock = b1 + (b2 << 8) + (b3 << 16) + (b4 << 24);

     data = inStream.read();
//    System.out.println("Synched: " + clock + " : " + dmgcpu.instrCount);

//    System.out.println(v + " " + dmgcpu.instrCount);
     if (dmgcpu != null) {
      while ( /*(v + 600 > dmgcpu.instrCount) || /*((clock != -1) && (clock > dmgcpu.instrCount)) ||*/ (!dmgcpu.interruptsEnabled) || ((dmgcpu.ioHandler.registers[0x0F] & dmgcpu.INT_SER) != 0) && (!terminate)) {
       try {
        java.lang.Thread.sleep(5);
       } catch (InterruptedException e) {
 
       }
      }
     }


     if (initial == 1) {
//      System.out.print("<-- " + data + ":" + JavaBoy.unsign(dmgcpu.ioHandler.registers[0x02]) + "    ");
      outStream.write(0);
  
      writeInt(outStream, -1);

      outStream.write(dmgcpu.ioHandler.registers[0x01]);
      outStream.flush();
      dmgcpu.ioHandler.registers[0x02] &= 0x7F;
//      System.out.println("--> " + JavaBoy.unsign(dmgcpu.ioHandler.registers[0x01]));

//     if ((clock & 0x01) != (JavaBoy.unsign(dmgcpu.ioHandler.registers[0x02]) & 0x01)) {
      dmgcpu.ioHandler.registers[0x01] = (byte) data;
      dmgcpu.triggerInterrupt(dmgcpu.INT_SER);
//     }
     } else if (initial == 0) {
      dmgcpu.ioHandler.registers[0x02] &= 0x7F;
      dmgcpu.ioHandler.registers[0x01] = (byte) data;
      dmgcpu.triggerInterrupt(dmgcpu.INT_SER);
     }


    }   

   } catch (IOException e) {
    // Nothing!
   }

   clientConnected = false;
   new ModalDialog(parent, "Connection lost", "The connection with the other", "machine has been lost.");
  }



 }

 /** Send a byte to the remote Gameboy */
 public void send(byte b) {
  try {
   outStream.write(1);
   writeInt(outStream, dmgcpu.instrCount);
   outStream.write(b);
   outStream.flush();

//   System.out.println("--> " + JavaBoy.unsign(b) + ":" + JavaBoy.unsign(dmgcpu.ioHandler.registers[0x02]));
   try {
    java.lang.Thread.sleep(10);
   } catch (InterruptedException e) {

   }

  } catch (IOException e) {

  }
 }


}
