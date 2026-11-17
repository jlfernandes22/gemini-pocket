package com.geminipocket;

import java.io.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;
import java.util.function.IntConsumer;

/** Tested byte transfer shared by attachment preparation and update downloads. */
public final class Transfer {
 private Transfer(){}
 public static long copy(InputStream in,OutputStream out,long limit,BooleanSupplier cancelled,LongConsumer progress)throws IOException{
  if(in==null)throw new IOException("Unreadable source");long size=0;byte[] buffer=new byte[32768];int count;
  while((count=in.read(buffer))!=-1){if(cancelled.getAsBoolean())throw new IOException("Cancelled");size+=count;if(size>limit)throw new IOException("File exceeds allowed size");out.write(buffer,0,count);if(progress!=null)progress.accept(size);}
  if(cancelled.getAsBoolean())throw new IOException("Cancelled");if(size==0)throw new IOException("Empty file");return size;
 }
 public static void download(InputStream in,OutputStream out,long expected,String sha,BooleanSupplier cancelled,IntConsumer progress)throws Exception{
  if(expected<=0||expected>100L*1024*1024)throw new IOException("Unexpected download size");
  MessageDigest digest=MessageDigest.getInstance("SHA-256");InputStream checked=new java.security.DigestInputStream(in,digest);final int[] previous={-1};
  long received=copy(checked,out,expected,cancelled,size->{int p=(int)(size*100/expected);if(p!=previous[0]){previous[0]=p;progress.accept(p);}});
  StringBuilder hash=new StringBuilder();for(byte b:digest.digest())hash.append(String.format(Locale.ROOT,"%02x",b&255));
  if(received!=expected||!sha.equalsIgnoreCase(hash.toString()))throw new IOException("Download verification failed");
 }
}
