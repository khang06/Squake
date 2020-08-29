package squeek.quakemovement;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class QuakeClientPlayer
{
	private static Random random = new Random();
	private static List<double[]> baseVelocities = new ArrayList<double[]>();

	public static boolean travel(PlayerEntity player, Vec3d movementInput)
	{
		if (!player.world.isClient)
			return false;

		if (!ModConfig.ENABLED)
			return false;

		boolean didQuakeMovement;

		if ((player.abilities.flying || player.isFallFlying()) && !player.hasVehicle())
			return false;
		else
			didQuakeMovement = quake_travel(player, movementInput);

		return didQuakeMovement;
	}

	public static void beforeOnLivingUpdate(PlayerEntity player)
	{
		if (!player.world.isClient)
			return;

		if (!baseVelocities.isEmpty())
		{
			baseVelocities.clear();
		}
	}

	public static boolean updateVelocity(Entity entity, Vec3d movementInput, float movementSpeed)
	{
		if (!(entity instanceof PlayerEntity))
			return false;

		return updateVelocityPlayer((PlayerEntity) entity, movementInput, movementSpeed);
	}

	public static boolean updateVelocityPlayer(PlayerEntity player, Vec3d movementInput, float movementSpeed)
	{
		if (!player.world.isClient)
			return false;

		if (!ModConfig.ENABLED)
			return false;

		if ((player.abilities.flying && !player.hasVehicle()) || player.isSubmergedIn(FluidTags.WATER) || player.isInLava() || !player.abilities.flying)
		{
			return false;
		}

		// this is probably wrong, but its what was there in 1.10.2
		float wishspeed = movementSpeed;
		wishspeed *= 2.15f;
		double[] wishdir = getMovementDirection(player, movementInput.x, movementInput.z);
		double[] wishvel = new double[]{wishdir[0] * wishspeed, wishdir[1] * wishspeed};
		baseVelocities.add(wishvel);

		return true;
	}

	public static void afterJump(PlayerEntity player)
	{
		if (!player.world.isClient)
			return;

		if (!ModConfig.ENABLED)
			return;

		// undo this dumb thing
		if (player.isSprinting())
		{
			float f = player.yaw * 0.017453292F;
			Vec3d deltaVelocity = new Vec3d(MathHelper.sin(f) * 0.2F, 0, -(MathHelper.cos(f) * 0.2F));
			player.setVelocity(player.getVelocity().add(deltaVelocity));
		}

		quake_Jump(player);
	}

	/* =================================================
	 * START HELPERS
	 * =================================================
	 */

	private static double getSpeed(PlayerEntity player)
	{
		Vec3d velocity = player.getVelocity();
		return MathHelper.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
	}

	private static float getSurfaceFriction(PlayerEntity player)
	{
		float f2 = 1.0F;

		if (player.isOnGround())
		{
			Vec3d pos = player.getPos();
			BlockPos groundPos = new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(player.getBoundingBox().minY) - 1, MathHelper.floor(pos.z));
			Block ground = player.world.getBlockState(groundPos).getBlock();
			f2 = 1.0F - ground.getSlipperiness();
		}

		return f2;
	}

	private static float getSlipperiness(PlayerEntity player)
	{
		float f2 = 0.91F;
		if (player.isOnGround())
		{
			f2 = 0.54600006F;
			Vec3d pos = player.getPos();
			BlockPos groundPos = new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(player.getBoundingBox().minY) - 1, MathHelper.floor(pos.z));
			Block ground = player.world.getBlockState(groundPos).getBlock();

			f2 = ground.getSlipperiness() * 0.91F;
		}
		return f2;
	}

	private static float minecraft_getMoveSpeed(PlayerEntity player)
	{
		float f2 = getSlipperiness(player);

		float f3 = 0.16277136F / (f2 * f2 * f2);

		return player.getMovementSpeed() * f3;
	}

	private static double[] getMovementDirection(PlayerEntity player, double sidemove, double forwardmove)
	{
		double f3 = sidemove * sidemove + forwardmove * forwardmove;
		double[] dir = {0.0F, 0.0F};

		if (f3 >= 1.0E-4F)
		{
			f3 = MathHelper.sqrt(f3);

			if (f3 < 1.0F)
			{
				f3 = 1.0F;
			}

			f3 = 1.0F / f3;
			sidemove *= f3;
			forwardmove *= f3;
			double f4 = MathHelper.sin(player.yaw * (float) Math.PI / 180.0F);
			double f5 = MathHelper.cos(player.yaw * (float) Math.PI / 180.0F);
			dir[0] = (sidemove * f5 - forwardmove * f4);
			dir[1] = (forwardmove * f5 + sidemove * f4);
		}

		return dir;
	}

	private static float quake_getMoveSpeed(PlayerEntity player)
	{
		float baseSpeed = player.getMovementSpeed();
		return !player.isSneaking() ? baseSpeed * 2.15F : baseSpeed * 1.11F;
	}

	private static float quake_getMaxMoveSpeed(PlayerEntity player)
	{
		float baseSpeed = player.getMovementSpeed();
		return baseSpeed * 2.15F;
	}

	private static void spawnBunnyhopParticles(PlayerEntity player, int numParticles)
	{
		Vec3d pos = player.getPos();
		// taken from sprint
		int j = MathHelper.floor(pos.x);
		int i = MathHelper.floor(pos.y - 0.20000000298023224D - player.getHeightOffset());
		int k = MathHelper.floor(pos.z);
		BlockState blockState = player.world.getBlockState(new BlockPos(j, i, k));

		if (blockState.getRenderType() != BlockRenderType.INVISIBLE)
		{
			for (int iParticle = 0; iParticle < numParticles; iParticle++)
			{
				player.world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK, blockState), pos.x + (random.nextFloat() - 0.5D) * player.getWidth(), pos.y + 0.1D, pos.z + (random.nextFloat() - 0.5D) * player.getWidth(), -player.getVelocity().x * 4.0D, 1.5D, -player.getVelocity().z * 4.0D);
			}
		}
	}

	public static interface IsJumpingGetter
	{
		boolean isJumping();
	}

	private static boolean isJumping(PlayerEntity player)
	{
		return ((IsJumpingGetter) player).isJumping();
	}

	/* =================================================
	 * END HELPERS
	 * =================================================
	 */

	/* =================================================
	 * START MINECRAFT PHYSICS
	 * =================================================
	 */

	private static void minecraft_ApplyGravity(PlayerEntity player)
	{
		Vec3d playerPos = player.getPos();
		BlockPos pos = new BlockPos((int) playerPos.x, (int) playerPos.y, (int) playerPos.z);
		double velocityY = player.getVelocity().y;
		if (player.world.isClient && !player.world.isChunkLoaded(pos))
		{
			if (playerPos.y > 0.0D)
			{
				velocityY = -0.1D;
			}
			else
			{
				velocityY = 0.0D;
			}
		}
		else
		{
			// gravity
			velocityY -= 0.08D;
		}

		// air resistance
		velocityY *= 0.9800000190734863D;
		player.setVelocity(player.getVelocity().x, velocityY, player.getVelocity().z);
	}

	private static void minecraft_ApplyFriction(PlayerEntity player, float momentumRetention)
	{
		player.setVelocity(player.getVelocity().multiply(momentumRetention, 1, momentumRetention));
	}

	/*
	private static void minecraft_ApplyLadderPhysics(PlayerEntity player)
	{
		if (player.canClimb())
		{
			float f5 = 0.15F;

			if (player.velocityX < (-f5))
			{
				player.velocityX = (-f5);
			}

			if (player.velocityX > f5)
			{
				player.velocityX = f5;
			}

			if (player.velocityZ < (-f5))
			{
				player.velocityZ = (-f5);
			}

			if (player.velocityZ > f5)
			{
				player.velocityZ = f5;
			}

			player.fallDistance = 0.0F;

			if (player.velocityY < -0.15D)
			{
				player.velocityY = -0.15D;
			}

			boolean flag = player.isSneaking();

			if (flag && player.velocityY < 0.0D)
			{
				player.velocityY = 0.0D;
			}
		}
	}

	private static void minecraft_ClimbLadder(PlayerEntity player)
	{
		if (player.horizontalCollision && player.canClimb())
		{
			player.velocityY = 0.2D;
		}
	}
	*/

	private static void minecraft_SwingLimbsBasedOnMovement(PlayerEntity player)
	{
		player.lastLimbDistance = player.limbDistance;
		Vec3d pos = player.getPos();
		double d0 = pos.x - player.prevX;
		double d1 = pos.z - player.prevZ;
		float f6 = MathHelper.sqrt(d0 * d0 + d1 * d1) * 4.0F;

		if (f6 > 1.0F)
		{
			f6 = 1.0F;
		}

		player.limbDistance += (f6 - player.limbDistance) * 0.4F;
		player.limbAngle += player.limbDistance;
	}

	private static void minecraft_WaterMove(PlayerEntity player, Vec3d movementInput)
	{
		//		double d0 = player.y;
		//		player.updateVelocity(0.04F, movementInput);
		//		player.move(MovementType.SELF, player.getVelocity());
		//		Vec3d velocity = player.getVelocity().multiply(0.800000011920929D).add(0, 0.02D, 0);
		//		player.setVelocity(velocity);
		//
		//		if (player.horizontalCollision && player.method_5654(velocity.x, velocity.y + 0.6000000238418579D - player.y + d0, velocity.z))
		//		{
		//			player.setVelocity(velocity.x, 0.30000001192092896D, velocity.z);
		//		}
	}

	/*
	public static void minecraft_moveEntityWithHeading(PlayerEntity player, float sidemove, float upmove, float forwardmove)
	{
		// take care of water and lava movement using default code
		if ((player.isInWater() && !player.abilities.flying)
			|| (player.isTouchingLava() && !player.abilities.flying))
		{
			player.travel(sidemove, upmove, forwardmove);
		}
		else
		{
			// get friction
			float momentumRetention = getSlipperiness(player);

			// alter motionX/motionZ based on desired movement
			player.moveRelative(sidemove, upmove, forwardmove, minecraft_getMoveSpeed(player));

			// make adjustments for ladder interaction
			minecraft_ApplyLadderPhysics(player);

			// do the movement
			player.move(MovementType.field_6308, player.velocityX, player.velocityY, player.velocityZ);

			// climb ladder here for some reason
			minecraft_ClimbLadder(player);

			// gravity + friction
			minecraft_ApplyGravity(player);
			minecraft_ApplyFriction(player, momentumRetention);

			// swing them arms
			minecraft_SwingLimbsBasedOnMovement(player);
		}
	}
	*/

	/* =================================================
	 * END MINECRAFT PHYSICS
	 * =================================================
	 */

	/* =================================================
	 * START QUAKE PHYSICS
	 * =================================================
	 */

	/**
	 * Moves the entity based on the specified heading.  Args: strafe, forward
	 */
	public static boolean quake_travel(PlayerEntity player, Vec3d movementInput)
	{
		// take care of ladder movement using default code
		if (player.isClimbing())
		{
			return false;
		}
		// take care of lava movement using default code
		else if ((player.isInLava() && !player.abilities.flying))
		{
			return false;
		}
		else if (player.isSubmergedInWater() && !player.abilities.flying)
		{
			if (ModConfig.SHARKING_ENABLED)
				quake_WaterMove(player, (float) movementInput.x, (float) movementInput.y, (float) movementInput.z);
			else
				return false;
		}
		else
		{
			// get all relevant movement values
			float wishspeed = (movementInput.x != 0.0D || movementInput.z != 0.0D) ? quake_getMoveSpeed(player) : 0.0F;
			double[] wishdir = getMovementDirection(player, movementInput.x, movementInput.z);
			boolean onGroundForReal = player.isOnGround() && !isJumping(player);
			float momentumRetention = getSlipperiness(player);

			// ground movement
			if (onGroundForReal)
			{
				// apply friction before acceleration so we can accelerate back up to maxspeed afterwards
				//quake_Friction(); // buggy because material-based friction uses a totally different format
				minecraft_ApplyFriction(player, momentumRetention);

				double sv_accelerate = ModConfig.ACCELERATE;

				if (wishspeed != 0.0F)
				{
					// alter based on the surface friction
					sv_accelerate *= minecraft_getMoveSpeed(player) * 2.15F / wishspeed;

					quake_Accelerate(player, wishspeed, wishdir[0], wishdir[1], sv_accelerate);
				}

				if (!baseVelocities.isEmpty())
				{
					float speedMod = wishspeed / quake_getMaxMoveSpeed(player);
					// add in base velocities
					for (double[] baseVel : baseVelocities)
					{
						player.setVelocity(player.getVelocity().add(baseVel[0] * speedMod, 0, baseVel[1] * speedMod));
					}
				}
			}
			// air movement
			else
			{
				double sv_airaccelerate = ModConfig.AIR_ACCELERATE;
				quake_AirAccelerate(player, wishspeed, wishdir[0], wishdir[1], sv_airaccelerate);

				/*
				if (ModConfig.SHARKING_ENABLED && ModConfig.SHARKING_SURFACE_TENSION > 0.0D && isJumping(player) && player.getVelocity().y < 0.0F)
				{
					Box axisalignedbb = player.getBoundingBox().offset(player.getVelocity());
					boolean isFallingIntoWater = player.world.containsBlockWithMaterial(axisalignedbb, Material.WATER);

					if (isFallingIntoWater)
						player.setVelocity(player.getVelocity().multiply(1.0D, ModConfig.SHARKING_SURFACE_TENSION, 1.0D));
				}
				*/
			}

			// apply velocity
			player.move(MovementType.SELF, player.getVelocity());

			// HL2 code applies half gravity before acceleration and half after acceleration, but this seems to work fine
			minecraft_ApplyGravity(player);
		}

		// swing them arms
		minecraft_SwingLimbsBasedOnMovement(player);

		return true;
	}

	private static void quake_Jump(PlayerEntity player)
	{
		quake_ApplySoftCap(player, quake_getMaxMoveSpeed(player));

		boolean didTrimp = quake_DoTrimp(player);

		if (!didTrimp)
		{
			quake_ApplyHardCap(player, quake_getMaxMoveSpeed(player));
		}
	}

	private static boolean quake_DoTrimp(PlayerEntity player)
	{
		if (ModConfig.TRIMPING_ENABLED && player.isSneaking())
		{
			double curspeed = getSpeed(player);
			float movespeed = quake_getMaxMoveSpeed(player);
			if (curspeed > movespeed)
			{
				double speedbonus = curspeed / movespeed * 0.5F;
				if (speedbonus > 1.0F)
					speedbonus = 1.0F;

				player.setVelocity(player.getVelocity().add(0, speedbonus * curspeed * ModConfig.TRIMP_MULTIPLIER, 0));

				if (ModConfig.TRIMP_MULTIPLIER > 0)
				{
					float mult = 1.0f / ModConfig.TRIMP_MULTIPLIER;
					player.setVelocity(player.getVelocity().multiply(mult, 1, mult));
				}

				spawnBunnyhopParticles(player, 30);

				return true;
			}
		}

		return false;
	}

	private static void quake_ApplyWaterFriction(PlayerEntity player, double friction)
	{
		player.setVelocity(player.getVelocity().multiply(friction));

		/*
		float speed = (float)(player.getSpeed());
		float newspeed = 0.0F;
		if (speed != 0.0F)
		{
			newspeed = speed - 0.05F * speed * friction; //* player->m_surfaceFriction;

			float mult = newspeed/speed;
			player.velocityX *= mult;
			player.velocityY *= mult;
			player.velocityZ *= mult;
		}

		return newspeed;
		*/

		/*
		// slow in water
		player.velocityX *= 0.800000011920929D;
		player.velocityY *= 0.800000011920929D;
		player.velocityZ *= 0.800000011920929D;
		*/
	}

	@SuppressWarnings("unused")
	private static void quake_WaterAccelerate(PlayerEntity player, float wishspeed, float speed, double wishX, double wishZ, double accel)
	{
		float addspeed = wishspeed - speed;
		if (addspeed > 0)
		{
			float accelspeed = (float) (accel * wishspeed * 0.05F);
			if (accelspeed > addspeed)
			{
				accelspeed = addspeed;
			}

			Vec3d newVelocity = player.getVelocity().add(accelspeed * wishX, 0, accelspeed * wishZ);
			player.setVelocity(newVelocity);
		}
	}

	private static void quake_WaterMove(PlayerEntity player, float sidemove, float upmove, float forwardmove)
	{
		Vec3d pos = player.getPos();
		double lastPosY = pos.y;

		// get all relevant movement values
		float wishspeed = (sidemove != 0.0F || forwardmove != 0.0F) ? quake_getMaxMoveSpeed(player) : 0.0F;
		double[] wishdir = getMovementDirection(player, sidemove, forwardmove);
		boolean isOffsetInLiquid = player.world.getBlockState(new BlockPos(pos.x, pos.y + 1.0D, pos.z)).getFluidState().isEmpty();
		boolean isSharking = isJumping(player) && isOffsetInLiquid;
		double curspeed = getSpeed(player);

		if (!isSharking || curspeed < 0.078F)
		{
			minecraft_WaterMove(player, new Vec3d(sidemove, upmove, forwardmove));
		}
		else
		{
			if (curspeed > 0.09)
				quake_ApplyWaterFriction(player, ModConfig.SHARKING_WATER_FRICTION);

			if (curspeed > 0.098)
				quake_AirAccelerate(player, wishspeed, wishdir[0], wishdir[1], ModConfig.ACCELERATE);
			else
				quake_Accelerate(player, .0980F, wishdir[0], wishdir[1], ModConfig.ACCELERATE);

			player.move(MovementType.SELF, player.getVelocity());

			player.setVelocity(player.getVelocity().x, 0, player.getVelocity().z);
		}

		// water jump
		if (player.horizontalCollision && player.doesNotCollide(player.getVelocity().x, player.getVelocity().y + 0.6000000238418579D - pos.y + lastPosY, player.getVelocity().z))
		{
			player.setVelocity(player.getVelocity().x, 0.30000001192092896D, player.getVelocity().z);
		}

		if (!baseVelocities.isEmpty())
		{
			float speedMod = wishspeed / quake_getMaxMoveSpeed(player);
			// add in base velocities
			for (double[] baseVel : baseVelocities)
			{
				player.setVelocity(player.getVelocity().add(baseVel[0] * speedMod, 0, baseVel[1] * speedMod));
			}
		}
	}

	private static void quake_Accelerate(PlayerEntity player, float wishspeed, double wishX, double wishZ, double accel)
	{
		double addspeed, accelspeed, currentspeed;

		// Determine veer amount
		// this is a dot product
		currentspeed = player.getVelocity().x * wishX + player.getVelocity().z * wishZ;

		// See how much to add
		addspeed = wishspeed - currentspeed;

		// If not adding any, done.
		if (addspeed <= 0)
			return;

		// Determine acceleration speed after acceleration
		accelspeed = accel * wishspeed / getSlipperiness(player) * 0.05F;

		// Cap it
		if (accelspeed > addspeed)
			accelspeed = addspeed;

		// Adjust pmove vel.
		player.setVelocity(player.getVelocity().add(accelspeed * wishX, 0, accelspeed * wishZ));
	}

	private static void quake_AirAccelerate(PlayerEntity player, float wishspeed, double wishX, double wishZ, double accel)
	{
		double addspeed, accelspeed, currentspeed;

		float wishspd = wishspeed;
		float maxAirAcceleration = (float) ModConfig.MAX_AIR_ACCEL_PER_TICK;

		if (wishspd > maxAirAcceleration)
			wishspd = maxAirAcceleration;

		// Determine veer amount
		// this is a dot product
		currentspeed = player.getVelocity().x * wishX + player.getVelocity().z * wishZ;

		// See how much to add
		addspeed = wishspd - currentspeed;

		// If not adding any, done.
		if (addspeed <= 0)
			return;

		// Determine acceleration speed after acceleration
		accelspeed = accel * wishspeed * 0.05F;

		// Cap it
		if (accelspeed > addspeed)
			accelspeed = addspeed;

		// Adjust pmove vel.
		player.setVelocity(player.getVelocity().add(accelspeed * wishX, 0, accelspeed * wishZ));
	}

	@SuppressWarnings("unused")
	private static void quake_Friction(PlayerEntity player)
	{
		double speed, newspeed, control;
		float friction;
		float drop;

		// Calculate speed
		speed = getSpeed(player);

		// If too slow, return
		if (speed <= 0.0F)
		{
			return;
		}

		drop = 0.0F;

		// convars
		float sv_friction = 1.0F;
		float sv_stopspeed = 0.005F;

		float surfaceFriction = getSurfaceFriction(player);
		friction = sv_friction * surfaceFriction;

		// Bleed off some speed, but if we have less than the bleed
		//  threshold, bleed the threshold amount.
		control = (speed < sv_stopspeed) ? sv_stopspeed : speed;

		// Add the amount to the drop amount.
		drop += control * friction * 0.05F;

		// scale the velocity
		newspeed = speed - drop;
		if (newspeed < 0.0F)
			newspeed = 0.0F;

		if (newspeed != speed)
		{
			// Determine proportion of old speed we are using.
			newspeed /= speed;
			// Adjust velocity according to proportion.
			player.setVelocity(player.getVelocity().multiply(newspeed, 1, newspeed));

		}
	}

	private static void quake_ApplySoftCap(PlayerEntity player, float movespeed)
	{
		float softCapPercent = ModConfig.SOFT_CAP;
		float softCapDegen = ModConfig.SOFT_CAP_DEGEN;

		if (ModConfig.UNCAPPED_BUNNYHOP_ENABLED)
		{
			softCapPercent = 1.0F;
			softCapDegen = 1.0F;
		}

		float speed = (float) (getSpeed(player));
		float softCap = movespeed * softCapPercent;

		// apply soft cap first; if soft -> hard is not done, then you can continually trigger only the hard cap and stay at the hard cap
		if (speed > softCap)
		{
			if (softCapDegen != 1.0F)
			{
				float applied_cap = (speed - softCap) * softCapDegen + softCap;
				float multi = applied_cap / speed;
				player.setVelocity(player.getVelocity().multiply(multi, 1, multi));
			}

			spawnBunnyhopParticles(player, 10);
		}
	}

	private static void quake_ApplyHardCap(PlayerEntity player, float movespeed)
	{
		if (ModConfig.UNCAPPED_BUNNYHOP_ENABLED)
			return;

		float hardCapPercent = ModConfig.HARD_CAP;

		float speed = (float) (getSpeed(player));
		float hardCap = movespeed * hardCapPercent;

		if (speed > hardCap && hardCap != 0.0F)
		{
			float multi = hardCap / speed;
			player.setVelocity(player.getVelocity().multiply(multi, 1, multi));

			spawnBunnyhopParticles(player, 30);
		}
	}

	/* =================================================
	 * END QUAKE PHYSICS
	 * =================================================
	 */
}
