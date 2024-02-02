package wtf.choco.network.receiver;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.Message;
import wtf.choco.network.MessageProtocol;
import wtf.choco.network.data.NamespacedKey;

/**
 * A proxied target capable of being sent a {@link Message}.
 *
 * @param <T> the receiver being proxied
 *
 * @see MessageProtocol#registerProxiedReceiver(Class, ProxiedMessageReceiver)
 */
@FunctionalInterface
public interface ProxiedMessageReceiver<T> {

    /**
     * Send a message represented by the given bytes on the specified channel.
     *
     * @param receiver the proxied object to receive the message
     * @param channel the channel on which the message should be sent
     * @param message the message bytes to be sent
     */
    public void sendMessage(@NotNull T receiver, @NotNull NamespacedKey channel, byte[] message);

}
