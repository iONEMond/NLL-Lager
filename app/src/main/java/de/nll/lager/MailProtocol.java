package de.nll.lager;

import org.json.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Strict command grammar; the random command key is independent of the displayed sender. */
final class MailProtocol {
    static String header(JSONObject payload,String name) throws JSONException {
        JSONArray headers=payload.optJSONArray("headers");String value="";int matches=0;
        if(headers!=null) for(int i=0;i<headers.length();i++) {
            JSONObject h=headers.getJSONObject(i);
            if(name.equalsIgnoreCase(h.optString("name"))) {value=h.optString("value");matches++;}
        }
        return matches==1 ? value : "";
    }
    static String sender(String value) {
        String s=value.trim();int left=s.lastIndexOf('<');
        if(left>=0 && s.endsWith(">")) s=s.substring(left+1,s.length()-1);
        try {return MailConfig.email(s);} catch(IllegalArgumentException e) {return "";}
    }
    static String command(String subject,String secret) {
        String prefix="NLL "+secret+" ";
        if(secret.length()<32 || !subject.startsWith(prefix)) return "";
        String cmd=subject.substring(prefix.length()).trim();
        return Arrays.asList("EXPORT","IMPORT MATERIAL","IMPORT PERSONEN").contains(cmd) ? cmd : "";
    }
    static void attachments(JSONObject payload,List<JSONObject> out) throws JSONException {attachments(payload,out,0);}
    private static void attachments(JSONObject payload,List<JSONObject> out,int depth) throws JSONException {
        if(depth>12) throw new JSONException("Mail zu stark verschachtelt.");
        String filename=payload.optString("filename").toLowerCase(Locale.ROOT);
        // Corporate signatures may add inline or attached images. Never fetch them;
        // Excel filenames must still count even if their MIME type is mislabelled.
        boolean signatureImage=payload.optString("mimeType").toLowerCase(Locale.ROOT).startsWith("image/") && !filename.endsWith(".xlsx");
        if(!filename.isEmpty() && !signatureImage) out.add(payload);
        JSONArray parts=payload.optJSONArray("parts");
        if(parts!=null) for(int i=0;i<parts.length();i++) attachments(parts.getJSONObject(i),out,depth+1);
    }
    static String mime(String recipient,String request,String text,byte[] xlsx) throws IOException {
        MailConfig.email(recipient);
        if(recipient.contains("\r") || recipient.contains("\n")) throw new IOException("Ungültiger Empfänger.");
        String boundary="nll-"+UUID.randomUUID();
        String headers="To: "+recipient+"\r\nSubject: NLL Lager - Auftragsergebnis\r\nMessage-ID: <"+UUID.nameUUIDFromBytes(request.getBytes(StandardCharsets.UTF_8))+"@nll-lager.invalid>\r\nMIME-Version: 1.0\r\n";
        String body="Content-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n"+encoded(text.getBytes(StandardCharsets.UTF_8))+"\r\n";
        if(xlsx==null) return headers+body;
        return headers+"Content-Type: multipart/mixed; boundary=\""+boundary+"\"\r\n\r\n--"+boundary+"\r\n"+body+
            "--"+boundary+"\r\nContent-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\nContent-Disposition: attachment; filename=\"NLL-Warenbewegungen.xlsx\"\r\nContent-Transfer-Encoding: base64\r\n\r\n"+encoded(xlsx)+"\r\n--"+boundary+"--\r\n";
    }
    private static String encoded(byte[] bytes) {return Base64.getMimeEncoder(76,new byte[]{13,10}).encodeToString(bytes);}
}
