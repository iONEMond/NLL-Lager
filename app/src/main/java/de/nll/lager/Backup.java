package de.nll.lager;

import android.app.job.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import org.json.JSONArray;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** SAF documents survive uninstall; working data remains in the private database. */
final class Backup {
    static final int JOB=610;
    static final Object LOCK=new Object();
    static SharedPreferences prefs(Context c) {return c.getSharedPreferences("backup",0);}
    static void request(Context c) {
        if(prefs(c).getString("tree","").isEmpty()) return;
        try {((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(new JobInfo.Builder(JOB,new ComponentName(c,BackupJobService.class))
            .setMinimumLatency(3000).setOverrideDeadline(30000).setPersisted(true).build());}
        catch(RuntimeException e) {status(c,"Automatische Sicherung konnte nicht eingeplant werden.");}
    }
    static void status(Context c,String text) {prefs(c).edit().putString("status",text).apply();}
    static void configure(Context c,Uri tree,int flags) throws Exception {
        int rw=Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        if((flags&rw)!=rw) throw new IOException("Bitte einen les- und beschreibbaren Ordner auswählen.");
        c.getContentResolver().takePersistableUriPermission(tree,flags&rw);
        if(!DocumentsContract.isTreeUri(tree)) throw new IOException("Bitte einen Ordner auswählen.");
        prefs(c).edit().putString("tree",tree.toString()).remove("hash").remove("last").remove("latest").remove("revision").remove("databaseId").putString("status","Ordner gewählt · erste Sicherung steht aus").commit();request(c);
    }
    static JSONArray legacyLocations(Context c) throws Exception {return new JSONArray(c.getSharedPreferences("admin-settings",0).getString("locations","[]"));}
    static BackupData snapshot(Context c) throws Exception {try(Store store=new Store(c)) {return BackupData.snapshot(store,legacyLocations(c));}}
    static boolean save(Context c,boolean force) throws Exception {
        synchronized(LOCK) {
            String chosen=prefs(c).getString("tree","");if(chosen.isEmpty()) throw new IOException("Bitte zuerst einen Sicherungsordner auswählen.");
            BackupData backup=snapshot(c);
            if(!force && backup.hash.equals(prefs(c).getString("hash",""))) {
                String latest=prefs(c).getString("latest","");
                if(!latest.isEmpty()) try(Cursor cursor=c.getContentResolver().query(Uri.parse(latest),new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},android.os.Bundle.EMPTY,null)) {
                    if(cursor!=null && cursor.moveToFirst()) {recordRevision(c,backup);return false;}
                } catch(RuntimeException ignored) { /* A removed file must be recreated. */ }
            }
            Uri tree=Uri.parse(chosen),parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
            String name="NLL-Sicherung-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+"-"+UUID.randomUUID()+".nllbackup";
            Uri document=DocumentsContract.createDocument(c.getContentResolver(),parent,"application/json",name+".partial");
            if(document==null) throw new IOException("Sicherungsdatei konnte nicht angelegt werden.");
            try(OutputStream out=c.getContentResolver().openOutputStream(document,"w")) {
                if(out==null) throw new IOException("Sicherungsdatei kann nicht geschrieben werden.");out.write(backup.bytes());out.flush();
            }
            try(InputStream in=c.getContentResolver().openInputStream(document)) {
                if(!backup.hash.equals(BackupData.read(in).hash)) throw new IOException("Sicherung konnte nicht bestätigt werden.");
            }
            Uri completed=DocumentsContract.renameDocument(c.getContentResolver(),document,name);
            if(completed==null) throw new IOException("Sicherung geschrieben, aber nicht abgeschlossen. Ältere Stände bleiben erhalten.");
            prefs(c).edit() .putString("latest",completed.toString()).putString("hash",backup.hash).putLong("revision",backup.revision).putString("databaseId",backup.databaseId).putLong("last",System.currentTimeMillis()).putString("status","Sicherung erfolgreich · "+backup.summary()).commit();
            try {prune(c,tree,completed);} catch(Exception e) {status(c,"Sicherung erfolgreich · ältere Stände konnten nicht bereinigt werden.");}
            return true;
        }
    }
    private static void recordRevision(Context c,BackupData backup) {prefs(c).edit().putLong("revision",backup.revision).putString("databaseId",backup.databaseId).commit();}
    static String statusLine(Context c,Store store) {
        SharedPreferences p=prefs(c);long last=p.getLong("last",0);
        if(p.getString("tree","").isEmpty())return "Sicherung: nicht eingerichtet";
        if(last==0)return "Sicherung: erster geprüfter Stand steht aus";
        String time=new SimpleDateFormat("dd.MM. HH:mm",Locale.GERMANY).format(new Date(last));
        boolean unchanged=store.databaseId().equals(p.getString("databaseId","")) && store.revision()==p.getLong("revision",-1);
        return (unchanged ? "Keine Änderungen seit letzter Sicherung" : "Nicht gesicherte Änderungen vorhanden")+" · "+time;
    }
    static final class Document {final String name;final Uri uri;Document(String name,Uri uri) {this.name=name;this.uri=uri;}}
    static List<Document> documents(Context c,Uri tree) throws Exception {
        List<Document> result=new ArrayList<>();
        Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));
        try(Cursor cursor=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME},android.os.Bundle.EMPTY,null)) {
            if(cursor==null) throw new IOException("Sicherungsordner ist nicht erreichbar.");
            while(cursor.moveToNext()) {
                String name=cursor.getString(1);
                if(name!=null && name.matches("NLL-Sicherung-[0-9]{8}-[0-9]{6}-[0-9a-f-]{36}\\.nllbackup")) result.add(new Document(name,DocumentsContract.buildDocumentUriUsingTree(tree,cursor.getString(0))));
            }
        }
        result.sort((a,b) -> b.name.compareTo(a.name));return result;
    }
    static void prune(Context c,Uri tree,Uri newest) throws Exception {
        List<Document> docs=documents(c,tree);
        docs.sort((a,b) -> a.uri.equals(newest) ? -1 : b.uri.equals(newest) ? 1 : b.name.compareTo(a.name));
        for(int i=7;i<docs.size();i++) {
            // Only a valid NLL backup is eligible for retention cleanup; never touch other files.
            try(InputStream in=c.getContentResolver().openInputStream(docs.get(i).uri)) {BackupData.read(in);}
            DocumentsContract.deleteDocument(c.getContentResolver(),docs.get(i).uri);
        }
    }
    static void restore(Context c,BackupData selected) throws Exception {
        synchronized(LOCK) {
            if(!MailSync.BUSY.compareAndSet(false,true)) throw new IOException("Mailverarbeitung läuft. Bitte später wiederherstellen.");
            try {
                // Require a verified rescue copy before replacing any current business data.
                save(c,true);
                if(!MailConfig.prefs(c).edit().putBoolean("connected",false).putLong("since",System.currentTimeMillis()).commit()) throw new IOException("Mailverarbeitung konnte nicht getrennt werden.");MailJobService.schedule(c);
                try(Store store=new Store(c)) {selected.restore(store);}
                prefs(c).edit().remove("hash").putString("status","Wiederhergestellt · Gmail bei Bedarf erneut verbinden").commit();request(c);
            } finally {MailSync.BUSY.set(false);}
        }
    }
}
