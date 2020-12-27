package me.modmuss50.justsleep.mixin;

import me.modmuss50.justsleep.JustSleep;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.SleepingChatScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(SleepingChatScreen.class)
public abstract class MixinGuiChatSleeping extends ChatScreen {

	private ButtonWidget button;
	private boolean didSetSpawn = false;

	public MixinGuiChatSleeping(String string_1) {
		super(string_1);
	}

	@Inject(method = "init", at = @At("RETURN"))
	protected void init(CallbackInfo info) {
		button = new ButtonWidget(width / 2 - 100, height - 62, 200, 20, new LiteralText("Set Spawn"), (widget) -> {
			didSetSpawn = true;
			ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
			if (networkHandler != null) {
				networkHandler.getConnection().send(JustSleep.createSetSpawnPacket());
			}
		});
		button.visible = false;
		addButton(button);
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		super.render(matrices, mouseX, mouseY, delta);
		PlayerEntity player = MinecraftClient.getInstance().player;
		String currentDimensionId = player.world.getRegistryKey().getValue().toString();

		JustSleep.SpawnPoint spawnPoint = JustSleep.getSpawnPoint(player);
		String spawnPointDimensionId = null;
		BlockPos spawnPointPos = null;
		if (spawnPoint != null) {
			spawnPointDimensionId = spawnPoint.getDimensionId();
			spawnPointPos = spawnPoint.getSpawnPoint();
		}

		boolean isCurrentSpawnPoint = player.getSleepingPosition().orElse(BlockPos.ORIGIN).equals(spawnPointPos) &&
				currentDimensionId.equals(spawnPointDimensionId);
		button.visible = JustSleep.hasValidSpawnPoint(player) && !isCurrentSpawnPoint && !didSetSpawn;
		if (didSetSpawn) {
			this.textRenderer.draw(matrices, "Spawn point updated to this bed", width / 2 - 80, height - 55, Color.GREEN.getRGB());
		} else if (isCurrentSpawnPoint) {
			this.textRenderer.draw(matrices, "This is the current spawn bed", width / 2 - 80, height - 55, Color.CYAN.getRGB());
		} else if (!JustSleep.hasValidSpawnPoint(player)) {
			this.textRenderer.draw(matrices, "Setting spawn point (No previous bed found)", width / 2 - 120, height - 55, Color.RED.getRGB());
		}
	}

}
