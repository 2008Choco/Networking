package wtf.choco.network.data;

import com.google.common.base.Preconditions;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.MessageByteBuffer;

/**
 * A registry for handling custom type serialization and deserialization to a
 * {@link MessageByteBuffer}.
 */
public final class CustomProtocolDataRegistry {

    private final Map<Class<?>, SerializationStrategy<?>> serializers = new HashMap<>();

    private record SerializationStrategy<T>(@NotNull Class<T> type, @NotNull BiConsumer<T, MessageByteBuffer> serializer, @NotNull Function<MessageByteBuffer, T> deserializer) { }

    /**
     * Register a custom data type that may be serialized and deserialized across a
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
     */
    public <T> void registerType(@NotNull Class<T> type, @NotNull BiConsumer<T, MessageByteBuffer> serializer, @NotNull Function<MessageByteBuffer, T> deserializer) {
        Preconditions.checkArgument(type != null, "type must not be null");
        Preconditions.checkArgument(serializer != null, "serializer must not be null");
        Preconditions.checkArgument(deserializer != null, "deserializer must not be null");

        this.serializers.put(type, new SerializationStrategy<>(type, serializer, deserializer));
    }

    /**
     * Register a custom data type that may be serialized and deserialized across a
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
     */
    public <T extends ProtocolData> void registerType(@NotNull Class<T> type, @NotNull Function<MessageByteBuffer, T> deserializer) {
        Preconditions.checkArgument(type != null, "type must not be null");
        this.registerType(type, ProtocolData::write, deserializer);
    }

    /**
     * Serialize an object to the given {@link MessageByteBuffer}.
     *
     * @param <T> the object type to serialize
     * @param buffer the buffer to which the data should be written
     * @param data the data to write
     *
     * @throws UnsupportedOperationException if there is no known means of serializing the
     * given type
     */
    public <T> void serialize(@NotNull MessageByteBuffer buffer, @NotNull T data) {
        Preconditions.checkArgument(buffer != null, "buffer must not be null");
        Preconditions.checkArgument(data != null, "data must not be null");

        this.<T>findSerializationStrategy(data.getClass(), "serialize").serializer().accept(data, buffer);
    }

    /**
     * Deserialize an object from the given {@link MessageByteBuffer}.
     *
     * @param <T> the object type to deserialize
     * @param buffer the buffer from which the object should be read
     * @param type the type to read
     *
     * @return the object
     *
     * @throws UnsupportedOperationException if there is no known means of deserializing the
     * given type
     */
    public <T> T deserialize(@NotNull MessageByteBuffer buffer, @NotNull Class<T> type) {
        Preconditions.checkArgument(buffer != null, "buffer must not be null");
        Preconditions.checkArgument(type != null, "type must not be null");

        return this.<T>findSerializationStrategy(type, "deserialize").deserializer().apply(buffer);
    }

    @SuppressWarnings("unchecked")
    private <T> SerializationStrategy<T> findSerializationStrategy(@NotNull Class<?> type, @NotNull String serializeOrDeserialize) {
        // Try to find the type directly first
        SerializationStrategy<?> strategy = serializers.get(type);
        if (strategy != null) {
            try {
                return (SerializationStrategy<T>) strategy;
            } catch (ClassCastException e) {
                throw new UnsupportedOperationException("Don't know how to " + serializeOrDeserialize + " \"" + type.getName() + "\". Failed to cast known " + serializeOrDeserialize + "r to type", e);
            }
        }

        // Otherwise we'll try and find subtypes (due to APIs like Bukkit where often implementation types are referenced)
        for (Map.Entry<Class<?>, SerializationStrategy<?>> entry : serializers.entrySet()) {
            if (!entry.getClass().isAssignableFrom(type)) {
                continue;
            }

            try {
                return (SerializationStrategy<T>) serializers.get(type);
            } catch (ClassCastException e) {
                throw new UnsupportedOperationException("Don't know how to " + serializeOrDeserialize + " \"" + type.getName() + "\". Failed to cast known " + serializeOrDeserialize + "r to type", e);
            }
        }

        throw new UnsupportedOperationException("Don't know how to " + serializeOrDeserialize + " \"" + type.getName() + "\". Is it or one of its parent types registered?");
    }

}
