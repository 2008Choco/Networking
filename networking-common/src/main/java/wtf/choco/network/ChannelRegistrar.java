package wtf.choco.network;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;

import wtf.choco.network.data.NamespacedKey;
import wtf.choco.network.listener.ClientboundMessageListener;
import wtf.choco.network.listener.ServerboundMessageListener;

/**
 * Represents a registrar capable of registering incoming and outgoing message channel handlers.
 * <p>
 * Some implementations of this registrar may not need to implement message handlers in one
 * of the directions. For instance, while on a Bukkit server both incoming and outgoing
 * message channels must be registered, a Fabric client need not register an outgoing message
 * channel. Implementations of this interface should register (and handle) incoming/outgoing
 * messages and delegate them to the appropriate message listener for messages registered to
 * the supplied registries.
 *
 * @param <S> the serverbound message listener type
 * @param <C> the clientbound message listener type
 *
 * @see MessageProtocol#registerChannels(ChannelRegistrar)
 */
@OverrideOnly
public interface ChannelRegistrar<S extends ServerboundMessageListener, C extends ClientboundMessageListener> {

    /**
     * Register a message handler for messages being sent from server to client.
     *
     * @param channel the channel on which to register the handler
     * @param registry the client bound message registry
     */
    public void registerClientboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<C> registry);

    /**
     * Register a message handler for messages being sent from client to server.
     *
     * @param channel the channel on which to register the handler
     * @param registry the server bound message registry
     */
    public void registerServerboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<S> registry);

}
