import com.geminipocket.UrlPolicy;
public class UrlPolicyTest {
 static int n;
 static void check(boolean actual, boolean expected, String label) { n++; if(actual != expected) throw new AssertionError(label); }
 public static void main(String[] args) {
  String[] allowed={"https://gemini.google.com/app","https://accounts.google.com/ServiceLogin","https://accounts.google.com:443/"};
  for(String u:allowed) check(UrlPolicy.navigation(u,true),true,u);
  String[] blocked={"http://gemini.google.com/","https://gemini.google.com.evil.org/","https://evilgemini.google.com/","https://google.com/","https://gemini.google.com@evil.org/","https://user@gemini.google.com/","https://gemini.google.com:444/","javascript:alert(1)","intent://foo","file:///etc/passwd","https://gemini.google.com\\@evil.org/","https://gemini.google.com./","not a url","https://127.0.0.1/"};
  for(String u:blocked) check(UrlPolicy.navigation(u,true),false,u);
  check(UrlPolicy.navigation("https://example.com/",false),true,"unrestricted https");
  check(UrlPolicy.navigation("http://example.com/",false),false,"unrestricted still https");
  check(UrlPolicy.resource("https://www.gstatic.com/a.js",true),true,"static");
  check(UrlPolicy.resource("https://lh3.googleusercontent.com/a",true),true,"media");
  check(UrlPolicy.resource("https://evilgoogleusercontent.com/a",true),false,"suffix");
  check(UrlPolicy.resource("https://evil.org/",true),false,"resource blocked");
  check(UrlPolicy.resource("https://content-push.googleapis.com/",true),true,"google API");
  check(UrlPolicy.navigation("https://www.gstatic.com/",true),false,"resource not navigation");
  check(UrlPolicy.mediaOrigin("https://gemini.google.com"),true,"camera origin");
  check(UrlPolicy.mediaOrigin("https://accounts.google.com"),false,"no auth camera");
  check(UrlPolicy.navigation(null,true),false,"null");
  check(UrlPolicy.navigation("https://www.google.com/sorry/index?continue=https%3A%2F%2Fgemini.google.com%2Fapp",true),true,"Google verification path");
  check(UrlPolicy.navigation("https://www.google.com/search?q=test",true),false,"Google search stays blocked");
  check(UrlPolicy.navigation("https://www.google.com/sorryevil/index",true),false,"Verification path boundary");
  check(UrlPolicy.resource("https://geminiweb-pa.clients6.google.com/",true),true,"Observed message API");
  check(UrlPolicy.navigation("https://geminiweb-pa.clients6.google.com/",true),false,"API not navigation");
  check(UrlPolicy.resource("https://speechs3proto2-pa.clients6.google.com/",true),true,"Observed speech service");
  check(UrlPolicy.navigation("https://speechs3proto2-pa.clients6.google.com/",true),false,"Speech is resource only");
  check(UrlPolicy.resource("https://speechs3proto2-pa.clients6.google.com.evil.org/",true),false,"Speech suffix attack");
  check(UrlPolicy.navigation("https://accounts.google.pt/accounts/SetSID?ssdc=1",true),true,"Observed Portuguese sign-in cookie handoff");
  check(UrlPolicy.resource("https://accounts.google.pt/accounts/SetSID?ssdc=1",true),true,"Handoff resource");
  check(UrlPolicy.navigation("https://accounts.google.pt/accounts/Other",true),false,"No broad regional account access");
  check(UrlPolicy.navigation("https://accounts.google.pt.evil.org/accounts/SetSID",true),false,"Handoff suffix attack");
  check(UrlPolicy.navigation("http://accounts.google.pt/accounts/SetSID",true),false,"Handoff HTTPS only");
  check(UrlPolicy.navigation("https://accounts.google.pt/accounts/SetSID/extra",true),false,"Exact handoff path");
  try {
   java.util.List<String> domains=java.nio.file.Files.readAllLines(java.nio.file.Paths.get("tests/fixtures/google-supported-domains.txt"));
   for(String entry:domains){String domain=entry.trim().substring(1);String regional="https://accounts."+domain;
    check(UrlPolicy.navigation(regional+"/accounts/SetSID?ssdc=1",true),true,"Regional handoff "+domain);
    check(UrlPolicy.resource(regional+"/accounts/SetSID",true),true,"Regional resource "+domain);
    if(!domain.equals("google.com"))check(UrlPolicy.navigation(regional+"/accounts/Other",true),false,"No regional general browsing "+domain);
    check(UrlPolicy.navigation(regional+".evil.org/accounts/SetSID",true),false,"Regional suffix attack "+domain);
   }
  }catch(java.io.IOException e){throw new AssertionError(e);}
  check(UrlPolicy.navigation("https://accounts.google.evil/accounts/SetSID",true),false,"Unpublished suffix");
  check(UrlPolicy.navigation("https://accounts.google.co.uk/accounts/%53etSID",true),false,"Encoded path not allowed");
  check(UrlPolicy.navigation("https://accounts.google.co.uk:444/accounts/SetSID",true),false,"Regional nonstandard port");
  check(UrlPolicy.navigation("https://user@accounts.google.co.uk/accounts/SetSID",true),false,"Regional userinfo");
  String sandbox="https://00000000000000000000000000000000000000000000000000-h994388388.scf.usercontent.goog";
  String shim=sandbox+"/gemini-code-immersive/shim.html?origin=https%3A%2F%2Fgemini.google.com";
  String preview="blob:"+sandbox+"/12345678-1234-1234-1234-123456789abc#page=1";
  for(boolean restricted:new boolean[]{true,false}){
   check(UrlPolicy.frameNavigation(shim,restricted),true,"Embedded document shim");
   check(UrlPolicy.frameNavigation(preview,restricted),true,"Same-sandbox preview blob");
   check(UrlPolicy.frameNavigation("blob:https://evil.org/12345678-1234-1234-1234-123456789abc",restricted),false,"Untrusted preview blob");
   check(UrlPolicy.frameNavigation("blob:"+sandbox+":444/12345678-1234-1234-1234-123456789abc",restricted),false,"Preview blob nonstandard port");
   check(UrlPolicy.frameNavigation(preview.replace(".goog/",".goog.evil.org/"),restricted),false,"Preview blob deceptive suffix");
  }
  check(UrlPolicy.resource(shim,true),true,"Restricted requests can fetch the shim");
  check(UrlPolicy.resource(preview,true),true,"Preview blob can load when intercepted");
  check(UrlPolicy.navigation(shim,true),false,"Shim is not allowed as top-level restricted page");
  check(UrlPolicy.navigation(preview,false),false,"Blob remains blocked as a top-level page");
  check(UrlPolicy.frameNavigation(shim.replace("gemini-code-immersive","test-product"),true),false,"Other sandbox product stays blocked");
  check(UrlPolicy.frameNavigation(shim.replace(".goog/",".goog.evil.org/"),true),false,"Deceptive preview host stays blocked");
  check(UrlPolicy.frameNavigation(shim.replace("shim.html","%73him.html"),true),false,"Encoded shim path stays blocked");
  check(UrlPolicy.frameNavigation(shim.replace("0000-h","0000.extra-h"),true),false,"Unexpected sandbox host structure stays blocked");
  check(UrlPolicy.frameNavigation("https://www.gstatic.com/a.js",true),false,"Resource allowlist does not become frame navigation allowlist");
  check(UrlPolicy.frameNavigation("https://example.com/",false),true,"Open HTTPS mode still permits HTTPS frames");
  System.out.println("PASS: "+n+" policy assertions");
 }
}
