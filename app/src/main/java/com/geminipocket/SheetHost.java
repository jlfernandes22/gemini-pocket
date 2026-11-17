package com.geminipocket;
import android.graphics.Color;
import android.os.Build;
import android.view.*;
import android.widget.*;
/** Common window/inset/handle mounting; individual screens only supply content. */
public final class SheetHost {
 private SheetHost(){}
 private static int dp(android.content.Context c,int value){return (int)(value*c.getResources().getDisplayMetrics().density);}
 public static void mount(android.app.Activity activity,AnimatedSheetDialog sheet,LinearLayout c){
  boolean night=(activity.getResources().getConfiguration().uiMode&48)==32;
  View handle=new View(activity);android.graphics.drawable.GradientDrawable grip=new android.graphics.drawable.GradientDrawable();grip.setColor(night?0xFF8F98A8:0xFF737D8E);grip.setCornerRadius(dp(activity,3));handle.setBackground(grip);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(dp(activity,36),dp(activity,4));hp.gravity=Gravity.CENTER;hp.setMargins(0,dp(activity,4),0,dp(activity,16));FrameLayout gripArea=new FrameLayout(activity);gripArea.setContentDescription("Drag down to dismiss");FrameLayout.LayoutParams gripSize=new FrameLayout.LayoutParams(dp(activity,36),dp(activity,4),Gravity.CENTER);gripArea.addView(handle,gripSize);c.addView(gripArea,0,new LinearLayout.LayoutParams(-1,dp(activity,40)));
  DraggableSheetScrollView scroll=new DraggableSheetScrollView(activity);scroll.onDismiss(()->sheet.cancel());scroll.addView(c);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(night?0xFF1E1F21:0xFFF2F5FC);bg.setCornerRadii(new float[]{dp(activity,28),dp(activity,28),dp(activity,28),dp(activity,28),0,0,0,0});scroll.setBackground(bg);c.setBackground(null);FrameLayout host=new FrameLayout(activity);host.setOnClickListener(v->sheet.cancel());scroll.setOnClickListener(v->{});FrameLayout.LayoutParams panel=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);host.addView(scroll,panel);sheet.setContentView(host);sheet.panel(scroll);Window w=sheet.getWindow();w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND|WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);w.setDimAmount(.4f);w.setLayout(-1,-1);sheet.show();w.setLayout(-1,-1);w.setGravity(Gravity.FILL);w.setNavigationBarColor(night?0xFF1E1F21:0xFFF2F5FC);if(Build.VERSION.SDK_INT>=30){w.setDecorFitsSystemWindows(false);w.setNavigationBarContrastEnforced(false);}else w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  host.setOnApplyWindowInsetsListener((v,i)->{int bottom=Build.VERSION.SDK_INT>=30?i.getInsets(WindowInsets.Type.navigationBars()).bottom:i.getSystemWindowInsetBottom();c.setPadding(dp(activity,16),dp(activity,12),dp(activity,16),dp(activity,24)+bottom);return Build.VERSION.SDK_INT>=30?WindowInsets.CONSUMED:i.consumeSystemWindowInsets();});host.requestApplyInsets();
 }
}
