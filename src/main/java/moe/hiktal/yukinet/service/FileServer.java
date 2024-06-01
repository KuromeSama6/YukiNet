package moe.hiktal.yukinet.service;

import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.SaltedPasswordEncryptor;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.util.ArrayList;
import java.util.List;

public class FileServer {
    @Getter
    private static FileServer instance;
    private FtpServer server;

    public FileServer() {
        instance = this;

        FtpServerFactory factory = new FtpServerFactory();
        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(8633);
        factory.addListener("default", listenerFactory.createListener());
        ConnectionConfigFactory connectionConfigFactory = new ConnectionConfigFactory();
        connectionConfigFactory.setAnonymousLoginEnabled(true);
        factory.setConnectionConfig(connectionConfigFactory.createConnectionConfig());

        BaseUser user = new BaseUser();
        user.setName("anonymous");
        user.setHomeDirectory(YukiNet.CWD + "/template");

        try {
            factory.getUserManager().save(user);
            // Create and start the server
            server = factory.createServer();
            server.start();

            YukiNet.getLogger().info("FTP server started on port %d".formatted(listenerFactory.getPort()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
