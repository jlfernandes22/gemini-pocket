package com.geminipocket;
import java.io.File;
/** Only stale app-generated attachments, never WebView cache or gallery files. */
public final class TempPhotos {
 public static final long MAX_AGE=24L*60*60*1000;
 private TempPhotos(){}
 public static void clean(File dir,long now){File[] files=dir.listFiles();if(files==null)return;for(File f:files)if(f.isFile()&&UploadPolicy.validName(f.getName())&&f.lastModified()>0&&now-f.lastModified()>MAX_AGE)f.delete();}
}
