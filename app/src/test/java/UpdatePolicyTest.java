import com.geminipocket.UpdatePolicy;
public final class UpdatePolicyTest {
 private static int count;
 private static void check(boolean ok){count++;if(!ok)throw new AssertionError("Update assertion "+count);}
 public static void main(String[] args){
  check(UpdatePolicy.downloadUrl("https://github.com/jlfernandes22/gemini-pocket/releases/download/build-8/app.apk"));
  check(UpdatePolicy.downloadUrl("https://release-assets.githubusercontent.com/asset?sig=example"));
  for(String u:new String[]{"http://github.com/jlfernandes22/gemini-pocket/releases/download/a.apk","https://github.com/other/repo/releases/download/app.apk","https://github.com.evil.org/jlfernandes22/gemini-pocket/releases/download/app.apk","https://user@release-assets.githubusercontent.com/a.apk","https://release-assets.githubusercontent.com:444/a.apk","file:///tmp/a.apk","https://github.com/jlfernandes22/gemini-pocket/releases/download/../secret","https://github.com/jlfernandes22/gemini-pocket/releases/download/%2e%2e/secret"})check(!UpdatePolicy.downloadUrl(u));
  check(UpdatePolicy.publishedCode("build-8")==300008);
  check(UpdatePolicy.publishedCode("dev-8")==-1);
  check(UpdatePolicy.publishedCode("build-999999999999999999999")==-1);
  check(UpdatePolicy.publishedCode(null)==-1);
  System.out.println("PASS: "+count+" updater policy assertions");
 }
}
