package com.rabakilgur.darkcraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.WorldTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
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

	private void setupCommon(final FMLCommonSetupEvent event) { }

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
				// ((ServerWorld)event.world).getServer().getPlayerList().broadcastMessage(new StringTextComponent("test"), ChatType.SYSTEM, UUID.randomUUID()); // Broadcast message
				((ServerWorld)event.world).getServer().getPlayerList().getPlayers().forEach((player) -> {
					Integer[] timers = timerMap.get(player.getId());
					if (timers == null) {
						timerMap.put(player.getId(), new Integer[]{5,0});
						timers = new Integer[]{5,0};
					}
					Integer graceTimer = timers[0];
					Integer dmgTimer = timers[1];
					Integer light = player.getLevel().getBrightness(LightType.BLOCK, player.blockPosition());
					if (light == 0 && player.isAlive()) {
						if (graceTimer > 0) {
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
	}



	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class EventHandlerPlayerLoggedIn{
		@SubscribeEvent
		public static void onPlayerLoggedIn(PlayerLoggedInEvent event){
			PlayerEntity player = event.getPlayer();
			World world = player.level;
			CompoundNBT compountNBT = player.getPersistentData();
			if(!compountNBT.getBoolean("PLAYERALREADYJOINED")){
				BlockPos campFirePostiBlockPos = getCampFireBlockPos(world, player.blockPosition());
				world.setBlock(campFirePostiBlockPos, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), 3);
				compountNBT.putBoolean("PLAYERALREADYJOINED", true);
			}
		}

		private static BlockPos getCampFireBlockPos(World world, BlockPos playerBlockPos){
			BlockState air = Blocks.AIR.defaultBlockState();
			BlockState water = Blocks.WATER.defaultBlockState();
			List<BlockPos> possibleBlockPosList = new ArrayList<>();
			for(int y = 0; y < 9; y++){
				for(int z = 0; z < 9; z++){
					for(int x = 0; x < 9; x++){
						if(x == 4 && y == 4 && z ==4)
							continue;
						BlockPos blockPos = new BlockPos(playerBlockPos.getX()+4-x, playerBlockPos.getY()+4-y, playerBlockPos.getZ()+4-z);
						if( (world.getBlockState(blockPos).getMaterial() == Material.TOP_SNOW
						|| world.getBlockState(blockPos).getMaterial() == Material.REPLACEABLE_PLANT
						|| world.getBlockState(blockPos).equals(air))
						&& !world.getBlockState(blockPos.below()).equals(air)
						&& !world.getBlockState(blockPos.below()).equals(water)
						&& world.getBlockState(blockPos).getMaterial() != Material.LEAVES
						&& world.getBlockState(blockPos).getMaterial() != Material.PLANT)
							possibleBlockPosList.add(blockPos);
					}
				}
			}
			double random = Math.random();
			try{
				return possibleBlockPosList.get((int)(random * possibleBlockPosList.size()));
			}
			catch(Exception e){
				return playerBlockPos.below();
			}
		}
	}


	@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class EventHandlerBlockPlaced{
		private static List<BlockState> banedBlocks = getListOfBanedBlocks();
		@SubscribeEvent
		public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event){

			if(banedBlocks.contains(event.getPlacedBlock())){
				event.setCanceled(true);
				event.getWorld().playSound(null, event.getPos(), SoundEvents.ENDERMAN_SCREAM, SoundCategory.HOSTILE, 10.0f, 0.5f);
				//event.getWorld().addParticle(ParticleTypes.CRIMSON_SPORE, event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), 1D, 0D, 0D);
			}


		}

		private static List<BlockState> getListOfBanedBlocks(){

			List<BlockState> blockStateList = new ArrayList<>();
			blockStateList.add(Blocks.GLOWSTONE.defaultBlockState());
			blockStateList.add(Blocks.REDSTONE_LAMP.defaultBlockState());
			blockStateList.add(Blocks.SEA_PICKLE.defaultBlockState());
			blockStateList.add(Blocks.LANTERN.defaultBlockState());
			blockStateList.add(Blocks.SOUL_LANTERN.defaultBlockState());
			return blockStateList;
		}


	}
}
