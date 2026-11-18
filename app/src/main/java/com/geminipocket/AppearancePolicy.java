package com.geminipocket;

/** Shared loader palette and motion decisions, independent of Android. */
public final class AppearancePolicy {
 private AppearancePolicy() {}

 public static int accent(String color, boolean dark, Integer systemAccent) {
  if ("system".equals(color) && systemAccent != null) return systemAccent;
  if ("teal".equals(color)) return dark ? 0xFF80DED0 : 0xFF006B61;
  if ("violet".equals(color)) return dark ? 0xFFD5BFFF : 0xFF6943A5;
  if ("amber".equals(color)) return dark ? 0xFFFFC678 : 0xFF855200;
  return dark ? 0xFFAAC7FF : 0xFF3459CF;
 }

 /** A tinted neutral surface keeps secondary actions distinct from solid accent fills. */
 public static int tonalSurface(int accent, boolean dark) {
  int base=dark?0xFF202124:0xFFF6F7FA;
  float tint=dark?.22f:.16f;
  return 0xFF000000 | (blendChannel(base,accent,16,tint)<<16)
      | (blendChannel(base,accent,8,tint)<<8) | blendChannel(base,accent,0,tint);
 }

 private static int blendChannel(int base,int accent,int shift,float tint) {
  int from=(base>>>shift)&255,to=(accent>>>shift)&255;
  return Math.round(from+(to-from)*tint);
 }

 /** Pick whichever of black and white has the higher WCAG contrast on the fill. */
 public static int onAccent(int fill) {
  double luminance=.2126*linearChannel(fill>>>16)+.7152*linearChannel(fill>>>8)+.0722*linearChannel(fill);
  return luminance>=Math.sqrt(.0525)-.05?0xFF000000:0xFFFFFFFF;
 }

 private static double linearChannel(int channel) {
  double value=(channel&255)/255d;
  return value<=.04045?value/12.92:Math.pow((value+.055)/1.055,2.4);
 }

 public enum StartupStyle { MORPH, ARC, HIDDEN }

 public static StartupStyle startupStyle(String choice, boolean legacyShape) {
  if ("arc".equals(choice)) return StartupStyle.ARC;
  if ("morph".equals(choice)) return StartupStyle.MORPH;
  if ("hidden".equals(choice)) return StartupStyle.HIDDEN;
  return legacyShape ? StartupStyle.MORPH : StartupStyle.HIDDEN;
 }

 public static float arcStart(float phase, boolean animate) {
  return animate ? -90f + 360f * phase - (float)(45 * Math.sin(phase * 2 * Math.PI)) : -90f;
 }

 public static float arcSweep(float phase, boolean animate) {
  return animate ? 145f + (float)(90 * Math.sin(phase * 2 * Math.PI)) : 145f;
 }

 // Both ends move forward, but the leading end starts faster and the tail catches up.
 public static float lineStart(float phase, boolean animate) {
  return animate ? phase * phase : 0f;
 }

 public static float lineEnd(float phase, boolean animate) {
  return animate ? phase * (2f - phase) : 1f;
 }

 public static boolean drawWave(boolean prefersWave, boolean systemMotion) {
  return prefersWave && systemMotion;
 }
}
