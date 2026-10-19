package de.nll.lager;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MailSyncTest {
    Context context;Store store;MailConfig config;MailStore mail;
    @Before public void setup() {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase("nll-lager.db");MailConfig.prefs(context).edit().clear().commit();
        MailConfig.save(context,"scanner@gmail.com","lager@example.com",true);
        config=new MailConfig(context);store=new Store(context);mail=new MailStore(store);
    }
    @After public void close() {store.close();context.deleteDatabase("nll-lager.db");}
    JSONObject message(String id,String from,String command) throws Exception {
        return new JSONObject().put("id",id).put("internalDate",System.currentTimeMillis()+1000)
            .put("payload",new JSONObject().put("headers",new JSONArray()
                .put(new JSONObject().put("name","From").put("value",from))
                .put(new JSONObject().put("name","Subject").put("value",command))));
    }
    String subject(String command) {return "NLL "+(command.equals("IMPORT MATERIAL") ? "Material" : command.equals("IMPORT PERSONEN") ? "Personen" : command);}
    void movement(String qty) {
        if(store.person("007")==null) store.addPerson("007","Anna","B");
        if(store.material("001")==null) store.addMaterial("001","M1","Ventil","Material","Stk","A");
        store.book(store.material("001"),"A","B",qty,store.person("007"));
    }
    static class FakeClient extends GmailClient {
        List<JSONObject> messages=new ArrayList<>();List<String> sent=new ArrayList<>();List<String> fetched=new ArrayList<>();boolean fail;byte[] xlsx;
        FakeClient() {super("unused");}
        @Override JSONObject call(String path,JSONObject payload) throws Exception {
            JSONArray ids=new JSONArray();for(JSONObject m:messages) ids.put(new JSONObject().put("id",m.getString("id")));
            return new JSONObject().put("messages",ids);
        }
        @Override JSONObject message(String id) throws Exception {for(JSONObject m:messages) if(id.equals(m.getString("id"))) return m;throw new IOException();}
        @Override byte[] attachment(String id,JSONObject part) {fetched.add(part.optString("filename"));return xlsx;}
        @Override void send(String mime) throws Exception {sent.add(mime);if(fail) throw new IOException("connection lost after acceptance");}
    }
    @Test public void simpleCommandsRequireExactSubjectAndSingleHeaders() throws Exception {
        assertEquals("",MailProtocol.command("EXPORT"));
        assertEquals("",MailProtocol.command("NLL wrong EXPORT"));
        assertEquals("",MailProtocol.command("NLL Export DELETE"));
        assertEquals("",MailProtocol.command("Re: NLL Export"));
        assertEquals("EXPORT",MailProtocol.command("  NLL eXpOrT  "));
        assertEquals("IMPORT MATERIAL",MailProtocol.command("NLL Material"));
        assertEquals("IMPORT PERSONEN",MailProtocol.command("NLL Personen"));
        JSONObject p=message("a",config.recipient,subject("EXPORT")).getJSONObject("payload");
        p.getJSONArray("headers").put(new JSONObject().put("name","From").put("value",config.recipient));
        assertEquals("",MailProtocol.header(p,"From"));
    }
    @Test public void wrongSenderAndStaleMailHaveNoEffectsOrReply() throws Exception {
        FakeClient client=new FakeClient();
        client.messages.add(message("a","other@example.com",subject("EXPORT")));
        JSONObject old=message("b",config.recipient,subject("EXPORT"));old.put("internalDate",config.since-1);client.messages.add(old);
        MailSync.sync(context,config,store,client);assertTrue(client.sent.isEmpty());assertTrue(mail.records(config.gmail).isEmpty());
    }
    @Test public void exportSurvivesRestartDoesNotArchiveUntilConfirmedAndExcludesLaterBookings() throws Exception {
        movement("1");FakeClient client=new FakeClient();client.messages.add(message("a",config.recipient,subject("EXPORT")));
        MailSync.sync(context,config,store,client);assertEquals(1,client.sent.size());assertEquals(1,store.count(false));assertTrue(client.sent.get(0).contains("NLL-Warenbewegungen.xlsx"));
        store.close();store=new Store(context);mail=new MailStore(store);
        MailSync.sync(context,config,store,client);assertEquals(1,client.sent.size());
        movement("2");mail.confirm(config.gmail+":a");assertEquals(1,store.count(false));assertEquals(1,store.count(true));
        assertThrows(IllegalArgumentException.class,() -> mail.confirm(config.gmail+":a"));
    }
    @Test public void ambiguousSendIsNeverAutomaticallyRepeatedAndCannotArchive() throws Exception {
        movement("1");FakeClient client=new FakeClient();client.fail=true;client.messages.add(message("a",config.recipient,subject("EXPORT")));
        assertThrows(IOException.class,() -> MailSync.sync(context,config,store,client));assertEquals("uncertain",mail.records(config.gmail).get(0)[1]);
        client.fail=false;MailSync.sync(context,config,store,client);assertEquals(1,client.sent.size());assertEquals(1,store.count(false));
        assertThrows(IllegalArgumentException.class,() -> mail.confirm(config.gmail+":a"));
        mail.state(config.gmail+":a","pending");MailSync.sync(context,config,store,client);assertEquals(2,client.sent.size());assertEquals(client.sent.get(0),client.sent.get(1));
    }
    @Test public void interruptedSendRecoveredAsUncertain() {mail.queue("a","file",0,"test");mail.state("a","sending");mail.recoverInterrupted();try(Cursor c=store.getReadableDatabase().rawQuery("SELECT state FROM mail_commands",null)) {assertTrue(c.moveToFirst());assertEquals("uncertain",c.getString(0));}}
    @Test public void upsertPreservesMissingRowsLocationsAndHistoricalSnapshots() {
        movement("1");store.addMaterial("002","M2","Zweiter","Material","Stk","C");
        SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try {assertArrayEquals(new int[]{0,1},mail.upsert(Collections.singletonList(new String[]{"001","M1","Neuer Name","Material","kg"}),false));db.setTransactionSuccessful();} finally {db.endTransaction();}
        assertEquals("A",store.material("001").location);assertNotNull(store.material("002"));assertEquals("Neuer Name",store.material("001").description);assertTrue(store.history(false).get(0).contains("Ventil"));
    }
    @Test public void failedBatchRollsBackAllUpdates() {
        store.addPerson("007","Anna","B");SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
        try {assertThrows(IllegalArgumentException.class,() -> mail.upsert(Arrays.asList(new String[]{"007","Neu"},new String[]{"008",""}),true));} finally {db.endTransaction();}
        assertEquals("Anna",store.person("007").name);assertNull(store.person("008"));
    }
    @Test public void excelMailImportAndReplyAreIdempotent() throws Exception {
        FakeClient client=new FakeClient();
        try(InputStream in=getClass().getResourceAsStream("/Personen-Import.xlsx")) {assertNotNull(in);client.xlsx=in.readAllBytes();}
        JSONObject m=message("import",config.recipient,subject("IMPORT PERSONEN"));
        m.getJSONObject("payload").put("parts",new JSONArray().put(new JSONObject().put("filename","personen.xlsx").put("body",new JSONObject().put("data","fake"))));
        client.messages.add(m);MailSync.sync(context,config,store,client);int count=store.people().size();assertTrue(count>0);assertEquals(1,client.sent.size());
        MailSync.sync(context,config,store,client);assertEquals(count,store.people().size());assertEquals(1,client.sent.size());
    }
    @Test public void malformedImportRepliesWithErrorAndLeavesCatalogIntact() throws Exception {
        store.addPerson("007","Anna","B");FakeClient client=new FakeClient();client.xlsx=new byte[]{1,2,3};
        JSONObject m=message("bad",config.recipient,subject("IMPORT PERSONEN"));
        m.getJSONObject("payload").put("parts",new JSONArray().put(new JSONObject().put("filename","personen.xlsx").put("body",new JSONObject())));
        client.messages.add(m);MailSync.sync(context,config,store,client);assertEquals(1,store.people().size());assertTrue(mail.records(config.gmail).get(0)[2].contains("abgelehnt"));assertEquals(1,client.sent.size());
    }
    @Test public void nestedSignatureImagesAreIgnoredWithoutDownloadingAndImportIsIdempotent() throws Exception {
        FakeClient client=new FakeClient();
        try(InputStream in=getClass().getResourceAsStream("/Personen-Import.xlsx")) {client.xlsx=in.readAllBytes();}
        JSONObject m=message("signature",config.recipient,subject("IMPORT PERSONEN"));
        m.getJSONObject("payload").put("parts",new JSONArray()
            .put(new JSONObject().put("parts",new JSONArray()
                .put(new JSONObject().put("filename","logo.png").put("mimeType","image/png"))
                .put(new JSONObject().put("filename","banner.jpg").put("mimeType","image/jpeg"))))
            .put(new JSONObject().put("filename","personen.xlsx")));
        client.messages.add(m);MailSync.sync(context,config,store,client);
        assertTrue(store.people().size()>0);assertEquals(Collections.singletonList("personen.xlsx"),client.fetched);
        assertTrue(mail.records(config.gmail).get(0)[2].contains("ergänzt"));
        MailSync.sync(context,config,store,client);assertEquals(1,client.sent.size());assertEquals(1,client.fetched.size());
    }
    @Test public void imagesDoNotRelaxExcelCountOrAllowOtherAttachments() throws Exception {
        for(String extra:Arrays.asList("zweite.xlsx","dokument.pdf","logo.png")) {
            FakeClient client=new FakeClient();JSONObject m=message(extra,config.recipient,subject("IMPORT PERSONEN"));
            m.getJSONObject("payload").put("parts",new JSONArray()
                .put(new JSONObject().put("filename","personen.xlsx"))
                .put(new JSONObject().put("filename",extra).put("mimeType",extra.endsWith("xlsx") ? "image/png" : "application/octet-stream")));
            client.messages.add(m);MailSync.sync(context,config,store,client);
            assertTrue(client.fetched.isEmpty());assertTrue(store.people().isEmpty());
            assertEquals(1,client.sent.size());
        }
    }
    @Test public void signatureImagesWithoutExcelAreRejected() throws Exception {
        FakeClient client=new FakeClient();JSONObject m=message("images-only",config.recipient,subject("IMPORT PERSONEN"));
        m.getJSONObject("payload").put("parts",new JSONArray().put(new JSONObject().put("filename","logo.png").put("mimeType","image/png")));
        client.messages.add(m);MailSync.sync(context,config,store,client);
        assertTrue(client.fetched.isEmpty());assertTrue(store.people().isEmpty());assertTrue(mail.records(config.gmail).get(0)[2].contains("abgelehnt"));
    }
    @Test public void versionTwoDatabaseMigratesWithoutLosingMovements() {
        movement("1");store.getWritableDatabase().execSQL("DROP TABLE mail_commands");store.getWritableDatabase().setVersion(2);store.close();
        store=new Store(context);mail=new MailStore(store);assertEquals(1,store.count(false));assertEquals("A",store.material("001").location);
        mail.queue("migration-test","file",0,"test");assertTrue(mail.exists("migration-test"));
    }
    @Test public void existingConnectionAndCutoffSurviveRemovalOfCommandKey() {
        MailConfig.prefs(context).edit().putBoolean("connected",true).putString("secret","old-unused-key").commit();
        long since=config.since;
        MailConfig.save(context,config.gmail,config.recipient,true);
        MailConfig updated=new MailConfig(context);
        assertTrue(updated.ready());assertEquals(since,updated.since);
    }
    @Test public void automaticMailExportArchivesOnlySnapshotAfterConfirmedSend() throws Exception {
        movement("1");FakeClient client=new FakeClient();
        MailSync.prepare(context,config,store,client,message("auto",config.recipient,subject("EXPORT")),true);
        assertEquals(1,store.count(false));movement("2");
        MailSync.sync(context,config,store,client);
        assertEquals(1,client.sent.size());assertEquals(1,store.count(false));assertEquals(1,store.count(true));
        assertEquals("archived",mail.records(config.gmail).get(0)[1]);
        MailSync.sync(context,config,store,client);assertEquals(1,client.sent.size());
    }
    @Test public void ambiguousAutomaticExportDoesNotArchive() throws Exception {
        movement("1");FakeClient client=new FakeClient();client.fail=true;
        MailSync.prepare(context,config,store,client,message("auto-fail",config.recipient,subject("EXPORT")),true);
        assertThrows(IOException.class,() -> MailSync.sync(context,config,store,client));
        assertEquals(1,store.count(false));assertEquals(0,store.count(true));
        client.fail=false;MailSync.sync(context,config,store,client);assertEquals(1,client.sent.size());assertEquals(1,store.count(false));
    }
    @Test public void automaticArchiveRecoversAfterSendWithoutResending() throws Exception {
        movement("1");FakeClient client=new FakeClient();
        MailSync.prepare(context,config,store,client,message("recover",config.recipient,subject("EXPORT")),true);
        mail.state(config.gmail+":recover","sent");store.close();store=new Store(context);mail=new MailStore(store);
        MailSync.sync(context,config,store,client);assertTrue(client.sent.isEmpty());assertEquals(1,store.count(true));
    }
    @Test public void versionThreeMigrationPreservesPendingCommandsAndMovements() {
        movement("1");SQLiteDatabase db=store.getWritableDatabase();db.execSQL("DROP TABLE mail_commands");
        db.execSQL("CREATE TABLE mail_commands (id TEXT PRIMARY KEY,state TEXT NOT NULL,reply TEXT NOT NULL,cutoff INTEGER NOT NULL DEFAULT 0,result TEXT NOT NULL,created INTEGER NOT NULL)");
        mail.queue("old","file",1,"old export");db.setVersion(3);store.close();store=new Store(context);mail=new MailStore(store);
        assertEquals(1,store.count(false));assertTrue(mail.exists("old"));mail.finishAutomaticArchives();assertEquals(1,store.count(false));
    }
    @Test public void receiverCannotInjectMailHeaders() {
        assertThrows(IllegalArgumentException.class,() -> MailProtocol.mime("a@example.com\r\nBcc: attacker@example.com","id","text",null));
    }
}
