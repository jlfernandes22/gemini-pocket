package com.geminipocket;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.webkit.CookieManager;
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
 static final int SAVE_IMAGE = 14;
 private static final int CHUNK_BYTES = 32768;
 private static final String FILE_PREFIX = "gemini-download-";
 private static final String FAILED = "Could not download the image. Try Download full size again or use Browser session.";
 private final Activity activity;
 private final Handler ui = new Handler(Looper.getMainLooper());
 private final Handler blobWorker;
 private final android.os.HandlerThread blobThread;
 private Job job;

 private static final class Job {
  final WebView page;
  final Operation operation = new Operation();
  final File file;
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

 void start(WebView page, String url, String agent, String mime, long length) {
  if (!ImageDownloadPolicy.allowedPage(page.getUrl())) return;
  if (job != null) { message("Finish the current image download first."); return; }
  boolean blob = ImageDownloadPolicy.blob(url);
  if (!blob && !ImageDownloadPolicy.https(url)) { message("This download isn't supported in Isolated mode. Try Browser session."); return; }
  if (length > ImageDownloadPolicy.MAX_BYTES) { message("Image is larger than the 32 MB download limit."); return; }
  if (blob && url.startsWith("blob:null/") && (length <= 0 || mime == null || !mime.matches("image/(jpeg|png|webp|gif)"))) {
   message(FAILED); return;
  }
  Job current = new Job(page, new File(activity.getCacheDir(), FILE_PREFIX + UUID.randomUUID() + ".tmp"));
  job = current;
  ui.postDelayed(() -> { if (job == current && !current.picking) fail(current); }, 50000);
  if (blob) receiveBlob(current, url, mime, length);
  else new Thread(() -> {
   try {
    fetch(current, url, agent);
    ready(current, null);
   } catch (IOException | RuntimeException e) { ui.post(() -> fail(current)); }
   finally { if (!current.operation.active()) current.file.delete(); }
  }, "GeminiImageFetch").start();
 }

 private void fetch(Job current, String url, String agent) throws IOException {
  for (int hops = 0; hops <= 4; hops++) {
   if (!current.operation.active() || !ImageDownloadPolicy.https(url)) throw new IOException("Blocked image URL");
   HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection();
   try {
    conn.setInstanceFollowRedirects(false);
    conn.setConnectTimeout(12000);
    conn.setReadTimeout(15000);
    conn.setRequestProperty("Accept", "image/*,application/octet-stream;q=0.5");
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
    if (response != 200 || conn.getContentLengthLong() > ImageDownloadPolicy.MAX_BYTES) throw new IOException("Invalid image response");
    if (!current.operation.active()) throw new IOException("Cancelled");
    try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(current.file)) {
     Transfer.copy(in, out, ImageDownloadPolicy.MAX_BYTES, () -> !current.operation.active(), null);
    }
    return;
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
     .put("type", mime).put("size", length);
   script = new String(out.toByteArray(), StandardCharsets.UTF_8).replace("/* REQUEST */ null", request.toString());
   WebMessagePort[] channel = current.page.createWebMessageChannel();
   current.port = channel[0];
   channel[0].setWebMessageCallback(new WebMessagePort.WebMessageCallback() {
    private long total;
    private boolean ended;
    @Override public void onMessage(WebMessagePort port, WebMessage message) {
     if (ended || !current.operation.active()) return;
     try {
      String data = message.getData();
      if ("done".equals(data) && total > 0) {
       if (opaque && total != length) throw new IOException("Image length changed");
       ended = true; ready(current, opaque ? mime : null); return;
      }
      if (data == null || data.length() > ((CHUNK_BYTES + 2) / 3) * 4 || "error".equals(data)) throw new IOException("Invalid image chunk");
      byte[] decoded = Base64.decode(data, Base64.DEFAULT);
      if (decoded.length == 0 || decoded.length > CHUNK_BYTES || total + decoded.length > ImageDownloadPolicy.MAX_BYTES) throw new IOException("Invalid image size");
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

 private void ready(Job current, String expectedMime) {
  try (InputStream in = new FileInputStream(current.file)) {
   byte[] header = new byte[12];
   int offset = 0, n;
   while (offset < header.length && (n = in.read(header, offset, header.length - offset)) != -1) offset += n;
   String mime = ImageDownloadPolicy.mime(java.util.Arrays.copyOf(header, offset));
   if (mime == null || (expectedMime != null && !expectedMime.equals(mime))) throw new IOException("Invalid raster image signature");
   ui.post(() -> chooseDestination(current, mime));
  } catch (IOException e) { ui.post(() -> fail(current)); }
 }

 private void chooseDestination(Job current, String mime) {
  if (job != current || !current.operation.active()) return;
  if (!ImageDownloadPolicy.allowedPage(current.page.getUrl()) || activity.isFinishing() || activity.isDestroyed()) {
   discard(current); return;
  }
  closePort(current);
  Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime);
  save.putExtra(Intent.EXTRA_TITLE, "Gemini-image-" + System.currentTimeMillis() + "." + ImageDownloadPolicy.extension(mime));
  try {
   current.picking = true;
   activity.startActivityForResult(save, SAVE_IMAGE);
  } catch (ActivityNotFoundException e) { fail(current); }
 }

 void result(int result, Intent data) {
  Job current = job;
  if (current == null || !current.picking) {
   if (result == Activity.RESULT_OK) message("Image download expired. Please try again.");
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
  }, "GeminiImageSave").start();
 }

 private void discardFileAndReport(Job current, boolean saved) {
  current.file.delete();
  ui.post(() -> message(saved ? "Image saved." : "Could not save the image. Choose a different folder and retry."));
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
