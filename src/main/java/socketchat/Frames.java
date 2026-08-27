package socketchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;

/** Messages on the wire are a 4-byte length prefix followed by that many bytes of payload. */
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

    /**
     * Returns null quando o par fechou a conexão de forma limpa. Lança SocketTimeoutException
     * se o prazo de leitura esgotar antes de uma mensagem nova começar a chegar — nesse caso
     * não perdemos nenhum byte, então é seguro o chamador tentar ler de novo mais tarde. Se o
     * prazo esgotar no meio de uma mensagem já iniciada, isso vira uma IOException comum, porque
     * aí sim já não dá mais pra confiar que os próximos bytes vão ser um novo prefixo de tamanho.
     */
    public static byte[] read(DataInputStream in) throws IOException {
        int size;
        try {
            size = in.readInt();
        } catch (EOFException e) {
            return null;
        }

        if (size < 0 || size > MAX_MESSAGE_SIZE) {
            throw new IOException("Invalid message size: " + size);
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
