package idk.plugin.worldessentials;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Level;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Data {

    private final WorldEssentials plugin;

    private final File worldsFolder;
    private final Config itemNames;

    private final Map<Level, Config> levelConfigs = new HashMap<>();
    private final Map<Player, Map<Level, Config>> inventoryCache = new HashMap<>();

    private final String survivalWorld;

    public Data(WorldEssentials plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        plugin.getDataFolder().mkdirs();
        worldsFolder = new File(plugin.getDataFolder(), "worlds");
        worldsFolder.mkdirs();
        itemNames = new Config(plugin.getDataFolder() + "/itemnames.yml", Config.YAML);
        survivalWorld = plugin.getConfig().getString("survivalWorld", "world");
    }

    public void setLevelGamemode(Level level, int gamemode) {
        Config levelConfig = getLevelConfig(level);
        levelConfig.set("gamemode", gamemode);
        levelConfig.save(true);

        for (Player levelPlayer : level.getPlayers().values()) {
            levelPlayer.setGamemode(gamemode);
        }
    }

    LinkedHashMap<Integer, Item> getInvContentsAndSetEC(Player player, Level level) {
        Config plc = getPlayerConfig(player, level);
        String lvlname = level.getName();
        
        if (lvlname.equals("nether") || lvlname.equals("the_end")) {
            lvlname = survivalWorld;
        }

        boolean needSaveItemNames = false;

        @SuppressWarnings("unchecked")
        LinkedHashMap<Integer, ArrayList<Integer>> enderchest = (LinkedHashMap<Integer, ArrayList<Integer>>) plc.get("inventories.enderchest");
        if (enderchest != null) {
            LinkedHashMap<Integer, Item> echestItems = new LinkedHashMap<>();
            for (Map.Entry<Integer, ArrayList<Integer>> entry : enderchest.entrySet()) {
                int key = Integer.parseInt(String.valueOf(entry.getKey()));
                ArrayList<Integer> item = entry.getValue();
                Item i = Item.get(item.get(0), item.get(1), item.get(2));

                if (!itemNames.getString(player.getName() + "." + lvlname + ".enderchest" + "." + key).isEmpty()) {
                    i.setCustomName(itemNames.getString(player.getName() + "." + lvlname + ".enderchest" + "." + key));
                }

                int edata = 4;
                if (item.get(3) != 0) {
                    int co = 0;
                    while (co < item.get(3)) {
                        Enchantment e = Enchantment.get(item.get(edata));
                        e.setLevel(item.get(edata + 1));
                        i.addEnchantment(e);
                        edata = edata + 2;
                        co++;
                    }
                }

                echestItems.put(key, i);
            }

            Map map = ((Map) itemNames.get(player.getName() + "." + lvlname));
            if (map != null) {
                map.remove("enderchest");
            }
            needSaveItemNames = true;

            player.getEnderChestInventory().setContents(echestItems);
        }

        @SuppressWarnings("unchecked")
        LinkedHashMap<Integer, ArrayList<Integer>> inventory = (LinkedHashMap<Integer, ArrayList<Integer>>) plc.get("inventories.survival");
        if (inventory == null) {
            if (needSaveItemNames) {
                itemNames.save();
            }

            return new LinkedHashMap<>();
        }
        LinkedHashMap<Integer, Item> contents = new LinkedHashMap<>();
        boolean oldConfig = plc.getInt("ver", 2) == 2;

        for (Map.Entry<Integer, ArrayList<Integer>> entry : inventory.entrySet()) {
            int key = Integer.parseInt(String.valueOf(entry.getKey()));
            ArrayList<Integer> item = entry.getValue();
            Item i = Item.get(item.get(0), item.get(1), item.get(2));
            if (oldConfig) {
                try {
                    if (item.get(3) != null && item.get(3) != 0) {
                        Enchantment e = Enchantment.get(item.get(3));
                        e.setLevel(item.get(4));
                        i.addEnchantment(e);
                    }
                } catch (Exception ignored) {}
            } else {
                if (!itemNames.getString(player.getName() + "." + lvlname + "." + key).isEmpty()) {
                    i.setCustomName(itemNames.getString(player.getName() + "." + lvlname + "." + key));
                }

                int edata = 4;
                if (item.get(3) != 0) {
                    int co = 0;
                    while (co < item.get(3)) {
                        Enchantment e = Enchantment.get(item.get(edata));
                        e.setLevel(item.get(edata + 1));
                        i.addEnchantment(e);
                        edata = edata + 2;
                        co++;
                    }
                }
            }

            if (key == 999) {
                player.getUIInventory().setItem(0, i);
            } else if (key == 1000) {
                player.getOffhandInventory().setItem(0, i);
            } else {
                contents.put(key, i);
            }
        }

        Map map = ((Map) itemNames.get(player.getName()));
        if (map != null) {
            map.remove(lvlname);
        }

        itemNames.save();

        return contents;
    }

    int getLevelGamemode(Level level) {
        return getLevelConfig(level).getInt("gamemode",  plugin.getServer().getDefaultGamemode());
    }

    void trySavePlayerData(Player player) {
        if (player.getGamemode() != 0 && player.getGamemode() != 2) return;
        Config playerConfig = getPlayerConfig(player, player.getLevel());
        LinkedHashMap<String, Object> infos = new LinkedHashMap<>();
        if (player.getGamemode() == 0 || player.getGamemode() == 2) {
            LinkedHashMap<String, LinkedHashMap<Integer, ArrayList<Integer>>> playerInventorySave = getPlayerInventorySave(player);
            if (playerInventorySave != null) {
                infos.put("inventories", playerInventorySave);
            }

            infos.put("xp", player.getExperience());
            infos.put("xplevel", player.getExperienceLevel());

            if (player.getFoodData() != null) {
                if (player.getHealth() <= 0) {
                    infos.put("food", 20);
                } else {
                    infos.put("food", player.getFoodData().getLevel());
                }
            }

            if (player.getHealth() <= 0) {
                infos.put("health", 20);
            } else {
                infos.put("health", player.getHealth());
            }

            infos.put("timeSinceRest", player.getTimeSinceRest());

            infos.put("ver", 3);
        }

        playerConfig.setAll(infos);
        playerConfig.save(plugin.getServer().isRunning());
    }

    private LinkedHashMap<String, LinkedHashMap<Integer, ArrayList<Integer>>> getPlayerInventorySave(Player player) {
        if (player.getInventory() == null) return null;
        LinkedHashMap<Integer, ArrayList<Integer>> inventory = new LinkedHashMap<>();
        LinkedHashMap<Integer, ArrayList<Integer>> enderchest = new LinkedHashMap<>();
        boolean nameCacheEdited = false;

        String lvlname = player.getLevel().getName();

        if (lvlname.equals("nether") || lvlname.equals("the_end")) {
            lvlname = survivalWorld;
        }

        for (Map.Entry<Integer, Item> entry : player.getInventory().getContents().entrySet()) {
            Item item = entry.getValue();
            int ecount = item.getEnchantments().length;
            ArrayList<Integer> data = new ArrayList<>();
            data.add(0, item.getId());
            data.add(1, item.getDamage());
            data.add(2, item.getCount());
            data.add(3, ecount);

            int temp = 4;
            for (Enchantment enc : item.getEnchantments()) {
                data.add(temp, enc.getId());
                data.add(temp + 1, enc.getLevel());
                temp += 2;
            }

            inventory.put(entry.getKey(), data);

            if (item.hasCustomName() && item.getCustomName().length() < 100) {
                itemNames.set(player.getName() + "." + lvlname + "." + entry.getKey(), item.getCustomName());
                nameCacheEdited = true;
            }
        }

        Item citem = player.getUIInventory().getItem(0);
        if (citem != null && citem.getId() != 0) {
            int ecount = citem.getEnchantments().length;
            ArrayList<Integer> data = new ArrayList<>();
            data.add(0, citem.getId());
            data.add(1, citem.getDamage());
            data.add(2, citem.getCount());
            data.add(3, ecount);

            int temp = 4;
            for (Enchantment enc : citem.getEnchantments()) {
                data.add(temp, enc.getId());
                data.add(temp + 1, enc.getLevel());
                temp += 2;
            }

            inventory.put(999, data);

            if (citem.hasCustomName() && citem.getCustomName().length() < 100) {
                itemNames.set(player.getName() + "." + lvlname + ".999", citem.getCustomName());
                nameCacheEdited = true;
            }
        }

        Item oitem = player.getOffhandInventory().getItem(0);
        if (oitem != null && oitem.getId() != 0) {
            int ecount = oitem.getEnchantments().length;
            ArrayList<Integer> data = new ArrayList<>();
            data.add(0, oitem.getId());
            data.add(1, oitem.getDamage());
            data.add(2, oitem.getCount());
            data.add(3, ecount);

            int temp = 4;
            for (Enchantment enc : oitem.getEnchantments()) {
                data.add(temp, enc.getId());
                data.add(temp + 1, enc.getLevel());
                temp += 2;
            }

            inventory.put(1000, data);

            if (oitem.hasCustomName() && oitem.getCustomName().length() < 100) {
                itemNames.set(player.getName() + "." + lvlname + ".1000", oitem.getCustomName());
                nameCacheEdited = true;
            }
        }

        if (player.getEnderChestInventory() != null) {
            for (Map.Entry<Integer, Item> entry : player.getEnderChestInventory().getContents().entrySet()) {
                Item item = entry.getValue();
                int ecount = item.getEnchantments().length;
                ArrayList<Integer> data = new ArrayList<>();
                data.add(0, item.getId());
                data.add(1, item.getDamage());
                data.add(2, item.getCount());
                data.add(3, ecount);

                int temp = 4;
                for (Enchantment enc : item.getEnchantments()) {
                    data.add(temp, enc.getId());
                    data.add(temp + 1, enc.getLevel());
                    temp += 2;
                }

                enderchest.put(entry.getKey(), data);

                if (item.hasCustomName() && item.getCustomName().length() < 31) {
                    itemNames.set(player.getName() + "." + lvlname + ".enderchest" + "." + entry.getKey(), item.getCustomName());
                    nameCacheEdited = true;
                }
            }
        }

        if (nameCacheEdited) {
            itemNames.save();
        }

        LinkedHashMap<String, LinkedHashMap<Integer, ArrayList<Integer>>> inventories = getPlayerConfig(player, player.getLevel()).get("inventories", new LinkedHashMap<>());
        inventories.put("survival", inventory);
        inventories.put("enderchest", enderchest);
        return inventories;
    }

    private Config getLevelConfig(Level l) {
        Config c = levelConfigs.get(l);
        if (c == null) {
            c = new Config(new File(worldsFolder, l.getName() + ".yml"), Config.YAML);
            levelConfigs.put(l, c);
        }
        return c;
    }

    private Config getPlayerConfig(Player p, Level l) {
        String n = l.getName();
        if (n.equals("nether") || n.equals("the_end")) {
            Level lvl = plugin.getServer().getLevelByName(survivalWorld);
            Config c = loadFromInventoryCache(p, lvl);
            if (c == null) {
                c = new Config(new File(getLevelFolder(lvl), p.getName().toLowerCase() + ".yml"), Config.YAML);
                inventoryCache.get(p).put(lvl, c);
            }
            return c;
        } else {
            Config c = loadFromInventoryCache(p, l);
            if (c == null) {
                c = new Config(new File(getLevelFolder(l), p.getName().toLowerCase() + ".yml"), Config.YAML);
                inventoryCache.get(p).put(l, c);
            }
            return c;
        }
    }

    private File getLevelFolder(Level level) {
        File folder = new File(worldsFolder, level.getName());
        folder.mkdirs();
        return folder;
    }

    void setData(Player p, Level l) {
        Config c = getPlayerConfig(p, l);

        p.setExperience(c.getInt("xp"), c.getInt("xplevel"));

        int food = c.getInt("food", -1);
        if (food < 0 || food > 20) {
            p.getFoodData().setLevel(20);
        } else {
            p.getFoodData().setLevel(c.getInt("food"));
        }

        int health = c.getInt("health");
        if (health <= 0) {
            p.setHealth(20);
        } else {
            p.setHealth(health);
        }

        int tsr = c.getInt("timeSinceRest", -1);
        if (tsr > 0) {
            p.setTimeSinceRest(tsr);
        }
    }

    void closeInventoryCache(Player p) {
        Map<Level, Config> map = inventoryCache.get(p);
        if (map != null) {
            for (Config c : map.values()) {
                c.save(plugin.getServer().isRunning());
            }
            inventoryCache.remove(p);
        }
    }

    Config loadFromInventoryCache(Player p, Level l) {
        if (!inventoryCache.containsKey(p)) {
            inventoryCache.put(p, new HashMap<>());
            return null;
        } else if (!inventoryCache.get(p).containsKey(l)) {
            return null;
        }

        return inventoryCache.get(p).get(l);
    }
}
