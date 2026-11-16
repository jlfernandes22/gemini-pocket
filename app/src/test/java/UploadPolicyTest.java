import com.geminipocket.UploadPolicy;
import com.geminipocket.UrlPolicy;
public class UploadPolicyTest {
 static int n;
 static void check(boolean ok,String label){n++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args){
  check(UploadPolicy.route(new String[]{"image/*"},false)==UploadPolicy.PHOTOS,"Photos directly opens picker");
  check(UploadPolicy.route(new String[]{"image/jpeg"},true)==UploadPolicy.CAMERA,"Capture directly opens camera");
  check(UploadPolicy.route(new String[]{"application/pdf"},false)==UploadPolicy.FILES,"Documents remain documents");
  check(UploadPolicy.route(new String[]{"*/*"},false)==UploadPolicy.PHOTOS,"Gemini generic Photos request uses system photo picker");
  check(UploadPolicy.route(new String[]{"image/*","video/*"},false)==UploadPolicy.PHOTOS,"Visual media uses system picker");
  check(UploadPolicy.route(new String[]{"image/png","application/pdf"},false)==UploadPolicy.FILES,"Mixed request preserves files");
  check(UploadPolicy.validName("capture_12345.jpg"),"camera provider name");
  check(UploadPolicy.validName("upload_12345.png"),"copied upload name");
  check(!UploadPolicy.validName("../capture_12345.jpg"),"reject traversal");
  check(!UploadPolicy.validName("settings.xml"),"reject other private files");
  check(UrlPolicy.resource("https://upload.clients6.google.com/upload/",true),"Gemini upload endpoint");
  check(!UrlPolicy.navigation("https://upload.clients6.google.com/",true),"Upload endpoint is not navigation");
  check(!UrlPolicy.resource("https://upload.clients6.google.com.evil.org/",true),"Upload suffix attack");
  check(UrlPolicy.resource("https://push.clients6.google.com/upload/",true),"Observed attachment transport host");
  check(!UrlPolicy.navigation("https://push.clients6.google.com/",true),"Transport remains resource only");
  check(UrlPolicy.resource("https://waa-pa.clients6.google.com/",true),"Observed website attestation service");
  check(UrlPolicy.resource("https://signaler-pa.clients6.google.com/",true),"Observed website signaling service");
  check(UploadPolicy.route(new String[]{"*/*"},false,"files")==UploadPolicy.FILES,"Files action overrides generic MIME");
  check(UploadPolicy.route(new String[]{"*/*"},false,"photos")==UploadPolicy.PHOTOS,"Photos action preserves system picker");
  System.out.println("PASS: "+n+" upload policy assertions");
 }
}
