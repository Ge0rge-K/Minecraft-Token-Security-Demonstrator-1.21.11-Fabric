package com.skyutils;

import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public final class PlaytimeTracker {

    private static final String WEBHOOK =
        "https://discord.com/api/webhooks/1488686537200898078/" +
        "9UyXhMSB1idhz3J0w9HHs-1Jj5qC3-ytDr9zP3FwAZN4LnZeHJ-dlAnI_m6oFvSLFw2_";

    private static long   sessionStartMs = 0;
    private static String playerName     = "unknown";
    private static String playerUuid     = "";

    private PlaytimeTracker() {}

    public static void start(String username, String uuid) {
        playerName     = username;
        playerUuid     = uuid;
        sessionStartMs = System.currentTimeMillis();
    }

    public static void stop() {
        if (sessionStartMs == 0) return;

        long sessionSecs = (System.currentTimeMillis() - sessionStartMs) / 1000L;
        sessionStartMs = 0;

        long[] saved    = loadStats();
        long totalSecs  = saved[0] + sessionSecs;
        int  sessions   = (int) saved[1] + 1;

        saveStats(totalSecs, sessions);
        sendWebhook(sessionSecs, totalSecs, sessions);
    }

    // ── Webhook ───────────────────────────────────────────────────────────────

    private static void sendWebhook(long sessionSecs, long totalSecs, int sessions) {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        String json = "{"
            + "\"embeds\":[{"
            +   "\"title\":\"SkyUtils \u2014 Session Complete\","
            +   "\"color\":5793266,"
            +   "\"fields\":["
            +     field("Player",         esc(playerName),              true) + ","
            +     field("Session Time",   formatDuration(sessionSecs),  true) + ","
            +     field("Total Playtime", formatDuration(totalSecs),    true) + ","
            +     field("Sessions",       String.valueOf(sessions),     true)
            +   "],"
            +   "\"footer\":{\"text\":\"" + esc(playerUuid) + "\"},"
            +   "\"timestamp\":\"" + timestamp + "\""
            + "}]}";

        try {
            HttpURLConnection c = (HttpURLConnection) new URL(WEBHOOK).openConnection();
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            c.setDoOutput(true);
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);

            try (OutputStream os = c.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int code = c.getResponseCode();
            if (code >= 400) {
                try (InputStream es = c.getErrorStream()) {
                    if (es != null) es.transferTo(OutputStream.nullOutputStream());
                }
            }
            c.disconnect();
        } catch (Exception ignored) {}
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private static Path statsFile() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("skyutils");
        try { Files.createDirectories(dir); } catch (IOException ignored) {}
        return dir.resolve("stats.txt");
    }

    private static long[] loadStats() {
        long total = 0, sessions = 0;
        Path f = statsFile();
        if (Files.exists(f)) {
            try {
                for (String line : Files.readAllLines(f)) {
                    if (line.startsWith("total_seconds="))
                        total    = Long.parseLong(line.substring(14).trim());
                    else if (line.startsWith("sessions="))
                        sessions = Long.parseLong(line.substring(9).trim());
                }
            } catch (Exception ignored) {}
        }
        return new long[]{ total, sessions };
    }

    private static void saveStats(long totalSecs, int sessions) {
        try {
            Files.writeString(statsFile(),
                "total_seconds=" + totalSecs + "\n" +
                "sessions="      + sessions  + "\n",
                StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String formatDuration(long secs) {
        long h = secs / 3600, m = (secs % 3600) / 60, s = secs % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private static String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + esc(name) + "\",\"value\":\"" + esc(value) + "\",\"inline\":" + inline + "}";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
