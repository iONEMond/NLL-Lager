package de.nll.lager;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class LocationsExportTest {
    private Context context;
    private Store store;
    @Before public void setup() {context=RuntimeEnvironment.getApplication();context.deleteDatabase("nll-lager.db");store=new Store(context);}
    @After public void cleanup() {store.close();context.deleteDatabase("nll-lager.db");}
    @Test public void migrationPreservesLegacyCatalogAndMovements() {
        store.close();
        SQLiteDatabase db=context.openOrCreateDatabase("nll-lager.db",0,null);
        db.execSQL("CREATE TABLE materials(code TEXT PRIMARY KEY,number TEXT,description TEXT,type TEXT,unit TEXT)");
        db.execSQL("CREATE TABLE people(code TEXT PRIMARY KEY,name TEXT)");
        db.execSQL("CREATE TABLE movements(id INTEGER PRIMARY KEY,timestamp TEXT,code TEXT,number TEXT,description TEXT,type TEXT,unit TEXT,from_location TEXT,to_location TEXT,quantity TEXT,person_code TEXT,person_name TEXT,archived_batch TEXT)");
        db.execSQL("INSERT INTO materials VALUES('001','M1','Ventil','Material','Stk')");
        db.execSQL("INSERT INTO people VALUES('007','Anna')");
        db.execSQL("INSERT INTO movements VALUES(1,'2026-10-19T10:00:00+02:00','001','M1','Ventil','Material','Stk','A','B','2','007','Anna',NULL)");
        db.setVersion(1);db.close();
        store=new Store(context); assertEquals("",store.material("001").location);assertEquals("",store.person("007").location);assertEquals(1,store.count(false));
        store.assignLocation("001","Hauptlager",false);store.assignLocation("007","0007-L",true);
        assertEquals("Hauptlager",store.material("001").location);assertEquals("0007-L",store.person("007").location);
        assertTrue(store.history(false).get(0).contains("A → B"));
    }
    @Test public void importsOldAndNewRowsAndPersistsWarehouseAssignments() {
        store.importRows(Collections.singletonList(new String[]{"007","Anna","0007-L"}),true);
        store.importRows(Collections.singletonList(new String[]{"001","M1","Ventil","Material","Stk","Hauptlager"}),false);
        store.importRows(Collections.singletonList(new String[]{"008","Ben"}),true);
        store.close();store=new Store(context);
        assertEquals("Hauptlager",store.material("001").location);assertEquals("0007-L",store.person("007").location);assertEquals("",store.person("008").location);
    }
    @Test public void excelContainsTypedAmountsDatesSafeTextAndOnlyExportSnapshot() throws Exception {
        store.addPerson("0007","Anna","0007-L");store.addMaterial("00123","00001","=Text <&> Ventil","Material","Stk","Hauptlager");
        store.book(store.material("00123"),"Hauptlager","0007-L","1.125",store.person("0007"));long cutoff=store.cutoff();
        store.book(store.material("00123"),"Hauptlager","0007-L","2",store.person("0007"));
        File output=new File("build/reports/export-check.xlsx");output.getParentFile().mkdirs();
        try(OutputStream out=new FileOutputStream(output)) {assertEquals(1,store.writeXlsx(out,cutoff,false));}
        try(ZipFile zip=new ZipFile(output)) {
            String xml=new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(),StandardCharsets.UTF_8);
            assertTrue(xml.contains("<c r=\"C2\" s=\"2\"><v>1.125</v>"));
            assertTrue(xml.contains("<c r=\"F2\" s=\"1\"><v>"));
            assertTrue(xml.contains("00123"));assertTrue(xml.contains("=Text &lt;&amp;&gt; Ventil"));assertFalse(xml.contains("<f>"));assertFalse(xml.contains("r=\"A3\""));
        }
        assertEquals(2,store.count(false));store.archive(cutoff,"first-report");
        try(OutputStream out=new ByteArrayOutputStream()) {assertEquals(1,store.writeXlsx(out,store.archiveCutoff(),true));}
    }
    @Test public void optionalWarehouseColumnsAreReadFromExcelHeaders() throws Exception {
        java.lang.reflect.Method map=ExcelImport.class.getDeclaredMethod("map",List.class,boolean.class);map.setAccessible(true);
        List<String[]> people=new ArrayList<>();people.add(new String[]{"Lagernummer","Name","Personencode"});people.add(new String[]{"0007-L","Anna","0007"});
        @SuppressWarnings("unchecked") List<String[]> persons=(List<String[]>)map.invoke(null,people,true);
        assertArrayEquals(new String[]{"0007","Anna","0007-L"},persons.get(0));
        List<String[]> material=new ArrayList<>();material.add(new String[]{"Lager","Einheit","Art","Beschreibung","Mat Nummer","Scancode"});material.add(new String[]{"Hauptlager","Stk","Material","Ventil","00001","00123"});
        @SuppressWarnings("unchecked") List<String[]> materials=(List<String[]>)map.invoke(null,material,false);
        assertArrayEquals(new String[]{"00123","00001","Ventil","Material","Stk","Hauptlager"},materials.get(0));
    }
    @Test public void requestedEightColumnsPreserveKindAndLeaveAccountingColumnEmpty() throws Exception {
        store.addPerson("p","Anna");store.addMaterial("0001","different-number","Ventil","Material","Stk");
        store.book(store.material("0001"),"A","B","2.5",store.person("p"),"Rückgabe");
        File f=File.createTempFile("export-layout-",".xlsx",context.getCacheDir());
        try(OutputStream out=new FileOutputStream(f)) {store.writeXlsx(out,store.cutoff(),false);}
        try(ZipFile zip=new ZipFile(f)) {
            javax.xml.parsers.DocumentBuilderFactory factory=javax.xml.parsers.DocumentBuilderFactory.newInstance();
            org.w3c.dom.Document doc=factory.newDocumentBuilder().parse(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")));
            org.w3c.dom.NodeList rows=doc.getElementsByTagName("row");assertEquals(2,rows.getLength());
            org.w3c.dom.NodeList headers=((org.w3c.dom.Element)rows.item(0)).getElementsByTagName("c");assertEquals(8,headers.getLength());
            String[] expected={"Code","Beschreibung","Menge","Bewegungsart","Mitarbeiter","Datum","Lagerort (Von -> Nach)","Buchung erfolgt?"};
            for(int j=0;j<8;j++) assertEquals(expected[j],headers.item(j).getTextContent());
            org.w3c.dom.NodeList cells=((org.w3c.dom.Element)rows.item(1)).getElementsByTagName("c");
            assertEquals(8,cells.getLength());assertEquals("0001",cells.item(0).getTextContent());assertEquals("2.5",cells.item(2).getTextContent());
            assertEquals("Rückgabe",cells.item(3).getTextContent());assertEquals("Anna",cells.item(4).getTextContent());
            assertEquals("A -> B",cells.item(6).getTextContent());assertEquals("",cells.item(7).getTextContent());
            assertEquals("A1:H2",((org.w3c.dom.Element)doc.getElementsByTagName("autoFilter").item(0)).getAttribute("ref"));
        } finally {f.delete();}
        StringWriter csv=new StringWriter();store.writeCsv(csv,store.cutoff(),false);
        assertTrue(csv.toString().contains("\"0001\";\"Ventil\";\"2,5\";\"Rückgabe\";\"Anna\""));
        assertTrue(csv.toString().endsWith("\"A -> B\";\"\"\r\n"));
        assertThrows(IllegalArgumentException.class,() -> store.book(store.material("0001"),"A","B","1",store.person("p"),"falsch"));
    }
    @Test public void failedExcelWriteDoesNotArchive() {
        store.addPerson("p","Person");store.addMaterial("m","M1","Material","Material","Stk");store.book(store.material("m"),"A","B","1",store.person("p"));
        OutputStream broken=new OutputStream() {public void write(int b) throws IOException {throw new IOException("disk full");}};
        assertThrows(IOException.class,() -> store.writeXlsx(broken,store.cutoff(),false));assertEquals(1,store.count(false));assertEquals(0,store.count(true));
    }
}
