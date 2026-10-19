package de.nll.lager;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

final class MailStore {
    final Store store;
    MailStore(Store store) {this.store=store;}
    boolean exists(String id) {
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT 1 FROM mail_commands WHERE id=?",new String[]{id})) {return c.moveToFirst();}
    }
    void queue(String id,String reply,long cutoff,String result) {
        ContentValues v=new ContentValues();v.put("id",id);v.put("state","pending");v.put("reply",reply);v.put("cutoff",cutoff);v.put("result",result);v.put("created",System.currentTimeMillis());
        store.getWritableDatabase().insertOrThrow("mail_commands",null,v);
    }
    void archiveAfterSend(String id) {
        ContentValues v=new ContentValues();v.put("auto_archive",1);
        store.getWritableDatabase().update("mail_commands",v,"id=? AND state='pending' AND cutoff>0",new String[]{id});
    }
    void finishAutomaticArchives() {
        List<String> ids=new ArrayList<>();
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT id FROM mail_commands WHERE auto_archive=1 AND state='sent' AND cutoff>0",null)) {
            while(c.moveToNext()) ids.add(c.getString(0));
        }
        for(String id:ids) confirm(id);
    }
    void state(String id,String state) {
        ContentValues v=new ContentValues();v.put("state",state);
        store.getWritableDatabase().update("mail_commands",v,"id=?",new String[]{id});
    }
    /** Existing fields absent from older templates are preserved. No catalog deletion. */
    int[] upsert(List<String[]> rows,boolean people) {
        SQLiteDatabase db=store.getWritableDatabase();int added=0,updated=0;
        for(String[] row:rows) {
            String code=Rules.required(row[0],"Code");boolean exists=people ? store.person(code)!=null : store.material(code)!=null;
            if(!exists) {
                if(people) store.addPerson(code,row[1],row.length>2 ? row[2] : "");
                else store.addMaterial(code,row[1],row[2],row[3],row[4],row.length>5 ? row[5] : "");
                added++;
            } else {
                ContentValues v=new ContentValues();
                if(people) v.put("name",Rules.required(row[1],"Name"));
                else {
                    v.put("number",Rules.required(row[1],"Materialnummer"));v.put("description",Rules.required(row[2],"Beschreibung"));
                    if(!Arrays.asList("Material","Kältemittel").contains(row[3])) throw new IllegalArgumentException("Ungültige Materialart.");
                    v.put("type",row[3]);v.put("unit",Rules.required(row[4],"Einheit"));
                }
                int index=people ? 2 : 5;
                if(row.length>index) v.put("location",row[index].isEmpty() ? "" : Rules.required(row[index],"Lager"));
                db.update(people ? "people" : "materials",v,"code=?",new String[]{code});updated++;
            }
        }
        return new int[]{added,updated};
    }
    List<String[]> records(String gmail) {
        List<String[]> out=new ArrayList<>();
        try(Cursor c=store.getReadableDatabase().rawQuery("SELECT id,state,result,cutoff FROM mail_commands WHERE substr(id,1,?)=? ORDER BY created DESC LIMIT 30",new String[]{Integer.toString(gmail.length()+1),gmail+":"})) {
            while(c.moveToNext()) out.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3)});
        }
        return out;
    }
    void confirm(String id) {
        SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try(Cursor c=db.rawQuery("SELECT cutoff,state FROM mail_commands WHERE id=?",new String[]{id})) {
            if(!c.moveToFirst() || !"sent".equals(c.getString(1)) || c.getLong(0)<=0) throw new IllegalArgumentException("Kein versandter Export zum Abschließen.");
            store.archive(c.getLong(0),"mail:"+id);state(id,"archived");db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
    void recoverInterrupted() {
        ContentValues v=new ContentValues();v.put("state","uncertain");
        store.getWritableDatabase().update("mail_commands",v,"state='sending'",null);
    }
}
