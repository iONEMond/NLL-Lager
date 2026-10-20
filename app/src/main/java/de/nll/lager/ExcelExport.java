package de.nll.lager;

import android.database.Cursor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Streaming OOXML export. Text identifiers remain text, amounts and dates are typed cells. */
public final class ExcelExport {
    private ExcelExport() {}
    static final String[] HEADERS={"Code","Beschreibung","Menge","Bewegungsart","Mitarbeiter","Datum","Lagerort (Von -> Nach)","Buchung erfolgt?"};
    static String[] values(Cursor c) {
        return new String[]{c.getString(2),c.getString(4),c.getString(9),c.getString(13),c.getString(11),c.getString(1),c.getString(7)+" -> "+c.getString(8),""};
    }
    private static final String NS="http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String REL="http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    public static int write(OutputStream output,Cursor cursor) throws IOException {return write(output,cursor,WorkProgress.NONE);}
    static int write(OutputStream output,Cursor cursor,WorkProgress progress) throws IOException {
        int total=cursor.getCount();progress.update(0,total,"Excel-Bericht schreiben …");
        try(ZipOutputStream zip=new ZipOutputStream(new FilterOutputStream(output) { @Override public void close() throws IOException {flush();} })) {
            entry(zip,"[Content_Types].xml","<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/><Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/><Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>");
            entry(zip,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\""+REL+"/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip,"xl/workbook.xml","<workbook xmlns=\""+NS+"\" xmlns:r=\""+REL+"\"><sheets><sheet name=\"Warenbewegungen\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            entry(zip,"xl/_rels/workbook.xml.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\""+REL+"/worksheet\" Target=\"worksheets/sheet1.xml\"/><Relationship Id=\"rId2\" Type=\""+REL+"/styles\" Target=\"styles.xml\"/></Relationships>");
            entry(zip,"xl/styles.xml","<styleSheet xmlns=\""+NS+"\"><numFmts count=\"2\"><numFmt numFmtId=\"164\" formatCode=\"dd.mm.yyyy hh:mm:ss\"/><numFmt numFmtId=\"165\" formatCode=\"0.###\"/></numFmts><fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts><fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill></fills><borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs><cellXfs count=\"4\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/><xf numFmtId=\"164\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/><xf numFmtId=\"165\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/><xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/></cellXfs><cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles></styleSheet>");
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            Writer w=new OutputStreamWriter(zip,StandardCharsets.UTF_8);
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\""+NS+"\"><sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews><cols><col min=\"1\" max=\"1\" width=\"18\" customWidth=\"1\"/><col min=\"2\" max=\"2\" width=\"40\" customWidth=\"1\"/><col min=\"3\" max=\"3\" width=\"14\" customWidth=\"1\"/><col min=\"4\" max=\"4\" width=\"20\" customWidth=\"1\"/><col min=\"5\" max=\"5\" width=\"26\" customWidth=\"1\"/><col min=\"6\" max=\"6\" width=\"24\" customWidth=\"1\"/><col min=\"7\" max=\"7\" width=\"36\" customWidth=\"1\"/><col min=\"8\" max=\"8\" width=\"24\" customWidth=\"1\"/></cols><sheetData><row r=\"1\">");
            String[] headers=HEADERS;
            for(int i=0;i<headers.length;i++) text(w,cell(i,1),headers[i],3);
            w.write("</row>"); int count=0;
            while(cursor.moveToNext()) {
                if(count>=1048575) throw new IOException("Zu viele Bewegungen für ein Excel-Blatt.");
                int row=++count+1; w.write("<row r=\""+row+"\">");
                OffsetDateTime time=OffsetDateTime.parse(cursor.getString(1));
                String[] values=values(cursor);
                for(int col=0;col<values.length;col++) {
                    String ref=cell(col,row);
                    if(col==5) {
                        double serial=time.toLocalDate().toEpochDay()-LocalDate.of(1899,12,30).toEpochDay()+time.toLocalTime().toNanoOfDay()/86400000000000d;
                        number(w,ref,Double.toString(serial),1);
                    } else if(col==2) number(w,ref,values[col],2);
                    else text(w,ref,values[col],0);
                }
                w.write("</row>");progress.update(count,total,"Excel-Bericht schreiben …");
            }
            w.write("</sheetData><autoFilter ref=\"A1:H"+(count+1)+"\"/></worksheet>"); w.flush(); zip.closeEntry();progress.update(0,0,"Excel-Datei abschließen …");
            return count;
        }
    }
    private static String cell(int column,int row) {return String.valueOf((char)('A'+column))+row;}
    private static void text(Writer w,String ref,String value,int style) throws IOException {w.write("<c r=\""+ref+"\" t=\"inlineStr\" s=\""+style+"\"><is><t xml:space=\"preserve\">"+xml(value)+"</t></is></c>");}
    private static void number(Writer w,String ref,String value,int style) throws IOException {w.write("<c r=\""+ref+"\" s=\""+style+"\"><v>"+value+"</v></c>");}
    private static void entry(ZipOutputStream zip,String name,String text) throws IOException {zip.putNextEntry(new ZipEntry(name)); zip.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+text).getBytes(StandardCharsets.UTF_8)); zip.closeEntry();}
    private static String xml(String value) {
        StringBuilder result=new StringBuilder();
        value.codePoints().forEach(c -> {if(c==9 || c==10 || c==13 || c>=32 && c<=0xD7FF || c>=0xE000 && c<=0xFFFD || c>=0x10000 && c<=0x10FFFF) result.appendCodePoint(c);});
        return result.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
}
