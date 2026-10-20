package de.nll.lager;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Visible progress for user-started operations; workers continue safely across activity destruction. */
final class OperationProgress implements WorkProgress {
    final Activity activity;
    final AlertDialog dialog;
    final ProgressBar bar;
    final TextView status;
    private volatile boolean closed;
    private int previous=-2;
    private String phase="";
    OperationProgress(Activity activity,String title,String message) {
        this.activity=activity;
        int padding=Math.round(24*activity.getResources().getDisplayMetrics().density);
        LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(padding,padding,padding,padding);
        status=new TextView(activity);status.setText(message);status.setTextSize(16);layout.addView(status);
        bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setIndeterminate(true);bar.setContentDescription("Fortschritt");layout.addView(bar,new LinearLayout.LayoutParams(-1,padding*2));
        dialog=new AlertDialog.Builder(activity).setTitle(title).setView(layout).setCancelable(false).create();dialog.show();
    }
    @Override public synchronized void update(int done,int total,String message) {
        if(closed) return;
        int percent=total<=0 ? -1 : (int)Math.min(99,Math.max(0,done*100L/total));
        if(percent==previous && message.equals(phase)) return;
        previous=percent;phase=message;
        activity.runOnUiThread(() -> {
            if(closed || activity.isDestroyed() || activity.isFinishing()) return;
            bar.setIndeterminate(total<=0);
            if(total>0) bar.setProgress(percent);
            status.setText(message+(total>0 ? "\n"+done+" / "+total : ""));
        });
    }
    void dismiss() {closed=true;dialog.dismiss();}
}
