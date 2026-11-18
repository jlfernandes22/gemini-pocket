package com.geminipocket;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

/** Resolves app preferences to a palette and style for the native loaders only. */
final class LoaderAppearance {
 static final String COLOR = "loaderColor";
 static final String WAVE = "loaderWave";
 static final String STARTUP = "loaderStartup";
 private static final String LEGACY_SHAPE = "loaderShape";
 final int accent;
 final boolean wave;
 final AppearancePolicy.StartupStyle startup;

 private LoaderAppearance(int accent, boolean wave, AppearancePolicy.StartupStyle startup) {
  this.accent = accent;
  this.wave = wave;
  this.startup = startup;
 }

 static LoaderAppearance read(Context context) {
  SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
  String color = prefs.getString(COLOR, "blue");
  boolean dark = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
      == Configuration.UI_MODE_NIGHT_YES;
  Integer systemAccent = null;
  if ("system".equals(color) && Build.VERSION.SDK_INT >= 31) {
   systemAccent = context.getColor(dark ? android.R.color.system_accent1_200 : android.R.color.system_accent1_700);
  }
  return new LoaderAppearance(AppearancePolicy.accent(color, dark, systemAccent),
      prefs.getBoolean(WAVE, true), startupStyle(prefs));
 }

 static AppearancePolicy.StartupStyle startupStyle(SharedPreferences prefs) {
  return AppearancePolicy.startupStyle(prefs.getString(STARTUP, null), prefs.getBoolean(LEGACY_SHAPE, true));
 }

 static void reset(SharedPreferences prefs) {
  prefs.edit().remove(COLOR).remove(WAVE).remove(STARTUP).remove(LEGACY_SHAPE).apply();
 }
}
