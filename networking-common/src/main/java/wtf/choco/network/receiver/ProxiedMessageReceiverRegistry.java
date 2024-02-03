package wtf.choco.network.receiver;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.Message;
import wtf.choco.network.MessageProtocol;
import wtf.choco.network.data.NamespacedKey;

/**
 * A registry for handling {@link ProxiedMessageReceiver ProxiedMessageReceiver} for a
 * {@link MessageProtocol}.
 */
public final class ProxiedMessageReceiverRegistry {

    private final Map<Class<?>, ProxiedMessageReceiver<?>> proxies = new HashMap<>();

    /**
     * Register a type that may be used as a proxy to send messages. This is particularly
     * useful when you have types provided by a third party library from which you cannot
     * implement {@link MessageReceiver}. When a proxied receiver is registered, instances
     * of that type (or those that inherit from it) may be passed directly to the
     * {@link MessageProtocol#sendMessageToClient(Object, Message)} and {@link
     * MessageProtocol#sendMessageToServer(Object, Message)} methods.
     *
     * @param <T> the proxy type
     * @param type the class of the proxy type
     * @param proxy the proxy receiver implementation to use when passed an instance of the
     * proxy type
     */
    public <T> void registerProxiedReceiver(@NotNull Class<T> type, @NotNull ProxiedMessageReceiver<T> proxy) {
        Preconditions.checkArgument(type != null, "type must not be null");
        Preconditions.checkArgument(proxy != null, "proxy must not be null");

        this.proxies.put(type, proxy);
    }

    /**
     * Send a raw message to the given receiver.
     *
     * @param <T> the proxied receiver type
     * @param receiver the proxied recipient of the message
     * @param channel the channel on which the message should be sent
     * @param data the data to send
     *
     * @throws UnsupportedOperationException if there is no known proxy types for the
     * given type
     */
    public <T> void sendMessage(@NotNull T receiver, @NotNull NamespacedKey channel, byte @NotNull [] data) {
        Preconditions.checkArgument(receiver != null, "receiver must not be null");
        Preconditions.checkArgument(channel != null, "key must not be null");
        Preconditions.checkArgument(data != null, "data must not be null");

        this.findProxiedType(receiver.getClass()).sendMessage(receiver, channel, data);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private <T> ProxiedMessageReceiver<T> findProxiedType(@NotNull Class<?> type) {
        // Try to find the type directly first
        ProxiedMessageReceiver<?> receiver = proxies.get(type);
        if (receiver != null) {
            try {
                return (ProxiedMessageReceiver<T>) receiver;
            } catch (ClassCastException e) {
                throw new UnsupportedOperationException("Don't know how to proxy \"" + type.getName() + "\". Failed to cast known proxy to type", e);
            }
        }

        // Otherwise we'll try and find subtypes (due to APIs like Bukkit where often implementation types are referenced)
        for (Map.Entry<Class<?>, ProxiedMessageReceiver<?>> entry : proxies.entrySet()) {
            if (!entry.getClass().isAssignableFrom(type)) {
                continue;
            }

            try {
                return (ProxiedMessageReceiver<T>) proxies.get(type);
            } catch (ClassCastException e) {
                throw new UnsupportedOperationException("Don't know how to proxy \"" + type.getName() + "\". Failed to cast known proxy to type", e);
            }
        }

        throw new UnsupportedOperationException("Don't know how to proxy \"" + type.getName() + "\". Is it or one of its parent types registered?");
    }

}
