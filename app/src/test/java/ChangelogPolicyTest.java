import com.geminipocket.ChangelogPolicy;

public final class ChangelogPolicyTest {
 private static void check(boolean result, String description) {
  if(!result)throw new AssertionError(description);
 }
 public static void main(String[] args) {
  String release="1.12.4-build.116";
  long code=300116;
  check(!ChangelogPolicy.shouldQueue(release,code,0,false,true),"Skip fresh install");
  check(ChangelogPolicy.shouldQueue(release,code,0,true,true),"Existing install with no version history");
  check(ChangelogPolicy.shouldQueue(release,code,300115,true,true),"Public upgrade");
  check(!ChangelogPolicy.shouldQueue(release,code,code,true,true),"Same build does not queue again");
  check(!ChangelogPolicy.shouldQueue(release,code,300117,true,true),"Downgrade does not queue");
  check(!ChangelogPolicy.shouldQueue(release,code,300115,true,false),"Respect disabled setting");
  check(!ChangelogPolicy.shouldQueue("1.12.4-appearance-test",code,300115,true,true),"Never queue test builds");
  check(!ChangelogPolicy.shouldQueue("1.12.4-build.116-debug",code,300115,true,true),"Only public version names match");
  check(ChangelogPolicy.shouldQueue("1.12.5-build.134",300134,300124,true,true),"Installed accent test build can show notes after public update");
  java.util.List<ChangelogPolicy.Section> sections=ChangelogPolicy.sections("  Buttons follow your accent\r\nFirst line.\r\nSecond line.\r\n \r\n  Easier release notes\r\nSeparate cards.\r\n\r\nOrphan heading\r\n");
  check(sections.size()==3,"Keep each change in its own section, including heading-only notes");
  check(sections.get(0).title.equals("Buttons follow your accent"),"Trim heading");
  check(sections.get(0).body.equals("First line.\nSecond line."),"Preserve multiline body and normalize line breaks");
  check(sections.get(1).body.equals("Separate cards."),"Keep bodies separate");
  check(sections.get(2).body.isEmpty(),"Heading-only note gets no duplicate body");
  check(ChangelogPolicy.sections(" \r\n\n").isEmpty(),"Blank release notes have no cards");
  System.out.println("PASS: changelog upgrade eligibility and section parsing");
 }
}
