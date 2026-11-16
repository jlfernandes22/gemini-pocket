package com.geminipocket;
/** Pure global zoom policy for exposed lenses; input ratios need not be sorted. */
public final class CameraZoom {
 private CameraZoom(){}
 public static int lens(float[] ratios,float global){int best=-1;float chosen=-1,smallest=Float.MAX_VALUE;int first=-1;for(int i=0;i<ratios.length;i++){float r=ratios[i];if(r<=0)continue;if(r<smallest){smallest=r;first=i;}if(r<=global && r>chosen){chosen=r;best=i;}}return best<0?first:best;}
 public static float local(float global,float optical){return Math.max(1,global/optical);}
}
