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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

import java.util.concurrent.TimeUnit;

/**
 * Reprents a number of threads that executes the same Event Loop for I/O
 * operations and scheduled blocks of code. Used by {@link Socket} and
 * its implementations.
 */
public class IOService {
    final EventLoopGroup group;

    /**
     * Creates one thread that will run all associated IO events
     */
    public IOService() {
        this(1);
    }

    /**
     * Creates {@code numberOfThreads} threads that will run all
     * associated IO events
     * @param numberOfThreads number of threads
     */
    public IOService(int numberOfThreads) {
        group = new NioEventLoopGroup(numberOfThreads);
    }

    /**
     * Executes the block of code in the event's loop Thread
     * @param block block of code
     */
    public void post(final Procedure block) {
        group.submit(new Runnable() {
            @Override
            public void run() {
                block.call();
            }
        });
    }

    /**
     * Executes the block of code in the event's loop Thread, when
     * have passed some {@code milliseconds}.
     * @param block block of code
     * @param milliseconds milliseconds of delay to execute the code
     * @return a {@link Future} for the scheduled task
     */
    public Future<?> schedule(final Procedure block, long milliseconds) {
        return new NettyFuture<>(group.schedule(new Runnable() {
            @Override
            public void run() {
                block.call();
            }
        }, milliseconds, TimeUnit.MILLISECONDS), this);
    }

    /**
     * Stops the event loop
     */
    public void cancel() {
        if(!group.isShutdown()) {
            group.shutdownGracefully().syncUninterruptibly();
        }
    }

    /**
     * Stops the event loop asynchronously, useful when calling
     * this inside a {@link Callback}.
     * @return {@link Future} or null if is cancelled already
     */
    public Future<?> cancelAsync() {
        if(!group.isShutdown()) {
            return new NettyFuture<>(group.shutdownGracefully(), this);
        }
        return null;
    }
}
