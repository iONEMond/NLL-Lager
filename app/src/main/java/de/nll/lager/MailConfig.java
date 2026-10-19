package de.nll.lager;

import android.accounts.Account;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.common.api.Scope;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/** No passwords or OAuth tokens are stored by the app. Google Play services manages grants. */
final class MailConfig {
    static final String READ="https://www.googleapis.com/auth/gmail.readonly", SEND="https://www.googleapis.com/auth/gmail.send";
    final String gmail, recipient, secret;
    final boolean connected, automatic;
    final long since;
    MailConfig(Context context) {
        SharedPreferences p=prefs(context);
        gmail=p.getString("gmail",""); recipient=p.getString("recipient",""); secret=p.getString("secret","");
        connected=p.getBoolean("connected",false); automatic=p.getBoolean("automatic",true); since=p.getLong("since",0);
    }
    static SharedPreferences prefs(Context c) {return c.getSharedPreferences("mail",Context.MODE_PRIVATE);}
    static String email(String value) {
        String s=value.trim().toLowerCase(Locale.ROOT);
        if(s.length()>200 || !s.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9-]+(?:\\.[a-z0-9-]+)+")) throw new IllegalArgumentException("Bitte eine gültige Mailadresse eingeben.");
        return s;
    }
    static void save(Context c,String gmail,String recipient,boolean automatic) {
        gmail=email(gmail); recipient=email(recipient);
        if(!gmail.endsWith("@gmail.com")) throw new IllegalArgumentException("Bitte das separate @gmail.com-Konto verwenden.");
        if(gmail.equals(recipient)) throw new IllegalArgumentException("Scanner- und Firmenpostfach müssen verschieden sein.");
        MailConfig old=new MailConfig(c);
        SharedPreferences.Editor e=prefs(c).edit().putString("gmail",gmail).putString("recipient",recipient).putBoolean("automatic",automatic);
        if(!gmail.equals(old.gmail) || !recipient.equals(old.recipient) || old.secret.isEmpty()) {
            e.putString("secret",UUID.randomUUID().toString().replace("-", "")).putLong("since",System.currentTimeMillis()).putBoolean("connected",false);
        }
        if(!e.commit()) throw new IllegalStateException("Mail-Einstellungen konnten nicht gespeichert werden.");
    }
    boolean ready() {return connected && !gmail.isEmpty() && !recipient.isEmpty() && !secret.isEmpty();}
    AuthorizationRequest request() {
        return AuthorizationRequest.builder().setAccount(new Account(gmail,"com.google"))
            .setRequestedScopes(Arrays.asList(new Scope(READ),new Scope(SEND))).build();
    }
    static void status(Context c,String s) {prefs(c).edit().putString("status",s).putLong("last",System.currentTimeMillis()).apply();}
}
