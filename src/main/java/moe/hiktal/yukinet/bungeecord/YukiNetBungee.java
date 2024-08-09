package moe.hiktal.yukinet.bungeecord;

import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.bungeecord.command.AddServerCommand;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.logging.Logger;

public class YukiNetBungee extends Plugin {
    @Getter
    private static YukiNetBungee instance;
    @Getter
    private static Logger log;

    @Override
    public void onEnable() {
        instance = this;
        log = getLogger();


        getProxy().getPluginManager().registerCommand(this, new AddServerCommand());
    }
}
