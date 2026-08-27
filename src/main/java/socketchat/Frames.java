package socketchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;

public class Frames {

    public static final int MAX_MESSAGE_SIZE = 64 * 1024;

    public static void write(DataOutputStream out, byte[] payload) throws IOException {
        if (payload.length > MAX_MESSAGE_SIZE) {
            throw new IOException("Mensagem de " + payload.length + " bytes excede o limite de " + MAX_MESSAGE_SIZE + " bytes.");
        }

        out.writeInt(payload.length);
        out.write(payload);
        out.flush();
    }

    public static byte[] read(DataInputStream in) throws IOException {
        int size;
        try {
            size = in.readInt();
        } catch (EOFException e) {
            return null;
        }

        if (size < 0 || size > MAX_MESSAGE_SIZE) {
            throw new IOException("Tamanho invalido de mensagem: " + size);
        }

        byte[] payload = new byte[size];
        try {
            in.readFully(payload);
        } catch (SocketTimeoutException e) {
            throw new IOException("Tempo esgotado no meio de uma mensagem.", e);
        }
        return payload;
    }
}
