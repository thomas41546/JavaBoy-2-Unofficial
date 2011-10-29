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



/** This class is one implementation of the GraphicsChip.
 *  It performs the output of the graphics screen, including the background, window, and sprite layers.
 *  It supports some raster effects, but only ones that happen on a tile row boundary.
 */
class TileBasedGraphicsChip extends GraphicsChip {
 /** Tile cache */
 GameboyTile[] tiles = new GameboyTile[384 * 2];

 // Hacks to allow some raster effects to work.  Or at least not to break as badly.
 boolean savedWindowDataSelect = false;
 boolean spritesEnabledThisFrame = false;

 boolean windowEnableThisLine = false;
 int windowStopLine = 144;


 public TileBasedGraphicsChip(Component a, Dmgcpu d) {
  super(a, d);
  for (int r = 0; r < 384 * 2; r++) {
   tiles[r] = new GameboyTile(a);
  }
 }

 /** Flush the tile cache */
 public void dispose() {
  for (int r = 0; r < 384 * 2; r++) {
   if (tiles[r] != null) tiles[r].dispose();
  }
 }

 /** Reads data from the specified video RAM address */
 public short addressRead(int addr) {
  return videoRam[addr + vidRamStart];
 }

 /** Writes data to the specified video RAM address */
 public void addressWrite(int addr, byte data) {
  if (addr < 0x1800) {   // Bkg Tile data area
   tiles[(addr >> 4) + tileStart].invalidate();
   videoRam[addr + vidRamStart] = data;
  } else {
   videoRam[addr + vidRamStart] = data;
  }
 }

 /** Invalidates all tiles in the tile cache that have the given attributes.
  *  These will be regenerated next time they are drawn.
  */
 public void invalidateAll(int attribs) {
  for (int r = 0; r < 384 * 2; r++) {
   tiles[r].invalidate(attribs);
  }
 }

 /** Invalidate all tiles in the tile cache */
 public void invalidateAll() {
  for (int r = 0; r < 384 * 2; r++) {
   tiles[r].invalidate();
  }
 }

 /** Set the size of the Gameboy window. */
 public void setMagnify(int m) {
  super.setMagnify(m);
  for (int r = 0; r < 384 * 2; r++) {
   tiles[r].setMagnify(m);
  }
 }

 /** Draw sprites into the back buffer which have the given priority */
 public void drawSprites(Graphics back, int priority) {
  
  int tileBankStart = 0;
  int vidRamAddress = 0;

  // Draw sprites
  for (int i = 0; i < 40; i++) {
   int spriteX = dmgcpu.addressRead(0xFE01 + (i * 4)) - 8;
   int spriteY = dmgcpu.addressRead(0xFE00 + (i * 4)) - 16;
   int tileNum = dmgcpu.addressRead(0xFE02 + (i * 4));
   int attributes = dmgcpu.addressRead(0xFE03 + (i * 4));

   if ((attributes & 0x80) >> 7 == priority) {

   int spriteAttrib = 0;

   if (doubledSprites) {
    tileNum &= 0xFE;
   }

   if (dmgcpu.gbcFeatures) {
    if ((attributes & 0x08) != 0) {
     vidRamAddress = 0x2000 + (tileNum << 4);
     tileNum += 384;
     tileBankStart = 0x2000;
    } else {
     vidRamAddress = tileNum << 4;
    }
    spriteAttrib += ((attributes & 0x07) << 2) + 32;

   } else {
    vidRamAddress = tileNum << 4;
    if ((attributes & 0x10) != 0) {
     spriteAttrib |= TILE_OBJ2;
    } else {
     spriteAttrib |= TILE_OBJ1;
    }
   }

   if ((attributes & 0x20) != 0) {
    spriteAttrib |= TILE_FLIPX;
   }
   if ((attributes & 0x40) != 0) {
    spriteAttrib |= TILE_FLIPY;
   }

   if (tiles[tileNum].invalid(spriteAttrib)) {
    tiles[tileNum].validate(videoRam, vidRamAddress, spriteAttrib);
   }

   if ((spriteAttrib & TILE_FLIPY) != 0) {
    if (doubledSprites) {
     tiles[tileNum].draw(back, spriteX, spriteY + 8, spriteAttrib);
	} else {
     tiles[tileNum].draw(back, spriteX, spriteY, spriteAttrib);
	}
   } else {
    tiles[tileNum].draw(back, spriteX, spriteY, spriteAttrib);
   }

//   back.drawString("" + tileNum, spriteX * 2, spriteY * 2);
//   System.out.println("Sprite " + i + ": " + spriteX + ", " + spriteY);

   if (doubledSprites) {
    if (tiles[tileNum + 1].invalid(spriteAttrib)) {
     tiles[tileNum + 1].validate(videoRam, vidRamAddress + 16, spriteAttrib);
    }
	

    if ((spriteAttrib & TILE_FLIPY) != 0) {
     tiles[tileNum + 1].draw(back, spriteX, spriteY, spriteAttrib);
    } else {
     tiles[tileNum + 1].draw(back, spriteX, spriteY + 8, spriteAttrib);
    }
   }
   }
  }

 }

 /** This must be called by the CPU for each scanline drawn by the display hardware.  It
  *  handles drawing of the background layer
  */
 public void notifyScanline(int line) {

  if ((framesDrawn % frameSkip) != 0) {
   return;
  }

  if (line == 0) {
   clearFrameBuffer();
   /*if (spritesEnabledThisFrame)*/ drawSprites(backBuffer.getGraphics(), 1);
   spritesEnabledThisFrame = spritesEnabled;
   windowStopLine = 144;
   windowEnableThisLine = winEnabled;
  }

  // SpritesEnabledThisFrame should be true if sprites were ever on this frame
  if (spritesEnabled) spritesEnabledThisFrame = true;

  if (windowEnableThisLine) {
   if (!winEnabled) {
    windowStopLine = line;
	windowEnableThisLine = false;
//	System.out.println("Stop line: " + windowStopLine);
   }
  }

  // Fix to screwed up status bars.  Record which data area is selected on the
  // first line the window is to be displayed.  Will work unless this is changed
  // after window is started
  // NOTE: Still no real support for hblank effects on window/sprites
  if (line == JavaBoy.unsign(dmgcpu.ioHandler.registers[0x4A]) + 1) {		// Compare against WY reg
   savedWindowDataSelect = bgWindowDataSelect;
  }

 // Can't disable background on GBC (?!).  Apperently not, according to BGB
  if ((!bgEnabled) && (!dmgcpu.gbcFeatures)) return;

  int xPixelOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x43]) % 8;
  int yPixelOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x42]) % 8;

//  if ((yPixelOfs + 4) % 8 == line % 8) {

  if ( ((yPixelOfs + line) % 8 == 4) || (line == 0)) {

   if ((line >= 144) && (line < 152)) notifyScanline(line + 8);

   Graphics back = backBuffer.getGraphics();

   int xTileOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x43]) / 8;
   int yTileOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x42]) / 8;
   int bgStartAddress, tileNum;

   int y = ((line + yPixelOfs) / 8);

//   System.out.println(y + "," + line);
//    System.out.println((8 * y) - yPixelOfs);

   if (hiBgTileMapAddress) {
    bgStartAddress = 0x1C00;  /* 1C00 */
   } else {
    bgStartAddress = 0x1800;
   }

   int tileNumAddress, attributeData, vidMemAddr;

   for (int x = 0; x < 21; x++) {
    if (bgWindowDataSelect) {
     tileNumAddress = bgStartAddress +
       (((y + yTileOfs) % 32) * 32) + ((x + xTileOfs) % 32);

     tileNum = JavaBoy.unsign(videoRam[tileNumAddress]);
     attributeData = JavaBoy.unsign(videoRam[tileNumAddress + 0x2000]);
    } else {
     tileNumAddress = bgStartAddress +
        (((y + yTileOfs) % 32) * 32) + ((x + xTileOfs) % 32);

     tileNum = 256 + videoRam[tileNumAddress];
     attributeData = JavaBoy.unsign(videoRam[tileNumAddress + 0x2000]);
    }

    int attribs = 0;

    if (dmgcpu.gbcFeatures) {

     if ((attributeData & 0x08) != 0) {
      vidMemAddr = 0x2000 + (tileNum << 4);
      tileNum += 384;
     } else {
      vidMemAddr = (tileNum << 4);
     }
     if ((attributeData & 0x20) != 0) {
      attribs |= TILE_FLIPX;
     }
     if ((attributeData & 0x40) != 0) {
      attribs |= TILE_FLIPY;
     }
     attribs += ((attributeData & 0x07) * 4);

    } else {
     vidMemAddr = (tileNum << 4);
     attribs = TILE_BKG;
    }


    if (tiles[tileNum].invalid(attribs)) {
     tiles[tileNum].validate(videoRam, vidMemAddr, attribs);
    }
    tiles[tileNum].
      draw(back, (8 * x) - xPixelOfs, (8 * y) - yPixelOfs, attribs);
   }
//   System.out.print((8 * y) - yPixelOfs + " ");

  }



 }

 /** Clears the frame buffer to the background colour */
 public void clearFrameBuffer() {
  Graphics back = backBuffer.getGraphics();
  back.setColor(new Color(backgroundPalette.getRgbEntry(0)));
  back.fillRect(0, 0, 160 * mag, 144 * mag);
 }

 public boolean isFrameReady() {
  return (framesDrawn % frameSkip) == 0;
 }

 /** Draw the current graphics frame into the given graphics context */
 public boolean draw(Graphics g, int startX, int startY, Component a) {
  int tileNum;


  calculateFPS();
  if ((framesDrawn % frameSkip) != 0) {
   frameDone = true;
   framesDrawn++;
   return false;
  } else {
   framesDrawn++;
  }
  Graphics back = backBuffer.getGraphics();

/*  g.setColor(new Color(255,0,0));
  g.drawRect(5,5, 10, 10);*/
//  System.out.println("- Drawing");
//  for (int r = 0; r < 384; r++) {
//   if (!spriteTiles[r].valid) System.out.println("Generating image for tile " + r);
//   tiles[r].validate(videoRam, r << 4, backgroundPalette, TILE_BKG);
//  }

/*  for (int r = 0; r < 20; r++) {
   bgTiles[r].draw(g, 8 * r, 0);
  }*/



//  drawSprites(back, 1);


  // Draw bg layer
/*
  int xTileOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x43]) / 8;
  int yTileOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x42]) / 8;
  int xPixelOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x43]) % 8;
  int yPixelOfs = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x42]) % 8;

  int bgStartAddress;
  if (hiBgTileMapAddress) {
   bgStartAddress = 0x1C00;  /* 1C00 
  } else {
   bgStartAddress = 0x1800;
  }

  int tileAddress = 0;
  int attribs = 0;

  
  for (int y = 0; y < 19; y++) {
   for (int x = 0; x < 21; x++) {
    int attributeData = 0;


    tileAddress = bgStartAddress +
       (((y + yTileOfs) % 32) * 32) + ((x + xTileOfs) % 32);
    attributeData = JavaBoy.unsign(videoRam[tileAddress + 0x2000]);

    if (bgWindowDataSelect) {
     tileNum = JavaBoy.unsign(videoRam[tileAddress]);
    } else {
     tileNum = 256 + videoRam[tileAddress];
    }

    if (dmgcpu.gbcFeatures) {
     attribs = (attributeData & 0x07) << 2;

     if ((attributeData & 0x20) != 0) {
      attribs |= TILE_FLIPX;
     }
     if ((attributeData & 0x40) != 0) {
      attribs |= TILE_FLIPY;
     }

    } else {
     attribs = TILE_BKG;
    }

    if (tiles[tileNum + tileStart].invalid(attribs)) {
     tiles[tileNum + tileStart].validate(videoRam, tileNum << 4 + vidMemStart, attribs);
    }
    tiles[tileNum + tileStart].
       draw(back, (8 * x) - xPixelOfs, (8 * y) - yPixelOfs, attribs);
   }
  }
*/


  /* Draw window */
  if (winEnabled) {
   int wx, wy;
   int windowStartAddress;

   if ((dmgcpu.ioHandler.registers[0x40] & 0x40) != 0) {
    windowStartAddress = 0x1C00;
   } else {
    windowStartAddress = 0x1800;
   }
   wx = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x4B]) - 7;
   wy = JavaBoy.unsign(dmgcpu.ioHandler.registers[0x4A]);

   back.setColor(new Color(backgroundPalette.getRgbEntry(0)));
   back.fillRect(wx * mag, wy * mag, 160 * mag, 144 * mag);

   int tileAddress;
   int attribData, attribs, tileDataAddress;

   for (int y = 0; y < 19 - (wy / 8); y++) {
    for (int x = 0; x < 21 - (wx / 8); x++) {
     tileAddress = windowStartAddress + (y * 32) + x;

//     if (!bgWindowDataSelect) {
     if (!savedWindowDataSelect) {
      tileNum = 256 + videoRam[tileAddress];
      } else {
      tileNum = JavaBoy.unsign(videoRam[tileAddress]);
     }
     tileDataAddress = tileNum << 4;

     if (dmgcpu.gbcFeatures) {
      attribData = JavaBoy.unsign(videoRam[tileAddress + 0x2000]);

      attribs = (attribData & 0x07) << 2;

      if ((attribData & 0x08) != 0) {
       tileNum += 384;
       tileDataAddress += 0x2000;
      }

      if ((attribData & 0x20) != 0) {
       attribs |= TILE_FLIPX;
      }
      if ((attribData & 0x40) != 0) {
       attribs |= TILE_FLIPY;
      }

     } else {
      attribs = TILE_BKG;
     }

	 if (wy + y * 8 < windowStopLine) {
      if (tiles[tileNum].invalid(attribs)) {
       tiles[tileNum].validate(videoRam, tileDataAddress, attribs);
      }
      tiles[tileNum].draw(back, wx + x * 8, wy + y * 8, attribs);
     }
	}
   }
  }

  // Draw sprites if the flag was on at any time during this frame
 /* if (spritesEnabledThisFrame) */drawSprites(back, 0);

  if ((spritesEnabled) && (dmgcpu.gbcFeatures)) {
   drawSprites(back, 1);
  }

/*  back.setColor(new Color(255, 255, 255));
  back.fillRect(0, 0, 160, 144);
  for (int r = 0; r < 384; r++) {
   tiles[r].validate(videoRam, r << 4, TILE_BKG);
   tiles[r].draw(back, 8 * (r % 20), 8 * (r / 20), TILE_BKG);
  }*/


  g.drawImage(backBuffer, startX, startY, null);

/*  if (mag == 1) {
   g.drawImage(backBuffer, startX, startY, null);
  } else {
   g.drawImage(backBuffer, startX, startY, width, height, null);
  }*/

  frameDone = true;
  return true;
 }


 /** This class represents a tile in the tile data area.  It
  * contains images for a tile in each of it's three palettes
  * and images that are flipped horizontally and vertically.
  * The images are only created when needed, by calling
  * updateImage().  They can then be drawn by calling draw().
  */
 class GameboyTile {

  Image[] image = new Image[64];

  /** True, if the tile's image in the image[] array is a valid representation of the tile as it
   *  appers in video memory. */
  boolean[] valid = new boolean[64];

  MemoryImageSource[] source = new MemoryImageSource[64];

  /** Current magnification value of Gameboy screen */
  int magnify = 2;
  int[] imageData = new int[64 * magnify * magnify];
  Component a;

  /** Intialize a new Gameboy tile */
  public GameboyTile(Component a) {
   allocateImage(TILE_BKG, a);
   this.a = a;
  }

  /** Allocate memory for the tile image with the specified attributes */
  public void allocateImage(int attribs, Component a) {
   source[attribs] = new MemoryImageSource(8 * magnify, 8 * magnify,
    new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000),
    imageData, 0, 8 * magnify);
   source[attribs].setAnimated(true);
   image[attribs] = a.createImage(source[attribs]);
  }

  /** Free memory used by this tile */
  public void dispose() {                  
   for (int r = 0; r < 64; r++) {
    if (image[r] != null) {
     image[r].flush();
     valid[r] = false;
    }
   }
  }

  /** Returns true if this tile does not contian a valid image for the tile with the specified
   *  attributes
   */
  public boolean invalid(int attribs) {
   return (!valid[attribs]);
  }

  /** Create the image of a tile in the tile cache by reading the relevant data from video
   *  memory
   */
  public void updateImage(byte[] videoRam, int offset, int attribs) {
   int px, py;
   int rgbValue;
 
   if (image[attribs] == null) {
    allocateImage(attribs, a);
   }
 
   GameboyPalette pal;

   if (offset == 0x31E0) {
//	 System.out.println("window updated with " + JavaBoy.hexByte(attribs) + " xflip = " + (attribs & TILE_FLIPX) + "  yflip = " + (attribs & TILE_FLIPY));
   }

   if (dmgcpu.gbcFeatures) {
    if (attribs < 32) {
     pal = gbcBackground[attribs >> 2];
    } else {
     pal = gbcSprite[(attribs >> 2) - 8];
    }
   } else {
    if ((attribs & TILE_OBJ1) != 0) {
     pal = obj1Palette;
    } else if ((attribs & TILE_OBJ2) != 0) {
     pal = obj2Palette;
    } else {
     pal = backgroundPalette;
    }
   }

   for (int y = 0; y < 8; y++) {
    for (int x = 0; x < 8; x++) {

     if ((attribs & TILE_FLIPX) != 0) {
      px = 7 - x;
     } else {
      px = x;
     }
     if ((attribs & TILE_FLIPY) != 0) {
      py = 7 - y;
     } else {
      py = y;
     }

     int pixelColorLower = (videoRam[offset + (py * 2)] & (0x80 >> px)) >> (7 - px);
     int pixelColorUpper = (videoRam[offset + (py * 2) + 1] & (0x80 >> px)) >> (7 - px);

     int entryNumber = (pixelColorUpper * 2) + pixelColorLower;
     int pixelColor = pal.getEntry(entryNumber);

/*     switch (pixelColor) {
      case 0 : rgbValue = 0xFFFFFFFF;
               break;
      case 1 : rgbValue = 0xFFAAAAAA;
               break;
      case 2 : rgbValue = 0xFF555555;
               break;
      default :
      case 3 : rgbValue = 0xFF000000;
               break;
     }*/
     rgbValue = pal.getRgbEntry(entryNumber);

     /* Turn on transparency for background */

     if ((!dmgcpu.gbcFeatures) || ((attribs >> 2) > 7)) {
      if (entryNumber == 0) {
       rgbValue &= 0x00FFFFFF;
      }
     }
/*     if ((entryNumber == 0) &&  ( ( (attribs & TILE_OBJ1) != 0) ||
                                  ( (attribs & TILE_OBJ2) != 0) ) ) {
      rgbValue &= 0x00FFFFFF;
     } else if ((entryNumber == 0) &&
         ((attribs & (TILE_OBJ1 | TILE_OBJ2)) == 0)) {
      rgbValue &= 0x00FFFFFF;
     } */

     for (int cy = 0; cy < magnify; cy++) {
      for (int cx = 0; cx < magnify; cx++) {
       imageData[(y * 8 * magnify * magnify) + (cy * 8 * magnify) +
                 (x * magnify) + cx] = rgbValue;
      }
     }

    }
   }

   source[attribs].newPixels();
   valid[attribs] = true;
  }

  /** Draw the tile with the specified attributes into the graphics context given */
  public void draw(Graphics g, int x, int y, int attribs) {
   g.drawImage(image[attribs], x * magnify, y * magnify, null);
  }

  /** Ensure that the tile is valid */
  public void validate(byte[] videoRam, int offset, int attribs) {
   if (!valid[attribs]) {
    updateImage(videoRam, offset, attribs);
   }
  }

  /** Change the magnification of the tile */
  public void setMagnify(int m) {
   for (int r = 0; r < 64; r++) {
    valid[r] = false;
    source[r] = null;
    if (image[r] != null) {
     image[r].flush();
     image[r] = null;
    }
   }
   magnify = m;
   imageData = new int[64 * magnify * magnify];
  }

  /** Invalidate tile with the specified palette, including all flipped versions. */
  public void invalidate(int attribs) {
   valid[attribs] = false;       /* Invalidate original image and */
   if (image[attribs] != null) image[attribs].flush();
   valid[attribs + 1] = false;   /* all flipped versions in cache */
   if (image[attribs + 1] != null) image[attribs + 1].flush();
   valid[attribs + 2] = false;
   if (image[attribs + 2] != null) image[attribs + 2].flush();
   valid[attribs + 3] = false;
   if (image[attribs + 3] != null) image[attribs + 3].flush();
  }

  /** Invalidate this tile */
  public void invalidate() {
   for (int r = 0; r < 64; r++) {
    valid[r] = false;
    if (image[r] != null) image[r].flush();
    image[r] = null;
   }
  }

 }

}
