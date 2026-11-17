import com.geminipocket.Transfer;
import java.io.*;
import java.security.MessageDigest;
public final class TransferTest {
 public static void main(String[] args)throws Exception{
  byte[] bytes="sample attachment".getBytes("UTF-8");ByteArrayOutputStream out=new ByteArrayOutputStream();
  Transfer.copy(new ByteArrayInputStream(bytes),out,100,()->false,null);
  if(!java.util.Arrays.equals(bytes,out.toByteArray()))throw new AssertionError("copy changed bytes");
  try{Transfer.copy(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),2,()->false,null);throw new AssertionError("limit not enforced");}catch(IOException expected){}
  try{Transfer.copy(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),100,()->true,null);throw new AssertionError("cancel ignored");}catch(IOException expected){}
  StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format("%02x",b&255));
  Transfer.download(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),bytes.length,hash.toString(),()->false,p->{});
  try{Transfer.download(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),bytes.length,"bad",()->false,p->{});throw new AssertionError("digest ignored");}catch(IOException expected){}
  try{Transfer.download(new ByteArrayInputStream(bytes),new ByteArrayOutputStream(),bytes.length+1,hash.toString(),()->false,p->{});throw new AssertionError("truncation ignored");}catch(IOException expected){}
  System.out.println("PASS: real stream copy, limits, cancellation, digest and truncation");
 }
}
