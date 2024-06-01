package moe.hiktal.yukinet.io;

import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.io.impl.LocalFile;
import moe.hiktal.yukinet.io.impl.RemoteFile;
import moe.hiktal.yukinet.server.ServerManager;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * The FileProvider class provides abstraction in accessing files
 * that may be present on this instance of YukiNet, or may be present
 * on the file system of a remote server.
 */
public class FileProvider {
    @Getter
    private final FTPClient ftpClient = new FTPClient();
    private final ServerManager serverManager;
    @Getter
    private final static File resourcesDirectory = new File(YukiNet.CWD + "/download-cache");;

    public FileProvider(ServerManager serverManager) throws IOException {
        this.serverManager = serverManager;
        if (!resourcesDirectory.exists()) resourcesDirectory.mkdirs();

        ftpClient.setListHiddenFiles(true);
        ftpClient.connect(YukiNet.getMasterIp(), 8633);
        ftpClient.login("anonymous", "");
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    public IFile Acquire(String path) {
        File cached = new File(resourcesDirectory + path);
        File localFile;
        if (cached.exists()) localFile = cached;
        else localFile = new File(YukiNet.CWD + path);

        if (localFile.exists()) return new LocalFile(this, localFile);
        else return new RemoteFile(this, path);
    }

}
