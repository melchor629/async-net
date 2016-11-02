package me.melchor9000.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Objects that can be serialized to and from a {@link ByteBuf},
 * to send and receive objects of this type
 */
public abstract class Serializable {

    /**
     * @return length in bytes of the serialized data
     */
    public abstract int byteBufSize();

    /**
     * Writes the data of this object into the {@link ByteBuf}.
     * @param buffer buffer to store the data
     */
    public abstract void toByteBuf(ByteBuf buffer);

    /**
     * Creates a {@link ByteBuf} and stores inside it the data
     * of this object.
     * @return a {@link ByteBuf} with the object serialized
     */
    public ByteBuf toByteBuf() {
        ByteBuf buf = Unpooled.buffer(byteBufSize());
        toByteBuf(buf);
        return buf;
    }

    /**
     * Fills this object with the data from the {@link ByteBuf}.
     * If the buffer contents doesn't match the object specification,
     * should throw {@link IllegalArgumentException}.
     * @param buffer buffer with the data
     * @throws DataNotRepresentsObject If the buffer data doesn't
     * represent the object
     */
    public abstract void fromByteBuf(ByteBuf buffer) throws DataNotRepresentsObject;
}
