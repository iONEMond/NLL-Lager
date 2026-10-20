package de.nll.lager;

/** Cancellation is cooperative; sealing establishes the final commit boundary. */
final class OperationControl implements WorkProgress {
    static final class Cancelled extends RuntimeException {Cancelled(){super("Vorgang abgebrochen.");}}
    private boolean cancelled,sealed;
    private String phase="Mailverarbeitung startet …";
    volatile boolean timedOut;
    private Runnable network;
    private Thread worker;
    @Override public void update(int done,int total,String message){synchronized(this){worker=Thread.currentThread();phase=message;}checkCancelled();}
    @Override public synchronized void checkCancelled(){if(cancelled)throw new Cancelled();}
    @Override public synchronized void completeCancellation(){checkCancelled();sealed=true;network=null;}
    @Override public synchronized void onCancel(Runnable action){checkCancelled();network=action;}
    synchronized String phase(){return phase;}
    synchronized void timeout(){if(!cancelled&&!sealed){timedOut=true;cancel();}}
    synchronized boolean isCancelled(){return cancelled;}
    synchronized boolean isSealed(){return sealed;}
    boolean cancel(){
        Runnable action;Thread target;
        synchronized(this){if(sealed || cancelled)return false;cancelled=true;action=network;network=null;target=worker;}
        if(target!=null)target.interrupt();
        if(action!=null)new Thread(action,"cancel-network").start();
        return true;
    }
}
