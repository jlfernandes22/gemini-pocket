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
   if(Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());android.graphics.Insets ime=i.getInsets(WindowInsets.Type.ime());left=bars.left;top=bars.top;right=bars.right;bottom=web!=null?ime.bottom:Math.max(bars.bottom,ime.bottom);}else{left=i.getSystemWindowInsetLeft();top=i.getSystemWindowInsetTop();right=i.getSystemWindowInsetRight();bottom=i.getSystemWindowInsetBottom();}
   root.setPadding(left,top,right,bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:i.consumeSystemWindowInsets();
  });backdrop.requestApplyInsets();
 }
 private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(dark()?Color.rgb(232,234,243):Color.rgb(25,31,50));t.setPadding(pad(20),pad(12),pad(20),pad(12));return t;}
 private void action(LinearLayout parent,String title,Runnable r){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(15);b.setMinHeight(pad(48));boolean primary="Apply".equals(title);b.setTextColor(primary?(dark()?0xFF102047:Color.WHITE):(dark()?0xFFD7E2FF:0xFF304D8C));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(primary?(dark()?0xFFB8CBFF:0xFF3459CF):(dark()?0xFF2B303A:0xFFE3EAF7));bg.setCornerRadius(pad(24));b.setBackground(bg);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,pad(48));lp.setMargins(pad(8),pad(5),pad(8),pad(5));parent.addView(b,lp);b.setOnClickListener(v->r.run());}
 private void setup(){
  base();ScrollView scroll=new ScrollView(this);LinearLayout content=new LinearLayout(this);content.setPadding(pad(12),pad(24),pad(12),pad(24));content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll);
  content.addView(text("Gemini Pocket",30));content.addView(text("Choose how you connect",20));
  content.addView(text("Browser session\nUse an existing Gemini login in a supported browser. Your cookies and passwords stay in that browser. The browser controls permissions, uploads and navigation; this app cannot restrict its URLs.",16));
  action(content,"Choose browser & continue",()->chooseBrowser());
  content.addView(text("Isolated app\nSeparate sign-in, modern photo picker and camera support. Required Google URLs only by default. Google may decline sign-in inside embedded browsers; browser mode is the reliable alternative.",16));
  action(content,"Use isolated app",()->{if(web!=null && returnFromModeSelection())return;confirmModeSelection();prefs.edit().putString("mode","isolated").apply();isolated();});
  content.addView(text("Switch modes in Settings anytime. Switching apps keeps your page where possible, but Android or the website can stop background work. Not affiliated with Google.",14));
 }
 private void chooseBrowser(){
  Intent service=new Intent("android.support.customtabs.action.CustomTabsService");
  List<ResolveInfo> found=getPackageManager().queryIntentServices(service,0);
  ArrayList<String> packages=new ArrayList<>(),labels=new ArrayList<>();
  for(ResolveInfo ri:found){String p=ri.serviceInfo.packageName;if(!packages.contains(p)){packages.add(p);labels.add(ri.loadLabel(getPackageManager()).toString()+"\n"+p);}}
  if(packages.isEmpty()){new AlertDialog.Builder(this).setTitle("No supported browser found").setMessage("Install or enable a browser supporting Custom Tabs, such as Chrome or Firefox, then try again. You can also use isolated mode.").setPositiveButton("OK",null).show();return;}
  new AlertDialog.Builder(this).setTitle("Choose browser").setItems(labels.toArray(new String[0]),(d,i)->{
   new AlertDialog.Builder(this).setTitle("Continue with "+labels.get(i).split("\n")[0]+"?").setMessage("Gemini will use this browser's existing cookies and sign-in. Gemini Pocket does not read or import your account credentials. Browser permissions and navigation rules apply, not the isolated-mode URL restriction.").setNegativeButton("Cancel",null).setPositiveButton("Allow & open",(x,y)->{
    confirmModeSelection();prefs.edit().putString("mode","browser").putString("browser",packages.get(i)).apply();browserHome();openBrowser();
   }).show();
  }).setNegativeButton("Cancel",null).show();
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
 private void settings(){
  final Dialog sheet=new Dialog(this,R.style.BrowsingSheetTheme);LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(pad(16),pad(12),pad(16),pad(24));
  android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark()?Color.rgb(30,31,33):Color.rgb(242,245,252));bg.setCornerRadii(new float[]{pad(28),pad(28),pad(28),pad(28),0,0,0,0});c.setBackground(bg);
  c.addView(text("Browsing controls",22));c.addView(text("Gemini Pocket settings · not Google account settings",12));
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
  action(c,"Reload page",()->{sheet.dismiss();if(web!=null)web.reload();});
  action(c,"Home",()->{sheet.dismiss();if(web!=null)web.loadUrl(HOME);});
  action(c,"Reset website device permissions",()->{prefs.edit().remove("consent.gemini.google.com.audio").remove("consent.gemini.google.com.video").apply();if(pendingPermission!=null)denyPermission(pendingPermission);sheet.dismiss();if(web!=null)web.reload();toast("Website device choices reset. Gemini will ask again.");});
  action(c,"Privacy & storage",()->{sheet.dismiss();privacyControls();});
  action(c,"Close",()->sheet.dismiss());ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(c);
  FrameLayout sheetHost=new FrameLayout(this);sheetHost.setOnClickListener(v->sheet.dismiss());scroll.setOnClickListener(v->{});FrameLayout.LayoutParams panel=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);sheetHost.addView(scroll,panel);sheet.setContentView(sheetHost);
  Window w=sheet.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);w.setDimAmount(.4f);sheet.show();w.setLayout(-1,-1);w.setGravity(Gravity.FILL);w.setNavigationBarColor(dark()?Color.rgb(30,31,33):Color.rgb(242,245,252));
  if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams attrs=w.getAttributes();attrs.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;w.setAttributes(attrs);}
  if(Build.VERSION.SDK_INT>=30){w.setDecorFitsSystemWindows(false);w.setNavigationBarContrastEnforced(false);}else w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  scroll.setBackground(c.getBackground());c.setBackground(null);sheetHost.setOnApplyWindowInsetsListener((v,insets)->{int bottom=Build.VERSION.SDK_INT>=30?insets.getInsets(WindowInsets.Type.navigationBars()).bottom:insets.getSystemWindowInsetBottom();c.setPadding(pad(16),pad(12),pad(16),pad(24)+bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:insets.consumeSystemWindowInsets();});sheetHost.requestApplyInsets();
 }
 private void privacyControls(){
  final Dialog sheet=new Dialog(this,R.style.BrowsingSheetTheme);
  LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(pad(16),pad(12),pad(16),pad(24));
  c.addView(text("Privacy & storage",22));c.addView(text("Manage this app’s local data",14));
  String[] titles={"Temporary attachments","Website cache","Reset website session"};
  String[] details={"Remove camera and upload copies. Gallery originals stay safe.","Free downloaded resources without signing out.","Sign out of isolated mode and erase its website data."};
  for(int i=0;i<titles.length;i++){final int which=i;c.addView(text(details[i],13));action(c,titles[i],()->{sheet.dismiss();confirmPrivacy(which);});}
  action(c,"Back to browsing controls",()->{sheet.dismiss();settings();});action(c,"Close",()->sheet.dismiss());
  ScrollView scroll=new ScrollView(this);scroll.addView(c);FrameLayout host=new FrameLayout(this);host.setOnClickListener(v->sheet.dismiss());scroll.setOnClickListener(v->{});host.addView(scroll,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
  android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark()?0xFF1E1F21:0xFFF2F5FC);bg.setCornerRadii(new float[]{pad(28),pad(28),pad(28),pad(28),0,0,0,0});scroll.setBackground(bg);sheet.setContentView(host);Window w=sheet.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.4f);sheet.show();w.setLayout(-1,-1);if(Build.VERSION.SDK_INT>=30){w.setDecorFitsSystemWindows(false);w.setNavigationBarContrastEnforced(false);}w.setNavigationBarColor(dark()?0xFF1E1F21:0xFFF2F5FC);
  host.setOnApplyWindowInsetsListener((v,insets)->{int bottom=Build.VERSION.SDK_INT>=30?insets.getInsets(WindowInsets.Type.navigationBars()).bottom:insets.getSystemWindowInsetBottom();c.setPadding(pad(16),pad(12),pad(16),pad(24)+bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:insets.consumeSystemWindowInsets();});host.requestApplyInsets();
 }
 private void confirmPrivacy(int which){
   String message=which==0?"Remove app-generated attachment copies. Do not continue while an upload or draft attachment is in use. Your gallery originals are untouched.":which==1?"Clear downloaded website resources. Cookies and sign-in remain. Avoid this during uploads.":"This clears isolated-mode cookies and website storage and reloads Gemini. Browser-mode sign-in is not affected. Drafts may be lost.";
   new AlertDialog.Builder(this).setTitle("Confirm cleanup").setMessage(message).setNegativeButton("Cancel",null).setPositiveButton("Continue",(a,b)->{
    if(upload!=null || preparingAttachment){toast("Finish attaching files before cleanup.");return;}
    if(which==0){new Thread(()->{java.io.File[] files=getCacheDir().listFiles();int removed=0;if(files!=null)for(java.io.File f:files)if(f.isFile()&&UploadPolicy.validName(f.getName())&&f.delete())removed++;final int count=removed;runOnUiThread(()->toast("Removed "+count+" temporary attachments"));},"AttachmentCleanup").start();}
    else if(which==1){if(web!=null)web.clearCache(true);toast("Website cache cleared");}
    else{if(web!=null)web.stopLoading();CookieManager.getInstance().removeAllCookies(ok->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();if(web!=null){web.clearHistory();web.loadUrl(HOME);}});}
   }).show();
 }
 private boolean preparingAttachment;
 private void updateShield(){if(shield!=null){shield.setContentDescription("Browsing controls: "+(restricted()?"restricted":"open HTTPS"));shield.setAlpha(restricted()?1f:.65f);}}
 @SuppressWarnings("SetJavaScriptEnabled") private void isolated(){
  base();status=new TextView(this);
  FrameLayout stage=new FrameLayout(this);root.addView(stage,new LinearLayout.LayoutParams(-1,0,1));
  web=new WebView(this);web.setBackgroundColor(dark()?Color.BLACK:Color.WHITE);stage.addView(web,new FrameLayout.LayoutParams(-1,-1));
  pageVisible=false;
  launchCover=new LinearLayout(this);launchCover.setOrientation(LinearLayout.VERTICAL);launchCover.setGravity(Gravity.CENTER);launchCover.setBackgroundColor(dark()?Color.BLACK:Color.WHITE);
  ExpressiveLoader loader=new ExpressiveLoader(this);launchCover.addView(loader,new LinearLayout.LayoutParams(pad(96),pad(96)));
  loadingStage=text("Connecting to Gemini",18);loadingStage.setGravity(Gravity.CENTER);loadingStage.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);launchCover.addView(loadingStage);
  WavyLoadingBar wave=new WavyLoadingBar(this);LinearLayout.LayoutParams waveSize=new LinearLayout.LayoutParams(pad(180),pad(24));waveSize.setMargins(0,pad(6),0,pad(8));launchCover.addView(wave,waveSize);
  loadingHint=text("Preparing your chat",14);loadingHint.setGravity(Gravity.CENTER);loadingHint.setAlpha(.7f);launchCover.addView(loadingHint);
  retryLaunch=new Button(this);retryLaunch.setText("↻  Retry");retryLaunch.setAllCaps(false);retryLaunch.setTextSize(15);retryLaunch.setTextColor(dark()?0xFF102047:Color.WHITE);retryLaunch.setPadding(pad(24),0,pad(24),0);android.graphics.drawable.GradientDrawable retryBg=new android.graphics.drawable.GradientDrawable();retryBg.setColor(dark()?0xFFAAC7FF:0xFF3459CF);retryBg.setCornerRadius(pad(28));retryLaunch.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33666666),retryBg,null));retryLaunch.setVisibility(View.GONE);LinearLayout.LayoutParams retrySize=new LinearLayout.LayoutParams(-2,pad(48));retrySize.setMargins(0,pad(16),0,0);launchCover.addView(retryLaunch,retrySize);
  retryLaunch.setOnClickListener(v->{startLaunchAttempt();web.reload();});
  stage.addView(launchCover,new FrameLayout.LayoutParams(-1,-1));startLaunchAttempt();
  shield=new ImageButton(this);shield.setImageResource(R.drawable.ic_shield);shield.setColorFilter(dark()?Color.rgb(210,220,240):Color.rgb(65,80,110));shield.setPadding(pad(12),pad(12),pad(12),pad(12));android.graphics.drawable.GradientDrawable buttonBg=new android.graphics.drawable.GradientDrawable();buttonBg.setColor(dark()?0xDD24262B:0xDDEEF2FA);buttonBg.setCornerRadius(pad(22));shield.setBackground(buttonBg);
  FrameLayout.LayoutParams shieldLayout=new FrameLayout.LayoutParams(pad(44),pad(44),Gravity.TOP|Gravity.END);shieldLayout.topMargin=pad(58);shieldLayout.rightMargin=pad(8);stage.addView(shield,shieldLayout);updateShield();shield.setOnClickListener(v->settings());
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setMediaPlaybackRequiresUserGesture(true);s.setSafeBrowsingEnabled(true);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
  web.setWebViewClient(client());web.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onConsoleMessage(ConsoleMessage m){
    if(m.message().matches("GP_MEDIA (granted|ended|error [A-Za-z]+)")){android.util.Log.i("GeminiMedia",m.message());return true;}if(m.message().matches("GP_ACTION (photos|files|camera)")){attachmentAction=m.message().substring(10);attachmentActionAt=SystemClock.elapsedRealtime();return true;}
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
  final int token=++launchAttempt;if(retryLaunch!=null)retryLaunch.setVisibility(View.GONE);if(loadingStage!=null)loadingStage.setText("Connecting to Gemini");if(loadingHint!=null)loadingHint.setText("Preparing your chat");
  if(launchCover!=null)launchCover.postDelayed(()->{if(token==launchAttempt && !pageVisible && launchCover!=null){loadingHint.setText("Gemini is taking longer than usual.\nCheck your connection or try again.");retryLaunch.setVisibility(View.VISIBLE);}},20000);
 }
 private void awaitGeminiReady(WebView view,String url,int attempt,int token){
  if(pageVisible || view!=web || isDestroyed() || token!=launchAttempt)return;
  if(!UrlPolicy.mediaOrigin(url)){revealPage();return;}
  view.evaluateJavascript("(function(){const visible=n=>!!n&&n.getBoundingClientRect().width>0&&n.getBoundingClientRect().height>0;const input=Array.from(document.querySelectorAll('[contenteditable=true],textarea')).some(visible);const nav=Array.from(document.querySelectorAll('button,[role=button]')).some(n=>visible(n)&&/menu|menu_open|navigation|navega|expand.*menu/i.test((n.getAttribute('aria-label')||'')+' '+(n.getAttribute('data-test-id')||'')+' '+n.textContent));return input&&nav?2:input?1:0;})()",value->{if(view!=web || pageVisible || token!=launchAttempt)return;if("2".equals(value) || ("1".equals(value) && attempt>=8))revealPage();else{loadingStage.setText("1".equals(value)?"Preparing navigation":"Waiting for the chat interface");if(attempt<120)view.postDelayed(()->awaitGeminiReady(view,url,attempt+1,token),250);else if(retryLaunch!=null)retryLaunch.setVisibility(View.VISIBLE);}});
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
  final Dialog consent=new Dialog(this,R.style.BrowsingSheetTheme);LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(pad(20),pad(16),pad(20),pad(28));
  ArrayList<String> devices=new ArrayList<>();for(String resource:r.getResources()){if(PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource))devices.add("microphone");if(PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource))devices.add("camera");}
  if(devices.isEmpty()){denyPermission(r);return;}
  content.addView(text("Gemini device access",22));content.addView(text("Allow gemini.google.com to use your "+android.text.TextUtils.join(" and ",devices)+"?",16));content.addView(text("Only the requested devices are allowed. Android will ask for any missing permissions. Your choice is saved for Gemini on this phone. Reset website device permissions in Browsing controls to ask again. Android permissions still apply.",13));
  action(content,"Allow",()->{consent.dismiss();if(pendingPermission!=r)return;rememberDeviceChoice(r,"allow");requestDevicePermissions(r);});
  action(content,"Deny",()->{rememberDeviceChoice(r,"deny");denyPermission(r);consent.dismiss();});consent.setOnCancelListener(d->denyPermission(r));
  FrameLayout host=new FrameLayout(this);host.setOnClickListener(v->{denyPermission(r);consent.dismiss();});content.setOnClickListener(v->{});android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(dark()?0xFF1E1F21:0xFFF2F5FC);bg.setCornerRadii(new float[]{pad(28),pad(28),pad(28),pad(28),0,0,0,0});content.setBackground(bg);host.addView(content,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));consent.setContentView(host);Window w=consent.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.4f);consent.show();w.setLayout(-1,-1);w.setNavigationBarColor(dark()?0xFF1E1F21:0xFFF2F5FC);if(Build.VERSION.SDK_INT>=30){w.setDecorFitsSystemWindows(false);w.setNavigationBarContrastEnforced(false);}host.setOnApplyWindowInsetsListener((v,i)->{int bottom=Build.VERSION.SDK_INT>=30?i.getInsets(WindowInsets.Type.navigationBars()).bottom:i.getSystemWindowInsetBottom();content.setPadding(pad(20),pad(16),pad(20),pad(28)+bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:i.consumeSystemWindowInsets();});host.requestApplyInsets();

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
 @Override protected void onDestroy(){destroyWeb();super.onDestroy();}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
