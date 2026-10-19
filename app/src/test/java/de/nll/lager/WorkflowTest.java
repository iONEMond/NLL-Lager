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
        db=new Store(RuntimeEnvironment.getApplication());
        db.addPerson("0007","Anna");
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
    private void press(String label) {
        View b=find(activity.getWindow().getDecorView(),label,false); assertNotNull(label,b); b.performClick();
    }
    private void scan(String label,String code) {EditText e=field(label); e.setText(code); e.onEditorAction(EditorInfo.IME_ACTION_DONE);}
    private void signInAndSelect() {scan("Personencode","0007"); scan("Material / Kältemittel scannen","00123");}
    @Test public void scannerLoginAndGermanDecimalBookingWorkEndToEnd() {
        signInAndSelect();
        field("Von Lager").setText("Lager A"); field("Nach Lager").setText("Fahrzeug 4"); field("Menge (Stk)").setText("1,25");
        assertEquals("1,25",field("Menge (Stk)").getText().toString());
        press("Bewegung speichern");
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog(); assertNotNull(dialog);
        assertEquals("Jetzt speichern",dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.shadowMainLooper().idle();
        assertEquals(1,db.count(false));
        assertTrue(db.history(false).get(0).contains("1.25 Stk")); assertTrue(db.history(false).get(0).contains("Anna"));
        assertEquals("",field("Material / Kältemittel scannen").getText().toString());
    }
    private void documentExport(boolean fail) throws Exception {
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
            if(!fail) {
                String csv=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(csv.contains("\"M-1\";\"Ventil\""));
                assertTrue(csv.contains("\"0007\";\"Anna\""));
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
        field("Von Lager").setText("A"); field("Nach Lager").setText("B");
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
        signInAndSelect(); field("Von Lager").setText("A"); field("Nach Lager").setText("B");
        field("Material / Kältemittel scannen").setText("different"); press("Bewegung speichern");
        AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("OK",dialog.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        assertEquals(0,db.count(false));
    }
}
