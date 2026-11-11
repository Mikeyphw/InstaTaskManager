package com.mikeyphw.instataskmanager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Local-only, user-triggered, bounded diagnostic report. No Instagram account/media content. */
final class ExportReport {
    private static final int MAX_SECTION_CHARS = 15000;
    private static final int MAX_REPORT_CHARS = 64000;

    private ExportReport() { }

    static String timestamp(long epochMillis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date(epochMillis));
    }

    static String filename(long epochMillis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return "InstaTaskManager-" + f.format(new Date(epochMillis)) + ".txt";
    }

    static String build(long time, String currentTab, String overview, String threads,
                        String components, String social) {
        StringBuilder b = new StringBuilder(4096);
        b.append("INSTA TASK MANAGER — LOCAL DIAGNOSTIC EXPORT\n")
         .append("Generated: ").append(timestamp(time)).append("\n")
         .append("Format: P02 read-only performance + Social P01\n")
         .append("Selected tab: ").append(allow(currentTab, 24)).append("\n")
         .append("Privacy: No Instagram account content, usernames, post IDs or media files are read by this exporter.\n")
         .append("No network request is made by the exporter.\n")
         .append("CPU is percentage of one core; UID network figures may cover multiple Instagram processes.\n")
         .append("Thread name hints and method shapes are not proof of feature ownership or account eligibility.\n");
        section(b, "PERFORMANCE OVERVIEW", overview);
        section(b, "HOT THREADS", threads);
        section(b, "DECLARED COMPONENTS (READ ONLY)", components);
        section(b, "SOCIAL P01 — EXACT-APK DISCOVERY (READ ONLY)", social);
        if (b.length() > MAX_REPORT_CHARS) throw new IllegalStateException("Diagnostic report exceeded allowed size");
        return b.toString();
    }

    private static void section(StringBuilder b, String title, String value) {
        b.append("\n=== ").append(title).append(" ===\n");
        b.append(allow(value == null ? "Not measured or not inspected." : value, MAX_SECTION_CHARS)).append('\n');
    }

    /** Reject control characters and cap every section; printable diagnostic data only. */
    static String allow(String raw, int maxChars) {
        if (raw == null || raw.isEmpty()) return "Not measured or not inspected.";
        StringBuilder result = new StringBuilder(Math.min(maxChars + 32, raw.length()));
        for (int i = 0; i < raw.length() && result.length() < maxChars; i++) {
            char ch = raw.charAt(i);
            if (ch == '\n' || ch == '\t' || (ch >= 32 && ch != 127 && ch != '\u202e')) {
                result.append(ch);
            }
        }
        if (raw.length() > maxChars) result.append("\n[Section truncated to safe export limit]");
        return result.toString();
    }
}
