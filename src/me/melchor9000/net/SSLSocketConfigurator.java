package me.melchor9000.net;

import io.netty.handler.ssl.SslContextBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLParameters;

/**
 * For a {@link SSLSocket}, allows to make custom configurations for the SSL Context
 */
public abstract class SSLSocketConfigurator {

    /**
     * Allows a custom configuration for the SSL Context of the socket
     * @param builder {@link SslContextBuilder} to configure
     * @see <a href="https://netty.io/4.1/api/io/netty/handler/ssl/SslContextBuilder.html">SslContextBuilder</a> documentation
     */
    public abstract void configure(@NotNull SslContextBuilder builder);

    /**
     * Allows to change some parameters in the SSL engine, like enable
     * host identification for HTTPS.
     * @param p the initial parameters
     * @return the parameters to set in the engine
     * @see <a href="http://docs.oracle.com/javase/7/docs/api/javax/net/ssl/SSLParameters.html">SSLParameters</a> documentation
     */
    public @Nullable SSLParameters changeParameters(@NotNull SSLParameters p) {
        return null;
    }

}
