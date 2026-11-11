package com.mikeyphw.instataskmanager;

final class ExportReportSmoke {
    private static void expect(boolean test, String label) {
        if (!test) throw new AssertionError(label);
    }
    public static void main(String... args) {
        String export = ExportReport.build(0L, "social", "CPU: 22%", "Renderer 8%", "1 service", "Repost: STRUCTURE_FOUND");
        expect(export.contains("CPU: 22%"), "overview not exported");
        expect(export.contains("Renderer 8%"), "threads not exported");
        expect(export.contains("1 service"), "components not exported");
        expect(export.contains("Repost: STRUCTURE_FOUND"), "social scan not exported");
        expect(export.contains("1970-01-01 00:00:00 UTC"), "timestamp mismatch");
        expect(ExportReport.filename(0).equals("InstaTaskManager-19700101-000000-000.txt"), "filename mismatch");
        expect(!ExportReport.filename(0).contains("/"), "path leak");
        expect(ExportReport.build(0,"social",null,null,null,null).contains("Not measured or not inspected"), "missing-data path");
        expect(ExportReport.allow("ok\u0000\u001b\u202e\nend", 32).equals("ok\nend"), "controls not stripped");
        expect(ExportReport.allow("x".repeat(25000), 24000).contains("[Section truncated"), "no length cap");
        System.out.println("ITM_P02_EXPORT_REPORT_SMOKE_PASS 10/10");
    }
}
