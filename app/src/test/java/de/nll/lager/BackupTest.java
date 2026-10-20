package de.nll.lager;

import android.content.*;
import android.content.pm.ProviderInfo;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowContentResolver;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class BackupTest {
    Context context;Store store;Provider provider;
    final Uri tree=DocumentsContract.buildTreeDocumentUri("backup.test","root");
    @Before public void setup() {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase("nll-lager.db");
        for(String pref:Arrays.asList("admin-settings","backup","mail")) context.getSharedPreferences(pref,0).edit().clear().commit();
        store=new Store(context);provider=new Provider();ProviderInfo info=new ProviderInfo();info.authority="backup.test";info.exported=true;info.grantUriPermissions=true;
        info.readPermission="android.permission.MANAGE_DOCUMENTS";info.writePermission="android.permission.MANAGE_DOCUMENTS";
        provider.attachInfo(context,info);ShadowContentResolver.registerProviderInternal("backup.test",provider);
    }
    @After public void cleanup() {
        store.close();context.deleteDatabase("nll-lager.db");
        for(String pref:Arrays.asList("admin-settings","backup","mail")) context.getSharedPreferences(pref,0).edit().clear().commit();
        for(File f:provider.files.values()) f.delete();
    }
    void folder() {Backup.prefs(context).edit().putString("tree",tree.toString()).commit();}
    void seed() {
        new AdminSettings(context).saveLocations(Arrays.asList("A","B"));
        store.addMaterial("0001","M1","Ventil","Material","Stk","A");store.addPerson("0007","Anna","B");
        store.book(store.material("0001"),"A","B","1.25",store.person("0007"),"Entnahme");store.archive(store.cutoff(),"old-export");
        store.book(store.material("0001"),"B","A","2",store.person("0007"),"Rückgabe");
    }
    @Test public void completeRoundTripPreservesSnapshotsArchiveLocationsAndNoCredentials() throws Exception {
        seed();new AdminSettings(context).setPassword("My-secret-password".toCharArray());
        BackupData before=Backup.snapshot(context);byte[] bytes=before.bytes();
        assertFalse(new String(bytes,"UTF-8").contains("passwordHash"));assertFalse(new String(bytes,"UTF-8").contains("My-secret-password"));
        store.addMaterial("extra","M2","Zweiter","Material","Stk");
        BackupData.read(new ByteArrayInputStream(bytes)).restore(store);
        assertNull(store.material("extra"));assertEquals("A",store.material("0001").location);assertEquals("Anna",store.person("0007").name);
        assertEquals(1,store.count(false));assertEquals(1,store.count(true));
        assertEquals(Arrays.asList("A","B"),new AdminSettings(context).locations());
        assertTrue(new AdminSettings(context).verify("My-secret-password".toCharArray()));
        assertEquals(before.hash,Backup.snapshot(context).hash);
        assertEquals(3,store.book(store.material("0001"),"A","B","1",store.person("0007"),"Umlagerung"));
    }
    @Test public void corruptedUnsupportedDuplicateAndMalformedBackupsCannotReplaceCurrentData() throws Exception {
        seed();JSONObject original=new JSONObject(new String(Backup.snapshot(context).bytes(),"UTF-8"));
        JSONObject broken=new JSONObject(original.toString());broken.put("sha256","bad");
        assertThrows(Exception.class,() -> BackupData.read(new ByteArrayInputStream(broken.toString().getBytes("UTF-8"))));
        original.put("version",2);assertThrows(Exception.class,() -> BackupData.read(new ByteArrayInputStream(original.toString().getBytes("UTF-8"))));
        original.put("version",1);JSONObject data=original.getJSONObject("data");data.getJSONArray("materials").put(data.getJSONArray("materials").getJSONArray(0));
        original.put("sha256",BackupData.digest(data.toString().getBytes("UTF-8")));
        assertThrows(Exception.class,() -> BackupData.read(new ByteArrayInputStream(original.toString().getBytes("UTF-8"))));
        assertEquals(1,store.allMaterials().size());assertEquals(1,store.count(false));assertEquals(1,store.count(true));
    }
    @Test public void documentBackupIsVerifiedAndRetentionKeepsSevenWithoutTouchingOtherFiles() throws Exception {
        seed();folder();Uri root=DocumentsContract.buildDocumentUriUsingTree(tree,"root");
        Uri unrelated=DocumentsContract.createDocument(context.getContentResolver(),root,"text/plain","meine-datei.txt");
        for(int i=0;i<9;i++) Backup.save(context,true);
        assertEquals(7,Backup.documents(context,tree).size());assertNotNull(provider.queryDocument(DocumentsContract.getDocumentId(unrelated),null));
        for(Backup.Document d:Backup.documents(context,tree)) try(InputStream in=context.getContentResolver().openInputStream(d.uri)) {assertEquals(Backup.snapshot(context).hash,BackupData.read(in).hash);}
        assertFalse(Backup.save(context,false));assertTrue(Backup.prefs(context).getLong("last",0)>0);
    }
    @Test public void failedWriteOrRenameDoesNotPruneOrClaimSuccessfulBackup() throws Exception {
        seed();folder();Backup.save(context,true);long last=Backup.prefs(context).getLong("last",0);
        provider.failWrite=true;assertThrows(Exception.class,() -> Backup.save(context,true));provider.failWrite=false;
        provider.failRename=true;assertThrows(Exception.class,() -> Backup.save(context,true));
        assertEquals(1,Backup.documents(context,tree).size());assertEquals(last,Backup.prefs(context).getLong("last",0));
        assertEquals(1,store.count(false));
    }
    @Test public void restoreCreatesRescueCopyAndNeverReplaysMail() throws Exception {
        seed();folder();BackupData selected=Backup.snapshot(context);store.addMaterial("new","M2","Neuer Eintrag","Material","Stk");
        new MailStore(store).queue("old-mail","missing-file",1,"old");
        MailConfig.prefs(context).edit().putBoolean("connected",true).commit();
        Backup.restore(context,selected);
        assertNull(store.material("new"));assertFalse(MailConfig.prefs(context).getBoolean("connected",true));assertFalse(new MailStore(store).exists("old-mail"));
        Backup.Document rescue=Backup.documents(context,tree).get(0);
        try(InputStream in=context.getContentResolver().openInputStream(rescue.uri)) {assertEquals(2,BackupData.read(in).data.getJSONArray("materials").length());}
        assertNotNull(((android.app.job.JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getPendingJob(Backup.JOB));
    }
    @Test public void restoreRequiresRescueDestinationAndWaitsForMailWorker() throws Exception {
        seed();BackupData selected=Backup.snapshot(context);store.addPerson("p2","Ben");
        assertThrows(Exception.class,() -> Backup.restore(context,selected));assertNotNull(store.person("p2"));
        folder();MailSync.BUSY.set(true);
        try {assertThrows(Exception.class,() -> Backup.restore(context,selected));assertNotNull(store.person("p2"));} finally {MailSync.BUSY.set(false);}
    }
    @Test public void databaseFailureRollsBackAllBusinessTablesAndWarehouseSettings() throws Exception {
        seed();BackupData selected=Backup.snapshot(context);store.addPerson("p2","Ben");new AdminSettings(context).saveLocations(Arrays.asList("Neu"));
        store.getWritableDatabase().execSQL("CREATE TRIGGER refuse_restore BEFORE INSERT ON movements BEGIN SELECT RAISE(ABORT,'test'); END");
        assertThrows(Exception.class,() -> selected.restore(store));assertNotNull(store.person("p2"));assertEquals(Arrays.asList("Neu"),new AdminSettings(context).locations());assertEquals(1,store.count(false));
    }
    @Test public void legacyWarehousePreferencesAreCapturedAndVersionFiveUpgradesSafely() throws Exception {
        context.getSharedPreferences("admin-settings",0).edit().putString("locations","[\"Alt\"]").commit();
        store.getWritableDatabase().execSQL("DROP TABLE app_settings");store.getWritableDatabase().setVersion(5);store.close();store=new Store(context);
        assertEquals(Arrays.asList("Alt"),new AdminSettings(context).locations());
        assertEquals("Alt",Backup.snapshot(context).data.getJSONArray("locations").getString(0));
    }
    @Test public void deletedLatestBackupIsRecreatedAndCorruptedWriteNeverBecomesComplete() throws Exception {
        seed();folder();Backup.save(context,true);
        DocumentsContract.deleteDocument(context.getContentResolver(),Backup.documents(context,tree).get(0).uri);
        assertTrue(Backup.save(context,false));assertEquals(1,Backup.documents(context,tree).size());
        provider.corruptRead=true;assertThrows(Exception.class,() -> Backup.save(context,true));provider.corruptRead=false;
        assertEquals(1,Backup.documents(context,tree).size());
    }
    @Test public void restoreAfterSimulatedAppDataDeletionUsesSurvivingDocument() throws Exception {
        seed();folder();Backup.save(context,true);Uri saved=Backup.documents(context,tree).get(0).uri;
        store.close();context.deleteDatabase("nll-lager.db");
        for(String pref:Arrays.asList("admin-settings","backup","mail")) context.getSharedPreferences(pref,0).edit().clear().commit();
        store=new Store(context);assertTrue(store.allMaterials().isEmpty());folder();
        try(InputStream in=context.getContentResolver().openInputStream(saved)) {Backup.restore(context,BackupData.read(in));}
        assertEquals("Anna",store.person("0007").name);assertEquals(1,store.count(true));assertEquals(1,store.count(false));assertEquals(Arrays.asList("A","B"),new AdminSettings(context).locations());
        assertFalse(new AdminSettings(context).hasPassword());
    }
    @Test public void configuredFolderSchedulesChangesButReadOnlyFolderIsRejected() throws Exception {
        assertThrows(Exception.class,() -> Backup.configure(context,tree,Intent.FLAG_GRANT_READ_URI_PERMISSION));
        assertEquals("",Backup.prefs(context).getString("tree",""));
        folder();seed();assertNotNull(((android.app.job.JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).getPendingJob(Backup.JOB));
    }
    public static class Provider extends DocumentsProvider {
        final Map<String,File> files=new LinkedHashMap<>();final Map<String,String> names=new HashMap<>();boolean failWrite,failRename,corruptRead;
        @Override public boolean onCreate() {return true;}
        @Override public Cursor queryRoots(String[] projection) {return new MatrixCursor(new String[]{"root_id","document_id","title","flags"});}
        String[] columns(String[] projection) {return projection!=null ? projection : new String[]{"document_id","_display_name","mime_type","flags","_size","last_modified"};}
        void row(MatrixCursor c,String id) throws FileNotFoundException {
            if(!id.equals("root") && !files.containsKey(id)) throw new FileNotFoundException();MatrixCursor.RowBuilder row=c.newRow();
            for(String col:c.getColumnNames()) {
                switch(col) {
                    case "document_id":row.add(id);break;
                    case "_display_name":row.add(names.getOrDefault(id,"root"));break;
                    case "mime_type":row.add(id.equals("root") ? DocumentsContract.Document.MIME_TYPE_DIR : "application/json");break;
                    case "flags":row.add(0);break;
                    case "_size":row.add(id.equals("root") ? 0 : files.get(id).length());break;
                    default:row.add(0);
                }
            }
        }
        @Override public Cursor queryDocument(String id,String[] projection) throws FileNotFoundException {MatrixCursor c=new MatrixCursor(columns(projection));row(c,id);return c;}
        @Override public Cursor queryChildDocuments(String id,String[] projection,String sort) throws FileNotFoundException {MatrixCursor c=new MatrixCursor(columns(projection));for(String key:files.keySet()) row(c,key);return c;}
        @Override public String createDocument(String parent,String mime,String name) throws FileNotFoundException {
            String id=UUID.randomUUID().toString();try {files.put(id,File.createTempFile("backup-provider-",".json",getContext().getCacheDir()));names.put(id,name);return id;} catch(IOException e) {throw new FileNotFoundException();}
        }
        @Override public String renameDocument(String id,String name) throws FileNotFoundException {if(failRename) throw new FileNotFoundException("rename failed");names.put(id,name);return id;}
        @Override public void deleteDocument(String id) {File file=files.remove(id);if(file!=null) file.delete();names.remove(id);}
        @Override public boolean isChildDocument(String parent,String id) {return parent.equals("root") && files.containsKey(id);}
        @Override public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal) throws FileNotFoundException {
            if(corruptRead && mode.equals("r")) {
                try {File damaged=File.createTempFile("damaged-",".json",getContext().getCacheDir());try(FileOutputStream out=new FileOutputStream(damaged)) {out.write("broken".getBytes());}return ParcelFileDescriptor.open(damaged,ParcelFileDescriptor.MODE_READ_ONLY);} catch(IOException e) {throw new FileNotFoundException();}
            }
            if(failWrite && mode.contains("w")) throw new FileNotFoundException("write failed");return ParcelFileDescriptor.open(files.get(id),ParcelFileDescriptor.parseMode(mode));
        }
    }
}
