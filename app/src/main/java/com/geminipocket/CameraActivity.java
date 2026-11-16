package com.geminipocket;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.*;
import android.util.Size;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/** Private in-app camera. No external camera intent or full-library permission. */
public class CameraActivity extends Activity {
 private TextureView preview;
 private CameraDevice camera;
 private CameraCaptureSession session;
 private ImageReader reader;
 private CameraManager manager;
 private String[] ids;
 private int selected;
 private HandlerThread worker;
 private Handler handler;
 private Button shutter;
 private boolean taking,active;
 private File output;
 private LinearLayout zoomBar;
 private CaptureRequest.Builder previewRequest;
 private float zoom=1f;
 private boolean opening;
 private int generation;
 private Size previewSize;
 private boolean delivered;
 private TextView zoomLabel;
 private View focusRing;
 private boolean night(){return (getResources().getConfiguration().uiMode&48)==32;}
 private int surface(){return night()?0xFF151820:0xFFF2F5FC;}
 private int ink(){return night()?0xFFE5E9F3:0xFF1B2334;}
 private int accent(){return night()?0xFFAAC7FF:0xFF3459CF;}
 private android.graphics.drawable.GradientDrawable rounded(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}

 private float globalZoom=1f;
 private final Map<String,CameraCharacteristics> capabilities=new HashMap<>();
 private final Handler zoomHandler=new Handler(Looper.getMainLooper());
 private boolean zoomQueued;
 private float pendingZoom=1f;
 private long lastLensSwitch;
 private CameraCharacteristics characteristics(int i)throws CameraAccessException{
  CameraCharacteristics c=capabilities.get(ids[i]);if(c==null){c=manager.getCameraCharacteristics(ids[i]);capabilities.put(ids[i],c);}return c;
 }
 private void queueZoom(float requested){pendingZoom=requested;if(zoomQueued)return;zoomQueued=true;zoomHandler.postDelayed(()->{zoomQueued=false;setZoom(pendingZoom);},33);}

 @Override public void onCreate(Bundle state){
  super.onCreate(state);manager=(CameraManager)getSystemService(CAMERA_SERVICE);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(surface());
  getWindow().setStatusBarColor(surface());getWindow().setNavigationBarColor(surface());
  FrameLayout viewfinder=new FrameLayout(this);preview=new TextureView(this);viewfinder.addView(preview,new FrameLayout.LayoutParams(-1,-1));viewfinder.setBackground(rounded(Color.BLACK,28));viewfinder.setClipToOutline(true);LinearLayout.LayoutParams finderLayout=new LinearLayout.LayoutParams(-1,0,1);finderLayout.setMargins(dp(12),0,dp(12),dp(12));root.addView(viewfinder,finderLayout);
  focusRing=new View(this);android.graphics.drawable.GradientDrawable ring=new android.graphics.drawable.GradientDrawable();ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);ring.setColor(Color.TRANSPARENT);ring.setStroke(dp(2),accent());focusRing.setBackground(ring);focusRing.setVisibility(View.INVISIBLE);viewfinder.addView(focusRing,new FrameLayout.LayoutParams(dp(64),dp(64)));
  zoomLabel=new TextView(this);zoomLabel.setTextColor(ink());zoomLabel.setTextSize(12);zoomLabel.setGravity(Gravity.CENTER);zoomLabel.setText("1× · pinch to zoom · tap to focus");root.addView(zoomLabel);
  final ScaleGestureDetector pinch=new ScaleGestureDetector(this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){queueZoom((zoomQueued?pendingZoom:globalZoom)*d.getScaleFactor());return true;}});
  final GestureDetector taps=new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){@Override public boolean onDown(android.view.MotionEvent e){return true;}@Override public boolean onSingleTapConfirmed(android.view.MotionEvent e){focus(e.getX(),e.getY());return true;}});
  preview.setOnTouchListener((v,e)->{pinch.onTouchEvent(e);if(!pinch.isInProgress())taps.onTouchEvent(e);return true;});
  zoomBar=new LinearLayout(this);zoomBar.setGravity(Gravity.CENTER);zoomBar.setPadding(dp(12),dp(14),dp(12),dp(12));root.addView(zoomBar,new LinearLayout.LayoutParams(-1,dp(76)));
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);controls.setPadding(20,12,20,28);
  Button cancel=new Button(this);cancel.setText("Close");style(cancel,false);controls.addView(cancel,new LinearLayout.LayoutParams(0,dp(56),1));cancel.setOnClickListener(v->finish());
  shutter=new Button(this);shutter.setText("");shutter.setContentDescription("Take photo");shutter.setTextSize(36);style(shutter,true);android.graphics.drawable.GradientDrawable outer=rounded(accent(),48);android.graphics.drawable.GradientDrawable inner=rounded(night()?0xFF172344:Color.WHITE,48);android.graphics.drawable.LayerDrawable shutterLayers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{outer,inner});shutterLayers.setLayerInset(1,dp(7),dp(7),dp(7),dp(7));shutter.setBackground(shutterLayers);LinearLayout.LayoutParams shot=new LinearLayout.LayoutParams(dp(80),dp(80));shot.setMargins(dp(16),0,dp(16),0);controls.addView(shutter,shot);shutter.setOnClickListener(v->take());
  ImageButton lens=new ImageButton(this);lens.setImageResource(com.geminipocket.R.drawable.ic_camera_flip);lens.setContentDescription("Switch front or back camera");lens.setColorFilter(ink());android.graphics.drawable.GradientDrawable flipBg=new android.graphics.drawable.GradientDrawable();flipBg.setColor(night()?0xFF293347:0xFFE0E8F8);flipBg.setCornerRadius(dp(40));lens.setBackground(flipBg);controls.addView(lens,new LinearLayout.LayoutParams(0,dp(56),1));lens.setOnClickListener(v->switchFacing());root.addView(controls);setContentView(root);
  root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
  try{ids=manager.getCameraIdList();for(int i=0;i<ids.length;i++)if(Integer.valueOf(CameraCharacteristics.LENS_FACING_BACK).equals(characteristics(i).get(CameraCharacteristics.LENS_FACING))){selected=i;break;}}catch(Exception e){fail("Cannot list cameras");}
  preview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(SurfaceTexture t,int w,int h){open();}public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){transformPreview();}public boolean onSurfaceTextureDestroyed(SurfaceTexture t){close();return true;}public void onSurfaceTextureUpdated(SurfaceTexture t){}});
 }
 @Override protected void onResume(){super.onResume();active=true;worker=new HandlerThread("CameraPreview");worker.start();handler=new Handler(worker.getLooper());if(preview.isAvailable())open();}
 @Override protected void onPause(){zoomHandler.removeCallbacksAndMessages(null);zoomQueued=false;active=false;close();if(worker!=null){worker.quitSafely();worker=null;}super.onPause();}
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private void style(Button b,boolean primary){b.setAllCaps(false);b.setTextSize(15);b.setTextColor(primary?(night()?0xFF142347:Color.WHITE):ink());android.graphics.drawable.GradientDrawable bg=rounded(primary?accent():(night()?0xFF293347:0xFFE0E8F8),28);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33666666),bg,null));}
 private boolean front(int i)throws Exception{return Integer.valueOf(CameraCharacteristics.LENS_FACING_FRONT).equals(characteristics(i).get(CameraCharacteristics.LENS_FACING));}
 private void switchFacing(){if(taking || opening)return;try{boolean current=front(selected);for(int i=0;i<ids.length;i++)if(front(i)!=current){close();selected=i;zoom=1;globalZoom=1;open();return;}}catch(Exception e){}}
 private float equivalent(int i)throws Exception{CameraCharacteristics c=characteristics(i);float[] f=c.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);android.util.SizeF size=c.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);return f!=null&&f.length>0&&size!=null?(float)(f[0]*43.27/Math.hypot(size.getWidth(),size.getHeight())):24;}
 private void zoomControls(){
  zoomLabel.setText(String.format(Locale.US,"%.1f× · tap to focus",globalZoom));zoomBar.removeAllViews();try{
   boolean selfie=front(selected);float base=equivalent(selected);if(!selfie){for(int i=0;i<ids.length;i++)if(!front(i)){float eq=equivalent(i);if(eq>=20 && eq<=35){base=eq;break;}}}
   java.util.TreeMap<Float,Integer> choices=new java.util.TreeMap<>();
   for(int i=0;i<ids.length;i++)if(front(i)==selfie){float ratio=equivalent(i)/base;float rounded=Math.round(ratio*10)/10f;if(!choices.containsKey(rounded))choices.put(rounded,i);}
   for(java.util.Map.Entry<Float,Integer> entry:choices.entrySet()){
    final int index=entry.getValue();final float ratio=entry.getKey();Button chip=new Button(this);chip.setText(String.format(java.util.Locale.US,"%s×",ratio==(int)ratio?Integer.toString((int)ratio):Float.toString(ratio)));chip.setContentDescription("Select "+chip.getText()+" lens");style(chip,index==selected);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(60),dp(48));lp.setMargins(dp(4),0,dp(4),0);zoomBar.addView(chip,lp);chip.setOnClickListener(v->{if(taking||opening)return;close();selected=index;zoom=1;globalZoom=ratio;open();});
   }
  }catch(Exception ignored){}
 }
 private float[] opticalRatios()throws Exception{
  float base=equivalent(selected);boolean selfie=front(selected);if(!selfie)for(int i=0;i<ids.length;i++)if(!front(i)){float eq=equivalent(i);if(eq>=20&&eq<=35){base=eq;break;}}
  float[] ratios=new float[ids.length];for(int i=0;i<ids.length;i++)if(front(i)==selfie)ratios[i]=Math.round(equivalent(i)/base*10)/10f;return ratios;
 }
 private void setZoom(float requested){if(taking||opening||previewRequest==null||session==null)return;try{
  float[] ratios=opticalRatios();float min=Float.MAX_VALUE,max=1;for(int i=0;i<ratios.length;i++)if(ratios[i]>0){min=Math.min(min,ratios[i]);CameraCharacteristics c=characteristics(i);float digital=1;android.util.Range<Float> range=Build.VERSION.SDK_INT>=30?c.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE):null;Float fallback=c.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);digital=range!=null?range.getUpper():fallback!=null?fallback:1;max=Math.max(max,ratios[i]*digital);}
  globalZoom=Math.max(min,Math.min(max,requested));int target=CameraZoom.lens(ratios,globalZoom);
  if(target!=selected && (SystemClock.elapsedRealtime()-lastLensSwitch<500 || (ratios[target]<ratios[selected] && globalZoom>ratios[selected]*.94f)))target=selected;
  zoom=CameraZoom.local(globalZoom,ratios[target]);zoomLabel.setText(String.format(Locale.US,"%.1f× · tap to focus",globalZoom));
  if(target!=selected){lastLensSwitch=SystemClock.elapsedRealtime();close();selected=target;open();return;}
  applyZoom(previewRequest);session.setRepeatingRequest(previewRequest.build(),null,handler);
 }catch(Exception e){android.util.Log.w("GeminiCamera","Zoom request failed",e);}}
 private int focusGeneration;
 private void showFocusRing(float x,float y){final int token=++focusGeneration;focusRing.animate().cancel();focusRing.setX(x-dp(32));focusRing.setY(y-dp(32));focusRing.setScaleX(1.2f);focusRing.setScaleY(1.2f);focusRing.setAlpha(1f);focusRing.setVisibility(View.VISIBLE);focusRing.animate().scaleX(1).scaleY(1).setDuration(160).start();focusRing.postDelayed(()->{if(token==focusGeneration)focusRing.animate().alpha(0).setDuration(250).withEndAction(()->{if(token==focusGeneration)focusRing.setVisibility(View.INVISIBLE);}).start();},900);}
 private void focus(float x,float y){if(taking||session==null||previewRequest==null)return;showFocusRing(x,y);try{
  CameraCharacteristics c=characteristics(selected);Integer regions=c.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);Rect sensor=c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(regions==null||regions==0||sensor==null)return;
  // Map through the inverse preview transform before scaling into sensor space.
  Matrix inverse=new Matrix();preview.getTransform(new Matrix()).invert(inverse);float[] pt={x,y};inverse.mapPoints(pt);
  int cx=sensor.left+(int)(Math.max(0,Math.min(1,pt[0]/preview.getWidth()))*sensor.width()),cy=sensor.top+(int)(Math.max(0,Math.min(1,pt[1]/preview.getHeight()))*sensor.height());int half=Math.min(sensor.width(),sensor.height())/12;
  Rect box=new Rect(Math.max(sensor.left,cx-half),Math.max(sensor.top,cy-half),Math.min(sensor.right,cx+half),Math.min(sensor.bottom,cy+half));
  previewRequest.set(CaptureRequest.CONTROL_AF_REGIONS,new android.hardware.camera2.params.MeteringRectangle[]{new android.hardware.camera2.params.MeteringRectangle(box,1000)});previewRequest.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO);previewRequest.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_START);session.capture(previewRequest.build(),null,handler);previewRequest.set(CaptureRequest.CONTROL_AF_TRIGGER,CaptureRequest.CONTROL_AF_TRIGGER_IDLE);session.setRepeatingRequest(previewRequest.build(),null,handler);zoomLabel.setText("Focus requested · pinch to zoom");
 }catch(Exception ignored){}}
 private void transformPreview(){if(previewSize==null||preview.getWidth()==0)return;try{
  int rotation=getWindowManager().getDefaultDisplay().getRotation();float w=preview.getWidth(),h=preview.getHeight();int sensor=characteristics(selected).get(CameraCharacteristics.SENSOR_ORIENTATION);boolean swap=((sensor-rotation*90+360)%180)!=0;float bw=swap?previewSize.getHeight():previewSize.getWidth(),bh=swap?previewSize.getWidth():previewSize.getHeight();float scale=Math.max(w/bw,h/bh);Matrix matrix=new Matrix();matrix.setScale(bw*scale/w,bh*scale/h,w/2,h/2);if(rotation==1||rotation==3)matrix.postRotate(rotation==1?-90:90,w/2,h/2);if(front(selected))matrix.postScale(-1,1,w/2,h/2);preview.setTransform(matrix);
 }catch(Exception ignored){}}
 @Override protected void onDestroy(){if(!delivered && output!=null)output.delete();super.onDestroy();}
 private void applyZoom(CaptureRequest.Builder b)throws Exception{
  if(Build.VERSION.SDK_INT>=30){android.util.Range<Float> r=characteristics(selected).get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE);if(r!=null)b.set(CaptureRequest.CONTROL_ZOOM_RATIO,Math.max(r.getLower(),Math.min(r.getUpper(),zoom)));}else{CameraCharacteristics c=characteristics(selected);Rect area=c.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);if(area!=null){int dx=(int)(area.width()*(1-1/zoom)/2),dy=(int)(area.height()*(1-1/zoom)/2);b.set(CaptureRequest.SCALER_CROP_REGION,new Rect(area.left+dx,area.top+dy,area.right-dx,area.bottom-dy));}}
 }
 @SuppressWarnings("MissingPermission") private void open(){
  if(!active || opening || camera!=null || ids==null || ids.length==0 || !preview.isAvailable())return;
  opening=true;final int token=++generation;try{manager.openCamera(ids[selected],new CameraDevice.StateCallback(){public void onOpened(CameraDevice c){opening=false;if(!active || token!=generation){c.close();return;}camera=c;runOnUiThread(()->zoomControls());configure();}public void onDisconnected(CameraDevice c){c.close();camera=null;}public void onError(CameraDevice c,int error){c.close();camera=null;fail("Camera unavailable ("+error+")");}},handler);}catch(Exception e){opening=false;fail("Could not open camera");}
 }
 private void configure(){try{
  CameraCharacteristics ch=characteristics(selected);StreamConfigurationMap map=ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
  Size[] sizes=map.getOutputSizes(ImageFormat.JPEG);Size image=sizes[0];long area=0;for(Size s:sizes)if(s.getWidth()<=2560 && s.getHeight()<=2560 && (long)s.getWidth()*s.getHeight()>area){image=s;area=(long)s.getWidth()*s.getHeight();}
  reader=ImageReader.newInstance(image.getWidth(),image.getHeight(),ImageFormat.JPEG,2);
  reader.setOnImageAvailableListener(r->{try(Image img=r.acquireLatestImage()){if(img==null)return;ByteBuffer b=img.getPlanes()[0].getBuffer();byte[] bytes=new byte[b.remaining()];b.get(bytes);output=File.createTempFile("capture_",".jpg",getCacheDir());try(FileOutputStream out=new FileOutputStream(output)){out.write(bytes);}runOnUiThread(()->review());}catch(Exception e){fail("Could not save photo");}},handler);
  Size[] previews=map.getOutputSizes(SurfaceTexture.class);Size p=previews[0];for(Size s:previews)if(s.getWidth()<=1920 && s.getWidth()>=640){p=s;break;}
  previewSize=p;runOnUiThread(()->transformPreview());preview.getSurfaceTexture().setDefaultBufferSize(p.getWidth(),p.getHeight());Surface surface=new Surface(preview.getSurfaceTexture());
  camera.createCaptureSession(Arrays.asList(surface,reader.getSurface()),new CameraCaptureSession.StateCallback(){public void onConfigured(CameraCaptureSession s){if(camera==null){s.close();return;}session=s;try{CaptureRequest.Builder b=camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);previewRequest=b;b.addTarget(surface);applyZoom(b);b.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);s.setRepeatingRequest(b.build(),null,handler);}catch(Exception e){fail("Preview failed");}}public void onConfigureFailed(CameraCaptureSession s){fail("Camera preview not supported");}},handler);
 }catch(Exception e){fail("Could not start camera preview");}}
 private void take(){if(camera==null || session==null || taking)return;taking=true;shutter.setEnabled(false);try{
  CaptureRequest.Builder b=camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);b.addTarget(reader.getSurface());applyZoom(b);b.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
  CameraCharacteristics ch=characteristics(selected);int sensor=ch.get(CameraCharacteristics.SENSOR_ORIENTATION);int rotation=getWindowManager().getDefaultDisplay().getRotation()*90;boolean front=Integer.valueOf(CameraCharacteristics.LENS_FACING_FRONT).equals(ch.get(CameraCharacteristics.LENS_FACING));b.set(CaptureRequest.JPEG_ORIENTATION,(sensor+(front?rotation:-rotation)+360)%360);
  session.capture(b.build(),null,handler);
 }catch(Exception e){taking=false;shutter.setEnabled(true);fail("Capture failed");}}
 private void review(){if(isFinishing() || !active)return;ImageView photo=new ImageView(this);photo.setAdjustViewBounds(true);BitmapFactory.Options opt=new BitmapFactory.Options();opt.inSampleSize=2;photo.setImageBitmap(BitmapFactory.decodeFile(output.getPath(),opt));
  new android.app.AlertDialog.Builder(this).setTitle("Use this photo?").setView(photo).setCancelable(false).setNegativeButton("Retake",(d,w)->{output.delete();taking=false;shutter.setEnabled(true);}).setPositiveButton("Use photo",(d,w)->{delivered=true;Intent result=new Intent();result.setData(android.net.Uri.parse("content://com.geminipocket.capture/"+output.getName()));setResult(RESULT_OK,result);finish();}).show();
 }
 private void close(){generation++;opening=false;previewRequest=null;if(session!=null){session.close();session=null;}if(camera!=null){camera.close();camera=null;}if(reader!=null){reader.close();reader=null;}}
 private void fail(String message){runOnUiThread(()->{if(!isFinishing()){Toast.makeText(this,message,Toast.LENGTH_LONG).show();setResult(RESULT_CANCELED);finish();}});}
}
