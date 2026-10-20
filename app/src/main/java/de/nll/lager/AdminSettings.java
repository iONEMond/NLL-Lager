package de.nll.lager;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONArray;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Device-local settings. Unlock state is deliberately never persisted. */
public final class AdminSettings {
    private final SharedPreferences prefs;
    private final Context app;
    public AdminSettings(Context context) { app=context.getApplicationContext();prefs=context.getSharedPreferences("admin-settings",Context.MODE_PRIVATE); }
    public boolean hasPassword() { return prefs.contains("passwordHash"); }
    public void setPassword(char[] password) {
        if(password.length<8) throw new IllegalArgumentException("Mindestens 8 Zeichen verwenden.");
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt);
        String hash=Base64.encodeToString(derive(password,salt),Base64.NO_WRAP);
        if(!prefs.edit().putString("passwordSalt",Base64.encodeToString(salt,Base64.NO_WRAP)).putString("passwordHash",hash).commit())
            throw new IllegalStateException("Passwort konnte nicht gespeichert werden.");
    }
    public boolean verify(char[] password) {
        if(!hasPassword()) return false;
        try {
            byte[] salt=Base64.decode(prefs.getString("passwordSalt",""),Base64.NO_WRAP);
            byte[] expected=Base64.decode(prefs.getString("passwordHash",""),Base64.NO_WRAP);
            return MessageDigest.isEqual(expected,derive(password,salt));
        } catch(IllegalArgumentException e) { return false; }
    }
    private byte[] derive(char[] password,byte[] salt) {
        PBEKeySpec spec=new PBEKeySpec(password,salt,120000,256);
        try {return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
        catch(Exception e) {throw new IllegalStateException("Passwortprüfung nicht verfügbar.",e);}
        finally {spec.clearPassword();}
    }
    public List<String> locations() {
        List<String> result=new ArrayList<>();
        try {String source=prefs.getString("locations","[]");
            try(Store store=new Store(app);android.database.Cursor c=store.getReadableDatabase().rawQuery("SELECT value FROM app_settings WHERE key='locations'",null)) {if(c.moveToFirst()) source=c.getString(0);}
            JSONArray data=new JSONArray(source); for(int i=0;i<data.length();i++) result.add(data.getString(i));}
        catch(org.json.JSONException e) {throw new IllegalStateException("Lagerliste konnte nicht gelesen werden.",e);}
        return result;
    }
    public void saveLocations(List<String> values) {
        List<String> checked=new ArrayList<>();
        for(String value:values) {
            String name=Rules.required(value,"Lagername");
            for(String other:checked) if(other.equalsIgnoreCase(name)) throw new IllegalArgumentException("Dieses Lager existiert bereits.");
            checked.add(name);
        }
        try(Store store=new Store(app)) {android.content.ContentValues v=new android.content.ContentValues();v.put("key","locations");v.put("value",new JSONArray(checked).toString());store.getWritableDatabase().insertWithOnConflict("app_settings",null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);}
        Backup.request(app);
    }
}
