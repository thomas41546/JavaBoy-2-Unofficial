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
import java.util.Hashtable;
import javax.sound.sampled.*;


public class DefineControls extends Frame implements KeyListener, WindowListener, ActionListener {

  TextField[] controlsField = new TextField[8];

  Hashtable keyNames;

  public DefineControls() {
   super("Define Controls");

   keyNames = new Hashtable();
   keyNames.put(new Integer(38), "Up arrow");
   keyNames.put(new Integer(40), "Down arrow");
   keyNames.put(new Integer(37), "Left arrow");
   keyNames.put(new Integer(39), "Right arrow");
   keyNames.put(new Integer(36), "Pad 7");
   keyNames.put(new Integer(33), "Pad 9");
   keyNames.put(new Integer(35), "Pad 1");
   keyNames.put(new Integer(64), "Pad 3");
   keyNames.put(new Integer(12), "Pad 5");
   keyNames.put(new Integer(155), "Insert");
   keyNames.put(new Integer(36), "Home");
   keyNames.put(new Integer(33), "Page up");
   keyNames.put(new Integer(127), "Delete");
   keyNames.put(new Integer(35), "End");
   keyNames.put(new Integer(34), "Page down");
   keyNames.put(new Integer(10), "Return");
   keyNames.put(new Integer(16), "Shift");
   keyNames.put(new Integer(17), "Control");
   keyNames.put(new Integer(18), "Alt");
   keyNames.put(new Integer(32), "Space");
   keyNames.put(new Integer(20), "Caps lock");
   keyNames.put(new Integer(8), "Backspace");

   GridLayout g = new GridLayout(9, 2, 12, 12);

   setLayout(g);

  controlsField[0] = addControlsLine("D-pad up:");
  controlsField[1] = addControlsLine("D-pad down:");
  controlsField[2] = addControlsLine("D-pad left:");
  controlsField[3] = addControlsLine("D-pad right:");
  controlsField[4] = addControlsLine("A button:");
  controlsField[5] = addControlsLine("B button:");
  controlsField[6] = addControlsLine("Start button:");
  controlsField[7] = addControlsLine("Select button:");

  for (int r = 0; r < 8; r++) {
   controlsField[r].setText(getKeyDesc(JavaBoy.keyCodes[r], (char) JavaBoy.keyCodes[r])
      + " (" + JavaBoy.keyCodes[r] + ")");
  }

  Button cancel = new Button("Close");
  cancel.setActionCommand("Controls close");
  cancel.addActionListener(this);
  add(cancel);

  setSize(230, 300);
  setResizable(false);
  addWindowListener(this);
  show();
 }

 public String getKeyDesc(int code, char c) {
  if (keyNames.containsKey(new Integer(code))) {
   return (String) keyNames.get(new Integer(code));
  } else {
   return c + "";
  }
 }

 public TextField addControlsLine(String name) {
  add(new Label(name));
  TextField t = new TextField(4);
  t.addKeyListener(this);
  add(t);
  return t;
 }

 public void keyPressed(KeyEvent e) {
 }

 public void keyReleased(KeyEvent e) {
  System.out.println(e.getKeyCode() + ", " + e.getKeyChar());

  for (int r = 0; r < 8; r++) {
   if (e.getSource() == controlsField[r]) {
    controlsField[r].setText(getKeyDesc(e.getKeyCode(), e.getKeyChar()) + " (" + e.getKeyCode() + ")");
    JavaBoy.keyCodes[r] = e.getKeyCode();
   }
  }
 }

 public void keyTyped(KeyEvent e) {

 }

 public void windowClosed(WindowEvent e) {
 }

 public void windowClosing(WindowEvent e) {
  hide();
 }

 public void windowOpened(WindowEvent e) {
 }

 public void windowIconified(WindowEvent e) {
 }

 public void windowDeiconified(WindowEvent e) {
 }

 public void windowActivated(WindowEvent e) {
 }

 public void windowDeactivated(WindowEvent e) {
 }

 public void actionPerformed(ActionEvent e) {
  hide();
 }

}