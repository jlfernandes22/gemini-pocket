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
  check(ImageDownloadPolicy.plainText("Text/Plain; charset=UTF-8", "notes.txt"), "Plain-text MIME with charset");
  check("text/markdown".equals(ImageDownloadPolicy.textMime("text/plain", "README.md")), "Keep Markdown served as plain text");
  check("text/markdown".equals(ImageDownloadPolicy.textMime("application/octet-stream", "README.markdown")), "Keep Markdown extension with generic MIME");
  check("text/markdown".equals(ImageDownloadPolicy.textMime("text/markdown; charset=UTF-8", null)), "Markdown MIME without filename");
  check("README.markdown".equals(ImageDownloadPolicy.textName("README.markdown", "text/markdown")), "Preserve .markdown");
  check("Gemini-document.md".equals(ImageDownloadPolicy.textName("readme.html", "text/markdown")), "Use safe fallback extension");
  check("# Notes\n".equals(ImageDownloadPolicy.textData("data:text/markdown,%23%20Notes%0A")), "Decode Markdown data URL");
  check(!ImageDownloadPolicy.plainText("text/javascript", "code.md"), "Reject active MIME");
  check(!ImageDownloadPolicy.plainText("application/octet-stream", "../README.md"), "Unsafe name cannot opt generic MIME into text");
  check(ImageDownloadPolicy.plainText("application/octet-stream", "notes.TXT"), "Explicit filename for generic MIME");
  check(!ImageDownloadPolicy.plainText("text/html", "notes.txt"), "HTML is not a text download");
  check(!ImageDownloadPolicy.plainText("application/octet-stream", "notes.txt.apk"), "Do not trust a double extension ending in APK");
  check("notes.txt".equals(ImageDownloadPolicy.textName("notes.TXT", "text/plain")), "Keep the downloaded name");
  check("Gemini-document.txt".equals(ImageDownloadPolicy.textName("../notes.txt", "text/plain")), "Do not use path components");
  check("Gemini-document.txt".equals(ImageDownloadPolicy.textName("note\u202egpj.txt", "text/plain")), "Do not use spoofed filenames");
  check(ImageDownloadPolicy.textData("data:text/plain;charset=utf-8,hello%20caf%C3%A9+%0A").equals("hello café+\n"), "Decode data URL without changing literal plus");
  check(ImageDownloadPolicy.textData("data:text/plain;base64,aGkK").equals("hi\n"), "Decode base64 text URL");
  check(ImageDownloadPolicy.textData("data:text/html,hello")==null, "Reject HTML data URLs");
  check(ImageDownloadPolicy.textData("data:text/plain;base64,/w==")==null, "Reject invalid UTF-8");
  check(ImageDownloadPolicy.textData("data:text/plain,text%00file")==null, "Reject binary controls");
  check(ImageDownloadPolicy.pdf("%PDF-1.7\n".getBytes(StandardCharsets.US_ASCII)), "Accept PDF signature");
  check(!ImageDownloadPolicy.pdf("%PDF-bad".getBytes(StandardCharsets.US_ASCII)), "Reject malformed PDF signature");
  check(!ImageDownloadPolicy.pdf("<html>error".getBytes(StandardCharsets.US_ASCII)), "Reject mislabeled PDF");
  check("Canvas export.pdf".equals(ImageDownloadPolicy.pdfName("Canvas export.PDF")), "Keep safe PDF name");
  check("Gemini-document.pdf".equals(ImageDownloadPolicy.pdfName("../export.pdf")), "Reject path in PDF name");
  System.out.println("PASS: Gemini image, text and PDF download policy");
 }
}
