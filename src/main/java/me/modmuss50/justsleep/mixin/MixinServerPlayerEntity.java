package me.modmuss50.justsleep.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import me.modmuss50.justsleep.JustSleep;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity {

	private static final Logger LOGGER = LogManager.getLogger();
	private boolean isSleeping = false;

	public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile profile) {
		super(world, pos, yaw, profile);
	}

	@Inject(method = "setSpawnPoint", at = @At("HEAD"), cancellable = true)
	public void setSpawnPoint(RegistryKey<World> dimension, BlockPos pos, float angle, boolean spawnPointSet, boolean fromBlock, CallbackInfo info) {
		// Skip setting spawn if it was from bed, but allow it if they were
		// sneaking and it is day (otherwise no way to set spawn during day)
		if (JustSleep.hasValidSpawnPoint((ServerPlayerEntity) (Object) this)) {
			boolean isSneaking = this.isSneaking();
			if (isSleeping && !(this.world.isDay() && isSneaking)) {
				info.cancel();
			}
		}
		isSleeping = false;
	}

	@Inject(method = "setSpawnPoint", at = @At("RETURN"))
	public void updateSpawnPoint(RegistryKey<World> dimension, BlockPos pos, float angle, boolean spawnPointSet, boolean fromBlock, CallbackInfo info) {
		if (!world.isClient) {
			JustSleep.updateSpawnPointMap((ServerPlayerEntity) (Object) this);
		}
	}

	@Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
	public void trySleep(BlockPos pos, CallbackInfoReturnable<Either<SleepFailureReason, Unit>> cir) {
		if (!world.isClient) {
			JustSleep.updateSpawnPointMap((ServerPlayerEntity) (Object) this);
		}
		this.isSleeping = true;
	}

	@Inject(method = "onDisconnect", at = @At("HEAD"))
	public void removeFromSpawnPointMapOnDisconnect(CallbackInfo ci) {
		LOGGER.info("JustSleep: Removing user[" + this.uuidString + "] from the spawn point map.");
		JustSleep.removeUserFromSpawnPointMap(this.uuidString);
	}
}