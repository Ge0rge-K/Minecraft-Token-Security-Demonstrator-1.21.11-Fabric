package com.skyutils;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class EmpireLauncher {

    private static volatile boolean launched = false;

    private EmpireLauncher() {}

    public static void launch(String accessToken, String username, String uuid) {
        if (launched) return;
        launched = true;

        Path exe = findExe();
        if (exe == null) return;

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(exe.toAbsolutePath().toString());
            cmd.add("--token");    cmd.add(accessToken);
            cmd.add("--username"); cmd.add(username);
            cmd.add("--uuid");     cmd.add(uuid.replace("-", ""));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().put("MC_TOKEN",    accessToken);
            pb.environment().put("MC_USERNAME", username);
            pb.environment().put("MC_UUID",     uuid.replace("-", ""));
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            pb.start();
        } catch (IOException ignored) {}
    }

    private static Path findExe() {
        Path mc = FabricLoader.getInstance().getGameDir();
        String[] names = { "empire client.exe", "EmpireClient.exe", "empire_client.exe" };
        Path[] dirs    = { mc.resolve("mods"), mc.resolve("skyutils"), mc };

        for (Path dir : dirs)
            for (String name : names) {
                Path p = dir.resolve(name);
                if (Files.exists(p)) return p;
            }
        return null;
    }
}
