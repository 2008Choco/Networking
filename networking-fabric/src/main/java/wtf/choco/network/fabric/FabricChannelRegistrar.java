package wtf.choco.network.fabric;

import com.google.common.base.Preconditions;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

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
 * An abstract {@link ChannelRegistrar} implementation for Fabric-based clients and servers.
 *
 * @param <S> the serverbound message listener type
 * @param <C> the clientbound message listener type
 *
 * @see #onUnknownClientboundMessage(ResourceLocation, byte[], int)
 * @see #onUnknownServerboundMessage(MinecraftServer, ServerPlayer, ResourceLocation, byte[], int)
 * @see #onClientboundMessageReadException(ResourceLocation, byte[], Throwable)
 * @see #onServerboundMessageReadException(MinecraftServer, ServerPlayer, ResourceLocation, byte[], Throwable)
 * @see #onSuccessfulClientboundMessage(ResourceLocation, Message)
 * @see #onSuccessfulServerboundMessage(MinecraftServer, ServerPlayer, ResourceLocation, Message)
 */
public abstract class FabricChannelRegistrar<S extends ServerboundMessageListener, C extends ClientboundMessageListener> implements ChannelRegistrar<S, C> {

    protected final MessageProtocol<S, C> protocol;
    protected final Logger logger;
    protected final boolean registerClientboundReceiver, registerServerboundReceiver;

    /**
     * @param protocol the protocol instance
     * @param logger the logger to use when printing warnings or errors
     * @param registerClientboundReceiver whether or not to register a clientbound message receiver
     * @param registerServerboundReceiver whether or not to register a serverbound message receiver
     */
    public FabricChannelRegistrar(@NotNull MessageProtocol<S, C> protocol, @NotNull Logger logger, boolean registerClientboundReceiver, boolean registerServerboundReceiver) {
        Preconditions.checkArgument(protocol != null, "protocol must not be null");
        Preconditions.checkArgument(logger != null, "logger must not be null");

        this.protocol = protocol;
        this.logger = logger;
        this.registerClientboundReceiver = registerClientboundReceiver;
        this.registerServerboundReceiver = registerServerboundReceiver;
    }

    /**
     * Convenience constructor, equivalent to calling
     * {@code FabricChannelRegistrar(protocol, logger, client, !client)}.
     *
     * @param protocol the protocol instance
     * @param logger the logger to use when printing warnings or errors
     * @param client true if on the client, false if on the server
     */
    public FabricChannelRegistrar(@NotNull MessageProtocol<S, C> protocol, @NotNull Logger logger, boolean client) {
        this(protocol, logger, client, !client);
    }

    @Override
    public void registerClientboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<C> registry) {
        if (!registerClientboundReceiver) {
            return;
        }

        ResourceLocation channelKey = new ResourceLocation(channel.namespace(), channel.key());
        ClientPlayNetworking.registerGlobalReceiver(channelKey, (client, handler, buf, responseSender) -> {
            MessageByteBuffer buffer = new MessageByteBuffer(protocol, buf.nioBuffer());

            try {
                int messageId = buffer.readVarInt();
                Message<C> message = registry.createMessage(messageId, buffer);

                // Ignore any unknown messages
                if (message == null) {
                    this.onUnknownClientboundMessage(channelKey, buf.array(), messageId);
                    return;
                }

                C listener = onSuccessfulClientboundMessage(channelKey, message);
                if (listener != null) {
                    message.handle(listener);
                }
            } catch (Exception e) {
                this.onClientboundMessageReadException(channelKey, buf.array(), e);
            }
        });
    }

    @Override
    public void registerServerboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<S> registry) {
        if (!registerServerboundReceiver) {
            return;
        }

        ResourceLocation channelKey = new ResourceLocation(channel.namespace(), channel.key());
        ServerPlayNetworking.registerGlobalReceiver(channelKey, (server, player, handler, buf, responseSender) -> {
            MessageByteBuffer buffer = new MessageByteBuffer(protocol, buf.nioBuffer());

            try {
                int messageId = buffer.readVarInt();
                Message<S> message = registry.createMessage(messageId, buffer);

                // Ignore any unknown messages
                if (message == null) {
                    this.onUnknownServerboundMessage(server, player, channelKey, buf.array(), messageId);
                    return;
                }

                S listener = onSuccessfulServerboundMessage(server, player, channelKey, message);
                if (listener != null) {
                    message.handle(listener);
                }
            } catch (Exception e) {
                this.onServerboundMessageReadException(server, player, channelKey, buf.array(), e);
            }
        });
    }

    /**
     * Called when a message was received from the server but the message id that was read
     * at the head of the message does not match a known registered message in the protocol.
     *
     * @param channel the channel on which the message was sent
     * @param data the raw byte data payload from the message (including the message id)
     * @param messageId the message id that was read from the message data
     */
    protected void onUnknownClientboundMessage(@NotNull ResourceLocation channel, byte @NotNull [] data, int messageId) {
        this.logger.warn("Received unknown packet with id " + messageId + " from server on channel \"" + channel + "\". Ignoring.");
    }

    /**
     * Called when a message was received from a client but the message id that was read
     * at the head of the message does not match a known registered message in the protocol.
     *
     * @param server the Minecraft server instance
     * @param sender the player that sent the message
     * @param channel the channel on which the message was sent
     * @param data the raw byte data payload from the message (including the message id)
     * @param messageId the message id that was read from the message data
     */
    protected void onUnknownServerboundMessage(@NotNull MinecraftServer server, @NotNull ServerPlayer sender, @NotNull ResourceLocation channel, byte @NotNull [] data, int messageId) {
        this.logger.warn("Received unknown packet with id " + messageId + " from " + sender.getName().getString() + " on channel \"" + channel + "\". Ignoring.");
    }

    /**
     * Called when a message was received from the server but reading the message failed for
     * one reason or another and threw an exception.
     *
     * @param channel the channel on which the message was sent
     * @param data the raw byte data payload from the message
     * @param e the exception that was thrown
     */
    protected void onClientboundMessageReadException(@NotNull ResourceLocation channel, byte @NotNull [] data, @NotNull Throwable e) {
        this.logger.warn("Failed to read message sent from server on channel \"" + channel + "\". Received erroneous data.");
        e.printStackTrace();
    }

    /**
     * Called when a message was received from a client but reading the message failed for
     * one reason or another and threw an exception.
     *
     * @param server the Minecraft server instance
     * @param sender the player that sent the message
     * @param channel the channel on which the message was sent
     * @param data the raw byte data payload from the message
     * @param e the exception that was thrown
     */
    protected void onServerboundMessageReadException(@NotNull MinecraftServer server, @NotNull ServerPlayer sender, @NotNull ResourceLocation channel, byte @NotNull [] data, @NotNull Throwable e) {
        this.logger.warn("Failed to read message sent by " + sender.getName().getString() + " on channel \"" + channel + "\". Received erroneous data.");
        e.printStackTrace();
    }

    /**
     * Called when a message was received from the server and successfully deserialized. This
     * method is expected not to <em>handle</em> the message, but rather return the
     * {@link ClientboundMessageListener} instance responsible for handling the message that
     * was received. The listener method to invoke when handling the message should be determined
     * by the {@link Message} implementation itself in its {@link Message#handle(MessageListener)}
     * method.
     * <p>
     * For example, say we have a single instance of MyClientboundMessageHandler in our mod
     * class that handles incoming messages. This method should return that instance.
     * <pre>
     * // Your mod class
     * public final class MyMod {
     *
     *     public static final MyClientboundMessageListener MESSAGE_LISTENER = new MyClientboundMessageListener();
     *
     * }
     *
     * // Your FabricChannelRegistrar implementation
     * public final class MyChannelRegistrar extends {@literal FabricChannelRegistrar<MyServerboundMessageListener, MyClientboundMessageListener>} {
     *
     *     public MyChannelRegistrar({@literal MessageProtocol<S, C>} protocol, Logger logger) {
     *         super(protocol, logger, true); // true = on the client
     *     }
     *
     *     protected MyClientboundMessageListener onSuccessfulClientboundMessage(ResourceLocation channel, {@literal Message<MyClientboundMessageListener>} message) {
     *         return MyMod.MESSAGE_LISTENER; // That's it. The FabricChannelRegistrar will handle the listening
     *     }
     *
     * }
     *
     * // Your message
     * public final class MyClientboundMessage implements {@literal Message<MyClientboundMessageListener>} {
     *
     *     // ...
     *
     *     // This is what gets invoked automatically, the method you choose to execute in the listener is done here!
     *     public void handle(MyClientboundMessageListener listener) {
     *         listener.handleMyMessage(this);
     *     }
     *
     * }
     * </pre>
     *
     * @param channel the channel on which the message was sent
     * @param message the message that was received
     *
     * @return the message listener implementation that should be used to handle the message, or
     * null if the message should not be handled
     */
    @Nullable
    protected C onSuccessfulClientboundMessage(@NotNull ResourceLocation channel, @NotNull Message<C> message) {
        this.logger.info("Received message from server (" + message.getClass().getName() + ") but it was not handled. Did you override onSuccessfulClientboundMessage()?");
        return null;
    }

    /**
     * Called when a message was received from a client and successfully deserialized. This
     * method is expected not to <em>handle</em> the message, but rather return the
     * {@link ServerboundMessageListener} instance responsible for handling the message that
     * was received. The listener method to invoke when handling the message should be determined
     * by the {@link Message} implementation itself in its {@link Message#handle(MessageListener)}
     * method.
     * <p>
     * For example, say we have a {@literal Map<UUID, MyServerboundMessageListener} in our mod
     * class that maps players to their respective message listener. This method should return the
     * value associated with the sender.
     * <pre>
     * // Your mod class
     * public final class MyMod {
     *
     *     private final {@literal Map<UUID, MyServerboundMessageListener>} messageListeners;
     *
     *     public MyServerboundMessageListener getMessageListener(Player player) {
     *         return messageListeners.get(player.getUniqueId());
     *     }
     *
     * }
     *
     * // Your FabricChannelRegistrar implementation
     * public final class MyChannelRegistrar extends {@literal FabricChannelRegistrar<MyServerboundMessageListener, MyClientboundMessageListener>} {
     *
     *     public MyChannelRegistrar({@literal MessageProtocol<S, C>} protocol, Logger logger) {
     *         super(protocol, logger, false); // false = on the server
     *     }
     *
     *     protected MyServerboundMessageListener onSuccessfulServerboundMessage(MinecraftServer server, ServerPlayer player, ResourceLocation channel, {@literal Message<MyServerboundMessageListener>} message) {
     *         return MyMod.getInstance().getMessageListener(player); // That's it. The FabricChannelRegistrar will handle the listening
     *     }
     *
     * }
     *
     * // Your message
     * public final class MyServerboundMessage implements {@literal Message<MyServerboundMessageListener>} {
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
     * @param server the Minecraft server instance
     * @param player the player that sent the message
     * @param channel the channel on which the message was sent
     * @param message the message that was received
     *
     * @return the message listener implementation that should be used to handle the message, or
     * null if the message should not be handled
     */
    @Nullable
    protected S onSuccessfulServerboundMessage(@NotNull MinecraftServer server, @NotNull ServerPlayer player, @NotNull ResourceLocation channel, @NotNull Message<S> message) {
        this.logger.info("Received message from " + player.getName().getString() + " (" + message.getClass().getName() + ") but it was not handled. Did you override onSuccessfulServerboundMessage()?");
        return null;
    }

}
