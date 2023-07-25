package idk.plugin.worldessentials;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityLevelChangeEvent;
import cn.nukkit.event.player.PlayerGameModeChangeEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.Level;

public class EventsListener implements Listener {

    private final WorldEssentials plugin;

    public EventsListener(WorldEssentials plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityLevelChange(EntityLevelChangeEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Player)) return;

        Player player = (Player) entity;
        Level origin = event.getOrigin();
        Level target = event.getTarget();

        if (origin.equals(target)) return;

        int targetGMode = plugin.data.getLevelGamemode(target);

        plugin.data.trySavePlayerData(player);
        player.setGamemode(targetGMode);
        player.getInventory().clearAll();
        player.getUIInventory().clearAll();
        player.getEnderChestInventory().clearAll();
        player.removeAllEffects();

        if (targetGMode == 0 || targetGMode == 2) {
            player.getInventory().setContents(plugin.data.getInvContentsAndSetEC(player, target));
            plugin.data.setData(player, target);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();

        if (event.getNewGamemode() == player.getGamemode()) return;
        if (player.getGamemode() == 0 && event.getNewGamemode() == 2) return;
        if (player.getGamemode() == 2 && event.getNewGamemode() == 0) return;

        plugin.data.trySavePlayerData(player);
        player.getInventory().clearAll();
        player.getUIInventory().clearAll();
        player.getEnderChestInventory().clearAll();
        player.removeAllEffects();

        if (event.getNewGamemode() == 0 || event.getNewGamemode() == 2) {
            player.getInventory().setContents(plugin.data.getInvContentsAndSetEC(player, player.getLevel()));
            plugin.data.setData(player, player.getLevel());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player.getInventory() != null) {
            plugin.data.trySavePlayerData(player);

            player.getInventory().clearAll();
            player.getUIInventory().clearAll();
            player.getEnderChestInventory().clearAll();
            player.removeAllEffects();
        }

        plugin.data.closeInventoryCache(player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getInventory().clearAll();
        player.getUIInventory().clearAll();
        player.getEnderChestInventory().clearAll();
        player.removeAllEffects();

        int targetGMode = plugin.data.getLevelGamemode(player.getLevel());

        if (targetGMode == 0 || targetGMode == 2) {
            player.getInventory().setContents(plugin.data.getInvContentsAndSetEC(player, player.getLevel()));
            plugin.data.setData(player, player.getLevel());
        }

        player.setGamemode(targetGMode);
    }
}
