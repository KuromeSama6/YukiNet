package moe.hiktal.yukinet.io;

import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.server.ServerManager;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileDownloader {

    @Getter
    private final FTPClient ftpClient = new FTPClient();
    private final ServerManager serverManager;
    private final File resources;

    public FileDownloader(ServerManager serverManager) throws IOException {
        this.serverManager = serverManager;

        resources = new File(YukiNet.CWD + "/download-cache");
        if (!resources.exists()) resources.mkdirs();

        ftpClient.setListHiddenFiles(true);

        ftpClient.connect(YukiNet.getMasterIp(), 8633);
        ftpClient.login("anonymous", "");
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
    }

    public void Download() throws IOException {
        DownloadDirectory("/");
    }

    private void DownloadDirectory(String dir) throws IOException{
        FTPFile[] files = ftpClient.listFiles(dir);

        if (files != null) {
            for (FTPFile file : files) {
                if (file.isDirectory()) {
                    DownloadDirectory(dir + "/" + file.getName());
                } else {
                    // download the file
                    long remoteSize = file.getSize();
                    File localFile = new File(YukiNet.CWD + "/download-cache/template" + dir + "/" + file.getName());

                    if (localFile.exists() && remoteSize == localFile.length()) {
                        String line = "\rFile skipped: %s".formatted(file.getName());
                        System.out.print(line + " ".repeat(25));
                    } else {
                        DownloadFileSync(dir, file, localFile);
                    }
                }
            }
        }

    }

    private void DownloadFileSync(String dir, FTPFile file, File localFile) throws IOException {
        if (!localFile.getParentFile().exists())
            localFile.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(localFile);
            ProgressOutputStream pos = new ProgressOutputStream(fos, file.getSize(), file.getName())) {
            ftpClient.retrieveFile(dir + "/" + file.getName(), pos);
            System.out.println("\r%s, %d bytes, done".formatted(file.getName(), file.getSize()) + " ".repeat(15));
        }
    }

}
