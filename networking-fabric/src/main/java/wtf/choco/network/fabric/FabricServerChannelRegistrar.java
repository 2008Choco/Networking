package wtf.choco.network.fabric;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import wtf.choco.network.ChannelRegistrar;
import wtf.choco.network.Message;
import wtf.choco.network.MessageProtocol;
import wtf.choco.network.MessageRegistry;
import wtf.choco.network.data.NamespacedKey;
import wtf.choco.network.listener.ClientboundMessageListener;
import wtf.choco.network.listener.MessageListener;
import wtf.choco.network.listener.ServerboundMessageListener;

/**
 * An abstract {@link ChannelRegistrar} implementation for Fabric-based servers.
 * <p>
 * This is a convenience alternative for the {@link FabricChannelRegistrar} where client-sided
 * message handling is unnecessary.
 *
 * @param <S> the serverbound message listener type
 * @param <C> the clientbound message listener type
 *
 * @see #onUnknownServerboundMessage(MinecraftServer, ServerPlayer, Identifier, byte[], int)
 * @see #onServerboundMessageReadException(MinecraftServer, ServerPlayer, Identifier, byte[], Throwable)
 * @see #onSuccessfulMessage(MinecraftServer, ServerPlayer, Identifier, Message)
 */
public abstract class FabricServerChannelRegistrar<S extends ServerboundMessageListener, C extends ClientboundMessageListener> extends FabricChannelRegistrar<S, C> {

    public FabricServerChannelRegistrar(MessageProtocol<S, C> protocol, Logger logger) {
        super(protocol, logger, true);
    }

    // Overriding and finalizing clientbound methods
    @Override
    public final void registerClientboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<C> registry) {
        this.registerClientboundPayload(channel);
    }

    @Override
    protected final void onUnknownClientboundMessage(@NotNull Identifier channel, byte @NotNull [] data, int messageId) { }

    @Override
    protected final void onClientboundMessageReadException(@NotNull Identifier channel, byte @NotNull [] data, @NotNull Throwable e) { }

    @Nullable
    @Override
    protected final C onSuccessfulClientboundMessage(@NotNull Identifier channel, @NotNull Message<C> message) {
        return null;
    }

    // Deferring onSuccesfulServerboundMessage() to an abstract onSuccessfulMessage()
    @Nullable
    @Override
    protected final S onSuccessfulServerboundMessage(@NotNull MinecraftServer server, @NotNull ServerPlayer player, @NotNull Identifier channel, @NotNull Message<S> message) {
        return onSuccessfulMessage(server, player, channel, message);
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
     * // Your FabricServerChannelRegistrar implementation
     * public final class MyChannelRegistrar extends {@literal FabricServerChannelRegistrar<MyServerboundMessageListener, MyClientboundMessageListener>} {
     *
     *     public MyChannelRegistrar({@literal MessageProtocol<S, C>} protocol, Logger logger) {
     *         super(protocol, logger);
     *     }
     *
     *     {@literal @Override}
     *     protected MyServerboundMessageListener onSuccessfulMessage(MinecraftServer server, ServerPlayer player, Identifier channel, {@literal Message<MyServerboundMessageListener>} message) {
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
    protected abstract S onSuccessfulMessage(@NotNull MinecraftServer server, @NotNull ServerPlayer player, @NotNull Identifier channel, @NotNull Message<S> message);

}
