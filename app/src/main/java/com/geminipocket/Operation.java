package com.geminipocket;
/** Thread-safe cancellation and one-shot completion gate. */
public final class Operation {
 private boolean cancelled,completed;
 public synchronized void cancel(){cancelled=true;}
 public synchronized boolean active(){return !cancelled&&!completed;}
 public synchronized boolean finish(){if(completed)return false;completed=true;return true;}
}
