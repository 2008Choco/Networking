package wtf.choco.network;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import wtf.choco.network.listener.MessageListener;

/**
 * Represents an internal registry mapping messages to numeric ids and constructors.
 *
 * @param <T> the type of listener for messages in this registry
 */
public final class MessageRegistry<T extends MessageListener> {

    private final Map<Class<? extends Message<T>>, Integer> messageIds = new IdentityHashMap<>();
    private final List<Function<MessageByteBuffer, ? extends Message<T>>> messageConstructors = new ArrayList<>();

    /**
     * Construct a new {@link MessageRegistry}.
     */
    MessageRegistry() { }

    /**
     * Register a new {@link Message} to this protocol.
     *
     * @param <M> the message
     *
     * @param messageClass the message class
     * @param messageConstructor a supplier to construct the message
     *
     * @return this instance. Allows for chained message calls
     */
    @NotNull
    public <M extends Message<T>> MessageRegistry<T> registerMessage(@NotNull Class<M> messageClass, @NotNull Function<MessageByteBuffer, M> messageConstructor) {
        Preconditions.checkArgument(messageClass != null, "messageClass must not be null");
        Preconditions.checkArgument(messageConstructor != null, "messageConstructor must not be null");

        int messageId = messageIds.size();

        Integer existingMessageId = messageIds.put(messageClass, messageId);
        if (existingMessageId != null) {
            throw new IllegalStateException("Attempted to register message " + messageClass.getName() + " with id " + existingMessageId.intValue() + " but is already registered.");
        }

        this.messageConstructors.add(messageConstructor);
        return this;
    }

    /**
     * Get the amount of messages registered to this message registry.
     *
     * @return the amount of messages
     */
    public int getRegisteredMessageCount() {
        return messageConstructors.size();
    }

    /**
     * Get the id of the given message class.
     *
     * @param message the class of the message whose id to get
     *
     * @return the id of the message, or -1 if no message exists
     */
    public int getMessageId(@NotNull Class<?> message) {
        return messageIds.getOrDefault(message, -1);
    }

    /**
     * Create a {@link Message} with the given message id and the provided
     * {@link MessageByteBuffer} data.
     *
     * @param messageId the id of the message to create
     * @param buffer the buffer containing message data
     *
     * @return the created message, or null if the message does not exist or could not
     * be handled
     */
    @Nullable
    public Message<T> createMessage(int messageId, @NotNull MessageByteBuffer buffer) {
        Preconditions.checkArgument(buffer != null, "buffer must not be null");

        if (messageId >= messageConstructors.size()) {
            return null;
        }

        Function<MessageByteBuffer, ? extends Message<T>> messageConstructor = messageConstructors.get(messageId);
        return (messageConstructor != null) ? messageConstructor.apply(buffer) : null;
    }

    /**
     * Get a {@link Map} containing all registered messages to their internal protocol ids.
     * <p>
     * <strong>NOTE: THIS IS NOT API AND IS ONLY VISIBLE FOR DOCUMENTATION/TESTING PURPOSES!</strong>
     *
     * @return all registered messages
     */
    @NotNull
    @Internal
    @VisibleForTesting
    public Map<Class<? extends Message<T>>, Integer> getRegisteredMessages() {
        return new HashMap<>(messageIds);
    }

}
