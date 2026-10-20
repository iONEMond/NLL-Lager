package de.nll.lager;

/** A known total reports completed records; total <= 0 denotes an unknown-length phase. */
interface WorkProgress {
    WorkProgress NONE=(done,total,message) -> {};
    void update(int done,int total,String message);
    default void checkCancelled() {}
    default void completeCancellation() {}
    default void onCancel(Runnable action) {}
}
