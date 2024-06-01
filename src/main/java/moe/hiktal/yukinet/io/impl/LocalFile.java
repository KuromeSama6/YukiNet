package moe.hiktal.yukinet.io.impl;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.io.FileProvider;
import moe.hiktal.yukinet.io.IFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class LocalFile implements IFile {
    private final FileProvider provider;
    private final File file;

    @Override
    public String GetName() {
        return file.getName();
    }

    @Override
    public boolean IsDirectory() {
        return file.isDirectory();
    }

    @Override
    public List<IFile> ListFiles() {
        if (!IsDirectory()) return null;
        List<IFile> ret = new ArrayList<>();
        for (File child : file.listFiles()) {
            ret.add(new LocalFile(provider, child));
        }
        return ret;
    }

    @Override
    public byte[] GetContent() throws IOException {
        if (IsDirectory()) return null;
        return Files.readAllBytes(file.toPath());
    }

    @Override
    public long GetSize() {
        return file.length();
    }
}
