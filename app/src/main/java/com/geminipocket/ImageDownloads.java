package com.geminipocket;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebMessage;
import android.webkit.WebMessagePort;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Stages an explicit WebView download, then exports it through Android's save picker. */
final class ImageDownloads {
 static final int SAVE_FILE = 14;
 private static final int CHUNK_BYTES = 32768;
 private static final String FILE_PREFIX = "gemini-download-";
 private static final String FAILED = "Could not download this file. Try again or use Browser session.";
 private final Activity activity;
 private final Handler ui = new Handler(Looper.getMainLooper());
 private final Handler blobWorker;
 private final android.os.HandlerThread blobThread;
 private Job job;

 private static final class Job {
  final WebView page;
  final Operation operation = new Operation();
  final File file;
  String name;
  boolean image;
  volatile boolean picking;
  WebMessagePort port;
  Job(WebView page, File file) { this.page = page; this.file = file; }
 }

 ImageDownloads(Activity activity) {
  this.activity = activity;
  blobThread = new android.os.HandlerThread("GeminiImageBlob");
  blobThread.start();
  blobWorker = new Handler(blobThread.getLooper());
 }

 static void clean(File cache, long now) {
  File[] files = cache.listFiles();
  if (files == null) return;
  for (File file : files) {
   if (file.getName().startsWith(FILE_PREFIX) && file.getName().endsWith(".tmp") &&
       file.lastModified() > 0 && now - file.lastModified() > TempPhotos.MAX_AGE) file.delete();
  }
 }

 void start(WebView page, String url, String agent, String disposition, String mime, long length) {
  if (!ImageDownloadPolicy.allowedPage(page.getUrl())) return;
  if (job != null) { message("Finish the current download first."); return; }
  boolean blob = ImageDownloadPolicy.blob(url);
  String inlineMime = ImageDownloadPolicy.dataMime(url);
  if (!blob && inlineMime == null && !ImageDownloadPolicy.https(url)) { message("This download isn't supported in Isolated mode. Try Browser session."); return; }
  if (length > ImageDownloadPolicy.MAX_BYTES) { message("File exceeds the 32 MB download limit."); return; }
  // Use disposition before any MIME inference: Android may otherwise turn .md into .txt.
  String name = URLUtil.guessFileName(blob || inlineMime != null ? "" : url, disposition, null);
  String type = ImageDownloadPolicy.baseMime(mime);
  if (blob && url.startsWith("blob:null/") && (length <= 0 ||
      (!type.matches("image/(jpeg|png|webp|gif)") && !ImageDownloadPolicy.plainText(type, name)))) {
   message(FAILED); return;
  }
  Job current = new Job(page, new File(activity.getCacheDir(), FILE_PREFIX + UUID.randomUUID() + ".tmp"));
  current.name = name;
  job = current;
  ui.postDelayed(() -> { if (job == current && !current.picking) fail(current); }, 50000);
  if (blob) receiveBlob(current, url, type, length);
  else new Thread(() -> {
   try {
    if (inlineMime != null) {
     String text = ImageDownloadPolicy.textData(url);
     if (text == null || !current.operation.active()) throw new IOException("Unreadable text file");
     try (OutputStream out = new FileOutputStream(current.file)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
     ready(current, inlineMime, false);
    } else {
     ready(current, fetch(current, url, agent), false);
    }
   } catch (IOException | RuntimeException e) { ui.post(() -> fail(current)); }
   finally { if (!current.operation.active()) current.file.delete(); }
  }, "GeminiFileFetch").start();
 }

 private String fetch(Job current, String url, String agent) throws IOException {
  for (int hops = 0; hops <= 4; hops++) {
   if (!current.operation.active() || !ImageDownloadPolicy.https(url)) throw new IOException("Blocked download URL");
   HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
   try {
    conn.setInstanceFollowRedirects(false);
    conn.setConnectTimeout(12000);
    conn.setReadTimeout(15000);
    conn.setRequestProperty("Accept", "image/*,text/plain,text/markdown,application/octet-stream;q=0.5");
    conn.setRequestProperty("Referer", ImageDownloadPolicy.ORIGIN + "/");
    if (agent != null && !agent.isEmpty()) conn.setRequestProperty("User-Agent", agent);
    // Look up cookies for this validated request only; never forward cookies from a previous hop.
    String cookies = CookieManager.getInstance().getCookie(url);
    if (cookies != null && !cookies.isEmpty()) conn.setRequestProperty("Cookie", cookies);
    int response = conn.getResponseCode();
    if (response == 301 || response == 302 || response == 303 || response == 307 || response == 308) {
     String redirect = conn.getHeaderField("Location");
     if (redirect == null) throw new IOException("Redirect missing location");
     url = URI.create(url).resolve(redirect).toString();
     continue;
    }
    if (response != 200 || conn.getContentLengthLong() > ImageDownloadPolicy.MAX_BYTES) throw new IOException("Invalid download response");
    String type = ImageDownloadPolicy.baseMime(conn.getContentType());
    String disposition = conn.getHeaderField("Content-Disposition");
    if (disposition != null) current.name = URLUtil.guessFileName(url, disposition, null);
    if (!ImageDownloadPolicy.plainText(type, current.name) && !type.matches("image/.*|application/octet-stream|"))
     throw new IOException("Unsupported download response");
    if (!current.operation.active()) throw new IOException("Cancelled");
    try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(current.file)) {
     Transfer.copy(in, out, ImageDownloadPolicy.MAX_BYTES, () -> !current.operation.active(), null);
    }
    return type;
   } finally { conn.disconnect(); }
  }
  throw new IOException("Too many redirects");
 }

 private void receiveBlob(Job current, String url, String mime, long length) {
  boolean opaque = url.startsWith("blob:null/");
  final String script;
  try (InputStream in = activity.getAssets().open("image-download.js"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
   Transfer.copy(in, out, 8192, () -> false, null);
   String key = UUID.randomUUID().toString();
   JSONObject request = new JSONObject().put("url", url).put("key", key).put("limit", ImageDownloadPolicy.MAX_BYTES)
     .put("type", mime).put("size", length).put("name", current.name);
   script = new String(out.toByteArray(), StandardCharsets.UTF_8).replace("/* REQUEST */ null", request.toString());
   WebMessagePort[] channel = current.page.createWebMessageChannel();
   current.port = channel[0];
   channel[0].setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
    private long total;
    private boolean ended, named;
    @Override public void onMessage(WebMessagePort port, WebMessage message) {
     if (ended || !current.operation.active()) return;
     try {
      String data = message.getData();
      if (data != null && data.startsWith("text-name:")) {
       String name = data.substring("text-name:".length());
       if (opaque || named || total != 0 || !ImageDownloadPolicy.plainText("application/octet-stream", name) ||
           !ImageDownloadPolicy.plainText(mime, name)) throw new IOException("Invalid document metadata");
       current.name = name;
       named = true;
       port.postMessage(new WebMessage("next"));
       return;
      }
      if ("done".equals(data) && total > 0) {
       if ((opaque || ImageDownloadPolicy.plainText(mime, current.name)) && length > 0 && total != length) throw new IOException("Download length changed");
       ended = true; ready(current, mime, opaque && !ImageDownloadPolicy.plainText(mime, current.name)); return;
      }
      if (data == null || data.length() > ((CHUNK_BYTES + 2) / 3) * 4 || "error".equals(data)) throw new IOException("Invalid download chunk");
      byte[] decoded = Base64.decode(data, Base64.DEFAULT);
      if (decoded.length == 0 || decoded.length > CHUNK_BYTES || total + decoded.length > ImageDownloadPolicy.MAX_BYTES) throw new IOException("Invalid download size");
      try (OutputStream out = new FileOutputStream(current.file, true)) { out.write(decoded); }
      total += decoded.length;
      if (current.operation.active()) port.postMessage(new WebMessage("next"));
     } catch (IOException | RuntimeException e) {
      ended = true;
      ui.post(() -> fail(current));
     } finally { if (!current.operation.active()) current.file.delete(); }
    }
   }, blobWorker);
   current.page.evaluateJavascript(script, result -> {
    if (job != current || !"true".equals(result) || !ImageDownloadPolicy.allowedPage(current.page.getUrl())) {
     channel[1].close(); fail(current); return;
    }
    try {
     current.page.postWebMessage(new WebMessage(key, new WebMessagePort[]{channel[1]}), Uri.parse(ImageDownloadPolicy.ORIGIN));
    } catch (RuntimeException e) { channel[1].close(); fail(current); }
   });
  } catch (Exception e) { fail(current); }
 }

 private void ready(Job current, String reportedMime, boolean requireImageMatch) {
  try {
   String textMime = requireImageMatch ? null : ImageDownloadPolicy.textMime(reportedMime, current.name);
   if (textMime != null) {
    try (InputStream in = new FileInputStream(current.file)) {
     if (!ImageDownloadPolicy.validText(in)) throw new IOException("Invalid text file");
    }
    ui.post(() -> chooseDestination(current, textMime, ImageDownloadPolicy.textName(current.name, textMime), false));
    return;
   }
   try (InputStream in = new FileInputStream(current.file)) {
    byte[] header = new byte[12];
    int offset = 0, n;
    while (offset < header.length && (n = in.read(header, offset, header.length - offset)) != -1) offset += n;
    String mime = ImageDownloadPolicy.mime(java.util.Arrays.copyOf(header, offset));
    if (mime == null || (requireImageMatch && !reportedMime.equals(mime))) throw new IOException("Invalid raster image signature");
    String name = "Gemini-image-" + System.currentTimeMillis() + "." + ImageDownloadPolicy.extension(mime);
    ui.post(() -> chooseDestination(current, mime, name, true));
   }
  } catch (IOException e) { ui.post(() -> fail(current)); }
 }

 private void chooseDestination(Job current, String mime, String name, boolean image) {
  if (job != current || !current.operation.active()) return;
  if (!ImageDownloadPolicy.allowedPage(current.page.getUrl()) || activity.isFinishing() || activity.isDestroyed()) {
   discard(current); return;
  }
  closePort(current);
  Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime);
  save.putExtra(Intent.EXTRA_TITLE, name);
  try {
   current.image = image;
   current.picking = true;
   activity.startActivityForResult(save, SAVE_FILE);
  } catch (ActivityNotFoundException e) { fail(current); }
 }

 void result(int result, Intent data) {
  Job current = job;
  if (current == null || !current.picking) {
   if (result == Activity.RESULT_OK) message("Download expired. Please try again.");
   return;
  }
  Uri destination = result == Activity.RESULT_OK && data != null ? data.getData() : null;
  if (destination == null) { discard(current); return; }
  if (!"content".equals(destination.getScheme()) || !android.provider.DocumentsContract.isDocumentUri(activity, destination)) {
   fail(current); return;
  }
  // The destination belongs to this explicit save request. Finish writing even if the activity closes.
  job = null;
  new Thread(() -> {
   boolean saved = false;
   try (InputStream in = new FileInputStream(current.file);
        OutputStream out = activity.getContentResolver().openOutputStream(destination, "w")) {
    if (out == null) throw new IOException("Destination unavailable");
    Transfer.copy(in, out, ImageDownloadPolicy.MAX_BYTES, () -> false, null);
    out.flush();
   } catch (IOException | RuntimeException e) {
    try { android.provider.DocumentsContract.deleteDocument(activity.getContentResolver(), destination); }
    catch (Exception ignored) { /* Some document providers do not support deletion. */ }
    discardFileAndReport(current, false);
    return;
   }
   saved = true;
   discardFileAndReport(current, saved);
  }, "GeminiFileSave").start();
 }

 private void discardFileAndReport(Job current, boolean saved) {
  current.file.delete();
  ui.post(() -> message(saved ? (current.image ? "Image saved." : "File saved.") : "Could not save the file. Choose a different folder and retry."));
 }

 void pageChanged(WebView page) { if (job != null && job.page == page && !job.picking) discard(job); }
 void destroy() {
  if (job != null) discard(job);
  blobThread.quitSafely();
 }
 private void closePort(Job current) {
  if (current.port != null) { current.port.close(); current.port = null; }
 }
 private void discard(Job current) {
  if (job != current) return;
  job = null;
  current.operation.cancel();
  closePort(current);
  current.file.delete();
 }
 private void fail(Job current) {
  if (job != current) return;
  discard(current);
  message(FAILED);
 }
 private void message(String text) {
  if (!activity.isFinishing() && !activity.isDestroyed()) Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
 }
}
