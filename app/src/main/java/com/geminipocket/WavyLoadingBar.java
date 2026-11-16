package com.geminipocket;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.animation.ValueAnimator;
/** Small indeterminate wave, never represents measured percentage. */
public final class WavyLoadingBar extends View {
 private final Paint p=new Paint(3);private final Path path=new Path();private float phase;private ValueAnimator animator;
 public WavyLoadingBar(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(ValueAnimator.areAnimatorsEnabled()){animator=ValueAnimator.ofFloat(0,1);animator.setDuration(1200);animator.setRepeatCount(-1);animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}}
 @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density;boolean dark=(getResources().getConfiguration().uiMode&48)==32;p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3*d);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(dark?0xFF303A50:0xFFE0E7F5);c.drawLine(3*d,getHeight()/2f,getWidth()-3*d,getHeight()/2f,p);path.reset();for(int i=0;i<=getWidth()-6*d;i+=2){float x=3*d+i,y=getHeight()/2f+(float)Math.sin(i/(22*d)*Math.PI*2-phase*Math.PI*2)*3*d;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}p.setColor(dark?0xFFAAC7FF:0xFF3459CF);c.drawPath(path,p);}
}
