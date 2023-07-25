package idk.plugin.worldessentials.command.defaults;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.level.Level;
import cn.nukkit.utils.TextFormat;
import idk.plugin.worldessentials.WorldEssentials;
import idk.plugin.worldessentials.command.BaseCommand;

public class SetWorldGamemodeCommand extends BaseCommand {

    public SetWorldGamemodeCommand(WorldEssentials plugin) {
        super("setworldgamemode", plugin);
        this.description = "Set world's default gamemode";
        this.usageMessage = "/setworldgamemode <mode> [world]";
        this.setPermission("worldessentials.command.setworldgamemode");
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.usageMessage));
            return false;
        }
        Level level;
        if (args.length > 1) {
            level = plugin.getServer().getLevelByName(args[1]);
            if (level == null) {
                sender.sendMessage(TextFormat.RED + "Can't find world " + args[1]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(TextFormat.RED + "You can only perform this command as a player");
                return true;
            }
            level = ((Player) sender).getLevel();
        }
        int gamemode = Server.getGamemodeFromString(args[0]);
        if (gamemode == -1) {
            sender.sendMessage(TextFormat.RED + "Unknown gamemode " + args[0]);
            return true;
        }
        plugin.data.setLevelGamemode(level, gamemode);
        for (Player levelPlayer : level.getPlayers().values()) {
            levelPlayer.sendMessage("This world's gamemode has been set to " + Server.getGamemodeString(gamemode));
        }
        sender.sendMessage("World " + level.getName() + "'s gamemode has been set to " + Server.getGamemodeString(gamemode));
        return true;
    }
}
