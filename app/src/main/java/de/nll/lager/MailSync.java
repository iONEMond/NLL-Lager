package de.nll.lager;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.tasks.Tasks;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class MailSync {
    static final AtomicBoolean BUSY=new AtomicBoolean();
    private static volatile OperationControl active;
    static String currentPhase(){OperationControl c=active;return c==null ? "Hintergrundverarbeitung läuft …" : c.isCancelled() ? "Abbruch angefordert · Abschluss abwarten …" : c.phase();}
    static boolean canCancel(){return active!=null;}
    static boolean cancelActive(){OperationControl c=active;return c!=null && c.cancel();}
    /** Never releases BUSY early: cancellation must first unwind network and transactions. */
    static final class RunGuard implements AutoCloseable {
        final OperationControl control;
        final Timer timer;
        RunGuard(WorkProgress progress,long timeoutMillis){
            control=progress instanceof OperationProgress ? ((OperationProgress)progress).control : progress instanceof OperationControl ? (OperationControl)progress : null;
            active=control;
            if(control!=null && progress instanceof OperationControl){timer=new Timer("background-mail-timeout",true);timer.schedule(new TimerTask(){@Override public void run(){control.timeout();}},timeoutMillis);}else timer=null;
        }
        @Override public void close(){if(timer!=null)timer.cancel();if(active==control)active=null;}
    }
    static boolean run(Context context) {return run(context,new OperationControl());}
    static boolean run(Context context,WorkProgress progress) {
        if(progress==WorkProgress.NONE)progress=new OperationControl();
        if(!BUSY.compareAndSet(false,true)) return false;
        try{return runOwned(context,progress);}finally{BUSY.set(false);Backup.request(context);}
    }
    /** Caller holds BUSY throughout preparation, cancellation and transmission. */
    static boolean runOwned(Context context,WorkProgress progress) {
        MailConfig config=new MailConfig(context);
        try(RunGuard guard=new RunGuard(progress,120000);Store store=new Store(context)) {
            progress.update(0,0,"Google-Verbindung herstellen …");
            if(!config.ready()) return false;
            MailStore mail=new MailStore(store);mail.recoverInterrupted();
            AuthorizationResult auth=Tasks.await(Identity.getAuthorizationClient(context).authorize(config.request()),45,TimeUnit.SECONDS);
            if(auth.hasResolution() || auth.getAccessToken()==null || !auth.getGrantedScopes().containsAll(Arrays.asList(MailConfig.READ,MailConfig.SEND))) throw new IOException("Gmail-Freigabe erforderlich. Im geschützten Mail-Menü erneut verbinden.");
            progress.update(0,0,"Google-Freigabe erhalten · Gmail-Postfach prüfen …");
            GmailClient client=new GmailClient(auth.getAccessToken(),progress);
            if(!config.gmail.equalsIgnoreCase(client.call("profile",null).getString("emailAddress"))) throw new IOException("Falsches Google-Konto ausgewählt.");
            sync(context,config,store,client,progress);
            MailConfig.status(context,"Synchronisierung abgeschlossen. Auftragsstatus unten prüfen.");return true;
        } catch(Exception e) {
            boolean timeout=progress instanceof OperationControl && ((OperationControl)progress).timedOut;
            MailConfig.status(context,timeout ? "Mail-Zeitlimit von 2 Minuten erreicht. Bereits abgeschlossene Aufträge bleiben erhalten; Versandstatus prüfen." : e instanceof OperationControl.Cancelled || Thread.currentThread().isInterrupted() ? "Synchronisierung abgebrochen. Bereits abgeschlossene Aufträge bleiben erhalten; Mailstatus prüfen." : e instanceof IOException ? e.getMessage() : "Gmail-Verbindung fehlgeschlagen. Internet, Google-Einrichtung und Kontofreigabe prüfen.");return false;
        } finally {Thread.interrupted();}
    }
    static void sync(Context context,MailConfig config,Store store,GmailClient client) throws Exception {sync(context,config,store,client,WorkProgress.NONE);}
    static void sync(Context context,MailConfig config,Store store,GmailClient client,WorkProgress progress) throws Exception {
        MailStore mail=new MailStore(store);mail.finishAutomaticArchives();sendPending(context,config,mail,client,progress);
        List<JSONObject> messages=new ArrayList<>();String page="";
        // Bounded pagination. Process oldest first so successive catalog updates keep their order.
        for(int batch=0;batch<10;batch++) {
            progress.update(0,0,"Posteingang prüfen · Seite "+(batch+1)+" …");
            JSONObject response=client.call("messages?maxResults=100&q="+GmailClient.query("in:inbox after:"+(config.since/1000)+" from:"+config.recipient+" subject:NLL")+(page.isEmpty() ? "" : "&pageToken="+GmailClient.query(page)),null);
            JSONArray list=response.optJSONArray("messages");
            if(list!=null) for(int i=0;i<list.length();i++) {
                String id=list.getJSONObject(i).getString("id");
                if(!mail.exists(config.gmail+":"+id)) messages.add(client.message(id));
            }
            page=response.optString("nextPageToken");if(page.isEmpty()) break;
            if(batch==9) throw new IOException("Zu viele passende Mails. Ältere Aufträge im Gmail-Postfach aus dem Posteingang verschieben.");
        }
        messages.sort(Comparator.comparingLong(m -> m.optLong("internalDate",0)));
        int processed=0;
        for(JSONObject message:messages) {
            progress.update(processed,messages.size(),"Mailaufträge verarbeiten …");
            if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
            prepare(context,config,store,client,message,false,progress);sendPending(context,config,mail,client,progress);
            progress.update(++processed,messages.size(),"Mailaufträge verarbeiten …");
        }
    }
    static void prepare(Context context,MailConfig config,Store store,GmailClient client,JSONObject message) throws Exception {
        prepare(context,config,store,client,message,false);
    }
    static void prepare(Context context,MailConfig config,Store store,GmailClient client,JSONObject message,boolean autoArchive) throws Exception {prepare(context,config,store,client,message,autoArchive,WorkProgress.NONE);}
    static void prepare(Context context,MailConfig config,Store store,GmailClient client,JSONObject message,boolean autoArchive,WorkProgress progress) throws Exception {
        prepare(context,config,store,client,message,autoArchive,progress,false);
    }
    /** Local UI exports are new explicit commands, independent of the inbox receipt cutoff. */
    static String prepareLocalExport(Context context,MailConfig config,Store store,String id,boolean autoArchive,WorkProgress progress)throws Exception {
        if(!id.matches("local-[0-9a-f-]{36}"))throw new IOException("Ungültige lokale Exportkennung.");
        progress.update(0,0,"Neuen Exportauftrag erstellen …");
        JSONObject payload=new JSONObject().put("headers",new JSONArray()
            .put(new JSONObject().put("name","From").put("value",config.recipient))
            .put(new JSONObject().put("name","Subject").put("value","NLL Export")));
        prepare(context,config,store,null,new JSONObject().put("id",id).put("internalDate",System.currentTimeMillis()).put("payload",payload),autoArchive,progress,true);
        String key=config.gmail+":"+id;
        if(!new MailStore(store).exists(key))throw new IOException("Neuer Exportauftrag wurde nicht gespeichert. Es wurde keine Mail versendet.");
        return key;
    }
    private static void prepare(Context context,MailConfig config,Store store,GmailClient client,JSONObject message,boolean autoArchive,WorkProgress progress,boolean localExport) throws Exception {
        String id=message.getString("id"),key=config.gmail+":"+id;MailStore mail=new MailStore(store);
        if(mail.exists(key)){if(localExport)throw new IOException("Exportauftrag mit dieser Kennung existiert bereits.");return;}
        if(!localExport && message.optLong("internalDate",0)<config.since)return;
        JSONObject payload=message.getJSONObject("payload");
        if(!config.recipient.equals(MailProtocol.sender(MailProtocol.header(payload,"From")))) return;
        String command=MailProtocol.command(MailProtocol.header(payload,"Subject"));
        if(command.isEmpty()) return; // No replies to unknown subjects.
        List<String[]> rows=null;long cutoff=0;byte[] attachment=null;String result;
        try {
            if(command.startsWith("IMPORT")) {
                progress.update(0,0,"Importanhang laden und prüfen …");
                List<JSONObject> parts=new ArrayList<>();MailProtocol.attachments(payload,parts);
                if(parts.size()!=1 || !parts.get(0).optString("filename").toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw new IOException("Genau einen .xlsx-Anhang senden. Signaturbilder werden ignoriert; andere zusätzliche Dateien sind nicht erlaubt.");
                File temp=File.createTempFile("mail-import-",".xlsx",context.getCacheDir());
                try {
                    try(OutputStream out=new FileOutputStream(temp)) {out.write(client.attachment(id,parts.get(0)));}
                    try {rows=ExcelImport.read(temp,command.endsWith("PERSONEN"),progress);}
                    catch(OperationControl.Cancelled e) {throw e;}
                    catch(Exception e) {throw new IOException("Excel-Datei ungültig: "+e.getMessage());}
                } finally {if(!temp.delete()) temp.deleteOnExit();}
            }
            SQLiteDatabase db=store.getWritableDatabase();db.beginTransaction();
            try {
                if(mail.exists(key)) return;
                if(rows!=null) {
                    int[] count=mail.upsert(rows,command.endsWith("PERSONEN"),progress);
                    result=command+": "+count[0]+" ergänzt, "+count[1]+" aktualisiert. Fehlende Einträge bleiben erhalten.";
                } else {
                    cutoff=store.cutoff();
                    try(ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                        OutputStream bounded=new FilterOutputStream(out) {
                            int size;
                            @Override public void write(int b) throws IOException {if(++size>12*1024*1024) throw new IOException("Export überschreitet das Mail-Limit (12 MB).");out.write(b);}
                            @Override public void write(byte[] b,int offset,int length) throws IOException {if((size+=length)>12*1024*1024) throw new IOException("Export überschreitet das Mail-Limit (12 MB).");out.write(b,offset,length);}
                        };
                        int count=store.writeXlsx(bounded,cutoff,false,progress);attachment=out.toByteArray();
                        result="EXPORT: "+count+" offene Bewegungen. Auftrag "+id+". "+(autoArchive ? "Automatische Archivierung nach bestätigtem Versand." : "Keine Archivierung beim Versand; nach Übernahme separat am Scanner bestätigen.");
                    }
                }
                mail.queue(key,saveReply(context,key,MailProtocol.mime(config.recipient,key,result,attachment)),cutoff,result);
                if(autoArchive) mail.archiveAfterSend(key);
                progress.checkCancelled();db.setTransactionSuccessful();
            } finally {db.endTransaction();}
        } catch(IOException | IllegalArgumentException e) {
            progress.checkCancelled();
            if(localExport)throw e; // The UI must not report a queued error reply as a successful report.
            // A failed transaction leaves the catalog intact and creates a durable error reply.
            if(!mail.exists(key)) {
                String reason=e.getMessage();if(reason==null) reason="Datei konnte nicht verarbeitet werden.";
                if(reason.length()>500) reason=reason.substring(0,500);
                result="Auftrag "+id+" abgelehnt: "+reason+" Es wurden keine Daten übernommen oder archiviert.";
                mail.queue(key,saveReply(context,key,MailProtocol.mime(config.recipient,key,result,null)),0,result);
            }
        }
    }
    private static String saveReply(Context context,String key,String mime) throws IOException {
        File directory=new File(context.getFilesDir(),"mail-outbox");
        if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Mail-Ausgang konnte nicht angelegt werden.");
        String name=UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8))+".eml";
        File file=new File(directory,name),temp=new File(directory,name+".tmp");
        try(FileOutputStream out=new FileOutputStream(temp)) {out.write(mime.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}
        if(!temp.renameTo(file)) throw new IOException("Mail-Ausgang konnte nicht gesichert werden.");return name;
    }
    private static File replyFile(Context context,String name) throws IOException {
        if(!name.matches("[0-9a-f-]{36}\\.eml")) throw new IOException("Ungültiger Mail-Ausgang.");
        return new File(new File(context.getFilesDir(),"mail-outbox"),name);
    }
    private static void sendPending(Context context,MailConfig config,MailStore mail,GmailClient client,WorkProgress progress) throws Exception {
        List<String[]> replies=new ArrayList<>();
        try(Cursor c=mail.store.getReadableDatabase().rawQuery("SELECT id,reply FROM mail_commands WHERE state='pending' AND substr(id,1,?)=? ORDER BY created LIMIT 30",new String[]{Integer.toString(config.gmail.length()+1),config.gmail+":"})) {
            while(c.moveToNext()) replies.add(new String[]{c.getString(0),c.getString(1)});
        }
        int sent=0;
        for(String[] r:replies) {
            progress.update(sent,replies.size(),"Berichte und Antworten senden …");
            File file=replyFile(context,r[1]);
            String mime;try(InputStream in=new FileInputStream(file)) {mime=new String(GmailClient.read(in,20*1024*1024),java.nio.charset.StandardCharsets.UTF_8);}
            if(!mime.startsWith("To: "+config.recipient+"\r\n")) continue;
            progress.checkCancelled();mail.state(r[0],"sending");
            try {client.send(mime);mail.state(r[0],"sent");}
            catch(Exception e) {mail.state(r[0],"uncertain");throw new IOException("Versandstatus unklar. Im Mail-Menü prüfen; keine automatische Wiederholung und keine Archivierung.");}
            mail.finishAutomaticArchives();progress.update(++sent,replies.size(),"Berichte und Antworten senden …");
        }
    }
}
