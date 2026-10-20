package de.nll.lager;

import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import com.android.apksig.ApkVerifier;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
public class AppUpdateTest {
    Context context;File fixture;PackageInfo current;byte[] apk;
    @Before public void setup() throws Exception {
        Context app=RuntimeEnvironment.getApplication();
        context=new ContextWrapper(app) {@Override public String getPackageName() {return AppUpdate.PACKAGE;}};
        Backup.prefs(context).edit().clear().commit();MailSync.BUSY.set(false);
        fixture=new File("../downloads/NLL-Lager-0.5.0-release.apk");assertTrue(fixture.exists());apk=Files.readAllBytes(fixture.toPath());
        current=info(AppUpdate.PACKAGE,"0.4.0",5);
        current.signatures=new Signature[]{new Signature(new ApkVerifier.Builder(fixture).build().verify().getSignerCertificates().get(0).getEncoded())};
        Shadows.shadowOf(context.getPackageManager()).installPackage(current);
        archive(fixture,AppUpdate.PACKAGE,"0.5.0",6);
    }
    PackageInfo info(String pkg,String version,int code) {
        PackageInfo p=new PackageInfo();p.packageName=pkg;p.versionName=version;p.versionCode=code;p.applicationInfo=new ApplicationInfo();p.applicationInfo.packageName=pkg;p.applicationInfo.minSdkVersion=26;return p;
    }
    void archive(File file,String pkg,String version,int code) {Shadows.shadowOf(context.getPackageManager()).setPackageArchiveInfo(file.getAbsolutePath(),info(pkg,version,code));}
    JSONObject metadata() throws Exception {
        return new JSONObject().put("tag_name","v0.5.0").put("draft",false).put("prerelease",false).put("body","Änderungen")
            .put("assets",new JSONArray().put(new JSONObject().put("name","NLL-Lager-0.5.0-release.apk").put("size",apk.length)
                .put("digest","sha256:"+BackupData.digest(apk)).put("browser_download_url","https://github.com/iONEMond/NLL-Lager/releases/download/v0.5.0/NLL-Lager-0.5.0-release.apk")));
    }
    @Test public void metadataAcceptsOnlyStableSingleApkWithDigestAndCorrectRepository() throws Exception {
        JSONObject json=metadata();AppUpdate.Release r=AppUpdate.parse(json);assertEquals("0.5.0",r.version);assertEquals(apk.length,r.size);
        json.put("draft",true);assertThrows(Exception.class,() -> AppUpdate.parse(json));json.put("draft",false).put("prerelease",true);assertThrows(Exception.class,() -> AppUpdate.parse(json));
        JSONObject duplicates=metadata();duplicates.getJSONArray("assets").put(duplicates.getJSONArray("assets").getJSONObject(0));assertThrows(Exception.class,() -> AppUpdate.parse(duplicates));
    }
    @Test public void missingDigestOversizeForeignUrlAndPrereleaseTagAreRejected() throws Exception {
        for(String field:Arrays.asList("digest","size","browser_download_url")) {
            JSONObject json=metadata();JSONObject asset=json.getJSONArray("assets").getJSONObject(0);
            asset.put(field,field.equals("digest") ? "" : field.equals("size") ? AppUpdate.MAX_APK+1L : "https://github.com/evil/repo/releases/download/v0.5.0/NLL-Lager-0.5.0-release.apk");
            assertThrows(Exception.class,() -> AppUpdate.parse(json));
        }
        JSONObject json=metadata().put("tag_name","v0.5.0-dev");assertThrows(Exception.class,() -> AppUpdate.parse(json));
    }
    @Test public void versionComparisonIsNumericAndRejectsUnexpectedFormats() throws Exception {
        assertTrue(AppUpdate.compare("0.10.0","0.9.9")>0);assertEquals(0,AppUpdate.compare("0.6.1","0.6.1-dev"));
        assertThrows(Exception.class,() -> AppUpdate.compare("latest","0.6.0"));assertTrue(AppUpdate.compare("0.5.0","0.6.0")<0);
    }
    @Test public void onlyHttpsGithubHostsCanReceiveRequestsAndRedirects() throws Exception {
        for(String url:Arrays.asList("http://github.com/a","https://github.com.evil/a","https://user:pass@github.com/a","https://github.com:444/a","https://evil.test/a")) assertThrows(Exception.class,() -> AppUpdate.checkUrl(new URL(url)));
        List<URL> opened=new ArrayList<>();AppUpdate update=new AppUpdate(url -> {opened.add(url);return new Connection(url,302,"https://evil.test/file",new byte[0]);});
        assertThrows(Exception.class,update::check);assertEquals(1,opened.size());
    }
    @Test public void anonymousMetadataRequestAndRedirectLimitsAreHandled() throws Exception {
        Connection c=new Connection(new URL("https://api.github.com"),200,null,metadata().toString().getBytes("UTF-8"));
        assertEquals("0.5.0",new AppUpdate(url -> c).check().version);assertNull(c.getRequestProperty("Authorization"));assertFalse(c.getInstanceFollowRedirects());
        AppUpdate loop=new AppUpdate(url -> new Connection(url,302,"/loop",new byte[0]));assertThrows(Exception.class,loop::check);
        AppUpdate rateLimit=new AppUpdate(url -> new Connection(url,429,null,new byte[0]));assertThrows(Exception.class,rateLimit::check);
    }
    @Test public void existingReleaseApkPassesFullCryptographicVerification() throws Exception {AppUpdate.verify(fixture,AppUpdate.parse(metadata()),context.getPackageManager(),current);}
    @Test public void checksumTamperingWrongSignerWrongPackageAndDowngradesAreRejected() throws Exception {
        AppUpdate.Release valid=AppUpdate.parse(metadata());
        AppUpdate.Release badHash=new AppUpdate.Release(valid.version,"",valid.url,String.join("",Collections.nCopies(64,"0")),valid.size);
        assertThrows(Exception.class,() -> AppUpdate.verify(fixture,badHash,context.getPackageManager(),current));
        current.signatures=new Signature[]{new Signature(new byte[]{1,2,3})};assertThrows(Exception.class,() -> AppUpdate.verify(fixture,valid,context.getPackageManager(),current));
        current.signatures=new Signature[]{new Signature(new ApkVerifier.Builder(fixture).build().verify().getSignerCertificates().get(0).getEncoded())};
        archive(fixture,"wrong.package","0.5.0",6);assertThrows(Exception.class,() -> AppUpdate.verify(fixture,valid,context.getPackageManager(),current));
        archive(fixture,AppUpdate.PACKAGE,"0.5.0",6);current.versionCode=6;assertThrows(Exception.class,() -> AppUpdate.verify(fixture,valid,context.getPackageManager(),current));
    }
    @Test public void modifiedSignedApkFailsEvenWithMatchingDownloadHash() throws Exception {
        byte[] changed=apk.clone();changed[500]^=1;File file=File.createTempFile("tampered-",".apk",context.getCacheDir());Files.write(file.toPath(),changed);archive(file,AppUpdate.PACKAGE,"0.5.0",6);
        try {AppUpdate.Release valid=AppUpdate.parse(metadata());AppUpdate.Release altered=new AppUpdate.Release(valid.version,"",valid.url,BackupData.digest(changed),changed.length);assertThrows(Exception.class,() -> AppUpdate.verify(file,altered,context.getPackageManager(),current));} finally {file.delete();}
    }
    @Test public void partialOversizedDownloadIsRemovedAndCanBeRetried() throws Exception {
        AppUpdate.Release valid=AppUpdate.parse(metadata());AppUpdate.Release tiny=new AppUpdate.Release(valid.version,"",valid.url,valid.digest,1);
        AppUpdate update=new AppUpdate(url -> new Connection(url,200,null,new byte[]{1,2}));
        assertThrows(Exception.class,() -> update.download(context,tiny));assertFalse(AppUpdate.BUSY.get());
        File[] pending=new File(context.getFilesDir(),"updates").listFiles((d,n) -> n.endsWith(".part"));assertEquals(0,pending.length);
    }
    @Test public void installPreparationRequiresBackupAndDoesNotRunWhileMailIsBusy() throws Exception {
        AppUpdate.Release valid=AppUpdate.parse(metadata());
        assertThrows(Exception.class,() -> AppUpdate.beforeInstall(context,fixture,valid));assertFalse(MailSync.BUSY.get());
        MailSync.BUSY.set(true);try {assertThrows(Exception.class,() -> AppUpdate.beforeInstall(context,fixture,valid));} finally {MailSync.BUSY.set(false);}
    }
    @Test public void providerIsReadOnlyAndCannotExposeDatabaseOrOtherPaths() throws Exception {
        UpdateFileProvider provider=new UpdateFileProvider();ProviderInfo info=new ProviderInfo();info.authority="de.nll.lager.updates";provider.attachInfo(context,info);
        File dir=new File(context.getFilesDir(),"updates");dir.mkdirs();String name=UUID.randomUUID()+".apk";File file=new File(dir,name);Files.write(file.toPath(),apk);
        Uri uri=Uri.parse("content://de.nll.lager.updates/"+name);
        try {
            assertThrows(FileNotFoundException.class,() -> provider.openFile(uri,"rw"));
            assertThrows(FileNotFoundException.class,() -> provider.openFile(Uri.parse("content://de.nll.lager.updates/../databases/nll-lager.db"),"r"));
            try(android.os.ParcelFileDescriptor fd=provider.openFile(uri,"r")) {assertEquals(apk.length,fd.getStatSize());}
        } finally {file.delete();}
    }
    @Test public void verifiedDownloadAndSuccessfulRescueBackupPrepareInstallation() throws Exception {
        AppUpdate.Release release=AppUpdate.parse(metadata());
        AppUpdate update=new AppUpdate(url -> new Connection(url,200,null,apk) {
            @Override public void disconnect() {
                File[] parts=new File(context.getFilesDir(),"updates").listFiles((dir,name) -> name.endsWith(".part"));
                assertEquals(1,parts.length);archive(parts[0],AppUpdate.PACKAGE,"0.5.0",6);
            }
        });
        File ready=update.download(context,release);assertTrue(ready.isFile());assertTrue(ready.getName().endsWith(".apk"));assertFalse(AppUpdate.BUSY.get());
        archive(ready,AppUpdate.PACKAGE,"0.5.0",6);
        BackupTest.Provider provider=new BackupTest.Provider();ProviderInfo info=new ProviderInfo();info.authority="update.backup.test";info.exported=true;info.grantUriPermissions=true;
        info.readPermission="android.permission.MANAGE_DOCUMENTS";info.writePermission="android.permission.MANAGE_DOCUMENTS";
        provider.attachInfo(context,info);org.robolectric.shadows.ShadowContentResolver.registerProviderInternal(info.authority,provider);
        Uri tree=android.provider.DocumentsContract.buildTreeDocumentUri(info.authority,"root");Backup.prefs(context).edit().putString("tree",tree.toString()).commit();
        try {AppUpdate.beforeInstall(context,ready,release);assertEquals(1,Backup.documents(context,tree).size());assertFalse(MailSync.BUSY.get());}
        finally {ready.delete();for(File f:provider.files.values()) f.delete();Backup.prefs(context).edit().clear().commit();}
    }
    @Test public void previewAndReleaseVersionMismatchCannotBeInstalled() throws Exception {
        AppUpdate.Release release=AppUpdate.parse(metadata());current.packageName="de.nll.lager.preview";
        assertThrows(Exception.class,() -> AppUpdate.verify(fixture,release,context.getPackageManager(),current));
        current.packageName=AppUpdate.PACKAGE;
        AppUpdate.Release mismatch=new AppUpdate.Release("9.0.0","",release.url,release.digest,release.size);
        assertThrows(Exception.class,() -> AppUpdate.verify(fixture,mismatch,context.getPackageManager(),current));
    }
    static class Connection extends HttpURLConnection {
        final int status;final String redirect;final byte[] bytes;
        Connection(URL url,int status,String redirect,byte[] bytes) {super(url);this.status=status;this.redirect=redirect;this.bytes=bytes;}
        @Override public int getResponseCode() {return status;}
        @Override public String getHeaderField(String name) {return "Location".equals(name) ? redirect : null;}
        @Override public InputStream getInputStream() {return new ByteArrayInputStream(bytes);}
        @Override public long getContentLengthLong() {return bytes.length;}
        @Override public void disconnect() {}
        @Override public boolean usingProxy() {return false;}
        @Override public void connect() {}
    }
}
