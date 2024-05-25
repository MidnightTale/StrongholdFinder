package net.hynse.strongholdfinder;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class StrongholdFinder extends JavaPlugin implements Listener {
    private static final Set<Material> INTERACTABLE_BLOCKS = EnumSet.of(
            Material.END_PORTAL_FRAME, Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST,
            Material.SHULKER_BOX, Material.CRAFTING_TABLE, Material.ANVIL, Material.LEVER,
            Material.ACACIA_DOOR, Material.BAMBOO_DOOR, Material.DARK_OAK_DOOR, Material.BIRCH_DOOR,
            Material.CHERRY_DOOR, Material.COPPER_DOOR, Material.CRIMSON_DOOR, Material.EXPOSED_COPPER_DOOR,
            Material.JUNGLE_DOOR, Material.MANGROVE_DOOR, Material.OAK_DOOR, Material.OXIDIZED_COPPER_DOOR,
            Material.SPRUCE_DOOR, Material.WARPED_DOOR, Material.WAXED_COPPER_DOOR, Material.WAXED_EXPOSED_COPPER_DOOR,
            Material.WAXED_WEATHERED_COPPER_DOOR, Material.WEATHERED_COPPER_DOOR,
            Material.ACACIA_TRAPDOOR, Material.BAMBOO_TRAPDOOR, Material.DARK_OAK_TRAPDOOR, Material.BIRCH_TRAPDOOR,
            Material.CHERRY_TRAPDOOR, Material.COPPER_TRAPDOOR, Material.CRIMSON_TRAPDOOR, Material.EXPOSED_COPPER_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR, Material.MANGROVE_TRAPDOOR, Material.OAK_TRAPDOOR, Material.OXIDIZED_COPPER_TRAPDOOR,
            Material.SPRUCE_TRAPDOOR, Material.WARPED_TRAPDOOR, Material.WAXED_COPPER_TRAPDOOR, Material.WAXED_EXPOSED_COPPER_TRAPDOOR,
            Material.WAXED_WEATHERED_COPPER_TRAPDOOR, Material.WEATHERED_COPPER_TRAPDOOR,
            Material.ACACIA_BUTTON, Material.BAMBOO_BUTTON, Material.DARK_OAK_BUTTON, Material.BIRCH_BUTTON,
            Material.CHERRY_BUTTON, Material.CRIMSON_BUTTON,
            Material.JUNGLE_BUTTON, Material.MANGROVE_BUTTON, Material.OAK_BUTTON,
            Material.SPRUCE_BUTTON, Material.WARPED_BUTTON,
            Material.STONE_BUTTON,
            Material.ACACIA_FENCE_GATE, Material.BAMBOO_FENCE_GATE, Material.DARK_OAK_FENCE_GATE, Material.BIRCH_FENCE_GATE,
            Material.CHERRY_FENCE_GATE, Material.CRIMSON_FENCE_GATE, Material.JUNGLE_FENCE_GATE, Material.MANGROVE_FENCE_GATE,
            Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.WARPED_FENCE_GATE,
            Material.BREWING_STAND, Material.BEACON, Material.BLAST_FURNACE, Material.BARREL,
            Material.CARTOGRAPHY_TABLE, Material.COMPOSTER, Material.ENCHANTING_TABLE, Material.END_PORTAL,
            Material.FURNACE, Material.GRINDSTONE, Material.HOPPER, Material.LOOM, Material.SMITHING_TABLE,
            Material.SMOKER, Material.STONECUTTER, Material.DAYLIGHT_DETECTOR
    );

    private List<Location> strongholds;
    private Logger logger;

    @Override
    public void onEnable() {
        this.logger = this.getLogger();
        this.strongholds = new ArrayList<>();
        saveDefaultConfigFile("stronghold.txt");
        loadStrongholds();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void saveDefaultConfigFile(String fileName) {
        File file = new File(getDataFolder(), fileName);
        if (!file.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource(fileName); OutputStream out = new FileOutputStream(file)) {
                if (in != null) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    logger.info("Default stronghold.txt file saved successfully.");
                }
            } catch (IOException e) {
                logger.severe("Error saving default stronghold.txt file: " + e.getMessage());
            }
        } else {
            logger.info("stronghold.txt already exists.");
        }
    }

    private void loadStrongholds() {
        File file = new File(getDataFolder(), "stronghold.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty() && !line.startsWith("seed")) {
                    String[] parts = line.split(";");
                    int x = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    this.strongholds.add(new Location(Bukkit.getWorlds().get(0), x, 64, z));
                    logger.info("Loaded stronghold location: " + x + ", " + z);
                }
            }
            logger.info("Stronghold locations loaded successfully.");
        } catch (IOException e) {
            logger.severe("Error loading stronghold locations: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerUse(PlayerInteractEvent event) {
        if (event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && INTERACTABLE_BLOCKS.contains(clickedBlock.getType())) {
                return;
            }

            Player player = event.getPlayer();
            Location nearestStronghold = getNearestStronghold(player.getLocation());
            if (nearestStronghold != null) {
                //logger.info("Player " + player.getName() + " used Ender Eye. Nearest stronghold at: " + nearestStronghold);
                EnderSignal signal = player.getWorld().spawn(player.getEyeLocation(), EnderSignal.class);

                signal.setTargetLocation(nearestStronghold);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 0.7f, 1.0f);
                if (event.getItem().getAmount() > 1 && player.getGameMode() != GameMode.CREATIVE) {
                    event.getItem().setAmount(event.getItem().getAmount() - 1);
                } else {
                    player.getInventory().removeItem(event.getItem());
                }
//            } else {
                //logger.info("No stronghold found for player " + player.getName());
            }
        }
    }

    private Location getNearestStronghold(Location playerLocation) {
        Location nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (Location stronghold : strongholds) {
            double distance = playerLocation.distanceSquared(stronghold);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = stronghold;
            }
        }

//        logger.info("Nearest stronghold distance: " + minDistance);
        return nearest;
    }
}
