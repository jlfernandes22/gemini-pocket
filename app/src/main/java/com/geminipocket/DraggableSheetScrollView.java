package com.geminipocket;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.*;
import android.widget.ScrollView;

/** Owns the entire touch stream, including non-clickable handle/header space. */
public final class DraggableSheetScrollView extends ScrollView {
 private float startX,startY;private boolean eligible,dragging,settling;private Runnable dismiss;
 public DraggableSheetScrollView(Context c){super(c);}
 public void onDismiss(Runnable r){dismiss=r;}
 @Override public boolean dispatchTouchEvent(MotionEvent e){
  if(settling)return true;
  int action=e.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){animate().cancel();startX=e.getRawX();startY=e.getRawY();eligible=!canScrollVertically(-1);dragging=false;}
  if(action==MotionEvent.ACTION_POINTER_DOWN)eligible=false;
  float dy=e.getRawY()-startY,dx=e.getRawX()-startX;
  if(!dragging&&action==MotionEvent.ACTION_MOVE&&eligible){int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();if(dy < -slop)eligible=false;else if(dy>slop*2&&dy>Math.abs(dx)*1.5f){dragging=true;MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();}}
  if(dragging){
   if(action==MotionEvent.ACTION_MOVE){setTranslationY(Math.max(0,dy));return true;}
   if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){boolean close=action==MotionEvent.ACTION_UP&&getTranslationY()>Math.min(getHeight()*.18f,96*getResources().getDisplayMetrics().density);dragging=false;
    if(close&&dismiss!=null){settling=true;dismiss.run();}
    else if(!ValueAnimator.areAnimatorsEnabled())setTranslationY(0);
    else{settling=true;animate().translationY(0).setDuration(220).withEndAction(()->settling=false).start();}
    return true;
   }return true;
  }
  boolean handled=super.dispatchTouchEvent(e);
  // Own DOWN even on the empty grip/header so later MOVE events reach us.
  return action==MotionEvent.ACTION_DOWN || handled;
 }
}
