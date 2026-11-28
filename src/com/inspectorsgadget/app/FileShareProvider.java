package com.inspectorsgadget.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Minimal FileProvider: lets the camera app write a photo into our private storage
 * and lets PDF viewers / share targets read reports. URIs look like
 * content://com.inspectorsgadget.app.files/files/<path> or /cache/<path>.
 */
public class FileShareProvider extends ContentProvider {
    public static final String AUTHORITY = "com.inspectorsgadget.app.files";

    public static Uri uriFor(Context c, File f) {
        String path = f.getAbsolutePath();
        String files = c.getFilesDir().getAbsolutePath();
        String cache = c.getCacheDir().getAbsolutePath();
        String rel;
        if (path.startsWith(files + "/")) rel = "files/" + path.substring(files.length() + 1);
        else if (path.startsWith(cache + "/")) rel = "cache/" + path.substring(cache.length() + 1);
        else throw new IllegalArgumentException("Not shareable: " + path);
        return new Uri.Builder().scheme("content").authority(AUTHORITY).encodedPath("/" + Uri.encode(rel, "/")).build();
    }

    private File fileFor(Uri uri) throws FileNotFoundException {
        String p = uri.getPath();
        if (p == null) throw new FileNotFoundException();
        if (p.startsWith("/")) p = p.substring(1);
        File base;
        if (p.startsWith("files/")) {
            base = getContext().getFilesDir();
            p = p.substring(6);
        } else if (p.startsWith("cache/")) {
            base = getContext().getCacheDir();
            p = p.substring(6);
        } else {
            throw new FileNotFoundException(p);
        }
        try {
            File f = new File(base, p).getCanonicalFile();
            if (!f.getPath().startsWith(base.getCanonicalPath() + "/")) throw new FileNotFoundException(p);
            return f;
        } catch (IOException e) {
            throw new FileNotFoundException(p);
        }
    }

    @Override
    public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = fileFor(uri);
        if (mode.contains("w")) f.getParentFile().mkdirs();
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        File f;
        try {
            f = fileFor(uri);
        } catch (FileNotFoundException e) {
            return null;
        }
        String[] cols = projection != null ? projection
                : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor c = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = f.getName();
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f.length();
        }
        c.addRow(row);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        String p = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
        if (p.endsWith(".pdf")) return "application/pdf";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".apk")) return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override
    public int delete(Uri uri, String s, String[] a) { return 0; }

    @Override
    public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
