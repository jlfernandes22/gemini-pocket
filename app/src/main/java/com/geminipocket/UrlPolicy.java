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
  String h=host(url); return h!=null && (!restricted || navHost(h) || verification(url,h) || signInHandoff(url,h));
 }
 private static boolean verification(String url,String h){
  try{return h.equals("www.google.com") && new URI(url).getRawPath().startsWith("/sorry/");}catch(Exception e){return false;}
 }
 private static boolean signInHandoff(String url,String h){
  try{return GoogleDomains.accountHost(h)&&"/accounts/SetSID".equals(new URI(url).getRawPath());}catch(Exception e){return false;}
 }
 public static String displayOrigin(String url){String h=host(url);return h==null?"Unsupported address":"https://"+h;}
 // Gemini's LaTeX/PDF Canvas renders HTML in this SafeContentFrame shim,
 // then the shim navigates to a blob on its own HTTPS origin.
 private static final java.util.regex.Pattern PREVIEW_HOST = java.util.regex.Pattern.compile("[a-z0-9]{50}-h[0-9]{1,11}\\.scf\\.usercontent\\.goog");
 private static final java.util.regex.Pattern PREVIEW_BLOB_PATH = java.util.regex.Pattern.compile("/[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
 private static boolean documentPreview(String url) {
  if(url==null)return false;
  boolean blob=url.startsWith("blob:");
  String address=blob?url.substring(5):url;
  String h=host(address);
  if(h==null || !PREVIEW_HOST.matcher(h).matches())return false;
  URI uri=URI.create(address); // host() already validated URI syntax.
  String path=uri.getRawPath();
  return blob ? uri.getRawQuery()==null && PREVIEW_BLOB_PATH.matcher(path).matches()
              : "/gemini-code-immersive/shim.html".equals(path);
 }
 public static boolean frameNavigation(String url,boolean restricted) {
  return navigation(url,restricted) || documentPreview(url);
 }
 public static boolean resource(String url,boolean restricted) {
  if(documentPreview(url))return true;
  String h=host(url);
  return h!=null && (!restricted || navHost(h) || signInHandoff(url,h) || domain(h,"gstatic.com") || domain(h,"googleusercontent.com") || domain(h,"googleapis.com") || h.equals("www.google.com") || h.equals("speechs3proto2-pa.clients6.google.com") || h.equals("geminiweb-pa.clients6.google.com") || h.equals("waa-pa.clients6.google.com") || h.equals("signaler-pa.clients6.google.com") || h.equals("push.clients6.google.com") || h.equals("upload.clients6.google.com") || h.equals("apis.google.com") || h.equals("ssl.google-analytics.com"));
 }
 public static boolean mediaOrigin(String url) { return "gemini.google.com".equals(host(url)); }
}
