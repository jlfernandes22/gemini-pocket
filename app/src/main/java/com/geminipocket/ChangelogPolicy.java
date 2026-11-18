package com.geminipocket;

import java.util.ArrayList;
import java.util.List;
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

 public static final class Section {
  public final String title;
  public final String body;

  private Section(String title, String body) {
   this.title = title;
   this.body = body;
  }
 }

 /** Each blank-line-separated block starts with a short heading. */
 public static List<Section> sections(String notes) {
  List<Section> sections = new ArrayList<>();
  if (notes == null) return sections;
  String normalized = notes.replace("\r\n", "\n").replace('\r', '\n').trim();
  if (normalized.isEmpty()) return sections;
  for (String block : normalized.split("\n(?:[ \\t]*\n)+")) {
   String[] parts = block.trim().split("\n", 2);
   sections.add(new Section(parts[0].trim(), parts.length == 2 ? parts[1].trim() : ""));
  }
  return sections;
 }

 static String notesVersion(String name) {
  if (name == null) return null;
  Matcher match = VERSION.matcher(name);
  return match.matches() ? match.group(1) : null;
 }
}
