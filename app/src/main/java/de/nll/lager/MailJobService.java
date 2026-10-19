package de.nll.lager;

import android.app.job.*;
import android.content.ComponentName;
import android.content.Context;

/** Android may defer periodic work; there is no foreground service or continuous polling. */
public final class MailJobService extends JobService {
    private static final int JOB=601;
    private Thread worker;
    static void schedule(Context c) {
        JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        MailConfig config=new MailConfig(c);
        if(!config.ready() || !config.automatic) {scheduler.cancel(JOB);return;}
        scheduler.schedule(new JobInfo.Builder(JOB,new ComponentName(c,MailJobService.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());
    }
    @Override public boolean onStartJob(JobParameters params) {
        if(!new MailConfig(this).ready()) return false;
        worker=new Thread(() -> {MailSync.run(getApplicationContext());if(!Thread.currentThread().isInterrupted()) jobFinished(params,false);},"mail-job");worker.start();return true;
    }
    @Override public boolean onStopJob(JobParameters params) {if(worker!=null) worker.interrupt();return true;}
}
