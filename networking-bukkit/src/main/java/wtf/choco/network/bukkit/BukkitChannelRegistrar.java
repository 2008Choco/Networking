package wtf.choco.network.bukkit;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import wtf.choco.network.ChannelRegistrar;
import wtf.choco.network.Message;
import wtf.choco.network.MessageByteBuffer;
import wtf.choco.network.MessageProtocol;
import wtf.choco.network.MessageRegistry;
import wtf.choco.network.data.NamespacedKey;
import wtf.choco.network.listener.ClientboundMessageListener;
import wtf.choco.network.listener.MessageListener;
import wtf.choco.network.listener.ServerboundMessageListener;

/**
 * An abstract {@link ChannelRegistrar} implementation for Bukkit-based servers.
 *
 * @param <P> the {@link Plugin} type
 * @param <S> the serverbound message listener type
 * @param <C> the clientbound message listener type
 *
 * @see #onUnknownMessage(Player, String, byte[], int)
 * @see #onMessageReadException(Player, String, byte[], Throwable)
 * @see #onSuccessfulMessage(Player, String, Message)
 */
public abstract class BukkitChannelRegistrar<P extends Plugin, S extends ServerboundMessageListener, C extends ClientboundMessageListener> implements ChannelRegistrar<S, C> {

    protected final P plugin;
    protected final MessageProtocol<S, C> protocol;

    /**
     * @param plugin the plugin instance from which to send and receive messages
     * @param protocol the protocol instance
     */
    public BukkitChannelRegistrar(@NotNull P plugin, @NotNull MessageProtocol<S, C> protocol) {
        Preconditions.checkArgument(plugin != null, "plugin must not be null");
        Preconditions.checkArgument(protocol != null, "protocol must not be null");

        this.plugin = plugin;
        this.protocol = protocol;
    }

    @Override
    public void registerClientboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<C> registry) {
        Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, channel.toString());
    }

    @Override
    public void registerServerboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<S> registry) {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, channel.toString(), (channelString, player, data) -> {
            MessageByteBuffer buffer = new MessageByteBuffer(protocol, ByteBuffer.wrap(data));

            try {
                int messageId = buffer.readVarInt();
                Message<S> message = registry.createMessage(messageId, buffer);

                if (message == null) {
                    this.onUnknownMessage(player, channelString, data, messageId);
                    return;
                }

                S listener = onSuccessfulMessage(player, channelString, message);
                if (listener != null) {
                    message.handle(listener);
                }
            } catch (Exception e) {
                this.onMessageReadException(player, channelString, data, e);
            }
        });
    }

    /**
     * Called when a message was received from a client but the message id that was read
     * at the head of the message does not match a known registered message in the protocol.
     *
     * @param sender the player that sent the message
     * @param channel the channel on which the message was sent
     * @param data the raw byte data payload from the message (including the message id)
     * @param messageId the message id that was read from the message data
     */
    protected void onUnknownMessage(Player sender, String channel, byte[] data, int messageId) {
        sender.kickPlayer("Received unrecognized packet with id " + messageId + " (" + channel + "). Contact an administrator!");
        this.plugin.getLogger().warning("Received unknown packet sent by " + sender.getName() + " on channel \"" + channel + "\".");
    }

    /**
     * Called when a message was received from a client but reading the message failed for
     * one reason or another and threw an exception.
     *
     * @param sender the player that sent the message
     * @param channel the channel on which the message was sent
     * @param data the raw byte data payload from the message
     * @param e the exception that was thrown
     */
    protected void onMessageReadException(Player sender, String channel, byte[] data, Throwable e) {
        sender.kickPlayer("Malformed or invalid packet (" + channel + "). Contact an administrator!\nReason: " + e.getMessage());
        this.plugin.getLogger().warning("Failed to read message sent by " + sender.getName() + " on channel \"" + channel + "\". Received erroneous data.");
        e.printStackTrace();
    }

    /**
     * Called when a message was received from a client and successfully deserialized. This
     * method is expected not to <em>handle</em> the message, but rather return the
     * {@link ServerboundMessageListener} instance responsible for handling the message that
     * was received. The listener method to invoke when handling the message should be determined
     * by the {@link Message} implementation itself in its {@link Message#handle(MessageListener)}
     * method.
     * <p>
     * For example, say we have a {@literal Map<UUID, MyServerboundMessageListener} in our plugin
     * class that maps players to their respective message listener. This method should return the
     * value associated with the sender.
     * <pre>
     * // Your plugin class
     * public final class MyPlugin extends JavaPlugin {
     *
     *     private final {@literal Map<UUID, MyServerboundMessageListener>} messageListeners;
     *
     *     public MyServerboundMessageListener getMessageListener(Player player) {
     *         return messageListeners.get(player.getUniqueId());
     *     }
     *
     * }
     *
     * // Your BukkitChannelRegistrar implementation
     * public final class MyChannelRegistrar extends {@literal BukkitChannelRegistrar<MyPlugin, MyServerboundMessageListener, MyClientboundMessageListener>} {
     *
     *     public MyChannelRegistrar(MyPlugin plugin, {@literal MessageProtocol<S, C>} protocol) {
     *         super(plugin, protocol);
     *     }
     *
     *     protected MyServerboundMessageListener onSuccessfulMessage(Player player, String channel, {@literal Message<MyServerboundMessageListener>} message) {
     *         return plugin.getMessageListener(player); // That's it. The BukkitChannelRegistrar will handle the listening
     *     }
     *
     * }
     *
     * // Your message
     * public final class MyMessage implements {@literal Message<MyServerboundMessageListener>} {
     *
     *     // ...
     *
     *     // This is what gets invoked automatically, the method you choose to execute in the listener is done here!
     *     public void handle(MyServerboundMessageListener listener) {
     *         listener.handleMyMessage(this);
     *     }
     *
     * }
     * </pre>
     *
     * @param player the player that sent the message
     * @param channel the channel on which the message was sent
     * @param message the message that was received
     *
     * @return the message listener implementation that should be used to handle the message, or
     * null if the message should not be handled
     */
    @Nullable
    protected abstract S onSuccessfulMessage(Player player, String channel, Message<S> message);

}
