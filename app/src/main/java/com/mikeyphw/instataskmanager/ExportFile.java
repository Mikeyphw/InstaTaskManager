package com.mikeyphw.instataskmanager;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Writes only the explicitly requested diagnostic report to public Downloads. */
final class ExportFile {
    private ExportFile() { }

    static String save(Context context, long when, String report) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Android 9 fallback: platform share sheet; no unsafe legacy storage permissions.
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, report);
            share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                Intent chooser = Intent.createChooser(share, "Export InstaTaskManager report");
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(chooser);
            } catch (RuntimeException error) {
                throw new IOException("Android share sheet unavailable", error);
            }
            return "Export opened in Android Share";
        }
        String name = ExportReport.filename(when);
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InstaTaskManager/");
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        ContentResolver resolver = context.getContentResolver();
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Download entry creation failed");
        boolean completed = false;
        try {
            try (OutputStream output = resolver.openOutputStream(uri, "w")) {
                if (output == null) throw new IOException("Unable to open Download entry");
                byte[] bytes = report.getBytes(StandardCharsets.UTF_8);
                if (bytes.length > 256 * 1024) throw new IOException("Refusing oversized export");
                output.write(bytes);
                output.flush();
            }
            ContentValues finished = new ContentValues();
            finished.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (resolver.update(uri, finished, null, null) != 1)
                throw new IOException("Unable to publish completed report");
            completed = true;
            return "Saved to Downloads/InstaTaskManager/" + name;
        } finally {
            if (!completed) resolver.delete(uri, null, null);
        }
    }
}
