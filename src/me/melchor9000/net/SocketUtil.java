/*
    async-net: A basic asynchronous network library, based on netty
    Copyright (C) 2016  melchor629 (melchor9000@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package me.melchor9000.net;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

/**
 * Some utilities when reading and writing stream based sockets
 */
public class SocketUtil {

    /**
     * Reads exactly {@code bytes} bytes of data from the socket synchronously.
     * @param socket Socket to read from
     * @param buffer Buffer to write the data
     * @param bytes Number of bytes to read
     * @throws Throwable If the operation fails
     */
    public static void read(@NotNull Socket socket, @NotNull ByteBuf buffer, int bytes) throws Throwable {
        while(bytes != 0) {
            bytes -= socket.receive(buffer, bytes);
        }
    }

    /**
     * Reads from the socket exactly the number of writable bytes
     * {@link ByteBuf#writableBytes()} of the buffer into the buffer synchronously.
     * @param socket Socket to read from
     * @param buffer Buffer to write the data
     * @throws Throwable If the operation fails
     */
    public static void read(@NotNull Socket socket, @NotNull ByteBuf buffer) throws Throwable {
        read(socket, buffer, buffer.writableBytes());
    }

    /**
     * Reads exactly {@code bytes} bytes of data from the socket asynchronously
     * and returns a {@link Future} with the result of the task. If there's an error,
     * everything that could read is kept inside the buffer.
     * @param socket Socket to read from
     * @param buffer Buffer to write the data
     * @param bytes Number of bytes to read
     * @return a {@link Future}
     */
    public static @NotNull Future<Long> readAsync(@NotNull final Socket socket, @NotNull final ByteBuf buffer, final int bytes) {
        final long read[] = new long[] { (long) bytes };
        final FutureImpl<Long> future = new FutureImpl<>(socket.service, null);
        final Callback<Future<Long>> c = new Callback<Future<Long>>() {
            @Override
            public void call(Future<Long> arg) {
                if(arg.getValueNow() >= 0) {
                    read[0] -= (int) (long) arg.getValueNow();
                    if(read[0] != 0) {
                        socket.receiveAsync(buffer, (int) read[0]).whenDone(this);
                    } else {
                        future.postSuccess(read[0]);
                    }
                } else {
                    future.postSuccess(read[0]);
                }
            }
        };

        socket.receiveAsync(buffer, bytes).whenDone(c);
        return future;
    }

    /**
     * Reads from the socket exactly the number of writable bytes
     * {@link ByteBuf#writableBytes()} of the buffer into the buffer asynchronously.
     * If there's an error, everything that could read is kept inside the buffer.
     * @param socket Socket to read from
     * @param buffer Buffer to write the data
     * @return a {@link Future}
     */
    public static Future<Long> readAsync(@NotNull Socket socket, @NotNull ByteBuf buffer) {
        return readAsync(socket, buffer, buffer.writableBytes());
    }
}
