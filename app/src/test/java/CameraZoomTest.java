import com.geminipocket.CameraZoom;
public class CameraZoomTest {
 public static void main(String[] args){float[] r={.6f,1f,3.1f};
  if(CameraZoom.lens(r,3.09f)!=1 || CameraZoom.lens(r,3.1f)!=2 || CameraZoom.lens(r,4f)!=2 || CameraZoom.lens(r,.6f)!=0)throw new AssertionError("Lens thresholds");
  if(Math.abs(CameraZoom.local(6.2f,3.1f)-2f)>.001)throw new AssertionError("Global/local conversion");
  System.out.println("PASS: camera zoom thresholds and local ratio");
 }
}
