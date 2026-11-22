package com.geminipocket;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Narrower than the WebView resource allowlist. Does not grant navigation. */
public final class ImageDownloadPolicy {
 static final String ORIGIN = "https://gemini.google.com";
 static final long MAX_BYTES = 32L * 1024 * 1024;
 private static final Pattern CDN = Pattern.compile("lh[0-9]+\\.googleusercontent\\.com");
 private static final Pattern BLOB_PATH = Pattern.compile("/[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");

 private ImageDownloadPolicy() {}

 public static boolean allowedPage(String url) { return UrlPolicy.mediaOrigin(url); }

 public static boolean https(String url) {
  if (allowedPage(url)) return true;
  try {
   String host = new URI(url).getHost();
   if (host == null) return false;
   String normalized = host.toLowerCase(Locale.ROOT);
   return (CDN.matcher(normalized).matches() || UrlPolicy.originalImageHost(normalized)) && UrlPolicy.resource(url, true);
  } catch (Exception e) { return false; }
 }

 public static boolean blob(String url) {
  if (url == null) return false;
  String prefix = url.startsWith("blob:null/") ? "blob:null" : "blob:" + ORIGIN;
  return url.startsWith(prefix + "/") && BLOB_PATH.matcher(url.substring(prefix.length())).matches();
 }

 private static boolean starts(byte[] data, int offset, int... signature) {
  if (data.length < offset + signature.length) return false;
  for (int i=0; i<signature.length; i++) if ((data[offset+i] & 255) != signature[i]) return false;
  return true;
 }

 public static String mime(byte[] header) {
  if (starts(header, 0, 137, 'P', 'N', 'G', 13, 10, 26, 10)) return "image/png";
  if (starts(header, 0, 255, 216, 255)) return "image/jpeg";
  if (starts(header, 0, 'R', 'I', 'F', 'F') && starts(header, 8, 'W', 'E', 'B', 'P')) return "image/webp";
  if (starts(header, 0, 'G', 'I', 'F', '8', '7', 'a') || starts(header, 0, 'G', 'I', 'F', '8', '9', 'a')) return "image/gif";
  return null;
 }

 static String extension(String mime) { return "image/jpeg".equals(mime) ? "jpg" : mime.substring("image/".length()); }
}
