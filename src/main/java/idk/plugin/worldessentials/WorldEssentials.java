package idk.plugin.worldessentials;

import cn.nukkit.plugin.PluginBase;
import idk.plugin.worldessentials.command.defaults.SetWorldGamemodeCommand;

public class WorldEssentials extends PluginBase {

    public Data data;

    public void onEnable() {
        data = new Data(this);

        getServer().getCommandMap().register("setworldgamemode", new SetWorldGamemodeCommand(this));
        getServer().getPluginManager().registerEvents(new EventsListener(this), this);
    }
}
