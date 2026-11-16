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
  System.out.println("PASS: "+n+" policy assertions");
 }
}
