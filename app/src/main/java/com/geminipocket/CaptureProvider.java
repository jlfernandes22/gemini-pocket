package com.geminipocket;
import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Only explicitly granted capture URIs are visible to external camera apps. */
public class CaptureProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 private File file(Uri u) throws FileNotFoundException {
  String n=u.getLastPathSegment();
  if(!"com.geminipocket.capture".equals(u.getAuthority()) || n==null || !UploadPolicy.validName(n) || !u.getPath().equals("/"+n)) throw new FileNotFoundException();
  return new File(getContext().getCacheDir(),n);
 }
 public ParcelFileDescriptor openFile(Uri u,String mode) throws FileNotFoundException {
  File f=file(u);
  return ParcelFileDescriptor.open(f,ParcelFileDescriptor.parseMode(mode));
 }
 public String getType(Uri u){String ext=android.webkit.MimeTypeMap.getFileExtensionFromUrl(u.toString());String type=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(java.util.Locale.ROOT));return type==null?"application/octet-stream":type;}
 public Cursor query(Uri u,String[] projection,String selection,String[] args,String sort){
  try { File f=file(u); String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
   MatrixCursor c=new MatrixCursor(cols); Object[] row=new Object[cols.length];
   for(int i=0;i<cols.length;i++) {if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))row[i]=f.getName(); else if(OpenableColumns.SIZE.equals(cols[i]))row[i]=f.length();}
   c.addRow(row);return c;
  } catch(Exception e){return null;}
 }
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
