package de.nll.lager;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

/** Uses isolated prefixed test storage, never the app's business database or Google account. */
@RunWith(AndroidJUnit4.class)
public class LocalExportDeviceTest {
    Context context;Store store;MailConfig config;
    @Before public void setup(){
        context=new android.content.ContextWrapper(InstrumentationRegistry.getInstrumentation().getTargetContext()){
            @Override public Context getApplicationContext(){return this;}
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("export-device-test-"+name,mode);}
            @Override public java.io.File getDatabasePath(String name){return super.getDatabasePath("export-device-test-"+name);}
            @Override public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name,int mode,android.database.sqlite.SQLiteDatabase.CursorFactory f){return getBaseContext().openOrCreateDatabase("export-device-test-"+name,mode,f);}
            @Override public android.database.sqlite.SQLiteDatabase openOrCreateDatabase(String name,int mode,android.database.sqlite.SQLiteDatabase.CursorFactory f,android.database.DatabaseErrorHandler h){return getBaseContext().openOrCreateDatabase("export-device-test-"+name,mode,f,h);}
            @Override public boolean deleteDatabase(String name){return getBaseContext().deleteDatabase("export-device-test-"+name);}
            @Override public java.io.File getFilesDir(){java.io.File dir=new java.io.File(super.getFilesDir(),"export-device-test");dir.mkdirs();return dir;}
        };
        context.deleteDatabase("nll-lager.db");MailConfig.prefs(context).edit().clear().commit();
        MailConfig.save(context,"scanner@gmail.com","receiver@example.com",false);
        MailConfig.prefs(context).edit().putLong("since",System.currentTimeMillis()+86400000).commit();config=new MailConfig(context);
        store=new Store(context);store.addPerson("P","Testperson");store.addMaterial("M","M1","Testmaterial","Material","Stk");
        store.book(store.material("M"),"A","B","1",store.person("P"),"Entnahme");
    }
    @After public void cleanup(){if(store!=null)store.close();context.deleteDatabase("nll-lager.db");MailConfig.prefs(context).edit().clear().commit();java.io.File dir=new java.io.File(context.getFilesDir(),"mail-outbox");java.io.File[] files=dir.listFiles();if(files!=null)for(java.io.File f:files)f.delete();}
    static class AcceptedClient extends GmailClient {
        final List<String> messages=new ArrayList<>();AcceptedClient(){super("test-no-token");}
        @Override JSONObject call(String path,JSONObject body)throws Exception{return new JSONObject().put("messages",new JSONArray());}
        @Override void send(String mime){messages.add(mime);}
    }
    @Test public void localExportIsQueuedAndAcceptedWithFutureInboxCutoff()throws Exception{
        MailStore mail=new MailStore(store);String first=MailSync.prepareLocalExport(context,config,store,"local-"+UUID.randomUUID(),true,WorkProgress.NONE);
        assertTrue(mail.exists(first));assertEquals("pending",mail.records(config.gmail).get(0)[1]);assertEquals(1,store.count(false));
        store.book(store.material("M"),"A","B","2",store.person("P"),"Entnahme");
        AcceptedClient client=new AcceptedClient();MailSync.sync(context,config,store,client);
        assertEquals(1,client.messages.size());assertTrue(client.messages.get(0).contains("NLL-Warenbewegungen.xlsx"));assertEquals(1,store.count(false));assertEquals(1,store.count(true));assertEquals("archived",mail.records(config.gmail).get(0)[1]);
        String second=MailSync.prepareLocalExport(context,config,store,"local-"+UUID.randomUUID(),false,WorkProgress.NONE);assertNotEquals(first,second);assertEquals(2,mail.records(config.gmail).size());
        MailSync.sync(context,config,store,client);assertEquals(2,client.messages.size());assertEquals(1,store.count(false));
    }
    @Test public void realIncomingMailStillRespectsReceiptCutoff()throws Exception{
        JSONObject message=new JSONObject().put("id","old-incoming").put("internalDate",System.currentTimeMillis()).put("payload",new JSONObject().put("headers",new JSONArray().put(new JSONObject().put("name","From").put("value",config.recipient)).put(new JSONObject().put("name","Subject").put("value","NLL Export"))));
        MailSync.prepare(context,config,store,new AcceptedClient(),message);assertTrue(new MailStore(store).records(config.gmail).isEmpty());assertEquals(1,store.count(false));
    }
}
