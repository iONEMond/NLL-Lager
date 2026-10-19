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
    public Store(Context context) { super(context, "nll-lager.db", null, 3); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE materials (code TEXT PRIMARY KEY, number TEXT NOT NULL, description TEXT NOT NULL, type TEXT NOT NULL, unit TEXT NOT NULL, location TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE people (code TEXT PRIMARY KEY, name TEXT NOT NULL, location TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE movements (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT NOT NULL, code TEXT NOT NULL, number TEXT NOT NULL, description TEXT NOT NULL, type TEXT NOT NULL, unit TEXT NOT NULL, from_location TEXT NOT NULL, to_location TEXT NOT NULL, quantity TEXT NOT NULL, person_code TEXT NOT NULL, person_name TEXT NOT NULL, archived_batch TEXT)");
        db.execSQL("CREATE INDEX movements_pending ON movements(archived_batch, id)");
        createMailTables(db);
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(oldVersion<2) {
            db.execSQL("ALTER TABLE materials ADD COLUMN location TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE people ADD COLUMN location TEXT NOT NULL DEFAULT ''");
        }
        if(oldVersion<3) createMailTables(db);
    }
    private static void createMailTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE mail_commands (id TEXT PRIMARY KEY, state TEXT NOT NULL, reply TEXT NOT NULL, cutoff INTEGER NOT NULL DEFAULT 0, result TEXT NOT NULL, created INTEGER NOT NULL)");
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
        getWritableDatabase().insertOrThrow("people", null, v);
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
        getWritableDatabase().insertOrThrow("materials", null, v);
    }
    private String optionalLocation(String location) { return location==null || location.trim().isEmpty() ? "" : Rules.required(location,"Lagernummer"); }
    public void assignLocation(String code,String location,boolean people) {
        ContentValues values=new ContentValues(); values.put("location",Rules.required(location,"Lagernummer"));
        if(getWritableDatabase().update(people ? "people" : "materials",values,"code=?",new String[]{code})!=1) throw new IllegalArgumentException("Eintrag nicht gefunden.");
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
    public int writeXlsx(java.io.OutputStream stream,long cutoff,boolean archived) throws IOException {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT id,timestamp,code,number,description,type,unit,from_location,to_location,quantity,person_code,person_name,archived_batch FROM movements WHERE id<=? AND archived_batch IS "+(archived ? "NOT NULL" : "NULL")+" ORDER BY id",new String[]{Long.toString(cutoff)})) {
            return ExcelExport.write(stream,c);
        }
    }
    public long book(Material material, String from, String to, String quantity, Person person) {
        if (material == null || person == null) throw new IllegalArgumentException("Material und Anmeldung erforderlich.");
        from = Rules.required(from, "Von Lager"); to = Rules.required(to, "Nach Lager"); Rules.locations(from, to);
        ContentValues v = new ContentValues();
        v.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT).format(new Date()));
        v.put("code", material.code); v.put("number", material.number); v.put("description", material.description);
        v.put("type", material.type); v.put("unit", material.unit);
        v.put("from_location", from); v.put("to_location", to); v.put("quantity", Rules.quantity(quantity));
        v.put("person_code", person.code); v.put("person_name", person.name);
        return getWritableDatabase().insertOrThrow("movements", null, v);
    }
    public void importRows(List<String[]> rows, boolean people) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (String[] row : rows) {
                if (people) {
                    if (person(row[0]) != null) throw new IllegalArgumentException("Personencode existiert bereits: " + row[0]);
                    addPerson(row[0], row[1],row.length>2 ? row[2] : "");
                } else {
                    if (material(row[0]) != null) throw new IllegalArgumentException("Scancode existiert bereits: " + row[0]);
                    addMaterial(row[0], row[1], row[2], row[3], row[4],row.length>5 ? row[5] : "");
                }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
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
    public int writeCsv(Writer writer, long cutoff, boolean archived) throws IOException {
        writer.write('\uFEFF');
        writer.write("ID;Zeitpunkt;Scancode;Mat Nummer;Beschreibung;Art;Einheit;Von Lager;Nach Lager;Menge;Personencode;Person;Archivbericht\r\n");
        int count = 0;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id,timestamp,code,number,description,type,unit,from_location,to_location,quantity,person_code,person_name,archived_batch FROM movements WHERE id<=? AND archived_batch IS " + (archived ? "NOT NULL" : "NULL") + " ORDER BY id", new String[]{Long.toString(cutoff)})) {
            while (c.moveToNext()) {
                for (int i = 0; i < 13; i++) {
                    if (i > 0) writer.write(';');
                    String value = c.isNull(i) ? "" : c.getString(i);
                    if (i == 9) value = value.replace('.', ','); // Decimal comma for German Excel.
                    writer.write(Rules.csv(value));
                }
                writer.write("\r\n"); count++;
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
        return getWritableDatabase().update("movements", v, "id<=? AND archived_batch IS NULL", new String[]{Long.toString(cutoff)});
    }
}
