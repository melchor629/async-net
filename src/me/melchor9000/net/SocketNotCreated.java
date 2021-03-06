package me.melchor9000.net;

import org.jetbrains.annotations.NotNull;

/**
 * When tries to call a method that requires the {@link Socket}
 * to be created. A {@link Socket} is created when a {@code bind}
 * or {@code connect} call is made.
 */
public class SocketNotCreated extends IllegalStateException {
    private Socket socket;

    SocketNotCreated(String s, Socket socket) {
        super(s);
        this.socket = socket;
    }

    public @NotNull Socket getSocket() {
        return socket;
    }
}
