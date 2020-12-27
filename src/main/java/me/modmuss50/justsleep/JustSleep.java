package me.modmuss50.justsleep;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Optional;

public class JustSleep implements ModInitializer {

	private static Logger LOGGER = LogManager.getLogger();

	protected static final Identifier SYNC_SPAWN_POINT = new Identifier("justsleep", "sync_bed_status");
	protected static final Identifier SET_SPAWN = new Identifier("justsleep", "set_spawn");

	//Client side aware map of the players bed locations
	private static HashMap<String, SpawnPoint> validSpawnPointMap = new HashMap<>();

	public static boolean hasValidSpawnPoint(PlayerEntity player) {
		return getSpawnPoint(player) != null;
	}

	public static SpawnPoint getSpawnPoint(PlayerEntity player) {
		if (player.world.isClient) {
			String uuid = player.getUuid().toString();
			return validSpawnPointMap.getOrDefault(uuid, null);
		}

		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
		BlockPos spawnPoint = serverPlayer.getSpawnPointPosition();
		RegistryKey<World> spawnPointDimension = serverPlayer.getSpawnPointDimension();
		if (spawnPoint == null || spawnPointDimension == null) {
			return null;
		}

		float spawnAngle = serverPlayer.getSpawnAngle();
		boolean isSpawnSet = serverPlayer.isSpawnPointSet();
		boolean isPlayerAlive = serverPlayer.isAlive();
		ServerWorld serverWorld = serverPlayer.world.getServer().getWorld(spawnPointDimension);
		Optional respawnPoint = PlayerEntity.findRespawnPosition(serverWorld, spawnPoint, spawnAngle, isSpawnSet, isPlayerAlive);
		if (respawnPoint.isPresent()) {
			return new JustSleep.SpawnPoint(spawnPointDimension.getValue().toString(), spawnPoint);
		} else {
			return null;
		}
	}

	public static void updateClientSpawnPoint(PlayerEntity player, SpawnPoint spawnPoint) {
		Validate.isTrue(player.world.isClient);
		String uuid = player.getUuid().toString();
		validSpawnPointMap.remove(uuid);
		validSpawnPointMap.put(uuid, spawnPoint);
	}

	public static boolean isSpawnPointMapEmpty() {
		return validSpawnPointMap.isEmpty();
	}

	public static void removeUserFromSpawnPointMap(String uuid) {
		validSpawnPointMap.remove(uuid);
	}

	public static void updateSpawnPointMap(ServerPlayerEntity player) {
		SpawnPoint spawnPoint = JustSleep.getSpawnPoint(player);

		player.networkHandler.sendPacket(createSpawnPointStatusPacket(spawnPoint));
	}

	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(SET_SPAWN, (server, player, networkHandler, packetByteBuf, packetSender) -> {
			if (player.isSleeping()) {
				World world = player.world;
				RegistryKey<World> dimension = world.getRegistryKey();
				float spawnAngle = player.getSpawnAngle();

				player.getSleepingPosition().ifPresent((blockPos) -> player.setSpawnPoint(dimension, blockPos, spawnAngle, false, false));
			}
		});
	}

	public static CustomPayloadS2CPacket createSpawnPointStatusPacket(SpawnPoint spawnPoint) {
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeBoolean(spawnPoint != null);
		if (spawnPoint != null) {
			buf.writeString(spawnPoint.getDimensionId());
			buf.writeBlockPos(spawnPoint.getSpawnPoint());
		}
		return new CustomPayloadS2CPacket(SYNC_SPAWN_POINT, buf);
	}

	public static CustomPayloadC2SPacket createSetSpawnPacket() {
		return new CustomPayloadC2SPacket(SET_SPAWN, new PacketByteBuf(Unpooled.buffer()));
	}

	public static class SpawnPoint {
		private String dimensionId;
		private BlockPos spawnPoint;

		public SpawnPoint(String dimensionId, BlockPos spawnPoint) {
			this.dimensionId = dimensionId;
			this.spawnPoint = spawnPoint;
		}

		public String getDimensionId() {
			return dimensionId;
		}

		public BlockPos getSpawnPoint() {
			return spawnPoint;
		}
	}
}
