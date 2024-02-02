package wtf.choco.network;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.listener.MessageListener;

/**
 * Represents a message sent between client and server.
 * <p>
 * By convention, a Message implementation should have a constructor that accepts a
 * {@link MessageByteBuffer} from which data may be read into final fields. Records may
 * be used for a similar purpose if the message is simple enough. An example implementation
 * may look something like the following:
 * <pre>
 * public final class MessageServerboundExample implements Message{@literal <ServerboundMessageListener>} {
 *
 *     private final String stringValue;
 *     private final int intValue;
 *
 *     // This is intended for when the message needs to be constructed to be sent to the client/server
 *     public MessageServerboundExample(String stringValue, int intValue) {
 *         this.stringValue = stringValue;
 *         this.intValue = intValue;
 *     }
 *
 *     // This is intended for reading from the byte buffer. Used on construction of this message.
 *     public MessageServerboundExample(MessageByteBuffer buffer) {
 *         this(buffer.readString(), buffer.readVarInt());
 *     }
 *
 *     public String getStringValue() {
 *         return stringValue;
 *     }
 *
 *     public int getIntValue() {
 *         return intValue;
 *     }
 *
 *     {@literal @Override}
 *     public void write(MessageByteBuffer buffer) {
 *         buffer.writeString(stringValue);
 *         buffer.writeVarInt(intValue);
 *     }
 *
 *     {@literal @Override}
 *     public void handle(ServerboundMessageListener listener) {
 *         // Handle here. Conventionally, the listener should have a method to handle this message in specific... like so:
 *         listener.handleExample(this);
 *     }
 *
 * }
 * </pre>
 *
 * @param <T> the type of listener that will handle this message
 */
public interface Message<T extends MessageListener> {

    /**
     * Write this message to the provided {@link MessageByteBuffer}.
     *
     * @param buffer the buffer to which data should be written
     */
    public void write(@NotNull MessageByteBuffer buffer);

    /**
     * Handle this message.
     *
     * @param listener the message listener
     */
    public void handle(@NotNull T listener);

}
