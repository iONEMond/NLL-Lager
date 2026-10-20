package de.nll.lager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Dependency-free XLSX reader: first worksheet, shared and inline strings, bounded XML. */
public final class ExcelImport {
    private static final int MAX_XML = 16 * 1024 * 1024;
    private ExcelImport() {}
    public static List<String[]> read(File file, boolean people) throws Exception {return read(file,people,WorkProgress.NONE);}
    static List<String[]> read(File file,boolean people,WorkProgress progress) throws Exception {
        progress.update(0,0,"Excel-Datei lesen und prüfen …");
        try(ZipFile zip=new ZipFile(file)) {
            List<String> strings=sharedStrings(zip);
            String relationship=null;
            XmlPullParser workbook=parser(zip,"xl/workbook.xml");
            int e;
            while((e=next(workbook))!=XmlPullParser.END_DOCUMENT) {
                if(e==XmlPullParser.START_TAG && "sheet".equals(workbook.getName())) {
                    for(int i=0;i<workbook.getAttributeCount();i++) if("id".equals(workbook.getAttributeName(i))) relationship=workbook.getAttributeValue(i);
                    break;
                }
            }
            String target=null;
            XmlPullParser rels=parser(zip,"xl/_rels/workbook.xml.rels");
            while((e=next(rels))!=XmlPullParser.END_DOCUMENT) {
                if(e==XmlPullParser.START_TAG && "Relationship".equals(rels.getName()) && Objects.equals(relationship,rels.getAttributeValue(null,"Id"))) {
                    if("External".equals(rels.getAttributeValue(null,"TargetMode"))) throw new IOException("Externe Tabellen werden nicht unterstützt.");
                    target=rels.getAttributeValue(null,"Target"); break;
                }
            }
            if(target==null) throw new IOException("Keine erste Tabelle gefunden.");
            if(target.contains("..") || target.contains("\\")) throw new IOException("Ungültiger Tabellenpfad.");
            String path=target.startsWith("/") ? target.substring(1) : "xl/"+target;
            XmlPullParser sheet=parser(zip,path);
            List<String[]> rows=new ArrayList<>();
            String[] row=null; int column=-1; String type=""; StringBuilder value=new StringBuilder();
            boolean collect=false; boolean formula=false;
            while((e=next(sheet))!=XmlPullParser.END_DOCUMENT) {
                if(e==XmlPullParser.START_TAG) {
                    String tag=sheet.getName();
                    if("row".equals(tag)) {row=new String[20]; Arrays.fill(row,""); column=-1;}
                    else if("c".equals(tag)) {
                        String ref=sheet.getAttributeValue(null,"r");
                        column=ref==null ? column+1 : column(ref);
                        type=sheet.getAttributeValue(null,"t"); value.setLength(0); formula=false;
                    } else if("f".equals(tag)) formula=true;
                    else if("v".equals(tag) || "t".equals(tag)) collect=true;
                } else if((e==XmlPullParser.TEXT || e==XmlPullParser.CDSECT || e==XmlPullParser.ENTITY_REF) && collect) {
                    if(sheet.getText()!=null) value.append(sheet.getText());
                    if(value.length()>10000) throw new IOException("Eine Zelle ist zu lang.");
                } else if(e==XmlPullParser.END_TAG) {
                    String tag=sheet.getName();
                    if("v".equals(tag) || "t".equals(tag)) collect=false;
                    else if("c".equals(tag) && row!=null && column>=0 && column<row.length) {
                        if(formula) throw new IOException("Formeln bitte zuerst als Werte speichern.");
                        String v=value.toString();
                        if("s".equals(type)) {
                            try {v=strings.get(Integer.parseInt(v));} catch(Exception ex) {throw new IOException("Ungültiger Excel-Textverweis.");}
                        }
                        row[column]=v;
                    } else if("row".equals(tag) && row!=null) {
                        boolean empty=true; for(String s:row) if(!s.trim().isEmpty()) {empty=false; break;}
                        if(!empty) {rows.add(row);if(rows.size()%100==0) progress.update(0,0,rows.size()+" Excel-Zeilen gelesen …");}
                        if(rows.size()>10001) throw new IOException("Maximal 10.000 Datensätze pro Import.");
                        row=null;
                    }
                }
            }
            return map(rows,people);
        }
    }
    private static int column(String ref) throws IOException {
        int col=0; int i=0;
        while(i<ref.length() && ref.charAt(i)>='A' && ref.charAt(i)<='Z') {col=col*26+ref.charAt(i++)-'A'+1; if(col>16384) throw new IOException("Ungültige Excel-Spalte.");}
        return col-1;
    }
    private static int next(XmlPullParser p) throws Exception {
        if(Thread.currentThread().isInterrupted())throw new OperationControl.Cancelled();
        int event=p.nextToken();
        if(event==XmlPullParser.DOCDECL) throw new IOException("XML-Dokumenttypen sind nicht erlaubt.");
        return event;
    }
    private static XmlPullParser parser(ZipFile zip,String path) throws Exception {
        ZipEntry entry=zip.getEntry(path);
        if(entry==null) throw new IOException("Excel-Bestandteil fehlt: "+path);
        if(entry.getSize()>MAX_XML) throw new IOException("Excel-Datei zu groß.");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=zip.getInputStream(entry)) {
            byte[] buffer=new byte[8192]; int n;
            while((n=in.read(buffer))!=-1) {if(bytes.size()+n>MAX_XML) throw new IOException("Excel-Datei zu groß."); bytes.write(buffer,0,n);}
        }
        XmlPullParserFactory f=XmlPullParserFactory.newInstance(); f.setNamespaceAware(true);
        XmlPullParser p=f.newPullParser(); p.setInput(new ByteArrayInputStream(bytes.toByteArray()),null); return p;
    }
    private static List<String> sharedStrings(ZipFile zip) throws Exception {
        List<String> result=new ArrayList<>(); if(zip.getEntry("xl/sharedStrings.xml")==null) return result;
        XmlPullParser p=parser(zip,"xl/sharedStrings.xml"); StringBuilder value=new StringBuilder(); boolean collect=false; int chars=0,e;
        while((e=next(p))!=XmlPullParser.END_DOCUMENT) {
            if(e==XmlPullParser.START_TAG && "si".equals(p.getName())) value.setLength(0);
            else if(e==XmlPullParser.START_TAG && "t".equals(p.getName())) collect=true;
            else if((e==XmlPullParser.TEXT || e==XmlPullParser.ENTITY_REF || e==XmlPullParser.CDSECT) && collect && p.getText()!=null) {
                value.append(p.getText()); chars+=p.getText().length(); if(chars>2_000_000) throw new IOException("Zu viele Excel-Texte.");
            } else if(e==XmlPullParser.END_TAG && "t".equals(p.getName())) collect=false;
            else if(e==XmlPullParser.END_TAG && "si".equals(p.getName())) {result.add(value.toString()); if(result.size()>100000) throw new IOException("Zu viele Excel-Texte.");}
        }
        return result;
    }
    private static String normalized(String s) {
        return s.toLowerCase(Locale.ROOT).replace(" ","").replace("_","").replace("-","").replace(".","");
    }
    private static final class MappedRows extends ArrayList<String[]> {
        final boolean[] supplied;
        MappedRows(int[] columns) {supplied=new boolean[columns.length];for(int i=0;i<columns.length;i++) supplied[i]=columns[i]>=0;}
    }
    static boolean supplied(List<String[]> rows,int column) {
        return !(rows instanceof MappedRows) || ((MappedRows)rows).supplied[column];
    }
    private static List<String[]> map(List<String[]> rows,boolean people) throws IOException {
        if(rows.isEmpty()) throw new IOException("Die Tabelle ist leer.");
        String[] names=people ? new String[]{"personencode","name"} : new String[]{"scancode","matnummer","beschreibung","art","einheit"};
        int[] columns=new int[names.length]; Arrays.fill(columns,-1);
        String[] header=rows.get(0);
        for(int i=0;i<header.length;i++) {
            String h=normalized(header[i]);
            if(h.equals("materialnummer") || h.equals("matnr")) h="matnummer";
            if(!people && (h.equals("barcode") || h.equals("code"))) h="scancode";
            if(!people && h.equals("title")) h="beschreibung";
            if(people && (h.equals("beschreibung") || h.equals("title"))) h="name";
            if(people && h.equals("code")) h="personencode";
            for(int j=0;j<names.length;j++) if(names[j].equals(h)) {if(columns[j]!=-1) throw new IOException("Doppelte Spaltenüberschrift: "+names[j]); columns[j]=i;}
        }
        for(int i=0;i<names.length;i++) if((i==0 || i==(people ? 1 : 2)) && columns[i]<0) throw new IOException("Spalte fehlt: "+names[i]+". Bitte Vorlage verwenden.");
        int locationColumn=-1;
        for(int i=0;i<header.length;i++) {
            String h=normalized(header[i]);
            if(h.equals(people ? "lagernummer" : "lager") || h.equals(people ? "lager" : "lagerort")) {
                if(locationColumn>=0) throw new IOException("Doppelte Lager-Spalte."); locationColumn=i;
            }
        }
        List<String[]> result=new MappedRows(columns); Set<String> codes=new HashSet<>();
        for(int i=1;i<rows.size();i++) {
            String[] mapped=new String[names.length+(locationColumn>=0 ? 1 : 0)];
            try {
                for(int j=0;j<names.length;j++) {
                    String value=columns[j]<0 ? "" : rows.get(i)[columns[j]].trim();
                    if(!people && value.isEmpty() && j!=0 && j!=2) value=j==1 ? mapped[0] : j==3 ? "Material" : "Stk";
                    mapped[j]=Rules.required(value,names[j]);
                }
                String location=locationColumn<0 ? "" : rows.get(i)[locationColumn].trim();
                if(locationColumn>=0) mapped[names.length]=location.isEmpty() ? "" : Rules.required(location,people ? "Lagernummer" : "Lager");
                if(!people) {
                    if(mapped[3].equalsIgnoreCase("material")) mapped[3]="Material";
                    else if(mapped[3].equalsIgnoreCase("kältemittel") || mapped[3].equalsIgnoreCase("kaeltemittel")) mapped[3]="Kältemittel";
                    else throw new IllegalArgumentException("Art muss Material oder Kältemittel sein.");
                }
                if(!codes.add(mapped[0])) throw new IllegalArgumentException("Doppelter Code in Datei: "+mapped[0]);
            } catch(IllegalArgumentException ex) {throw new IOException("Datensatz "+i+": "+ex.getMessage());}
            result.add(mapped);
        }
        if(result.isEmpty()) throw new IOException("Keine Datensätze vorhanden.");
        return result;
    }
}
