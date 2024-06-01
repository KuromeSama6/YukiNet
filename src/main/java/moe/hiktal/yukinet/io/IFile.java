package moe.hiktal.yukinet.io;

import java.io.IOException;
import java.util.List;

/**
 * Represents a file that may be present on either the
 * local instance, or a remote server.
 */
public interface IFile {
    String GetName();
    boolean IsDirectory();
    List<IFile> ListFiles() throws IOException;
    byte[] GetContent() throws IOException;
    long GetSize();
}
