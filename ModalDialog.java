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


/** This class implements a dialog box with some text and an OK button.  It is used for error 
  *  messages and confirmation messages. */
  
interface DialogListener {
 void yesPressed();
 void noPressed();
}

class ModalDialog implements ActionListener 
{ 
  Frame dialog; 
  boolean yesNo;
  boolean result;
  boolean finished;
  DialogListener listener;
  
  /** Create a dialog box with the specified title and body text */ 
  public ModalDialog(Frame parent, String title, String text, String text2) {  
   this.yesNo = false;
   if (parent == null) {
    System.out.println(text);
    System.out.println(text2);
   }
   dialog = new Frame(title);
   dialog.add(new Label(text), "North");
   dialog.add(new Label(text2), "Center");
   Button b = new Button("Ok");
   b.setActionCommand("Close dialog");
   b.addActionListener(this);
   dialog.add(b, "South");
   dialog.setSize(350, 110);
   dialog.show();

   finished = false;

  } 
  
  public ModalDialog(Frame parent, String title, String text, DialogListener listener) {  
   this.yesNo = true;
   this.listener = listener;
   if (parent == null) {
    System.out.println(text);
   }

   dialog = new Frame(title);
   dialog.setLayout(new GridLayout(3, 1));

   dialog.add(new Label(text));

   Button b = new Button("Yes");
   b.setActionCommand("Yes");
   b.addActionListener(this);
   dialog.add(b);

   b = new Button("No");
   b.setActionCommand("No");
   b.addActionListener(this);
   dialog.add(b);

   dialog.setSize(350, 110);
   dialog.show();

   finished = false;
	
} 

 public boolean getResult() {
  return result;
 }

 public boolean getFinished() {
  return finished;
 }

 public void actionPerformed(ActionEvent e) {  
  result = !e.getActionCommand().equals("No");
  if (yesNo) {
   if (result) {
    listener.yesPressed();
   } else {
    listener.noPressed();
   }
  }

  dialog.hide();
  dialog = null;
  finished = true;
 }
}