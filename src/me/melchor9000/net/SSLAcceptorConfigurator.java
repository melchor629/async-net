package me.melchor9000.net;

import javax.net.ssl.KeyManagerFactory;

/**
 * For a {@link SSLAcceptor}, allows to make custom configurations for the SSL Context.
 * See {@link SSLSocketConfigurator}
 */
public abstract class SSLAcceptorConfigurator extends SSLSocketConfigurator {

    /**
     * Obtains a {@link KeyManagerFactory} with the private and public
     * keys inside it.
     * @return a {@link KeyManagerFactory} with the keys for the server
     */
    abstract KeyManagerFactory getFactory();

}
