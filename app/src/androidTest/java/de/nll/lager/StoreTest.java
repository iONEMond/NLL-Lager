package de.nll.lager;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class StoreTest {
    private Context context;
    private Store store;
    @Before public void before() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("nll-lager.db"); store=new Store(context);
    }
    @After public void after() { store.close(); context.deleteDatabase("nll-lager.db"); }
    private void seed() {
        store.addPerson("0007","Anna");
        store.addMaterial("00123","MAT-17","Ventil; ½ Zoll","Material","Stk");
    }
    private void book(String quantity) {
        store.book(store.material("00123"),"Hauptlager","Fahrzeug 4",quantity,store.person("0007"));
    }
    @Test public void bookingPersistsSnapshotsAndExactAmounts() throws Exception {
        seed(); book("0,125"); store.close(); store=new Store(context);
        assertEquals(1,store.count(false)); assertEquals(0,store.count(true));
        StringWriter csv=new StringWriter(); assertEquals(1,store.writeCsv(csv,store.cutoff(),false));
        String report=csv.toString();
        assertTrue(report.contains("\"0,125\"")); assertTrue(report.contains("\"Anna\""));
        assertTrue(report.contains("\"00123\";\"Ventil; ½ Zoll\""));
        assertTrue(report.contains("\"Hauptlager -> Fahrzeug 4\""));
        assertTrue(report.matches("(?s).*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }
    @Test public void archiveOnlyExportedSnapshotAndAllowArchiveReexport() throws Exception {
        seed(); book("1"); long cutoff=store.cutoff(); book("2");
        StringWriter first=new StringWriter(); assertEquals(1,store.writeCsv(first,cutoff,false));
        assertEquals(2,store.count(false)); // writing alone does not archive
        assertEquals(1,store.archive(cutoff,"report-1"));
        assertEquals(1,store.count(false)); assertEquals(1,store.count(true));
        assertNotNull(store.material("00123")); assertNotNull(store.person("0007"));
        StringWriter archive=new StringWriter(); assertEquals(1,store.writeCsv(archive,store.archiveCutoff(),true));
        assertTrue(archive.toString().contains("Hauptlager -> Fahrzeug 4"));
        assertEquals(0,store.archive(cutoff,"report-2"));
    }
    @Test public void writeFailurePreservesAllMovements() throws Exception {
        seed(); book("3");
        Writer failing=new Writer() {
            @Override public void write(char[] b,int o,int l) throws IOException {throw new IOException("Full disk");}
            @Override public void flush() {}
            @Override public void close() {}
        };
        assertThrows(IOException.class,() -> store.writeCsv(failing,store.cutoff(),false));
        assertEquals(1,store.count(false)); assertEquals(0,store.count(true));
    }
    @Test public void manualDeletionRemovesOnlyCatalogRowsAndPreservesOpenAndArchivedSnapshots() throws Exception {
        seed();book("1");store.archive(store.cutoff(),"first");book("2");
        store.deleteEntry("00123",false);store.deleteEntry("0007",true);
        assertNull(store.material("00123"));assertNull(store.person("0007"));
        assertEquals(1,store.count(false));assertEquals(1,store.count(true));
        StringWriter csv=new StringWriter();store.writeCsv(csv,store.cutoff(),false);
        assertTrue(csv.toString().contains("Anna"));assertTrue(csv.toString().contains("Ventil"));
        assertThrows(IllegalArgumentException.class,() -> store.deleteEntry("00123",false));
    }
    @Test public void invalidMovementCannotEnterDatabase() {
        seed();
        assertThrows(IllegalArgumentException.class,() -> book("0"));
        assertThrows(IllegalArgumentException.class,() -> store.book(store.material("00123"),"A","a","1",store.person("0007")));
        assertEquals(0,store.count(false));
    }
    @Test public void duplicateImportRollsBackEveryRow() {
        seed();
        List<String[]> rows=new ArrayList<>();
        rows.add(new String[]{"new","New Person"}); rows.add(new String[]{"0007","Duplicate"});
        assertThrows(IllegalArgumentException.class,() -> store.importRows(rows,true));
        assertNull(store.person("new")); assertEquals("Anna",store.person("0007").name);
    }
    @Test public void importPoliciesSkipOrUpdateConflictsAndKeepHistory() throws Exception {
        seed();book("1");
        List<String[]> rows=Arrays.asList(new String[]{"00123","NEU","Neues Ventil","Material","Stk"},new String[]{"new","N","Neuer Artikel","Material","Stk"});
        assertEquals(1,store.importConflicts(rows,false));
        assertArrayEquals(new int[]{1,0,1},store.importRows(rows,false,Store.ImportPolicy.SKIP,WorkProgress.NONE));
        assertEquals("MAT-17",store.material("00123").number);assertNotNull(store.material("new"));
        assertArrayEquals(new int[]{0,2,0},store.importRows(rows,false,Store.ImportPolicy.UPDATE,WorkProgress.NONE));
        assertEquals("NEU",store.material("00123").number);
        StringWriter csv=new StringWriter();store.writeCsv(csv,store.cutoff(),false);
        assertTrue(csv.toString().contains("Ventil; ½ Zoll"));assertFalse(csv.toString().contains("Neues Ventil"));
        List<String[]> persons=Arrays.asList(new String[]{"0007","Neu Anna"},new String[]{"person-new","Ben"});
        assertArrayEquals(new int[]{1,0,1},store.importRows(persons,true,Store.ImportPolicy.SKIP,WorkProgress.NONE));
        assertEquals("Anna",store.person("0007").name);
        assertArrayEquals(new int[]{0,2,0},store.importRows(persons,true,Store.ImportPolicy.UPDATE,WorkProgress.NONE));
        assertEquals("Neu Anna",store.person("0007").name);
    }
    @Test public void failedUpdateBatchRollsBackUpdatedAndNewEntriesAndRejectsDuplicateFileCodes() {
        seed();
        List<String[]> rows=Arrays.asList(new String[]{"0007","Changed"},new String[]{"new","New"},new String[]{"bad",""});
        assertThrows(IllegalArgumentException.class,() -> store.importRows(rows,true,Store.ImportPolicy.UPDATE,WorkProgress.NONE));
        assertEquals("Anna",store.person("0007").name);assertNull(store.person("new"));
        assertThrows(IllegalArgumentException.class,() -> store.importRows(Arrays.asList(new String[]{"x","A"},new String[]{"x","B"}),true,Store.ImportPolicy.SKIP,WorkProgress.NONE));
        assertNull(store.person("x"));
    }
    @Test public void materialImportAndPersonImportPreserveCodes() {
        store.importRows(Collections.singletonList(new String[]{"0001","N-1","R32","Kältemittel","kg"}),false);
        store.importRows(Collections.singletonList(new String[]{"0007","Anna"}),true);
        assertEquals("0001",store.material("0001").code);
        assertEquals("Kältemittel",store.material("0001").type);
        assertEquals("Anna",store.person("0007").name);
    }
    private InputStream template(String name) throws IOException {
        try { return InstrumentationRegistry.getInstrumentation().getContext().getAssets().open(name); }
        catch (FileNotFoundException e) {
            InputStream in = getClass().getClassLoader().getResourceAsStream(name);
            if (in == null) throw e;
            return in;
        }
    }
    @Test public void distributedTemplatesAreImportable() throws Exception {
        for (String name : new String[]{"Material-Import.xlsx", "Personen-Import.xlsx"}) {
            File f = File.createTempFile("template-", ".xlsx", context.getCacheDir());
            try (InputStream in = template(name);
                 OutputStream out = new FileOutputStream(f)) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            }
            try {
                boolean people = name.startsWith("Personen");
                List<String[]> rows = ExcelImport.read(f, people);
                assertEquals(people ? 1 : 2, rows.size());
                assertEquals(people ? "0007" : "0001234567890", rows.get(0)[0]);
            } finally { f.delete(); }
        }
    }
    private File xlsx(String sheet,String shared) throws Exception {
        File f=File.createTempFile("test-",".xlsx",context.getCacheDir());
        try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(f))) {
            entry(z,"xl/workbook.xml","<workbook xmlns:r='http://schemas.openxmlformats.org/officeDocument/2006/relationships'><sheets><sheet name='Daten' r:id='rId7'/></sheets></workbook>");
            entry(z,"xl/_rels/workbook.xml.rels","<Relationships><Relationship Id='rId7' Target='worksheets/sheet2.xml'/></Relationships>");
            entry(z,"xl/worksheets/sheet2.xml","<worksheet><sheetData>"+sheet+"</sheetData></worksheet>");
            if(shared!=null) entry(z,"xl/sharedStrings.xml",shared);
        }
        return f;
    }
    private void entry(ZipOutputStream z,String name,String xml) throws Exception {
        z.putNextEntry(new ZipEntry(name)); z.write(xml.getBytes("UTF-8")); z.closeEntry();
    }
    private String inline(String ref,String text) {return "<c r='"+ref+"' t='inlineStr'><is><t>"+text+"</t></is></c>";}
    @Test public void readsInlineXlsxWithReorderedColumnsAndLeadingZeros() throws Exception {
        String sheet="<row>"+inline("A1","Name")+inline("C1","Personencode")+"</row><row>"+inline("A2","Anna &amp; Tom")+inline("C2","0007")+"</row>";
        File f=xlsx(sheet,null);
        try {List<String[]> rows=ExcelImport.read(f,true); assertEquals(1,rows.size()); assertArrayEquals(new String[]{"0007","Anna & Tom"},rows.get(0));}
        finally {f.delete();}
    }
    @Test public void readsSharedStringsAndRejectsDuplicates() throws Exception {
        String head="<row>"+inline("A1","Personencode")+inline("B1","Name")+"</row>";
        String row="<row><c r='A2' t='s'><v>0</v></c><c r='B2' t='s'><v>1</v></c></row>";
        String shared="<sst><si><t>0007</t></si><si><t>Anna</t></si></sst>";
        File good=xlsx(head+row,shared);
        try {assertArrayEquals(new String[]{"0007","Anna"},ExcelImport.read(good,true).get(0));} finally {good.delete();}
        File duplicate=xlsx(head+row+row,shared);
        try {assertThrows(IOException.class,() -> ExcelImport.read(duplicate,true));} finally {duplicate.delete();}
    }
    @Test public void companyCatalogueImportsNumericCodeAndIgnoresOrderingColumns() throws Exception {
        String head="<row>"+inline("A1","Title")+inline("B1","Code")+inline("C1","Typ")+inline("D1","Bemerkung")+inline("E1","BST Menge")+"</row>";
        File f=xlsx(head+"<row>"+inline("A2","REGLER DIXELL XR70CX (LIDL)")+"<c r='B2'><v>10129000</v></c>"+inline("C2","Regler")+inline("D2","Hinweis")+"<c r='E2'><v>10</v></c></row>",null);
        try {
            List<String[]> rows=ExcelImport.read(f,false);
            assertArrayEquals(new String[]{"10129000","10129000","REGLER DIXELL XR70CX (LIDL)","Material","Stk"},rows.get(0));
            store.importRows(rows,false);assertEquals("10129000",store.material("10129000").number);assertEquals(0,store.count(false));
        } finally {f.delete();}
    }
    @Test public void companyCatalogueAllowsReorderedColumnsTextCodesAndOptionalWarehouse() throws Exception {
        File f=xlsx("<row>"+inline("A1","Code")+inline("B1","Lager")+inline("D1","Title")+"</row><row>"+inline("A2","000140002")+inline("B2","Hauptlager")+inline("D2","Ventilatormotor")+"</row>",null);
        try {assertArrayEquals(new String[]{"000140002","000140002","Ventilatormotor","Material","Stk","Hauptlager"},ExcelImport.read(f,false).get(0));} finally {f.delete();}
    }
    @Test public void companyCatalogueRejectsDuplicateCodesMissingValuesAndAmbiguousHeaders() throws Exception {
        String head="<row>"+inline("A1","Title")+inline("B1","Code")+"</row>";
        String row="<row>"+inline("A2","Ventil")+inline("B2","001")+"</row>";
        for(String xml:new String[]{head+row+row,head+"<row>"+inline("A2","Ventil")+"</row>","<row>"+inline("A1","Title")+inline("B1","Code")+inline("C1","Code")+"</row>"+row}) {
            File f=xlsx(xml,null);try {assertThrows(IOException.class,() -> ExcelImport.read(f,false));} finally {f.delete();}
        }
    }
    @Test public void minimalHeadersUseDefaultsAndPreserveAbsentFieldsOnUpdate() throws Exception {
        File f=xlsx("<row>"+inline("A1","Code")+inline("B1","Beschreibung")+"</row><row>"+inline("A2","0001")+inline("B2","Neu")+"</row>",null);
        try {
            List<String[]> rows=ExcelImport.read(f,false);
            assertArrayEquals(new String[]{"0001","0001","Neu","Material","Stk"},rows.get(0));
            store.addMaterial("0001","KM-1","Alt","Kältemittel","kg","A");
            store.importRows(rows,false,Store.ImportPolicy.UPDATE,WorkProgress.NONE);
            Store.Material item=store.material("0001");
            assertEquals("Neu",item.description);assertEquals("KM-1",item.number);
            assertEquals("Kältemittel",item.type);assertEquals("kg",item.unit);assertEquals("A",item.location);
        } finally {f.delete();}
    }
    @Test public void optionalBlankFieldsUseDefaultsButCodeAndDescriptionRemainRequired() throws Exception {
        File f=xlsx("<row>"+inline("A1","Scancode")+inline("B1","Beschreibung")+inline("C1","Art")+inline("D1","Einheit")+"</row><row>"+inline("A2","0002")+inline("B2","Ventil")+"</row>",null);
        try {assertArrayEquals(new String[]{"0002","0002","Ventil","Material","Stk"},ExcelImport.read(f,false).get(0));}finally{f.delete();}
        File bad=xlsx("<row>"+inline("A1","Code")+"</row><row>"+inline("A2","0002")+"</row>",null);
        try {assertThrows(IOException.class,() -> ExcelImport.read(bad,false));}finally{bad.delete();}
    }
    @Test public void rejectsFormulasAndMissingHeaders() throws Exception {
        File formula=xlsx("<row>"+inline("A1","Personencode")+inline("B1","Name")+"</row><row><c r='A2'><f>1+1</f><v>2</v></c>"+inline("B2","Anna")+"</row>",null);
        try {assertThrows(IOException.class,() -> ExcelImport.read(formula,true));} finally {formula.delete();}
        File missing=xlsx("<row>"+inline("A1","Falsch")+"</row>",null);
        try {assertThrows(IOException.class,() -> ExcelImport.read(missing,true));} finally {missing.delete();}
    }
}
