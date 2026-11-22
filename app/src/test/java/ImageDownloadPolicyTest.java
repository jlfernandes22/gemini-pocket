import com.geminipocket.ImageDownloadPolicy;
import java.nio.charset.StandardCharsets;

public final class ImageDownloadPolicyTest {
 private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
 private static byte[] bytes(int... values) {
  byte[] out = new byte[values.length];
  for (int i=0;i<values.length;i++) out[i]=(byte)values[i];
  return out;
 }
 public static void main(String[] args) {
  String home="https://gemini.google.com/app";
  check(ImageDownloadPolicy.allowedPage(home), "Only Gemini top-level page can request images");
  check(!ImageDownloadPolicy.allowedPage("https://accounts.google.com/"), "Sign-in cannot request images");
  check(ImageDownloadPolicy.https("https://lh3.googleusercontent.com/gg-dl/sample"), "Google image delivery");
  check(ImageDownloadPolicy.https("https://lh3.google.com/gg-dl/sample"), "Gemini original-image delivery");
  check(!ImageDownloadPolicy.https("https://lh3.google.com.evil.test/gg-dl/sample"), "Reject image-host lookalikes");
  check(ImageDownloadPolicy.https("https://gemini.google.com/download?id=sample"), "Gemini attachment URL");
  for (String url : new String[]{"http://gemini.google.com/image", "https://lh3.googleusercontent.com.evil.test/image", "https://evil.test/image", "https://user@gemini.google.com/image", "https://gemini.google.com:8443/image", "https://accounts.google.com/image", "https://gemini.google.com\\@evil.test/image", "content://gemini.google.com/image"})
   check(!ImageDownloadPolicy.https(url), "Unexpected URL allowed: "+url);
  check(ImageDownloadPolicy.blob("blob:https://gemini.google.com/d1a4c320-70fa-4720-82b6-ad2523dd02dd"), "Gemini blob URL");
  check(ImageDownloadPolicy.blob("blob:null/d1a4c320-70fa-4720-82b6-ad2523dd02dd"), "WebView reports Gemini's downloaded image as an opaque blob");
  check(!ImageDownloadPolicy.blob("blob:null/d1a4c320-70fa-4720-82b6-ad2523dd02dd?x"), "Opaque blob must be exactly a UUID");
  check(!ImageDownloadPolicy.blob("blob:https://gemini.google.com.evil.test/d1a4c320-70fa-4720-82b6-ad2523dd02dd"), "Reject other origins");
  check(!ImageDownloadPolicy.blob("blob:https://gemini.google.com/read-me"), "Reject non-blob path");
  check("image/png".equals(ImageDownloadPolicy.mime(bytes(137,80,78,71,13,10,26,10))), "PNG signature");
  check("image/jpeg".equals(ImageDownloadPolicy.mime(bytes(255,216,255))), "JPEG signature");
  check("image/webp".equals(ImageDownloadPolicy.mime("RIFF....WEBP".getBytes(StandardCharsets.US_ASCII))), "WebP signature");
  check("image/gif".equals(ImageDownloadPolicy.mime("GIF89a".getBytes(StandardCharsets.US_ASCII))), "GIF signature");
  check(ImageDownloadPolicy.mime("<html>login required".getBytes(StandardCharsets.US_ASCII))==null, "Never export a sign-in page as an image");
  check(ImageDownloadPolicy.mime(bytes(137,80,78,71))==null, "Reject truncated signature");
  System.out.println("PASS: Gemini image download origins, blob URLs and raster image signatures");
 }
}
