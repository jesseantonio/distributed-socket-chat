package socketchat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registro dos pares atualmente conectados. Acessado por várias threads ao mesmo tempo. */
public class PeerTable {

    private final Map<String, PeerConnection> peers = new LinkedHashMap<>();

    public synchronized void add(PeerConnection peer) {
        peers.put(peer.nickname, peer);
    }

    public synchronized void remove(String nickname) {
        peers.remove(nickname);
    }

    public synchronized Collection<PeerConnection> all() {
        return new ArrayList<>(peers.values());
    }

    public synchronized PeerConnection find(String nickname) {
        return peers.get(nickname);
    }
}
