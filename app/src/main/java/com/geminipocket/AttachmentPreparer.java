package com.geminipocket;

import android.content.ContentResolver;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Resolves and copies selected content; Activity owns lifecycle, permissions and callback. */
public final class AttachmentPreparer {
 private AttachmentPreparer(){}
 public static List<Uri> prepare(ContentResolver resolver,File cache,List<Uri> selected,BooleanSupplier cancelled)throws IOException{
  ArrayList<Uri> ready=new ArrayList<>();ArrayList<File> created=new ArrayList<>();
  try{
   for(Uri uri:selected){
    if(!"content".equals(uri.getScheme()))throw new IOException("Unsupported source");
    String type=resolver.getType(uri);String ext=type==null?null:MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
    if(ext==null||!ext.matches("[A-Za-z0-9]{1,12}"))ext="bin";
    File copy=File.createTempFile("upload_","."+ext,cache);created.add(copy);
    try(InputStream in=resolver.openInputStream(uri);OutputStream out=new FileOutputStream(copy)){Transfer.copy(in,out,100L*1024*1024,cancelled,null);}
    ready.add(Uri.parse("content://com.geminipocket.capture/"+copy.getName()));
   }
   return ready;
  }catch(Exception e){for(File file:created)file.delete();if(e instanceof IOException)throw (IOException)e;throw new IOException("Cannot prepare attachment",e);}
 }
}
