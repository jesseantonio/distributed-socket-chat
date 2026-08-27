package socketchat;

import java.util.List;

public class NodeConfig {

    public final int port;
    public final String nickname;
    public final List<PeerAddress> knownPeers;

    public NodeConfig(int port, String nickname, List<PeerAddress> knownPeers) {
        this.port = port;
        this.nickname = nickname;
        this.knownPeers = knownPeers;
    }
}
