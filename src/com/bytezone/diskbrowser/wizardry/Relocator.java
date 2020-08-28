package com.bytezone.diskbrowser.wizardry;

import java.util.ArrayList;
import java.util.List;

import com.bytezone.diskbrowser.applefile.AbstractFile;
import com.bytezone.diskbrowser.disk.AppleDisk;
import com.bytezone.diskbrowser.disk.Disk;
import com.bytezone.diskbrowser.disk.DiskAddress;
import com.bytezone.diskbrowser.utilities.Utility;

// -----------------------------------------------------------------------------------//
public class Relocator extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  private final int checkByte;
  private final List<DiskRecord> diskRecords = new ArrayList<> ();

  private final int[] diskBlocks = new int[0x800];
  private final int[] diskOffsets = new int[0x800];

  // ---------------------------------------------------------------------------------//
  public Relocator (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer);

    checkByte = Utility.intValue (buffer[0], buffer[1]);

    int ptr = 2;            // skip checkByte

    while (buffer[ptr] != 0)
    {
      DiskRecord diskRecord = new DiskRecord (buffer, ptr);
      diskRecords.add (diskRecord);
      ptr += diskRecord.size ();
    }

    for (DiskRecord diskRecord : diskRecords)
      for (DiskSegment diskSegment : diskRecord.diskSegments)
        addLogicalBlock ((byte) diskRecord.diskNumber, diskSegment);
  }

  // ---------------------------------------------------------------------------------//
  private void addLogicalBlock (byte disk, DiskSegment diskSegment)
  // ---------------------------------------------------------------------------------//
  {
    int lo = diskSegment.logicalBlock;
    int hi = diskSegment.logicalBlock + diskSegment.segmentLength;

    for (int i = lo, count = 0; i < hi; i++, count++)
    //      if (diskBlocks[i] == 0)       // doesn't matter either way
    {
      diskBlocks[i] = disk;
      diskOffsets[i] = diskSegment.physicalBlock + count;
    }
  }

  // ---------------------------------------------------------------------------------//
  public void createNewBuffer (Disk[] dataDisks)
  // ---------------------------------------------------------------------------------//
  {
    AppleDisk master = (AppleDisk) dataDisks[0];
    //    byte[] key1 = { 0x55, 0x55, 0x15, 0x55 };
    //    byte[] key2 = { 0x00, 0x00, 0x01, 0x41 };
    //    byte[] key3 = { 0x01, 0x00, 0x01, 0x41 };
    //    byte[] key4 = { 0x00, 0x00, 0x15, 0x55 };

    for (int logicalBlock = 0; logicalBlock < diskBlocks.length; logicalBlock++)
    {
      int diskNo = diskBlocks[logicalBlock];
      if (diskNo > 0)
      {
        Disk disk = dataDisks[diskNo];
        byte[] temp = disk.readBlock (diskOffsets[logicalBlock]);
        DiskAddress da = master.getDiskAddress (logicalBlock);
        master.writeBlock (da, temp);
        //        if (da.getBlock () == 0x126)
        //          System.out.println (HexFormatter.format (buffer));
        //        if (Utility.find (temp, key1))
        //          if (Utility.find (temp, key2))
        //            if (Utility.find (temp, key3))
        //              if (Utility.find (temp, key4))
        //                System.out.println (da);
      }
    }
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    text.append ("Pascal Relocator\n\n");
    text.append (String.format ("Check byte..... %04X%n%n", checkByte));

    for (DiskRecord diskRecord : diskRecords)
    {
      text.append (diskRecord);
      text.append ("\n");
    }

    List<String> lines = new ArrayList<> ();
    String heading = " Logical   Size  Disk  Physical";
    String underline = "---------  ----  ----  ---------";

    int first = 0;
    int lastDisk = diskBlocks[0];
    int lastOffset = diskOffsets[0];
    for (int i = 0; i < diskBlocks.length; i++)
    {
      if (diskBlocks[i] != lastDisk || diskOffsets[i] != lastOffset + i - first)
      {
        int size = i - first;
        if (lastDisk > 0)
          lines.add (String.format ("%03X - %03X   %03X    %d   %03X - %03X", first,
              i - 1, size, lastDisk, lastOffset, lastOffset + size - 1));
        else
          lines.add (String.format ("%03X - %03X   %03X", first, i - 1, size));

        first = i;
        lastDisk = diskBlocks[i];
        lastOffset = diskOffsets[i];
      }
    }

    if (lastDisk > 0)
    {
      int max = diskBlocks.length;
      int size = max - first;
      lines.add (String.format ("%03X - %03X   %03X    %d   %03X - %03X", first, max - 1,
          size, lastDisk, lastOffset, lastOffset + size - 1));
    }

    for (int i = lines.size () - 1; i >= 0; i--)
    {
      String line = lines.get (i);
      if (line.length () > 20)
        break;
      lines.remove (i);
    }

    text.append (String.format ("   %s        %s%n   %s       %s%n", heading, heading,
        underline, underline));
    int offset = (lines.size () + 1) / 2;
    //    boolean oddLines = lines.size () % 2 == 1;
    int pairs = lines.size () / 2;

    for (int i = 0; i < pairs; i++)
    {
      text.append (
          String.format ("   %-35s    %s%n", lines.get (i), lines.get (i + offset)));
    }
    if (offset != pairs)
      text.append (String.format ("   %s%n", lines.get (pairs)));

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  private class DiskRecord
  // ---------------------------------------------------------------------------------//
  {
    int diskNumber;
    int totDiskSegments;
    List<DiskSegment> diskSegments = new ArrayList<> ();

    public DiskRecord (byte[] buffer, int ptr)
    {
      diskNumber = Utility.intValue (buffer[ptr], buffer[ptr + 1]);
      totDiskSegments = Utility.intValue (buffer[ptr + 2], buffer[ptr + 4]);

      ptr += 4;
      for (int i = 0; i < totDiskSegments; i++)
      {
        diskSegments.add (new DiskSegment (buffer, ptr));
        ptr += 6;
      }
    }

    int size ()
    {
      return 4 + diskSegments.size () * 6;
    }

    @Override
    public String toString ()
    {
      StringBuilder text = new StringBuilder ();

      text.append (String.format ("Disk number.... %04X%n", diskNumber));
      text.append (String.format ("Segments....... %04X%n%n", totDiskSegments));
      text.append (String.format (" Seg   Skip   Size     Logical      Physical%n"));
      text.append (String.format (" ---   ----   ----   -----------   -----------%n"));

      int count = 1;
      int last = 0;
      int skip = 0;

      for (DiskSegment segment : diskSegments)
      {
        if (segment.logicalBlock > last)
        {
          int end = segment.logicalBlock - 1;
          skip = end - last + 1;
        }
        last = segment.logicalBlock + segment.segmentLength;
        text.append (String.format ("  %02X   %04X  %s %n", count++, skip, segment));
      }

      return text.toString ();
    }
  }

  // ---------------------------------------------------------------------------------//
  private class DiskSegment
  // ---------------------------------------------------------------------------------//
  {
    int logicalBlock;
    int physicalBlock;
    int segmentLength;

    public DiskSegment (byte[] buffer, int ptr)
    {
      logicalBlock = Utility.intValue (buffer[ptr], buffer[ptr + 1]);
      physicalBlock = Utility.intValue (buffer[ptr + 2], buffer[ptr + 3]);
      segmentLength = Utility.intValue (buffer[ptr + 4], buffer[ptr + 5]);
    }

    @Override
    public String toString ()
    {
      return String.format (" %04X   %04X - %04X   %04X - %04X", segmentLength,
          logicalBlock, (logicalBlock + segmentLength - 1), physicalBlock,
          (physicalBlock + segmentLength - 1));
    }
  }
}