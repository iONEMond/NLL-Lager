package de.nll.lager;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.EditText;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FlowTest {
    private View byDescription(View v,String description) {
        if(description.contentEquals(v.getContentDescription()==null ? "" : v.getContentDescription())) return v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            View found=byDescription(((ViewGroup)v).getChildAt(i),description); if(found!=null) return found;
        }
        return null;
    }
    private Button button(View v,String label) {
        if(v instanceof Button && label.contentEquals(((Button)v).getText())) return (Button)v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            Button found=button(((ViewGroup)v).getChildAt(i),label); if(found!=null) return found;
        }
        return null;
    }
    @Test public void documentExportArchivesOnlyAfterSuccessfulWrite() throws Exception {
        documentExport(false);
    }
    @Test public void failedDocumentExportDoesNotArchive() throws Exception {
        documentExport(true);
    }
    private void documentExport(boolean fail) throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation(); Context c=i.getTargetContext();
        c.deleteDatabase("nll-lager.db"); Store db=new Store(c);
        db.addPerson("P1","Anna"); db.addMaterial("C1","M1","Ventil","Material","Stk");
        db.book(db.material("C1"),"A","B","1",db.person("P1"));
        MainActivity a=(MainActivity)i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        java.io.File file=fail ? new java.io.File(c.getCacheDir(),"missing-"+System.nanoTime()+"/report.csv") : java.io.File.createTempFile("report-",".csv",c.getCacheDir());
        try {
            java.lang.reflect.Field admin=MainActivity.class.getDeclaredField("adminUnlocked"); admin.setAccessible(true); admin.setBoolean(a,true);
            java.lang.reflect.Field cutoff=MainActivity.class.getDeclaredField("exportCutoff"); cutoff.setAccessible(true); cutoff.setLong(a,db.cutoff());
            java.lang.reflect.Field writing=MainActivity.class.getDeclaredField("writing"); writing.setAccessible(true);
            i.runOnMainSync(() -> a.onActivityResult(101,Activity.RESULT_OK,new Intent().setData(android.net.Uri.fromFile(file))));
            for(int attempt=0;attempt<120;attempt++) {
                i.waitForIdleSync(); if(!writing.getBoolean(a)) break; SystemClock.sleep(250);
            }
            assertFalse("Export completed",writing.getBoolean(a));
            assertEquals(fail ? 1 : 0,db.count(false)); assertEquals(fail ? 0 : 1,db.count(true));
            if(!fail) {
                String csv=new String(java.nio.file.Files.readAllBytes(file.toPath()),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(csv.contains("\"M1\";\"Ventil\"")); assertTrue(csv.contains("\"P1\";\"Anna\""));
            }
        } finally {i.runOnMainSync(a::finish); i.waitForIdleSync(); db.close(); c.deleteDatabase("nll-lager.db"); file.delete();}
    }
    @Test public void keyboardScannerSignsInSelectsMaterialAndBooksMovement() throws Exception {
        Instrumentation i=InstrumentationRegistry.getInstrumentation(); Context c=i.getTargetContext();
        c.deleteDatabase("nll-lager.db"); Store db=new Store(c);
        new AdminSettings(c).saveLocations(java.util.Arrays.asList("Lager A","Fahrzeug 4"));
        db.addPerson("0007","Anna"); db.addMaterial("00123","M-1","Ventil","Material","Stk");
        Activity a=i.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            i.runOnMainSync(() -> {EditText e=(EditText)byDescription(a.getWindow().getDecorView(),"Personencode"); assertNotNull(e); e.requestFocus(); e.setText("0007");});
            i.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER); i.waitForIdleSync();
            i.runOnMainSync(() -> button(a.getWindow().getDecorView(),"Material").performClick());
            i.waitForIdleSync();
            i.runOnMainSync(() -> {EditText e=(EditText)byDescription(a.getWindow().getDecorView(),"Suchen oder Code scannen"); assertNotNull(e); e.requestFocus(); e.setText("00123");});
            i.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER); i.waitForIdleSync();
            i.runOnMainSync(() -> {
                View root=a.getWindow().getDecorView();
                ((android.widget.Spinner)byDescription(root,"Von Lager")).setSelection(1);
                ((android.widget.Spinner)byDescription(root,"Nach Lager")).setSelection(2);
                ((EditText)byDescription(root,"Menge (Stk)")).setText("2,5");
                Button save=button(root,"Bewegung speichern"); assertNotNull(save); save.performClick();
            });
            i.waitForIdleSync();
            boolean confirmed=false;
            for(int attempt=0;attempt<20 && !confirmed;attempt++) {
                AccessibilityNodeInfo root=i.getUiAutomation().getRootInActiveWindow();
                if(root!=null) {
                    List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText("Jetzt speichern");
                    for(AccessibilityNodeInfo n:nodes) if(n.isClickable()) confirmed=n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                if(!confirmed) SystemClock.sleep(250);
            }
            assertTrue("Booking confirmation visible and clickable",confirmed);
            i.waitForIdleSync();
            for(int attempt=0;attempt<20 && db.count(false)==0;attempt++) SystemClock.sleep(250);
            assertEquals(1,db.count(false));
            assertTrue(db.history(false).get(0).contains("2.5 Stk"));
            assertTrue(db.history(false).get(0).contains("Anna"));
        } finally {
            i.runOnMainSync(a::finish); i.waitForIdleSync(); db.close(); c.deleteDatabase("nll-lager.db");
        }
    }
}
