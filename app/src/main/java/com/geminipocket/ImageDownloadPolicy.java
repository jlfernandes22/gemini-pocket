package com.geminipocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
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

 static String baseMime(String mime) {
  return mime == null ? "" : mime.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
 }

 private static String textExtension(String name) {
  if (name == null || !name.matches("[\\p{L}\\p{M}\\p{N}_ .()\\-]{1,120}") || name.startsWith(".") || name.startsWith(" ")) return "";
  int dot = name.lastIndexOf('.');
  if (dot < 1) return "";
  String ext = name.substring(dot).toLowerCase(Locale.ROOT);
  return ext.matches("\\.(txt|md|markdown)") ? ext : "";
 }

 public static boolean plainText(String mime, String name) { return textMime(mime, name) != null; }

 public static String textMime(String mime, String name) {
  String type = baseMime(mime), ext = textExtension(name);
  if ("text/markdown".equals(type) || "text/x-markdown".equals(type)) return "text/markdown";
  if ("text/plain".equals(type) || (!ext.isEmpty() && (type.isEmpty() || "application/octet-stream".equals(type))))
   return ".md".equals(ext) || ".markdown".equals(ext) ? "text/markdown" : "text/plain";
  return null;
 }

 public static String textName(String name, String mime) {
  String ext = textExtension(name);
  boolean markdown = "text/markdown".equals(mime);
  if (ext.isEmpty() || markdown == ".txt".equals(ext)) return "Gemini-document" + (markdown ? ".md" : ".txt");
  return name.substring(0, name.lastIndexOf('.')) + ext;
 }

 static String dataMime(String url) {
  int comma = url == null ? -1 : url.indexOf(',');
  if (comma < 0 || comma > 80 || !url.startsWith("data:")) return null;
  String metadata = url.substring(5, comma).toLowerCase(Locale.ROOT);
  return metadata.matches("text/(plain|markdown|x-markdown)(;charset=utf-8)?(;base64)?") ? baseMime(metadata) : null;
 }

 // ponytail: cap inline URLs at 1 MiB decoded; larger files should use a blob or HTTPS.
 public static String textData(String url) {
  if (url == null || url.length() > 3 * 1024 * 1024 + 80 || dataMime(url) == null) return null;
  try {
   int comma = url.indexOf(',');
   String encoded = url.substring(comma + 1);
   String octets = new String(encoded.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
   byte[] bytes = URLDecoder.decode(octets.replace("+", "%2B"), "ISO-8859-1").getBytes(StandardCharsets.ISO_8859_1);
   if (url.substring(5, comma).toLowerCase(Locale.ROOT).endsWith(";base64")) bytes = java.util.Base64.getDecoder().decode(bytes);
   if (bytes.length == 0 || bytes.length > 1024 * 1024) return null;
   if (!validText(new ByteArrayInputStream(bytes))) return null;
   return new String(bytes, StandardCharsets.UTF_8);
  } catch (IOException | IllegalArgumentException e) { return null; }
 }

 static boolean validText(InputStream in) throws IOException {
  InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8.newDecoder()
    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT));
  char[] buffer = new char[4096];
  for (int count; (count = reader.read(buffer)) != -1; ) {
   for (int i = 0; i < count; i++) {
    char c = buffer[i];
    if (Character.isISOControl(c) && c != '\t' && c != '\n' && c != '\r') return false;
   }
  }
  return true;
 }

 static String extension(String mime) { return "image/jpeg".equals(mime) ? "jpg" : mime.substring("image/".length()); }
}
