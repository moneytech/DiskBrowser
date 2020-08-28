package com.bytezone.diskbrowser.applefile;

// -----------------------------------------------------------------------------------//
public class PascalInfo extends AbstractFile
// -----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  public PascalInfo (String name, byte[] buffer)
  // ---------------------------------------------------------------------------------//
  {
    super (name, buffer);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String getText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (getHeader ());

    for (int i = 0; i < buffer.length; i++)
      if (buffer[i] == 0x0D)
        text.append ("\n");
      else
        text.append ((char) buffer[i]);

    return text.toString ();
  }
}