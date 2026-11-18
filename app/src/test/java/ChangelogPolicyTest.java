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
  System.out.println("PASS: changelog eligibility for public upgrades, not fresh installs, retries, downgrades or tests");
 }
}
