package de.nll.lager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class Feature07Test {
    Context context;Store store;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();context.deleteDatabase("nll-lager.db");for(String pref:Arrays.asList("admin-settings","backup","mail"))context.getSharedPreferences(pref,0).edit().clear().commit();store=new Store(context);seed();}
    @After public void cleanup(){store.close();context.deleteDatabase("nll-lager.db");}
    void seed(){store.addPerson("P","Anna","B");store.addMaterial("M","MAT1","Ventil","Material","Stk","A");}
    long book(){return store.book(store.material("M"),"A","B","1.25",store.person("P"),"Entnahme");}
    byte[] envelope(JSONObject value)throws Exception{value.put("sha256",BackupData.digest(value.getJSONObject("data").toString().getBytes(StandardCharsets.UTF_8)));return value.toString().getBytes(StandardCharsets.UTF_8);}
    @Test public void editAllFieldsIncludingCodesPreservesHistoryAndRejectsDuplicateCodes()throws Exception{
        book();store.addPerson("P2","Ben");store.addMaterial("M2","2","Andere","Material","Stk");
        assertThrows(android.database.sqlite.SQLiteConstraintException.class,()->store.updatePerson("P","P2","Changed","C"));assertEquals("Anna",store.person("P").name);
        assertThrows(android.database.sqlite.SQLiteConstraintException.class,()->store.updateMaterial("M","M2","X","Changed","Kältemittel","kg","C"));assertEquals("Ventil",store.material("M").description);
        store.updatePerson("P","P-neu","Anne","C");store.updateMaterial("M","M-neu","MAT2","R32","Kältemittel","kg","C");
        assertNull(store.person("P"));assertNull(store.material("M"));assertEquals("kg",store.material("M-neu").unit);
        StringWriter csv=new StringWriter();store.writeCsv(csv,store.cutoff(),false);assertTrue(csv.toString().contains("Ventil"));assertTrue(csv.toString().contains("Anna"));assertFalse(csv.toString().contains("R32"));
        assertEquals("Anne",store.person("P-neu").name);
    }
    @Test public void cancellationIsAnOpenLinkedReversalEvenWhenOriginalIsArchived()throws Exception{
        long id=book();store.archive(id,"exported");long reverse=store.cancelMovement(id,"Falsche Menge","P");
        Store.Movement row=store.movement(reverse);assertEquals("Storno",row.values[13]);assertEquals("B",row.values[7]);assertEquals("A",row.values[8]);assertEquals("1.25",row.values[9]);assertNull(row.values[12]);
        assertTrue(store.movement(id).reversed);assertEquals("Ventil",store.movement(id).values[4]);assertEquals("Falsche Menge",row.values[15]);assertEquals("P",row.values[16]);
        assertEquals(1,store.count(false));assertEquals(1,store.count(true));
        assertThrows(IllegalArgumentException.class,()->store.cancelMovement(id,"nochmal","P"));assertThrows(IllegalArgumentException.class,()->store.cancelMovement(reverse,"nochmal","P"));
        StringWriter csv=new StringWriter();store.writeCsv(csv,store.cutoff(),false);assertTrue(csv.toString().contains("Storno #"+id));assertTrue(csv.toString().contains("B -> A"));
        assertEquals(ExcelExport.HEADERS.length,csv.toString().split("\r\n")[0].split(";").length);
    }
    @Test public void invalidCancellationAndFailedTransactionDoNotCreateAnyReversal(){
        long id=book();long before=store.revision();
        assertThrows(IllegalArgumentException.class,()->store.cancelMovement(id,"","P"));assertEquals(before,store.revision());
        store.getWritableDatabase().execSQL("CREATE TRIGGER refuse_storno BEFORE INSERT ON movements WHEN NEW.reversal_of IS NOT NULL BEGIN SELECT RAISE(ABORT,'test'); END");
        assertThrows(Exception.class,()->store.cancelMovement(id,"Fehler","P"));assertEquals(1,store.count(false));assertFalse(store.movement(id).reversed);assertEquals(before,store.revision());
    }
    @Test public void duplicateCheckSurvivesRestartAndRequiresExplicitOverride(){
        store.bookChecked(store.material("M"),"A","B","1.25",store.person("P"),"Entnahme",false);store.close();store=new Store(context);
        assertTrue(store.recentDuplicate(store.material("M"),"A","B","1,250",store.person("P"),"Entnahme",System.currentTimeMillis()));
        assertFalse(store.recentDuplicate(store.material("M"),"A","B","2",store.person("P"),"Entnahme",System.currentTimeMillis()));
        assertThrows(Store.DuplicateMovement.class,()->store.bookChecked(store.material("M"),"A","B","1.25",store.person("P"),"Entnahme",false));
        assertEquals(1,store.count(false));store.bookChecked(store.material("M"),"A","B","1.25",store.person("P"),"Entnahme",true);assertEquals(2,store.count(false));
        assertFalse(store.recentDuplicate(store.material("M"),"A","B","1.25",store.person("P"),"Entnahme",System.currentTimeMillis()+11000));
    }
    @Test public void cancelledBookingDoesNotPreventCorrectedReentry(){
        long id=book();store.cancelMovement(id,"Korrektur","P");
        store.bookChecked(store.material("M"),"A","B","1.25",store.person("P"),"Entnahme",false);assertEquals(3,store.count(false));
    }
    @Test public void versionTwoBackupPreservesCancellationAndRejectsBrokenReferences()throws Exception{
        long id=book();long reversal=store.cancelMovement(id,"Korrektur","P");byte[] bytes=Backup.snapshot(context).bytes();assertEquals(2,new JSONObject(new String(bytes,StandardCharsets.UTF_8)).getInt("version"));
        store.addPerson("extra","Extra");BackupData.read(new ByteArrayInputStream(bytes)).restore(store);assertNull(store.person("extra"));assertTrue(store.movement(id).reversed);assertEquals("Korrektur",store.movement(reversal).values[15]);
        JSONObject malformed=new JSONObject(new String(bytes,StandardCharsets.UTF_8));malformed.getJSONObject("data").getJSONArray("movements").getJSONArray(1).put(14,999);
        assertThrows(Exception.class,()->BackupData.read(new ByteArrayInputStream(envelope(malformed))));
        assertThrows(IllegalArgumentException.class,()->store.cancelMovement(id,"doppelt","P"));
    }
    @Test public void legacyFormatOneBackupRemainsReadableAndChecksumIsCheckedBeforeMigration()throws Exception{
        book();JSONObject value=new JSONObject(new String(Backup.snapshot(context).bytes(),StandardCharsets.UTF_8));value.put("version",1);JSONArray row=value.getJSONObject("data").getJSONArray("movements").getJSONArray(0);while(row.length()>14)row.remove(row.length()-1);
        byte[] bytes=envelope(value);BackupData legacy=BackupData.read(new ByteArrayInputStream(bytes));assertEquals(17,legacy.data.getJSONArray("movements").getJSONArray(0).length());legacy.restore(store);assertEquals(1,store.count(false));
        value.put("sha256","wrong");assertThrows(Exception.class,()->BackupData.read(new ByteArrayInputStream(value.toString().getBytes(StandardCharsets.UTF_8))));
    }
    @Test public void previewShowsOnlyRealFieldChangesAndNeverWritesData(){
        List<String[]> rows=Arrays.asList(new String[]{"M","MAT2","Neues Ventil","Material","Stk","A"},new String[]{"N","N1","Neu","Material","Stk"});long revision=store.revision();
        ImportPreview preview=ImportPreview.read(store,rows,false,WorkProgress.NONE);assertEquals(1,preview.added);assertEquals(1,preview.changed);assertTrue(preview.entries.get(0).contains("MAT1 → MAT2"));assertTrue(preview.entries.get(0).contains("Ventil → Neues Ventil"));assertEquals(revision,store.revision());assertNull(store.material("N"));
        preview=ImportPreview.read(store,Collections.singletonList(new String[]{"P","Anna"}),true,WorkProgress.NONE);assertEquals(1,preview.unchanged);assertFalse(preview.entries.get(0).contains("Lagernummer"));
    }
    @Test public void staleImportPreviewCannotOverwriteLaterCatalogChanges(){
        List<String[]> rows=Collections.singletonList(new String[]{"M","MAT2","Import","Material","Stk"});
        ImportPreview preview=ImportPreview.read(store,rows,false,WorkProgress.NONE);
        store.updateMaterial("M","M","MAT3","Neu auf Gerät","Material","Stk","A");
        assertThrows(Store.ImportChanged.class,()->store.importRows(rows,false,Store.ImportPolicy.UPDATE,WorkProgress.NONE,preview.revision));
        assertEquals("Neu auf Gerät",store.material("M").description);
    }
    @Test public void revisionRollsBackWithBusinessTransactionAndSurvivesReopening(){
        long revision=store.revision();String identity=store.databaseId();SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();try{store.addPerson("transient","Transient");}finally{db.endTransaction();}
        assertEquals(revision,store.revision());assertNull(store.person("transient"));store.updatePerson("P","P","Anne","B");assertTrue(store.revision()>revision);
        store.close();store=new Store(context);assertEquals(identity,store.databaseId());assertTrue(store.revision()>revision);
    }
    @Test public void schemaSixUpgradesWithoutChangingOriginalMovementOrWarehouses(){
        store.close();context.deleteDatabase("nll-lager.db");SQLiteDatabase db=context.openOrCreateDatabase("nll-lager.db",0,null);
        db.execSQL("CREATE TABLE materials(code TEXT PRIMARY KEY,number TEXT,description TEXT,type TEXT,unit TEXT,location TEXT)");db.execSQL("INSERT INTO materials VALUES('M','MAT1','Ventil','Material','Stk','A')");
        db.execSQL("CREATE TABLE people(code TEXT PRIMARY KEY,name TEXT,location TEXT)");db.execSQL("INSERT INTO people VALUES('P','Anna','B')");
        db.execSQL("CREATE TABLE movements(id INTEGER PRIMARY KEY AUTOINCREMENT,timestamp TEXT,code TEXT,number TEXT,description TEXT,type TEXT,unit TEXT,from_location TEXT,to_location TEXT,quantity TEXT,person_code TEXT,person_name TEXT,archived_batch TEXT,movement_kind TEXT)");
        db.execSQL("INSERT INTO movements VALUES(1,'2026-10-20T10:00:00+02:00','M','MAT1','Ventil','Material','Stk','A','B','1','P','Anna',NULL,'Entnahme')");
        db.execSQL("CREATE TABLE mail_commands(id TEXT PRIMARY KEY,state TEXT,reply TEXT,cutoff INTEGER,result TEXT,created INTEGER,auto_archive INTEGER)");db.execSQL("CREATE TABLE app_settings(key TEXT PRIMARY KEY,value TEXT NOT NULL)");db.execSQL("INSERT INTO app_settings VALUES('locations','[\"A\",\"B\"]')");db.setVersion(6);db.close();
        store=new Store(context);assertEquals(7,store.getReadableDatabase().getVersion());assertEquals("Ventil",store.movement(1).values[4]);assertNull(store.movement(1).values[14]);assertEquals(Arrays.asList("A","B"),new AdminSettings(context).locations());
        assertTrue(store.cancelMovement(1,"Migration geprüft","P")>1);
    }
}
