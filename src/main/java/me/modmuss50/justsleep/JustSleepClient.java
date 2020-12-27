package me.modmuss50.justsleep;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

public class JustSleepClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(JustSleep.SYNC_SPAWN_POINT, (client, networkHandler, packetByteBuf, packetSender) -> {
            JustSleep.SpawnPoint spawnPoint = null;
            if (packetByteBuf.readBoolean()) {
                String dimensionId = packetByteBuf.readString();
                BlockPos spawnPointPos = packetByteBuf.readBlockPos();
                spawnPoint = new JustSleep.SpawnPoint(dimensionId, spawnPointPos);
            }

            JustSleep.updateClientSpawnPoint(client.player, spawnPoint);
        });
    }
}
