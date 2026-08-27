package socketchat;

public class PeerAddress {

    public final String host;
    public final int port;

    public PeerAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public static PeerAddress parse(String text) {
        int i = text.lastIndexOf(':');
        if (i <= 0 || i == text.length() - 1) {
            throw new IllegalArgumentException("Endereço de par inválido: '" + text + "'. Formato esperado: host:porta.");
        }

        String host = text.substring(0, i);
        int port;
        try {
            port = Integer.parseInt(text.substring(i + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Endereço de par inválido: '" + text + "'. Formato esperado: host:porta.");
        }
        return new PeerAddress(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
