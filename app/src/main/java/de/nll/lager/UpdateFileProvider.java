package de.nll.lager;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Grants the Android installer read-only access to one downloaded APK, never to app data. */
public final class UpdateFileProvider extends ContentProvider {
    @Override public boolean onCreate() {return true;}
    private File file(Uri uri) throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(uri.getPathSegments().size()!=1 || name==null || !name.matches("[0-9a-f-]{36}\\.apk")) throw new FileNotFoundException();
        File file=new File(new File(getContext().getFilesDir(),"updates"),name);if(!file.isFile()) throw new FileNotFoundException();return file;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode)) throw new FileNotFoundException("Nur Lesezugriff.");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri) {return "application/vnd.android.package-archive";}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
        try {File file=file(uri);String[] columns=projection==null ? new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE} : projection;
            MatrixCursor cursor=new MatrixCursor(columns);MatrixCursor.RowBuilder row=cursor.newRow();for(String col:columns) row.add(OpenableColumns.DISPLAY_NAME.equals(col) ? file.getName() : OpenableColumns.SIZE.equals(col) ? file.length() : null);return cursor;
        } catch(FileNotFoundException e) {return null;}
    }
    @Override public Uri insert(Uri uri,ContentValues values) {throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args) {throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) {throw new UnsupportedOperationException();}
}
