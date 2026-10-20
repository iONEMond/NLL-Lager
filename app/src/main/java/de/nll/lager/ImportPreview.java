package de.nll.lager;

import java.util.*;
import android.database.sqlite.SQLiteDatabase;

/** Read-only, consistent preview. Absent optional columns never appear as proposed changes. */
final class ImportPreview {
    final List<String> entries=new ArrayList<>();
    int added,changed,unchanged;
    long revision;
    static ImportPreview read(Store store,List<String[]> rows,boolean people,WorkProgress progress) {
        ImportPreview preview=new ImportPreview();SQLiteDatabase db=store.getReadableDatabase();db.beginTransaction();
        try {
            int count=0;
            for(String[] row:rows) {
                String[] old=null;
                if(people) {Store.Person person=store.person(row[0]);if(person!=null)old=new String[]{person.code,person.name,person.location};}
                else {Store.Material item=store.material(row[0]);if(item!=null)old=new String[]{item.code,item.number,item.description,item.type,item.unit,item.location};}
                String[] labels=people ? new String[]{"Code","Name","Lagernummer"} : new String[]{"Code","Materialnummer","Beschreibung","Art","Einheit","Lager"};
                StringBuilder detail=new StringBuilder();boolean change=false;
                for(int col=1;col<labels.length;col++) {
                    if(old!=null && (col>=row.length || (!people && col<5 && !ExcelImport.supplied(rows,col))))continue;
                    String value=col<row.length ? row[col] : "";
                    if(old==null || !Objects.equals(old[col],value)) {
                        change=true;detail.append("\n").append(labels[col]).append(": ");
                        if(old!=null)detail.append(old[col].isEmpty() ? "(leer)" : old[col]).append(" → ");
                        detail.append(value.isEmpty() ? "(leer)" : value);
                    }
                }
                String state;
                if(old==null){preview.added++;state="NEU";}
                else if(change){preview.changed++;state="ÄNDERUNG";}
                else {preview.unchanged++;state="UNVERÄNDERT";detail.append("\nKeine Änderungen an den gelieferten Feldern.");}
                preview.entries.add(state+" · "+row[0]+detail);
                progress.update(++count,rows.size(),"Importvorschau vorbereiten …");
            }
            preview.revision=store.revision();db.setTransactionSuccessful();return preview;
        } finally {db.endTransaction();}
    }
    String summary(){return added+" neu · "+changed+" mit Änderungen · "+unchanged+" unverändert";}
}
