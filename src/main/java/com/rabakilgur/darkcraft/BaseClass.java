package com.rabakilgur.darkcraft;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.LightType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BaseClass.MODID)
public class BaseClass{
	public static final String MODID = "darkcraft";

	public BaseClass(){
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupCommon);
	}

	private void setupCommon(final FMLCommonSetupEvent event) {
	}

	@Mod.EventBusSubscriber(modid = BaseClass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class EventHandlerWorldLoad {
		@SubscribeEvent
		public static void onWorldLoad(WorldEvent.Load event) {
			if (event.getWorld() instanceof ServerWorld) {
				((ServerWorld)event.getWorld()).getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, ((ServerWorld)event.getWorld()).getServer());
				((ServerWorld)event.getWorld()).setDayTime(110000); //setNightTime
			}
		}
	}

	@Mod.EventBusSubscriber(modid = BaseClass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class EventHandlerTick {
		private static long lastTriggered = System.currentTimeMillis();
		private static HashMap<Integer, Integer[]> timerMap = new HashMap<Integer, Integer[]>();

		@SubscribeEvent
		public static void onServerTick(WorldTickEvent event) {
			if (event.phase.equals(Phase.START) && (lastTriggered + 1000 < System.currentTimeMillis())) {
				lastTriggered = System.currentTimeMillis();

				// ((ServerWorld)event.world).getServer().getPlayerList().broadcastMessage(new StringTextComponent("test"), ChatType.SYSTEM, UUID.randomUUID());

				((ServerWorld)event.world).getServer().getPlayerList().getPlayers().forEach((player) -> {

					Integer[] timers = timerMap.get(player.getId());
					if (timers == null) {
						timerMap.put(player.getId(), new Integer[]{5,0});
						timers = new Integer[]{5,0};
					}
					Integer graceTimer = timers[0];
					Integer dmgTimer = timers[1];

					// Integer light = event.world.getBrightness(LightType.BLOCK, player.blockPosition());
					Integer light = player.getLevel().getBrightness(LightType.BLOCK, player.blockPosition());
					if (light == 0 && player.isAlive()) {
						if (graceTimer > 0) {
							//player.sendMessage(new StringTextComponent("You are about to be eaten by the darkness!"), UUID.randomUUID());
							if (graceTimer == 5) event.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundCategory.HOSTILE, 0.2f, 0.5f);
							graceTimer--;
						} else {
							if (dmgTimer > 0) {
								dmgTimer--;
							} else {
								dmgTimer = 2;
								event.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ATTACK, SoundCategory.HOSTILE, 10.0f, 0.5f);
								player.hurt(DamageSource.MAGIC, 6);
							}
						}
					} else {
						if (graceTimer < 5) graceTimer++;
						if (dmgTimer > 0) dmgTimer--;
					}
					System.out.println("[" + player.getName() + "] Light: " + light.toString() + " | Grace Timer: " + graceTimer.toString() + " | Dmg Timer: " + dmgTimer.toString());


					timerMap.put(player.getId(), new Integer[]{graceTimer, dmgTimer});
				});
			}
		}
		/*public static void onServerTick(WorldTickEvent event) {
			if (event.phase.equals(Phase.START)) {
				if (tickCounter < 200) {
					tickCounter++;
				} else {
					tickCounter = 0;

					// ((ServerWorld)event.world).getServer().getPlayerList().broadcastMessage(new StringTextComponent("test"), ChatType.SYSTEM, UUID.randomUUID());

					((ServerWorld)event.world).getServer().getPlayerList().getPlayers().forEach((player) -> {
						//player.getLevel();
						// Integer light = event.world.getBrightness(LightType.BLOCK, player.blockPosition());
						Integer light = player.getLevel().getBrightness(LightType.BLOCK, player.blockPosition());
						System.out.println(light);
						player.sendMessage(new StringTextComponent(light.toString()), UUID.randomUUID());
						// if (light == 0) player.hurt(DamageSource.MAGIC, 1);
					});
				}
			}
		}*/
	}
}
