package de.nll.lager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int EXPORT = 101, IMPORT = 102, MAIL_AUTH = 103, BACKUP_FOLDER=104, BACKUP_RESTORE=105, UPDATE_PERMISSION=106, UPDATE_INSTALL=107;
    private static final int INK = Color.rgb(50,49,48), BLUE = Color.rgb(0,120,212), MUTED = Color.rgb(96,94,92);
    private Store store;
    private Store.Person person;
    private Store.Material selected;
    private LinearLayout body;
    private EditText scan, amount;
    private Spinner from, to, movementKind;
    private AdminSettings settings;
    private boolean adminUnlocked;
    private SharedPreferences prefs;
    private AppUpdate.Release updateRelease;
    private java.io.File updateFile;
    private String screen = "login", notice = "", module = "Material", searchQuery = "";
    private long exportCutoff;
    private boolean exportArchive, pickerOpen, writing, importPeople, exportExcel;
    private String draftKind = "", draftFrom = "", draftTo = "", draftAmount = "1", draftScan = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store(this); settings = new AdminSettings(this); prefs = getSharedPreferences("preferences", MODE_PRIVATE);
        draftFrom = prefs.getString("from", "");
        if (state != null) {
            person = store.person(state.getString("person", ""));
            selected = store.material(state.getString("material", ""));
            screen = state.getString("screen", "login");
            module = state.getString("module", "Material"); searchQuery = state.getString("searchQuery", "");
            exportCutoff = state.getLong("cutoff"); exportArchive = state.getBoolean("archive"); exportExcel=state.getBoolean("exportExcel");
            pickerOpen = state.getBoolean("picker"); importPeople = state.getBoolean("importPeople");
            draftFrom = state.getString("from", draftFrom); draftTo = state.getString("to", draftTo);
            draftKind=state.getString("kind", "");
            draftAmount = state.getString("amount", "1"); draftScan = state.getString("scan", "");
        }
        render();
        MailJobService.schedule(this);Backup.request(this);AppUpdate.cleanup(this);
        if(new MailConfig(this).ready() && new MailConfig(this).automatic) new Thread(() -> MailSync.run(getApplicationContext()),"mail-start").start();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        captureDraft();
        state.putString("person", person == null ? "" : person.code);
        state.putString("material", selected == null ? "" : selected.code);
        state.putString("module", module); state.putString("searchQuery", searchQuery);
        state.putString("screen", screen); state.putLong("cutoff", exportCutoff);
        state.putBoolean("importPeople", importPeople);
        state.putBoolean("exportExcel",exportExcel); state.putBoolean("archive", exportArchive); state.putBoolean("picker", pickerOpen);
        state.putString("from", draftFrom); state.putString("to", draftTo);
        state.putString("kind",draftKind);
        state.putString("amount", draftAmount); state.putString("scan", draftScan);
        super.onSaveInstanceState(state);
    }
    private void captureDraft() {
        if ("book".equals(screen) && from != null) {
            draftFrom = from.getSelectedItemPosition()>0 ? from.getSelectedItem().toString() : ""; draftTo = spinnerValue(to);
            draftKind=spinnerValue(movementKind);
            draftAmount = amount.getText().toString(); draftScan = scan.getText().toString();
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(12));
        if (border != 0) d.setStroke(dp(1), border);
        return d;
    }
    private void base(String title, String subtitle) {
        from = null; to = null; movementKind=null; amount = null; scan = null;
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setFitsSystemWindows(true);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16), dp(16), dp(16), dp(24));
        scroll.setBackgroundColor(Color.rgb(245,245,245));
        scroll.addView(body);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setFitsSystemWindows(true); root.setBackgroundColor(Color.rgb(245,245,245));
        LinearLayout header=new LinearLayout(this); header.setGravity(android.view.Gravity.CENTER_VERTICAL); header.setPadding(dp(16),dp(4),dp(16),dp(4));
        if(!"login".equals(screen) && !"home".equals(screen)) {
            ImageButton back=new ImageButton(this); back.setImageResource(R.drawable.ic_arrow_back);
            back.setBackground(shape(Color.WHITE,0)); back.setContentDescription("Zurück");
            LinearLayout.LayoutParams backLayout=new LinearLayout.LayoutParams(dp(52),dp(52)); backLayout.setMarginEnd(dp(12)); header.addView(back,backLayout);
            back.setOnClickListener(v -> {if(!writing && !pickerOpen) {captureDraft(); hideKeyboard(); onBackPressed();}});
        }
        TextView brand=new TextView(this); brand.setText("Niederlassung Leipzig"); brand.setTextSize(14); brand.setTextColor(MUTED); brand.setTypeface(null,Typeface.BOLD);
        header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton lock=new ImageButton(this); lock.setImageResource(adminUnlocked ? R.drawable.ic_lock_open : R.drawable.ic_lock_closed);
        lock.setBackground(shape(Color.WHITE,0)); lock.setContentDescription(adminUnlocked ? "Einstellungen sperren" : "Einstellungen entsperren");
        header.addView(lock,new LinearLayout.LayoutParams(dp(52),dp(52)));
        lock.setOnClickListener(v -> {if(writing || pickerOpen) return; captureDraft(); hideKeyboard(); if(adminUnlocked) lockSettings(); else passwordDialog(false);});
        root.addView(header); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
        TextView heading = text(title, 25, INK); heading.setTypeface(null, Typeface.BOLD);
        heading.setPadding(dp(16),dp(20),dp(16),dp(20)); heading.setBackground(shape(Color.WHITE,0));
        text(subtitle, 16, MUTED);
        if (!notice.isEmpty()) {
            boolean failed = notice.contains("fehlgeschlagen");
            TextView n = text(notice, 17, failed ? Color.rgb(151,47,32) : BLUE); n.setPadding(dp(12),dp(12),dp(12),dp(12));
            n.setBackground(shape(failed ? Color.rgb(255,231,224) : Color.rgb(229,241,251),0));
        }
    }
    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(12); body.addView(t,p); return t;
    }
    private Button button(String value, boolean primary, Runnable action) {
        Button b = new Button(this); b.setText(value); b.setTextSize(18); b.setAllCaps(false);
        b.setTypeface(null,Typeface.BOLD); b.setTextColor(primary ? Color.WHITE : INK);
        b.setMinHeight(dp(60)); b.setPadding(dp(12),dp(10),dp(12),dp(10));
        b.setBackground(shape(primary ? BLUE : Color.WHITE, primary ? 0 : Color.rgb(225,223,221)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(12); body.addView(b,p);
        b.setOnClickListener(v -> { if (!writing && !pickerOpen) action.run(); }); return b;
    }
    private EditText field(String label, String value, int type) {
        text(label,16,INK).setTypeface(null,Typeface.BOLD);
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(21); e.setTextColor(INK);
        e.setInputType(type); e.setText(value); e.setMinHeight(dp(58)); e.setSelectAllOnFocus(true);
        e.setPadding(dp(12),dp(10),dp(12),dp(10)); e.setBackground(shape(Color.WHITE,Color.rgb(200,198,196)));
        e.setContentDescription(label);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(16); body.addView(e,p);
        return e;
    }
    private EditText scanner(String label, String initial, Runnable submit) {
        EditText e=field(label,initial,InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        e.setOnEditorActionListener((v,action,event) -> {
            if (event != null && event.getKeyCode()==KeyEvent.KEYCODE_ENTER) {
                if (event.getAction()==KeyEvent.ACTION_UP) submit.run();
                return true;
            }
            if (action==EditorInfo.IME_ACTION_DONE || action==EditorInfo.IME_ACTION_GO) { submit.run(); return true; }
            return false;
        });
        e.requestFocus(); return e;
    }
    private void hideKeyboard() {
        View focus=getCurrentFocus();
        if (focus!=null) ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focus.getWindowToken(),0);
    }
    private void error(String message) { new AlertDialog.Builder(this).setTitle("Bitte prüfen").setMessage(message).setPositiveButton("OK",null).show(); }
    private void render() {
        if(isAdminScreen() && !adminUnlocked) screen=person==null ? "login" : "home";
        if(person==null && !isAdminScreen()) screen="login";
        switch (screen) {
            case "settings": settingsMenu(); break;
            case "mail": mailMenu(); break;
            case "backup": backupMenu(); break;
            case "updates": updateMenu(); break;
            case "about": aboutMenu(); break;
            case "locations": locationMenu(); break;
            case "home": home(); break;
            case "browse": browse(); break;
            case "book": booking(); break;
            case "materials": catalog(false); break;
            case "people": catalog(true); break;
            case "history": history(false); break;
            case "archive": history(true); break;
            default: login();
        }
    }
    private void login() {
        screen="login"; selected=null;
        base("Lagerverwaltung", "Person anmelden");
        text("Persönlichen Code scannen und mit Enter bestätigen.",18,INK);
        scan=scanner("Personencode", "", this::signIn);
        button("Anmelden",true,this::signIn);
        text("Zum Einrichten das Schloss oben rechts öffnen. Dort werden Personen und Listen verwaltet.",15,MUTED);
    }
    private void signIn() {
        try {
            String code=Rules.required(scan.getText().toString(),"Personencode");
            Store.Person found=store.person(code);
            if (found==null) { error("Personencode unbekannt. Bitte die zuständige Person um Einrichtung über das Schloss bitten."); return; }
            hideKeyboard(); person=found; draftTo=person.location; screen="home"; notice=""; draftScan=""; render();
        } catch (IllegalArgumentException e) { error(e.getMessage()); }
    }
    private boolean isAdminScreen() {
        return java.util.Arrays.asList("settings","locations","materials","people","history","archive","mail","backup","updates","about").contains(screen);
    }
    private boolean requireAdmin() {
        if(adminUnlocked) return true;
        error("Diese Funktion ist nur in den freigeschalteten Einstellungen verfügbar."); return false;
    }
    private void lockSettings() {
        captureDraft(); adminUnlocked=false; notice="";
        if(isAdminScreen()) screen=person==null ? "login" : "home";
        render();
    }
    private void passwordDialog(boolean change) { passwordDialog(change,null); }
    private void passwordDialog(boolean change,Runnable afterUnlock) {
        if(change && !requireAdmin()) return;
        boolean setup=!settings.hasPassword();
        LinearLayout layout=dialogBody();
        EditText password=dialogField(layout,change || setup ? "Neues Passwort (mindestens 8 Zeichen)" : "Passwort","");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText repeat=(change || setup) ? dialogField(layout,"Passwort wiederholen","") : null;
        if(repeat!=null) repeat.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog dialog=form(change ? "Passwort ändern" : setup ? "Einstellungspasswort festlegen" : "Einstellungen entsperren",layout);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(change || setup ? "Passwort speichern" : "Entsperren");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            char[] value=password.getText().toString().toCharArray();
            try {
                if(change && !requireAdmin()) return;
                if(change || setup) {
                    if(!password.getText().toString().equals(repeat.getText().toString())) {repeat.setError("Passwörter stimmen nicht überein."); return;}
                    settings.setPassword(value);
                } else if(!settings.verify(value)) {password.setError("Passwort ist falsch."); password.setText(""); return;}
                password.setText(""); if(repeat!=null) repeat.setText("");
                adminUnlocked=true; dialog.dismiss(); hideKeyboard();
                if(afterUnlock!=null) afterUnlock.run();
                else {screen="settings"; notice=""; render();}
            } catch(IllegalArgumentException | IllegalStateException e) {password.setError(e.getMessage());}
            finally {java.util.Arrays.fill(value,'\0');}
        });
    }
    private void settingsMenu() {
        if(!requireAdmin()) return;
        base("Einstellungen", "Freigeschaltet · zum Sperren das offene Schloss antippen");
        button("Lageroptionen verwalten",true,() -> {screen="locations"; render();});
        button("Material- und Kältemittelliste",false,() -> {screen="materials"; render();});
        button("Personen verwalten",false,() -> {screen="people"; render();});
        button("Bericht exportieren",false,() -> beginExport(false));
        button("Bewegungen & Archiv",false,() -> {screen="history"; render();});
        button("Mail & Synchronisierung",false,() -> {screen="mail";render();});
        button("Sicherung & Wiederherstellung",false,() -> {screen="backup";render();});
        button("App aktualisieren",false,() -> {screen="updates";render();});
        button("Über die App (About)",false,() -> {screen="about";render();});
        button("Passwort ändern",false,() -> passwordDialog(true));
        button("Einstellungen sperren",false,this::lockSettings);
    }
    private void aboutMenu() {
        if(!requireAdmin()) return;
        base("Über die App", "NLL Lager · Material- und Kältemittelbewegungen");
        String version="unbekannt", build="unbekannt";
        try {
            android.content.pm.PackageInfo current=AppUpdate.installed(this);
            version=current.versionName;
            build=String.valueOf(AppUpdate.code(current));
        } catch(Exception ignored) {}
        text("Version: "+version+" · Build: "+build,22,INK);
        text(AppUpdate.PACKAGE.equals(getPackageName()) ? "Ausgabe: Haupt-App" : "Ausgabe: Vorschau · separate App-Daten",17,INK);
        text("Paket: "+getPackageName(),16,MUTED);
        text("Android: "+android.os.Build.VERSION.RELEASE+" · API "+android.os.Build.VERSION.SDK_INT,16,MUTED);
        text("Datenbankversion: "+store.getReadableDatabase().getVersion(),16,MUTED);
        text("Betrieb & Daten",20,INK);
        text("Buchungen und Listen werden offline in der lokalen App-Datenbank gespeichert. Eine Deinstallation oder das Löschen der App-Daten entfernt diese Daten. Sicherungen im selbst gewählten Dokumentenordner können unabhängig von der Installation erhalten bleiben. Sicherungsordner unter „Sicherung & Wiederherstellung“ einrichten.",16,MUTED);
        text("Mail & Updates",20,INK);
        text("Die optionale Gmail-Anbindung überträgt Listen und Berichte. Buchungen funktionieren auch ohne Internet. App-Updates stammen aus stabilen GitHub-Releases; vor der Installation werden Signatur und Version geprüft und eine Sicherung erstellt.",16,MUTED);
        text("Projekt & Versionshistorie",20,INK);
        text("github.com/"+AppUpdate.REPO+"\nVersionshistorie: CHANGELOG.md\nAnleitungen im Projektordner docs/: SICHERUNG.md · UPDATES.md · GMAIL-EINRICHTUNG.md",16,MUTED);
        text("Für Support bitte Version, Build, Android-Version und eine Beschreibung des Problems angeben. Keine Passwörter oder Schlüssel weitergeben.",16,MUTED);
    }
    private void updateMenu() {
        if(!requireAdmin()) return;
        screen="updates";base("App aktualisieren","Signierte Updates direkt von GitHub");
        String version="unbekannt";
        try {android.content.pm.PackageInfo current=AppUpdate.installed(this);version=current.versionName+" ("+AppUpdate.code(current)+")";} catch(Exception ignored) {}
        text("Installiert: "+version,18,INK);
        text("Quelle: github.com/"+AppUpdate.REPO+" · stabile Releases. Vor Installation werden Version, Prüfsumme und Release-Signatur geprüft und eine vollständige Sicherung im gewählten Sicherungsordner erstellt. Android fragt anschließend nach deiner Installationsbestätigung.",16,MUTED);
        if(!AppUpdate.PACKAGE.equals(getPackageName())) {text("In der Vorschau-App ist die Updateinstallation deaktiviert. Bitte die signierte Haupt-App verwenden.",17,INK);return;}
        button("Nach Updates suchen",true,this::checkAppUpdate);
        if(updateRelease!=null) {
            text("Verfügbar: "+updateRelease.version+" · "+String.format(Locale.GERMANY,"%.1f MB",updateRelease.size/1048576d),20,INK);
            text(updateRelease.notes,16,INK);
            if(updateFile==null) button("Update herunterladen",true,this::downloadAppUpdate);
            else button("Sichern & Update installieren",true,this::installAppUpdate);
        }
    }
    private void checkAppUpdate() {
        if(!requireAdmin()) return;
        writing=true;
        AlertDialog progress=new AlertDialog.Builder(this).setTitle("Suche nach Updates …").setMessage("GitHub wird geprüft.").setCancelable(false).create();progress.show();
        new Thread(() -> {
            AppUpdate.Release found=null;String result;
            try {
                AppUpdate.Release latest=new AppUpdate().check();android.content.pm.PackageInfo current=AppUpdate.installed(this);
                int cmp=AppUpdate.compare(latest.version,current.versionName);
                if(cmp>0 || cmp==0 && current.versionName.endsWith("-dev")) {found=latest;result="Neue Version gefunden";}
                else result="Keine neuere stabile Version verfügbar · GitHub: "+latest.version;
            } catch(Exception e) {result="Updatesuche fehlgeschlagen: "+e.getMessage();}
            final AppUpdate.Release release=found;final String message=result;
            runOnUiThread(() -> {writing=false;if(isDestroyed()) return;progress.dismiss();updateRelease=release;updateFile=null;notice=message;render();});
        },"update-check").start();
    }
    private void downloadAppUpdate() {
        if(!requireAdmin() || updateRelease==null) return;
        final AppUpdate.Release release=updateRelease;
        new AlertDialog.Builder(this).setTitle("Update herunterladen?").setMessage("Version "+release.version+" von GitHub herunterladen und prüfen? Zur Installation ist anschließend eine vollständige Sicherung erforderlich.")
            .setNegativeButton("Abbrechen",null).setPositiveButton("Herunterladen",(d,w) -> {
                if(!requireAdmin()) return;writing=true;
                AlertDialog progress=new AlertDialog.Builder(this).setTitle("Update wird geladen …").setMessage("APK wird heruntergeladen und auf Echtheit geprüft.").setCancelable(false).create();progress.show();
                new Thread(() -> {
                    java.io.File file=null;String result;
                    try {file=new AppUpdate().download(getApplicationContext(),release);result="✓ Update heruntergeladen und geprüft";} catch(Exception e) {result="Update-Download fehlgeschlagen: "+e.getMessage();}
                    final java.io.File ready=file;final String message=result;
                    runOnUiThread(() -> {writing=false;if(isDestroyed()) return;progress.dismiss();updateFile=ready;notice=message;render();});
                },"update-download").start();
            }).show();
    }
    private void installAppUpdate() {
        if(!requireAdmin() || updateRelease==null || updateFile==null) return;
        if(Backup.prefs(this).getString("tree","").isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Sicherungsordner fehlt").setMessage("Bitte vor dem Update einen Sicherungsordner auswählen. Die Installation wird ohne erfolgreiche Sicherung nicht gestartet.")
                .setNegativeButton("Abbrechen",null).setPositiveButton("Sicherung einrichten",(d,w) -> {if(requireAdmin()) {screen="backup";render();}}).show();return;
        }
        if(!getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this).setTitle("Android-Freigabe erforderlich").setMessage("Android muss dieser App das Installieren von Updates erlauben. Bitte die Freigabe in den Android-Einstellungen selbst aktivieren, danach das Schloss erneut öffnen und die Installation starten.")
                .setNegativeButton("Abbrechen",null).setPositiveButton("Android-Einstellungen",(d,w) -> {
                    if(!requireAdmin()) return;
                    try {startActivityForResult(new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())),UPDATE_PERMISSION);} catch(Exception e) {error("Installationsfreigabe ist auf diesem Gerät nicht verfügbar.");}
                }).show();return;
        }
        final java.io.File file=updateFile;final AppUpdate.Release release=updateRelease;
        new AlertDialog.Builder(this).setTitle("Update "+release.version+" installieren?").setMessage("Die aktuellen Geschäftsdaten werden zuerst gesichert. Danach öffnet sich die Android-Installationsbestätigung. Die App wird als Update installiert; keine Deinstallation erforderlich.")
            .setNegativeButton("Abbrechen",null).setPositiveButton("Sichern & installieren",(d,w) -> {
                if(!requireAdmin()) return;writing=true;
                AlertDialog progress=new AlertDialog.Builder(this).setTitle("Update wird vorbereitet …").setMessage("Signatur wird erneut geprüft und Daten werden gesichert.").setCancelable(false).create();progress.show();
                new Thread(() -> {
                    String failure="";try {AppUpdate.beforeInstall(getApplicationContext(),file,release);} catch(Exception e) {failure="Installation nicht gestartet: "+e.getMessage();}
                    final String problem=failure;
                    runOnUiThread(() -> {writing=false;if(isDestroyed()) return;progress.dismiss();
                        if(!problem.isEmpty()) {notice=problem;render();return;}
                        if(!adminUnlocked) {notice="Vorbereitung abgeschlossen · Einstellungen bitte erneut entsperren";render();return;}
                        Uri uri=new Uri.Builder().scheme("content").authority(getPackageName()+".updates").appendPath(file.getName()).build();
                        Intent install=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        install.setClipData(android.content.ClipData.newRawUri("NLL Lager Update",uri));
                        try {startActivityForResult(install,UPDATE_INSTALL);} catch(Exception e) {error("Android-Installation nicht verfügbar oder durch Geräteverwaltung gesperrt.");}
                    });
                },"update-install").start();
            }).show();
    }
    private void backupMenu() {
        if(!requireAdmin()) return;
        screen="backup";base("Sicherung & Wiederherstellung","Vollständige Geschäftsdaten · unabhängig von der Installation");
        String tree=Backup.prefs(this).getString("tree","");
        text(tree.isEmpty() ? "Noch kein Sicherungsordner ausgewählt." : "Sicherungsordner: "+Uri.decode(tree),15,MUTED);
        text(Backup.prefs(this).getString("status","Noch keine Sicherung erstellt."),17,INK);
        long last=Backup.prefs(this).getLong("last",0);
        if(last>0) text("Letzte erfolgreiche Sicherung: "+new SimpleDateFormat("dd.MM.yyyy HH:mm:ss",Locale.GERMANY).format(new Date(last)),15,MUTED);
        text("Einmal einen Ordner im Gerätespeicher wählen, z. B. Dokumente/NLL-Lager. Die letzten sieben geprüften Sicherungen bleiben erhalten. Enthalten: Material, Personen, Lager, offene Bewegungen und Archiv. Die Dateien sind lesbar und enthalten keine Passwörter oder Google-Zugriffsdaten.",16,MUTED);
        button("Sicherungsordner auswählen",true,() -> {
            if(!requireAdmin()) return;
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            try {pickerOpen=true;startActivityForResult(intent,BACKUP_FOLDER);} catch(Exception e) {pickerOpen=false;error("Ordnerauswahl nicht verfügbar.");}
        });
        if(!tree.isEmpty()) button("Jetzt sichern",true,() -> backupWork(null));
        button("Sicherung wiederherstellen",false,() -> {
            if(!requireAdmin()) return;
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(Intent.CATEGORY_OPENABLE);
            try {pickerOpen=true;startActivityForResult(intent,BACKUP_RESTORE);} catch(Exception e) {pickerOpen=false;error("Dateiauswahl nicht verfügbar.");}
        });
        text("Automatische Sicherungen folgen Änderungen, sobald Android die Hintergrundarbeit zulässt. Bei Fehlern bleiben ältere Stände erhalten. Nach Neuinstallation den Ordner erneut auswählen und eine Sicherung öffnen. Vor Wiederherstellung wird der aktuelle Stand zusätzlich gesichert; Gmail muss danach erneut verbunden werden. Ein Geräteverlust erfordert eine Kopie der Sicherungen außerhalb des Geräts.",16,MUTED);
    }
    private void backupWork(BackupData selected) {
        if(!requireAdmin()) return;
        writing=true;
        final android.content.Context app=getApplicationContext();
        AlertDialog progress=new AlertDialog.Builder(this).setTitle(selected==null ? "Sicherung wird erstellt …" : "Daten werden wiederhergestellt …").setMessage("Bitte warten.").setCancelable(false).create();progress.show();
        new Thread(() -> {
            String result;
            try {if(selected==null) {Backup.save(app,true);result="✓ Sicherung erstellt und geprüft";} else {Backup.restore(app,selected);result="✓ Wiederhergestellt · Gmail bitte erneut verbinden";}}
            catch(Exception e) {result="Sicherung/Wiederherstellung fehlgeschlagen: "+e.getMessage();Backup.status(app,result);}
            final String message=result;
            runOnUiThread(() -> {writing=false;if(isDestroyed()) return;progress.dismiss();
                if(selected!=null) {person=null;this.selected=null;adminUnlocked=false;screen="login";}notice=message;render();});
        },"backup-manual").start();
    }
    private void inspectBackup(Uri uri) {
        if(!requireAdmin()) return;
        writing=true;
        new Thread(() -> {
            BackupData data=null;String failure="";
            try(java.io.InputStream in=getContentResolver().openInputStream(uri)) {data=BackupData.read(in);}
            catch(Exception e) {failure="Ungültige Sicherung: "+e.getMessage();}
            final BackupData backup=data;final String problem=failure;
            runOnUiThread(() -> {
                writing=false;if(isDestroyed() || !adminUnlocked) return;
                if(backup==null) {error(problem);return;}
                try {new AlertDialog.Builder(this).setTitle("Aktuelle Daten ersetzen?").setMessage(backup.summary()+"\n\nDie Sicherung ersetzt alle aktuellen Geschäftsdaten, einschließlich Archiv. Zuerst wird eine Sicherheitskopie im gewählten Ordner erstellt. Gmail wird getrennt; alte Mailaufträge werden nicht erneut ausgeführt.")
                    .setNegativeButton("Abbrechen",null).setPositiveButton("Ersetzen & wiederherstellen",(d,w) -> backupWork(backup)).show();}
                catch(Exception e) {error("Sicherung konnte nicht geprüft werden.");}
            });
        },"backup-inspect").start();
    }
    private void mailMenu() {
        if(!requireAdmin()) return;
        screen="mail";base("Mail & Synchronisierung","Gmail · eigener Scanner-Posteingang");
        MailConfig config=new MailConfig(this);
        text(config.gmail.isEmpty() ? "Noch kein Postfach eingerichtet." : "Scanner: "+config.gmail+"\nFirmenpostfach: "+config.recipient,17,INK);
        text(config.connected ? "Google-Zugriff freigegeben" : "Google-Zugriff noch nicht bestätigt",16,MUTED);
        text("Status: "+MailConfig.prefs(this).getString("status","Noch keine Synchronisierung"),16,MUTED);
        long last=MailConfig.prefs(this).getLong("last",0);
        if(last>0) text("Letzter Versuch: "+new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.GERMANY).format(new Date(last)),15,MUTED);
        button("Postfach & Empfänger einrichten",true,this::configureMail);
        if(!config.gmail.isEmpty()) button("Mit Google verbinden",false,this::authorizeMail);
        if(config.ready()) {
            button("Jetzt synchronisieren",true,this::syncMail);
            button("Export jetzt per Mail senden",false,() -> localMailExport(config));
        }
        if(!config.gmail.isEmpty() && !config.recipient.isEmpty()) {
            text("Mailbefehle nur vom hinterlegten Firmenpostfach, Betreff wie unten; Groß-/Kleinschreibung ist egal. Für einen Import genau eine Excel-Datei anhängen. Signaturbilder werden ignoriert; andere zusätzliche Dateien sind nicht erlaubt.",16,MUTED);
            text("NLL Material\n\nNLL Personen\n\nNLL Export",15,INK).setTextIsSelectable(true);
        }
        if(config.connected) button("Automatischen Mailzugriff deaktivieren",false,() -> {
            if(!requireAdmin() || mailBusy()) return;
            MailConfig.prefs(this).edit().putBoolean("connected",false).apply();MailJobService.schedule(this);render();
        });
        text("Letzte Aufträge",20,INK);
        for(String[] record:new MailStore(store).records(config.gmail)) {
            String state=record[1];
            String label="sent".equals(state) ? "Versendet" : "archived".equals(state) ? "Abgeschlossen" : "uncertain".equals(state) ? "Versand unklar" : "Wartet auf Versand";
            text(label+" · "+record[2],15,INK);
            if("sent".equals(state) && Long.parseLong(record[3])>0) button("Übernahme bestätigen & archivieren",false,() -> {
                if(!requireAdmin() || mailBusy()) return;
                new AlertDialog.Builder(this).setTitle("Export übernommen?").setMessage("Nur die noch offenen Bewegungen dieses Berichts archivieren? Spätere Buchungen bleiben offen.")
                    .setNegativeButton("Abbrechen",null).setPositiveButton("Abschließen",(d,w) -> {
                        if(!requireAdmin() || mailBusy()) return;new MailStore(store).confirm(record[0]);render();
                    }).show();
            });
            if("uncertain".equals(state)) button("Versand prüfen / erneut versuchen",false,() -> {
                if(!requireAdmin() || mailBusy()) return;
                new AlertDialog.Builder(this).setTitle("Versandstatus unklar").setMessage("Bitte zuerst das Firmenpostfach und den Gmail-Ordner Gesendet prüfen. Bei erneutem Versand kann eine zweite Mail entstehen. Es wird derselbe Bericht versendet; Stammdaten werden nicht erneut importiert.")
                    .setNegativeButton("Abbrechen",null).setNeutralButton("Ist angekommen",(d,w) -> {if(requireAdmin() && !mailBusy()) {new MailStore(store).state(record[0],"sent");render();}})
                    .setPositiveButton("Erneut senden",(d,w) -> {if(requireAdmin() && !mailBusy()) {new MailStore(store).state(record[0],"pending");syncMail();}}).show();
            });
        }
    }
    private boolean mailBusy() {
        if(!MailSync.BUSY.get()) return false;error("Mail-Synchronisierung läuft. Bitte kurz warten.");return true;
    }
    private void configureMail() {
        if(!requireAdmin() || mailBusy()) return;
        MailConfig config=new MailConfig(this);LinearLayout layout=dialogBody();
        EditText gmail=dialogField(layout,"Scanner-Gmail-Adresse",config.gmail),recipient=dialogField(layout,"Firmenadresse (Absender und Empfänger)",config.recipient);
        gmail.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);recipient.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        CheckBox automatic=new CheckBox(this);automatic.setText("Beim Start und im Hintergrund synchronisieren (ca. 15 Minuten oder später)");automatic.setChecked(config.automatic);layout.addView(automatic);
        AlertDialog dialog=form("Mail einrichten",layout);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if(!requireAdmin() || mailBusy()) return;
            try {MailConfig.save(this,gmail.getText().toString(),recipient.getText().toString(),automatic.isChecked());MailJobService.schedule(this);dialog.dismiss();render();}
            catch(Exception e) {recipient.setError(e.getMessage());}
        });
    }
    private void authorizeMail() {
        if(!requireAdmin() || mailBusy()) return;
        MailConfig config=new MailConfig(this);
        com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this).authorize(config.request())
            .addOnSuccessListener(this,result -> {
                if(!adminUnlocked) return;
                if(result.hasResolution()) {
                    try {pickerOpen=true;startIntentSenderForResult(result.getPendingIntent().getIntentSender(),MAIL_AUTH,null,0,0,0);}
                    catch(Exception e) {pickerOpen=false;error("Google-Anmeldung konnte nicht geöffnet werden.");}
                } else verifyMailAuthorization(result);
            }).addOnFailureListener(this,e -> error("Google-Freigabe fehlgeschlagen. Gmail API und Android-OAuth-Client (Paketname und Zertifikat) im Google-Cloud-Projekt prüfen."));
    }
    private void verifyMailAuthorization(com.google.android.gms.auth.api.identity.AuthorizationResult result) {
        if(!requireAdmin()) return;
        if(result.getAccessToken()==null || !result.getGrantedScopes().containsAll(java.util.Arrays.asList(MailConfig.READ,MailConfig.SEND))) {error("Bitte sowohl Lesen als auch Senden für das Scanner-Postfach freigeben.");return;}
        MailConfig requested=new MailConfig(this);
        new Thread(() -> {
            boolean valid=false;
            try {valid=requested.gmail.equalsIgnoreCase(new GmailClient(result.getAccessToken()).call("profile",null).getString("emailAddress"));} catch(Exception ignored) { }
            final boolean connected=valid;
            runOnUiThread(() -> {
                if(isDestroyed() || !adminUnlocked) return;
                MailConfig current=new MailConfig(this);
                if(!current.gmail.equals(requested.gmail) || !current.recipient.equals(requested.recipient)) return;
                if(!connected) {error("Gmail-Postfach konnte nicht bestätigt werden. Kontowahl und API-Freigabe prüfen.");return;}
                MailConfig.prefs(this).edit().putBoolean("connected",true).apply();MailJobService.schedule(this);notice="Gmail-Verbindung bestätigt";render();
            });
        },"mail-authorize").start();
    }
    private void syncMail() {
        if(!requireAdmin() || mailBusy()) return;
        final android.content.Context app=getApplicationContext();
        new Thread(() -> {MailSync.run(app);runOnUiThread(() -> {if(!isDestroyed()) render();});},"mail-manual").start();
        notice="Synchronisierung gestartet";render();
    }
    private void localMailExport(MailConfig config) { localMailExport(config,false); }
    private void localMailExport(MailConfig config,boolean archiveAfterSend) {
        if(!requireAdmin() || mailBusy()) return;
        if(!config.ready()) {error("Bitte zuerst unter Mail & Synchronisierung das Postfach einrichten und Google verbinden.");return;}
        new AlertDialog.Builder(this).setTitle(archiveAfterSend ? "Per Mail senden & archivieren?" : "Excel per Mail senden?").setMessage("Offene Bewegungen an "+config.recipient+" senden? "+(archiveAfterSend ? "Nach bestätigtem Versand werden genau diese Bewegungen archiviert. Bei Fehler oder unklarem Versand bleiben sie offen. Versand bestätigt noch nicht den Empfang im Firmenpostfach." : "Sie bleiben bis zur separaten Bestätigung offen."))
            .setNegativeButton("Abbrechen",null).setPositiveButton("Senden",(d,w) -> {
                if(!requireAdmin() || mailBusy()) return;
                final android.content.Context app=getApplicationContext();
                new Thread(() -> {
                    if(!MailSync.BUSY.compareAndSet(false,true)) return;
                    try(Store local=new Store(app)) {
                        org.json.JSONObject payload=new org.json.JSONObject().put("headers",new org.json.JSONArray()
                            .put(new org.json.JSONObject().put("name","From").put("value",config.recipient))
                            .put(new org.json.JSONObject().put("name","Subject").put("value","NLL Export")));
                        String id="local-"+UUID.randomUUID();
                        MailSync.prepare(app,config,local,null,new org.json.JSONObject().put("id",id).put("internalDate",System.currentTimeMillis()).put("payload",payload),archiveAfterSend);
                    } catch(Exception e) {MailConfig.status(app,"Export konnte nicht vorbereitet werden.");}
                    finally {MailSync.BUSY.set(false);}
                    MailSync.run(app);runOnUiThread(() -> {if(!isDestroyed()) render();});
                },"mail-export").start();
            }).show();
    }
    private void locationMenu() {
        if(!requireAdmin()) return;
        screen="locations"; base("Lageroptionen", "Auswahl für „Von Lager“ bei Material und Kältemittel");
        button("+ Lager hinzufügen",true,() -> editLocation(null));
        for(String location:settings.locations()) {
            button(location+" · bearbeiten",false,() -> editLocation(location));
        }
        if(settings.locations().isEmpty()) text("Noch keine Lager hinterlegt.",18,MUTED);
    }
    private void editLocation(String original) {
        if(!requireAdmin()) return;
        LinearLayout layout=dialogBody(); EditText name=dialogField(layout,"Lagername",original==null ? "" : original);
        ScrollView scroll=new ScrollView(this); scroll.addView(layout);
        AlertDialog.Builder builder=new AlertDialog.Builder(this).setTitle(original==null ? "Lager hinzufügen" : "Lager bearbeiten").setView(scroll).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",null);
        if(original!=null) builder.setNeutralButton("Entfernen",null);
        AlertDialog dialog=builder.create(); dialog.setCanceledOnTouchOutside(false); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if(!requireAdmin()) return;
            try {
                List<String> values=settings.locations(); String replacement=Rules.required(name.getText().toString(),"Lagername");
                if(original==null) values.add(replacement); else values.set(values.indexOf(original),replacement);
                settings.saveLocations(values); if(original!=null && original.equals(draftFrom)) draftFrom=replacement;
                dialog.dismiss(); render();
            } catch(IllegalArgumentException | IllegalStateException e) {name.setError(e.getMessage());}
        });
        if(original!=null) {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                if(!requireAdmin()) return;
                List<String> values=settings.locations(); values.remove(original); settings.saveLocations(values);
                if(original.equals(draftFrom)) draftFrom=""; dialog.dismiss(); render();
            });
        }
    }
    @Override protected void onStop() {
        captureDraft();
        if(!pickerOpen && !writing) adminUnlocked=false;
        super.onStop();
    }
    @Override protected void onStart() {
        super.onStart();
        if(settings!=null && !pickerOpen && !writing) {captureDraft(); render();}
    }
    private void home() {
        screen="home";
        base("Lagerverwaltung", "Angemeldet: " + person.name);
        text("Was möchtest du machen?",20,INK).setTypeface(null,Typeface.BOLD);
        button("Material",true,() -> openModule("Material"));
        button("Kältemittel",true,() -> openModule("Kältemittel"));
        text(store.count(false)+" offene Warenbewegungen",16,MUTED);
        if(adminUnlocked) button("Einstellungen",false,() -> {screen="settings"; notice=""; render();});
        button("Person wechseln / abmelden",false,this::signOut);
    }
    private void signOut() {
        adminUnlocked=false; person=null; draftTo=""; draftFrom=""; selected=null; draftScan=""; draftAmount="1"; searchQuery=""; notice=""; screen="login"; render();
    }
    private void openModule(String type) {
        module=type; selected=null; draftScan=""; draftAmount="1"; searchQuery=""; notice=""; screen="browse"; render();
    }
    private void browse() {
        screen="browse";
        base(module, "Artikel auswählen und Warenbewegung erfassen");
        scan=scanner("Suchen oder Code scannen",searchQuery,this::searchArticles);
        button("Suchen / Code übernehmen",true,this::searchArticles);
        LinearLayout results=new LinearLayout(this); results.setOrientation(LinearLayout.VERTICAL); body.addView(results);
        showArticles(results,searchQuery);
        scan.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { searchQuery=s.toString(); showArticles(results,searchQuery); }
            public void afterTextChanged(android.text.Editable s) {}
        });

    }
    private void searchArticles() {
        searchQuery=scan.getText().toString().trim(); hideKeyboard();
        Store.Material found=store.material(searchQuery);
        if(found!=null && module.equals(found.type)) selectArticle(found);
        else if(found!=null) error("Dieser Artikel gehört zum Bereich „"+found.type+"“.");
        else if(!searchQuery.isEmpty() && store.materials(module,searchQuery).isEmpty()) requestNewArticle(searchQuery);
        else {notice=""; render();}
    }
    private void requestNewArticle(String code) {
        new AlertDialog.Builder(this).setTitle("Unbekannter Code").setMessage("Code „"+code+"“ als "+module+" anlegen? Dafür ist das Gerätepasswort erforderlich.")
            .setPositiveButton("Anlegen",(d,w) -> {
                if(adminUnlocked) addMaterial(code,true);
                else if(!settings.hasPassword()) error("Bitte zuerst über das Schloss ein Gerätepasswort einrichten.");
                else passwordDialog(false,() -> addMaterial(code,true,true));
            }).setNegativeButton("Abbrechen",null).show();
    }
    private void showArticles(LinearLayout results,String query) {
        results.removeAllViews();
        List<Store.Material> articles=store.materials(module,query);
        if(articles.isEmpty()) {
            TextView empty=new TextView(this); empty.setText("Keine passenden Artikel. Bitte die Suche prüfen oder die zuständige Person um Einrichtung bitten."); empty.setTextSize(17); empty.setTextColor(MUTED); empty.setPadding(0,dp(16),0,dp(20)); results.addView(empty);
        }
        for(Store.Material article:articles) {
            Button item=new Button(this); item.setAllCaps(false); item.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            item.setText(article.description+"\n"+article.number+"  ·  "+article.unit+"\nCode: "+article.code+(article.location.isEmpty() ? "" : "\nLager: "+article.location));
            item.setContentDescription(article.description+", "+article.number); item.setTextSize(18); item.setTextColor(INK);
            item.setPadding(dp(16),dp(16),dp(16),dp(16)); item.setMinHeight(dp(100)); item.setBackground(shape(Color.WHITE,Color.rgb(225,223,221)));
            LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2); layout.bottomMargin=dp(12); results.addView(item,layout);
            item.setOnClickListener(v -> {if(!writing && !pickerOpen) selectArticle(article);});
        }
        if(articles.size()==200) {TextView hint=new TextView(this); hint.setText("Erste 200 Treffer · Suche eingrenzen"); results.addView(hint);}
    }
    private void selectArticle(Store.Material article) {
        // A remote catalog update takes effect for the next booking, not an existing draft.
        person=store.person(person.code);
        article=store.material(article.code);
        if(person==null || article==null) {screen=person==null ? "login" : "browse";render();return;}
        selected=article; draftScan=article.code; draftFrom=article.location; draftTo=person.location; draftAmount="1"; draftKind=""; notice=""; screen="book"; render();
    }
    private void booking() {
        screen="book";
        base(module + " · Warenbewegung", "Angemeldet: " + person.name);
        long pending=store.count(false);
        text(pending + " offene Bewegungen · " + store.count(true) + " archiviert",16,MUTED);
        scan=scanner("Material / Kältemittel scannen",draftScan,this::lookup);
        button("Code übernehmen",false,this::lookup);
        if (selected==null) text("Vorhandenen Artikelcode scannen. Unbekannte Codes können anschließend angelegt werden.",17,MUTED);
        else {
            TextView card=text(selected.number + " · " + selected.description + "\n" + selected.type + " / " + selected.unit,21,INK);
            card.setTypeface(null,Typeface.BOLD);
        }
        from=warehouseDropdown(body,"Von Lager",sourceOptions(),draftFrom);
        to=warehouseDropdown(body,"Nach Lager",destinationOptions(),draftTo);
        movementKind=warehouseDropdown(body,"Bewegungsart",java.util.Arrays.asList("Entnahme","Rückgabe","Umlagerung"),draftKind);
        if(sourceOptions().isEmpty()) text("Noch keine Lager eingerichtet. Bitte über das Schloss ein Lager hinterlegen.",16,MUTED);
        if(person.location.isEmpty()) text("Für diese Person fehlt die Lagernummer. Bitte in den Einstellungen nachpflegen.",16,MUTED);
        amount=field("Menge" + (selected==null ? "" : " ("+selected.unit+")"),"",InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        // Accept both decimal separators even on scanners configured with an English system locale.
        amount.setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));
        amount.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(draftAmount);
        amount.setImeOptions(EditorInfo.IME_ACTION_DONE);
        amount.setOnEditorActionListener((v,a,event) -> { if(a==EditorInfo.IME_ACTION_DONE) {hideKeyboard(); return true;} return false; });
        button("Bewegung speichern",true,this::confirmBooking);
    }
    private void lookup() {
        try {
            String code=Rules.required(scan.getText().toString(),"Scancode");
            captureDraft(); hideKeyboard();
            Store.Material material=store.material(code);
            if (material==null) {
                selected=null;
                requestNewArticle(code);
            } else if(!module.equals(material.type)) { error("Dieser Artikel gehört zum Bereich „"+material.type+"“. Bitte auf der Startseite den passenden Bereich öffnen."); }
            else { selectArticle(material); }
        } catch (IllegalArgumentException e) { error(e.getMessage()); }
    }
    private void confirmBooking() {
        try {
            if (selected==null) throw new IllegalArgumentException("Bitte zuerst einen bekannten Materialcode übernehmen.");
            if (!scan.getText().toString().trim().equals(selected.code)) throw new IllegalArgumentException("Der Code wurde geändert. Bitte zuerst „Code übernehmen“ drücken.");
            captureDraft();
            final String f=Rules.required(draftFrom,"Von Lager"), t=Rules.required(draftTo,"Nach Lager"), q=Rules.quantity(draftAmount), kind=Rules.required(draftKind,"Bewegungsart");
            if(!sourceOptions().contains(f)) throw new IllegalArgumentException("Bitte ein eingerichtetes Lager auswählen.");
            if(!destinationOptions().contains(t)) throw new IllegalArgumentException("Bitte ein eingerichtetes Ziellager auswählen.");
            Rules.locations(f,t); hideKeyboard();
            new AlertDialog.Builder(this).setTitle("Bewegung bestätigen")
                .setMessage(selected.number+" · "+selected.description+"\n\n"+q+" "+selected.unit+"\n"+f+" → "+t+"\nBewegungsart: "+kind+"\nPerson: "+person.name)
                .setNegativeButton("Zurück",null).setPositiveButton("Jetzt speichern",(d,w) -> {
                    try {
                        store.book(selected,f,t,q,person,kind);
                        prefs.edit().putString("from",f).apply();
                        notice="✓ Bewegung gespeichert: "+q+" "+selected.unit;
                        selected=null; draftScan=""; draftAmount="1"; screen="browse"; render();
                    } catch (Exception e) { error("Bewegung konnte nicht gespeichert werden. Bitte erneut versuchen."); }
                }).show();
        } catch (IllegalArgumentException e) { error(e.getMessage()); }
    }
    private void catalog(boolean people) {
        if(!requireAdmin()) return;
        screen=people ? "people" : "materials";
        base(people ? "Personen" : "Material & Kältemittel", "Stammdaten und Lagerzuordnung verwalten");
        button(people ? "+ Person anlegen" : "+ Material anlegen",true,() -> { if(people) addPerson(); else addMaterial("",false); });
        button("Aus Excel importieren (.xlsx)",false,() -> beginImport(people));
        text(people ? "Excel-Spalten: Personencode, Name, Lagernummer (optional)" : "Excel: Firmenliste mit Title und Code (Material / Stk) oder Vorlage mit Scancode, Mat Nummer, Beschreibung, Art, Einheit. Lager ist optional.",15,MUTED);
        text("Erste Tabelle, erste Zeile = Überschriften. Codes als Text speichern, damit führende Nullen erhalten bleiben. Vorhandene Codes werden nicht überschrieben.",15,MUTED);
        if(people) {
            for(Store.Person entry:store.people()) button(entry.name+" · Lager: "+(entry.location.isEmpty() ? "nicht zugeordnet" : entry.location),false,() -> editAssignment(entry.code,true));
        } else {
            for(Store.Material entry:store.allMaterials()) button(entry.number+" · "+entry.description+"\nLager: "+(entry.location.isEmpty() ? "nicht zugeordnet" : entry.location),false,() -> editAssignment(entry.code,false));
        }
    }
    private List<String> sourceOptions() {
        java.util.LinkedHashSet<String> values=new java.util.LinkedHashSet<>(settings.locations());
        if(selected!=null && !selected.location.isEmpty()) values.add(selected.location);
        return new java.util.ArrayList<>(values);
    }
    private List<String> destinationOptions() {
        java.util.LinkedHashSet<String> values=new java.util.LinkedHashSet<>(settings.locations());
        for(Store.Person entry:store.people()) if(!entry.location.isEmpty()) values.add(entry.location);
        return new java.util.ArrayList<>(values);
    }
    private String spinnerValue(Spinner spinner) {return spinner!=null && spinner.getSelectedItemPosition()>0 ? spinner.getSelectedItem().toString() : "";}
    private Spinner warehouseDropdown(LinearLayout layout,String label,List<String> values,String initial) {
        TextView title=new TextView(this); title.setText(label); title.setTextSize(16); title.setTextColor(INK); title.setTypeface(null,Typeface.BOLD); layout.addView(title);
        List<String> options=new java.util.ArrayList<>(); options.add("Bewegungsart".equals(label) ? "Bewegungsart auswählen …" : "Lager auswählen …"); options.addAll(values);
        Spinner spinner=new Spinner(this,Spinner.MODE_DROPDOWN); spinner.setContentDescription(label); spinner.setMinimumHeight(dp(58));
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,options); spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0,options.indexOf(initial))); layout.addView(spinner,new LinearLayout.LayoutParams(-1,dp(58))); return spinner;
    }
    private void editAssignment(String code,boolean people) {
        if(!requireAdmin()) return;
        LinearLayout layout=dialogBody();
        String current=people ? store.person(code).location : store.material(code).location;
        EditText location=people ? dialogField(layout,"Lagernummer",current) : null;
        Spinner warehouse=people ? null : warehouseDropdown(layout,"Lager",settings.locations(),current);
        AlertDialog dialog=form(people ? "Mitarbeiterlager zuordnen" : "Materiallager zuordnen",layout);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if(!requireAdmin()) return;
            try {
                store.assignLocation(code,people ? location.getText().toString() : spinnerValue(warehouse),people);
                if(people && person!=null && person.code.equals(code)) {person=store.person(code); draftTo=person.location;}
                dialog.dismiss(); render();
            } catch(IllegalArgumentException e) {error(e.getMessage());}
        });
    }

    private void listCard(String s) {
        TextView t=text(s,18,INK); t.setPadding(dp(14),dp(14),dp(14),dp(14)); t.setBackground(shape(Color.WHITE,0));
    }
    private LinearLayout dialogBody() {
        LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(20),dp(8),dp(20),dp(12)); return l;
    }
    private EditText dialogField(LinearLayout layout,String label,String value) {
        TextView t=new TextView(this); t.setText(label); t.setTextColor(INK); t.setTextSize(16); layout.addView(t);
        EditText e=new EditText(this); e.setSingleLine(true); e.setTextSize(20); e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setHint(label); e.setContentDescription(label); e.setText(value); e.setMinHeight(dp(56)); layout.addView(e); return e;
    }
    private AlertDialog form(String title,LinearLayout layout) {
        ScrollView sc=new ScrollView(this); sc.addView(layout);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(sc).setNegativeButton("Abbrechen",null).setPositiveButton("Speichern",null).create();
        dialog.setCanceledOnTouchOutside(false); dialog.show(); return dialog;
    }
    private void addPerson() {
        if(!requireAdmin()) return;
        LinearLayout l=dialogBody();
        EditText code=dialogField(l,"Personencode scannen",""); EditText name=dialogField(l,"Name","");
        EditText location=dialogField(l,"Lagernummer","");
        AlertDialog d=form("Person anlegen",l);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try { if(!requireAdmin()) return; store.addPerson(code.getText().toString(),name.getText().toString(),Rules.required(location.getText().toString(),"Lagernummer")); d.dismiss(); notice="✓ Person angelegt"; render(); }
            catch(SQLiteConstraintException e) {code.setError("Dieser Code existiert bereits.");}
            catch(IllegalArgumentException e) {error(e.getMessage());}
        });
    }
    private void addMaterial(String initial,boolean useAfter) {addMaterial(initial,useAfter,false);}
    private void addMaterial(String initial,boolean useAfter,boolean relock) {
        if(!requireAdmin()) return;
        LinearLayout l=dialogBody();
        EditText code=dialogField(l,"Scancode",initial);
        EditText number=dialogField(l,"Materialnummer",""); EditText description=dialogField(l,"Beschreibung","");
        CheckBox refrigerant=new CheckBox(this); refrigerant.setText("Kältemittel"); refrigerant.setTextSize(18); l.addView(refrigerant);
        EditText unit=dialogField(l,"Einheit (z. B. Stk oder kg)","Stk");
        refrigerant.setOnCheckedChangeListener((b,checked) -> unit.setText(checked ? "kg" : "Stk"));
        if(useAfter) refrigerant.setChecked("Kältemittel".equals(module));
        Spinner warehouse=warehouseDropdown(l,"Lager",settings.locations(),"");
        AlertDialog d=form("Material anlegen",l);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                if(!requireAdmin()) return;
                String c=Rules.required(code.getText().toString(),"Scancode");
                store.addMaterial(c,number.getText().toString(),description.getText().toString(),refrigerant.isChecked() ? "Kältemittel" : "Material",unit.getText().toString(),Rules.required(spinnerValue(warehouse),"Lager"));
                if(useAfter) {selected=store.material(c); module=selected.type; draftScan=c; draftFrom=selected.location; draftTo=person.location; draftAmount="1";draftKind="";screen="book";}
                if(relock) adminUnlocked=false;
                d.dismiss(); notice="✓ Material angelegt"; render();
            } catch(SQLiteConstraintException e) {code.setError("Dieser Scancode existiert bereits.");}
            catch(IllegalArgumentException e) {error(e.getMessage());}
        });
        if(relock) d.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {adminUnlocked=false;d.dismiss();render();});
        d.setOnCancelListener(v -> {if(relock) adminUnlocked=false;render();});
    }
    private void history(boolean archived) {
        if(!requireAdmin()) return;
        screen=archived ? "archive" : "history";
        base(archived ? "Archiv" : "Offene Bewegungen", store.count(archived)+" Einträge · Anzeige der letzten 100");
        button(archived ? "Archiv erneut exportieren" : "Bericht exportieren & archivieren",true,() -> beginExport(archived));
        button(archived ? "Offene Bewegungen anzeigen" : "Archiv anzeigen",false,() -> {screen=archived ? "history" : "archive"; notice=""; render();});
        List<String> entries=store.history(archived);
        if(entries.isEmpty()) text("Keine Bewegungen vorhanden.",18,MUTED);
        for(String s:entries) listCard(s);
    }
    private void beginExport(boolean archived) {
        if(!requireAdmin()) return;
        if(store.count(archived)==0) {error("Keine Bewegungen für diesen Bericht vorhanden."); return;}
        new AlertDialog.Builder(this).setTitle("Exportformat wählen")
            .setItems(archived ? new String[]{"Excel-Tabelle (.xlsx)","CSV (.csv)"} : new String[]{"Excel-Tabelle (.xlsx)","CSV (.csv)","Per Mail senden & archivieren"},(d,which) -> {if(which==2) localMailExport(new MailConfig(this),true);else exportFile(archived,which==0);}).setNegativeButton("Abbrechen",null).show();
    }
    private void exportFile(boolean archived,boolean excel) {
        if(!requireAdmin()) return;
        new AlertDialog.Builder(this).setTitle(archived ? "Archiv exportieren" : "Bericht erstellen")
            .setMessage(archived ? "Alle archivierten Bewegungen erneut speichern?" : "Alle offenen Bewegungen speichern? Erst nach erfolgreichem Speichern werden genau diese Bewegungen archiviert.")
            .setNegativeButton("Abbrechen",null).setPositiveButton("Speicherort wählen",(d,w) -> {
                if(!requireAdmin()) return;
                exportArchive=archived; exportExcel=excel; exportCutoff=archived ? store.archiveCutoff() : store.cutoff();
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType(excel ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv");
                i.putExtra(Intent.EXTRA_TITLE,"NLL-"+(archived ? "Archiv-" : "Bewegungen-")+new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.ROOT).format(new Date())+(excel ? ".xlsx" : ".csv"));
                try {pickerOpen=true; startActivityForResult(i,EXPORT);} catch(Exception e) {pickerOpen=false; error("Kein Dateidialog verfügbar.");}
            }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==UPDATE_PERMISSION || request==UPDATE_INSTALL) {
            notice="Android-Dialog beendet · bei Bedarf Schloss öffnen und Update erneut starten";render();return;
        }
        if(request==BACKUP_FOLDER || request==BACKUP_RESTORE) {
            pickerOpen=false;
            if(!adminUnlocked) {notice="Einstellungen gesperrt · Vorgang abgebrochen";render();return;}
            if(result!=RESULT_OK || data==null || data.getData()==null) {notice="Vorgang abgebrochen · keine Änderung";render();return;}
            if(request==BACKUP_FOLDER) {
                try {Backup.configure(this,data.getData(),data.getFlags());notice="Sicherungsordner gewählt";render();} catch(Exception e) {error(e.getMessage());}
            } else inspectBackup(data.getData());
            return;
        }
        if(request==MAIL_AUTH) {
            pickerOpen=false;
            if(!adminUnlocked || result!=RESULT_OK || data==null) {notice="Gmail-Verbindung abgebrochen. Einstellungen ggf. erneut entsperren.";render();return;}
            try {verifyMailAuthorization(com.google.android.gms.auth.api.identity.Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data));}
            catch(Exception e) {error("Gmail-Freigabe konnte nicht übernommen werden.");}return;
        }
        if((request==IMPORT || request==EXPORT) && !adminUnlocked) {pickerOpen=false; notice="Einstellungen gesperrt · Vorgang abgebrochen"; render(); return;}
        if(request==IMPORT) {
            pickerOpen=false;
            if(result==RESULT_OK && data!=null && data.getData()!=null) readImport(data.getData());
            return;
        }
        if(request!=EXPORT) return;
        pickerOpen=false;
        if(result!=RESULT_OK || data==null || data.getData()==null) {notice="Export abgebrochen · keine Archivierung"; render(); return;}
        if(exportCutoff<=0) {error("Exportauftrag fehlt. Bitte Bericht erneut starten. Es wurde nichts archiviert."); return;}
        Uri uri=data.getData(); writing=true;
        AlertDialog progress=new AlertDialog.Builder(this).setTitle("Bericht wird gespeichert …").setMessage("Bitte warten. Anschließend wird die Archivierung bestätigt.").setCancelable(false).create(); progress.show();
        final long cutoff=exportCutoff; final boolean archive=exportArchive; final boolean excel=exportExcel;
        new Thread(() -> {
            String message;
            try {
                int rows;
                try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")) {
                    if(stream==null) throw new IOException("Kein Ausgabestream");
                    if(excel) rows=store.writeXlsx(stream,cutoff,archive);
                    else try(BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(stream,StandardCharsets.UTF_8))) {
                        rows=store.writeCsv(writer,cutoff,archive);
                    }
                }
                if(!archive) {
                    try {store.archive(cutoff,UUID.randomUUID().toString());}
                    catch(Exception e) {throw new ArchiveException();}
                }
                message="✓ "+rows+" Bewegungen exportiert"+(archive ? "" : " und archiviert");
            } catch(ArchiveException e) {
                message="Bericht gespeichert, Archivierung fehlgeschlagen. Bewegungen bleiben offen; erneuter Export kann Duplikate erzeugen.";
            } catch(Exception e) {
                message="Export fehlgeschlagen · nichts archiviert. Speicherplatz und Ziel prüfen. Eine unvollständige Berichtsdatei ggf. löschen.";
            }
            final String outcome=message;
            runOnUiThread(() -> {writing=false; if(!isFinishing() && !isDestroyed()) {progress.dismiss(); notice=outcome; render();}});
        },"csv-export").start();
    }
    private void beginImport(boolean people) {
        if(!requireAdmin()) return;
        importPeople=people;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/octet-stream"});
        try {pickerOpen=true; startActivityForResult(i,IMPORT);} catch(Exception e) {pickerOpen=false; error("Kein Android-Dateidialog verfügbar.");}
    }
    private void readImport(Uri uri) {
        if(!requireAdmin()) return;
        writing=true;
        AlertDialog progress=new AlertDialog.Builder(this).setTitle("Excel wird geprüft …").setMessage("Die Datenbank wird noch nicht geändert.").setCancelable(false).create(); progress.show();
        final boolean people=importPeople;
        new Thread(() -> {
            java.io.File temporary=null;
            try {
                temporary=java.io.File.createTempFile("excel-import-",".xlsx",getCacheDir());
                try(java.io.InputStream in=getContentResolver().openInputStream(uri); java.io.FileOutputStream out=new java.io.FileOutputStream(temporary)) {
                    if(in==null) throw new IOException("Datei nicht lesbar.");
                    byte[] buffer=new byte[8192]; int n; long total=0;
                    while((n=in.read(buffer))!=-1) {total+=n; if(total>20*1024*1024) throw new IOException("Maximale Dateigröße: 20 MB."); out.write(buffer,0,n);}
                }
                List<String[]> rows=ExcelImport.read(temporary,people);
                runOnUiThread(() -> {
                    writing=false; if(isFinishing() || isDestroyed()) return; progress.dismiss();
                    String[] first=rows.get(0);
                    new AlertDialog.Builder(this).setTitle(rows.size()+" "+(people ? "Personen" : "Materialien")+" importieren?")
                        .setMessage("Erster Eintrag: "+first[0]+" · "+first[1]+"\n\nVorhandene Codes führen zum Abbruch des gesamten Imports. Bestehende Einträge bleiben unverändert.")
                        .setNegativeButton("Abbrechen",null).setPositiveButton("Importieren",(d,w) -> saveImport(rows,people)).show();
                });
            } catch(Exception e) {
                String message=e instanceof java.util.zip.ZipException ? "Bitte eine echte Excel-Datei (.xlsx) verwenden. Alte .xls-Dateien zuerst als .xlsx speichern." : e.getMessage();
                runOnUiThread(() -> {writing=false; if(!isFinishing() && !isDestroyed()) {progress.dismiss(); error("Import abgebrochen. Keine Daten geändert.\n\n"+message);}});
            } finally {if(temporary!=null) temporary.delete();}
        },"excel-read").start();
    }
    private void saveImport(List<String[]> rows,boolean people) {
        if(!requireAdmin()) return;
        writing=true;
        AlertDialog progress=new AlertDialog.Builder(this).setTitle("Daten werden importiert …").setCancelable(false).create(); progress.show();
        new Thread(() -> {
            String failure=null;
            try {store.importRows(rows,people);} catch(Exception e) {failure=e.getMessage();}
            final String errorMessage=failure;
            runOnUiThread(() -> {
                writing=false; if(isFinishing() || isDestroyed()) return; progress.dismiss();
                if(errorMessage!=null) error("Import vollständig zurückgerollt. Keine Daten geändert.\n\n"+errorMessage);
                else {notice="✓ "+rows.size()+" Einträge importiert"; render();}
            });
        },"excel-save").start();
    }
    private static class ArchiveException extends Exception { private static final long serialVersionUID=1L; }
    @Override protected void onDestroy() {
        if (store != null && !writing) store.close();
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if(writing || pickerOpen) return;
        if("book".equals(screen)) {
            new AlertDialog.Builder(this).setTitle("Buchung verlassen?").setMessage("Ungespeicherte Eingaben werden verworfen.")
                .setNegativeButton("Bleiben",null).setPositiveButton("Verlassen",(d,w) -> {selected=null; draftScan=""; draftAmount="1"; screen="browse"; notice=""; render();}).show();
        } else if("home".equals(screen)) {
            new AlertDialog.Builder(this).setTitle("Person abmelden?").setNegativeButton("Bleiben",null).setPositiveButton("Abmelden",(d,w) -> signOut()).show();
        } else if(isAdminScreen() && !"settings".equals(screen)) {screen="settings"; notice=""; render();}
        else if(!"login".equals(screen)) {screen=person==null ? "login" : "home"; notice=""; render();}
        else super.onBackPressed();
    }
}
