package wtf.choco.network;

import org.jetbrains.annotations.NotNull;

/**
 * A configuration object that may be used to apply changes to a {@link MessageProtocol}.
 * <p>
 * While messages cannot be registered in a configuration, things such as custom protocol
 * data types and proxied senders may be created and registered.
 */
@FunctionalInterface
public interface ProtocolConfiguration {

    /**
     * Configure the given {@link MessageProtocol}.
     *
     * @param protocol the protocol to configure
     */
    public void configure(@NotNull MessageProtocol<?, ?> protocol);

}
