# distributed-socket-chat

Two-peer chat over a raw TCP socket. No relay server: one instance listens, the other connects.

Java port of the original SocketChat C# project, preserving the same protocol
(length-prefixed frames) and behavior.

## Build

    mvn package

## Run

Terminal 1:

    mvn exec:java -Dexec.args="listen 9000 alice"

Terminal 2:

    mvn exec:java -Dexec.args="connect 127.0.0.1 9000 bob"

Or, after `mvn package`, run the jar directly:

    java -jar target/socket-chat.jar listen 9000 alice
    java -jar target/socket-chat.jar connect 127.0.0.1 9000 bob

Type a message and press Enter. Type `/quit` to leave.

## Files

| File | Role |
|---|---|
| `Main.java` | Argument parsing and mode selection |
| `Connection.java` | Listen / connect |
| `ChatSession.java` | Full-duplex send and receive loops |
| `Frames.java` | Length-prefixed message framing |
| `Cancellation.java` | Ctrl+C support for blocking socket calls |

## Notes

Requires Java 17 or later and Maven.
