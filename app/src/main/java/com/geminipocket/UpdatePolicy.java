package com.geminipocket;
import java.net.URI;
public final class UpdatePolicy {
 private UpdatePolicy(){}
 public static boolean downloadUrl(String value){try{URI u=new URI(value);String h=u.getHost();String path=u.getRawPath();if(path==null||path.contains("..")||path.toLowerCase(java.util.Locale.ROOT).contains("%2e")||path.contains("\\"))return false;return "https".equals(u.getScheme())&&u.getRawUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&("github.com".equals(h)&&u.getPath().startsWith("/jlfernandes22/gemini-pocket/releases/download/")||"release-assets.githubusercontent.com".equals(h));}catch(Exception e){return false;}}
 public static long publishedCode(String tag){try{return tag.matches("build-[0-9]+")?300000L+Long.parseLong(tag.substring(6)):-1;}catch(Exception e){return -1;}}
}
