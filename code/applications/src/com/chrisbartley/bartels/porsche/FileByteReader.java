package com.chrisbartley.bartels.porsche;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import edu.cmu.ri.createlab.util.ByteUtils;

/**
 * @author Chris Bartley (bartley@cmu.edu)
 */
public class FileByteReader
   {
   private static final String FILE = "/org/chargecar/bms/bartels-bms.bin";

   public static void main(final String[] args) throws IOException
      {
      final FileByteReader fileByteReader = new FileByteReader();
      fileByteReader.read(FILE);
      }

   private void read(final String filePath) throws IOException
      {
      final InputStream is = this.getClass().getResourceAsStream(filePath);
      final InputStream bis = new BufferedInputStream(is);

      while (true)
         {
         final int i = bis.read();
         if (i >= 0)
            {
            final byte b = (byte)i;
            final char c = (char)b;

            System.out.printf("%4d  %4s  %4d ---> ", b, c, ByteUtils.unsignedByteToInt((byte)i));
            System.out.println(b + " = [" + (i >= 32 ? c : "?") + "] " + ByteUtils.unsignedByteToInt((byte)i));
            }
         else
            {
            System.out.println("End of stream.");
            break;
            }
         }
      }
   }