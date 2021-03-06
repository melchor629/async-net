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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import java.io.File;
import java.io.InputStream;

/**
 * <p>SSL Socket for connexions to servers, based on a {@link TCPSocket}.</p>
 * <p>
 *     To create a SSL socket, you can use the default constructor {@link #SSLSocket(IOService)}
 *     that uses the Java SSL implementation. You can also provide a custom public
 *     certificate chain that will be used to identify (the) server(s) (and will
 *     override the system certificate chain). For do that, you could call
 *     {@link #SSLSocket(IOService, File)} or {@link #SSLSocket(IOService, InputStream)}
 *     to accomplish it. But if your needs doesn't fit the previous constructors,
 *     you have a constructor that accepts a {@link SSLSocketConfigurator} to
 *     allow you to make custom configurations ({@link #SSLSocket(IOService, SSLSocketConfigurator)}).
 * </p>
 * <p>
 *     As the implementation of an SSL socket is always on top of a {@link TCPSocket},
 *     the behaviour is the same as the {@link TCPSocket}. One thing to note is that
 *     the SSL protocol could decide to send packets grouped or split a big packet,
 *     so take account on that.
 * </p>
 * <p>
 *     See {@link TCPSocket} and {@link Socket} for the whole API
 * </p>
 * <p>
 *     <b>Note for use in Android.</b> This library cannot handle correctly SSL Sockets
 *     on Android, but there's an easy workaround to it. First you need to use the fourth
 *     constructor ({@link #SSLSocket(IOService, SSLSocketConfigurator)}. In the implementation
 *     of the {@link SSLSocketConfigurator} you can use this code (it's written in Kotlin but
 *     you will catch it easily):
 * </p>
 *     <pre><code>
 *     //certFile is an File object. In Kotlin you can open an InputStream easily from a File.
 *     //You can try with new FileInputStream(certFile) in plain Java, for example.
 *     val cert = CertificateFactory.getInstance("X.509").generateCertificate(certFile.inputStream())
 *
 *     val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
 *     val ks = KeyStore.getInstance("AndroidKeyStore")
 *     ks.load(null)
 *     ks.setCertificateEntry("some-name", cert)
 *     kmf.init(ks, null)
 *
 *     val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
 *     val ts = KeyStore.getInstance("AndroidKeyStore")
 *     ts.load(null)
 *     ts.setCertificateEntry("some-name", cert)
 *     tmf.init(ts)
 *
 *     builder.keyManager(kmf)
 *     builder.trustManager(tmf)
 *     </code></pre>
 * <p>
 *     What this code does is create a pair of KeyStores using the default certificates from Android
 *     and adding to it your custom certificate. Also this method allows you to insert more public
 *     certificates, as long as you add them using {@code setCertificateEntry(String, Certificate)}
 * </p>
 */
public class SSLSocket extends TCPSocket {

    /**
     * <p>Creates a SSL socket using the Java implementation and the system's keychain
     * certificates.</p>
     * <p>In HTTPS, the host identification is not done. If you need this
     * security extra, you should use {@link #SSLSocket(IOService, SSLSocketConfigurator)}.</p>
     * @param service {@link IOService} to attach this socket
     */
    public SSLSocket(@NotNull IOService service) {
        super(service);
        bootstrap
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        SslContextBuilder ctx = SslContextBuilder.forClient();
                        SslContext ctx2 = ctx.build();
                        ch.pipeline().addLast("readManager", readManager);
                        ch.pipeline().addBefore("readManager","ssl", ctx2.newHandler(ch.alloc()));
                    }
                });
    }

    /**
     * <p>Creates a SSL socket using the Java implementation and the provided certificate
     * chain in {@code .pem} format.</p>
     * <p>In HTTPS, the host identification is not done. If you need this extra of security,
     * you should use {@link #SSLSocket(IOService, SSLSocketConfigurator)} and use the
     * option {@link SslContextBuilder#trustManager(File)}.</p>
     * @param service {@link IOService} to attach this socket
     * @param certificate Certificate chain in {@code .pem} format
     */
    public SSLSocket(@NotNull IOService service, @NotNull final File certificate) {
        super(service);
        bootstrap
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        SslContextBuilder ctx = SslContextBuilder.forClient();
                        ctx.trustManager(certificate.getAbsoluteFile());
                        ch.pipeline().addLast("readManager", readManager);
                        ch.pipeline().addBefore("readManager","ssl", ctx.build().newHandler(ch.alloc()));
                    }
                });
    }

    /**
     * <p>Creates a SSL socket using the Java implementation and the provided certificate
     * chain in {@code .pem} format.</p>
     * <p>In HTTPS, the host identification is not done. If you need this extra of security,
     * you should use {@link #SSLSocket(IOService, SSLSocketConfigurator)} and use the
     * option {@link SslContextBuilder#trustManager(InputStream)}.</p>
     * @param service {@link IOService} to attach this socket
     * @param certificate Certificate chain in {@code .pem} format
     */
    public SSLSocket(@NotNull IOService service, @NotNull final InputStream certificate) {
        super(service);
        bootstrap
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        SslContextBuilder ctx = SslContextBuilder.forClient();
                        ctx.trustManager(certificate);
                        ch.pipeline().addLast("readManager", readManager);
                        ch.pipeline().addBefore("readManager","ssl", ctx.build().newHandler(ch.alloc()));
                    }
                });
    }

    /**
     * <p>Creates a SSL socket using the custom options you set in the
     * {@link SSLSocketConfigurator#configure(SslContextBuilder)} method.
     * All methods available can be found in <a href="https://netty.io/4.1/api/io/netty/handler/ssl/SslContextBuilder.html">
     * SslContextBuilder</a>.</p>
     * <p>For enable host identification for HTTPS, you should override
     * {@link SSLSocketConfigurator#changeParameters(SSLParameters)} and set the option
     * {@link SSLParameters#setEndpointIdentificationAlgorithm(String)} to {@code "HTTPS"}</p>
     * @param service {@link IOService} to attach this socket
     * @param conf Custom configuration set in {@link SSLSocketConfigurator}
     */
    public SSLSocket(@NotNull IOService service, @NotNull final SSLSocketConfigurator conf) {
        super(service);
        bootstrap
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        SslContextBuilder ctx = SslContextBuilder.forClient();
                        conf.configure(ctx);
                        SslHandler handler = ctx.build().newHandler(ch.alloc());
                        SSLParameters p = handler.engine().getSSLParameters();
                        SSLParameters np = conf.changeParameters(p);
                        if(np != null) handler.engine().setSSLParameters(np);
                        ch.pipeline().addLast("readManager", readManager);
                        ch.pipeline().addBefore("readManager","ssl", handler);
                    }
                });
    }

    SSLSocket(SSLAcceptor acceptor, SocketChannel socket, File publicKey, File privateKey, String passwd) throws SSLException {
        super(acceptor, socket);
        SslContext ctx = (passwd != null ? SslContextBuilder.forServer(publicKey, privateKey, passwd) : SslContextBuilder.forServer(publicKey, privateKey)).build();
        socket.pipeline().addBefore("readManager", "ssl", ctx.newHandler(socket.alloc()));
    }

    SSLSocket(SSLAcceptor acceptor, SocketChannel socket, InputStream publicKey, InputStream privateKey, String passwd) throws SSLException {
        super(acceptor, socket);
        SslContext ctx = (passwd != null ? SslContextBuilder.forServer(publicKey, privateKey, passwd) : SslContextBuilder.forServer(publicKey, privateKey)).build();
        socket.pipeline().addBefore("readManager", "ssl", ctx.newHandler(socket.alloc()));
    }

    SSLSocket(SSLAcceptor acceptor, SocketChannel socket, SSLAcceptorConfigurator configurator) throws SSLException {
        super(acceptor, socket);
        SslContextBuilder builder = SslContextBuilder.forServer(configurator.getFactory());
        configurator.configure(builder);
        SslHandler handler = builder.build().newHandler(socket.alloc());
        SSLParameters p = handler.engine().getSSLParameters();
        SSLParameters np = configurator.changeParameters(p);
        if(np != null) handler.engine().setSSLParameters(np);
        socket.pipeline().addBefore("readManager", "ssl", handler);
    }
}
