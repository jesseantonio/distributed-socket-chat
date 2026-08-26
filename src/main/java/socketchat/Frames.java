package socketchat;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Length-prefixed message framing: a 4-byte big-endian size header followed by the payload. */
public final class Frames {

    public static final int MAX_FRAME_SIZE = 64 * 1024;

    private Frames() {
    }

    public static void write(OutputStream out, byte[] payload) throws IOException {
        if (payload.length > MAX_FRAME_SIZE) {
            throw new IllegalArgumentException("Payload of " + payload.length + " bytes exceeds the limit.");
        }

        byte[] header = new byte[4];
        writeInt32BigEndian(header, payload.length);

        out.write(header);
        out.write(payload);
        out.flush();
    }

    /** Returns null when the peer disconnects cleanly at a frame boundary. */
    public static byte[] read(InputStream in) throws IOException {
        byte[] header = new byte[4];
        if (!readExactly(in, header)) {
            return null;
        }

        int size = readInt32BigEndian(header);
        if (size < 0 || size > MAX_FRAME_SIZE) {
            throw new IOException("Invalid frame size: " + size);
        }

        if (size == 0) {
            return new byte[0];
        }

        byte[] payload = new byte[size];
        if (!readExactly(in, payload)) {
            throw new EOFException("Connection closed in the middle of a frame.");
        }

        return payload;
    }

    private static boolean readExactly(InputStream in, byte[] destination) throws IOException {
        int read = 0;
        while (read < destination.length) {
            int n = in.read(destination, read, destination.length - read);
            if (n < 0) {
                if (read == 0) {
                    return false;
                }
                throw new EOFException("Missing " + (destination.length - read) + " bytes of the frame.");
            }
            read += n;
        }
        return true;
    }

    private static void writeInt32BigEndian(byte[] dest, int value) {
        dest[0] = (byte) (value >>> 24);
        dest[1] = (byte) (value >>> 16);
        dest[2] = (byte) (value >>> 8);
        dest[3] = (byte) value;
    }

    private static int readInt32BigEndian(byte[] src) {
        return ((src[0] & 0xFF) << 24) | ((src[1] & 0xFF) << 16) | ((src[2] & 0xFF) << 8) | (src[3] & 0xFF);
    }
}
