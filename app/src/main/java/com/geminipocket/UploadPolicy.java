package com.geminipocket;
public final class UploadPolicy {
 public static final int PHOTOS=1,CAMERA=2,FILES=3;
 private UploadPolicy(){}
 public static int route(String[] types,boolean capture){
  boolean visual=false,other=false;
  if(types!=null)for(String group:types)if(group!=null)for(String t:group.split(",")){t=t.trim();if(t.startsWith("image/") || t.startsWith("video/"))visual=true;else if(!t.isEmpty() && !t.equals("*/*"))other=true;}
  if(capture && !other)return CAMERA;
  return !other ? PHOTOS : FILES;
 }
 public static int route(String[] types,boolean capture,String action){
  if(capture || "camera".equals(action))return CAMERA;
  if("files".equals(action))return FILES;
  if("photos".equals(action))return PHOTOS;
  return route(types,capture);
 }
 public static boolean validName(String n){return n!=null && n.matches("(?:capture_[0-9]+\\.jpg|upload_[0-9]+\\.[A-Za-z0-9]{1,12})");}
}
