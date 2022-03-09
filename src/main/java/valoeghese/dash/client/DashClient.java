package valoeghese.dash.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import valoeghese.dash.Dash;
import valoeghese.dash.DashTracker;

public class DashClient implements ClientModInitializer {
	private static final ResourceLocation DASH_ICONS = new ResourceLocation("dtdash", "textures/dash_icons.png");
	private static KeyMapping dashKey;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(Dash.RESET_TIMER_PACKET, (client, handler, buf, responseSender) -> {
			client.player.resetAttackStrengthTicker();
		});

		dashKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.dtdash.dash",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(), // not bound by default
				"key.categories.movement"
		));
	}

	/**
	 * Needs to be run on load complete, as options are not initialised and loaded at init time.
	 */
	public static void onLoadComplete() {
		// If globally enabled
		DashInputHandler.FORWARD_DASH.setEnabled(Dash.config.dashDirections()[Dash.FORWARD]);
		DashInputHandler.BACKWARDS_DASH.setEnabled(Dash.config.dashDirections()[Dash.BACKWARDS]);
		DashInputHandler.LEFT_DASH.setEnabled(Dash.config.dashDirections()[Dash.LEFT]);
		DashInputHandler.RIGHT_DASH.setEnabled(Dash.config.dashDirections()[Dash.RIGHT]);
	}

	public static void renderBar(PoseStack stack, Gui gui) {
		Player player = Minecraft.getInstance().player;

		if (player != null) {
			float progress = Mth.clamp(((DashTracker) player).getDashCooldown(), 0, 1);

			if (progress < 1) {
				int blitHeight = (int) (progress * 32);
				stack.pushPose();
				RenderSystem.disableBlend();
				// set texture
				AbstractTexture texture = Minecraft.getInstance().getTextureManager().getTexture(DASH_ICONS);
				RenderSystem.setShaderTexture(0, texture.getId());
				// render
				Window window = Minecraft.getInstance().getWindow();
				gui.blit(stack, 8, window.getGuiScaledHeight() - 32 - 8, 0, 0, 32, 32); // render the background
				gui.blit(stack, 8, window.getGuiScaledHeight() - blitHeight - 8, 0, 32 + (32 - blitHeight), 32, blitHeight); // render the foreground
				stack.popPose();
			}
		}
	}

	// This code could be much better beautified by extracting commonalities but it's not gonna change any time soon and it's 1am
	public static boolean tryDash() {
		boolean attempted = false;
		boolean dashKeyPressed = dashKey.consumeClick();

		if (DashInputHandler.FORWARD_DASH.shouldDash(dashKeyPressed)) {
			DashInputHandler.FORWARD_DASH.reset();
			sendDash(Dash.FORWARD);
			attempted = true;
		}

		if (DashInputHandler.BACKWARDS_DASH.shouldDash(dashKeyPressed)) {
			DashInputHandler.BACKWARDS_DASH.reset();
			sendDash(Dash.BACKWARDS);
			attempted = true;
		}

		if (DashInputHandler.LEFT_DASH.shouldDash(dashKeyPressed)) {
			DashInputHandler.LEFT_DASH.reset();
			sendDash(Dash.LEFT);
			attempted = true;
		}

		if (DashInputHandler.RIGHT_DASH.shouldDash(dashKeyPressed)) {
			DashInputHandler.RIGHT_DASH.reset();
			sendDash(Dash.RIGHT);
			attempted = true;
		}

		return attempted;
	}

	private static void sendDash(int direction) {
		FriendlyByteBuf buf = PacketByteBufs.create();
		buf.writeByte(direction);
		ClientPlayNetworking.send(Dash.DASH_PACKET, buf);
	}
}
