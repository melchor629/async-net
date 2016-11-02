package me.melchor9000.net;

import io.netty.buffer.ByteBuf;

/**
 * When deserializing an object, if the data inside a {@code ByteBuf}
 * doesn't match to the object serialization, this exception is thrown.
 * Will contain the original {@code ByteBuf} data.
 */
public class DataNotRepresentsObject extends IllegalArgumentException {
    private ByteBuf data;

    public DataNotRepresentsObject(String message, ByteBuf data) {
        super(message);
        this.data = data;
    }

    public ByteBuf data() {
        return data;
    }
}
