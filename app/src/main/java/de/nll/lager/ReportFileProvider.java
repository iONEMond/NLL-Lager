package de.nll.lager;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Only immutable report files are exposed through temporary read grants. */
public final class ReportFileProvider extends ContentProvider {
    static final String MIME="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    @Override public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(uri.getPathSegments().size()!=1 || name==null || !name.matches("NLL-Bericht-[0-9a-f-]{36}\\.xlsx"))throw new FileNotFoundException();
        File file=new File(new File(getContext().getFilesDir(),"shared-reports"),name);
        if(!file.isFile())throw new FileNotFoundException();return file;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {if(!"r".equals(mode))throw new FileNotFoundException("Nur Lesezugriff.");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public String getType(Uri uri){return MIME;}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        try{File f=file(uri);String[] cols=projection==null ? new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);MatrixCursor.RowBuilder r=c.newRow();for(String col:cols)r.add(OpenableColumns.DISPLAY_NAME.equals(col)?f.getName():OpenableColumns.SIZE.equals(col)?f.length():null);return c;}catch(FileNotFoundException e){return null;}
    }
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
}
