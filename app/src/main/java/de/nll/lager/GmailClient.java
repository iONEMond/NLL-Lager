package de.nll.lager;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** HTTPS Gmail REST client. Tokens never occur in URLs, exceptions or logs. */
class GmailClient {
    private final String token;
    private final WorkProgress progress;
    GmailClient(String token) {this(token,WorkProgress.NONE);}
    GmailClient(String token,WorkProgress progress) {this.token=token;this.progress=progress;}
    JSONObject call(String path,JSONObject payload) throws Exception {
        progress.checkCancelled();
        HttpURLConnection c=(HttpURLConnection)new URL("https://gmail.googleapis.com/gmail/v1/users/me/"+path).openConnection();
        c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("Authorization","Bearer "+token);
        try {
            progress.onCancel(c::disconnect);
            if(payload!=null) {
                c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                try(OutputStream out=c.getOutputStream()) {out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
            }
            int code=c.getResponseCode();
            if(code<200 || code>=300) throw new IOException(code==401 || code==403 ? "Gmail-Zugriff verweigert ("+code+"). Verbindung im geschützten Menü prüfen." : "Gmail-Anfrage fehlgeschlagen (HTTP "+code+").");
            try(InputStream in=c.getInputStream()) {return new JSONObject(new String(read(in,32*1024*1024),StandardCharsets.UTF_8));}
        } finally {c.disconnect();}
    }
    static byte[] read(InputStream in,int max) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1) {if(out.size()+n>max) throw new IOException("Datei oder Antwort ist zu groß.");out.write(buffer,0,n);}return out.toByteArray();
    }
    static String query(String value) throws UnsupportedEncodingException {return URLEncoder.encode(value,"UTF-8");}
    JSONObject message(String id) throws Exception {return call("messages/"+query(id)+"?format=full",null);}
    byte[] attachment(String id,JSONObject part) throws Exception {
        JSONObject body=part.getJSONObject("body");
        if(body.optInt("size",0)>20*1024*1024) throw new IOException("Excel-Anhang ist zu groß (maximal 20 MB).");
        if(body.has("attachmentId")) body=call("messages/"+query(id)+"/attachments/"+query(body.getString("attachmentId")),null);
        byte[] bytes=Base64.getUrlDecoder().decode(body.optString("data",""));
        if(bytes.length>20*1024*1024) throw new IOException("Excel-Anhang ist zu groß.");return bytes;
    }
    void send(String mime) throws Exception {
        JSONObject response=call("messages/send",new JSONObject().put("raw",Base64.getUrlEncoder().withoutPadding().encodeToString(mime.getBytes(StandardCharsets.UTF_8))));
        if(response.optString("id").isEmpty()) throw new IOException("Gmail hat den Versand nicht bestätigt.");
    }
}
