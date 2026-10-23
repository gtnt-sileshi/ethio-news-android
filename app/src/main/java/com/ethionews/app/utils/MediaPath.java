package com.ethionews.app.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MediaPath {

    private MediaPath() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Copies the content at {@code uri} into a temporary file and returns its
     * absolute path.  Works on all Android versions including API 29+ where the
     * legacy {@code MediaStore.MediaColumns.DATA} column is no longer populated.
     *
     * Returns {@code null} if the URI cannot be read.
     */
    public static String getPathImage(Context ctx, Uri uri) {
        if (uri == null) return null;
        try {
            InputStream inputStream = ctx.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = File.createTempFile("upload_", ".jpg", ctx.getCacheDir());
            tempFile.deleteOnExit();

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            } finally {
                inputStream.close();
            }
            return tempFile.getAbsolutePath();
        } catch (Exception e) {
            android.util.Log.e("MediaPath", "getPathImage failed", e);
            return null;
        }
    }
}
