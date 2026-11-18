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
