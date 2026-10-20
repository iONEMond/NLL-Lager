package de.nll.lager;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import java.io.*;
import java.util.*;
import org.json.*;

/** A durable report snapshot, never treated as proof that the receiving mail app sent it. */
final class SharedReport {
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("shared-report",0);}
    static String pending(Context c){return prefs(c).getString("pending","");}
    static String fingerprint(Store.Movement m)throws Exception {JSONArray a=new JSONArray();for(int i=0;i<m.values.length;i++)if(i!=12)a.put(m.values[i]==null?JSONObject.NULL:m.values[i]);return BackupData.digest(a.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static String create(Context context,Store store,boolean archived,WorkProgress progress)throws Exception {
        if(!pending(context).isEmpty())throw new IOException("Zuerst den vorherigen geteilten Bericht abschließen oder offen lassen.");
        File dir=new File(context.getFilesDir(),"shared-reports");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Berichtordner nicht verfügbar.");
        File[] old=dir.listFiles();if(old!=null)for(File f:old)if(f.getName().matches("NLL-Bericht-[0-9a-f-]{36}\\.xlsx") && f.lastModified()<System.currentTimeMillis()-7L*24*60*60*1000)f.delete();
        String name="NLL-Bericht-"+UUID.randomUUID()+".xlsx";File file=new File(dir,name);boolean success=false;
        SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try{
            long cutoff=archived?store.archiveCutoff():store.cutoff();JSONArray entries=new JSONArray();
            if(!archived)try(Cursor c=db.rawQuery("SELECT id FROM movements WHERE id<=? AND archived_batch IS NULL ORDER BY id",new String[]{Long.toString(cutoff)})){while(c.moveToNext()){progress.checkCancelled();long id=c.getLong(0);entries.put(new JSONObject().put("id",id).put("hash",fingerprint(store.movement(id))));}}
            try(FileOutputStream out=new FileOutputStream(file)){store.writeXlsx(out,cutoff,archived,progress);out.getFD().sync();}
            progress.completeCancellation();
            if(!archived && !prefs(context).edit().putString("pending",new JSONObject().put("file",name).put("database",store.databaseId()).put("entries",entries).toString()).commit())throw new IOException("Berichtstatus konnte nicht gespeichert werden.");
            db.setTransactionSuccessful();success=true;return name;
        }finally{db.endTransaction();if(!success)file.delete();}
    }
    static Intent intent(Context context,String name,String recipient){
        Uri uri=new Uri.Builder().scheme("content").authority(context.getPackageName()+".reports").appendPath(name).build();
        Intent send=new Intent(Intent.ACTION_SEND).setType(ReportFileProvider.MIME).putExtra(Intent.EXTRA_STREAM,uri).putExtra(Intent.EXTRA_EMAIL,new String[]{recipient}).putExtra(Intent.EXTRA_SUBJECT,"NLL Lager – Bewegungsbericht").putExtra(Intent.EXTRA_TEXT,"Anbei der Bewegungsbericht als Excel-Tabelle.").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newUri(context.getContentResolver(),"Excel-Bericht",uri));return send;
    }
    static int archive(Context context,Store store)throws Exception {
        JSONObject report=new JSONObject(pending(context));SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try{
            if(!store.databaseId().equals(report.getString("database")))throw new IOException("Datenbank wurde ersetzt. Keine Archivierung.");
            JSONArray entries=report.getJSONArray("entries");
            for(int i=0;i<entries.length();i++){JSONObject e=entries.getJSONObject(i);Store.Movement m=store.movement(e.getLong("id"));if(m==null||!fingerprint(m).equals(e.getString("hash")))throw new IOException("Berichtdaten haben sich verändert. Keine Archivierung.");}
            int count=0;ContentValues v=new ContentValues();v.put("archived_batch","shared:"+report.getString("file"));
            for(int i=0;i<entries.length();i++)count+=db.update("movements",v,"id=? AND archived_batch IS NULL",new String[]{Long.toString(entries.getJSONObject(i).getLong("id"))});
            db.setTransactionSuccessful();return count;
        }finally{db.endTransaction();Backup.request(context);}
    }
    static void clear(Context c){prefs(c).edit().remove("pending").commit();}
}
