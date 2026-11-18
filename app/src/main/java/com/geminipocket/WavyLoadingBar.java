package com.geminipocket;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.animation.ValueAnimator;
/** Small indeterminate wave, never represents measured percentage. */
public final class WavyLoadingBar extends View {
 private final Paint p=new Paint(3);private final Path path=new Path();private float phase;private ValueAnimator animator;
 private LoaderAppearance appearance;
 public WavyLoadingBar(Context c){super(c);appearance=LoaderAppearance.read(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
 void applyAppearance(LoaderAppearance next){appearance=next;syncAnimator();invalidate();}
 private void syncAnimator(){
  if(!isAttachedToWindow()||getWindowVisibility()!=VISIBLE||!ValueAnimator.areAnimatorsEnabled()){stopAnimator();return;}
  if(animator!=null)return;
  animator=ValueAnimator.ofFloat(0,1);animator.setDuration(1200);animator.setRepeatCount(ValueAnimator.INFINITE);
  animator.setInterpolator(new android.view.animation.LinearInterpolator());
  animator.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});animator.start();
 }
 private void stopAnimator(){if(animator!=null){animator.cancel();animator=null;}}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();syncAnimator();}
 @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);syncAnimator();}
 @Override protected void onDetachedFromWindow(){stopAnimator();super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas c) {
  float d=getResources().getDisplayMetrics().density;
  float left=3*d, right=getWidth()-left, center=getHeight()/2f;
  if (right<=left) return;
  p.setStyle(Paint.Style.STROKE);
  p.setStrokeWidth(3*d);
  p.setStrokeCap(Paint.Cap.ROUND);
  p.setColor(appearance.accent);
  boolean animate=ValueAnimator.areAnimatorsEnabled();
  if (!AppearancePolicy.drawWave(appearance.wave,animate)) {
   float width=right-left;
   float start=left+width*AppearancePolicy.lineStart(phase,animate);
   float end=left+width*AppearancePolicy.lineEnd(phase,animate);
   if(end>start)c.drawLine(start,center,end,center,p);
   return;
  }
  path.reset();
  int steps=(int)Math.ceil((right-left)/2);
  for(int i=0;i<=steps;i++){
   float offset=(right-left)*i/steps;
   float y=center+(float)Math.sin(offset/(22*d)*Math.PI*2-phase*Math.PI*2)*3*d;
   if(i==0)path.moveTo(left+offset,y);else path.lineTo(left+offset,y);
  }
  c.drawPath(path,p);
 }
}
