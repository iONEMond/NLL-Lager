package de.nll.lager;

import android.app.Activity;
import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class ProgressTest {
    @Test public void importAndBothReportsEmitActualRecordCounts() throws Exception {
        android.content.Context app=RuntimeEnvironment.getApplication();app.deleteDatabase("nll-lager.db");
        try(Store store=new Store(app)) {
            List<String> updates=new ArrayList<>();WorkProgress progress=(done,total,message) -> updates.add(done+"/"+total);
            store.importRows(Arrays.asList(new String[]{"1","Anna"},new String[]{"2","Ben"}),true,progress);
            assertTrue(updates.contains("1/2"));assertTrue(updates.contains("2/2"));
            store.addMaterial("M","M","Ventil","Material","Stk");
            store.book(store.material("M"),"A","B","1",store.person("1"));
            store.book(store.material("M"),"A","B","2",store.person("2"));
            updates.clear();store.writeXlsx(new ByteArrayOutputStream(),store.cutoff(),false,progress);
            assertTrue(updates.contains("1/2"));assertTrue(updates.contains("2/2"));
            updates.clear();store.writeCsv(new StringWriter(),store.cutoff(),false,progress);
            assertTrue(updates.contains("1/2"));assertTrue(updates.contains("2/2"));
            assertEquals(2,store.count(false));assertEquals(0,store.count(true));
        } finally {app.deleteDatabase("nll-lager.db");}
    }
    @Test public void visibleBarSwitchesBetweenUnknownPhaseAndRecordCountAndCloses() {
        org.robolectric.android.controller.ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup();
        OperationProgress progress=new OperationProgress(controller.get(),"Import","Datei öffnen");
        assertTrue(progress.dialog.isShowing());assertTrue(progress.bar.isIndeterminate());
        progress.update(1,4,"Einträge übernehmen");assertFalse(progress.bar.isIndeterminate());assertEquals(25,progress.bar.getProgress());
        assertTrue(progress.status.getText().toString().contains("1 / 4"));
        progress.update(0,0,"Datei abschließen");assertTrue(progress.bar.isIndeterminate());
        progress.dismiss();progress.update(4,4,"Fertig");assertFalse(progress.dialog.isShowing());
        controller.pause().stop().destroy();
    }
}
