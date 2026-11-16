import com.geminipocket.TempPhotos;
import java.io.*;
public class TempPhotoTest {
 public static void main(String[] args)throws Exception{
  File dir=new File("build-tests","gp-test-"+System.nanoTime());dir.mkdirs();long now=System.currentTimeMillis();
  File old=new File(dir,"capture_123.jpg"),fresh=new File(dir,"upload_456.png"),other=new File(dir,"web-cache");
  old.createNewFile();fresh.createNewFile();other.createNewFile();old.setLastModified(now-TempPhotos.MAX_AGE-1000);
  TempPhotos.clean(dir,now);
  if(old.exists()||!fresh.exists()||!other.exists())throw new AssertionError("Cleanup must remove only stale attachment files");
  fresh.delete();other.delete();dir.delete();System.out.println("PASS: safe temporary-photo cleanup");
 }
}
