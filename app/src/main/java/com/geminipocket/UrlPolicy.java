package com.geminipocket;
import java.net.URI;
import java.util.Locale;

/** Host policy, deliberately separate from Android for deterministic tests. */
public final class UrlPolicy {
 private UrlPolicy() {}
 private static String host(String url) {
  try {
   if(url==null || url.indexOf('\\')>=0) return null;
   URI u=new URI(url);
   if(!"https".equalsIgnoreCase(u.getScheme()) || u.getRawUserInfo()!=null || (u.getPort()!=-1 && u.getPort()!=443)) return null;
   String h=u.getHost();
   return h==null ? null : h.toLowerCase(Locale.ROOT);
  } catch(Exception e) { return null; }
 }
 private static boolean domain(String h,String base) { return h.equals(base) || h.endsWith("."+base); }
 private static boolean navHost(String h) {
  return h.equals("gemini.google.com") || h.equals("accounts.google.com") || h.equals("consent.google.com") || h.equals("myaccount.google.com");
 }
 public static boolean navigation(String url,boolean restricted) {
  String h=host(url); return h!=null && (!restricted || navHost(h) || verification(url,h));
 }
 private static boolean verification(String url,String h){
  try{return h.equals("www.google.com") && new URI(url).getRawPath().startsWith("/sorry/");}catch(Exception e){return false;}
 }
 public static boolean resource(String url,boolean restricted) {
  String h=host(url);
  return h!=null && (!restricted || navHost(h) || domain(h,"gstatic.com") || domain(h,"googleusercontent.com") || domain(h,"googleapis.com") || h.equals("www.google.com") || h.equals("speechs3proto2-pa.clients6.google.com") || h.equals("geminiweb-pa.clients6.google.com") || h.equals("waa-pa.clients6.google.com") || h.equals("signaler-pa.clients6.google.com") || h.equals("push.clients6.google.com") || h.equals("upload.clients6.google.com") || h.equals("apis.google.com") || h.equals("ssl.google-analytics.com"));
 }
 public static boolean mediaOrigin(String url) { return "gemini.google.com".equals(host(url)); }
}
