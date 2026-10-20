package de.nll.lager;

import android.content.*;
import android.app.AlertDialog;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class MailShareCancelTest {
    Context context;Store store;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();context.deleteDatabase("nll-lager.db");SharedReport.clear(context);store=new Store(context);store.addPerson("P","Anna","B");store.addMaterial("M","1","Ventil","Material","Stk","A");}
    @After public void cleanup(){Thread.interrupted();MailSync.BUSY.set(false);store.close();context.deleteDatabase("nll-lager.db");SharedReport.clear(context);}
    long book(){return store.book(store.material("M"),"A","B","1",store.person("P"),"Entnahme");}
    @Test public void cancellationRollsBackPartialImportAndRevision(){
        long before=store.revision();OperationControl token=new OperationControl();
        WorkProgress progress=(n,total,msg)->{if(n==1){token.cancel();token.checkCancelled();}};
        assertThrows(OperationControl.Cancelled.class,()->store.importRows(Arrays.asList(new String[]{"X","x","Erste","Material","Stk"},new String[]{"Y","y","Zweite","Material","Stk"}),false,Store.ImportPolicy.REJECT,progress));
        assertNull(store.material("X"));assertNull(store.material("Y"));assertEquals(before,store.revision());
    }
    @Test public void cancellationCannotWinAfterCommitBoundary(){OperationControl token=new OperationControl();token.completeCancellation();assertFalse(token.cancel());token.checkCancelled();}
    @Test public void cancellationInterruptsWaitingWorker()throws Exception {
        OperationControl token=new OperationControl();java.util.concurrent.CountDownLatch waiting=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicBoolean interrupted=new java.util.concurrent.atomic.AtomicBoolean();
        Thread worker=new Thread(()->{token.update(0,0,"");waiting.countDown();try{new java.util.concurrent.CountDownLatch(1).await();}catch(InterruptedException e){interrupted.set(true);}});worker.start();assertTrue(waiting.await(2,java.util.concurrent.TimeUnit.SECONDS));assertTrue(token.cancel());worker.join(2000);assertFalse(worker.isAlive());assertTrue(interrupted.get());
    }
    @Test public void cancelledExportCreatesNoPendingReportOrArchive()throws Exception {
        book();OperationControl token=new OperationControl();token.cancel();assertThrows(OperationControl.Cancelled.class,()->SharedReport.create(context,store,false,token));assertEquals("",SharedReport.pending(context));assertEquals(1,store.count(false));
    }
    @Test public void sharingHasReadGrantAndDoesNotArchiveUntilExplicitConfirmation()throws Exception {
        long first=book();String name=SharedReport.create(context,store,false,WorkProgress.NONE);Intent i=SharedReport.intent(context,name,"receiver@example.com");assertEquals(Intent.ACTION_SEND,i.getAction());assertEquals(ReportFileProvider.MIME,i.getType());assertEquals("receiver@example.com",i.getStringArrayExtra(Intent.EXTRA_EMAIL)[0]);assertNotEquals(0,i.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);assertNotNull(i.getClipData());assertEquals(1,store.count(false));
        long later=book();assertEquals(1,SharedReport.archive(context,store));assertNotNull(store.movement(first).values[12]);assertNull(store.movement(later).values[12]);assertEquals(1,store.count(false));
    }
    @Test public void restoreWithChangedReportDataBlocksArchiveAtomically()throws Exception {
        long id=book();SharedReport.create(context,store,false,WorkProgress.NONE);android.content.ContentValues changed=new android.content.ContentValues();changed.put("quantity","99");store.getWritableDatabase().update("movements",changed,"id=?",new String[]{Long.toString(id)});
        assertThrows(java.io.IOException.class,()->SharedReport.archive(context,store));assertEquals(1,store.count(false));assertFalse(SharedReport.pending(context).isEmpty());
    }
    @Test public void providerOnlyAllowsReadOfReportFile()throws Exception {
        book();String name=SharedReport.create(context,store,false,WorkProgress.NONE);ReportFileProvider provider=Robolectric.buildContentProvider(ReportFileProvider.class).create().get();android.net.Uri uri=SharedReport.intent(context,name,"").getParcelableExtra(Intent.EXTRA_STREAM);
        try(android.os.ParcelFileDescriptor fd=provider.openFile(uri,"r")){assertTrue(fd.getStatSize()>0);}
        assertThrows(java.io.FileNotFoundException.class,()->provider.openFile(uri,"w"));assertThrows(java.io.FileNotFoundException.class,()->provider.openFile(android.net.Uri.parse("content://test/../nll-lager.db"),"r"));
    }
    @Test public void timeoutRemainsActiveWhenProgressViewIsDetached()throws Exception {
        android.app.Activity activity=Robolectric.buildActivity(android.app.Activity.class).setup().get();OperationProgress progress=new OperationProgress(activity,"Test","Warten");
        java.util.concurrent.CountDownLatch cancelled=new java.util.concurrent.CountDownLatch(1);progress.onCancel(cancelled::countDown);progress.timeoutAfter(10);progress.detach();
        assertTrue(cancelled.await(2,java.util.concurrent.TimeUnit.SECONDS));assertTrue(progress.control.isCancelled());assertTrue(progress.timedOut);progress.dismiss();
    }
    @Test public void progressButtonRequestsRealCancellation(){
        android.app.Activity activity=Robolectric.buildActivity(android.app.Activity.class).setup().get();OperationProgress progress=new OperationProgress(activity,"Test","Warten");progress.dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertTrue(progress.control.isCancelled());assertThrows(OperationControl.Cancelled.class,progress::checkCancelled);assertTrue(progress.dialog.isShowing());progress.dismiss();
    }
}
