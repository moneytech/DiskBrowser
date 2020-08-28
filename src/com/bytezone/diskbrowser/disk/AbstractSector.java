package com.bytezone.diskbrowser.disk;

import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.bytezone.diskbrowser.applefile.AssemblerProgram;
import com.bytezone.diskbrowser.gui.DataSource;
import com.bytezone.diskbrowser.utilities.HexFormatter;

// -----------------------------------------------------------------------------------//
public abstract class AbstractSector implements DataSource
// -----------------------------------------------------------------------------------//
{
  private static String newLine = String.format ("%n");
  private static String newLine2 = newLine + newLine;

  final public byte[] buffer;
  protected Disk disk;
  protected DiskAddress diskAddress;
  AssemblerProgram assembler;
  String description;

  // ---------------------------------------------------------------------------------//
  public AbstractSector (Disk disk, byte[] buffer, DiskAddress diskAddress)
  // ---------------------------------------------------------------------------------//
  {
    this.buffer = buffer;
    this.disk = disk;
    this.diskAddress = diskAddress;
  }

  // ---------------------------------------------------------------------------------//
  public AbstractSector (Disk disk, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    this.buffer = buffer;
    this.disk = disk;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getAssembler ()
  // ---------------------------------------------------------------------------------//
  {
    if (assembler == null)
      assembler = new AssemblerProgram ("noname", buffer, 0);
    return assembler.getText ();
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getHexDump ()
  // ---------------------------------------------------------------------------------//
  {
    return HexFormatter.format (buffer, 0, buffer.length);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public BufferedImage getImage ()
  // ---------------------------------------------------------------------------------//
  {
    return null;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getText ()
  // ---------------------------------------------------------------------------------//
  {
    if (description == null)
      description = createText ();
    return description;
  }

  // ---------------------------------------------------------------------------------//
  public abstract String createText ();
  // ---------------------------------------------------------------------------------//

  // ---------------------------------------------------------------------------------//
  protected StringBuilder getHeader (String title)
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append (title + newLine2);
    text.append ("Offset    Value         Description" + newLine);
    text.append ("=======   ===========   "
        + "===============================================================" + newLine);
    return text;
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public JComponent getComponent ()
  // ---------------------------------------------------------------------------------//
  {
    JPanel panel = new JPanel ();
    return panel;
  }

  // ---------------------------------------------------------------------------------//
  protected void addText (StringBuilder text, byte[] buffer, int offset, int size,
      String desc)
  // ---------------------------------------------------------------------------------//
  {
    if ((offset + size - 1) > buffer.length)
      return;

    switch (size)
    {
      case 1:
        text.append (String.format ("%03X       %02X            %s%n", offset,
            buffer[offset], desc));
        break;
      case 2:
        text.append (String.format ("%03X-%03X   %02X %02X         %s%n", offset,
            offset + 1, buffer[offset], buffer[offset + 1], desc));
        break;
      case 3:
        text.append (String.format ("%03X-%03X   %02X %02X %02X      %s%n", offset,
            offset + 2, buffer[offset], buffer[offset + 1], buffer[offset + 2], desc));
        break;
      case 4:
        text.append (String.format ("%03X-%03X   %02X %02X %02X %02X   %s%n", offset,
            offset + 3, buffer[offset], buffer[offset + 1], buffer[offset + 2],
            buffer[offset + 3], desc));
        break;
      default:
        System.out.println ("Invalid length : " + size);
    }
  }

  // ---------------------------------------------------------------------------------//
  protected void addTextAndDecimal (StringBuilder text, byte[] b, int offset, int size,
      String desc)
  // ---------------------------------------------------------------------------------//
  {
    if (size == 1)
      desc += " (" + (b[offset] & 0xFF) + ")";
    else if (size == 2)
      desc +=
          String.format (" (%,d)", ((b[offset + 1] & 0xFF) * 256 + (b[offset] & 0xFF)));
    else if (size == 3)
      desc += String.format (" (%,d)", ((b[offset + 2] & 0xFF) * 65536)
          + ((b[offset + 1] & 0xFF) * 256) + (b[offset] & 0xFF));

    addText (text, b, offset, size, desc);
  }
}