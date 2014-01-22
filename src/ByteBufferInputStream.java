import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class ByteBufferInputStream extends InputStream {
    ByteBuffer buf;

    public ByteBufferInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    public int read() throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }
        return buf.get() & 0xFF;
    }

    public int readInt() throws IOException {
        if (!buf.hasRemaining() || buf.remaining() < 4) {
            return -1;
        }
        return buf.getInt();
    }

    public int read(byte[] bytes, int off, int len)
            throws IOException {
        if (!buf.hasRemaining()) {
            return -1;
        }

        len = Math.min(len, buf.remaining());
        buf.get(bytes, off, len);
        return len;
    }
}
