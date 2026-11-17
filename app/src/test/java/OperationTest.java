import com.geminipocket.Operation;
public final class OperationTest {
 public static void main(String[] args){
  Operation op=new Operation();if(!op.active())throw new AssertionError();op.cancel();if(op.active()||!op.finish()||op.finish())throw new AssertionError("cancelled completion must occur once");
  Operation next=new Operation();if(!next.active()||!next.finish()||next.active()||next.finish())throw new AssertionError("completion ownership");
  System.out.println("PASS: cancellation after transfer and exactly-once operation completion");
 }
}
