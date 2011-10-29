// Mahjong Quest - Value set at 045E

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

/** This is the main controlling class for the emulation
 *  It contains the code to emulate the Z80-like processor
 *  found in the Gameboy, and code to provide the locations
 *  in CPU address space that points to the correct area of
 *  ROM/RAM/IO.
 */
class Dmgcpu {
 /** Registers: 8-bit */
 int a, b, c, d, e, f;
 /** Registers: 16-bit */
 public int sp, pc, hl;

 /** The number of instructions that have been executed since the
  *  last reset
  */
 int instrCount = 0;

 boolean interruptsEnabled = false;

 /** Used to implement the IE delay slot */
 int ieDelay = -1;

 boolean timaEnabled = false;
 int instrsPerTima = 6000;

 /** TRUE when the CPU is currently processing an interrupt */
 boolean inInterrupt = false;

 /** Enable the breakpoint flag.  As breakpoint instruction is used in some games, this is used to skip over it unless the breakpoint is actually in use */
 boolean breakpointEnable = false;

 // Constants for flags register

 /** Zero flag */
 final short F_ZERO =      0x80;
 /** Subtract/negative flag */
 final short F_SUBTRACT =  0x40;
 /** Half carry flag */
 final short F_HALFCARRY = 0x20;
 /** Carry flag */
 final short F_CARRY =     0x10;

 final short INSTRS_PER_VBLANK = 9000; /* 10000  */

 /** Used to set the speed of the emulator.  This controls how
  *  many instructions are executed for each horizontal line scanned
  *  on the screen.  Multiply by 154 to find out how many instructions
  *  per frame.
  */
 final short BASE_INSTRS_PER_HBLANK = 60;    /* 60    */
 short INSTRS_PER_HBLANK = BASE_INSTRS_PER_HBLANK;

 /** Used to set the speed of DIV increments */
 final short BASE_INSTRS_PER_DIV    = 33;    /* 33    */
 short INSTRS_PER_DIV = BASE_INSTRS_PER_DIV;

 // Constants for interrupts

 /** Vertical blank interrupt */
 public final short INT_VBLANK =  0x01;

 /** LCD Coincidence interrupt */
 public final short INT_LCDC =    0x02;
         
 /** TIMA (programmable timer) interrupt */
 public final short INT_TIMA =    0x04;

 /** Serial interrupt */
 public final short INT_SER =     0x08;

 /** P10 - P13 (Joypad) interrupt */
 public final short INT_P10 =     0x10;

 String[] registerNames =
     {"B", "C", "D", "E", "H", "L", "(HL)", "A"};
 String[] aluOperations =
     {"ADD", "ADC", "SUB", "SBC", "AND", "XOR", "OR", "CP"};
 String[] shiftOperations =
     {"RLC", "RRC", "RL", "RR", "SLA", "SRA", "SWAP", "SRL"};

 // 8Kb main system RAM appears at 0xC000 in address space
 // 32Kb for GBC
 byte[] mainRam = new byte[0x8000];

 // 256 bytes at top of RAM are used mainly for registers
 byte[] oam = new byte[0x100];

 Cartridge cartridge;
 GraphicsChip graphicsChip;
 SoundChip soundChip;
 GameLink gameLink;
 IoHandler ioHandler;
 Component applet;
 boolean terminate;
 boolean running = false;

 boolean gbcFeatures = true;
 boolean allowGbcFeatures = true;
 int gbcRamBank = 1;

 /** Create a CPU emulator with the supplied cartridge and game link objects.  Both can be set up
  *  or changed later if needed
  */
 public Dmgcpu(Cartridge c, GameLink l, Component a) {
  cartridge = c;
  gameLink = l;
  if (gameLink != null) gameLink.setDmgcpu(this);
  graphicsChip = new TileBasedGraphicsChip(a, this);
  checkEnableGbc();
  boolean java1point3 = true;

  String version = System.getProperty("java.version");

  // Sound not supported until Java 1.2
  java1point3 = !( (version.startsWith("1.0") || version.startsWith("1.1")) );

  if (java1point3) {
    soundChip = new SoundChip();
  }
  ioHandler = new IoHandler(this);
  applet = a;
//  reset();
 }

 /** Clear up memory */
 public void dispose() {
  graphicsChip.dispose();
 }

 /** Force the execution thread to stop and return to it's caller */
 public void terminateProcess() {
  terminate = true;
/*  do {
   try {
    java.lang.Thread.sleep(100);
    System.out.println("Wating for CPU...");
   } catch (InterruptedException e) {
    // Nothing
   }
  } while (running);*/
 }

 /** Perform a CPU address space read.  This maps all the relevant objects into the correct parts of
  *  the memory
  */
 public final short addressRead(int addr) {

/*  if ((addr >= 0xDFD8) && (addr <= 0xDFF0) && (running)) {
   System.out.println(JavaBoy.hexWord(addr) + " read at " + JavaBoy.hexWord(pc) + " bank " + cartridge.currentBank);
  }*/

/*  if ((addr < 0) || (addr > 65535)) {
    System.out.println("Tried to read address " + addr + ".  pc = " + JavaBoy.hexWord(pc));
    return 0xFF;
  }*/

  addr = addr & 0xFFFF;

  switch ((addr & 0xF000)) {
   case 0x0000 :
   case 0x1000 :
   case 0x2000 :
   case 0x3000 :
   case 0x4000 :
   case 0x5000 :
   case 0x6000 :
   case 0x7000 :
    return cartridge.addressRead(addr);

   case 0x8000 :
   case 0x9000 :
    return graphicsChip.addressRead(addr - 0x8000);

   case 0xA000 :
   case 0xB000 :
    return cartridge.addressRead(addr);

   case 0xC000 :
    return (mainRam[addr - 0xC000]);

   case 0xD000 :
    return (mainRam[addr - 0xD000 + (gbcRamBank * 0x1000)]);

   case 0xE000 :
    return mainRam[addr - 0xE000];
     /* (short) (mainRam[addr - 0xE000] & 0x00FF); */
    
   case 0xF000 :
    if (addr < 0xFE00) {
     return mainRam[addr - 0xE000];
    } else if (addr < 0xFF00) {
     return (short) (oam[addr - 0xFE00] & 0x00FF);
    } else {
     return ioHandler.ioRead(addr - 0xFF00);
    }

   default:
    System.out.println("Tried to read address " + addr + ".  pc = " + JavaBoy.hexWord(pc));
    return 0xFF;
  }

 }

 /** Performs a CPU address space write.  Maps all of the relevant object into the right parts of
  *  memory.
  */
 public final void addressWrite(int addr, int data) {

/*  if ((addr >= 0xCFF8) && (addr <= 0xCFF8) && (running)) {
   System.out.println(JavaBoy.hexWord(data) + " written to " + JavaBoy.hexWord(addr) + " at " + JavaBoy.hexWord(pc) + " bank " + cartridge.currentBank);
  }
  if ((addr >= 0xEFF8) && (addr <= 0xEFF8) && (running)) {
   System.out.println(JavaBoy.hexWord(data) + " written to " + JavaBoy.hexWord(addr) + " at " + JavaBoy.hexWord(pc) + " bank " + cartridge.currentBank);
  }*/

/*  if ((addr < 0) || (addr > 65535)) {
   System.out.println(JavaBoy.hexWord(data) + " written to " + JavaBoy.hexWord(addr) + " at " + JavaBoy.hexWord(pc) + " bank " + cartridge.currentBank);
  }*/

  switch (addr & 0xF000) {
   case 0x0000 :
   case 0x1000 :
   case 0x2000 :
   case 0x3000 :
   case 0x4000 :
   case 0x5000 :
   case 0x6000 :
   case 0x7000 :
    if (!running) {
     cartridge.debuggerAddressWrite(addr, data);
    } else {
     cartridge.addressWrite(addr, data);
 //    System.out.println("Tried to write to ROM! PC = " + JavaBoy.hexWord(pc) + ", Data = " + JavaBoy.hexByte(JavaBoy.unsign((byte) data)));
    }
    break;

   case 0x8000 :
   case 0x9000 :
    graphicsChip.addressWrite(addr - 0x8000, (byte) data);
    break;

   case 0xA000 :
   case 0xB000 :
    cartridge.addressWrite(addr, data);
    break;

   case 0xC000 :
    mainRam[addr - 0xC000] = (byte) data;
    break;

   case 0xD000 :
    mainRam[addr - 0xD000 + (gbcRamBank * 0x1000)] = (byte) data;
    break;

   case 0xE000 :
    mainRam[addr - 0xE000] = (byte) data;
    break;

   case 0xF000 :
    if (addr < 0xFE00) {
     try {
      mainRam[addr - 0xE000] = (byte) data;
     } catch (ArrayIndexOutOfBoundsException e) {
      System.out.println("Address error: " + addr + " pc = " + JavaBoy.hexWord(pc));
     }
    } else if (addr < 0xFF00) {
     oam[addr - 0xFE00] = (byte) data;
    } else {
     ioHandler.ioWrite(addr - 0xFF00, (short) data);
    }
    break;
  }


 }


 public final void addressWriteOld(int addr, int data) {

/*  if ((addr >= 0xFFA4) && (addr <= 0xFFA5) && (running)) {
   System.out.println(JavaBoy.hexWord(addr) + " written at " + JavaBoy.hexWord(pc) + " bank " + cartridge.currentBank);
  }*/


//  System.out.print(JavaBoy.hexByte(JavaBoy.unsign((short) data)) + " --> " + JavaBoy.hexWord(addr) + ", ");
  if ((addr < 0x8000)) {
   if (!running) {
    cartridge.debuggerAddressWrite(addr, data);
   } else {
    cartridge.addressWrite(addr, data);
//    System.out.println("Tried to write to ROM! PC = " + JavaBoy.hexWord(pc) + ", Data = " + JavaBoy.hexByte(JavaBoy.unsign((byte) data)));
   }
  } else if (addr < 0xA000) {
   try {
    graphicsChip.addressWrite(addr - 0x8000, (byte) data);
   } catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("Error address " + addr);
   }
  } else if (addr < 0xC000) {
   // RAM Bank write
//   System.out.println("RAM bank write! + " + JavaBoy.hexWord(addr) + " = " + JavaBoy.hexByte(data) + " at " + JavaBoy.hexWord(pc));
   cartridge.addressWrite(addr, data);
  } else if (addr < 0xE000) {
   mainRam[addr - 0xC000] = (byte) data;
  } else if (addr < 0xFE00) {
   mainRam[addr - 0xE000] = (byte) data;
  } else if (addr < 0xFF00) {
   oam[addr - 0xFE00] = (byte) data;
  } else if (addr <= 0xFFFF) {
   if (addr == 0xFF80) {
//    System.out.println("Register write: " + JavaBoy.hexWord(addr) + " = " + JavaBoy.hexWord(data));
   }
   ioHandler.ioWrite(addr - 0xFF00, (short) data);
//   registers[addr - 0xFF00] = (byte) data;
  } else {
   System.out.println("Attempt to write to address "+ JavaBoy.hexWord(addr));
  }
 }

 /** Sets the value of a register by it's name */
 public boolean setRegister(String reg, int value) {
  if (reg.equals("a") || reg.equals("acc")) {
   a = (short) value;
  } else if (reg.equals("b")) {
   b = (short) value;
  } else if (reg.equals("c")) {
   c = (short) value;
  } else if (reg.equals("d")) {
   d = (short) value;
  } else if (reg.equals("e")) {
   e = (short) value;
  } else if (reg.equals("f")) {
   f = (short) value;
  } else if (reg.equals("h")) {
   hl = (hl & 0x00FF) | (value << 8);
  } else if (reg.equals("l")) {
   hl = (hl & 0xFF00) | value;
  } else if (reg.equals("sp")) {
   sp = value;
  } else if (reg.equals("pc") || reg.equals("ip")) {
   pc = value;                             
  } else if (reg.equals("bc")) {
   b = (short) (value >> 8);
   c = (short) (value & 0x00FF);
  } else if (reg.equals("de")) {
   d = (short) (value >> 8);
   e = (short) (value & 0x00FF);
  } else if (reg.equals("hl")) {
   hl = value;
  } else {
   return false;
  }
  return true;
 }

 public void setBC(int value) {
  b = (short) ((value & 0xFF00) >> 8);
  c = (short) (value & 0x00FF);
 }

 public void setDE(int value) {
  d = (short) ((value & 0xFF00) >> 8);
  e = (short) (value & 0x00FF);
 }

 public void setHL(int value) {
  hl = value;
 }

 /** Performs a read of a register by internal register number */
 public final int registerRead(int regNum) {
  switch (regNum) {
   case 0  : return b;
   case 1  : return c;
   case 2  : return d;
   case 3  : return e;
   case 4  : return (short) ((hl & 0xFF00) >> 8);
   case 5  : return (short) (hl & 0x00FF);
   case 6  : return JavaBoy.unsign(addressRead(hl));
   case 7  : return a;
   default : return -1;
  }
 }

 /** Performs a write of a register by internal register number */
 public final void registerWrite(int regNum, int data) {
  switch (regNum) {
   case 0  : b = (short) data;
             return;
   case 1  : c = (short) data;
             return;
   case 2  : d = (short) data;
             return;
   case 3  : e = (short) data;
             return;
   case 4  : hl = (hl & 0x00FF) | (data << 8);
             return;
   case 5  : hl = (hl & 0xFF00) | data;
             return;
   case 6  : addressWrite(hl, data);
             return;
   case 7  : a = (short) data;
             return;
   default : return;
  }
 }

 public void checkEnableGbc() {
  if ( ((cartridge.rom[0x143] & 0x80) == 0x80) && (allowGbcFeatures)) { // GBC Cartridge ID
   gbcFeatures = true;
  } else {
   gbcFeatures = false;
  }
 }


 /** Resets the CPU to it's power on state.  Memory contents are not cleared. */
 public void reset() {

  checkEnableGbc();
  setDoubleSpeedCpu(false);
  graphicsChip.dispose();
  cartridge.reset();
  interruptsEnabled = false;
  ieDelay = -1;
  pc = 0x0100;
  sp = 0xFFFE;
  f = 0xB0;
  gbcRamBank = 1;
  instrCount = 0;

  if (gbcFeatures) {
   a = 0x11;
  } else {
   a = 0x01;
  }

  for (int r = 0; r < 0x8000; r++) {
   mainRam[r] = 0;
  }

  setBC(0x0013);
  setDE(0x00D8);
  setHL(0x014D);
  JavaBoy.debugLog("CPU reset");

  ioHandler.reset();
//  pc = 0x0100;
 }

 public void setDoubleSpeedCpu(boolean enabled) {

  if (enabled) {
   INSTRS_PER_HBLANK = BASE_INSTRS_PER_HBLANK * 2;
   INSTRS_PER_DIV = BASE_INSTRS_PER_DIV * 2;
  } else {
   INSTRS_PER_HBLANK = BASE_INSTRS_PER_HBLANK;
   INSTRS_PER_DIV = BASE_INSTRS_PER_DIV;
  }

 }

 /** If an interrupt is enabled an the interrupt register shows that it has occured, jump to
  *  the relevant interrupt vector address
  */
 public final void checkInterrupts() {
  int intFlags = ioHandler.registers[0x0F];
  int ieReg = ioHandler.registers[0xFF];
  if ((intFlags & ieReg) != 0) {
   sp -= 2;
   addressWrite(sp + 1, pc >> 8);  // Push current program counter onto stack
   addressWrite(sp, pc & 0x00FF);
   interruptsEnabled = false;

   if ((intFlags & ieReg & INT_VBLANK) != 0) {
    pc = 0x40;                      // Jump to Vblank interrupt address
    intFlags -= INT_VBLANK;
//    System.out.println("VBLANK Interrupt called");
   } else if ((intFlags & ieReg & INT_LCDC) != 0) {
    pc = 0x48;
    intFlags -= INT_LCDC;
//    System.out.println("LCDC Interrupt called");
   } else if ((intFlags & ieReg & INT_TIMA) != 0) {
    pc = 0x50;
    intFlags -= INT_TIMA;
//    System.out.println("TIMA Interrupt called");
   } else if ((intFlags & ieReg & INT_SER) != 0) {
    pc = 0x58;
    intFlags -= INT_SER;
//    System.out.println("TIMA Interrupt called");
   } else if ((intFlags & ieReg & INT_P10) != 0) {	// Joypad interrupt
    pc = 0x60;
    intFlags -= INT_P10;
//	System.out.println("Joypad int.");
   } /* Other interrupts go here, not done yet */

   ioHandler.registers[0x0F] = (byte) intFlags;
   inInterrupt = true;
  }
 }

 /** Initiate an interrupt of the specified type */
 public final void triggerInterrupt(int intr) {
  ioHandler.registers[0x0F] |= intr;
//  System.out.println("Triggered:" + intr);
 }

 public final void triggerInterruptIfEnabled(int intr) {
  if ((ioHandler.registers[0xFF] & (short) (intr)) != 0) ioHandler.registers[0x0F] |= intr;
//  System.out.println("Triggered:" + intr);
 }

 /** Check for interrupts that need to be initiated */
 public final void initiateInterrupts() {
   if (timaEnabled && ((instrCount % instrsPerTima) == 0)) {
    if (JavaBoy.unsign(ioHandler.registers[05]) == 0) {
     ioHandler.registers[05] = ioHandler.registers[06]; // Set TIMA modulo
     if ((ioHandler.registers[0xFF] & INT_TIMA) != 0)
      triggerInterrupt(INT_TIMA);
    }
    ioHandler.registers[05]++;
   }

   if ((instrCount % INSTRS_PER_DIV) == 0) {
    ioHandler.registers[04]++;
   }

   if ((instrCount % INSTRS_PER_HBLANK) == 0) {


    // LCY Coincidence
	// The +1 is due to the LCY register being just about to be incremented
	int cline = JavaBoy.unsign(ioHandler.registers[0x44]) + 1;
	if (cline == 152) cline = 0;

    if (((ioHandler.registers[0xFF] & INT_LCDC) != 0) &&
	     ((ioHandler.registers[0x41] & 64) != 0) &&
       (JavaBoy.unsign(ioHandler.registers[0x45]) == cline) && ((ioHandler.registers[0x40] & 0x80) != 0) && (cline < 0x90)) {
//    System.out.println("Hblank " + cline);
//	 System.out.println("** LCDC Int **");
     triggerInterrupt(INT_LCDC);
    }

	// Trigger on every line
    if (((ioHandler.registers[0xFF] & INT_LCDC) != 0) &&
	     ((ioHandler.registers[0x41] & 0x8) != 0) && ((ioHandler.registers[0x40] & 0x80) != 0) && (cline < 0x90) ) {
//	 System.out.println("** LCDC Int **");
     triggerInterrupt(INT_LCDC);
    }
	
	

    if ((gbcFeatures) && (ioHandler.hdmaRunning)) {
     ioHandler.performHdma();
    }

    if (JavaBoy.unsign(ioHandler.registers[0x44]) == 143) {
//     System.out.println("VBLANK!");
     for (int r = 144; r < 170; r++) {
      graphicsChip.notifyScanline(r);
     } 
     if ( ((ioHandler.registers[0x40] & 0x80) != 0) && ((ioHandler.registers[0xFF] & INT_VBLANK) != 0) ) {
      triggerInterrupt(INT_VBLANK);
	  if ( ((ioHandler.registers[0x41] & 16) != 0) && ((ioHandler.registers[0xFF] & INT_LCDC) != 0) ) {
       triggerInterrupt(INT_LCDC);
//	   System.out.println("VBlank LCDC!");
	  }
     }

     boolean speedThrottle = true;
     if (!JavaBoy.runningAsApplet) {
      GameBoyScreen g = (GameBoyScreen) applet;
      speedThrottle = g.viewSpeedThrottle.getState();
     }
     if ((speedThrottle) && (graphicsChip.frameWaitTime >= 0)) {
//      System.out.println("Waiting for " + graphicsChip.frameWaitTime + "ms.");
      try {
       java.lang.Thread.sleep(graphicsChip.frameWaitTime);
      } catch (InterruptedException e) {
       // Nothing.
      }
     }

     
    }


	graphicsChip.notifyScanline(JavaBoy.unsign(ioHandler.registers[0x44]));
    ioHandler.registers[0x44] = (byte) (JavaBoy.unsign(ioHandler.registers[0x44]) + 1);
//	System.out.println("Reg 44 = " + JavaBoy.unsign(ioHandler.registers[0x44]));

    if (JavaBoy.unsign(ioHandler.registers[0x44]) >= 153) {
//     System.out.println("VBlank");

     ioHandler.registers[0x44] = 0;
     if (soundChip != null) soundChip.outputSound();
     graphicsChip.frameDone = false;
	 if (JavaBoy.runningAsApplet) {
      ((JavaBoy) (applet)).drawNextFrame();
	 } else {
      ((GameBoyScreen) (applet)).repaint();
	 }
     try {
      while (!graphicsChip.frameDone) {
       java.lang.Thread.sleep(1);
      }
     } catch (InterruptedException e) {
      // Nothing.
     }
      

//     System.out.println("LCDC reset");
    }
   }
 }

 /** Execute the specified number of Gameboy instructions.  Use '-1' to execute forever */
 public final void execute(int numInstr) {

  terminate = false;
  short newf;
  int dat;
  running = true;
  graphicsChip.startTime = System.currentTimeMillis();
  int b1, b2, b3, offset;

  for (int r = 0; (r != numInstr) && (!terminate); r++) {

/*   GameBoyScreen j = (GameBoyScreen) applet;
   if (j.viewFrameCounter.getState()) {
    System.out.print(" " + JavaBoy.hexWord(pc) + ":" + JavaBoy.hexByte(cartridge.currentBank));
   }*/
//   System.out.print(" " + JavaBoy.hexWord(pc) + ":" + JavaBoy.hexByte(cartridge.currentBank));
   instrCount++;

   b1 = JavaBoy.unsign(addressRead(pc));
   offset = addressRead(pc + 1);
   b3 = JavaBoy.unsign(addressRead(pc + 2));
   b2 = JavaBoy.unsign((short) offset);

   switch (b1) {
    case 0x00 :               // NOP
        pc++;
        break;
    case 0x01 :               // LD BC, nn
        pc+=3;
        b = b3;
        c = b2;
        break;
    case 0x02 :               // LD (BC), A
        pc++;
        addressWrite((b << 8) | c, a);
        break;
    case 0x03 :               // INC BC
        pc++;
        c++;
        if (c == 0x0100) {
         b++;
         c = 0;
         if (b == 0x0100) {
          b = 0;
         }
        }
        break;
    case 0x04 :               // INC B
        pc++;
        f &= F_CARRY;
        switch (b) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    b = 0x00;
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    b = 0x10;
                    break;
         default:   b++;
                    break;
        }
        break;
    case 0x05 :               // DEC B
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        switch (b) {
         case 0x00: f |= F_HALFCARRY;
                    b = 0xFF;
                    break;
         case 0x10: f |= F_HALFCARRY;
                    b = 0x0F;
                    break;
         case 0x01: f |= F_ZERO;
                    b = 0x00;
                    break;
         default:   b--;
                    break;
        }
        break;
    case 0x06 :               // LD B, nn
        pc += 2;
        b = b2;
        break;
    case 0x07 :               // RLC A
        pc++;
        f = 0;

        a <<= 1;

        if ((a & 0x0100) != 0) {
         f |= F_CARRY;
         a |= 1;
         a &= 0xFF;
        }
        if (a == 0) {
         f |= F_ZERO;
        }
        break;
    case 0x08 :               // LD (nnnn), SP   /* **** May be wrong! **** */
        pc+=3;
        addressWrite((b3 << 8) + b2 + 1, (sp & 0xFF00) >> 8);
        addressWrite((b3 << 8) + b2, (sp & 0x00FF));
        break;
    case 0x09 :               // ADD HL, BC
        pc++;
        hl = (hl + ((b << 8) + c));
        if ((hl & 0xFFFF0000) != 0) {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)) | (F_CARRY));
         hl &= 0xFFFF;
        } else {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)));
        }
        break;
    case 0x0A :               // LD A, (BC)
        pc++;
        a = JavaBoy.unsign(addressRead((b << 8) + c));
        break;
    case 0x0B :               // DEC BC
        pc++;
        c--;
        if ((c & 0xFF00) != 0) {
         c = 0xFF;
         b--;
         if ((b & 0xFF00) != 0) {
          b = 0xFF;
         }
        }
        break;
    case 0x0C :               // INC C
        pc++;
        f &= F_CARRY;
        switch (c) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    c = 0x00;
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    c = 0x10;
                    break;
         default:   c++;
                    break;
        }
        break;
    case 0x0D :               // DEC C
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        switch (c) {
         case 0x00: f |= F_HALFCARRY;
                    c = 0xFF;
                    break;
         case 0x10: f |= F_HALFCARRY;
                    c = 0x0F;
                    break;
         case 0x01: f |= F_ZERO;
                    c = 0x00;
                    break;
         default:   c--;
                    break;
        }
        break;
    case 0x0E :               // LD C, nn
        pc+=2;
        c = b2;
        break;        
    case 0x0F :               // RRC A
        pc++;
        if ((a & 0x01) == 0x01) {
         f = F_CARRY;
        } else {
         f = 0;
        }
        a >>= 1;
        if ((f & F_CARRY) == F_CARRY) {
         a |= 0x80;
        }
        if (a == 0) {
         f |= F_ZERO;
        }
        break;
    case 0x10 :               // STOP
        pc+=2;

        if (gbcFeatures) {
         if ((ioHandler.registers[0x4D] & 0x01) == 1) {
          int newKey1Reg = ioHandler.registers[0x4D] & 0xFE;
          if ((newKey1Reg & 0x80) == 0x80) {
           setDoubleSpeedCpu(false);
           newKey1Reg &= 0x7F;
          } else {
           setDoubleSpeedCpu(true);
           newKey1Reg |= 0x80;
//           System.out.println("CAUTION: Game uses double speed CPU, humoungus PC required!");
          }
          ioHandler.registers[0x4D] = (byte) newKey1Reg;
         }
        }

//        terminate = true;
//        System.out.println("- Breakpoint reached");
        break;
    case 0x11 :               // LD DE, nnnn
        pc+=3;
        d = b3;
        e = b2;
        break;
    case 0x12 :               // LD (DE), A
        pc++;
        addressWrite((d << 8) + e, a);
        break;
    case 0x13 :               // INC DE
        pc++;
        e++;
        if (e == 0x0100) {
         d++;
         e = 0;
         if (d == 0x0100) {
          d = 0;
         }
        }
        break;
    case 0x14 :               // INC D
        pc++;
        f &= F_CARRY;
        switch (d) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    d = 0x00;
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    d = 0x10;
                    break;
         default:   d++;
                    break;
        }
        break;
    case 0x15 :               // DEC D
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        switch (d) {
         case 0x00: f |= F_HALFCARRY;
                    d = 0xFF;
                    break;
         case 0x10: f |= F_HALFCARRY;
                    d = 0x0F;
                    break;
         case 0x01: f |= F_ZERO;
                    d = 0x00;
                    break;
         default:   d--;
                    break;
        }
        break;
    case 0x16 :               // LD D, nn
        pc += 2;
        d = b2;
        break;
    case 0x17 :               // RL A
        pc++;
        if ((a & 0x80) == 0x80) {
         newf = F_CARRY;
        } else {
         newf = 0;
        }
        a <<= 1;
          
        if ((f & F_CARRY) == F_CARRY) {
         a |= 1;
        }

        a &= 0xFF;
        if (a == 0) {
         newf |= F_ZERO;
        }
        f = newf;
        break;
    case 0x18 :               // JR nn
        pc += 2 + offset;
        break;
    case 0x19 :               // ADD HL, DE
        pc++;
        hl = (hl + ((d << 8) + e));
        if ((hl & 0xFFFF0000) != 0) {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)) | (F_CARRY));
         hl &= 0xFFFF;
        } else {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)));
        }
        break;
    case 0x1A :               // LD A, (DE)
        pc++;
        a = JavaBoy.unsign(addressRead((d << 8) + e));
        break;
    case 0x1B :               // DEC DE
        pc++;
        e--;
        if ((e & 0xFF00) != 0) {
         e = 0xFF;
         d--;
         if ((d & 0xFF00) != 0) {
          d = 0xFF;
         }
        }
        break;
    case 0x1C :               // INC E
        pc++;
        f &= F_CARRY;
        switch (e) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    e = 0x00;
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    e = 0x10;
                    break;
         default:   e++;
                    break;
        }
        break;
    case 0x1D :               // DEC E
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        switch (e) {
         case 0x00: f |= F_HALFCARRY;
                    e = 0xFF;
                    break;
         case 0x10: f |= F_HALFCARRY;
                    e = 0x0F;
                    break;
         case 0x01: f |= F_ZERO;
                    e = 0x00;
                    break;
         default:   e--;
                    break;
        }
        break;
    case 0x1E :               // LD E, nn
        pc+=2;
        e = b2;
        break;
    case 0x1F :               // RR A
        pc++;
        if ((a & 0x01) == 0x01) {
         newf = F_CARRY;
        } else {
         newf = 0;
        }
        a >>= 1;
  
        if ((f & F_CARRY) == F_CARRY) {
         a |= 0x80;
        }
      
        if (a == 0) {
         newf |= F_ZERO;
        }
        f = newf;
        break;
    case 0x20 :               // JR NZ, nn
        if ((f & 0x80) == 0x00) {
         pc += 2 + offset;
        } else {
         pc += 2;
        }
        break;
    case 0x21 :               // LD HL, nnnn
        pc += 3;
        hl = (b3 << 8) + b2;
        break;
    case 0x22 :               // LD (HL+), A
        pc++;
        addressWrite(hl, a);
        hl = (hl + 1) & 0xFFFF;
        break;
    case 0x23 :               // INC HL
        pc++;
        hl = (hl + 1) & 0xFFFF;
        break;
    case 0x24 :               // INC H         ** May be wrong **
        pc++;
        f &= F_CARRY;
        switch ((hl & 0xFF00) >> 8) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    hl = (hl & 0x00FF);
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    hl = (hl & 0x00FF) | 0x10;
                    break;
         default:   hl = (hl + 0x0100);
                    break;
        }
        break;
    case 0x25 :               // DEC H           ** May be wrong **
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        switch ((hl & 0xFF00) >> 8) {
         case 0x00: f |= F_HALFCARRY;
                    hl = (hl & 0x00FF) | (0xFF00);
                    break;
         case 0x10: f |= F_HALFCARRY;
                    hl = (hl & 0x00FF) | (0x0F00);
                    break;
         case 0x01: f |= F_ZERO;
                    hl = (hl & 0x00FF);
                    break;
         default:   hl = (hl & 0x00FF) | ((hl & 0xFF00) - 0x0100);
                    break;
        }
        break;
    case 0x26 :               // LD H, nn
        pc+=2;
        hl = (hl & 0x00FF) | (b2 << 8);
        break;
    case 0x27 :               // DAA         ** This could be wrong! **
        pc++;

        int upperNibble = (a & 0xF0) >> 4;
        int lowerNibble = a & 0x0F;

//        System.out.println("Daa at " + JavaBoy.hexWord(pc));

        newf = (short) (f & F_SUBTRACT);

        if ((f & F_SUBTRACT) == 0) {

         if ((f & F_CARRY) == 0) {
          if ((upperNibble <= 8) && (lowerNibble >= 0xA) &&
             ((f & F_HALFCARRY) == 0)) {
           a += 0x06;
          }

          if ((upperNibble <= 9) && (lowerNibble <= 0x3) &&
             ((f & F_HALFCARRY) == F_HALFCARRY)) {
           a += 0x06;
          }

          if ((upperNibble >= 0xA) && (lowerNibble <= 0x9) &&
             ((f & F_HALFCARRY) == 0)) {
           a += 0x60;
           newf |= F_CARRY;
          }

          if ((upperNibble >= 0x9) && (lowerNibble >= 0xA) &&
             ((f & F_HALFCARRY) == 0)) {
           a += 0x66;
           newf |= F_CARRY;
          }

          if ((upperNibble >= 0xA) && (lowerNibble <= 0x3) &&
             ((f & F_HALFCARRY) == F_HALFCARRY)) {
           a += 0x66;
           newf |= F_CARRY;
          }

         } else {  // If carry set

          if ((upperNibble <= 0x2) && (lowerNibble <= 0x9) &&
             ((f & F_HALFCARRY) == 0)) {
           a += 0x60;
           newf |= F_CARRY;
          }

          if ((upperNibble <= 0x2) && (lowerNibble >= 0xA) &&
             ((f & F_HALFCARRY) == 0)) {
           a += 0x66;
           newf |= F_CARRY;
          }

          if ((upperNibble <= 0x3) && (lowerNibble <= 0x3) &&
             ((f & F_HALFCARRY) == F_HALFCARRY)) {
           a += 0x66;
           newf |= F_CARRY;
          }

         }

        } else { // Subtract is set

         if ((f & F_CARRY) == 0) {

          if ((upperNibble <= 0x8) && (lowerNibble >= 0x6) &&
             ((f & F_HALFCARRY) == F_HALFCARRY)) {
           a += 0xFA;
          }

         } else { // Carry is set

          if ((upperNibble >= 0x7) && (lowerNibble <= 0x9) &&
             ((f & F_HALFCARRY) == 0)) {
           a += 0xA0;
           newf |= F_CARRY;
          }

          if ((upperNibble >= 0x6) && (lowerNibble >= 0x6) &&
             ((f & F_HALFCARRY) == F_HALFCARRY)) {
           a += 0x9A;
           newf |= F_CARRY;
          }

         }

        }

        a &= 0x00FF;
        if (a == 0) newf |= F_ZERO;

        f = newf;

        break;
    case 0x28 :               // JR Z, nn
        if ((f & F_ZERO) == F_ZERO) {
         pc += 2 + offset;
        } else {
         pc += 2;
        }
        break;
    case 0x29 :               // ADD HL, HL
        pc++;
        hl = (hl + hl);
        if ((hl & 0xFFFF0000) != 0) {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)) | (F_CARRY));
         hl &= 0xFFFF;
        } else {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)));
        }
        break;
    case 0x2A :               // LDI A, (HL)
        pc++;                    
        a = JavaBoy.unsign(addressRead(hl));
        hl++;
        break;
    case 0x2B :               // DEC HL
        pc++;
        if (hl == 0) {
         hl = 0xFFFF;
        } else {
         hl--;
        }
        break;
    case 0x2C :               // INC L
        pc++;
        f &= F_CARRY;
        switch (hl & 0x00FF) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    hl = hl & 0xFF00;
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    hl++;
                    break;
         default:   hl++;
                    break;
        }
        break;
    case 0x2D :               // DEC L
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        switch (hl & 0x00FF) {
         case 0x00: f |= F_HALFCARRY;
                    hl = (hl & 0xFF00) | 0x00FF;
                    break;
         case 0x10: f |= F_HALFCARRY;
                    hl = (hl & 0xFF00) | 0x000F;
                    break;
         case 0x01: f |= F_ZERO;
                    hl = (hl & 0xFF00);
                    break;
         default:   hl = (hl & 0xFF00) | ((hl & 0x00FF) - 1);
                    break;
        }
        break;
    case 0x2E :               // LD L, nn
        pc+=2;
        hl = (hl & 0xFF00) | b2;
        break;
    case 0x2F :               // CPL A
        pc++;
        short mask = 0x80;
/*        short result = 0;
        for (int n = 0; n < 8; n++) {
         if ((a & mask) == 0) {
          result |= mask;
         } else {
         }
         mask >>= 1;
        }*/
        a = (short) ((~a) & 0x00FF);
        f = (short) ((f & (F_CARRY | F_ZERO)) | F_SUBTRACT | F_HALFCARRY);
        break;
    case 0x30 :               // JR NC, nn
        if ((f & F_CARRY) == 0) {
         pc += 2 + offset;
        } else {
         pc += 2;
        }
        break;
    case 0x31 :               // LD SP, nnnn
        pc += 3;
        sp = (b3 << 8) + b2;
        break;
    case 0x32 :
        pc++;
        addressWrite(hl, a);  // LD (HL-), A
        hl--;
        break;
    case 0x33 :               // INC SP
        pc++;
        sp = (sp + 1) & 0xFFFF;
        break;
    case 0x34 :               // INC (HL)
        pc++;
        f &= F_CARRY;
        dat = JavaBoy.unsign(addressRead(hl));
        switch (dat) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    addressWrite(hl, 0x00);
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    addressWrite(hl, 0x10);
                    break;
         default:   addressWrite(hl, dat + 1);
                    break;
        }
        break;
    case 0x35 :               // DEC (HL)
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        dat = JavaBoy.unsign(addressRead(hl));
        switch (dat) {
         case 0x00: f |= F_HALFCARRY;
                    addressWrite(hl, 0xFF);
                    break;
         case 0x10: f |= F_HALFCARRY;
                    addressWrite(hl, 0x0F);
                    break;
         case 0x01: f |= F_ZERO;
                    addressWrite(hl, 0x00);
                    break;
         default:   addressWrite(hl, dat - 1);
                    break;
        }
        break;
    case 0x36 :               // LD (HL), nn
        pc += 2;
        addressWrite(hl, b2);
        break;
    case 0x37 :               // SCF
        pc++;
        f &= F_ZERO;
        f |= F_CARRY;
        break;
    case 0x38 :               // JR C, nn
        if ((f & F_CARRY) == F_CARRY) {
         pc += 2 + offset;
        } else {
         pc += 2;
        }
        break;
    case 0x39 :               // ADD HL, SP      ** Could be wrong **
        pc++;
        hl = (hl + sp);
        if ((hl & 0xFFFF0000) != 0) {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)) | (F_CARRY));
         hl &= 0xFFFF;
        } else {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)));
        }
        break;
    case 0x3A :               // LD A, (HL-)
        pc++;
        a = JavaBoy.unsign(addressRead(hl));
        hl = (hl - 1) & 0xFFFF;
        break;
    case 0x3B :               // DEC SP
        pc++;
        sp = (sp - 1) & 0xFFFF;
        break;
    case 0x3C :               // INC A
        pc++;
        f &= F_CARRY;
        switch (a) {
         case 0xFF: f |= F_HALFCARRY + F_ZERO;
                    a = 0x00;
                    break;
         case 0x0F: f |= F_HALFCARRY;
                    a = 0x10;
                    break;
         default:   a++;
                    break;
        }
        break;
    case 0x3D :               // DEC A
        pc++;
        f &= F_CARRY;
        f |= F_SUBTRACT;
        switch (a) {
         case 0x00: f |= F_HALFCARRY;
                    a = 0xFF;
                    break;
         case 0x10: f |= F_HALFCARRY;
                    a = 0x0F;
                    break;
         case 0x01: f |= F_ZERO;
                    a = 0x00;
                    break;
         default:   a--;
                    break;
        }
        break;
    case 0x3E :               // LD A, nn
        pc += 2;
        a = b2;
        break;
    case 0x3F :               // CCF
        pc++;
        if ((f & F_CARRY) == 0) {
         f = (short) ((f & F_ZERO) | F_CARRY);
        } else {
         f = (short) (f & F_ZERO);
        }
        break;
    case 0x52 :               // Debug breakpoint (LD D, D)
	    // As this insturction is used in games (why?) only break here if the breakpoint is on in the debugger
		if (breakpointEnable) {
         terminate = true;
         System.out.println("- Breakpoint reached");
		} else {
		 pc++;
		}
        break;

    case 0x76 :               // HALT
 	    interruptsEnabled = true;
//		System.out.println("Halted, pc = " + JavaBoy.hexWord(pc));
        while (ioHandler.registers[0x0F] == 0) {
         initiateInterrupts();
         instrCount++;
        }

//		System.out.println("intrcount: " + instrCount + " IE: " + JavaBoy.hexByte(ioHandler.registers[0xFF]));
//		System.out.println(" Finished halt");
        pc++;
        break;
    case 0xAF :               // XOR A, A (== LD A, 0)
        pc ++;
        a = 0;
        f = 0x80;             // Set zero flag
        break;
    case 0xC0 :               // RET NZ
        if ((f & F_ZERO) == 0) {
         pc = (JavaBoy.unsign(addressRead(sp + 1)) << 8) + JavaBoy.unsign(addressRead(sp));
         sp += 2;
        } else {
         pc++;
        }
        break;
    case 0xC1 :               // POP BC
        pc++;
        c = JavaBoy.unsign(addressRead(sp));
        b = JavaBoy.unsign(addressRead(sp + 1));
        sp+=2;
        break;
    case 0xC2 :               // JP NZ, nnnn
        if ((f & F_ZERO) == 0) {
         pc = (b3 << 8) + b2;
        } else {
         pc += 3;
        }
        break;
    case 0xC3 :
        pc = (b3 << 8) + b2;  // JP nnnn
        break;
    case 0xC4 :               // CALL NZ, nnnnn
        if ((f & F_ZERO) == 0) {
         pc += 3;
         sp -= 2;
         addressWrite(sp + 1, pc >> 8);
         addressWrite(sp, pc & 0x00FF);
         pc = (b3 << 8) + b2;
        } else {
         pc+=3;
        }
        break;
    case 0xC5 :               // PUSH BC
        pc++;
        sp -= 2;
        sp &= 0xFFFF;
        addressWrite(sp, c);
        addressWrite(sp + 1, b);
        break;
    case 0xC6 :               // ADD A, nn
        pc+=2;
        f = 0;

        if ((((a & 0x0F) + (b2 & 0x0F)) & 0xF0) != 0x00) {
         f |= F_HALFCARRY;
        }

        a += b2;

        if ((a & 0xFF00) != 0) {     // Perform 8-bit overflow and set zero flag
         if (a == 0x0100) {
          f |= F_ZERO + F_CARRY + F_HALFCARRY;
          a = 0;
         } else {
          f |= F_CARRY + F_HALFCARRY;
          a &= 0x00FF;
         }
        }
        break;
    case 0xCF :               // RST 08
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = 0x08;
        break;
    case 0xC8 :               // RET Z
        if ((f & F_ZERO) == F_ZERO) {
         pc = (JavaBoy.unsign(addressRead(sp + 1)) << 8) + JavaBoy.unsign(addressRead(sp));
         sp += 2;
        } else {
         pc++;
        }
        break;
    case 0xC9 :               // RET
        pc = (JavaBoy.unsign(addressRead(sp + 1)) << 8) + JavaBoy.unsign(addressRead(sp));
        sp += 2;
        break;
    case 0xCA :               // JP Z, nnnn
        if ((f & F_ZERO) == F_ZERO) {
         pc = (b3 << 8) + b2;
        } else {
         pc += 3;
        }
        break;
    case 0xCB :               // Shift/bit test
        pc += 2;
        int regNum = b2 & 0x07;
        int data = registerRead(regNum);
//        System.out.println("0xCB instr! - reg " + JavaBoy.hexByte((short) (b2 & 0xF4)));
        if ((b2 & 0xC0) == 0) {
         switch ((b2 & 0xF8)) {
          case 0x00 :          // RLC A
           if ((data & 0x80) == 0x80) {
            f = F_CARRY;
           } else {
            f = 0;
           }
           data <<= 1;
           if ((f & F_CARRY) == F_CARRY) {
            data |= 1;
           }

           data &= 0xFF;
           if (data == 0) {
            f |= F_ZERO;
           }
           registerWrite(regNum, data);
           break;
          case 0x08 :          // RRC A
           if ((data & 0x01) == 0x01) {
            f = F_CARRY;
           } else {
            f = 0;
           }
           data >>= 1;
           if ((f & F_CARRY) == F_CARRY) {
            data |= 0x80;
           }
           if (data == 0) {
            f |= F_ZERO;
           }
           registerWrite(regNum, data);
           break;
          case 0x10 :          // RL r
   
           if ((data & 0x80) == 0x80) {
            newf = F_CARRY;
           } else {
            newf = 0;
           }
           data <<= 1;
          
           if ((f & F_CARRY) == F_CARRY) {
            data |= 1;
           }

           data &= 0xFF;
           if (data == 0) {
            newf |= F_ZERO;
           }
           f = newf;
           registerWrite(regNum, data);
           break;
          case 0x18 :          // RR r
           if ((data & 0x01) == 0x01) {
            newf = F_CARRY;
           } else {
            newf = 0;
           }
           data >>= 1;
  
           if ((f & F_CARRY) == F_CARRY) {
            data |= 0x80;
           }
      
           if (data == 0) {
            newf |= F_ZERO;
           }
           f = newf;
           registerWrite(regNum, data);
           break;
          case 0x20 :          // SLA r
           if ((data & 0x80) == 0x80) {
            f = F_CARRY;
           } else {
            f = 0;
           }

           data <<= 1;
  
           data &= 0xFF;
           if (data == 0) {
            f |= F_ZERO;
           }
           registerWrite(regNum, data);
           break;
          case 0x28 :          // SRA r
           short topBit = 0;

           topBit = (short) (data & 0x80);
           if ((data & 0x01) == 0x01) {
            f = F_CARRY;
           } else {
            f = 0;
           }

           data >>= 1;
           data |= topBit;

           if (data == 0) {
            f |= F_ZERO;
           }
           registerWrite(regNum, data);
           break;
          case 0x30 :          // SWAP r
  
           data = (short) (((data & 0x0F) << 4) | ((data & 0xF0) >> 4));
           if (data == 0) {
            f = F_ZERO;
           } else {
            f = 0;
           }
//           System.out.println("SWAP - answer is " + JavaBoy.hexByte(data));
           registerWrite(regNum, data);
           break;
          case 0x38 :          // SRL r
           if ((data & 0x01) == 0x01) {
            f = F_CARRY;
           } else {
            f = 0;
           }

           data >>= 1;

           if (data == 0) {
            f |= F_ZERO;
           }
           registerWrite(regNum, data);
           break;
          }
        } else {

         int bitNumber = (b2 & 0x38) >> 3;

         if ((b2 & 0xC0) == 0x40)  {  // BIT n, r
          mask = (short) (0x01 << bitNumber);
          if ((data & mask) != 0) {
           f = (short) ((f & F_CARRY) | F_HALFCARRY);
          } else {
           f = (short) ((f & F_CARRY) | (F_HALFCARRY + F_ZERO));
          }
         }
         if ((b2 & 0xC0) == 0x80) {  // RES n, r
          mask = (short) (0xFF - (0x01 << bitNumber));
          data = (short) (data & mask);
          registerWrite(regNum, data);
         }
         if ((b2 & 0xC0) == 0xC0) {  // SET n, r
          mask = (short) (0x01 << bitNumber);
          data = (short) (data | mask);
          registerWrite(regNum, data);
         }

        }

        break;
    case 0xCC :               // CALL Z, nnnnn
        if ((f & F_ZERO) == F_ZERO) {
         pc += 3;
         sp -= 2;
         addressWrite(sp + 1, pc >> 8);
         addressWrite(sp, pc & 0x00FF);
         pc = (b3 << 8) + b2;
        } else {
         pc+=3;
        }
        break;
    case 0xCD :               // CALL nnnn
        pc += 3;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = (b3 << 8) + b2;
        break;
    case 0xCE :               // ADC A, nn
        pc+=2;

        if ((f & F_CARRY) != 0) {
         b2++;
        }
        f = 0;

        if ((((a & 0x0F) + (b2 & 0x0F)) & 0xF0) != 0x00) {
         f |= F_HALFCARRY;
        }

        a += b2;

        if ((a & 0xFF00) != 0) {     // Perform 8-bit overflow and set zero flag
         if (a == 0x0100) {
          f |= F_ZERO + F_CARRY + F_HALFCARRY;
          a = 0;
         } else {
          f |= F_CARRY + F_HALFCARRY;
          a &= 0x00FF;
         }
        }
        break;
    case 0xC7 :               // RST 00
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
//        terminate = true;
        pc = 0x00;
        break;
    case 0xD0 :               // RET NC
        if ((f & F_CARRY) == 0) {
         pc = (JavaBoy.unsign(addressRead(sp + 1)) << 8) + JavaBoy.unsign(addressRead(sp));
         sp += 2;
        } else {
         pc++;
        }
        break;
    case 0xD1 :               // POP DE
        pc++;
        e = JavaBoy.unsign(addressRead(sp));
        d = JavaBoy.unsign(addressRead(sp + 1));
        sp+=2;
        break;
    case 0xD2 :               // JP NC, nnnn
        if ((f & F_CARRY) == 0) {
         pc = (b3 << 8) + b2;
        } else {
         pc += 3;
        }
        break;
    case 0xD4 :               // CALL NC, nnnn
        if ((f & F_CARRY) == 0) {
         pc += 3;
         sp -= 2;
         addressWrite(sp + 1, pc >> 8);
         addressWrite(sp, pc & 0x00FF);
         pc = (b3 << 8) + b2;
        } else {
         pc+=3;
        }
        break;
    case 0xD5 :               // PUSH DE
        pc++;
        sp -= 2;
        sp &= 0xFFFF;
        addressWrite(sp, e);
        addressWrite(sp + 1, d);
        break;
    case 0xD6 :               // SUB A, nn
        pc+=2;

        f = F_SUBTRACT;

        if ((((a & 0x0F) - (b2 & 0x0F)) & 0xFFF0) != 0x00) {
         f |= F_HALFCARRY;
        }

        a -= b2;

        if ((a & 0xFF00) != 0) {
         a &= 0x00FF;
         f |= F_CARRY;
         }
         if (a == 0) {
          f |= F_ZERO;
         }
         break;
    case 0xD7 :               // RST 10
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = 0x10;
        break;
    case 0xD8 :               // RET C
        if ((f & F_CARRY) == F_CARRY) {
         pc = (JavaBoy.unsign(addressRead(sp + 1)) << 8) + JavaBoy.unsign(addressRead(sp));
         sp += 2;
        } else {
         pc++;
        }
        break;
    case 0xD9 :               // RETI
        interruptsEnabled = true;
        inInterrupt = false;
        pc = (JavaBoy.unsign(addressRead(sp + 1)) << 8) + JavaBoy.unsign(addressRead(sp));
        sp += 2;
        break;
    case 0xDA :               // JP C, nnnn
        if ((f & F_CARRY) == F_CARRY) {
         pc = (b3 << 8) + b2;
        } else {
         pc += 3;
        }
        break;
    case 0xDC :               // CALL C, nnnn
        if ((f & F_CARRY) == F_CARRY) {
         pc += 3;
         sp -= 2;
         addressWrite(sp + 1, pc >> 8);
         addressWrite(sp, pc & 0x00FF);
         pc = (b3 << 8) + b2;
        } else {
         pc+=3;
        }
        break;
    case 0xDE :               // SBC A, nn
        pc+=2;
        if ((f & F_CARRY) != 0) {
         b2++;
        }

        f = F_SUBTRACT;
        if ((((a & 0x0F) - (b2 & 0x0F)) & 0xFFF0) != 0x00) {
         f |= F_HALFCARRY;
        }

        a -= b2;

        if ((a & 0xFF00) != 0) {
         a &= 0x00FF;
         f |= F_CARRY;
        }

        if (a == 0) {
         f |= F_ZERO;
        }
        break;
    case 0xDF :               // RST 18
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = 0x18;
        break;
    case 0xE0 :               // LDH (FFnn), A
        pc += 2;
        addressWrite(0xFF00 + b2, a);
        break;
    case 0xE1 :               // POP HL
        pc++;
        hl = (JavaBoy.unsign(addressRead(sp + 1)) << 8) + JavaBoy.unsign(addressRead(sp));
        sp += 2;
        break;
    case 0xE2 :               // LDH (FF00 + C), A
        pc++;
        addressWrite(0xFF00 + c, a);
        break;
    case 0xE5 :               // PUSH HL
        pc++;
        sp -= 2;
        sp &= 0xFFFF;
        addressWrite(sp + 1, hl >> 8);
        addressWrite(sp, hl & 0x00FF);
        break;
    case 0xE6 :               // AND nn
        pc+=2;
        a &= b2;
        if (a == 0) {
         f = F_ZERO;
        } else {
         f = 0;
        }
        break;
    case 0xE7 :               // RST 20
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = 0x20;
        break;
    case 0xE8 :               // ADD SP, nn
        pc+=2;
        sp = (sp + offset);
        if ((sp & 0xFFFF0000) != 0) {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)) | (F_CARRY));
         sp &= 0xFFFF;
        } else {
         f = (short) ((f & (F_SUBTRACT + F_ZERO + F_HALFCARRY)));
        }
        break;
    case 0xE9 :               // JP (HL)
        pc++;
        pc = hl;
        break;
    case 0xEA :               // LD (nnnn), A
        pc += 3;              
        addressWrite((b3 << 8) + b2, a);
        break;
    case 0xEE :               // XOR A, nn
        pc+=2;
        a ^= b2;
        if (a == 0) {
         f = F_ZERO;
        } else {
         f = 0;
        }
        break;
    case 0xEF :               // RST 28
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = 0x28;
        break;
    case 0xF0 :               // LDH A, (FFnn)
        pc += 2;
        a = JavaBoy.unsign(addressRead(0xFF00 + b2));
        break;
    case 0xF1 :               // POP AF
        pc++;
        f = JavaBoy.unsign(addressRead(sp));
        a = JavaBoy.unsign(addressRead(sp + 1));
        sp+=2;
        break;
    case 0xF2 :               // LD A, (FF00 + C)
        pc++;
        a = JavaBoy.unsign(addressRead(0xFF00 + c));
        break;
    case 0xF3 :               // DI
        pc++;
        interruptsEnabled = false;
    //    addressWrite(0xFFFF, 0);
        break;
    case 0xF5 :               // PUSH AF
        pc++;
        sp -= 2;
        sp &= 0xFFFF;
        addressWrite(sp, f);
        addressWrite(sp + 1, a);
        break;
    case 0xF6 :               // OR A, nn
        pc+=2;
        a |= b2;
        if (a == 0) {
         f = F_ZERO;
        } else {
         f = 0;
        }
        break;
    case 0xF7 :               // RST 30
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = 0x30;
        break;
    case 0xF8 :               // LD HL, SP + nn  ** HALFCARRY FLAG NOT SET ***
        pc += 2;
        hl = (sp + offset);
        if ((hl & 0x10000) != 0) {
         f = F_CARRY;
         hl &= 0xFFFF;
        } else {
         f = 0;
        }
        break;
    case 0xF9 :               // LD SP, HL
        pc++;
        sp = hl;
        break;
    case 0xFA :               // LD A, (nnnn)
        pc+=3;
        a = JavaBoy.unsign(addressRead((b3 << 8) + b2));
        break;
    case 0xFB :               // EI
        pc++;
        ieDelay = 1;
      //  interruptsEnabled = true;
      //  addressWrite(0xFFFF, 0xFF);
        break;
    case 0xFE :               // CP nn     ** FLAGS ARE WRONG! **
        pc += 2;
        f = 0;
        if (b2 == a) {
         f |= F_ZERO;
        } else {
         if (a < b2) {
          f |= F_CARRY;
         }
        }
        break;
    case 0xFF :               // RST 38
        pc++;
        sp -= 2;
        addressWrite(sp + 1, pc >> 8);
        addressWrite(sp, pc & 0x00FF);
        pc = 0x38;
        break;

    default :

        if ((b1 & 0xC0) == 0x80) {       // Byte 0x10?????? indicates ALU op
         pc++;
         int operand = registerRead(b1 & 0x07);
         switch ((b1 & 0x38) >> 3) {
          case 1 : // ADC A, r
              if ((f & F_CARRY) != 0) {
               operand++;
              }
              // Note!  No break!
          case 0 : // ADD A, r

              f = 0;

              if ((((a & 0x0F) + (operand & 0x0F)) & 0xF0) != 0x00) {
               f |= F_HALFCARRY;
              }

              a += operand;

              if (a == 0) {
               f |= F_ZERO;
              }

              if ((a & 0xFF00) != 0) {     // Perform 8-bit overflow and set zero flag
               if (a == 0x0100) {
                f |= F_ZERO + F_CARRY + F_HALFCARRY;
                a = 0;
               } else {
                f |= F_CARRY + F_HALFCARRY;
                a &= 0x00FF;
               }
              }
              break;
          case 3 : // SBC A, r
              if ((f & F_CARRY) != 0) {
               operand++;
              }
              // Note! No break!
          case 2 : // SUB A, r

              f = F_SUBTRACT;

              if ((((a & 0x0F) - (operand & 0x0F)) & 0xFFF0) != 0x00) {
               f |= F_HALFCARRY;
              }

              a -= operand;

              if ((a & 0xFF00) != 0) {
               a &= 0x00FF;
               f |= F_CARRY;
              }
              if (a == 0) {
               f |= F_ZERO;
              }

              break;
          case 4 : // AND A, r
              a &= operand;
              if (a == 0) {
               f = F_ZERO;
              } else {
               f = 0;
              }
              break;
          case 5 : // XOR A, r
              a ^= operand;
              if (a == 0) {
               f = F_ZERO;
              } else {
               f = 0;
              }
              break;
          case 6 : // OR A, r
              a |= operand;
              if (a == 0) {
               f = F_ZERO;
              } else {
               f = 0;
              }
              break;
          case 7 : // CP A, r (compare)
              f = F_SUBTRACT;
              if (a == operand) {
               f |= F_ZERO;
              }
              if (a < operand) {
               f |= F_CARRY;
              }
              if ((a & 0x0F) < (operand & 0x0F)) {
               f |= F_HALFCARRY;
              }
              break;
         }
        } else if ((b1 & 0xC0) == 0x40) {   // Byte 0x01xxxxxxx indicates 8-bit ld

         pc++;
         registerWrite((b1 & 0x38) >> 3, registerRead(b1 & 0x07));

        } else {
         System.out.println("Unrecognized opcode (" + JavaBoy.hexByte(b1) + ")");
         terminate = true;
         pc++;
         break;
        }
   }

    
   if (ieDelay != -1) {

    if (ieDelay > 0) {
     ieDelay--;
    } else {
     interruptsEnabled = true;
     ieDelay = -1;
    }

   }


   if (interruptsEnabled) {
    checkInterrupts();
   }

   cartridge.update();


   initiateInterrupts();


/*   if ((hl & 0xFFFF0000) != 0) {
    terminate = true;
    System.out.println("Overflow in HL!");
   }*/

  }
  running = false;
  terminate = false;
 }

 public void setBreakpoint(boolean on) {
  breakpointEnable = on;
 }

	
	
 /** Output a disassembly of the specified number of instructions starting at the speicifed address.
  */
 public String disassemble(int address, int numInstr) {

  System.out.println("Addr  Data      Instruction");

  for (int r = 0; r < numInstr; r++) {
   short b1 = JavaBoy.unsign(addressRead(address));
   short offset = addressRead(address + 1);
   short b3 = JavaBoy.unsign(addressRead(address + 2));
   short b2 = JavaBoy.unsign(offset);

   String instr = new String("Unknown Opcode! (" + Integer.toHexString(JavaBoy.unsign(b1)) + ")");
   byte instrLength = 1;

   switch (b1) {
    case 0x00 :
           instr = "NOP";
           break;
    case 0x01 :
           instr = "LD BC, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0x02 :
           instr = "LD (BC), A";
           break;
    case 0x03 :
           instr = "INC BC";
           break;
    case 0x04 :
           instr = "INC B";
           break;
    case 0x05 :
           instr = "DEC B";
           break;
    case 0x06 :
           instr = "LD B, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0x07 :
           instr = "RLC A";
           break;
    case 0x08 :
           instr = "LD (" + JavaBoy.hexWord((b3 << 8) + b2) + "), SP";
           instrLength = 3;        // Non Z80
           break;
    case 0x09 :
           instr = "ADD HL, BC";
           break;
    case 0x0A :
           instr = "LD A, (BC)";
           break;
    case 0x0B :
           instr = "DEC BC";
           break;
    case 0x0C :
           instr = "INC C";
           break;
    case 0x0D :
           instr = "DEC C";
           break;
    case 0x0E :
           instr = "LD C, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0x0F :
           instr = "RRC A";
           break;
    case 0x10 :
           instr = "STOP";
           instrLength = 2;  // STOP instruction must be followed by a NOP
           break;
    case 0x11 :
           instr = "LD DE, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0x12 :
           instr = "LD (DE), A";
           break;
    case 0x13 :
           instr = "INC DE";
           break;
    case 0x14 :
           instr = "INC D";
           break;
    case 0x15 :
           instr = "DEC D";
           break;
    case 0x16 :
           instr = "LD D, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0x17 :
           instr = "RL A";
           break;
    case 0x18 :
           instr = "JR " + JavaBoy.hexWord(address + 2 + offset);
           instrLength = 2;
           break;
    case 0x19 :
           instr = "ADD HL, DE";
           break;
    case 0x1A :
           instr = "LD A, (DE)";
           break;
    case 0x1B :
           instr = "DEC DE";
           break;
    case 0x1C :
           instr = "INC E";
           break;
    case 0x1D :
           instr = "DEC E";
           break;
    case 0x1E :
           instr = "LD E, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0x1F :
           instr = "RR A";
           break;
    case 0x20 :
           instr = "JR NZ, " + JavaBoy.hexWord(address + 2 + offset) + ": " + offset;
           
           instrLength = 2;
           break;
    case 0x21 :
           instr = "LD HL, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0x22 :
           instr = "LD (HL+), A";     // Non Z80
           break;
    case 0x23 :
           instr = "INC HL";
           break;
    case 0x24 :
           instr = "INC H";
           break;
    case 0x25 :
           instr = "DEC H";
           break;
    case 0x26 :
           instr = "LD H, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0x27 :
           instr = "DAA";
           break;
    case 0x28 :
           instr = "JR Z, " + JavaBoy.hexWord(address + 2 + offset);
           instrLength = 2;
           break;
    case 0x29 :
           instr = "ADD HL, HL";
           break;
    case 0x2A :
           instr = "LDI A, (HL)";
           break;
    case 0x2B :
           instr = "DEC HL";
           break;
    case 0x2C :
           instr = "INC L";
           break;
    case 0x2D :
           instr = "DEC L";
           break;
    case 0x2E :
           instr = "LD L, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0x2F :
           instr = "CPL";
           break;
    case 0x30 :
           instr = "JR NC, " + JavaBoy.hexWord(address + 2 + offset);
           instrLength = 2;
           break;
    case 0x31 :
           instr = "LD SP, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0x32 :
           instr = "LD (HL-), A";
           break;
    case 0x33 :
           instr = "INC SP";
           break;
    case 0x34 :
           instr = "INC (HL)";
           break;
    case 0x35 :
           instr = "DEC (HL)";
           break;
    case 0x36 :
           instr = "LD (HL), " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0x37 :
           instr = "SCF";     // Set carry flag? 
           break;
    case 0x38 :
           instr = "JR C, " + JavaBoy.hexWord(address + 2 + offset);
           instrLength = 2;
           break;
    case 0x39 :
           instr = "ADD HL, SP";
           break;
    case 0x3A :
           instr = "LD A, (HL-)";
           break;
    case 0x3B :
           instr = "DEC SP";
           break;
    case 0x3C :
           instr = "INC A";
           break;
    case 0x3D :
           instr = "DEC A";
           break;
    case 0x3E :
           instr = "LD A, " + JavaBoy.hexByte(JavaBoy.unsign(b2));
           instrLength = 2;
           break;
    case 0x3F :
           instr = "CCF";   // Clear carry flag? 
           break;

    case 0x76 :
           instr = "HALT";
           break;

    // 0x40 - 0x7F = LD Reg, Reg - see below
    // 0x80 - 0xBF = ALU ops - see below

    case 0xC0 :
           instr = "RET NZ";
           break;
    case 0xC1 :
           instr = "POP BC";
           break;
    case 0xC2 :
           instr = "JP NZ, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0xC3 :
           instr = "JP " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0xC4 :
           instr = "CALL NZ, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0xC5 :
           instr = "PUSH BC";
           break;
    case 0xC6 :
           instr = "ADD A, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0xC7 :
           instr = "RST 00";      // Is this an interrupt call?
           break;
    case 0xC8 :
           instr = "RET Z";
           break;
    case 0xC9 :
           instr = "RET";
           break;
    case 0xCA :
           instr = "JP Z, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;

    // 0xCB = Shifts (see below)

    case 0xCC :
           instr = "CALL Z, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0xCD :
           instr = "CALL " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0xCE :
           instr = "ADC A, " + JavaBoy.hexByte(b2);  // Signed or unsigned?
           instrLength = 2;
           break;
    case 0xCF :
           instr = "RST 08";      // Is this an interrupt call?
           break;
    case 0xD0 :
           instr = "RET NC";
           break;
    case 0xD1 :
           instr = "POP DE";
           break;
    case 0xD2 :
           instr = "JP NC, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;

    // 0xD3: Unknown

    case 0xD4 :
           instr = "CALL NC, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;
    case 0xD5 :
           instr = "PUSH DE";
           break;
    case 0xD6 :
           instr = "SUB A, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0xD7 :
           instr = "RST 10";
           break;
    case 0xD8 :
           instr = "RET C";
           break;
    case 0xD9 :
           instr = "RETI";
           break;
    case 0xDA :
           instr = "JP C, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;

    // 0xDB: Unknown

    case 0xDC :
           instr = "CALL C, " + JavaBoy.hexWord((b3 << 8) + b2);
           instrLength = 3;
           break;

    // 0xDD: Unknown

    case 0xDE :
           instr = "SBC A, " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0xDF :
           instr = "RST 18";
           break;
    case 0xE0 :
           instr = "LDH (FF" + JavaBoy.hexByte(b2 & 0xFF) + "), A";
           instrLength = 2;
           break;
    case 0xE1 :
           instr = "POP HL";
           break;
    case 0xE2 :
           instr = "LDH (FF00 + C), A";
           break;

    // 0xE3 - 0xE4: Unknown

    case 0xE5 :
           instr = "PUSH HL";
           break;
    case 0xE6 :
           instr = "AND " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0xE7 :
           instr = "RST 20";
           break;
    case 0xE8 :
           instr = "ADD SP, " + JavaBoy.hexByte(offset);
           instrLength = 2;
           break;
    case 0xE9 :
           instr = "JP (HL)";
           break;
    case 0xEA :
           instr = "LD (" + JavaBoy.hexWord((b3 << 8) + b2) + "), A";
           instrLength = 3;
           break;

    // 0xEB - 0xED: Unknown

    case 0xEE :
           instr = "XOR " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0xEF :
           instr = "RST 28";
           break;
    case 0xF0 :
           instr = "LDH A, (FF" + JavaBoy.hexByte(b2) + ")";
           instrLength = 2;
           break;
    case 0xF1 :
           instr = "POP AF";
           break;
    case 0xF2 :
           instr = "LD A, (FF00 + C)";              // What's this for?
           break;
    case 0xF3 :
           instr = "DI";
           break;

    // 0xF4: Unknown

    case 0xF5 :
           instr = "PUSH AF";
           break;
    case 0xF6 :
           instr = "OR " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0xF7 :
           instr = "RST 30";
           break;
    case 0xF8 :
           instr = "LD HL, SP + " + JavaBoy.hexByte(offset);  // Check this one, docs disagree
           instrLength = 2;
           break;
    case 0xF9 :
           instr = "LD SP, HL";
           break;
    case 0xFA :
           instr = "LD A, (" + JavaBoy.hexWord((b3 << 8) + b2) + ")";
           instrLength = 3;
           break;
    case 0xFB :
           instr = "EI";
           break;

    // 0xFC - 0xFD: Unknown

    case 0xFE :
           instr = "CP " + JavaBoy.hexByte(b2);
           instrLength = 2;
           break;
    case 0xFF :
           instr = "RST 38";
           break;
   }

 // The following section handles LD Reg, Reg instructions
 // Bit 7 6 5 4 3 2 1 0    D = Dest register
 //     0 1 D D D S S S    S = Source register
 // The exception to this rule is 0x76, which is HALT, and takes
 // the place of LD (HL), (HL)

   if ((JavaBoy.unsign(b1) >= 0x40) && (JavaBoy.unsign(b1) <= 0x7F) && (
       (JavaBoy.unsign(b1) != 0x76))) {
    /* 0x76 is HALT, and takes the place of LD (HL), (HL) */
    int sourceRegister = b1 & 0x07;         /* Lower 3 bits */
    int destRegister = (b1 & 0x38) >> 3;    /* Bits 5 - 3 */

//    System.out.println("LD Op src" + sourceRegister + " dest " + destRegister);

    instr = "LD " + registerNames[destRegister] + ", " +
                    registerNames[sourceRegister];
   }
   
 // The following section handles arithmetic instructions
 // Bit 7 6 5 4 3 2 1 0    Operation       Opcode
 //     1 0 0 0 0 R R R    Add             ADD
 //     1 0 0 0 1 R R R    Add with carry  ADC
 //     1 0 0 1 0 R R R    Subtract        SUB
 //     1 0 0 1 1 R R R    Sub with carry  SBC
 //     1 0 1 0 0 R R R    Logical and     AND
 //     1 0 1 0 1 R R R    Logical xor     XOR
 //     1 0 1 1 0 R R R    Logical or      OR
 //     1 0 1 1 1 R R R    Compare?        CP

   if ((JavaBoy.unsign(b1) >= 0x80) && (JavaBoy.unsign(b1) <= 0xBF)) {
    int sourceRegister = JavaBoy.unsign(b1) & 0x07;
    int operation = (JavaBoy.unsign(b1) & 0x38) >> 3;

 //   System.out.println("ALU Op " + operation + " reg " + sourceRegister);

    instr = aluOperations[operation] + " A, " + registerNames[sourceRegister];
   }

 // The following section handles shift instructions
 // These are formed by the byte 0xCB followed by the this:
 // Bit 7 6 5 4 3 2 1 0    Operation             Opcode
 //     0 0 0 0 0 R R R    Rotate Left Carry     RLC
 //     0 0 0 0 1 R R R    Rotate Right Carry    RRC
 //     0 0 0 1 0 R R R    Rotate Left           RL
 //     0 0 0 1 1 R R R    Rotate Right          RR
 //     0 0 1 0 0 R R R    Arith. Shift Left     SLA
 //     0 0 1 0 1 R R R    Arith. Shift Right    SRA
 //     0 0 1 1 0 R R R    Hi/Lo Nibble Swap     SWAP
 //     0 0 1 1 1 R R R    Shift Right Logical   SRL
 //     0 1 N N N R R R    Bit Test n            BIT
 //     1 0 N N N R R R    Reset Bit n           RES
 //     1 1 N N N R R R    Set Bit n             SET

   if (JavaBoy.unsign(b1) == 0xCB) {
    int operation;
    int sourceRegister;
    int bitNumber;

    instrLength = 2;

    switch ((JavaBoy.unsign(b2) & 0xC0) >> 6) {
     case 0 :
      operation = (JavaBoy.unsign(b2) & 0x38) >> 3;
      sourceRegister = JavaBoy.unsign(b2) & 0x07;
      instr = shiftOperations[operation] + " " + registerNames[sourceRegister];
      break;
     case 1 :
      bitNumber = (JavaBoy.unsign(b2) & 0x38) >> 3;
      sourceRegister = JavaBoy.unsign(b2) & 0x07;
      instr = "BIT " + bitNumber + ", " + registerNames[sourceRegister];
      break;
     case 2 :
      bitNumber = (JavaBoy.unsign(b2) & 0x38) >> 3;
      sourceRegister = JavaBoy.unsign(b2) & 0x07;
      instr = "RES " + bitNumber + ", " + registerNames[sourceRegister];
      break;
     case 3 :
      bitNumber = (JavaBoy.unsign(b2) & 0x38) >> 3;
      sourceRegister = JavaBoy.unsign(b2) & 0x07;
      instr = "SET " + bitNumber + ", " + registerNames[sourceRegister];
      break;
    }
   }


   System.out.print(JavaBoy.hexWord(address) + ": " + JavaBoy.hexByte(JavaBoy.unsign(b1)));

   if (instrLength >= 2) {
    System.out.print(" " + JavaBoy.hexByte(JavaBoy.unsign(b2)));
   } else {
    System.out.print("   ");
   }

   if (instrLength == 3) {
    System.out.print(" " + JavaBoy.hexByte(JavaBoy.unsign(b3))+ "  ");
   } else {
    System.out.print("     ");
   }


   System.out.println(instr);
   address += instrLength;
  }


  return null;
 }
	
 public void jumpInstructions(int address, int endAddress, int destination){
		
		int numInstr = endAddress - address + 1; //inclusive
		
		for (int r = 0; r < numInstr; r++) {
			short b1 = JavaBoy.unsign(addressRead(address));
			short offset = addressRead(address + 1);
			short b3 = JavaBoy.unsign(addressRead(address + 2));
			short b2 = JavaBoy.unsign(offset);
			byte instrLength = 1;
			
			switch (b1) {
				case 0x01 :
				case 0x08 :
				case 0x11 : 
				case 0x31 :
				case 0x21 :
				case 0xCC :
				case 0xCD :
				case 0xC4 :
				case 0xDC :
				case 0xEA :
				case 0xFA :
				case 0xD4 :
					instrLength = 3;
					break;
					
				case 0x06 :
				case 0x0E :
				case 0x10 :
				case 0x16 :
				case 0x1E :
				case 0x26 :
				case 0x2E :
				case 0x36 :
				case 0x3E :
				case 0xC6 :
				case 0xCE :
				case 0xD6 :
				case 0xDE :
				case 0xE0 :
				case 0xE6 :
				case 0xE8 :
				case 0xEE :
				case 0xF0 :
				case 0xF6 :
				case 0xF8 :
				case 0xFE :
					instrLength = 2;
					break;
					
				case 0x18 :
					if(address + 2 + offset == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JR " + JavaBoy.hexWord(address + 2 + offset));
					}
					instrLength = 2;
					break;
				case 0xC2 :
					if((b3 << 8) + b2 == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JP NZ, " + JavaBoy.hexWord((b3 << 8) + b2));
					}
					instrLength = 3;
					break;
				case 0x20 :
					if(address + 2 + offset == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JR NZ, " + 
										   JavaBoy.hexWord(address + 2 + offset) + ": " + offset);
					}
					instrLength = 2;
					break;
				case 0x28 :
					if(address + 2 + offset == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JR Z, " + JavaBoy.hexWord(address + 2 + offset));
					}
					instrLength = 2;
					break;
				case 0x30 :
					if(address + 2 + offset == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JR NC, " + JavaBoy.hexWord(address + 2 + offset));
					}
					instrLength = 2;
					break;
				case 0x38 :
					if(address + 2 + offset == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JR C, " + JavaBoy.hexWord(address + 2 + offset));
					}
					instrLength = 2;
					break;
				case 0xC3 :
					if((b3 << 8) + b2 == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JP " + JavaBoy.hexWord((b3 << 8) + b2));
					}
					instrLength = 3;
					break;
				case 0xCA :
					if((b3 << 8) + b2 == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JP Z, " + JavaBoy.hexWord((b3 << 8) + b2));
					}
					instrLength = 3;
					break;
				case 0xD2 :
					if((b3 << 8) + b2 == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JP NC, " + JavaBoy.hexWord((b3 << 8) + b2));
					}
					instrLength = 3;
					break;
				case 0xDA :
					if((b3 << 8) + b2 == destination){
						System.out.println(JavaBoy.hexWord(address)+ ": JP C, " + JavaBoy.hexWord((b3 << 8) + b2));
					}
					instrLength = 3;
					break;
					/*
					 case 0xE9 :
					 instr = "JP (H5L)";
					 break;
					 */
					
			}
			if (JavaBoy.unsign(b1) == 0xCB) {
				instrLength = 2;
			}
			address += instrLength;
			
		}
	}
}
