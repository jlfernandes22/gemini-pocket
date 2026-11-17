package com.geminipocket;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 private static final String HOME="https://gemini.google.com/app";
 private static final int PICK=10,CAPTURE=11,CAMERA_PERMISSION=12,WEB_PERMISSION=13;
 private android.content.SharedPreferences prefs;
 private LinearLayout root;
 private WebView web;
 private TextView status;
 private LinearLayout launchCover;
 private boolean pageVisible;
 private Button retryLaunch;
 private Button openIncomplete;
 private String readinessScript;
 private TextView loadingStage,loadingHint;
 private int launchAttempt;
 private ValueCallback<Uri[]> upload;
 private Uri captureUri;
 private PermissionRequest pendingPermission;
 private boolean multiple;
 private String[] accept;
 private String attachmentAction="";
 private long attachmentActionAt;
 private Bundle restore;
 private final ArrayList<WebView> popups=new ArrayList<>();
 private int pad(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private boolean restricted(){return prefs.getBoolean("restricted",true);}
 private boolean dark(){return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);new Thread(()->TempPhotos.clean(getCacheDir(),System.currentTimeMillis()),"TempPhotoCleanup").start();prefs=getSharedPreferences("settings",MODE_PRIVATE);restore=state;
  if(state!=null){String c=state.getString("capture");if(c!=null)captureUri=Uri.parse(c);}
  getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);
  if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);getWindow().setNavigationBarContrastEnforced(false);}else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  if(!prefs.contains("mode"))setup();else if("browser".equals(prefs.getString("mode","")))browserHome();else isolated();
 }
 private void base(){
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(dark()?Color.BLACK:Color.WHITE);FrameLayout backdrop=new FrameLayout(this);backdrop.setBackgroundColor(dark()?Color.BLACK:Color.WHITE);backdrop.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(backdrop);
  backdrop.setOnApplyWindowInsetsListener((v,i)->{int left,top,right,bottom;
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets ime=i.getInsets(WindowInsets.Type.ime());android.graphics.Insets tappable=i.getInsets(WindowInsets.Type.tappableElement());int navigationBottom=tappable.bottom>0?bars.bottom:0;left=bars.left;top=bars.top;right=bars.right;bottom=Math.max(navigationBottom,ime.bottom);}else{left=i.getSystemWindowInsetLeft();top=i.getSystemWindowInsetTop();right=i.getSystemWindowInsetRight();bottom=i.getSystemWindowInsetBottom();}
   root.setPadding(left,top,right,bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:i.consumeSystemWindowInsets();
  });backdrop.requestApplyInsets();
 }
 private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(dark()?Color.rgb(232,234,243):Color.rgb(25,31,50));t.setPadding(pad(20),pad(12),pad(20),pad(12));return t;}
 private void action(LinearLayout parent,String title,Runnable r){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(15);b.setMinHeight(pad(48));boolean primary="Apply".equals(title);b.setTextColor(primary?(dark()?0xFF102047:Color.WHITE):(dark()?0xFFD7E2FF:0xFF304D8C));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(primary?(dark()?0xFFB8CBFF:0xFF3459CF):(dark()?0xFF2B303A:0xFFE3EAF7));bg.setCornerRadius(pad(24));b.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,pad(48));lp.setMargins(pad(8),pad(5),pad(8),pad(5));parent.addView(b,lp);b.setOnClickListener(v->r.run());}
 private LinearLayout surfaceCard(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(pad(12),pad(12),pad(12),pad(16));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark()?0xFF1E222B:0xFFEEF2FA);bg.setCornerRadius(pad(28));c.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(pad(8),pad(10),pad(8),pad(10));parent.addView(c,lp);return c;}
 private void setup(){
  base();ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setPadding(pad(12),pad(32),pad(12),pad(24));content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll);
  content.addView(text("Gemini Pocket",30));content.addView(text("Your Gemini. Your connection.",20));content.addView(text("Choose a private app session or continue with a browser you already use. You can change this later.",15));
  CheckBox updates=new CheckBox(this);updates.setText("Automatically check for updates");updates.setMinHeight(pad(48));updates.setChecked(prefs.getBoolean("autoUpdates",false));updates.setOnCheckedChangeListener((button,on)->prefs.edit().putBoolean("autoUpdates",on).apply());content.addView(updates);content.addView(text("Checks GitHub at most once daily when opened. Downloads require your approval.",12));
  LinearLayout isolatedCard=surfaceCard(content);isolatedCard.addView(text("Isolated app",22));isolatedCard.addView(text("A focused space for Gemini",14));isolatedCard.addView(text("Separate sign-in, native camera and photo picker, with required URLs only by default.",15));action(isolatedCard,"Continue in app",()->{if(web!=null && returnFromModeSelection())return;confirmModeSelection();prefs.edit().putString("mode","isolated").apply();isolated();});
  LinearLayout browserCard=surfaceCard(content);browserCard.addView(text("Browser session",22));browserCard.addView(text("Keep your existing browser login",14));browserCard.addView(text("Cookies and passwords stay with your browser. Its navigation, permissions and upload controls apply.",15));action(browserCard,"Choose a browser",()->chooseBrowser());
  content.addView(text("Google may limit embedded sign-in. Browser mode is an alternative. Android can stop background work in either mode. Not affiliated with Google.",12));
 }
 private void mountSheet(AnimatedSheetDialog sheet,LinearLayout c){
  View handle=new View(this);android.graphics.drawable.GradientDrawable grip=new android.graphics.drawable.GradientDrawable();grip.setColor(dark()?0xFF8F98A8:0xFF737D8E);grip.setCornerRadius(pad(3));handle.setBackground(grip);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(pad(36),pad(4));hp.gravity=Gravity.CENTER;hp.setMargins(0,pad(4),0,pad(16));FrameLayout gripArea=new FrameLayout(this);gripArea.setContentDescription("Drag down to dismiss");FrameLayout.LayoutParams gripSize=new FrameLayout.LayoutParams(pad(36),pad(4),Gravity.CENTER);gripArea.addView(handle,gripSize);c.addView(gripArea,0,new LinearLayout.LayoutParams(-1,pad(40)));
  DraggableSheetScrollView scroll=new DraggableSheetScrollView(this);scroll.onDismiss(()->sheet.cancel());scroll.addView(c);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark()?0xFF1E1F21:0xFFF2F5FC);bg.setCornerRadii(new float[]{pad(28),pad(28),pad(28),pad(28),0,0,0,0});scroll.setBackground(bg);c.setBackground(null);FrameLayout host=new FrameLayout(this);host.setOnClickListener(v->sheet.cancel());scroll.setOnClickListener(v->{});FrameLayout.LayoutParams panel=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);host.addView(scroll,panel);sheet.setContentView(host);sheet.panel(scroll);Window w=sheet.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);w.setDimAmount(.4f);w.setLayout(-1,-1);sheet.show();w.setLayout(-1,-1);w.setGravity(Gravity.FILL);w.setNavigationBarColor(dark()?0xFF1E1F21:0xFFF2F5FC);if(Build.VERSION.SDK_INT>=30){w.setDecorFitsSystemWindows(false);w.setNavigationBarContrastEnforced(false);}else w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  host.setOnApplyWindowInsetsListener((v,i)->{int bottom=Build.VERSION.SDK_INT>=30?i.getInsets(WindowInsets.Type.navigationBars()).bottom:i.getSystemWindowInsetBottom();c.setPadding(pad(16),pad(12),pad(16),pad(24)+bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:i.consumeSystemWindowInsets();});host.requestApplyInsets();
 }
 private void addSheetBack(LinearLayout content,AnimatedSheetDialog sheet){
  sheet.backAction(()->settings());ImageButton back=new ImageButton(this);back.setImageResource(R.drawable.ic_back);back.setColorFilter(dark()?0xFFD7E2FF:0xFF304D8C);back.setBackgroundColor(Color.TRANSPARENT);back.setContentDescription("Back to browsing controls");content.addView(back,0,new LinearLayout.LayoutParams(pad(48),pad(48)));back.setOnClickListener(v->sheet.onBackPressed());
 }
 private Dialog themedSheet(String title,String subtitle,java.util.function.BiConsumer<LinearLayout,Dialog> contents){
  AnimatedSheetDialog sheet=new AnimatedSheetDialog(this);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(pad(16),pad(12),pad(16),pad(24));c.addView(text(title,24));if(subtitle!=null)c.addView(text(subtitle,14));contents.accept(c,sheet);if(title.equals("Connection & browsing")||title.equals("Gemini Pocket")||title.equals("Reset device access?"))addSheetBack(c,sheet);mountSheet(sheet,c);return sheet;
 }
 private void chooseBrowser(){
  Intent service=new Intent("android.support.customtabs.action.CustomTabsService");List<ResolveInfo> found=getPackageManager().queryIntentServices(service,0);ArrayList<String> packages=new ArrayList<>(),labels=new ArrayList<>();
  for(ResolveInfo ri:found){String p=ri.serviceInfo.packageName;if(!packages.contains(p)){packages.add(p);labels.add(ri.loadLabel(getPackageManager()).toString());}}
  themedSheet("Choose your browser","Your login stays in the browser you choose.",(content,sheet)->{
   if(packages.isEmpty())content.addView(text("No compatible browser found. Install or enable a browser supporting Custom Tabs, such as Firefox or Chrome.",15));
   for(int i=0;i<packages.size();i++){final int index=i;action(content,labels.get(i),()->{sheet.dismiss();themedSheet("Continue with "+labels.get(index),"Browser session",(consent,dialog)->{
    consent.addView(text("Gemini will use this browser’s existing sign-in. Gemini Pocket does not read or import your passwords or cookies.",16));consent.addView(text("The browser manages camera, uploads and navigation. Isolated-mode URL restrictions do not apply here.",14));
    action(consent,"Continue",()->{dialog.dismiss();confirmModeSelection();prefs.edit().putString("mode","browser").putString("browser",packages.get(index)).apply();browserHome();openBrowser();});action(consent,"Back",()->{dialog.dismiss();chooseBrowser();});
   });});}
   action(content,"Cancel",()->sheet.dismiss());
  });
 }
 private void browserHome(){
  base();root.addView(text("Gemini Pocket",28));root.addView(text("Browser session",20));
  root.addView(text("Your account stays in your selected browser. Return to its Gemini tab after switching apps. Camera and photo selection are managed by that browser. URL restriction is available only in isolated mode.",16));
  action(root,"Open Gemini",()->openBrowser());action(root,"Settings",()->settings());
 }
 private void openBrowser(){
  String pkg=prefs.getString("browser","");if(pkg.isEmpty()){chooseBrowser();return;}
  Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(HOME));i.setPackage(pkg);
  Bundle b=new Bundle();b.putBinder("android.support.customtabs.extra.SESSION",null);i.putExtras(b);
  i.putExtra("android.support.customtabs.extra.TITLE_VISIBILITY",1);
  i.putExtra("android.support.customtabs.extra.TOOLBAR_COLOR",dark()?0xFF121318:0xFFF2F5FC);
  i.putExtra("androidx.browser.customtabs.extra.COLOR_SCHEME",0);
  i.putExtra("androidx.browser.customtabs.extra.NAVIGATION_BAR_COLOR",dark()?0xFF121318:0xFFF2F5FC);
  try{startActivity(i);}catch(ActivityNotFoundException e){toast("Browser unavailable. Choose another browser.");chooseBrowser();}
 }
 private ImageButton shield;
 private View previousScreen;
 private LinearLayout previousRoot;
 private void beginModeSelection(){
  if(previousScreen!=null)return;
  previousRoot=root;previousScreen=(View)root.getParent();setup();
 }
 private boolean returnFromModeSelection(){
  if(previousScreen==null)return false;
  View screen=previousScreen;root=previousRoot;previousScreen=null;previousRoot=null;setContentView(screen);screen.requestApplyInsets();return true;
 }
 private void confirmModeSelection(){previousScreen=null;previousRoot=null;destroyWeb();}
 private void settingsRow(LinearLayout parent,String title,String description,Runnable click){
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(pad(16),pad(12),pad(16),pad(12));row.setMinimumHeight(pad(64));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark()?0xFF292D36:0xFFE7EDF7);bg.setCornerRadius(pad(20));row.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22666666),bg,null));TextView name=text(title,16);name.setPadding(0,0,0,pad(4));row.addView(name);TextView detail=text(description,13);detail.setAlpha(.75f);detail.setPadding(0,0,0,0);row.addView(detail);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(pad(8),pad(4),pad(8),pad(4));parent.addView(row,lp);row.setFocusable(true);row.setOnClickListener(v->click.run());
 }
 private void settings(){
  themedSheet("Browsing controls",("browser".equals(prefs.getString("mode","isolated"))?"Browser session":"Isolated app")+" · "+(restricted()?"Restricted":"Open HTTPS"),(c,sheet)->{
   LinearLayout quick=new LinearLayout(this);c.addView(quick);LinearLayout reloadBox=new LinearLayout(this),homeBox=new LinearLayout(this);quick.addView(reloadBox,new LinearLayout.LayoutParams(0,-2,1));quick.addView(homeBox,new LinearLayout.LayoutParams(0,-2,1));action(reloadBox,"Reload",()->{sheet.dismiss();navigateWithLoading(false);});action(homeBox,"Home",()->{sheet.dismiss();navigateWithLoading(true);});
   c.addView(text("CONNECTION",12));settingsRow(c,"Connection & browsing","Session and permitted websites",()->{sheet.dismiss();connectionControls();});
   c.addView(text("PRIVACY & PERMISSIONS",12));settingsRow(c,"Privacy & storage","Attachments, website cache and session",()->{sheet.dismiss();privacyControls();});settingsRow(c,"Website device access","Reset saved camera and microphone choices",()->{sheet.dismiss();themedSheet("Reset device access?","Gemini will ask for camera and microphone access again. This reloads the page.",(content,confirm)->{action(content,"Reset permissions",()->{prefs.edit().remove("consent.gemini.google.com.audio").remove("consent.gemini.google.com.video").apply();if(pendingPermission!=null)denyPermission(pendingPermission);confirm.dismiss();if(web!=null)web.reload();});action(content,"Cancel",()->confirm.onBackPressed());});});
   c.addView(text("APP",12));settingsRow(c,"About & updates","Version, updates and project links",()->{sheet.dismiss();about();});
  });
 }
 private void connectionControls(){
  themedSheet("Connection & browsing","Changes apply only after tapping Apply.",(c,sheet)->{
  RadioGroup choices=new RadioGroup(this);RadioButton locked=new RadioButton(this);locked.setId(View.generateViewId());locked.setText("Restricted browsing\nOnly required Gemini & Google services");locked.setMinHeight(pad(64));choices.addView(locked);
  RadioButton open=new RadioButton(this);open.setId(View.generateViewId());open.setText("Open HTTPS browsing\nAllow other secure websites");open.setMinHeight(pad(64));choices.addView(open);choices.check(restricted()?locked.getId():open.getId());c.addView(choices);
  c.addView(text("Applies only to isolated mode. Not a firewall. Existing pages stay open; new requests use your selection.",13));
  c.addView(text("Connection mode",18));
  RadioGroup modes=new RadioGroup(this);RadioButton isolatedMode=new RadioButton(this);isolatedMode.setId(View.generateViewId());isolatedMode.setText("Isolated app\nSeparate sign-in · app-controlled URL restrictions");isolatedMode.setMinHeight(pad(64));modes.addView(isolatedMode);
  RadioButton browserMode=new RadioButton(this);browserMode.setId(View.generateViewId());browserMode.setText("Browser session\nUse browser login · browser navigation rules");browserMode.setMinHeight(pad(64));modes.addView(browserMode);modes.check("browser".equals(prefs.getString("mode","isolated"))?browserMode.getId():isolatedMode.getId());c.addView(modes);
  action(c,"Apply",()->{boolean browserSelected=modes.getCheckedRadioButtonId()==browserMode.getId();prefs.edit().putBoolean("restricted",choices.getCheckedRadioButtonId()==locked.getId()).apply();updateShield();sheet.dismiss();
   if(browserSelected){chooseBrowser();return;}
   prefs.edit().putString("mode","isolated").apply();if(web==null)isolated();
  });
  });
 }
 private void privacyControls(){
  final AnimatedSheetDialog sheet=new AnimatedSheetDialog(this);
  LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(pad(16),pad(12),pad(16),pad(24));
  c.addView(text("Privacy & storage",22));c.addView(text("Manage this app’s local data",14));
  String[] titles={"Temporary attachments","Website cache","Reset website session"};
  String[] details={"Remove camera and upload copies. Gallery originals stay safe.","Free downloaded resources without signing out.","Sign out of isolated mode and erase its website data."};
  for(int i=0;i<titles.length;i++){final int which=i;c.addView(text(details[i],13));action(c,titles[i],()->{sheet.dismiss();confirmPrivacy(which);});}

  addSheetBack(c,sheet);mountSheet(sheet,c);
 }
 private void confirmPrivacy(int which){
   String message=which==0?"Remove app-generated attachment copies. Do not continue while an upload or draft attachment is in use. Your gallery originals are untouched.":which==1?"Clear downloaded website resources. Cookies and sign-in remain. Avoid this during uploads.":"This clears isolated-mode cookies and website storage and reloads Gemini. Browser-mode sign-in is not affected. Drafts may be lost.";
   themedSheet("Confirm cleanup",message,(content,confirm)->{action(content,"Continue",()->{confirm.dismiss();
    if(upload!=null || preparingAttachment){toast("Finish attaching files before cleanup.");return;}
    if(which==0){new Thread(()->{java.io.File[] files=getCacheDir().listFiles();int removed=0;if(files!=null)for(java.io.File f:files)if(f.isFile()&&UploadPolicy.validName(f.getName())&&f.delete())removed++;final int count=removed;runOnUiThread(()->toast("Removed "+count+" temporary attachments"));},"AttachmentCleanup").start();}
    else if(which==1){if(web!=null)web.clearCache(true);toast("Website cache cleared");}
    else{if(web!=null)web.stopLoading();CookieManager.getInstance().removeAllCookies(ok->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();if(web!=null){web.clearHistory();web.loadUrl(HOME);}});}
   });action(content,"Cancel",()->confirm.dismiss());});
 }
 private boolean preparingAttachment;
 private void about(){
  String version="Unknown";try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
  final String installed=version;themedSheet("Gemini Pocket",installed+(installed.contains("dev")?" · Development":""),(content,sheet)->{
   content.addView(text("A lightweight, unofficial home for Gemini. Not affiliated with Google.",15));
   Switch toggle=new Switch(this);toggle.setText("Automatically check for updates");toggle.setMinHeight(pad(56));toggle.setChecked(prefs.getBoolean("autoUpdates",false));toggle.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean("autoUpdates",on).apply());content.addView(toggle);content.addView(text("Daily launch checks only. No automatic APK downloads.",12));
   action(content,"Check for updates",()->{sheet.dismiss();checkUpdate(true);});
   action(content,"GitHub repository",()->openAppLink("https://github.com/jlfernandes22/gemini-pocket"));
   action(content,"Releases",()->openAppLink("https://github.com/jlfernandes22/gemini-pocket/releases"));
   action(content,"Report an issue",()->openAppLink("https://github.com/jlfernandes22/gemini-pocket/issues"));
   action(content,"Privacy",()->openAppLink("https://github.com/jlfernandes22/gemini-pocket/blob/main/PRIVACY.md"));
   action(content,"MIT license",()->openAppLink("https://github.com/jlfernandes22/gemini-pocket/blob/main/LICENSE"));
   action(content,"Inspired by geminiAssist",()->openAppLink("https://github.com/AcideFluorhydrique/geminiAssist"));
  });
 }
 private void openAppLink(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(ActivityNotFoundException e){toast("No browser available");}}
 private boolean checkingUpdate,downloadingUpdate,awaitingInstallPermission;
 private Dialog checkSheet;
 private UpdateCheckView checkCircle;
 private TextView checkStatus,checkDetail;
 private void showUpdateChecking(){
  checkSheet=themedSheet("App updates",null,(content,sheet)->{
   checkCircle=new UpdateCheckView(this);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(pad(120),pad(120));lp.gravity=Gravity.CENTER;lp.setMargins(0,pad(12),0,pad(16));content.addView(checkCircle,lp);
   checkStatus=text("Checking for updates",21);checkStatus.setGravity(Gravity.CENTER);content.addView(checkStatus);
   checkDetail=text("Looking for a newer public release on GitHub.",14);checkDetail.setGravity(Gravity.CENTER);content.addView(checkDetail);

  });
 }
 private void dismissUpdateChecking(){if(checkSheet!=null){checkSheet.dismiss();checkSheet=null;}}
 private void showUpToDate(long published,long installed){
  if(checkSheet==null||!checkSheet.isShowing())return;
  checkCircle.finishCheck();checkStatus.setText("Up to date");checkDetail.setText(installed>published?"Your installed build is ahead of the latest public release. No downgrade will be offered.":"You’re running the latest public release.");
 }
 private volatile boolean cancelUpdate;
 private java.net.HttpURLConnection updateConnection;
 private java.io.File verifiedUpdate;
 private long versionCode(android.content.pm.PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
 private java.net.HttpURLConnection updateRequest(String address)throws Exception{
  for(int hop=0;hop<5;hop++){
   if(!address.equals("https://api.github.com/repos/jlfernandes22/gemini-pocket/releases/latest")&&!UpdatePolicy.downloadUrl(address))throw new IOException("Untrusted update URL");
   java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(address).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Gemini-Pocket-Updater");int code=c.getResponseCode();
   if(code>=300&&code<400){String next=c.getHeaderField("Location");c.disconnect();if(next==null)throw new IOException("Missing redirect");address=new java.net.URL(new java.net.URL(address),next).toString();continue;}
   if(code!=200){c.disconnect();throw new IOException("GitHub HTTP "+code);}return c;
  }throw new IOException("Too many redirects");
 }
 private void checkUpdate(boolean manual){
  if(checkingUpdate||downloadingUpdate){if(manual)toast("Update task already running");return;}checkingUpdate=true;prefs.edit().putLong("lastUpdateCheck",System.currentTimeMillis()).apply();if(manual)showUpdateChecking();
  new Thread(()->{try{
   java.net.HttpURLConnection c=updateRequest("https://api.github.com/repos/jlfernandes22/gemini-pocket/releases/latest");String json;try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){if(out.size()+n>2*1024*1024)throw new IOException("Release metadata too large");out.write(buf,0,n);}json=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}finally{c.disconnect();}
   org.json.JSONObject release=new org.json.JSONObject(json);long published=UpdatePolicy.publishedCode(release.getString("tag_name"));long installed=versionCode(getPackageManager().getPackageInfo(getPackageName(),0));
   if(release.optBoolean("prerelease")||release.optBoolean("draft")||published<=installed){runOnUiThread(()->{checkingUpdate=false;if(manual&&!isDestroyed())showUpToDate(published,installed);});return;}
   org.json.JSONArray assets=release.getJSONArray("assets");org.json.JSONObject found=null;for(int i=0;i<assets.length();i++){org.json.JSONObject a=assets.getJSONObject(i);if(a.getString("name").endsWith(".apk")){if(found!=null)throw new IOException("Ambiguous APK assets");found=a;}}
   if(found==null)throw new IOException("Release has no APK");final String url=found.getString("browser_download_url"),digest=found.optString("digest");final long size=found.getLong("size");final String name=release.optString("name",release.getString("tag_name")),notes=release.optString("body");if(!UpdatePolicy.downloadUrl(url)||size<=0||size>100L*1024*1024||!digest.matches("sha256:[0-9a-fA-F]{64}"))throw new IOException("Release verification data missing");
   runOnUiThread(()->{checkingUpdate=false;dismissUpdateChecking();if(isDestroyed())return;themedSheet("Update available",name,(content,sheet)->{content.addView(text(String.format(java.util.Locale.US,"Download size: %.2f MB",size/1048576.0),14));content.addView(text(notes.length()>2500?notes.substring(0,2500)+"…":notes,13));action(content,"Download update",()->{sheet.dismiss();downloadUpdate(url,digest.substring(7),size);});action(content,"Not now",()->sheet.dismiss());});});
  }catch(Exception e){runOnUiThread(()->{checkingUpdate=false;dismissUpdateChecking();if(manual&&!isDestroyed())themedSheet("Could not check updates",e.getMessage(),(content,sheet)->{action(content,"Retry",()->{sheet.dismiss();checkUpdate(true);});});});}},"UpdateCheck").start();
 }
 private void downloadUpdate(String url,String sha,long expected){
  if(downloadingUpdate)return;downloadingUpdate=true;cancelUpdate=false;
  final TextView[] label=new TextView[1];final UpdateProgressView[] ring=new UpdateProgressView[1];final Dialog dialog=themedSheet("Downloading update","Keep Gemini Pocket open until verification finishes.",(content,sheet)->{ring[0]=new UpdateProgressView(this);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(pad(120),pad(120));lp.gravity=Gravity.CENTER;content.addView(ring[0],lp);label[0]=text("Starting download…",14);label[0].setGravity(Gravity.CENTER);content.addView(label[0]);action(content,"Cancel",()->{cancelUpdate=true;if(updateConnection!=null)updateConnection.disconnect();sheet.dismiss();});});dialog.setOnDismissListener(d->{if(downloadingUpdate){cancelUpdate=true;if(updateConnection!=null)updateConnection.disconnect();}});
  new Thread(()->{File dir=new File(getFilesDir(),"updates");dir.mkdirs();File part=new File(dir,"update.part"),apk=new File(dir,"update.apk");try{
   updateConnection=updateRequest(url);java.security.MessageDigest md=java.security.MessageDigest.getInstance("SHA-256");long received=0;int previous=-1;try(InputStream in=updateConnection.getInputStream();OutputStream out=new FileOutputStream(part)){byte[] buf=new byte[32768];int n;while((n=in.read(buf))!=-1){if(cancelUpdate)throw new IOException("Cancelled");received+=n;if(received>expected)throw new IOException("Unexpected download size");out.write(buf,0,n);md.update(buf,0,n);int percent=(int)(received*100/expected);if(percent!=previous){previous=percent;final int p=percent;runOnUiThread(()->{if(!isDestroyed()){ring[0].progress(p);label[0].setText("Downloading · "+p+"%");}});}}}
   if(cancelUpdate)throw new IOException("Cancelled");StringBuilder hash=new StringBuilder();for(byte b:md.digest())hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));if(received!=expected||!sha.equalsIgnoreCase(hash.toString()))throw new IOException("Download verification failed");
   verifyUpdate(part);if(apk.exists()&&!apk.delete())throw new IOException("Cannot replace old download");if(!part.renameTo(apk))throw new IOException("Cannot store verified update");verifiedUpdate=apk;
   runOnUiThread(()->{downloadingUpdate=false;dialog.dismiss();if(!isDestroyed())installVerifiedUpdate();});
  }catch(Exception e){part.delete();runOnUiThread(()->{downloadingUpdate=false;dialog.dismiss();if(!cancelUpdate&&!isDestroyed())themedSheet("Update download failed",e.getMessage(),(content,sheet)->{action(content,"Retry",()->{sheet.dismiss();downloadUpdate(url,sha,expected);});});});}finally{if(updateConnection!=null)updateConnection.disconnect();updateConnection=null;}},"UpdateDownload").start();
 }
 @SuppressWarnings("deprecation") private void verifyUpdate(File apk)throws Exception{
  android.content.pm.PackageInfo current=getPackageManager().getPackageInfo(getPackageName(),android.content.pm.PackageManager.GET_SIGNATURES),candidate=getPackageManager().getPackageArchiveInfo(apk.getPath(),android.content.pm.PackageManager.GET_SIGNATURES);
  if(candidate==null||!getPackageName().equals(candidate.packageName)||versionCode(candidate)<=versionCode(current)||current.signatures==null||candidate.signatures==null||!java.util.Arrays.equals(current.signatures,candidate.signatures))throw new SecurityException("APK identity, version or signing certificate does not match");
 }
 private void installVerifiedUpdate(){
  if(verifiedUpdate==null||!verifiedUpdate.isFile())return;try{verifyUpdate(verifiedUpdate);
   if(!getPackageManager().canRequestPackageInstalls()){themedSheet("Allow update installation","Android requires permission to install updates from Gemini Pocket. Enable it on the next screen, then return.",(content,sheet)->{action(content,"Open Android settings",()->{sheet.dismiss();awaitingInstallPermission=true;startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));});action(content,"Not now",()->sheet.dismiss());});return;}
   Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse("content://com.geminipocket.updates/update.apk"),"application/vnd.android.package-archive");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);
  }catch(Exception e){toast("Could not install update: "+e.getMessage());}
 }
 @Override protected void onResume(){super.onResume();if(awaitingInstallPermission){awaitingInstallPermission=false;if(getPackageManager().canRequestPackageInstalls())installVerifiedUpdate();}if(prefs!=null&&prefs.getBoolean("autoUpdates",false)&&System.currentTimeMillis()-prefs.getLong("lastUpdateCheck",0)>24L*60*60*1000)checkUpdate(false);}
 private void updateShield(){if(shield!=null){shield.setContentDescription("Browsing controls: "+(restricted()?"restricted":"open HTTPS"));shield.setAlpha(restricted()?1f:.65f);}}
 private void showLoadingOverlay(FrameLayout stage){
  if(launchCover!=null){launchCover.animate().cancel();if(launchCover.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)launchCover.getParent()).removeView(launchCover);}
  pageVisible=false;
  launchCover=new LinearLayout(this);launchCover.setOrientation(LinearLayout.VERTICAL);launchCover.setGravity(Gravity.CENTER);launchCover.setBackgroundColor(dark()?Color.BLACK:Color.WHITE);
  ExpressiveLoader loader=new ExpressiveLoader(this);launchCover.addView(loader,new LinearLayout.LayoutParams(pad(96),pad(96)));
  loadingStage=text("Connecting to Gemini",18);loadingStage.setGravity(Gravity.CENTER);loadingStage.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);launchCover.addView(loadingStage);
  WavyLoadingBar wave=new WavyLoadingBar(this);LinearLayout.LayoutParams waveSize=new LinearLayout.LayoutParams(pad(180),pad(24));waveSize.setMargins(0,pad(6),0,pad(8));launchCover.addView(wave,waveSize);
  loadingHint=text("Preparing your chat",14);loadingHint.setGravity(Gravity.CENTER);loadingHint.setAlpha(.7f);launchCover.addView(loadingHint);
  retryLaunch=new Button(this);retryLaunch.setText("↻  Retry");retryLaunch.setAllCaps(false);retryLaunch.setTextSize(15);retryLaunch.setTextColor(dark()?0xFF102047:Color.WHITE);retryLaunch.setPadding(pad(24),0,pad(24),0);android.graphics.drawable.GradientDrawable retryBg=new android.graphics.drawable.GradientDrawable();retryBg.setColor(dark()?0xFFAAC7FF:0xFF3459CF);retryBg.setCornerRadius(pad(28));retryLaunch.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33666666),retryBg,null));retryLaunch.setVisibility(View.GONE);LinearLayout.LayoutParams retrySize=new LinearLayout.LayoutParams(-2,pad(48));retrySize.setMargins(0,pad(16),0,0);launchCover.addView(retryLaunch,retrySize);
  retryLaunch.setOnClickListener(v->{startLaunchAttempt();web.reload();});
  openIncomplete=new Button(this);openIncomplete.setText("Open page anyway");openIncomplete.setAllCaps(false);openIncomplete.setTextSize(15);openIncomplete.setTextColor(dark()?0xFFD7E2FF:0xFF304D8C);openIncomplete.setPadding(pad(24),0,pad(24),0);openIncomplete.setMinHeight(pad(48));android.graphics.drawable.GradientDrawable secondaryBg=new android.graphics.drawable.GradientDrawable();secondaryBg.setColor(dark()?0xFF2B303A:0xFFE3EAF7);secondaryBg.setCornerRadius(pad(28));openIncomplete.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(dark()?0x33AAC7FF:0x223459CF),secondaryBg,null));openIncomplete.setVisibility(View.GONE);LinearLayout.LayoutParams secondarySize=new LinearLayout.LayoutParams(-2,pad(48));secondarySize.setMargins(0,pad(12),0,0);launchCover.addView(openIncomplete,secondarySize);openIncomplete.setOnClickListener(v->revealPage());
  stage.addView(launchCover,new FrameLayout.LayoutParams(-1,-1));startLaunchAttempt();
  if(shield!=null&&shield.getParent()==stage)shield.bringToFront();
 }
 private void navigateWithLoading(boolean home){if(web==null||!(web.getParent() instanceof FrameLayout))return;showLoadingOverlay((FrameLayout)web.getParent());if(home)web.loadUrl(HOME);else web.reload();}
 @SuppressWarnings("SetJavaScriptEnabled") private void isolated(){
  base();status=new TextView(this);
  FrameLayout stage=new FrameLayout(this);root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
  web=new WebView(this);web.setBackgroundColor(dark()?Color.BLACK:Color.WHITE);stage.addView(web,new FrameLayout.LayoutParams(-1,-1));
  showLoadingOverlay(stage);
  shield=new ImageButton(this);shield.setImageResource(R.drawable.ic_shield);shield.setColorFilter(dark()?Color.rgb(210,220,240):Color.rgb(65,80,110));shield.setPadding(pad(12),pad(12),pad(12),pad(12));android.graphics.drawable.GradientDrawable buttonBg=new android.graphics.drawable.GradientDrawable();buttonBg.setColor(dark()?0xDD24262B:0xDDEEF2FA);buttonBg.setCornerRadius(pad(22));shield.setBackground(buttonBg);
  FrameLayout.LayoutParams shieldLayout=new FrameLayout.LayoutParams(pad(44),pad(44),Gravity.TOP|Gravity.END);shieldLayout.topMargin=pad(58);shieldLayout.rightMargin=pad(8);stage.addView(shield,shieldLayout);updateShield();shield.setOnClickListener(v->settings());
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setMediaPlaybackRequiresUserGesture(true);s.setSafeBrowsingEnabled(true);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
  web.setWebViewClient(client());web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onConsoleMessage(ConsoleMessage m){
    if(m.message().startsWith("GP_MENU ") && m.message().length()<16000){android.util.Log.i("GeminiMenu",m.message());return true;}if(m.message().matches("GP_MEDIA (granted|ended|error [A-Za-z]+)")){android.util.Log.i("GeminiMedia",m.message());return true;}if(m.message().matches("GP_ACTION (photos|files|camera)")){attachmentAction=m.message().substring(10);attachmentActionAt=SystemClock.elapsedRealtime();return true;}
    return false;
   }
   @Override public void onProgressChanged(WebView v,int p){}
   @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> cb,FileChooserParams params){
    if(!UrlPolicy.mediaOrigin(v.getUrl())){cb.onReceiveValue(null);return true;}
    cancelUpload();upload=cb;multiple=params.getMode()==FileChooserParams.MODE_OPEN_MULTIPLE;accept=params.getAcceptTypes();
    String action=SystemClock.elapsedRealtime()-attachmentActionAt<5000?attachmentAction:"";attachmentAction="";
    int route=UploadPolicy.route(accept,params.isCaptureEnabled(),action);
    android.util.Log.i("GeminiUpload","chooser route="+route+" multiple="+multiple);
    if(route==UploadPolicy.CAMERA)camera();else if(route==UploadPolicy.PHOTOS)pickPhotos();else documents();
    return true;
   }
   @Override public void onPermissionRequest(PermissionRequest r){runOnUiThread(()->webPermission(r));}
   @Override public void onPermissionRequestCanceled(PermissionRequest r){if(pendingPermission==r)pendingPermission=null;}
   @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean gesture,Message result){
    if(!gesture)return false;
    WebView child=new WebView(MainActivity.this);popups.add(child);
    child.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView x,WebResourceRequest req){if(req.isForMainFrame()){navigate(req.getUrl().toString());closePopup(x);return true;}return false;}});
    ((WebView.WebViewTransport)result.obj).setWebView(child);result.sendToTarget();child.postDelayed(()->closePopup(child),10000);return true;
   }
  });
  Bundle state=restore;restore=null;
  boolean restoredVisible=state!=null && state.getBoolean("pageVisible",false);
  if(restoredVisible)revealPage();
  if(state==null || web.restoreState(state)==null)web.loadUrl(HOME);
  else if(!UrlPolicy.navigation(web.getUrl(),restricted()))web.loadUrl(HOME);
 }
 private void closePopup(WebView v){if(popups.remove(v))v.destroy();}
 private void revealPage(){if(pageVisible || launchCover==null)return;pageVisible=true;LinearLayout cover=launchCover;launchCover=null;if(android.animation.ValueAnimator.areAnimatorsEnabled())cover.animate().alpha(0).setDuration(180).withEndAction(()->{if(cover.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)cover.getParent()).removeView(cover);}).start();else if(cover.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)cover.getParent()).removeView(cover);}
 private void launchError(String message){if(!pageVisible && launchCover!=null){launchAttempt++;loadingStage.setText("Could not open Gemini");loadingHint.setText(message);retryLaunch.setVisibility(View.VISIBLE);}else toast(message);}
 private void startLaunchAttempt(){
  final int token=++launchAttempt;if(openIncomplete!=null)openIncomplete.setVisibility(View.GONE);if(retryLaunch!=null)retryLaunch.setVisibility(View.GONE);if(loadingStage!=null)loadingStage.setText("Connecting to Gemini");if(loadingHint!=null)loadingHint.setText("Preparing your chat");
  if(launchCover!=null)launchCover.postDelayed(()->{if(token==launchAttempt && !pageVisible && launchCover!=null){loadingHint.setText("Gemini’s controls are still loading. Retry, or open the incomplete page.");retryLaunch.setVisibility(View.VISIBLE);openIncomplete.setVisibility(View.VISIBLE);}},5000);
 }
 private void awaitGeminiReady(WebView view,String url,int attempt,int token){
  if(pageVisible || view!=web || isDestroyed() || token!=launchAttempt)return;
  if(!UrlPolicy.mediaOrigin(url)){revealPage();return;}
  if(readinessScript==null){try(InputStream in=getAssets().open("launch-readiness.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);readinessScript=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}catch(IOException e){launchError("Could not check the page controls. Retry.");return;}}
  view.evaluateJavascript(readinessScript,value->{if(view!=web || pageVisible || token!=launchAttempt)return;try{org.json.JSONObject state=new org.json.JSONObject(value);if(state.getBoolean("ready")){revealPage();return;}loadingStage.setText(!state.getBoolean("composer")?"Waiting for the chat interface":!state.getBoolean("nav")?"Preparing navigation":"Preparing model controls");if(attempt==0 || attempt==40)android.util.Log.i("GeminiReadiness",state.toString());}catch(Exception ignored){}if(attempt<120)view.postDelayed(()->awaitGeminiReady(view,url,attempt+1,token),250);else{retryLaunch.setVisibility(View.VISIBLE);openIncomplete.setVisibility(View.VISIBLE);}});
 }
 private String integrationScript;
 private String websiteScript(){
  if(integrationScript!=null)return integrationScript;
  try(InputStream in=getAssets().open("gemini-integration.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
   integrationScript=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
  }catch(IOException e){integrationScript="";android.util.Log.w("GeminiPocket","UI integration unavailable",e);}
  return integrationScript;
 }
 private WebViewClient client(){return new WebViewClient(){
  @Override public void onPageCommitVisible(WebView v,String u){awaitGeminiReady(v,u,0,launchAttempt);}

  @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){if(!UrlPolicy.navigation(r.getUrl().toString(),restricted())){if(r.isForMainFrame())external(r.getUrl().toString());return true;}return false;}
  @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
   String u=r.getUrl().toString();
   if(!r.isForMainFrame() && "content".equals(r.getUrl().getScheme()) && "com.geminipocket.capture".equals(r.getUrl().getAuthority()) && UploadPolicy.validName(r.getUrl().getLastPathSegment()))return null;
   boolean ok=r.isForMainFrame()?UrlPolicy.navigation(u,restricted()):UrlPolicy.resource(u,restricted());
   if(ok)return null;android.util.Log.w("GeminiUpload","blocked resource scheme="+r.getUrl().getScheme()+" host="+r.getUrl().getHost());return new WebResourceResponse("text/plain","UTF-8",403,"Blocked by URL policy",Collections.emptyMap(),new ByteArrayInputStream(new byte[0]));
  }
  @Override public void onPageFinished(WebView v,String u){if(UrlPolicy.mediaOrigin(u))v.evaluateJavascript(websiteScript(),null);CookieManager.getInstance().flush();status.setText("Gemini · "+(restricted()?"restricted":"open HTTPS"));}
  @Override public void onReceivedSslError(WebView v,SslErrorHandler h,android.net.http.SslError e){h.cancel();launchError("Secure connection failed. Certificate errors cannot be bypassed.");}
  @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())launchError("Connection failed. Check your network, then Retry or use browser mode.");}
  @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){cancelUpload();v.destroy();web=null;new AlertDialog.Builder(MainActivity.this).setTitle("Page stopped").setMessage("Android stopped the web renderer. Reopen Gemini to continue; an unfinished upload may need to be selected again.").setPositiveButton("Reopen",(a,b)->isolated()).setNegativeButton("Setup",(a,b)->setup()).show();return true;}
 };}
 private void navigate(String u){if(UrlPolicy.navigation(u,restricted()))web.loadUrl(u);else external(u);}
 private void external(String u){
  if(!UrlPolicy.navigation(u,false)){toast("Blocked unsafe or unsupported URL");return;}
  new AlertDialog.Builder(this).setTitle("Outside permitted URLs").setMessage("This link is blocked inside the app. Open in your external browser?\n\n"+u).setNegativeButton("Stay here",null).setPositiveButton("Open browser",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(u)));}catch(ActivityNotFoundException e){toast("No browser found");}}).show();
 }
 private boolean acceptsImages(){if(accept==null || accept.length==0)return true;for(String a:accept)if(a==null || a.isEmpty() || a.equals("*/*") || a.startsWith("image/"))return true;return false;}
 private void pickPhotos(){
  if(Build.VERSION.SDK_INT>=33){Intent i=new Intent(MediaStore.ACTION_PICK_IMAGES);i.setType("image/*");if(multiple)i.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX,Math.min(10,MediaStore.getPickImagesMaxLimit()));
   try{startActivityForResult(i,PICK);return;}catch(ActivityNotFoundException ignored){}
  }
  documents();
 }
 private void documents(){
  Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
  if(accept!=null){ArrayList<String> mime=new ArrayList<>();for(String a:accept)if(a!=null && a.contains("/") && !a.equals("*/*"))mime.add(a);if(!mime.isEmpty())i.putExtra(Intent.EXTRA_MIME_TYPES,mime.toArray(new String[0]));}
  i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,multiple);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);launchPicker(i);
 }
 private void launchPicker(Intent i){try{startActivityForResult(i,PICK);}catch(ActivityNotFoundException e){toast("No file picker available");cancelUpload();}}
 private void camera(){if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},CAMERA_PERMISSION);return;}capture();}
 private void capture(){
  try{
   Intent i=new Intent(this,CameraActivity.class);
   startActivityForResult(i,CAPTURE);
  }catch(Exception e){toast("Camera unavailable");cancelUpload();}
 }
 private void webPermission(PermissionRequest r){
  if(!UrlPolicy.mediaOrigin(r.getOrigin().toString()) || web==null || !UrlPolicy.mediaOrigin(web.getUrl())){r.deny();return;}
  if(pendingPermission!=null)pendingPermission.deny();pendingPermission=r;
  boolean remembered=true;
  for(String resource:r.getResources()){String key=consentKey(resource);if(key==null){r.deny();pendingPermission=null;return;}String choice=prefs.getString(key,"");if("deny".equals(choice)){denyPermission(r);return;}if(!"allow".equals(choice))remembered=false;}
  if(remembered){requestDevicePermissions(r);return;}
  final AnimatedSheetDialog consent=new AnimatedSheetDialog(this);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(pad(20),pad(16),pad(20),pad(28));
  ArrayList<String> devices=new ArrayList<>();for(String resource:r.getResources()){if(PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource))devices.add("microphone");if(PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource))devices.add("camera");}
  if(devices.isEmpty()){denyPermission(r);return;}
  content.addView(text("Gemini device access",22));content.addView(text("Allow gemini.google.com to use your "+android.text.TextUtils.join(" and ",devices)+"?",16));content.addView(text("Only the requested devices are allowed. Android will ask for any missing permissions. Your choice is saved for Gemini on this phone. Reset website device permissions in Browsing controls to ask again. Android permissions still apply.",13));
  action(content,"Allow",()->{consent.dismiss();if(pendingPermission!=r)return;rememberDeviceChoice(r,"allow");requestDevicePermissions(r);});
  action(content,"Deny",()->{rememberDeviceChoice(r,"deny");denyPermission(r);consent.dismiss();});consent.setOnCancelListener(d->denyPermission(r));
  mountSheet(consent,content);

 }
 private String consentKey(String resource){return PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)?"consent.gemini.google.com.audio":PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)?"consent.gemini.google.com.video":null;}
 private void rememberDeviceChoice(PermissionRequest r,String value){android.content.SharedPreferences.Editor editor=prefs.edit();for(String resource:r.getResources()){String key=consentKey(resource);if(key!=null)editor.putString(key,value);}editor.apply();}
 private void requestDevicePermissions(PermissionRequest r){if(pendingPermission!=r)return;ArrayList<String> needed=new ArrayList<>();for(String resource:r.getResources()){String p=PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)?Manifest.permission.CAMERA:PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)?Manifest.permission.RECORD_AUDIO:null;if(p!=null&&checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED&&!needed.contains(p))needed.add(p);}if(needed.isEmpty())grantWeb();else requestPermissions(needed.toArray(new String[0]),WEB_PERMISSION);}
 private void denyPermission(PermissionRequest r){if(pendingPermission==r){r.deny();pendingPermission=null;}}
 private void grantWeb(){
  PermissionRequest r=pendingPermission;pendingPermission=null;if(r==null)return;
  if(web==null || !UrlPolicy.mediaOrigin(web.getUrl())){r.deny();return;}
  ArrayList<String> granted=new ArrayList<>();for(String resource:r.getResources()){
   if(PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource) && checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)granted.add(resource);
   if(PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource) && checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)granted.add(resource);
  }if(granted.isEmpty())r.deny();else r.grant(granted.toArray(new String[0]));
 }
 @Override public void onRequestPermissionsResult(int code,String[] p,int[] results){super.onRequestPermissionsResult(code,p,results);if(code==CAMERA_PERMISSION){if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)capture();else cancelUpload();}if(code==WEB_PERMISSION)grantWeb();}
 @Override protected void onActivityResult(int code,int result,Intent data){
  super.onActivityResult(code,result,data);if(code!=PICK && code!=CAPTURE)return;
  if(code==CAPTURE && result==RESULT_OK && data!=null)captureUri=data.getData();
  ArrayList<Uri> selected=new ArrayList<>();
  if(code==CAPTURE && captureUri!=null){File output=new File(getCacheDir(),captureUri.getLastPathSegment());android.util.Log.i("GeminiUpload","camera returned ok="+(result==RESULT_OK)+" outputBytes="+output.length());}
  if(result==RESULT_OK){if(code==CAPTURE && captureUri!=null)selected.add(captureUri);else if(data!=null){if(data.getClipData()!=null){for(int j=0;j<data.getClipData().getItemCount() && j<50;j++)selected.add(data.getClipData().getItemAt(j).getUri());}else if(data.getData()!=null)selected.add(data.getData());}}
  final Uri cameraOutput=code==CAPTURE?captureUri:null;
  if(code==CAPTURE)captureUri=null;
  final ValueCallback<Uri[]> cb=upload;upload=null;
  android.util.Log.i("GeminiUpload","result code="+code+" ok="+(result==RESULT_OK)+" count="+selected.size()+" callback="+(cb!=null));
  if(cb==null){if(!selected.isEmpty())toast("Page was recreated. Please attach your selection again.");return;}
  if(selected.isEmpty()){cb.onReceiveValue(null);return;}
  preparingAttachment=true;
  new Thread(()->{
   ArrayList<Uri> safe=new ArrayList<>();ArrayList<File> copies=new ArrayList<>();boolean failed=false;
   try{
    for(Uri u:selected){
     if(!"content".equals(u.getScheme()))throw new IOException("Unsupported source");
     String mime=getContentResolver().getType(u);
     String ext=mime==null?null:MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
     if(ext==null || !ext.matches("[A-Za-z0-9]{1,12}"))ext="bin";
     File copy=File.createTempFile("upload_","."+ext,getCacheDir());copies.add(copy);
     try(InputStream in=getContentResolver().openInputStream(u);OutputStream out=new FileOutputStream(copy)){
      if(in==null)throw new IOException("Unreadable source");byte[] buffer=new byte[65536];long size=0;int count;
      while((count=in.read(buffer))!=-1){size+=count;if(size>100L*1024*1024)throw new IOException("File exceeds 100 MB");out.write(buffer,0,count);}
      if(size==0)throw new IOException("Empty file");
     }
     safe.add(Uri.parse("content://com.geminipocket.capture/"+copy.getName()));
    }
   }catch(Exception e){failed=true;android.util.Log.w("GeminiUpload","Cannot prepare selected attachment",e);for(File f:copies)f.delete();safe.clear();}
   if(cameraOutput!=null && !failed && "com.geminipocket.capture".equals(cameraOutput.getAuthority()))new File(getCacheDir(),cameraOutput.getLastPathSegment()).delete();
   final boolean error=failed;
   runOnUiThread(()->{
    preparingAttachment=false;
    if(cameraOutput!=null)revokeUriPermission(cameraOutput,Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
    if(isDestroyed()){cb.onReceiveValue(null);return;}
    if(error)toast("Could not read the attachment (maximum 100 MB). Please select it again.");
    cb.onReceiveValue(safe.isEmpty()?null:safe.toArray(new Uri[0]));
    // Gemini renders its own attachment state; successful handoff is intentionally silent.
   });
  },"GeminiUploadCopy").start();
 }
 private void cancelUpload(){ValueCallback<Uri[]> cb=upload;upload=null;if(cb!=null)cb.onReceiveValue(null);}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putBoolean("pageVisible",pageVisible);if(web!=null)web.saveState(out);if(captureUri!=null)out.putString("capture",captureUri.toString());}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);}
 @Override protected void onStop(){super.onStop();CookieManager.getInstance().flush();}
 @Override public void onBackPressed(){if(returnFromModeSelection())return;if(web!=null && web.canGoBack())web.goBack();else super.onBackPressed();}
 private void destroyWeb(){cancelUpload();if(pendingPermission!=null){pendingPermission.deny();pendingPermission=null;}for(WebView v:new ArrayList<>(popups))closePopup(v);if(web!=null){web.stopLoading();web.destroy();web=null;}}
 @Override protected void onDestroy(){cancelUpdate=true;if(updateConnection!=null)updateConnection.disconnect();destroyWeb();super.onDestroy();}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
