package de.nll.lager;

import android.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(qualifiers="de")
public class WorkflowTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    private Store db;
    @Before public void setup() {
        RuntimeEnvironment.getApplication().deleteDatabase("nll-lager.db");
        RuntimeEnvironment.getApplication().getSharedPreferences("admin-settings",0).edit().clear().commit();
        new AdminSettings(RuntimeEnvironment.getApplication()).saveLocations(java.util.Arrays.asList("A","Lager A","Lager","B","Fahrzeug","Fahrzeug 4"));
        db=new Store(RuntimeEnvironment.getApplication());
        db.addPerson("0007","Anna","0007-L");
        db.addMaterial("00123","M-1","Ventil","Material","Stk");
        controller=Robolectric.buildActivity(MainActivity.class).setup(); activity=controller.get();
    }
    @After public void cleanup() {controller.pause().stop().destroy(); db.close();}
    private View find(View view,String text,boolean description) {
        CharSequence label=description ? view.getContentDescription() : view instanceof Button ? ((Button)view).getText() : null;
        if(label!=null && text.contentEquals(label)) return view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View child=find(((ViewGroup)view).getChildAt(i),text,description); if(child!=null) return child;
        }
        return null;
    }
    private EditText field(String label) {
        EditText e=(EditText)find(activity.getWindow().getDecorView(),label,true); assertNotNull(label,e); return e;
    }
    private void chooseFrom(String value) {chooseWarehouse("Von Lager",value);chooseWarehouse("Bewegungsart","Umlagerung");}
    private void chooseTo(String value) {chooseWarehouse("Nach Lager",value);chooseWarehouse("Bewegungsart","Umlagerung");}
    private void chooseWarehouse(String label,String value) {
        android.widget.Spinner spinner=(android.widget.Spinner)find(activity.getWindow().getDecorView(),label,true); assertNotNull(spinner);
        for(int i=1;i<spinner.getCount();i++) if(value.equals(spinner.getItemAtPosition(i))) {spinner.setSelection(i); return;}
        fail("Lager fehlt: "+value);
    }
    private void unlockForTest() throws Exception {
        java.lang.reflect.Field f=MainActivity.class.getDeclaredField("adminUnlocked"); f.setAccessible(true); f.setBoolean(activity,true);
    }
    private void press(String label) {
        View b=find(activity.getWindow().getDecorView(),label,false); assertNotNull(label,b); b.performClick();
    }
    private void scan(String label,String code) {EditText e=field(label); e.setText(code); e.onEditorAction(EditorInfo.IME_ACTION_DONE);}
    private void signInAndSelect() {scan("Personencode","0007"); press("Material"); scan("Suchen oder Code scannen","00123");}
    private AlertDialog authorizeUnknown(String module,String code) throws Exception {
        new AdminSettings(activity).setPassword("Test-password-42".toCharArray());
        scan("Personencode","0007");press(module);scan("Suchen oder Code scannen",code);
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        AlertDialog auth=ShadowAlertDialog.getLatestAlertDialog();
        EditText password=(EditText)find(auth.getWindow().getDecorView(),"Passwort",true);
        password.setText("wrong");auth.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertFalse(isUnlocked());assertNull(db.material(code));
        password.setText("Test-password-42");auth.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        return ShadowAlertDialog.getLatestAlertDialog();
    }
    @Test public void unknownMaterialCanBeCreatedAfterPasswordAndReturnsToLockedBooking() throws Exception {
        AlertDialog d=authorizeUnknown("Material","NEW001");
        assertEquals("NEW001",((EditText)find(d.getWindow().getDecorView(),"Scancode",true)).getText().toString());
        ((EditText)find(d.getWindow().getDecorView(),"Materialnummer",true)).setText("M-new");
        ((EditText)find(d.getWindow().getDecorView(),"Beschreibung",true)).setText("Neuer Artikel");
        android.widget.Spinner warehouse=(android.widget.Spinner)find(d.getWindow().getDecorView(),"Lager",true);warehouse.setSelection(1);
        d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertNotNull(db.material("NEW001"));assertFalse(isUnlocked());assertEquals("NEW001",field("Material / Kältemittel scannen").getText().toString());
        assertNotNull(find(activity.getWindow().getDecorView(),"Bewegung speichern",false));assertEquals(0,db.count(false));
    }
    @Test public void unknownRefrigerantDefaultsToKmAndCancelRelocks() throws Exception {
        AlertDialog d=authorizeUnknown("Kältemittel","KMNEW");
        assertEquals("kg",((EditText)find(d.getWindow().getDecorView(),"Einheit (z. B. Stk oder kg)",true)).getText().toString());
        d.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertFalse(isUnlocked());assertNull(db.material("KMNEW"));
    }
    @Test public void exportChooserOffersMailOnlyForOpenReport() throws Exception {
        db.book(db.material("00123"),"A","B","1",db.person("0007"));
        unlockForTest();java.lang.reflect.Method export=MainActivity.class.getDeclaredMethod("beginExport",boolean.class);export.setAccessible(true);export.invoke(activity,false);
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        AlertDialog d=ShadowAlertDialog.getLatestAlertDialog();assertEquals(3,d.getListView().getAdapter().getCount());
        assertEquals("Per Mail senden & archivieren",d.getListView().getAdapter().getItem(2));
    }
    @Test public void movementKindIsRequiredAndStoredWithBooking() {
        signInAndSelect();chooseWarehouse("Von Lager","A");chooseWarehouse("Nach Lager","B");
        press("Bewegung speichern");assertEquals(0,db.count(false));
        AlertDialog missing=ShadowAlertDialog.getLatestAlertDialog();assertEquals("OK",missing.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());missing.dismiss();
        chooseWarehouse("Bewegungsart","Entnahme");press("Bewegung speichern");
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        try(android.database.Cursor c=db.getReadableDatabase().rawQuery("SELECT movement_kind FROM movements",null)) {assertTrue(c.moveToFirst());assertEquals("Entnahme",c.getString(0));}
    }
    @Test public void updatedEmployeeWarehouseAppliesToNextArticleSelection() {
        scan("Personencode","0007");db.assignLocation("0007","NEU",true);press("Material");scan("Suchen oder Code scannen","00123");
        android.widget.Spinner target=(android.widget.Spinner)find(activity.getWindow().getDecorView(),"Nach Lager",true);
        assertEquals("NEU",target.getSelectedItem());
    }
    @Test public void mailSettingsAreProtectedAndRelockWithApp() throws Exception {
        java.lang.reflect.Method menu=MainActivity.class.getDeclaredMethod("mailMenu");menu.setAccessible(true);
        menu.invoke(activity);assertNull(find(activity.getWindow().getDecorView(),"Postfach & Empfänger einrichten",false));
        AlertDialog denied=ShadowAlertDialog.getLatestAlertDialog();if(denied!=null) denied.dismiss();
        unlockForTest();menu.invoke(activity);assertNotNull(find(activity.getWindow().getDecorView(),"Postfach & Empfänger einrichten",false));
        controller.pause().stop().start().resume();assertNull(find(activity.getWindow().getDecorView(),"Postfach & Empfänger einrichten",false));
    }
    @Test public void scannerLoginAndGermanDecimalBookingWorkEndToEnd() {
        signInAndSelect();
        chooseFrom("Lager A"); chooseTo("Fahrzeug 4"); field("Menge (Stk)").setText("1,25");
        assertEquals("1,25",field("Menge (Stk)").getText().toString());
        press("Bewegung speichern");
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog(); assertNotNull(dialog);
        assertEquals("Jetzt speichern",dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
        assertEquals(1,db.count(false));
        assertTrue(db.history(false).get(0).contains("1.25 Stk")); assertTrue(db.history(false).get(0).contains("Anna"));
        assertNotNull(field("Suchen oder Code scannen"));
    }
    @Test public void homeSeparatesMaterialAndRefrigerantAndSupportsArticleSelection() {
        db.addMaterial("00999","KM-32","R32","Kältemittel","kg");
        scan("Personencode","0007");
        assertNotNull(find(activity.getWindow().getDecorView(),"Material",false));
        assertNotNull(find(activity.getWindow().getDecorView(),"Kältemittel",false));
        press("Kältemittel");
        assertNull(find(activity.getWindow().getDecorView(),"Ventil, M-1",true));
        View article=find(activity.getWindow().getDecorView(),"R32, KM-32",true); assertNotNull(article); article.performClick();
        assertNotNull(field("Menge (kg)"));
        chooseFrom("Lager"); chooseTo("Fahrzeug"); field("Menge (kg)").setText("2,5");
        chooseWarehouse("Bewegungsart","Umlagerung");
        press("Bewegung speichern"); ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
        assertTrue(db.history(false).get(0).contains("2.5 kg"));
        assertNotNull(field("Suchen oder Code scannen"));
    }
    @Test public void articleSearchFiltersDescriptionAndMaterialNumber() {
        scan("Personencode","0007"); press("Material");
        field("Suchen oder Code scannen").setText("Vent");
        assertNotNull(find(activity.getWindow().getDecorView(),"Ventil, M-1",true));
        field("Suchen oder Code scannen").setText("missing");
        assertNull(find(activity.getWindow().getDecorView(),"Ventil, M-1",true));
        field("Suchen oder Code scannen").setText("M-1");
        assertNotNull(find(activity.getWindow().getDecorView(),"Ventil, M-1",true));
    }
    private void invoke(String method,Class<?>[] types,Object... args) throws Exception {
        java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(method,types); m.setAccessible(true); m.invoke(activity,args);
    }
    private boolean isUnlocked() throws Exception {
        java.lang.reflect.Field f=MainActivity.class.getDeclaredField("adminUnlocked"); f.setAccessible(true); return f.getBoolean(activity);
    }
    @Test public void articleAndEmployeeWarehousesAreSelectedForEveryNewMovement() {
        db.assignLocation("00123","Lager A",false);
        signInAndSelect();
        assertEquals("Lager A",((android.widget.Spinner)find(activity.getWindow().getDecorView(),"Von Lager",true)).getSelectedItem());
        assertEquals("0007-L",((android.widget.Spinner)find(activity.getWindow().getDecorView(),"Nach Lager",true)).getSelectedItem());
        chooseTo("Fahrzeug"); press("Bewegung speichern");
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
        scan("Suchen oder Code scannen","00123");
        assertEquals("0007-L",((android.widget.Spinner)find(activity.getWindow().getDecorView(),"Nach Lager",true)).getSelectedItem());
    }
    @Test public void normalModeOnlyOffersMovementFunctions() {
        assertNull(find(activity.getWindow().getDecorView(),"Personen verwalten",false));
        scan("Personencode","0007");
        for(String label:new String[]{"Personen verwalten","Stammdaten verwalten","Bericht exportieren","Bewegungen & Archiv","Einstellungen"})
            assertNull(label,find(activity.getWindow().getDecorView(),label,false));
        press("Material");
        assertNull(find(activity.getWindow().getDecorView(),"Stammdaten verwalten",false));
        scan("Suchen oder Code scannen","00123");
        assertTrue(find(activity.getWindow().getDecorView(),"Von Lager",true) instanceof android.widget.Spinner);
        assertNull(find(activity.getWindow().getDecorView(),"Material verwalten",false));
    }
    @Test public void lockedEntryPointsRejectImportExportAndPersonCreation() throws Exception {
        invoke("beginImport",new Class<?>[]{boolean.class},true);
        invoke("beginExport",new Class<?>[]{boolean.class},false);
        invoke("addPerson",new Class<?>[]{});
        invoke("saveImport",new Class<?>[]{java.util.List.class,boolean.class},java.util.Collections.singletonList(new String[]{"blocked","Blocked"}),true);
        assertNull(db.person("blocked"));
        assertNull(org.robolectric.Shadows.shadowOf(activity).getNextStartedActivity());
        db.book(db.material("00123"),"A","B","1",db.person("0007"));
        activity.onActivityResult(101,android.app.Activity.RESULT_OK,new android.content.Intent().setData(android.net.Uri.parse("file:///not-written.csv")));
        assertEquals(1,db.count(false)); assertEquals(0,db.count(true));
    }
    @Test public void wrongPasswordStaysLockedAndCorrectPasswordOpensSettings() throws Exception {
        AdminSettings settings=new AdminSettings(activity); settings.setPassword("Test-password-42".toCharArray());
        find(activity.getWindow().getDecorView(),"Einstellungen entsperren",true).performClick();
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
        EditText password=(EditText)find(dialog.getWindow().getDecorView(),"Passwort",true); password.setText("wrong");
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); assertFalse(isUnlocked()); assertTrue(dialog.isShowing());
        password.setText("Test-password-42"); dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        assertTrue(isUnlocked()); assertNotNull(find(activity.getWindow().getDecorView(),"Lageroptionen verwalten",false));
        find(activity.getWindow().getDecorView(),"Einstellungen sperren",true).performClick();
        assertFalse(isUnlocked()); assertNull(find(activity.getWindow().getDecorView(),"Lageroptionen verwalten",false));
    }
    @Test public void returningFromBackgroundRelocksSettings() throws Exception {
        unlockForTest(); invoke("settingsMenu",new Class<?>[]{});
        controller.pause().stop().start().resume().visible();
        assertFalse(isUnlocked());
        assertNull(find(activity.getWindow().getDecorView(),"Lageroptionen verwalten",false));
    }
    @Test public void removedSourceCannotBeBookedFromStaleDropdown() {
        signInAndSelect(); chooseFrom("A"); chooseTo("B");
        new AdminSettings(activity).saveLocations(java.util.Collections.singletonList("Lager"));
        press("Bewegung speichern");
        assertEquals(0,db.count(false));
        assertEquals("OK",ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
    }
    @Test public void successfulExcelExportArchivesOnlyAfterCompletedFile() throws Exception {
        java.lang.reflect.Field format=MainActivity.class.getDeclaredField("exportExcel"); format.setAccessible(true); format.setBoolean(activity,true);
        documentExport(false);
    }
    @Test public void failedExcelExportKeepsMovementsOpen() throws Exception {
        java.lang.reflect.Field format=MainActivity.class.getDeclaredField("exportExcel"); format.setAccessible(true); format.setBoolean(activity,true);
        documentExport(true);
    }
    private void documentExport(boolean fail) throws Exception {
        unlockForTest();
        db.book(db.material("00123"),"A","B","2.5",db.person("0007"));
        java.lang.reflect.Field cutoff=MainActivity.class.getDeclaredField("exportCutoff");
        cutoff.setAccessible(true); cutoff.setLong(activity,db.cutoff());
        java.io.File file=fail ? new java.io.File(activity.getCacheDir(),"missing-"+System.nanoTime()+"/report.csv") : java.io.File.createTempFile("report-",".csv",activity.getCacheDir());
        try {
            activity.onActivityResult(101,android.app.Activity.RESULT_OK,new android.content.Intent().setData(android.net.Uri.fromFile(file)));
            java.lang.reflect.Field writing=MainActivity.class.getDeclaredField("writing"); writing.setAccessible(true);
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(15);
            do {
                org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
                if(!writing.getBoolean(activity)) break;
                Thread.sleep(20);
            } while(System.nanoTime()<deadline);
            org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
            assertFalse("Export completed",writing.getBoolean(activity));
            assertEquals(fail ? 1 : 0,db.count(false)); assertEquals(fail ? 0 : 1,db.count(true));
            java.lang.reflect.Field format=MainActivity.class.getDeclaredField("exportExcel"); format.setAccessible(true);
            if(!fail && format.getBoolean(activity)) {
                try(java.util.zip.ZipFile zip=new java.util.zip.ZipFile(file)) { assertNotNull(zip.getEntry("xl/worksheets/sheet1.xml")); }
            }
            if(!fail && !format.getBoolean(activity)) {
                String csv=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(csv.contains("\"00123\";\"Ventil\""));
                assertTrue(csv.contains("\"Anna\""));
                assertTrue(csv.contains("\"2,5\""));
            }
        } finally {file.delete();}
    }
    @Test public void successfulFileExportArchivesMovements() throws Exception { documentExport(false); }
    @Test public void failedFileExportKeepsMovementsOpen() throws Exception { documentExport(true); }
    @Test public void cancelledFilePickerKeepsMovementsOpen() {
        db.book(db.material("00123"),"A","B","1",db.person("0007"));
        activity.onActivityResult(101,android.app.Activity.RESULT_CANCELED,null);
        assertEquals(1,db.count(false)); assertEquals(0,db.count(true));
    }
    @Test @Config(qualifiers="en") public void decimalCommaIsPreservedOnEnglishScannerSettings() {
        signInAndSelect();
        chooseFrom("A"); chooseTo("B");
        field("Menge (Stk)").setText("1,25");
        assertEquals("1,25",field("Menge (Stk)").getText().toString());
        press("Bewegung speichern");
        ShadowAlertDialog.getLatestAlertDialog().getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
        assertEquals(1,db.count(false)); assertTrue(db.history(false).get(0).contains("1.25 Stk"));
    }
    @Test public void unknownPersonCannotSignIn() {
        scan("Personencode","unknown");
        assertNotNull(find(activity.getWindow().getDecorView(),"Personencode",true));
        assertNull(find(activity.getWindow().getDecorView(),"Material / Kältemittel scannen",true));
        assertEquals(0,db.count(false));
    }
    @Test public void changedCodeCannotBookPreviouslySelectedMaterial() {
        signInAndSelect(); chooseFrom("A"); chooseTo("B");
        field("Material / Kältemittel scannen").setText("different"); press("Bewegung speichern");
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("OK",dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(0,db.count(false));
    }
}
