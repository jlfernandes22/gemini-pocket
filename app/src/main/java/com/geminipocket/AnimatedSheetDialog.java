package com.geminipocket;

import android.app.Dialog;
import android.content.Context;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.PathInterpolator;

/** Shared sheet motion for buttons, Back, scrim and swipe dismissal. */
public final class AnimatedSheetDialog extends Dialog {
 private View panel;
 private Runnable backAction;
 private Runnable afterDismiss;
 public void backAction(Runnable action){backAction=action;}
 @Override public void onBackPressed(){if(backAction!=null){afterDismiss=backAction;dismiss();}else cancel();}
 private void finishDismiss(){super.dismiss();Runnable next=afterDismiss;afterDismiss=null;if(next!=null)next.run();}
 private boolean exiting;
 public AnimatedSheetDialog(Context c){super(c,R.style.BrowsingSheetTheme);}
 public void panel(View view){panel=view;}
 @Override public void show(){super.show();exiting=false;if(panel!=null){panel.setAlpha(0);panel.post(()->{if(!isShowing()||exiting)return;if(!ValueAnimator.areAnimatorsEnabled()){panel.setAlpha(1);return;}panel.setTranslationY(panel.getHeight());panel.animate().alpha(1).translationY(0).setDuration(320).setInterpolator(new PathInterpolator(.2f,0,0,1)).start();});}}
 @Override public void dismiss(){if(exiting)return;if(panel==null||!isShowing()||!ValueAnimator.areAnimatorsEnabled()){finishDismiss();return;}exiting=true;panel.animate().cancel();panel.animate().translationY(panel.getHeight()).alpha(0).setDuration(220).setInterpolator(new PathInterpolator(.4f,0,1,1)).withEndAction(()->{exiting=false;finishDismiss();}).start();}
 @Override public void cancel(){super.cancel();}
}
