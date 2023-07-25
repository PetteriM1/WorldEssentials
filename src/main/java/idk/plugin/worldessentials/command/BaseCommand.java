package idk.plugin.worldessentials.command;

import cn.nukkit.command.Command;
import cn.nukkit.command.PluginIdentifiableCommand;
import idk.plugin.worldessentials.WorldEssentials;

public abstract class BaseCommand extends Command implements PluginIdentifiableCommand {

    protected WorldEssentials plugin;

    public BaseCommand(String name, WorldEssentials plugin) {
        super(name);
        this.plugin = plugin;
    }

    @Override
    public WorldEssentials getPlugin() {
        return plugin;
    }
}
