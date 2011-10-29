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

/** This class is used when JavaBoy is run as an application
 *  to provide the user interface.
 */

class GameBoyScreen extends Frame implements ActionListener,
   ComponentListener, ItemListener  {
 GraphicsChip graphicsChip = null;
 JavaBoy applet;

 CheckboxMenuItem viewFrameCounter;
 CheckboxMenuItem viewSpeedThrottle;

 CheckboxMenuItem viewFrameSkip0;
 CheckboxMenuItem viewFrameSkip1;
 CheckboxMenuItem viewFrameSkip2;
 CheckboxMenuItem viewFrameSkip3;
 CheckboxMenuItem viewFrameSkip4;

 CheckboxMenuItem soundChannel1Enable;
 CheckboxMenuItem soundChannel2Enable;
 CheckboxMenuItem soundChannel3Enable;
 CheckboxMenuItem soundChannel4Enable;

 CheckboxMenuItem soundFreq11;
 CheckboxMenuItem soundFreq22;
 CheckboxMenuItem soundFreq44;

 CheckboxMenuItem soundBuffer200;
 CheckboxMenuItem soundBuffer300;
 CheckboxMenuItem soundBuffer400;

 CheckboxMenuItem networkServer;
 CheckboxMenuItem fileGameboyColor;
 
 CheckboxMenuItem viewSingle;
 CheckboxMenuItem viewDouble;
 CheckboxMenuItem viewTriple;
 CheckboxMenuItem viewQuadrouple;

 CheckboxMenuItem networkPrinter;

 TextField hostAddress;
 Dialog connectDialog;

 CheckboxMenuItem[] schemes =
   new CheckboxMenuItem[JavaBoy.schemeNames.length];

 /** Creates the JavaBoy interface, with the specified title text */
 public GameBoyScreen(String s, JavaBoy a) {
  super(s);
  applet = a;
  setWindowSize(2);

  this.addComponentListener(this);

  MenuBar menuBar = new MenuBar();

  MenuItem fileOpen = new MenuItem("Open ROM");
  fileOpen.setActionCommand("Open ROM");
  fileOpen.addActionListener(this);

  MenuItem fileEmulate = new MenuItem("Emulate");
  fileEmulate.setActionCommand("Emulate");
  fileEmulate.addActionListener(this);

  MenuItem fileReset = new MenuItem("Reset");
  fileReset.setActionCommand("Reset");
  fileReset.addActionListener(this);

  MenuItem filePause = new MenuItem("Pause");
  filePause.setActionCommand("Pause");
  filePause.addActionListener(this);

  MenuItem fileControls = new MenuItem("Define controls...");
  fileControls.setActionCommand("Controls");
  fileControls.addActionListener(this);

  fileGameboyColor = new CheckboxMenuItem("Use Gameboy Color features");
  fileGameboyColor.addItemListener(this);
  fileGameboyColor.setState(true);

  MenuItem fileQuit = new MenuItem("Exit");
  fileQuit.setActionCommand("Exit");
  fileQuit.addActionListener(this);

  viewSingle = new CheckboxMenuItem("Size: actual");
  viewSingle.addItemListener(this);

  viewDouble = new CheckboxMenuItem("Size: 2x");
  viewDouble.addItemListener(this);

  viewTriple = new CheckboxMenuItem("Size: 3x");
  viewTriple.addItemListener(this);

  viewQuadrouple = new CheckboxMenuItem("Size: 4x");
  viewQuadrouple.addItemListener(this);

  viewFrameSkip0 = new CheckboxMenuItem("Frame skip: 0");
  viewFrameSkip0.addItemListener(this);

  viewFrameSkip1 = new CheckboxMenuItem("Frame skip: 1");
  viewFrameSkip1.addItemListener(this);

  viewFrameSkip2 = new CheckboxMenuItem("Frame skip: 2");
  viewFrameSkip2.addItemListener(this);

  viewFrameSkip3 = new CheckboxMenuItem("Frame skip: 3");
  viewFrameSkip3.addItemListener(this);

  viewFrameSkip4 = new CheckboxMenuItem("Frame skip: 4");
  viewFrameSkip4.addItemListener(this);

  viewFrameCounter = new CheckboxMenuItem("Frame counter");
  viewFrameCounter.setActionCommand("Frame counter");
  viewFrameCounter.addActionListener(this);

  viewSpeedThrottle = new CheckboxMenuItem("Speed throttle");
  viewSpeedThrottle.setActionCommand("Speed throttle");
  viewSpeedThrottle.addActionListener(this);
  viewSpeedThrottle.setState(true);

  CheckboxMenuItem viewStandardCols = new CheckboxMenuItem("Standard colours");
  viewStandardCols.addItemListener(this);
  viewStandardCols.setState(true);

  CheckboxMenuItem viewLcdCols = new CheckboxMenuItem("LCD shades");
  viewLcdCols.addItemListener(this);

  CheckboxMenuItem viewGreenyCols = new CheckboxMenuItem("Greeny shades");
  viewGreenyCols.addItemListener(this);

  MenuItem debugEnter = new MenuItem("Enter debugger");
  debugEnter.setActionCommand("Enter debugger");
  debugEnter.addActionListener(this);

  MenuItem debugExecuteScript = new MenuItem("Execute script");
  debugExecuteScript.setActionCommand("Execute script");
  debugExecuteScript.addActionListener(this);

  soundChannel1Enable = new CheckboxMenuItem("Channel 1 (Square wave)");
  soundChannel1Enable.addItemListener(this);
  soundChannel1Enable.setState(true);

  soundChannel2Enable = new CheckboxMenuItem("Channel 2 (Square wave)");
  soundChannel2Enable.addItemListener(this);
  soundChannel2Enable.setState(true);

  soundChannel3Enable = new CheckboxMenuItem("Channel 3 (Voluntary wave)");
  soundChannel3Enable.addItemListener(this);
  soundChannel3Enable.setState(true);

  soundChannel4Enable = new CheckboxMenuItem("Channel 4 (Noise)");
  soundChannel4Enable.addItemListener(this);
  soundChannel4Enable.setState(true);

  soundFreq11 = new CheckboxMenuItem("Sample rate: 11khz");
  soundFreq11.addItemListener(this);

  soundFreq22 = new CheckboxMenuItem("Sample rate: 22khz");
  soundFreq22.addItemListener(this);

  soundFreq44 = new CheckboxMenuItem("Sample rate: 44khz");
  soundFreq44.addItemListener(this);
  soundFreq44.setState(true);

  soundBuffer200 = new CheckboxMenuItem("Buffer length: 200ms");
  soundBuffer200.addItemListener(this);
  soundBuffer200.setState(true);

  soundBuffer300 = new CheckboxMenuItem("Buffer length: 300ms");
  soundBuffer300.addItemListener(this);

  soundBuffer400 = new CheckboxMenuItem("Buffer length: 400ms");
  soundBuffer400.addItemListener(this);

  MenuItem networkConnect = new MenuItem("Connect to client");
  networkConnect.setActionCommand("Connect to client");
  networkConnect.addActionListener(this);

  networkServer = new CheckboxMenuItem("Allow connections");
  networkServer.addItemListener(this);

  networkPrinter = new CheckboxMenuItem("Emulate printer");
  networkPrinter.addItemListener(this);



  Menu fileMenu = new Menu("File");
  Menu viewMenu = new Menu("View");
  Menu soundMenu = new Menu("Sound");
  Menu networkMenu = new Menu("Serial Port");
  Menu debugMenu = new Menu("Debug");

  fileMenu.add(fileOpen);
  fileMenu.add(fileReset);
  fileMenu.add(filePause);
  fileMenu.add(fileEmulate);
  fileMenu.add(fileGameboyColor);
  fileMenu.add(fileControls);
  fileMenu.add(new MenuItem("-"));
  fileMenu.add(fileQuit);

  viewMenu.add(viewSingle);
  viewMenu.add(viewDouble);
  viewMenu.add(viewTriple);
  viewMenu.add(viewQuadrouple);
  viewMenu.add(new MenuItem("-"));
  viewMenu.add(viewFrameSkip0);
  viewMenu.add(viewFrameSkip1);
  viewMenu.add(viewFrameSkip2);
  viewMenu.add(viewFrameSkip3);
  viewMenu.add(viewFrameSkip4);
  viewMenu.add(new MenuItem("-"));
  viewMenu.add(viewFrameCounter);
  viewMenu.add(viewSpeedThrottle);
  viewMenu.add(new MenuItem("-"));

  for (int r = 0; r < JavaBoy.schemeNames.length; r++) {
   schemes[r] = new CheckboxMenuItem(JavaBoy.schemeNames[r]);
   schemes[r].addItemListener(this);
   viewMenu.add(schemes[r]);
   if (r == 0) schemes[r].setState(true);
  }

  soundMenu.add(soundChannel1Enable);
  soundMenu.add(soundChannel2Enable);
  soundMenu.add(soundChannel3Enable);
  soundMenu.add(soundChannel4Enable);
  soundMenu.add(new MenuItem("-"));
  soundMenu.add(soundFreq11);
  soundMenu.add(soundFreq22);
  soundMenu.add(soundFreq44);
  soundMenu.add(new MenuItem("-"));
  soundMenu.add(soundBuffer200);
  soundMenu.add(soundBuffer300);
  soundMenu.add(soundBuffer400);

  networkMenu.add(networkConnect);
  networkMenu.add(networkServer);
  networkMenu.add(networkPrinter);

  debugMenu.add(debugEnter);
  debugMenu.add(debugExecuteScript);

  menuBar.add(fileMenu);
  menuBar.add(viewMenu);
  menuBar.add(soundMenu);
  menuBar.add(networkMenu);
  menuBar.add(debugMenu);

  setMenuBar(menuBar);

 }


 /** Creates a connection dialog for Game Link connections */
 public void makeConnectDialog() {
  connectDialog = new Dialog(this, "Game Link connect", true);
  Panel p1 = new Panel();
  Panel p2 = new Panel();
  Panel p3 = new Panel();

  p1.add(new Label("Host address:"), "Center");

  hostAddress = new TextField(35);
  p2.add(hostAddress, "Center");

  Button connectButton = new Button("Connect");
  connectButton.setActionCommand("Connect ok");
  connectButton.addActionListener(this);

  Button cancelButton = new Button("Cancel");
  cancelButton.setActionCommand("Connect cancel");
  cancelButton.addActionListener(this);

  p3.add(cancelButton, "West");
  p3.add(connectButton, "East");

  connectDialog.add(p1, "North");
  connectDialog.add(p2, "Center");
  connectDialog.add(p3, "South");

  connectDialog.setSize(350, 125);
  connectDialog.setResizable(false);
  connectDialog.show();
 }


 /** Sets the current GraphicsChip object which is responsible for drawing the screen */
 public void setGraphicsChip(GraphicsChip g) {
  graphicsChip = g;
 }

 public void update(Graphics g) {
  paint(g);
 }

 /** Clear the frame to white */
 public void clearWindow() {
  Dimension d = getSize();
  Graphics g = getGraphics();
  g.setColor(new Color(255, 255, 255));
  g.fillRect(0, 0, d.width, d.height);
 }

 public void componentHidden(ComponentEvent e) {

 }

 public void componentMoved(ComponentEvent e) {

 }

 public void componentResized(ComponentEvent e) {
  clearWindow();
 }              

 public void componentShown(ComponentEvent e) {

 }

 /** Resize the Frame to a suitable size for a Gameboy with a magnification given */
 public void setWindowSize(int mag) {
  setSize(175 * mag + 20, 174 * mag + 20);
 }

 public void setSoundFreq() {
  if ((applet.dmgcpu != null) && (applet.dmgcpu.soundChip.soundEnabled)) {
   if (soundFreq11.getState()) {
    applet.dmgcpu.soundChip.setSampleRate(11025);
   }
   if (soundFreq22.getState()) {
    applet.dmgcpu.soundChip.setSampleRate(22050);
   }
   if (soundFreq44.getState()) {
    applet.dmgcpu.soundChip.setSampleRate(44100);
   }
  }
 }

 public void setBufferLength() {
  if ((applet.dmgcpu != null) && (applet.dmgcpu.soundChip.soundEnabled)) {
   if (soundBuffer200.getState()) {
    applet.dmgcpu.soundChip.setBufferLength(200);
   }
   if (soundBuffer300.getState()) {
    applet.dmgcpu.soundChip.setBufferLength(300);
   }
   if (soundBuffer400.getState()) {
    applet.dmgcpu.soundChip.setBufferLength(400);
   }
  }
 }

 public void setChannelEnable() {
  if ((applet.dmgcpu != null) && (applet.dmgcpu.soundChip.soundEnabled)) {
   applet.dmgcpu.soundChip.channel1Enable = soundChannel1Enable.getState();
   applet.dmgcpu.soundChip.channel2Enable = soundChannel2Enable.getState();
   applet.dmgcpu.soundChip.channel3Enable = soundChannel3Enable.getState();
   applet.dmgcpu.soundChip.channel4Enable = soundChannel4Enable.getState();
  }
 }

 public void setMagnify() {
  if (applet.dmgcpu != null) {
   if (viewSingle.getState()) {
    applet.dmgcpu.graphicsChip.setMagnify(1);
   }
   if (viewDouble.getState()) {
    applet.dmgcpu.graphicsChip.setMagnify(2);
   }
   if (viewTriple.getState()) {
    applet.dmgcpu.graphicsChip.setMagnify(3);
   }
   if (viewQuadrouple.getState()) {
    applet.dmgcpu.graphicsChip.setMagnify(4);
   }
  }
 }

 public void setFrameSkip() {
  if (applet.dmgcpu != null) {
   if (viewFrameSkip0.getState()) {
    graphicsChip.frameSkip = 1;
   }
   if (viewFrameSkip1.getState()) {
    graphicsChip.frameSkip = 2;
   }
   if (viewFrameSkip2.getState()) {
    graphicsChip.frameSkip = 3;
   }
   if (viewFrameSkip3.getState()) {
    graphicsChip.frameSkip = 4;
   }
   if (viewFrameSkip4.getState()) {
    graphicsChip.frameSkip = 5;
   }
  }
 }

 public void actionPerformed(ActionEvent e) {
  String command = e.getActionCommand();
//  System.out.println("Command: " + command);

  if (command.equals("Open ROM")) {

   if (applet.dmgcpu != null) {
    applet.dmgcpu.terminate = true;
    if (applet.cartridge != null) applet.cartridge.dispose();
    if (applet.dmgcpu != null) {
     applet.dmgcpu.dispose();
     applet.dmgcpu = null;
    }
    clearWindow();
   }

   FileDialog fd = new FileDialog(this, "Open ROM");
   fd.show();

   if (fd.getFile() != null) {
    applet.cartridge = new Cartridge(fd.getDirectory() + fd.getFile(), this);
    applet.dmgcpu = new Dmgcpu(applet.cartridge, applet.gameLink, this);
//	applet.gameBoyPrinter = new GameBoyPrinter();
    if (applet.gameLink != null) applet.gameLink.setDmgcpu(applet.dmgcpu);
    setGraphicsChip(applet.dmgcpu.graphicsChip);
    setSoundFreq();
    setBufferLength();
    setMagnify();
    setFrameSkip();
    setChannelEnable();
    applet.dmgcpu.allowGbcFeatures = fileGameboyColor.getState();
    applet.dmgcpu.reset();
   }

  } else if (command.equals("Frame counter")) {
   viewFrameCounter.setState(!viewFrameCounter.getState());
  } else if (command.equals("Speed throttle")) {
   viewSpeedThrottle.setState(!viewSpeedThrottle.getState());
  } else if (command.equals("Emulate")) {
   if ((applet.cartridge != null) && (applet.cartridge.cartridgeReady)) {
    applet.queueDebuggerCommand("g");
    applet.dmgcpu.terminate = true;
   } else {
    new ModalDialog(this, "Error", "You need to load a ROM before", "you select 'Emulate'.");    
   }
  } else if (command.equals("Reset")) {
   applet.queueDebuggerCommand("s;g");
   applet.dmgcpu.terminate = true;
  } else if (command.equals("Pause")) {
   applet.dmgcpu.terminate = true;
  } else if (command.equals("Controls")) {
//   makeControlsDialog();
   new DefineControls();
  } else if (command.equals("Execute script")) {
   if (applet.dmgcpu != null) {
    FileDialog fd = new FileDialog(this, "Execute debugger script");
    fd.show();
    applet.queueDebuggerCommand("c " + fd.getDirectory() + fd.getFile());
    applet.dmgcpu.terminate = true;
   } else {
    new ModalDialog(this, "Error", "Load a ROM before executing a debugger script", "");
   }
  } else if (command.equals("Enter debugger")) {
   if (applet.dmgcpu != null) {
    applet.debuggerActive = true;
    applet.dmgcpu.terminate = true;
   } else {
    new ModalDialog(this, "Error", "Load a ROM before entering the debugger", "");
   }
  } else if (command.equals("1x")) {
   applet.dmgcpu.graphicsChip.setMagnify(1);
   setWindowSize(1);
   clearWindow();
  } else if (command.equals("2x")) {
   applet.dmgcpu.graphicsChip.setMagnify(2);
   setWindowSize(2);
   clearWindow();
  } else if (command.equals("3x")) {
   applet.dmgcpu.graphicsChip.setMagnify(3);
   setWindowSize(3);
   clearWindow();
  } else if (command.equals("4x")) {
   applet.dmgcpu.graphicsChip.setMagnify(4);
   setWindowSize(4);
   clearWindow();
  } else if (command.equals("Connect to client")) {
   makeConnectDialog();
  } else if (command.equals("Connect cancel")) {
   connectDialog.hide();
   connectDialog = null;
  } else if (command.equals("Connect ok")) {
   connectDialog.hide();
   connectDialog = null;
   applet.gameLink = new TCPGameLink(this, hostAddress.getText());
   if (applet.dmgcpu != null) {
    applet.dmgcpu.gameLink = applet.gameLink;
    applet.gameLink.setDmgcpu(applet.dmgcpu);
   }
  } else if (command.equals("Exit")) {
   applet.dispose();
   System.exit(0);
  } 
 }

 public void setColourScheme(String command) {
  if (applet.dmgcpu == null) {
   new ModalDialog(this, "Error", "Load a ROM before selecting", "a colour scheme.");
   for (int r = 0; r < JavaBoy.schemeNames.length; r++) {
    if (JavaBoy.schemeNames[r] == command) {
     schemes[r].setState(false);
    }
   }
  } else {
   for (int r = 0; r < JavaBoy.schemeNames.length; r++) {
    if (JavaBoy.schemeNames[r] == command) {
     applet.dmgcpu.graphicsChip.backgroundPalette.setColours(
      JavaBoy.schemeColours[r][0], JavaBoy.schemeColours[r][1],
      JavaBoy.schemeColours[r][2], JavaBoy.schemeColours[r][3]);

     applet.dmgcpu.graphicsChip.obj1Palette.setColours(
      JavaBoy.schemeColours[r][4], JavaBoy.schemeColours[r][5],
      JavaBoy.schemeColours[r][6], JavaBoy.schemeColours[r][7]);

     applet.dmgcpu.graphicsChip.obj2Palette.setColours(
      JavaBoy.schemeColours[r][8], JavaBoy.schemeColours[r][9],
      JavaBoy.schemeColours[r][10], JavaBoy.schemeColours[r][11]);
     applet.dmgcpu.graphicsChip.invalidateAll();
    } else {
     schemes[r].setState(false);
    }
   }
  }
 }

 public void itemStateChanged(ItemEvent e) {
  String command = (String) e.getItem();
  System.out.println(command);
  if (command.equals("Channel 1 (Square wave)")) {
   if (applet.dmgcpu != null) {
    applet.dmgcpu.soundChip.channel1Enable = soundChannel1Enable.getState();
   }
  } else if (command.equals("Channel 2 (Square wave)")) {
   if (applet.dmgcpu != null) {
    applet.dmgcpu.soundChip.channel2Enable = soundChannel2Enable.getState();
   }
  } else if (command.equals("Channel 3 (Voluntary wave)")) {
   if (applet.dmgcpu != null) {
    applet.dmgcpu.soundChip.channel3Enable = soundChannel3Enable.getState();
   }
  } else if (command.equals("Channel 4 (Noise)")) {
   if (applet.dmgcpu != null) {
    applet.dmgcpu.soundChip.channel4Enable = soundChannel4Enable.getState();
   }
  } else if (command.equals("Size: actual")) {
   viewSingle.setState(true);
   viewDouble.setState(false);
   viewTriple.setState(false);
   viewQuadrouple.setState(false);
   setMagnify();
   setWindowSize(1);
  } else if (command.equals("Size: 2x")) {
   viewSingle.setState(false);
   viewDouble.setState(true);
   viewTriple.setState(false);
   viewQuadrouple.setState(false);
   setMagnify();
   setWindowSize(2);
  } else if (command.equals("Size: 3x")) {
   viewSingle.setState(false);
   viewDouble.setState(false);
   viewTriple.setState(true);
   viewQuadrouple.setState(false);
   setMagnify();
   setWindowSize(3);
  } else if (command.equals("Size: 4x")) {
   viewSingle.setState(false);
   viewDouble.setState(false);
   viewTriple.setState(false);
   viewQuadrouple.setState(true);
   setMagnify();
   setWindowSize(4);
  } else if (command.equals("Sample rate: 11khz")) {
   soundFreq22.setState(false);
   soundFreq44.setState(false);
   soundFreq11.setState(true);
   setSoundFreq();
  } else if (command.equals("Sample rate: 22khz")) {
   soundFreq11.setState(false);
   soundFreq44.setState(false);
   soundFreq22.setState(true);
   setSoundFreq();
  } else if (command.equals("Frame skip: 0")) {
   viewFrameSkip0.setState(true);
   viewFrameSkip1.setState(false);
   viewFrameSkip2.setState(false);
   viewFrameSkip3.setState(false);
   viewFrameSkip4.setState(false);
   setFrameSkip();
  } else if (command.equals("Frame skip: 1")) {
   viewFrameSkip0.setState(false);
   viewFrameSkip1.setState(true);
   viewFrameSkip2.setState(false);
   viewFrameSkip3.setState(false);
   viewFrameSkip4.setState(false);
   setFrameSkip();
  } else if (command.equals("Frame skip: 2")) {
   viewFrameSkip0.setState(false);
   viewFrameSkip1.setState(false);
   viewFrameSkip2.setState(true);
   viewFrameSkip3.setState(false);
   viewFrameSkip4.setState(false);
   setFrameSkip();
  } else if (command.equals("Frame skip: 3")) {
   viewFrameSkip0.setState(false);
   viewFrameSkip1.setState(false);
   viewFrameSkip2.setState(false);
   viewFrameSkip3.setState(true);
   viewFrameSkip4.setState(false);
   setFrameSkip();
  } else if (command.equals("Frame skip: 4")) {
   viewFrameSkip0.setState(false);
   viewFrameSkip1.setState(false);
   viewFrameSkip2.setState(false);
   viewFrameSkip3.setState(false);
   viewFrameSkip4.setState(true);
   setFrameSkip();
  } else if (command.equals("Sample rate: 44khz")) {
   soundFreq11.setState(false);
   soundFreq22.setState(false);
   soundFreq44.setState(true);
   setSoundFreq();
  } else if (command.equals("Buffer length: 200ms")) {
   soundBuffer300.setState(false);
   soundBuffer400.setState(false);
   soundBuffer200.setState(true);
   setBufferLength();
  } else if (command.equals("Buffer length: 300ms")) {
   soundBuffer200.setState(false);
   soundBuffer400.setState(false);
   soundBuffer300.setState(true);
   setBufferLength();
  } else if (command.equals("Buffer length: 400ms")) {
   soundBuffer200.setState(false);
   soundBuffer300.setState(false);
   soundBuffer400.setState(true);
   setBufferLength();
  } else if (command.equals("Use Gameboy Color features")) {
   if (applet.dmgcpu != null) {
    applet.dmgcpu.allowGbcFeatures = !applet.dmgcpu.allowGbcFeatures;
   } else {
    fileGameboyColor.setState(!fileGameboyColor.getState());
   }
  } else if (command.equals("Allow connections")) {
   if (applet.gameLink == null) {
    applet.gameLink = new TCPGameLink(this);
    if (applet.gameLink.serverRunning) {
     networkServer.setState(true);
    } else {
     networkServer.setState(false);
     applet.gameLink = null;
    }
    if (applet.dmgcpu != null) {
     applet.dmgcpu.gameLink = applet.gameLink;
     applet.gameLink.setDmgcpu(applet.dmgcpu);
    }
   } else {
    applet.gameLink.shutDown();
    applet.gameLink = null;
    if (applet.dmgcpu != null) applet.dmgcpu.gameLink = null;
   }
  } else if (command.equals("Emulate printer")) {
   if (networkPrinter.getState()) {
    if (applet.gameLink != null) {
     applet.gameLink.shutDown();
	 networkServer.setState(false);
	}
    applet.gameLink = new GameBoyPrinter();
	applet.gameLink.setDmgcpu(applet.dmgcpu);
	applet.dmgcpu.gameLink = applet.gameLink;
   } else {
    applet.gameLink.shutDown();
    applet.gameLink = null;
   }
  } else {
   setColourScheme(command);
  }

 }

 public void paint(Graphics g) {
  if (graphicsChip != null) {
   Dimension d = getSize();
   int x = (d.width / 2) - (graphicsChip.width / 2);
   int y = (d.height / 2) - (graphicsChip.height / 2);
   boolean b = graphicsChip.draw(g, x, y + 20, this);
   if (viewFrameCounter.getState()) {
    g.setColor(new Color(255, 255, 255));
    g.fillRect(0, d.height - 20, d.width, 20);
    g.setColor(new Color(0, 0, 0));
    g.drawString(graphicsChip.getFPS() + " frames per second", 10, d.height - 7);
   }
  }
 }
}

