package moe.hiktal.yukinet.bungeecord.command;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Command;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class AddServerCommand extends Command {
    public AddServerCommand() {
        super("addserver", null);
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!commandSender.equals(ProxyServer.getInstance().getConsole())) {
            commandSender.sendMessage(new TextComponent("EACCES"));
            return;
        }

        String name = strings[0];
        String ip = strings[1];
        int port = Integer.parseInt(strings[2]);

        InetSocketAddress address = new InetSocketAddress(ip, port);
        ServerInfo server = ProxyServer.getInstance().constructServerInfo(name, address, "", false);
        ProxyServer.getInstance().getServers().put(name, server);

        commandSender.sendMessage(new TextComponent("Add server: %s [%s]".formatted(name, address)));
    }
}
