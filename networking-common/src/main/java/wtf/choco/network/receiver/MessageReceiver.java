package wtf.choco.network.receiver;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.Message;
import wtf.choco.network.data.NamespacedKey;

/**
 * A target capable of being sent a {@link Message}.
 */
public interface MessageReceiver {

    /**
     * Send a message represented by the given bytes on the specified channel.
     *
     * @param channel the channel on which the message should be sent
     * @param message the message bytes to be sent
     */
    public void sendMessage(@NotNull NamespacedKey channel, byte[] message);

}
