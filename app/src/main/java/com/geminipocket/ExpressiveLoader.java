package com.geminipocket;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.animation.ValueAnimator;

/** Canvas approximation of M3 expressive shapes; see docs/loader-references.md. */
public final class ExpressiveLoader extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path path=new Path();
 private ValueAnimator motion;
 private float elapsed;
 private int accent;
 private AppearancePolicy.StartupStyle startup;
 private static final long ARC_DURATION = 1450;

 void applyAppearance(LoaderAppearance appearance){
  accent=appearance.accent;
  if(startup!=appearance.startup){stopMotion();elapsed=0;startup=appearance.startup;}
  setVisibility(startup==AppearancePolicy.StartupStyle.HIDDEN?GONE:VISIBLE);
  syncMotion();invalidate();
 }
 public ExpressiveLoader(Context c){super(c);applyAppearance(LoaderAppearance.read(c));setContentDescription("Loading Gemini");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();syncMotion();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)syncMotion();else stopMotion();}
 @Override protected void onDetachedFromWindow(){stopMotion();super.onDetachedFromWindow();}

 private void syncMotion(){
  if(!isAttachedToWindow()||getWindowVisibility()!=VISIBLE||startup==null
      ||startup==AppearancePolicy.StartupStyle.HIDDEN||!ValueAnimator.areAnimatorsEnabled()){stopMotion();return;}
  if(motion!=null)return;
  long duration=startup==AppearancePolicy.StartupStyle.ARC?ARC_DURATION:650*7;
  motion=ValueAnimator.ofFloat(0,duration);motion.setDuration(duration);motion.setRepeatCount(ValueAnimator.INFINITE);
  motion.setInterpolator(new android.view.animation.LinearInterpolator());
  motion.addUpdateListener(a->{elapsed=(float)a.getAnimatedValue();invalidate();});motion.start();
 }

 private void stopMotion(){if(motion!=null){motion.cancel();motion=null;}}
 private double radius(int shape,double a){switch(shape%7){
  case 0:return .86+.12*Math.cos(10*a);
  case 1:return .9+.08*Math.cos(9*a);
  case 2:return .88+.09*Math.cos(5*a);
  case 3:return 1/Math.sqrt(Math.pow(Math.cos(a)/1.05,2)+Math.pow(Math.sin(a)/.64,2));
  case 4:return .86+.13*Math.cos(8*a);
  case 5:return .88+.1*Math.cos(4*a);
  default:return 1/Math.sqrt(Math.pow(Math.cos(a)/1.03,2)+Math.pow(Math.sin(a)/.78,2));
 }}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);
  if(startup==AppearancePolicy.StartupStyle.HIDDEN)return;
  float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.69f;
  boolean animate=ValueAnimator.areAnimatorsEnabled();
  if(startup==AppearancePolicy.StartupStyle.ARC){
   float phase=elapsed/ARC_DURATION;
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(r*.17f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(accent);
   c.drawArc(x-r,y-r,x+r,y+r,AppearancePolicy.arcStart(phase,animate),AppearancePolicy.arcSweep(phase,animate),false,paint);
   return;
  }
  paint.setStyle(Paint.Style.FILL);
  float time=animate?elapsed:0;
  path.reset();int shape=(int)(time/650);double t=(time%650)/650;double morph=1-Math.exp(-7*t)*Math.cos(10*t);double rotation=time/4666*Math.PI*2+shape*Math.PI/2+morph*Math.PI/2;
  for(int i=0;i<=240;i++){double a=i*Math.PI*2/240;float rad=r*(float)(radius(shape,a)*(1-morph)+radius(shape+1,a)*morph);float px=x+rad*(float)Math.cos(a+rotation),py=y+rad*(float)Math.sin(a+rotation);if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();paint.setColor(accent);c.drawPath(path,paint);
 }
}
