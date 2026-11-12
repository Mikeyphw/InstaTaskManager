package com.mikeyphw.instataskmanager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Optional, user-requested, *read-only* structural investigation of IG455.
 *
 * IMPORTANT: A method existing in DEX does not imply account eligibility,
 * server-provided friend data, a functioning Repost button, or an active hook.
 * There is NO method invocation, no Xposed hook, no suppression, no network,
 * no extraction of user IDs, and no change to Instagram's UI feature gates.
 */
final class SocialDiscovery {
    static final String IG_PACKAGE = "com.instagram.android";
    static final String IG_VERSION = "455.0.0.8.72";
    static final long APK_SIZE = 238346353L;
    static final String APK_SHA256 = "fb6ca8c21d909d95892e67339746b8fe43f409a9dea11f87d9ab0cee8e97c5b6";
    static final String ECLIPSE_COMMIT = "e19ab0def2dc1ac197b7a2d4f8763f701ef68b1d";
    static final String SOURCE = "Instagram 455 APK static audit, 2026-11-10";

    private SocialDiscovery() { }

    /** Static code-shape inventory, NOT live instrumentation or readiness to enable features. */
    private static final class Target {
        final String label, owner, method, result;
        final int arguments;
        Target(String label, String owner, String method, int arguments, String result) {
            this.label = label;
            this.owner = owner;
            this.method = method;
            this.arguments = arguments;
            this.result = result;
        }
    }

    private static final Target[] CANDIDATES = {
        new Target("Repost eligibility and UI state (multipurpose lambda)", "X.0Ie0", "invoke", 0, "java.lang.Object"),
        new Target("Feed/Reels Repost UFI state", "X.0F2B", "invoke", 1, "java.lang.Object"),
        new Target("Native Reels/profile Repost tap (not visibility)", "X.06GI", "EIk", 8, "void"),
        new Target("Native Reels onboarding Repost proxy", "X.0Muj", "EIk", 8, "void"),
        new Target("Reposted-by social context tap (not data)", "X.05NL", "EIn", 3, "void"),
        new Target("Liked-by composite social context", "X.0G4F", "A03", 24, "X.0G4I"),
        new Target("Friendly viewer avatar renderer", "X.0R3f", "A0q", -1, "*"),
        new Target("Profile picture facepile renderer", "X.0G9s", "A0q", -1, "*"),
        new Target("Social context facepile renderer", "X.0G4I", "A0q", -1, "*"),
    };

    static String inspect(Context context, ClassLoader instagramLoader, BooleanSupplier keepRunning) {
        StringBuilder report = new StringBuilder();
        report.append("REPOST + FRIEND FACEPILES | IG455\n")
              .append("READ-ONLY / USER-REQUESTED / ACTIONS OFF\n\n");
        if (context == null || !IG_PACKAGE.equals(context.getPackageName()))
            return "REFUSED: Not running in Instagram.";
        if (!keepRunning.getAsBoolean()) return "Scan cancelled.";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pkg = pm.getPackageInfo(IG_PACKAGE, 0);
            if (!IG_VERSION.equals(pkg.versionName))
                return "REFUSED: Version mismatch. Expected IG455.0.0.8.72; no reflection performed.";
            ApplicationInfo app = pm.getApplicationInfo(IG_PACKAGE, 0);
            File apk = new File(app.sourceDir);
            if (!apk.isFile() || apk.length() != APK_SIZE)
                return "REFUSED: Installed base APK byte length differs from verified IG455; no reflection performed.";
            byte[] buffer = new byte[256 * 1024];
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(apk)) {
                for (int count; (count = in.read(buffer)) != -1;) {
                    if (!keepRunning.getAsBoolean()) return "Scan cancelled. No report retained.";
                    hash.update(buffer, 0, count);
                }
            }
            StringBuilder actualHash = new StringBuilder(64);
            for (byte b : hash.digest()) actualHash.append(String.format(Locale.ROOT, "%02x", b & 0xff));
            if (!APK_SHA256.equals(actualHash.toString()))
                return "REFUSED: Installed base APK hash differs from IG455 evidence; no reflection performed.";
            report.append("EXACT IG455 APK SHA256 + size: MATCH\n")
                  .append("Source: InstaEclipse and direct APK review\n\n")
                  .append("METHOD SHAPES (metadata only)\n");
        } catch (Throwable e) {
            // Never output exception text, source paths, filenames, tokens or user data.
            return "REFUSED: APK verification unavailable (" + e.getClass().getSimpleName() + ").";
        }
        if (instagramLoader == null) return report + "REFUSED: Instagram class loader unavailable.";
        int matched = 0, missing = 0, ambiguous = 0;
        for (Target target : CANDIDATES) {
            if (!keepRunning.getAsBoolean()) return "Scan cancelled.";
            String state;
            try {
                Class<?> owner = Class.forName(target.owner, false, instagramLoader);
                SocialMethodShape.Match method = SocialMethodShape.examine(
                    owner, target.method, target.arguments, target.result);
                state = method.state;
            } catch (ClassNotFoundException e) {
                state = "NOT_FOUND";
            } catch (Throwable e) {
                state = "UNAVAILABLE";
            }
            if ("STRUCTURE_FOUND".equals(state)) matched++;
            else if ("AMBIGUOUS".equals(state)) ambiguous++;
            else missing++;
            report.append("• ").append(target.label).append("\n   ")
                  .append(state).append(" (not proof of feature eligibility)\n");
        }
        report.append("\nMatched structures: ").append(matched)
              .append(" / ").append(CANDIDATES.length)
              .append(" | ambiguous: ").append(ambiguous)
              .append(" | unavailable: ").append(missing)
              .append("\n\nFEATURE STATUS\n")
              .append("Repost Feed button: NOT VERIFIED\n")
              .append("Repost Reels button: NOT VERIFIED\n")
              .append("Liked-by friend avatars: NOT VERIFIED\n")
              .append("Reposted-by friend avatars: NOT VERIFIED\n")
              .append("Floating friendly bubbles: NOT VERIFIED\n\n")
              .append("InstaEclipse Disable repost must be OFF for native repost actions.\n")
              .append("A hidden button may require account/server eligibility.\n")
              .append("Empty social data cannot be turned into real friend avatars.\n")
              .append("The IG455 Scan is read-only; HF08 has a separate opt-in, bounded UI gate trial. No native repost action is dispatched by this scan.\n")
              .append("No usernames, IDs, Media values or URLs collected.");
        return report.toString();
    }
}
