package the_fireplace.lessforgivingvoid;

import com.google.common.collect.HashMultimap;
import net.minecraft.block.BlockPortal;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.DuplicateModsFoundException;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;
import java.util.Random;

@Mod(modid = LessForgivingVoid.MOD_ID, name = "Less Forgiving Void", acceptedMinecraftVersions = "[1.10.2,)", acceptableRemoteVersions = "*", dependencies = "after:forgivingvoid", updateJSON = "https://bitbucket.org/The_Fireplace/minecraft-mod-updates/raw/master/lfv.json")
@Mod.EventBusSubscriber
public class LessForgivingVoid {

	public static final String MOD_ID = "lessforgivingvoid";

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if(event.side == Side.SERVER && event.phase == TickEvent.Phase.START) {
			if(event.player.getPosition().getY() < ModConfig.triggerAtY) {
				event.player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, 60, 3));
				Random rand = new Random();
				BlockPos spawnPos = new BlockPos(event.player.getPosition().getX()/8-ModConfig.baseDistanceOffset *2+rand.nextInt(ModConfig.baseDistanceOffset *4), rand.nextInt(100)+16, event.player.getPosition().getZ()/8-ModConfig.baseDistanceOffset *2+rand.nextInt(ModConfig.baseDistanceOffset *4));
				event.player.setPortal(spawnPos);
				if(event.player.changeDimension(-1) != null) {
					int expandMult = 1;
					while(event.player.world.getBlockState(spawnPos).isNormalCube()){
						spawnPos = new BlockPos(event.player.getPosition().getX()/8-ModConfig.baseDistanceOffset *2*expandMult+rand.nextInt(ModConfig.baseDistanceOffset *4*expandMult), rand.nextInt(100)+16, event.player.getPosition().getZ()/8-ModConfig.baseDistanceOffset *2*expandMult+rand.nextInt(ModConfig.baseDistanceOffset *4*expandMult));
						event.player.setPortal(spawnPos);
						expandMult++;
					}
					event.player.setPositionAndUpdate(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
					//Even though the player doesn't spawn a portal by doing this, their sudden appearance should destroy any existing portal in the spot they appear.
					for(int x=event.player.getPosition().getX()-2;x<=event.player.getPosition().getX()+2;x++){
						for(int y=event.player.getPosition().getY()-3;y<=event.player.getPosition().getY()+3;y++){
							for(int z=event.player.getPosition().getZ()-2;z<=event.player.getPosition().getZ()+2;z++){
								if(event.player.world.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockPortal) {
									event.player.world.setBlockToAir(new BlockPos(x, y, z));
									event.player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 2.0f);
								}
							}
						}
					}

					if(ModConfig.dropObsidian)
						event.player.world.spawnEntity(new EntityItem(event.player.world, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), new ItemStack(Blocks.OBSIDIAN, 10)));
				}else{
					event.player.setPositionAndUpdate(event.player.getPosition().getX()-ModConfig.baseDistanceOffset *16+rand.nextInt(ModConfig.baseDistanceOffset *32), rand.nextInt(ModConfig.baseDistanceOffset)+ModConfig.baseDistanceOffset /2, event.player.getPosition().getZ()-ModConfig.baseDistanceOffset *16+rand.nextInt(ModConfig.baseDistanceOffset *32));
				}
				event.player.getEntityData().setBoolean("ForgivingVoidNoFallDamage", true);
			}
		}
	}

	@SubscribeEvent
	public static void onPlayerFall(LivingFallEvent event) {
		if(event.getEntity() instanceof EntityPlayer) {
			if(event.getEntity().getEntityData().getBoolean("ForgivingVoidNoFallDamage")) {
				float damage = ModConfig.damageOnFall;
				if(ModConfig.preventDeath && event.getEntityLiving().getHealth() - damage <= 0) {
					damage = event.getEntityLiving().getHealth() - 1f;
				}
				event.getEntity().attackEntityFrom(DamageSource.FALL, damage);
				event.setDamageMultiplier(0f);
				event.setCanceled(true);
				event.getEntity().getEntityData().setBoolean("ForgivingVoidNoFallDamage", false);
			}
		}
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event){
		if(Loader.isModLoaded("forgivingvoid")) {//stop loading, either this mod won't work, or Forgiving Void won't. Either way, users need to pick which one they want to use.
			HashMultimap<ModContainer, File> map = HashMultimap.create();
			ModContainer thisContainer = Loader.instance().getIndexedModList().get(MOD_ID);
			ModContainer thatContainer = Loader.instance().getIndexedModList().get("forgivingvoid");
			map.put(thisContainer, thisContainer.getSource());
			map.put(thatContainer, thatContainer.getSource());
			throw new DuplicateModsFoundException(map);
		}
	}

	@Config(modid = MOD_ID)
	public static class ModConfig {
		@Config.Comment("The y level at which Less Forgiving Void should send the player to the Nether.")
		@Config.RangeInt(min = -64, max = 0)
		public static int triggerAtY = -32;

		@Config.Comment("The amount of damage applied to the player when they land.")
		@Config.RangeInt(min = 0, max = 40)
		public static int damageOnFall = 19;

		@Config.Comment("Prevent death by fall damage when you fall to the nether (limits damage to leave at least 0.5 hearts)")
		public static boolean preventDeath = true;

		@Config.Comment("Drop obsidian for the player when they appear in the Nether")
		public static boolean dropObsidian = true;

		@Config.Comment("The base offset from which the player will spawn from after falling through the void. This is not going to be the exact offset. Height is random.")
		@Config.RangeInt(min = 128, max = 8192)
		public static int baseDistanceOffset = 512;
	}
}