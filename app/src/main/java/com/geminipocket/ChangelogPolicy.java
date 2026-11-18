package com.geminipocket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Eligibility for automatic release notes; manual viewing is always available. */
public final class ChangelogPolicy {
 private static final Pattern VERSION = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+)(?:-[a-zA-Z0-9.-]+)?$");
 private static final Pattern PUBLIC_RELEASE = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+-build\\.[1-9][0-9]*$");
 private ChangelogPolicy() {}

 public static boolean shouldQueue(String name, long code, long previous, boolean upgraded, boolean enabled) {
  return enabled && upgraded && code > previous && name != null && PUBLIC_RELEASE.matcher(name).matches();
 }

 static String notesVersion(String name) {
  if (name == null) return null;
  Matcher match = VERSION.matcher(name);
  return match.matches() ? match.group(1) : null;
 }
}
