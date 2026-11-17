package com.geminipocket;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import java.io.*;
import java.net.*;
import java.util.Arrays;

/** Update transport and APK trust checks; owns no Activity or UI state. */
public final class UpdateClient {
 public static final String LATEST="https://api.github.com/repos/jlfernandes22/gemini-pocket/releases?per_page=100";
 private UpdateClient(){}
 public static long versionCode(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
 private static boolean metadataUrl(String address){return address.matches("https://api\\.github\\.com/repos/jlfernandes22/gemini-pocket/releases\\?per_page=100(?:&page=[1-9][0-9]{0,2})?");}
 public static org.json.JSONObject latestRelease()throws Exception{
  org.json.JSONObject best=null;long bestCode=-1;
  for(int page=1;page<=100;page++){
   HttpURLConnection c=request(LATEST+"&page="+page);String json;
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Release metadata too large");out.write(buffer,0,n);}json=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}finally{c.disconnect();}
   org.json.JSONArray releases=new org.json.JSONArray(json);
   for(int index=0;index<releases.length();index++){org.json.JSONObject candidate=releases.getJSONObject(index);long code=UpdatePolicy.publishedCode(candidate.optString("tag_name"));if(!candidate.optBoolean("draft")&&!candidate.optBoolean("prerelease")&&code>bestCode){best=candidate;bestCode=code;}}
   if(releases.length()<100){if(best==null)throw new IOException("No supported public release found");return best;}
  }throw new IOException("Release history exceeds safe scan limit; check Releases manually");
 }
 public static HttpURLConnection request(String address)throws IOException{
  for(int hop=0;hop<5;hop++){
   if(!metadataUrl(address)&&!UpdatePolicy.downloadUrl(address))throw new IOException("Untrusted update URL");
   HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();
   try{
    c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Gemini-Pocket-Updater");int code=c.getResponseCode();
    if(code>=300&&code<400){String next=c.getHeaderField("Location");if(next==null)throw new IOException("Missing redirect");address=new URL(new URL(address),next).toString();c.disconnect();continue;}
    if(code!=200)throw new IOException("GitHub HTTP "+code);return c;
   }catch(IOException e){c.disconnect();throw e;}
  }throw new IOException("Too many redirects");
 }
 @SuppressWarnings("deprecation") public static void verify(Context context,File apk)throws Exception{
  PackageManager manager=context.getPackageManager();String name=context.getPackageName();
  PackageInfo current=manager.getPackageInfo(name,PackageManager.GET_SIGNATURES),candidate=manager.getPackageArchiveInfo(apk.getPath(),PackageManager.GET_SIGNATURES);
  if(candidate==null||!name.equals(candidate.packageName)||versionCode(candidate)<=versionCode(current)||current.signatures==null||candidate.signatures==null||current.signatures.length==0||!Arrays.equals(current.signatures,candidate.signatures))throw new SecurityException("APK identity, version or signing certificate does not match");
 }
}
