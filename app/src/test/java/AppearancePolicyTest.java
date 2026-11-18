import com.geminipocket.AppearancePolicy;

public final class AppearancePolicyTest {
 private static void check(boolean condition, String message) {
  if (!condition) throw new AssertionError(message);
 }
 public static void main(String[] args) {
  check(AppearancePolicy.accent("blue", false, null)==0xFF3459CF,"Existing light default");
  check(AppearancePolicy.accent("system", true, null)==0xFFAAC7FF,"Older Android uses dark blue fallback");
  check(AppearancePolicy.accent("system", false, 0xFF184626)==0xFF184626,"Use system accent when available");
  check(AppearancePolicy.accent("unknown", false, 0xFF184626)==0xFF3459CF,"Unknown preference does not select system color");
  for(String preset:new String[]{"teal","violet","amber"}) {
   int light=AppearancePolicy.accent(preset,false,null), dark=AppearancePolicy.accent(preset,true,null);
   check(light!=dark && light!=0xFF3459CF && dark!=0xFFAAC7FF,"Preset must have distinct light/dark colors: "+preset);
  }
  check(AppearancePolicy.drawWave(true,true),"Squiggly with system motion");
  check(!AppearancePolicy.drawWave(true,false),"Never freeze wave with system motion off");
  check(!AppearancePolicy.drawWave(false,true),"Straight preference wins with system motion on");
  check(AppearancePolicy.startupStyle(null,true)==AppearancePolicy.StartupStyle.MORPH,"Preserve default shape");
  check(AppearancePolicy.startupStyle(null,false)==AppearancePolicy.StartupStyle.HIDDEN,"Preserve legacy hidden choice");
  check(AppearancePolicy.startupStyle("arc",false)==AppearancePolicy.StartupStyle.ARC,"Explicit arc overrides legacy choice");
  check(AppearancePolicy.startupStyle("morph",false)==AppearancePolicy.StartupStyle.MORPH,"Explicit shape overrides legacy choice");
  check(AppearancePolicy.startupStyle("hidden",true)==AppearancePolicy.StartupStyle.HIDDEN,"Explicit hidden choice");
  check(AppearancePolicy.startupStyle("unknown",false)==AppearancePolicy.StartupStyle.HIDDEN,"Invalid choice falls back to legacy value");
  float lastTail=-Float.MAX_VALUE,lastHead=-Float.MAX_VALUE,minSweep=360,maxSweep=0;
  for(int i=0;i<=120;i++){
   float phase=i/120f;
   float tail=AppearancePolicy.arcStart(phase,true),sweep=AppearancePolicy.arcSweep(phase,true);
   check(sweep>0 && sweep<360,"Arc never closes");
   check(tail>lastTail && tail+sweep>lastHead,"Both ends chase forward");
   minSweep=Math.min(minSweep,sweep);maxSweep=Math.max(maxSweep,sweep);
   lastTail=tail;lastHead=tail+sweep;
  }
  check(maxSweep-minSweep>60,"Arc length varies visibly");
  check(Math.abs(AppearancePolicy.arcStart(1,true)-AppearancePolicy.arcStart(0,true)-360)<0.01f,"Arc wraps continuously");
  check(Math.abs(AppearancePolicy.arcSweep(1,true)-AppearancePolicy.arcSweep(0,true))<0.01f,"Arc length wraps continuously");
  check(AppearancePolicy.arcStart(.2f,false)==AppearancePolicy.arcStart(.8f,false),"Reduced-motion arc stays still");
  check(AppearancePolicy.arcSweep(.2f,false)==AppearancePolicy.arcSweep(.8f,false),"Reduced-motion length stays fixed");
  float previousStart=-1,previousEnd=-1;
  for(int i=0;i<=100;i++){
   float phase=i/100f;
   float start=AppearancePolicy.lineStart(phase,true),end=AppearancePolicy.lineEnd(phase,true);
   check(start>=0 && end<=1 && start<=end,"Moving line stays within its bounds");
   check(start>previousStart && end>previousEnd,"Both ends travel forward");
   previousStart=start;previousEnd=end;
  }
  check(AppearancePolicy.lineStart(0,true)==0 && AppearancePolicy.lineEnd(0,true)==0,"Segment starts collapsed at left");
  check(AppearancePolicy.lineStart(1,true)==1 && AppearancePolicy.lineEnd(1,true)==1,"Segment ends collapsed at right");
  check(AppearancePolicy.lineEnd(.5f,true)-AppearancePolicy.lineStart(.5f,true)>.25f,"Segment visibly stretches");
  for(float phase:new float[]{0,.3f,.7f,1})
   check(AppearancePolicy.lineStart(phase,false)==0 && AppearancePolicy.lineEnd(phase,false)==1,"Reduced-motion line stays full and still");
  System.out.println("PASS: appearance palette, motion precedence, startup migration, chasing arc and straight-bar motion");
 }
}
