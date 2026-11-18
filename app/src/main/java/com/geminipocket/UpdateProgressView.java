package com.geminipocket;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.animation.ValueAnimator;
/** Determinate wavy circle; percentage comes from received bytes only. */
public final class UpdateProgressView extends View {
 private final Paint paint=new Paint(3);private final Path path=new Path();private int percent;private float phase;private ValueAnimator animator;
 private final LoaderAppearance appearance;
 public UpdateProgressView(Context c){super(c);appearance=LoaderAppearance.read(c);}
 public void progress(int p){percent=Math.max(0,Math.min(100,p));setContentDescription("Download "+percent+" percent");invalidate();}
 protected void onAttachedToWindow(){super.onAttachedToWindow();if(AppearancePolicy.drawWave(appearance.wave,ValueAnimator.areAnimatorsEnabled())){animator=ValueAnimator.ofFloat(0,1);animator.setDuration(1500);animator.setRepeatCount(-1);animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();}}
 protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
 protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-8*d;boolean dark=(getResources().getConfiguration().uiMode&48)==32;paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3*d);paint.setColor(dark?0xFF303A50:0xFFE0E7F5);boolean animate=ValueAnimator.areAnimatorsEnabled();
  float sweep=percent*3.6f;
  paint.setStrokeCap(Paint.Cap.BUTT);
  c.drawArc(cx-r,cy-r,cx+r,cy+r,-90+sweep,360-sweep,false,paint);
  paint.setStrokeCap(Paint.Cap.ROUND);
  paint.setColor(appearance.accent);
  if(!AppearancePolicy.drawWave(appearance.wave,animate)){
   c.drawArc(cx-r,cy-r,cx+r,cy+r,-90,sweep,false,paint);
  }else if(percent>0){
   path.reset();
   for(int i=0;i<=percent*3;i++){
    double a=(i/300.0)*Math.PI*2-Math.PI/2;
    float radius=r+(float)Math.sin(a*12-phase*6.283)*2*d;
    float x=cx+(float)Math.cos(a)*radius,y=cy+(float)Math.sin(a)*radius;
    if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
   }
   c.drawPath(path,paint);
  }paint.setStyle(Paint.Style.FILL);paint.setTextSize(22*d);paint.setTextAlign(Paint.Align.CENTER);c.drawText(percent+"%",cx,cy-(paint.ascent()+paint.descent())/2,paint);}
}
