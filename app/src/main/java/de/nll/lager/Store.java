package de.nll.lager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class Store extends SQLiteOpenHelper {
    public static final class Material {
        public final String code, number, description, type, unit, location;
        Material(Cursor c) {
            code = c.getString(0); number = c.getString(1); description = c.getString(2);
            type = c.getString(3); unit = c.getString(4); location=c.getString(5);
        }
    }
    public static final class Person {
        public final String code, name, location;
        Person(String code, String name, String location) { this.code = code; this.name = name; this.location=location; }
    }
    private final Context app;
    public Store(Context context) { super(context, "nll-lager.db", null, 7);app=context.getApplicationContext(); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE materials (code TEXT PRIMARY KEY, number TEXT NOT NULL, description TEXT NOT NULL, type TEXT NOT NULL, unit TEXT NOT NULL, location TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE people (code TEXT PRIMARY KEY, name TEXT NOT NULL, location TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE movements (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT NOT NULL, code TEXT NOT NULL, number TEXT NOT NULL, description TEXT NOT NULL, type TEXT NOT NULL, unit TEXT NOT NULL, from_location TEXT NOT NULL, to_location TEXT NOT NULL, quantity TEXT NOT NULL, person_code TEXT NOT NULL, person_name TEXT NOT NULL, archived_batch TEXT)");
        db.execSQL("CREATE INDEX movements_pending ON movements(archived_batch, id)");
        createMailTables(db);
        db.execSQL("CREATE TABLE app_settings (key TEXT PRIMARY KEY,value TEXT NOT NULL)");
        db.execSQL("ALTER TABLE movements ADD COLUMN movement_kind TEXT NOT NULL DEFAULT ''");
        upgradeSeven(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<2) {
            db.execSQL("ALTER TABLE materials ADD COLUMN location TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE people ADD COLUMN location TEXT NOT NULL DEFAULT ''");
        }
        if(oldVersion<3) createMailTables(db);
        else if(oldVersion<4) db.execSQL("ALTER TABLE mail_commands ADD COLUMN auto_archive INTEGER NOT NULL DEFAULT 0");
        if(oldVersion<6) db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY,value TEXT NOT NULL)");
        if(oldVersion<5) db.execSQL("ALTER TABLE movements ADD COLUMN movement_kind TEXT NOT NULL DEFAULT ''");
        if(oldVersion<7) upgradeSeven(db);
    }
    private static void upgradeSeven(SQLiteDatabase db) {
        java.util.Set<String> columns=new java.util.HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info(movements)",null)){while(c.moveToNext())columns.add(c.getString(1));}
        if(!columns.contains("reversal_of")) db.execSQL("ALTER TABLE movements ADD COLUMN reversal_of INTEGER");
        if(!columns.contains("cancellation_reason")) db.execSQL("ALTER TABLE movements ADD COLUMN cancellation_reason TEXT NOT NULL DEFAULT ''");
        if(!columns.contains("cancelled_by")) db.execSQL("ALTER TABLE movements ADD COLUMN cancelled_by TEXT NOT NULL DEFAULT ''");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS movements_one_reversal ON movements(reversal_of) WHERE reversal_of IS NOT NULL");
        ContentValues value=new ContentValues();value.put("key","database_id");value.put("value",java.util.UUID.randomUUID().toString());db.insertWithOnConflict("app_settings",null,value,SQLiteDatabase.CONFLICT_IGNORE);
        db.execSQL("INSERT OR IGNORE INTO app_settings(key,value) VALUES('data_revision','0')");
        for(String table:new String[]{"materials","people","movements","app_settings"}) for(String operation:new String[]{"INSERT","UPDATE","DELETE"}) {
            String condition="app_settings".equals(table) ? " WHEN "+("DELETE".equals(operation) ? "OLD" : "NEW")+".key='locations'" : "";
            db.execSQL("CREATE TRIGGER IF NOT EXISTS revision_"+table+"_"+operation+" AFTER "+operation+" ON "+table+condition+" BEGIN UPDATE app_settings SET value=CAST(value AS INTEGER)+1 WHERE key='data_revision'; END");
        }
    }
    String databaseId() {try(Cursor c=getReadableDatabase().rawQuery("SELECT value FROM app_settings WHERE key='database_id'",null)){return c.moveToFirst() ? c.getString(0) : "";}}
    long revision() {try(Cursor c=getReadableDatabase().rawQuery("SELECT value FROM app_settings WHERE key='data_revision'",null)){return c.moveToFirst() ? c.getLong(0) : 0;}}
    static final class Movement {
        final String[] values;final long id;final boolean reversed;
        Movement(Cursor c) {values=new String[17];for(int i=0;i<17;i++) values[i]=c.isNull(i) ? null : c.getString(i);id=c.getLong(0);reversed=c.getInt(17)!=0;}
        boolean isReversal(){return values[14]!=null;}
        String label(){return "#"+id+" · "+values[3]+" · "+values[4]+"\n"+values[9]+" "+values[6]+"  |  "+values[7]+" → "+values[8]+"\n"+values[11]+" · "+values[1]+" · "+values[13]+(reversed ? "\nStorniert – Gegenbuchung vorhanden" : "")+(isReversal() ? "\nBezug: #"+values[14]+" · Grund: "+values[15]+" · Ausgeführt von: "+values[16] : "");}
    }
    Movement movement(long id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT m.*,EXISTS(SELECT 1 FROM movements r WHERE r.reversal_of=m.id) FROM movements m WHERE m.id=?",new String[]{Long.toString(id)})){return c.moveToFirst() ? new Movement(c) : null;}
    }
    List<Movement> movements(boolean archived) {
        List<Movement> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT m.*,EXISTS(SELECT 1 FROM movements r WHERE r.reversal_of=m.id) FROM movements m WHERE archived_batch IS "+(archived ? "NOT NULL" : "NULL")+" ORDER BY m.id DESC LIMIT 100",null)){while(c.moveToNext())result.add(new Movement(c));}return result;
    }
    long cancelMovement(long id,String reason,String actor) {
        reason=Rules.required(reason,"Stornogrund");actor=Rules.required(actor,"Ausgeführt von");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            Movement original=movement(id);
            if(original==null || original.isReversal() || original.reversed) throw new IllegalArgumentException("Diese Buchung kann nicht erneut storniert werden.");
            String[] row=original.values;ContentValues v=new ContentValues();
            v.put("timestamp",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX",Locale.ROOT).format(new Date()));
            for(int i:new int[]{2,3,5,6,9,10,11}) v.put(BackupData.COLUMNS[2][i],row[i]);
            String description="Storno #"+id+" ("+reason+"): "+row[4];v.put("description",description.substring(0,Math.min(200,description.length())));
            v.put("from_location",row[8]);v.put("to_location",row[7]);v.put("movement_kind","Storno");v.put("reversal_of",id);v.put("cancellation_reason",reason);v.put("cancelled_by",actor);
            long reversal=db.insertOrThrow("movements",null,v);db.setTransactionSuccessful();Backup.request(app);return reversal;
        } finally {db.endTransaction();}
    }
    static final class DuplicateMovement extends IllegalArgumentException {DuplicateMovement(){super("Mögliche Doppelbuchung innerhalb von 10 Sekunden.");}}
    boolean recentDuplicate(Material material,String from,String to,String amount,Person person,String kind,long now) {
        if(material==null || person==null) return false;
        try(Cursor c=getReadableDatabase().rawQuery("SELECT code,from_location,to_location,quantity,person_code,movement_kind,timestamp FROM movements m WHERE reversal_of IS NULL AND NOT EXISTS(SELECT 1 FROM movements r WHERE r.reversal_of=m.id) ORDER BY id DESC LIMIT 1",null)) {
            if(!c.moveToFirst())return false;
            long elapsed=now-java.time.OffsetDateTime.parse(c.getString(6)).toInstant().toEpochMilli();
            return elapsed>=0 && elapsed<=10000 && material.code.equals(c.getString(0)) && from.equals(c.getString(1)) && to.equals(c.getString(2)) && Rules.quantity(amount).equals(c.getString(3)) && person.code.equals(c.getString(4)) && kind.equals(c.getString(5));
        }
    }
    long bookChecked(Material material,String from,String to,String amount,Person person,String kind,boolean allowDuplicate) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            if(material==null || person==null || material(material.code)==null || person(person.code)==null) throw new IllegalArgumentException("Artikel oder Person nicht mehr vorhanden. Bitte neu auswählen.");
            if(!allowDuplicate && recentDuplicate(material,from,to,amount,person,kind,System.currentTimeMillis()))throw new DuplicateMovement();
            long id=book(material,from,to,amount,person,kind);db.setTransactionSuccessful();return id;
        }finally{db.endTransaction();}
    }
    private static void createMailTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE mail_commands (id TEXT PRIMARY KEY, state TEXT NOT NULL, reply TEXT NOT NULL, cutoff INTEGER NOT NULL DEFAULT 0, result TEXT NOT NULL, created INTEGER NOT NULL, auto_archive INTEGER NOT NULL DEFAULT 0)");
    }
    public Person person(String code) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT name,location FROM people WHERE code=?", new String[]{code})) {
            return c.moveToFirst() ? new Person(code, c.getString(0),c.getString(1)) : null;
        }
    }
    public void addPerson(String code,String name) { addPerson(code,name,""); }
    public void addPerson(String code, String name, String location) {
        ContentValues v = new ContentValues();
        v.put("code", Rules.required(code, "Personencode")); v.put("name", Rules.required(name, "Name")); v.put("location",optionalLocation(location));
        getWritableDatabase().insertOrThrow("people", null, v);Backup.request(app);
    }
    public Material material(String code) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT code,number,description,type,unit,location FROM materials WHERE code=?", new String[]{code})) {
            return c.moveToFirst() ? new Material(c) : null;
        }
    }
    public List<Material> materials(String type, String query) {
        List<Material> items=new ArrayList<>();
        String term=query==null ? "" : query.trim();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT code,number,description,type,unit,location FROM materials WHERE type=? AND (instr(lower(description),lower(?))>0 OR instr(lower(number),lower(?))>0 OR instr(code,?)>0) ORDER BY description COLLATE NOCASE LIMIT 200",new String[]{type,term,term,term})) {
            while(c.moveToNext()) items.add(new Material(c));
        }
        return items;
    }
    public void addMaterial(String code,String number,String description,String type,String unit) {addMaterial(code,number,description,type,unit,"");}
    public void addMaterial(String code, String number, String description, String type, String unit,String location) {
        ContentValues v = new ContentValues();
        v.put("code", Rules.required(code, "Scancode")); v.put("number", Rules.required(number, "Materialnummer"));
        v.put("description", Rules.required(description, "Beschreibung"));
        v.put("type", Rules.required(type, "Art")); v.put("unit", Rules.required(unit, "Einheit")); v.put("location",optionalLocation(location));
        getWritableDatabase().insertOrThrow("materials", null, v);Backup.request(app);
    }
    private String optionalLocation(String location) { return location==null || location.trim().isEmpty() ? "" : Rules.required(location,"Lagernummer"); }
    public void assignLocation(String code,String location,boolean people) {
        ContentValues values=new ContentValues(); values.put("location",Rules.required(location,"Lagernummer"));
        if(getWritableDatabase().update(people ? "people" : "materials",values,"code=?",new String[]{code})!=1) throw new IllegalArgumentException("Eintrag nicht gefunden.");
        Backup.request(app);
    }
    void updatePerson(String original,String code,String name,String location) {
        ContentValues values=new ContentValues();values.put("code",Rules.required(code,"Personencode"));values.put("name",Rules.required(name,"Name"));values.put("location",optionalLocation(location));
        if(getWritableDatabase().update("people",values,"code=?",new String[]{original})!=1) throw new IllegalArgumentException("Person nicht mehr vorhanden.");Backup.request(app);
    }
    void updateMaterial(String original,String code,String number,String description,String type,String unit,String location) {
        if(!java.util.Arrays.asList("Material","Kältemittel").contains(type)) throw new IllegalArgumentException("Ungültige Artikelart.");
        ContentValues values=new ContentValues();values.put("code",Rules.required(code,"Scancode"));values.put("number",Rules.required(number,"Materialnummer"));values.put("description",Rules.required(description,"Beschreibung"));
        values.put("type",type);values.put("unit",Rules.required(unit,"Einheit"));values.put("location",optionalLocation(location));
        if(getWritableDatabase().update("materials",values,"code=?",new String[]{original})!=1) throw new IllegalArgumentException("Material nicht mehr vorhanden.");Backup.request(app);
    }
    public void deleteEntry(String code,boolean people) {
        if(getWritableDatabase().delete(people ? "people" : "materials","code=?",new String[]{Rules.required(code,"Code")})!=1)
            throw new IllegalArgumentException("Eintrag nicht mehr vorhanden.");
        // Bookings own their historical snapshots; never cascade into movements or archives.
        Backup.request(app);
    }
    public List<Person> people() {
        List<Person> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT code,name,location FROM people ORDER BY name",null)) {while(c.moveToNext()) result.add(new Person(c.getString(0),c.getString(1),c.getString(2)));}
        return result;
    }
    public List<Material> allMaterials() {
        List<Material> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT code,number,description,type,unit,location FROM materials ORDER BY description",null)) {while(c.moveToNext()) result.add(new Material(c));}
        return result;
    }
    public int writeXlsx(java.io.OutputStream stream,long cutoff,boolean archived) throws IOException {return writeXlsx(stream,cutoff,archived,WorkProgress.NONE);}
    int writeXlsx(java.io.OutputStream stream,long cutoff,boolean archived,WorkProgress progress) throws IOException {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,timestamp,code,number,description,type,unit,from_location,to_location,quantity,person_code,person_name,archived_batch,movement_kind FROM movements WHERE id<=? AND archived_batch IS "+(archived ? "NOT NULL" : "NULL")+" ORDER BY id",new String[]{Long.toString(cutoff)})) {
            return ExcelExport.write(stream,c,progress);
        }
    }
    public long book(Material material, String from, String to, String quantity, Person person) {return book(material,from,to,quantity,person,"");}
    public long book(Material material, String from, String to, String quantity, Person person,String kind) {
        if(!kind.isEmpty() && !java.util.Arrays.asList("Entnahme","Rückgabe","Umlagerung").contains(kind)) throw new IllegalArgumentException("Ungültige Bewegungsart.");
        if (material == null || person == null) throw new IllegalArgumentException("Material und Anmeldung erforderlich.");
        from = Rules.required(from, "Von Lager"); to = Rules.required(to, "Nach Lager"); Rules.locations(from, to);
        ContentValues v = new ContentValues();
        v.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT).format(new Date()));
        v.put("code", material.code); v.put("number", material.number); v.put("description", material.description);
        v.put("type", material.type); v.put("unit", material.unit);v.put("movement_kind",kind);
        v.put("from_location", from); v.put("to_location", to); v.put("quantity", Rules.quantity(quantity));
        v.put("person_code", person.code); v.put("person_name", person.name);
        long id=getWritableDatabase().insertOrThrow("movements", null, v);Backup.request(app);return id;
    }
    static final class ImportChanged extends IllegalArgumentException {ImportChanged(){super("Die Daten wurden seit der Vorschau geändert. Bitte erneut prüfen.");}}
    enum ImportPolicy { REJECT, SKIP, UPDATE }
    static final class ImportConflict extends IllegalArgumentException {
        final int count;
        ImportConflict(int count) {super(count+" Codes sind bereits vorhanden.");this.count=count;}
    }
    int importConflicts(List<String[]> rows,boolean people) {
        int count=0;
        for(String[] row:rows) if(people ? person(row[0])!=null : material(row[0])!=null) count++;
        return count;
    }
    public void importRows(List<String[]> rows,boolean people) {importRows(rows,people,ImportPolicy.REJECT,WorkProgress.NONE);}
    void importRows(List<String[]> rows,boolean people,WorkProgress progress) {importRows(rows,people,ImportPolicy.REJECT,progress);}
    int[] importRows(List<String[]> rows,boolean people,ImportPolicy policy,WorkProgress progress) {return importRows(rows,people,policy,progress,-1);}
    int[] importRows(List<String[]> rows,boolean people,ImportPolicy policy,WorkProgress progress,long expectedRevision) {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            if(expectedRevision>=0 && revision()!=expectedRevision)throw new ImportChanged();
            java.util.Set<String> codes=new java.util.HashSet<>();
            for(String[] row:rows) if(!codes.add(Rules.required(row[0],"Code"))) throw new IllegalArgumentException("Doppelter Code innerhalb der Datei: "+row[0]);
            int conflicts=importConflicts(rows,people);
            if(policy==ImportPolicy.REJECT && conflicts>0) throw new ImportConflict(conflicts);
            int[] result;
            if(policy==ImportPolicy.UPDATE) {
                int[] counts=new MailStore(this).upsert(rows,people,progress);
                result=new int[]{counts[0],counts[1],0};
            } else {
                int processed=0,added=0,skipped=0;progress.update(0,rows.size(),"Einträge übernehmen …");
                for(String[] row:rows) {
                    boolean exists=people ? person(row[0])!=null : material(row[0])!=null;
                    if(exists) skipped++;
                    else {
                        if(people) addPerson(row[0],row[1],row.length>2 ? row[2] : "");
                        else addMaterial(row[0],row[1],row[2],row[3],row[4],row.length>5 ? row[5] : "");
                        added++;
                    }
                    progress.update(++processed,rows.size(),"Einträge übernehmen …");
                }
                result=new int[]{added,0,skipped};
            }
            progress.completeCancellation();db.setTransactionSuccessful();Backup.request(app);return result;
        } finally {db.endTransaction();}
    }
    public long cutoff() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(MAX(id),0) FROM movements WHERE archived_batch IS NULL", null)) {
            c.moveToFirst(); return c.getLong(0);
        }
    }
    public long count(boolean archived) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM movements WHERE archived_batch IS " + (archived ? "NOT NULL" : "NULL"), null)) {
            c.moveToFirst(); return c.getLong(0);
        }
    }
    public List<String> catalog(boolean people) {
        List<String> lines = new ArrayList<>();
        String sql = people ? "SELECT code,name FROM people ORDER BY name LIMIT 200" : "SELECT number,description,code,unit FROM materials ORDER BY description LIMIT 200";
        try (Cursor c = getReadableDatabase().rawQuery(sql, null)) {
            while (c.moveToNext()) lines.add(people ? c.getString(1) + "\nCode: " + c.getString(0) : c.getString(0) + " · " + c.getString(1) + "\nCode: " + c.getString(2) + " · " + c.getString(3));
        }
        return lines;
    }
    public List<String> history(boolean archived) {
        List<String> lines = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT number,description,quantity,unit,from_location,to_location,timestamp,person_name FROM movements WHERE archived_batch IS " + (archived ? "NOT NULL" : "NULL") + " ORDER BY id DESC LIMIT 100", null)) {
            while (c.moveToNext()) {
                lines.add(c.getString(0) + " · " + c.getString(1) + "\n" + c.getString(2) + " " + c.getString(3) + "  |  " + c.getString(4) + " → " + c.getString(5) + "\n" + c.getString(7) + " · " + c.getString(6));
            }
        }
        return lines;
    }
    /** Writes only the snapshot selected before opening the system file picker. Does not archive. */
    public int writeCsv(Writer writer, long cutoff, boolean archived) throws IOException {return writeCsv(writer,cutoff,archived,WorkProgress.NONE);}
    int writeCsv(Writer writer,long cutoff,boolean archived,WorkProgress progress) throws IOException {
        writer.write('\uFEFF');
        writer.write(String.join(";",ExcelExport.HEADERS)+"\r\n");
        int count = 0;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,timestamp,code,number,description,type,unit,from_location,to_location,quantity,person_code,person_name,archived_batch,movement_kind FROM movements WHERE id<=? AND archived_batch IS " + (archived ? "NOT NULL" : "NULL") + " ORDER BY id", new String[]{Long.toString(cutoff)})) {
            int total=c.getCount();progress.update(0,total,"CSV-Bericht schreiben …");
            while (c.moveToNext()) {
                String[] values=ExcelExport.values(c);
                values[2]=values[2].replace('.', ',');
                for(int i=0;i<values.length;i++) {
                    if(i>0) writer.write(';');
                    writer.write(Rules.csv(values[i]));
                }
                writer.write("\r\n"); count++;progress.update(count,total,"CSV-Bericht schreiben …");
            }
        }
        return count;
    }
    public long archiveCutoff() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(MAX(id),0) FROM movements WHERE archived_batch IS NOT NULL", null)) {
            c.moveToFirst(); return c.getLong(0);
        }
    }
    /** Called only after the output stream has successfully closed. Catalogs and people remain intact. */
    public int archive(long cutoff, String batch) {
        ContentValues v = new ContentValues(); v.put("archived_batch", batch);
        int count=getWritableDatabase().update("movements", v, "id<=? AND archived_batch IS NULL", new String[]{Long.toString(cutoff)});Backup.request(app);return count;
    }
}
