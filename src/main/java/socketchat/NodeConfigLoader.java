package socketchat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NodeConfigLoader {

    public static NodeConfig load(String[] args) throws IOException {
        String configFile = getValue(args, "--config");
        if (configFile != null) {
            return loadFromFile(configFile);
        }
        return loadFromArgs(args);
    }

    private static NodeConfig loadFromArgs(String[] args) {
        String portText = getValue(args, "--port");
        String nickname = getValue(args, "--nick");
        String peersText = getValue(args, "--peers");

        if (portText == null || nickname == null) {
            throw new IllegalArgumentException("Faltando --port ou --nick (ou use --config <arquivo>).");
        }

        return new NodeConfig(Integer.parseInt(portText), nickname, parsePeers(peersText));
    }

    private static NodeConfig loadFromFile(String path) throws IOException {
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(Path.of(path))) {
            props.load(reader);
        }

        String portText = props.getProperty("port");
        String nickname = props.getProperty("nick");
        if (portText == null || nickname == null) {
            throw new IllegalArgumentException("O arquivo de configuração precisa das chaves 'port' e 'nick'.");
        }

        return new NodeConfig(Integer.parseInt(portText), nickname, parsePeers(props.getProperty("peers")));
    }

    private static List<PeerAddress> parsePeers(String text) {
        List<PeerAddress> peers = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return peers;
        }

        for (String part : text.split(",")) {
            part = part.trim();
            if (!part.isEmpty()) {
                peers.add(PeerAddress.parse(part));
            }
        }
        return peers;
    }

    private static String getValue(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        return null;
    }
}
