package squeek.quakemovement;

import com.mojang.blaze3d.platform.GlStateManager;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ModQuakeMovement implements ModInitializer
{
	@Override
	public void onInitialize()
	{
	}

	public static float getFriction()
	{
		return 0.6f;
	}

	public static void drawSpeedometer()
	{
		GlStateManager.pushMatrix();
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		Vec3d pos = player.getPos();
		double deltaX = pos.x - player.prevX;
		double deltaZ = pos.z - player.prevZ;
		double speed = MathHelper.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0;
		String speedString = String.format("%.02f", speed);
		mc.textRenderer.drawWithShadow(new MatrixStack(), speedString, 10, mc.getWindow().getScaledHeight() - mc.textRenderer.fontHeight - 10, 0xFFDDDDDD);
		GlStateManager.popMatrix();
	}
}