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
    private static final int EXPORT = 101, IMPORT = 102;
    private static final int INK = Color.rgb(18,51,47), GREEN = Color.rgb(8,123,101), MUTED = Color.rgb(83,106,100);
    private Store store;
    private Store.Person person;
    private Store.Material selected;
    private LinearLayout body;
    private EditText scan, from, to, amount;
    private SharedPreferences prefs;
    private String screen = "login", notice = "";
    private long exportCutoff;
    private boolean exportArchive, pickerOpen, writing, importPeople;
    private String draftFrom = "", draftTo = "", draftAmount = "1", draftScan = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store(this); prefs = getSharedPreferences("preferences", MODE_PRIVATE);
        draftFrom = prefs.getString("from", ""); draftTo = prefs.getString("to", "");
        if (state != null) {
            person = store.person(state.getString("person", ""));
            selected = store.material(state.getString("material", ""));
            screen = state.getString("screen", "login");
            exportCutoff = state.getLong("cutoff"); exportArchive = state.getBoolean("archive");
            pickerOpen = state.getBoolean("picker"); importPeople = state.getBoolean("importPeople");
            draftFrom = state.getString("from", draftFrom); draftTo = state.getString("to", draftTo);
            draftAmount = state.getString("amount", "1"); draftScan = state.getString("scan", "");
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        captureDraft();
        state.putString("person", person == null ? "" : person.code);
        state.putString("material", selected == null ? "" : selected.code);
        state.putString("screen", screen); state.putLong("cutoff", exportCutoff);
        state.putBoolean("importPeople", importPeople);
        state.putBoolean("archive", exportArchive); state.putBoolean("picker", pickerOpen);
        state.putString("from", draftFrom); state.putString("to", draftTo);
        state.putString("amount", draftAmount); state.putString("scan", draftScan);
        super.onSaveInstanceState(state);
    }
    private void captureDraft() {
        if ("book".equals(screen) && from != null) {
            draftFrom = from.getText().toString(); draftTo = to.getText().toString();
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
        from = null; to = null; amount = null; scan = null;
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setFitsSystemWindows(true);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20), dp(20), dp(20), dp(28));
        scroll.addView(body); setContentView(scroll);
        TextView brand = text("NLL / LAGER", 14, MUTED); brand.setTypeface(null, Typeface.BOLD);
        text(title, 30, INK).setTypeface(null, Typeface.BOLD);
        text(subtitle, 16, MUTED);
        if (!notice.isEmpty()) {
            boolean failed = notice.contains("fehlgeschlagen");
            TextView n = text(notice, 17, failed ? Color.rgb(151,47,32) : GREEN); n.setPadding(dp(12),dp(12),dp(12),dp(12));
            n.setBackground(shape(failed ? Color.rgb(255,231,224) : Color.rgb(218,239,230),0));
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
        b.setBackground(shape(primary ? GREEN : Color.WHITE, primary ? 0 : Color.rgb(202,216,210)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin=dp(12); body.addView(b,p);
        b.setOnClickListener(v -> { if (!writing && !pickerOpen) action.run(); }); return b;
    }
    private EditText field(String label, String value, int type) {
        text(label,16,INK).setTypeface(null,Typeface.BOLD);
        EditText e = new EditText(this); e.setSingleLine(true); e.setTextSize(21); e.setTextColor(INK);
        e.setInputType(type); e.setText(value); e.setMinHeight(dp(58)); e.setSelectAllOnFocus(true);
        e.setPadding(dp(12),dp(10),dp(12),dp(10)); e.setBackground(shape(Color.WHITE,Color.rgb(174,195,185)));
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
        if (person==null && !"people".equals(screen)) screen="login";
        switch (screen) {
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
        base("Person anmelden", "Offline bereit · Daten bleiben auf diesem Gerät");
        text("Persönlichen Code scannen und mit Enter bestätigen.",18,INK);
        scan=scanner("Personencode", "", this::signIn);
        button("Anmelden",true,this::signIn);
        button("Personen verwalten",false,() -> {screen="people"; notice=""; render();});
        text("Erster Start? Zuerst eine Person mit ihrem vorhandenen Scancode anlegen. Der Code dient zur Zuordnung, nicht als sichere Authentifizierung.",15,MUTED);
    }
    private void signIn() {
        try {
            String code=Rules.required(scan.getText().toString(),"Personencode");
            Store.Person found=store.person(code);
            if (found==null) { error("Personencode unbekannt. Bitte zuerst unter „Personen verwalten“ anlegen."); return; }
            hideKeyboard(); person=found; screen="book"; notice=""; draftScan=""; render();
        } catch (IllegalArgumentException e) { error(e.getMessage()); }
    }
    private void booking() {
        screen="book";
        base("Bewegung buchen", "Angemeldet: " + person.name);
        long pending=store.count(false);
        text(pending + " offene Bewegungen · " + store.count(true) + " archiviert",16,MUTED);
        scan=scanner("Material / Kältemittel scannen",draftScan,this::lookup);
        button("Code übernehmen",false,this::lookup);
        if (selected==null) text("Vorhandenen Artikelcode scannen. Unbekannte Codes können anschließend angelegt werden.",17,MUTED);
        else {
            TextView card=text(selected.number + " · " + selected.description + "\n" + selected.type + " / " + selected.unit,21,INK);
            card.setTypeface(null,Typeface.BOLD);
        }
        from=field("Von Lager",draftFrom,InputType.TYPE_CLASS_TEXT);
        to=field("Nach Lager",draftTo,InputType.TYPE_CLASS_TEXT);
        amount=field("Menge" + (selected==null ? "" : " ("+selected.unit+")"),"",InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        // Accept both decimal separators even on scanners configured with an English system locale.
        amount.setKeyListener(DigitsKeyListener.getInstance("0123456789.,"));
        amount.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setText(draftAmount);
        amount.setImeOptions(EditorInfo.IME_ACTION_DONE);
        amount.setOnEditorActionListener((v,a,event) -> { if(a==EditorInfo.IME_ACTION_DONE) {hideKeyboard(); return true;} return false; });
        button("Bewegung speichern",true,this::confirmBooking);
        button("Bericht exportieren (" + pending + ")",false,() -> {captureDraft(); beginExport(false);});
        button("Bewegungen & Archiv",false,() -> {captureDraft(); screen="history"; notice=""; render();});
        button("Material verwalten",false,() -> {captureDraft(); screen="materials"; notice=""; render();});
        button("Person wechseln / abmelden",false,() -> {captureDraft(); person=null; selected=null; draftScan=""; draftAmount="1"; notice=""; render();});
    }
    private void lookup() {
        try {
            String code=Rules.required(scan.getText().toString(),"Scancode");
            captureDraft(); hideKeyboard();
            Store.Material material=store.material(code);
            if (material==null) {
                selected=null;
                new AlertDialog.Builder(this).setTitle("Unbekannter Code").setMessage("Code „"+code+"“ als Material oder Kältemittel anlegen?")
                    .setPositiveButton("Anlegen",(d,w) -> addMaterial(code,true)).setNegativeButton("Abbrechen",(d,w) -> render()).show();
            } else { selected=material; draftScan=code; notice=""; render(); }
        } catch (IllegalArgumentException e) { error(e.getMessage()); }
    }
    private void confirmBooking() {
        try {
            if (selected==null) throw new IllegalArgumentException("Bitte zuerst einen bekannten Materialcode übernehmen.");
            if (!scan.getText().toString().trim().equals(selected.code)) throw new IllegalArgumentException("Der Code wurde geändert. Bitte zuerst „Code übernehmen“ drücken.");
            captureDraft();
            final String f=Rules.required(draftFrom,"Von Lager"), t=Rules.required(draftTo,"Nach Lager"), q=Rules.quantity(draftAmount);
            Rules.locations(f,t); hideKeyboard();
            new AlertDialog.Builder(this).setTitle("Bewegung bestätigen")
                .setMessage(selected.number+" · "+selected.description+"\n\n"+q+" "+selected.unit+"\n"+f+" → "+t+"\nPerson: "+person.name)
                .setNegativeButton("Zurück",null).setPositiveButton("Jetzt speichern",(d,w) -> {
                    try {
                        store.book(selected,f,t,q,person);
                        prefs.edit().putString("from",f).putString("to",t).apply();
                        notice="✓ Bewegung gespeichert: "+q+" "+selected.unit;
                        selected=null; draftScan=""; draftAmount="1"; render();
                    } catch (Exception e) { error("Bewegung konnte nicht gespeichert werden. Bitte erneut versuchen."); }
                }).show();
        } catch (IllegalArgumentException e) { error(e.getMessage()); }
    }
    private void catalog(boolean people) {
        screen=people ? "people" : "materials";
        base(people ? "Personen" : "Material & Kältemittel", "Vorhandene Codes einmalig zuordnen · Anzeige max. 200");
        button(people ? "+ Person anlegen" : "+ Material anlegen",true,() -> { if(people) addPerson(); else addMaterial("",false); });
        button("Aus Excel importieren (.xlsx)",false,() -> beginImport(people));
        text(people ? "Excel-Spalten: Personencode, Name" : "Excel-Spalten: Scancode, Mat Nummer, Beschreibung, Art, Einheit",15,MUTED);
        text("Erste Tabelle, erste Zeile = Überschriften. Codes als Text speichern, damit führende Nullen erhalten bleiben. Vorhandene Codes werden nicht überschrieben.",15,MUTED);
        button("Zurück",false,() -> {screen=person==null ? "login" : "book"; notice=""; render();});
        List<String> entries=store.catalog(people);
        if(entries.isEmpty()) text("Noch keine Einträge vorhanden.",18,MUTED);
        for(String s:entries) listCard(s);
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
        LinearLayout l=dialogBody();
        EditText code=dialogField(l,"Personencode scannen",""); EditText name=dialogField(l,"Name","");
        AlertDialog d=form("Person anlegen",l);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try { store.addPerson(code.getText().toString(),name.getText().toString()); d.dismiss(); notice="✓ Person angelegt"; render(); }
            catch(SQLiteConstraintException e) {code.setError("Dieser Code existiert bereits.");}
            catch(IllegalArgumentException e) {error(e.getMessage());}
        });
    }
    private void addMaterial(String initial,boolean useAfter) {
        LinearLayout l=dialogBody();
        EditText code=dialogField(l,"Scancode",initial);
        EditText number=dialogField(l,"Materialnummer",""); EditText description=dialogField(l,"Beschreibung","");
        CheckBox refrigerant=new CheckBox(this); refrigerant.setText("Kältemittel"); refrigerant.setTextSize(18); l.addView(refrigerant);
        EditText unit=dialogField(l,"Einheit (z. B. Stk oder kg)","Stk");
        refrigerant.setOnCheckedChangeListener((b,checked) -> unit.setText(checked ? "kg" : "Stk"));
        AlertDialog d=form("Material anlegen",l);
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String c=Rules.required(code.getText().toString(),"Scancode");
                store.addMaterial(c,number.getText().toString(),description.getText().toString(),refrigerant.isChecked() ? "Kältemittel" : "Material",unit.getText().toString());
                if(useAfter) {selected=store.material(c); draftScan=c;}
                d.dismiss(); notice="✓ Material angelegt"; render();
            } catch(SQLiteConstraintException e) {code.setError("Dieser Scancode existiert bereits.");}
            catch(IllegalArgumentException e) {error(e.getMessage());}
        });
        d.setOnCancelListener(v -> render());
    }
    private void history(boolean archived) {
        screen=archived ? "archive" : "history";
        base(archived ? "Archiv" : "Offene Bewegungen", store.count(archived)+" Einträge · Anzeige der letzten 100");
        button(archived ? "Archiv erneut exportieren" : "Bericht exportieren & archivieren",true,() -> beginExport(archived));
        button(archived ? "Offene Bewegungen anzeigen" : "Archiv anzeigen",false,() -> {screen=archived ? "history" : "archive"; notice=""; render();});
        button("Zurück zur Buchung",false,() -> {screen="book"; notice=""; render();});
        List<String> entries=store.history(archived);
        if(entries.isEmpty()) text("Keine Bewegungen vorhanden.",18,MUTED);
        for(String s:entries) listCard(s);
    }
    private void beginExport(boolean archived) {
        if(store.count(archived)==0) {error("Keine Bewegungen für diesen Bericht vorhanden."); return;}
        new AlertDialog.Builder(this).setTitle(archived ? "Archiv exportieren" : "Bericht erstellen")
            .setMessage(archived ? "Alle archivierten Bewegungen erneut als CSV speichern?" : "Alle offenen Bewegungen als CSV speichern? Nach erfolgreichem Speichern werden genau diese Bewegungen archiviert. Bei Abbruch bleibt alles offen.")
            .setNegativeButton("Abbrechen",null).setPositiveButton("Speicherort wählen",(d,w) -> {
                exportArchive=archived; exportCutoff=archived ? store.archiveCutoff() : store.cutoff();
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("text/csv");
                i.putExtra(Intent.EXTRA_TITLE,"NLL-"+(archived ? "Archiv-" : "Bewegungen-")+new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss",Locale.ROOT).format(new Date())+".csv");
                try {pickerOpen=true; startActivityForResult(i,EXPORT);} catch(Exception e) {pickerOpen=false; error("Kein Dateidialog verfügbar. Bitte einen Android-Dokumentanbieter aktivieren.");}
            }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
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
        final long cutoff=exportCutoff; final boolean archive=exportArchive;
        new Thread(() -> {
            String message;
            try {
                int rows;
                try(OutputStream stream=getContentResolver().openOutputStream(uri,"wt")) {
                    if(stream==null) throw new IOException("Kein Ausgabestream");
                    try(BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(stream,StandardCharsets.UTF_8))) {
                        rows=store.writeCsv(writer,cutoff,archive);
                    }
                }
                if(!archive) {
                    try {store.archive(cutoff,UUID.randomUUID().toString());}
                    catch(Exception e) {throw new ArchiveException();}
                }
                message="✓ "+rows+" Bewegungen exportiert"+(archive ? "" : " und archiviert");
            } catch(ArchiveException e) {
                message="CSV gespeichert, Archivierung fehlgeschlagen. Bewegungen bleiben offen; erneuter Export kann Duplikate erzeugen.";
            } catch(Exception e) {
                message="Export fehlgeschlagen · nichts archiviert. Speicherplatz und Ziel prüfen. Eine unvollständige CSV ggf. löschen.";
            }
            final String outcome=message;
            runOnUiThread(() -> {writing=false; if(!isFinishing() && !isDestroyed()) {progress.dismiss(); notice=outcome; render();}});
        },"csv-export").start();
    }
    private void beginImport(boolean people) {
        importPeople=people;
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*"); i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/octet-stream"});
        try {pickerOpen=true; startActivityForResult(i,IMPORT);} catch(Exception e) {pickerOpen=false; error("Kein Android-Dateidialog verfügbar.");}
    }
    private void readImport(Uri uri) {
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
        if(!"login".equals(screen) && !"book".equals(screen)) {screen=person==null ? "login" : "book"; notice=""; render();}
        else if("book".equals(screen)) {
            new AlertDialog.Builder(this).setTitle("Person abmelden?").setMessage("Ungespeicherte Eingaben werden verworfen.")
                .setNegativeButton("Bleiben",null).setPositiveButton("Abmelden",(d,w) -> {person=null; selected=null; draftScan=""; draftAmount="1"; notice=""; render();}).show();
        } else super.onBackPressed();
    }
}
