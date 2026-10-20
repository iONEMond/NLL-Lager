package de.nll.lager;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;

/** Versioned logical snapshot: business data only, never credentials or executable SQL. */
final class BackupData {
    static final int MAX_BYTES=64*1024*1024, MAX_ROWS=100000;
    static final String[][] COLUMNS={
        {"code","number","description","type","unit","location"},
        {"code","name","location"},
        {"id","timestamp","code","number","description","type","unit","from_location","to_location","quantity","person_code","person_name","archived_batch","movement_kind"}
    };
    static final String[] TABLES={"materials","people","movements"};
    final JSONObject data;
    final String hash;
    private BackupData(JSONObject data) throws Exception {this.data=data;this.hash=digest(data.toString().getBytes(StandardCharsets.UTF_8));validate();}
    static BackupData snapshot(Store store,JSONArray legacyLocations) throws Exception {
        SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try {
            JSONObject data=new JSONObject();
            for(int t=0;t<TABLES.length;t++) {
                JSONArray rows=new JSONArray();
                try(Cursor c=db.query(TABLES[t],COLUMNS[t],null,null,null,null,t==2 ? "id" : "code")) {
                    while(c.moveToNext()) {
                        if(rows.length()>=MAX_ROWS) throw new IOException("Zu viele Datensätze für eine Sicherung.");
                        JSONArray row=new JSONArray();
                        for(int col=0;col<COLUMNS[t].length;col++) row.put(c.isNull(col) ? JSONObject.NULL : t==2 && col==0 ? c.getLong(col) : c.getString(col));
                        rows.put(row);
                    }
                }
                data.put(TABLES[t],rows);
            }
            JSONArray locations=legacyLocations;
            try(Cursor c=db.rawQuery("SELECT value FROM app_settings WHERE key='locations'",null)) {if(c.moveToFirst()) locations=new JSONArray(c.getString(0));}
            data.put("locations",locations);db.setTransactionSuccessful();return new BackupData(data);
        } finally {db.endTransaction();}
    }
    byte[] bytes() throws Exception {
        byte[] bytes=new JSONObject().put("format","NLL-Lager-Sicherung").put("version",1).put("created",OffsetDateTime.now().toString()).put("sha256",hash).put("data",data).toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES) throw new IOException("Sicherung überschreitet 64 MB.");return bytes;
    }
    static BackupData read(InputStream in) throws Exception {
        if(in==null) throw new IOException("Sicherung kann nicht geöffnet werden.");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1) {if(out.size()+n>MAX_BYTES) throw new IOException("Sicherung überschreitet 64 MB.");out.write(buffer,0,n);}
        JSONObject envelope=new JSONObject(out.toString("UTF-8"));
        if(!"NLL-Lager-Sicherung".equals(envelope.getString("format")) || envelope.getInt("version")!=1) throw new IOException("Unbekanntes Sicherungsformat.");
        BackupData backup=new BackupData(envelope.getJSONObject("data"));
        if(!backup.hash.equals(envelope.getString("sha256"))) throw new IOException("Prüfsumme stimmt nicht. Sicherung beschädigt.");return backup;
    }
    static String digest(byte[] bytes) throws Exception {
        StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes)) out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
    String summary() throws JSONException {return data.getJSONArray("materials").length()+" Materialien, "+data.getJSONArray("people").length()+" Personen, "+data.getJSONArray("movements").length()+" Bewegungen (offen und archiviert), "+data.getJSONArray("locations").length()+" Lager";}
    private void validate() throws Exception {
        for(int t=0;t<TABLES.length;t++) {
            JSONArray rows=data.getJSONArray(TABLES[t]);if(rows.length()>MAX_ROWS) throw new IOException("Zu viele Datensätze.");Set<String> keys=new HashSet<>();
            for(int r=0;r<rows.length();r++) {
                JSONArray row=rows.getJSONArray(r);if(row.length()!=COLUMNS[t].length) throw new IOException("Ungültige Datensatzstruktur.");
                for(int col=0;col<row.length();col++) {
                    if(t==2 && col==12 && row.isNull(col)) continue;
                    Object value=row.get(col);
                    if(t==2 && col==0) {if(!(value instanceof Number) || row.getLong(0)<=0 || !value.toString().equals(Long.toString(row.getLong(0)))) throw new IOException("Ungültige Bewegungs-ID.");}
                    else if(!(value instanceof String) || ((String)value).length()>(t==2 && col==12 ? 1000 : 200) || ((String)value).indexOf('\0')>=0) throw new IOException("Ungültiges Datenfeld.");
                }
                String key=row.get(0).toString();if(key.isEmpty() || !keys.add(key)) throw new IOException("Leerer oder doppelter Schlüssel.");
                if(t==0 && (!Arrays.asList("Material","Kältemittel").contains(row.getString(3)) || row.getString(1).isEmpty() || row.getString(2).isEmpty() || row.getString(4).isEmpty())) throw new IOException("Ungültiger Materialeintrag.");
                if(t==1 && row.getString(1).isEmpty()) throw new IOException("Ungültiger Personeneintrag.");
                if(t==2) {
                    OffsetDateTime.parse(row.getString(1));Rules.quantity(row.getString(9));Rules.locations(row.getString(7),row.getString(8));
                    if(!Arrays.asList("","Entnahme","Rückgabe","Umlagerung").contains(row.getString(13))) throw new IOException("Ungültige Bewegungsart.");
                }
            }
        }
        JSONArray locations=data.getJSONArray("locations");if(locations.length()>10000) throw new IOException("Zu viele Lager.");Set<String> seen=new HashSet<>();
        for(int i=0;i<locations.length();i++) {String value=Rules.required(locations.getString(i),"Lager");if(!seen.add(value.toLowerCase(Locale.ROOT))) throw new IOException("Doppeltes Lager.");}
    }
    void restore(Store store) throws Exception {
        validate();SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try {
            for(int t=0;t<TABLES.length;t++) {
                db.delete(TABLES[t],null,null);JSONArray rows=data.getJSONArray(TABLES[t]);
                for(int r=0;r<rows.length();r++) {
                    JSONArray row=rows.getJSONArray(r);ContentValues values=new ContentValues();
                    for(int col=0;col<row.length();col++) {
                        if(row.isNull(col)) values.putNull(COLUMNS[t][col]);
                        else if(t==2 && col==0) values.put(COLUMNS[t][col],row.getLong(col));
                        else values.put(COLUMNS[t][col],row.getString(col));
                    }
                    db.insertOrThrow(TABLES[t],null,values);
                }
            }
            // Never replay queued mail or old export cutoffs after restoring older business data.
            db.delete("mail_commands",null,null);
            ContentValues settings=new ContentValues();settings.put("key","locations");settings.put("value",data.getJSONArray("locations").toString());
            db.insertWithOnConflict("app_settings",null,settings,SQLiteDatabase.CONFLICT_REPLACE);db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }
}
