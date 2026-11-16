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
 public ExpressiveLoader(Context c){super(c);setContentDescription("Loading Gemini");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(ValueAnimator.areAnimatorsEnabled()){motion=ValueAnimator.ofFloat(0,650*7);motion.setDuration(650*7);motion.setRepeatCount(ValueAnimator.INFINITE);motion.setInterpolator(new android.view.animation.LinearInterpolator());motion.addUpdateListener(a->{elapsed=(float)a.getAnimatedValue();invalidate();});motion.start();}}
 @Override protected void onDetachedFromWindow(){if(motion!=null){motion.cancel();motion=null;}super.onDetachedFromWindow();}
 private double radius(int shape,double a){switch(shape%7){
  case 0:return .86+.12*Math.cos(10*a);
  case 1:return .9+.08*Math.cos(9*a);
  case 2:return .88+.09*Math.cos(5*a);
  case 3:return 1/Math.sqrt(Math.pow(Math.cos(a)/1.05,2)+Math.pow(Math.sin(a)/.64,2));
  case 4:return .86+.13*Math.cos(8*a);
  case 5:return .88+.1*Math.cos(4*a);
  default:return 1/Math.sqrt(Math.pow(Math.cos(a)/1.03,2)+Math.pow(Math.sin(a)/.78,2));
 }}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*.69f;path.reset();int shape=(int)(elapsed/650);double t=(elapsed%650)/650;double morph=1-Math.exp(-7*t)*Math.cos(10*t);double rotation=elapsed/4666*Math.PI*2+shape*Math.PI/2+morph*Math.PI/2;
  for(int i=0;i<=240;i++){double a=i*Math.PI*2/240;float rad=r*(float)(radius(shape,a)*(1-morph)+radius(shape+1,a)*morph);float px=x+rad*(float)Math.cos(a+rotation),py=y+rad*(float)Math.sin(a+rotation);if(i==0)path.moveTo(px,py);else path.lineTo(px,py);}path.close();boolean dark=(getResources().getConfiguration().uiMode&48)==32;paint.setColor(dark?0xFFAAC7FF:0xFF3459CF);c.drawPath(path,paint);
 }
}
