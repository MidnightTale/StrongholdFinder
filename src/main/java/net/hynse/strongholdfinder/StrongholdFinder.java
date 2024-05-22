package net.hynse.strongholdfinder;

import org.bukkit.*;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class StrongholdFinder extends JavaPlugin implements Listener {

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
