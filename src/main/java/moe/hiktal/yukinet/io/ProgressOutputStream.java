package moe.hiktal.yukinet.io;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutputStream extends OutputStream {
    private final OutputStream outputStream;
    private final long totalSize;
    private final String name;
    private long bytesWritten = 0;

    public ProgressOutputStream(OutputStream outputStream, long totalSize, String name) {
        this.outputStream = outputStream;
        this.totalSize = totalSize;
        this.name = name;
    }

    @Override
    public void write(int b) throws IOException {
        outputStream.write(b);
        bytesWritten++;
        printProgress();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        outputStream.write(b, off, len);
        bytesWritten += len;
        printProgress();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    @Override
    public void flush() throws IOException {
        outputStream.flush();
    }

    private void printProgress() {
        double progress = (double) bytesWritten / totalSize * 100;
        System.out.print("\r%s: %d / %d bytes [%.2f%%]".formatted(name, bytesWritten, totalSize, progress) + " ".repeat(15));
    }
}
