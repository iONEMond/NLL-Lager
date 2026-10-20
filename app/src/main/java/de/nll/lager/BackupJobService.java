package de.nll.lager;

import android.app.job.*;

public final class BackupJobService extends JobService {
    private volatile boolean stopped;
    @Override public boolean onStartJob(JobParameters params) {
        stopped=false;
        new Thread(() -> {
            try {Backup.save(getApplicationContext(),false);}
            catch(Exception e) {Backup.status(this,"Sicherung fehlgeschlagen · Ordnerzugriff und Speicherplatz prüfen. Ältere Stände bleiben erhalten.");}
            if(!stopped) jobFinished(params,false);
        },"database-backup").start();return true;
    }
    @Override public boolean onStopJob(JobParameters params) {stopped=true;return true;}
}
