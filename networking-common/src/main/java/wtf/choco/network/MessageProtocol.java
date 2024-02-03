package wtf.choco.network;

import com.google.common.base.Preconditions;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import wtf.choco.network.data.CustomProtocolDataRegistry;
import wtf.choco.network.data.NamespacedKey;
import wtf.choco.network.data.ProtocolData;
import wtf.choco.network.listener.ClientboundMessageListener;
import wtf.choco.network.listener.MessageListener;
import wtf.choco.network.listener.ServerboundMessageListener;
import wtf.choco.network.receiver.MessageReceiver;
import wtf.choco.network.receiver.ProxiedMessageReceiver;
import wtf.choco.network.receiver.ProxiedMessageReceiverRegistry;

/**
 * Represents a protocol definition by which messages ("custom packets") may be registered and
 * parsed in a more convenient and object-oriented way. Mods and plugins can define their own protocols
 * and register custom {@link Message} implementations.
 * <pre>
 * public static final PluginMessageProtocol{@literal <S, C>} PROTOCOL = new PluginMessageProtocol{@literal <>}(new NamespacedKey("namespace", "key"), 1,
 *     serverRegistry {@literal ->} serverRegistry
 *         .registerMessage(PluginMessageServerboundExampleOne.class, PluginMessageServerboundExampleOne::new) // 0x00
 *         .registerMessage(PluginMessageServerboundExampleTwo.class, PluginMessageServerboundExampleTwo::new), // 0x01
 *
 *     clientRegistry {@literal ->} clientRegistry
 *         .registerMessage(PluginMessageClientboundExampleOne.class, PluginMessageClientboundExampleOne::new) // 0x00
 * );
 *
 * { // Somewhere in initialization, the channels have to be registered with a ChannelRegistrar
 *     PROTOCOL.registerChannels(new MyChannelRegistrarImplementation());
 *
 *     // Now things are ready to go and send messages
 *     PROTOCOL.sendServerMessage(messageReceiver, new PluginMessageServerboundExampleOne("some parameter", "whatever data you want in here", 10));
 * }
 * </pre>
 * The above may be used to send or receive client or server bound Messages to and from third
 * party software listening for Minecraft's custom payload packet.
 *
 * @param <S> the serverbound message listener type
 * @param <C> the clientbound message listener type
 *
 * @see ChannelRegistrar
 * @see Message
 * @see MessageByteBuffer
 */
public final class MessageProtocol<S extends ServerboundMessageListener, C extends ClientboundMessageListener> {

    private final NamespacedKey channel;
    private final int version;

    private final MessageRegistry<S> serverboundRegistry = new MessageRegistry<>();
    private final MessageRegistry<C> clientboundRegistry = new MessageRegistry<>();
    private final ProxiedMessageReceiverRegistry proxiedMessageReceiverRegistry = new ProxiedMessageReceiverRegistry();
    final CustomProtocolDataRegistry customProtocolDataRegistry = new CustomProtocolDataRegistry();

    /**
     * Construct a new {@link MessageProtocol}.
     *
     * @param channel the channel on which this protocol is registered
     * @param version the protocol version
     * @param serverboundMessageSupplier the supplier to which server-bound messages should be registered
     * @param clientboundMessageSupplier the supplier to which client-bound messages should be registered
     */
    public MessageProtocol(@NotNull NamespacedKey channel, int version, @NotNull Consumer<MessageRegistry<S>> serverboundMessageSupplier, @NotNull Consumer<MessageRegistry<C>> clientboundMessageSupplier) {
        Preconditions.checkArgument(channel != null, "channel must not be null");
        Preconditions.checkArgument(serverboundMessageSupplier != null, "serverboundMessageSupplier must not be null");
        Preconditions.checkArgument(clientboundMessageSupplier != null, "clientboundMessageSupplier must not be null");

        this.channel = channel;
        this.version = version;

        // Register messages to the registry
        serverboundMessageSupplier.accept(serverboundRegistry);
        clientboundMessageSupplier.accept(clientboundRegistry);

        // Register standard native types
        this.registerCustomDataType(NamespacedKey.class, NamespacedKey::fromMesageByteBuffer);
    }

    /**
     * Get the channel on which this protocol is listening.
     *
     * @return the channel
     */
    @NotNull
    public NamespacedKey getChannel() {
        return channel;
    }

    /**
     * Get the protocol version.
     *
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Send a client-bound {@link Message} to the given {@link MessageReceiver}.
     *
     * @param receiver the receiver to which the message should be send
     * @param message the message to send
     *
     * @throws IllegalStateException if the message being sent was not registered, or if the
     * message failed to serialize for some reason
     */
    public void sendMessageToClient(@NotNull MessageReceiver receiver, @NotNull Message<C> message) {
        this.sendMessageTo(MessageDirection.CLIENTBOUND, receiver, message);
    }

    /**
     * Send a client-bound {@link Message} to the given object. The provided {@code receiver}
     * must be a registered proxied receiver.
     *
     * @param receiver the receiver to which the message should be send
     * @param message the message to send
     *
     * @throws IllegalStateException if the message being sent was not registered, or if the
     * message failed to serialize for some reason
     *
     * @see #registerProxiedReceiver(Class, ProxiedMessageReceiver)
     */
    public void sendMessageToClient(@NotNull Object receiver, @NotNull Message<C> message) {
        this.sendMessageTo(MessageDirection.CLIENTBOUND, receiver, message);
    }

    /**
     * Send a server-bound {@link Message} to the given {@link MessageReceiver}.
     *
     * @param receiver the receiver to which the message should be send
     * @param message the message to send
     *
     * @throws IllegalStateException if the message being sent was not registered, or if the
     * message failed to serialize for some reason
     */
    public void sendMessageToServer(@NotNull MessageReceiver receiver, @NotNull Message<S> message) {
        this.sendMessageTo(MessageDirection.SERVERBOUND, receiver, message);
    }

    /**
     * Send a server-bound {@link Message} to the given {@link MessageReceiver}. The provided
     * {@code receiver} must be a registered proxied receiver.
     *
     * @param receiver the receiver to which the message should be send
     * @param message the message to send
     *
     * @throws IllegalStateException if the message being sent was not registered, or if the
     * message failed to serialize for some reason
     *
     * @see #registerProxiedReceiver(Class, ProxiedMessageReceiver)
     */
    public void sendMessageToServer(@NotNull Object receiver, @NotNull Message<S> message) {
        this.sendMessageTo(MessageDirection.SERVERBOUND, receiver, message);
    }

    private void sendMessageTo(@NotNull MessageDirection direction, @NotNull MessageReceiver receiver, @NotNull Message<?> message) {
        Preconditions.checkArgument(direction != null, "direction must not be null");
        Preconditions.checkArgument(receiver != null, "receiver must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        byte[] data = serializeMessageToByteArray(direction, message);
        receiver.sendMessage(channel, data);
    }

    private void sendMessageTo(@NotNull MessageDirection direction, @NotNull Object receiver, @NotNull Message<?> message) {
        Preconditions.checkArgument(direction != null, "direction must not be null");
        Preconditions.checkArgument(receiver != null, "receiver must not be null");
        Preconditions.checkArgument(message != null, "message must not be null");

        // If the receiver is an instance of MessageReceiver, we'll delegate and use it directly instead of trying to proxy itself
        if (receiver instanceof MessageReceiver messageReceiver) {
            this.sendMessageTo(direction, messageReceiver, message);
            return;
        }

        byte[] data = serializeMessageToByteArray(direction, message);
        this.proxiedMessageReceiverRegistry.sendMessage(receiver, channel, data);
    }

    private byte[] serializeMessageToByteArray(@NotNull MessageDirection direction, @NotNull Message<?> message) {
        int messageId = getPacketRegistry(direction).getMessageId(message.getClass());
        if (messageId < 0) {
            throw new IllegalStateException("Invalid message, " + message.getClass().getName() + ". Is it registered?");
        }

        MessageByteBuffer buffer = new MessageByteBuffer(this);
        buffer.writeVarInt(messageId);

        try {
            message.write(buffer);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write message data to buffer", e);
        }

        return buffer.asByteArray();
    }

    /**
     * Configure this MessageProtocol using the given {@link ProtocolConfiguration}.
     *
     * @param configuration the configuration to apply
     *
     * @return this instance. Allows for chained method calls
     */
    @NotNull
    public MessageProtocol<S, C> configure(@NotNull ProtocolConfiguration configuration) {
        Preconditions.checkArgument(configuration != null, "configuration must not be null");

        configuration.configure(this);
        return this;
    }

    /**
     * Register messaging channels with the given {@link ChannelRegistrar}.
     *
     * @param registrar the registrar
     */
    public void registerChannels(@NotNull ChannelRegistrar<S, C> registrar) {
        Preconditions.checkArgument(registrar != null, "registrar must not be null");

        registrar.registerServerboundMessageHandler(channel, getPacketRegistry(MessageDirection.SERVERBOUND));
        registrar.registerClientboundMessageHandler(channel, getPacketRegistry(MessageDirection.CLIENTBOUND));
    }

    /**
     * Register a type that may be used as a proxy to send messages. This is particularly
     * useful when you have types provided by a third party library from which you cannot
     * implement {@link MessageReceiver}. When a proxied receiver is registered, instances
     * of that type (or those that inherit from it) may be passed directly to the
     * {@link #sendMessageToClient(Object, Message)} and {@link #sendMessageToServer(Object, Message)}
     * methods.
     *
     * @param <T> the proxy type
     * @param type the class of the proxy type
     * @param proxy the proxy receiver implementation to use when passed an instance of the
     * proxy type
     *
     * @return this instance. Allows for chained method calls
     */
    @NotNull
    public <T> MessageProtocol<S, C> registerProxiedReceiver(@NotNull Class<T> type, @NotNull ProxiedMessageReceiver<T> proxy) {
        Preconditions.checkArgument(!MessageReceiver.class.isAssignableFrom(type), "Cannot proxy a type that implements (or is) MessageReceiver: %s", type.getName());

        this.proxiedMessageReceiverRegistry.registerProxiedReceiver(type, proxy);
        return this;
    }

    /**
     * Register a custom data type that may be serialized and deserialized across this
     * protocol via {@link MessageByteBuffer#read(Class)} and {@link MessageByteBuffer
     * #write(Object)}. This is particularly useful when you have types provided by a
     * third party library that you wish to serialize and deserialize often over the
     * network via a simple convenience method.
     * <p>
     * Because some libraries hide implementation types (Bukkit, namely), the type
     * class provided to this method may be an API type, and any children of that type
     * will be considered when reading or writing to the buffer. The type passed to
     * this method, however, will always be prioritized and searched for first, so if
     * there is class A and class B that extends A, only A needs to be registered in
     * order for both types to be serialized and deserialized, but B will be serialized
     * and deserialized as A. If you wish for B to be handled individually, you would
     * need to register a new custom data type for class B.
     *
     * @param <T> the type to serialize and deserialize
     * @param type the class of the type
     * @param serializer the serialization function to apply when trying to write an
     * object of this type to a {@link MessageByteBuffer}
     * @param deserializer the deserialization function to apply when trying to read
     * an object of this type from a {@link MessageByteBuffer}
     *
     * @return this instance. Allows for chained method calls
     */
    @NotNull
    public <T> MessageProtocol<S, C> registerCustomDataType(@NotNull Class<T> type, @NotNull BiConsumer<T, MessageByteBuffer> serializer, @NotNull Function<MessageByteBuffer, T> deserializer) {
        this.customProtocolDataRegistry.registerType(type, serializer, deserializer);
        return this;
    }

    /**
     * Register a custom data type that may be serialized and deserialized across this
     * protocol via {@link MessageByteBuffer#read(Class)} and {@link MessageByteBuffer
     * #write(Object)}. This is particularly useful when you have types provided by a
     * first party library that you wish to serialize and deserialize often over the
     * network via a simple convenience method.
     * <p>
     * This is a convenience function for types that implement {@link ProtocolData}
     * and is equivalent to {@code registerCustomDataType(type, ProtocolData::write,
     * deserializer)}.
     * <p>
     * Because some libraries hide implementation types (Bukkit, namely), the type
     * class provided to this method may be an API type, and any children of that type
     * will be considered when reading or writing to the buffer. The type passed to
     * this method, however, will always be prioritized and searched for first, so if
     * there is class A and class B that extends A, only A needs to be registered in
     * order for both types to be serialized and deserialized, but B will be serialized
     * and deserialized as A. If you wish for B to be handled individually, you would
     * need to register a new custom data type for class B.
     *
     * @param <T> the type to serialize and deserialize
     * @param type the class of the type
     * @param deserializer the deserialization function to apply when trying to read
     * an object of this type from a {@link MessageByteBuffer}
     *
     * @return this instance. Allows for chained method calls
     */
    @NotNull
    public <T extends ProtocolData> MessageProtocol<S, C> registerCustomDataType(@NotNull Class<T> type, @NotNull Function<MessageByteBuffer, T> deserializer) {
        this.customProtocolDataRegistry.registerType(type, deserializer);
        return this;
    }

    /**
     * Get the {@link MessageRegistry} for the given {@link MessageDirection}.
     * <p>
     * <strong>NOTE: THIS IS NOT API AND IS ONLY VISIBLE FOR DOCUMENTATION PURPOSES!</strong>
     *
     * @param <T> the listener interface type
     * @param direction the direction
     *
     * @return the registry
     */
    @NotNull
    @Internal
    @VisibleForTesting
    @SuppressWarnings("unchecked")
    public <T extends MessageListener> MessageRegistry<T> getPacketRegistry(@NotNull MessageDirection direction) {
        return switch (direction) {
            case CLIENTBOUND -> (MessageRegistry<T>) clientboundRegistry;
            case SERVERBOUND -> (MessageRegistry<T>) serverboundRegistry;
            default -> throw new UnsupportedOperationException("Unsupported MessageDirection: " + direction);
        };
    }

}
