package moe.hiktal.yukinet.io.impl;

import moe.hiktal.yukinet.io.FileProvider;
import moe.hiktal.yukinet.io.IFile;
import moe.hiktal.yukinet.io.ProgressOutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.hamcrest.core.Is;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class RemoteFile implements IFile {
    private final FileProvider provider;
    private final String abstractPath;

    public RemoteFile(FileProvider provider, String abstractPath) {
        this.provider = provider;
        this.abstractPath = abstractPath;
    }

    public FTPFile GetFile() {
        try {
            return provider.getFtpClient().mlistFile(abstractPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String GetName() {
        return GetFile().getName();
    }

    @Override
    public boolean IsDirectory() {
        return GetFile().isDirectory();
    }

    @Override
    public List<IFile> ListFiles() throws IOException {
        if (!IsDirectory()) return null;
        FTPFile[] files = provider.getFtpClient().listFiles(abstractPath);
        return Arrays.stream(files)
                .map(c -> new RemoteFile(provider, abstractPath + File.separator + c.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public byte[] GetContent() throws IOException {
        if (IsDirectory()) return null;

        FTPFile file = GetFile();
        try (ByteArrayOutputStream fos = new ByteArrayOutputStream();
             ProgressOutputStream pos = new ProgressOutputStream(fos, file.getSize(), file.getName())) {
            provider.getFtpClient().retrieveFile(abstractPath, pos);
            System.out.println("\r%s, %d bytes, done".formatted(file.getName(), file.getSize()) + " ".repeat(15));
            return fos.toByteArray();
        }
    }

    @Override
    public long GetSize() {
        return GetFile().getSize();
    }
}
