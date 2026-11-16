package com.geminipocket;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Indeterminate check transitions to a real completed-result checkmark. */
public final class UpdateCheckView extends View {
 private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 private final Path wave=new Path();
 private ValueAnimator spin,morph;
 private float phase,completion;
 private boolean complete;
 public UpdateCheckView(Context c){super(c);setContentDescription("Checking for updates");}
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(!complete&&ValueAnimator.areAnimatorsEnabled()){spin=ValueAnimator.ofFloat(0,1);spin.setDuration(1500);spin.setRepeatCount(ValueAnimator.INFINITE);spin.setInterpolator(new android.view.animation.LinearInterpolator());spin.addUpdateListener(a->{phase=(float)a.getAnimatedValue();invalidate();});spin.start();}}
 public void finishCheck(){complete=true;setContentDescription("Update check complete");if(spin!=null)spin.cancel();if(!ValueAnimator.areAnimatorsEnabled()){completion=1;invalidate();return;}morph=ValueAnimator.ofFloat(completion,1);morph.setDuration(400);morph.setInterpolator(new android.view.animation.PathInterpolator(.2f,0,0,1));morph.addUpdateListener(a->{completion=(float)a.getAnimatedValue();invalidate();});morph.start();}
 @Override protected void onDetachedFromWindow(){if(spin!=null)spin.cancel();if(morph!=null)morph.cancel();super.onDetachedFromWindow();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);float d=getResources().getDisplayMetrics().density,x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)-10*d;boolean dark=(getResources().getConfiguration().uiMode&48)==32;int accent=dark?0xFFAAC7FF:0xFF3459CF;
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(3*d);paint.setColor(dark?0xFF303A50:0xFFE0E7F5);c.drawCircle(x,y,r,paint);
  wave.reset();float sweep=complete?260+100*completion:260;float start=complete?phase*360:phase*360;for(int i=0;i<=180;i++){double a=Math.toRadians(start+sweep*i/180-90);float rr=r+(1-completion)*3*d*(float)Math.sin(a*10-phase*6.283);float px=x+rr*(float)Math.cos(a),py=y+rr*(float)Math.sin(a);if(i==0)wave.moveTo(px,py);else wave.lineTo(px,py);}paint.setColor(accent);c.drawPath(wave,paint);
  if(completion>0){paint.setAlpha((int)(255*completion));Path tick=new Path();tick.moveTo(x-r*.4f,y);tick.lineTo(x-r*.1f,y+r*.28f);tick.lineTo(x+r*.43f,y-r*.3f);c.save();c.scale(.75f+.25f*completion,.75f+.25f*completion,x,y);c.drawPath(tick,paint);c.restore();paint.setAlpha(255);}
 }
}
