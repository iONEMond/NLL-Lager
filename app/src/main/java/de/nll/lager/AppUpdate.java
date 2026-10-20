package de.nll.lager;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import com.android.apksig.ApkVerifier;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Anonymous GitHub releases, bounded HTTPS downloads and cryptographically verified APKs. */
final class AppUpdate {
    static final String PACKAGE="de.nll.lager", REPO="iONEMond/NLL-Lager";
    static final String CERT="1d70da7786708b6dcd04319235fc76ca4d411f12474a63bc98852fc9b5fa25c0";
    static final int MAX_APK=64*1024*1024;
    static final AtomicBoolean BUSY=new AtomicBoolean();
    interface Transport {HttpURLConnection open(URL url) throws IOException;}
    final Transport transport;
    AppUpdate() {this(url -> (HttpURLConnection)url.openConnection());}
    AppUpdate(Transport transport) {this.transport=transport;}
    static final class Release {
        final String version,notes,url,digest;final long size;
        Release(String version,String notes,String url,String digest,long size) {this.version=version;this.notes=notes;this.url=url;this.digest=digest;this.size=size;}
    }
    static int compare(String a,String b) throws IOException {
        int[] left=version(a),right=version(b);
        for(int i=0;i<3;i++) if(left[i]!=right[i]) return Integer.compare(left[i],right[i]);return 0;
    }
    static int[] version(String text) throws IOException {
        if(text==null || !text.matches("v?[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}(-dev)?")) throw new IOException("Versionsnummer wird nicht unterstützt.");
        String[] parts=text.replaceFirst("^v","").replaceFirst("-dev$","").split("\\.");
        return new int[]{Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2])};
    }
    static Release parse(JSONObject json) throws Exception {
        if(json.optBoolean("draft") || json.optBoolean("prerelease")) throw new IOException("Keine freigegebene stabile Version.");
        String tag=json.getString("tag_name");version(tag);
        if(!tag.matches("v?[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")) throw new IOException("Keine stabile Release-Version.");
        String version=tag.replaceFirst("^v","");String wanted="NLL-Lager-"+version+"-release.apk";
        JSONArray assets=json.getJSONArray("assets");JSONObject match=null;
        for(int i=0;i<assets.length();i++) if(wanted.equals(assets.getJSONObject(i).optString("name"))) {
            if(match!=null) throw new IOException("Mehrere passende APKs im Release.");match=assets.getJSONObject(i);
        }
        if(match==null) throw new IOException("Im Release fehlt die signierte APK.");
        long size=match.getLong("size");if(size<=0 || size>MAX_APK) throw new IOException("APK-Dateigröße nicht zulässig.");
        String digest=match.optString("digest");if(!digest.matches("sha256:[0-9a-f]{64}")) throw new IOException("Im Release fehlt die SHA-256-Prüfsumme.");
        URL url=new URL(match.getString("browser_download_url"));checkUrl(url);
        if(!url.getHost().equals("github.com") || !url.getPath().startsWith("/"+REPO+"/releases/download/") || !url.getPath().endsWith("/"+wanted)) throw new IOException("APK stammt nicht aus dem vorgesehenen Repository.");
        String notes=json.optString("body","");if(notes.length()>8000) notes=notes.substring(0,8000)+"…";
        return new Release(version,notes,url.toString(),digest.substring(7),size);
    }
    Release check() throws Exception {
        HttpURLConnection connection=connect(new URL("https://api.github.com/repos/"+REPO+"/releases/latest"));
        try(InputStream in=connection.getInputStream()) {
            return parse(new JSONObject(new String(read(in,1024*1024),StandardCharsets.UTF_8)));
        } finally {connection.disconnect();}
    }
    static void checkUrl(URL url) throws IOException {
        if(!url.getProtocol().equals("https") || url.getUserInfo()!=null || (url.getPort()!=-1 && url.getPort()!=443) || url.getRef()!=null ||
            !Arrays.asList("api.github.com","github.com","release-assets.githubusercontent.com","objects.githubusercontent.com","github-releases.githubusercontent.com").contains(url.getHost())) throw new IOException("Unsichere oder unbekannte Update-Adresse.");
    }
    HttpURLConnection connect(URL url) throws Exception {
        for(int redirects=0;redirects<=5;redirects++) {
            checkUrl(url);HttpURLConnection c=transport.open(url);
            try {
                c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);
                c.setRequestProperty("User-Agent","NLL-Lager-Updater");c.setRequestProperty("Accept",url.getHost().equals("api.github.com") ? "application/vnd.github+json" : "application/octet-stream");
                int status=c.getResponseCode();
                if(status==200) return c;
                if(Arrays.asList(301,302,303,307,308).contains(status)) {
                    String next=c.getHeaderField("Location");if(next==null) throw new IOException("Ungültige Download-Weiterleitung.");url=new URL(url,next);c.disconnect();continue;
                }
                throw new IOException(status==403 || status==429 ? "GitHub begrenzt die Abrufe. Bitte später erneut versuchen." : status==404 ? "Kein öffentlich zugänglicher Release gefunden." : "GitHub-Anfrage fehlgeschlagen ("+status+").");
            } catch(Exception e) {c.disconnect();throw e;}
        }
        throw new IOException("Zu viele Download-Weiterleitungen.");
    }
    static byte[] read(InputStream in,int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1) {if(out.size()+n>limit) throw new IOException("Update-Antwort zu groß.");out.write(buffer,0,n);}return out.toByteArray();
    }
    static long code(PackageInfo info) {return Build.VERSION.SDK_INT>=28 ? info.getLongVersionCode() : info.versionCode;}
    static PackageInfo installed(Context c) throws Exception {
        return c.getPackageManager().getPackageInfo(c.getPackageName(),Build.VERSION.SDK_INT>=28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES);
    }
    static void trusted(byte[] certificate) throws Exception {
        if(!CERT.equals(BackupData.digest(certificate))) throw new IOException("APK oder installierte App hat nicht unseren Release-Schlüssel.");
    }
    static void verify(File apk,Release release,PackageManager pm,PackageInfo current) throws Exception {
        if(!PACKAGE.equals(current.packageName)) throw new IOException("Updates sind nur in der signierten Haupt-App verfügbar, nicht in der Vorschau.");
        Signature[] signatures=Build.VERSION.SDK_INT>=28 && current.signingInfo!=null ? current.signingInfo.getApkContentsSigners() : current.signatures;
        if(signatures==null || signatures.length!=1) throw new IOException("Installierte App-Signatur konnte nicht geprüft werden.");trusted(signatures[0].toByteArray());
        if(apk.length()!=release.size) throw new IOException("APK unvollständig heruntergeladen.");
        java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int n;
        try(InputStream in=new FileInputStream(apk)) {while((n=in.read(buffer))!=-1) hash.update(buffer,0,n);}
        StringBuilder digest=new StringBuilder();for(byte b:hash.digest()) digest.append(String.format(Locale.ROOT,"%02x",b&255));
        if(!digest.toString().equals(release.digest)) throw new IOException("APK-Prüfsumme stimmt nicht.");
        ApkVerifier.Result verified=new ApkVerifier.Builder(apk).setMinCheckedPlatformVersion(26).build().verify();
        if(!verified.isVerified() || verified.getSignerCertificates().size()!=1) throw new IOException("APK-Signatur ist ungültig.");trusted(verified.getSignerCertificates().get(0).getEncoded());
        PackageInfo candidate=pm.getPackageArchiveInfo(apk.getAbsolutePath(),0);
        if(candidate==null || !PACKAGE.equals(candidate.packageName)) throw new IOException("APK gehört nicht zur Lager-App.");
        if(code(candidate)<=code(current) || compare(candidate.versionName,current.versionName)<0) throw new IOException("APK ist kein neueres Update.");
        if(!release.version.equals(candidate.versionName)) throw new IOException("APK-Version passt nicht zum GitHub-Release.");
        if(candidate.applicationInfo==null || candidate.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT) throw new IOException("Dieses Update benötigt eine neuere Android-Version.");
    }
    File download(Context c,Release release) throws Exception {
        if(!BUSY.compareAndSet(false,true)) throw new IOException("Ein Update-Download läuft bereits.");
        File directory=new File(c.getFilesDir(),"updates"),temp=null;
        try {
            if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Updateordner konnte nicht erstellt werden.");
            temp=new File(directory,UUID.randomUUID()+".part");HttpURLConnection connection=connect(new URL(release.url));
            try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temp)) {
                if(connection.getContentLengthLong()>MAX_APK) throw new IOException("APK zu groß.");
                byte[] buffer=new byte[32768];long total=0,start=System.nanoTime();int n;
                while((n=in.read(buffer))!=-1) {
                    if((total+=n)>release.size || total>MAX_APK) throw new IOException("APK größer als angekündigt.");
                    if(System.nanoTime()-start>300_000_000_000L || Thread.currentThread().isInterrupted()) throw new IOException("Update-Download abgebrochen.");
                    out.write(buffer,0,n);
                }
                out.getFD().sync();
            } finally {connection.disconnect();}
            verify(temp,release,c.getPackageManager(),installed(c));
            File ready=new File(directory,UUID.randomUUID()+".apk");if(!temp.renameTo(ready)) throw new IOException("APK konnte nicht gespeichert werden.");return ready;
        } finally {if(temp!=null) temp.delete();BUSY.set(false);}
    }
    static void cleanup(Context c) {
        File[] files=new File(c.getFilesDir(),"updates").listFiles();if(files==null) return;
        long cutoff=System.currentTimeMillis()-24*60*60*1000L;
        for(File file:files) if(file.getName().matches("[0-9a-f-]{36}\\.(apk|part)") && file.lastModified()<cutoff) file.delete();
    }
    static void beforeInstall(Context c,File file,Release release) throws Exception {
        if(!MailSync.BUSY.compareAndSet(false,true)) throw new IOException("Mailverarbeitung läuft. Bitte kurz warten.");
        try {verify(file,release,c.getPackageManager(),installed(c));Backup.save(c,true);} finally {MailSync.BUSY.set(false);}
    }
}
