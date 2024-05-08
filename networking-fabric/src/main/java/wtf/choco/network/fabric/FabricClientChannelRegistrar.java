package wtf.choco.network.fabric;

import net.minecraft.resources.ResourceLocation;
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
 * An abstract {@link ChannelRegistrar} implementation for Fabric-based clients.
 * <p>
 * This is a convenience alternative for the {@link FabricChannelRegistrar} where server-sided
 * message handling is unnecessary.
 *
 * @param <S> the serverbound message listener type
 * @param <C> the clientbound message listener type
 *
 * @see #onUnknownClientboundMessage(ResourceLocation, byte[], int)
 * @see #onClientboundMessageReadException(ResourceLocation, byte[], Throwable)
 * @see #onSuccessfulMessage(ResourceLocation, Message)
 */
public abstract class FabricClientChannelRegistrar<S extends ServerboundMessageListener, C extends ClientboundMessageListener> extends FabricChannelRegistrar<S, C> {

    public FabricClientChannelRegistrar(MessageProtocol<S, C> protocol, Logger logger) {
        super(protocol, logger, true);
    }

    // Overriding and finalizing serverbound methods
    @Override
    public final void registerServerboundMessageHandler(@NotNull NamespacedKey channel, @NotNull MessageRegistry<S> registry) {
        this.registerServerboundPayload(channel);
    }

    @Override
    protected final void onUnknownServerboundMessage(@NotNull MinecraftServer server, @NotNull ServerPlayer sender, @NotNull ResourceLocation channel, byte @NotNull [] data, int messageId) { }

    @Override
    protected final void onServerboundMessageReadException(@NotNull MinecraftServer server, @NotNull ServerPlayer sender, @NotNull ResourceLocation channel, byte @NotNull [] data, @NotNull Throwable e) { }

    @Nullable
    @Override
    protected final S onSuccessfulServerboundMessage(@NotNull MinecraftServer server, @NotNull ServerPlayer player, @NotNull ResourceLocation channel, @NotNull Message<S> message) {
        return null;
    }

    // Deferring onSuccesfulClientboundMessage() to an abstract onSuccessfulMessage()
    @Nullable
    @Override
    protected final C onSuccessfulClientboundMessage(@NotNull ResourceLocation channel, @NotNull Message<C> message) {
        return onSuccessfulMessage(channel, message);
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
     * // Your FabricClientChannelRegistrar implementation
     * public final class MyChannelRegistrar extends {@literal FabricClientChannelRegistrar<MyServerboundMessageListener, MyClientboundMessageListener>} {
     *
     *     public MyChannelRegistrar({@literal MessageProtocol<S, C>} protocol, Logger logger) {
     *         super(protocol, logger);
     *     }
     *
     *     {@literal @Override}
     *     protected MyClientboundMessageListener onSuccessfulMessage(ResourceLocation channel, {@literal Message<MyClientboundMessageListener>} message) {
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
    protected abstract C onSuccessfulMessage(@NotNull ResourceLocation channel, @NotNull Message<C> message);

}
