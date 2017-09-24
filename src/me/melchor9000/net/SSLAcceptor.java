/*
    async-net: A basic asynchronous network library, based on netty
    Copyright (C) 2017  melchor629 (melchor9000@gmail.com)

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

import io.netty.channel.socket.SocketChannel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 *     Accepts SSL connections, and creates {@link SSLSocket} for any new one,
 *     using a public and private keys.
 * </p>
 * <p>
 *     SSL servers uses a public and private keys (with a public certificate and
 *     a private key both in {@code .pem} format) to identify itself and cipher
 *     all data during a connection. You can provide both certificates using
 *     {@link File} objects or {@link InputStream}{@code s}.
 * </p>
 * <p>
 *     A {@link SSLAcceptor} is just a layer over a {@link TCPAcceptor}, the full
 *     documentation will be found in {@link TCPAcceptor} and {@link Acceptor}.
 *     All sockets will be of type {@link SSLSocket}.
 * </p>
 */
public class SSLAcceptor extends TCPAcceptor {
    private File publicKeyFile;
    private File privateKeyFile;
    private InputStream publicKeyInputStream;
    private InputStream privateKeyInputStream;
    private String passwd;

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private must not have a passphrase.
     * @param service {@link IOService} for the acceptor and the sockets
     * @param publicKey a {@link File} to the public certificate in {@code .pem} format
     * @param privateKey a {@link File} to the public certificate in {@code .pem} format
     */
    public SSLAcceptor(IOService service, File publicKey, File privateKey) {
        this(service, publicKey, privateKey, null);
    }

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private must not have a passphrase.
     * @param service {@link IOService} for the acceptor and the sockets
     * @param publicKey a {@link InputStream} to the public certificate in {@code .pem} format
     * @param privateKey a {@link InputStream} to the public certificate in {@code .pem} format
     */
    public SSLAcceptor(IOService service, InputStream publicKey, InputStream privateKey) {
        this(service, publicKey, privateKey, null);
    }

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private one has a passphrase.
     * @param service {@link IOService} for the acceptor and the sockets
     * @param publicKey a {@link File} to the public certificate in {@code .pem} format
     * @param privateKey a {@link File} to the public certificate in {@code .pem} format
     * @param password the private key passphrase
     */
    public SSLAcceptor(IOService service, File publicKey, File privateKey, String password) {
        super(service);
        if(publicKey == null || !publicKey.isFile()) throw new IllegalArgumentException("publicKey cannot be null and must exist");
        if(privateKey == null || !privateKey.isFile()) throw new IllegalArgumentException("privateKey cannot be null and must exist");
        this.publicKeyFile = publicKey;
        this.privateKeyFile = privateKey;
        this.passwd = password;
    }

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private one has a passphrase.
     * @param service {@link IOService} for the acceptor and the sockets
     * @param publicKey a {@link InputStream} to the public certificate in {@code .pem} format
     * @param privateKey a {@link InputStream} to the public certificate in {@code .pem} format
     * @param password the private key passphrase
     */
    public SSLAcceptor(IOService service, InputStream publicKey, InputStream privateKey, String password) {
        super(service);
        this.publicKeyInputStream = publicKey;
        this.privateKeyInputStream = privateKey;
        this.passwd = password;
    }

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private must not have a passphrase.
     * @param service {@link IOService} for the acceptor
     * @param worker {@link IOService} for the sockets
     * @param publicKey a {@link File} to the public certificate in {@code .pem} format
     * @param privateKey a {@link File} to the public certificate in {@code .pem} format
     */
    public SSLAcceptor(IOService service, IOService worker, File publicKey, File privateKey) {
        this(service, worker, publicKey, privateKey, null);
    }

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private must not have a passphrase.
     * @param service {@link IOService} for the acceptor
     * @param worker {@link IOService} for the sockets
     * @param publicKey a {@link InputStream} to the public certificate in {@code .pem} format
     * @param privateKey a {@link InputStream} to the public certificate in {@code .pem} format
     */
    public SSLAcceptor(IOService service, IOService worker, InputStream publicKey, InputStream privateKey) {
        this(service, worker, publicKey, privateKey, null);
    }

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private one has a passphrase.
     * @param service {@link IOService} for the acceptor
     * @param worker {@link IOService} for the sockets
     * @param publicKey a {@link File} to the public certificate in {@code .pem} format
     * @param privateKey a {@link File} to the public certificate in {@code .pem} format
     * @param password the private key passphrase
     */
    public SSLAcceptor(IOService service, IOService worker, File publicKey, File privateKey, String password) {
        super(service, worker);
        if(publicKey == null || !publicKey.isFile()) throw new IllegalArgumentException("publicKey cannot be null and must exist");
        if(privateKey == null || !privateKey.isFile()) throw new IllegalArgumentException("privateKey cannot be null and must exist");
        this.publicKeyFile = publicKey;
        this.privateKeyFile = privateKey;
        this.passwd = password;
    }

    /**
     * Creates a SSL acceptor for server applications. Uses a public and private certificates,
     * and the private one has a passphrase.
     * @param service {@link IOService} for the acceptor
     * @param worker {@link IOService} for the sockets
     * @param publicKey a {@link InputStream} to the public certificate in {@code .pem} format
     * @param privateKey a {@link InputStream} to the public certificate in {@code .pem} format
     * @param password the private key passphrase
     */
    public SSLAcceptor(IOService service, IOService worker, InputStream publicKey, InputStream privateKey, String password) {
        super(service, worker);
        this.publicKeyInputStream = publicKey;
        this.privateKeyInputStream = privateKey;
        this.passwd = password;
    }

    @Override
    protected TCPSocket createSocketForImplementation(SocketChannel ch) throws IOException {
        if(publicKeyFile != null) {
            return new SSLSocket(this, ch, this.publicKeyFile, this.privateKeyFile, this.passwd);
        } else if(publicKeyInputStream != null) {
            return new SSLSocket(this, ch, this.publicKeyInputStream, this.privateKeyInputStream, this.passwd);
        } else {
            throw new IllegalStateException("Unreachable code, reached :(");
        }
    }

}
