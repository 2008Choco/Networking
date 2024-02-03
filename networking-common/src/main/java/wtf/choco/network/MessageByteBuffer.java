package wtf.choco.network;

import com.google.common.base.Preconditions;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

/**
 * A utility class to allow for reading and writing of complex types to/from a byte array.
 */
public class MessageByteBuffer {

    private ByteBuffer inputBuffer;
    private ByteArrayOutputStream outputStream;

    private final MessageProtocol<?, ?> protocol;

    /**
     * Construct a new {@link MessageByteBuffer} wrapping a {@link ByteBuffer}.
     * Intended for reading data.
     *
     * @param protocol the protocol
     * @param buffer the buffer to wrap
     */
    public MessageByteBuffer(@NotNull MessageProtocol<?, ?> protocol, @NotNull ByteBuffer buffer) {
        Preconditions.checkArgument(protocol != null, "protocol must not be null");
        Preconditions.checkArgument(buffer != null, "buffer must not be null");

        this.protocol = protocol;
        this.inputBuffer = buffer;
    }

    /**
     * Construct a new {@link MessageByteBuffer} wrapping a byte array. Intended
     * for reading data.
     *
     * @param protocol the protocol
     * @param data the data to wrap
     */
    public MessageByteBuffer(@NotNull MessageProtocol<?, ?> protocol, byte @NotNull [] data) {
        Preconditions.checkArgument(protocol != null, "protocol must not be null");
        Preconditions.checkArgument(data != null, "data must not be null");

        this.protocol = protocol;
        this.inputBuffer = ByteBuffer.wrap(data);
    }

    /**
     * Construct a new {@link MessageByteBuffer}. Intended for writing data.
     *
     * @param protocol the protocol
     */
    public MessageByteBuffer(@NotNull MessageProtocol<?, ?> protocol) {
        Preconditions.checkArgument(protocol != null, "protocol must not be null");

        this.protocol = protocol;
        this.outputStream = new ByteArrayOutputStream();
    }

    /**
     * Write a 4 byte integer.
     *
     * @param value the value to write
     */
    public void writeInt(int value) {
        this.ensureWriting();

        for (int i = Integer.BYTES; i > 0; i--) {
            this.writeByte((byte) (value & 0xFF));
            value >>= 8;
        }
    }

    /**
     * Read a 4 byte integer.
     *
     * @return the read value
     */
    public int readInt() {
        this.ensureReading();

        int result = 0;
        for (int i = 0; i < Integer.BYTES; i++) {
            result <<= 8;
            result |= (readByte() & 0xFF);
        }

        return Integer.reverseBytes(result);
    }

    /**
     * Write a variable-length integer.
     *
     * @param value the value to write
     */
    public void writeVarInt(int value) {
        this.ensureWriting();

        while (true) {
            if ((value & ~0x7F) == 0) {
                this.writeByte(value);
                return;
            }

            this.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    /**
     * Read a variable-length integer.
     *
     * @return the read value
     *
     * @throws IllegalStateException if the var int is too large
     */
    public int readVarInt() {
        this.ensureReading();

        int value = 0;
        int length = 0;
        byte currentByte;

        while (true) {
            currentByte = readByte();
            value |= (currentByte & 0x7F) << (length * 7);

            length += 1;
            if (length > 5) {
                throw new RuntimeException("VarInt is too big");
            }

            if ((currentByte & 0x80) != 0x80) {
                break;
            }
        }

        return value;
    }

    /**
     * Write an 8 byte long.
     *
     * @param value the value to write
     */
    public void writeLong(long value) {
        this.ensureWriting();

        for (int i = Long.BYTES; i > 0; i--) {
            this.writeByte((byte) (value & 0xFF));
            value >>= 8;
        }
    }

    /**
     * Read an 8 byte long.
     *
     * @return the read value
     */
    public long readLong() {
        this.ensureReading();

        long result = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            result <<= 8;
            result |= (readByte() & 0xFF);
        }

        return Long.reverseBytes(result);
    }

    /**
     * Write a variable-length long.
     *
     * @param value the value to write
     */
    public void writeVarLong(long value) {
        this.ensureWriting();

        while (true) {
            if ((value & ~0x7F) == 0) {
                this.writeByte((byte) value);
                return;
            }

            this.writeByte((byte) (value & 0x7F) | 0x80);
            value >>>= 7;
        }
    }

    /**
     * Read a variable-length long.
     *
     * @return the read value
     *
     * @throws IllegalStateException if the var long is too large
     */
    public long readVarLong() {
        this.ensureReading();

        long value = 0;
        int length = 0;
        byte currentByte;

        while (true) {
            currentByte = readByte();
            value |= (currentByte & 0x7F) << (length * 7);

            length += 1;
            if (length > 10) {
                throw new RuntimeException("VarLong is too big");
            }

            if ((currentByte & 0x80) != 0x80) {
                break;
            }
        }
        return value;
    }

    /**
     * Write a 4 byte float.
     *
     * @param value the value to write
     */
    public void writeFloat(float value) {
        this.ensureWriting();
        this.writeInt(Float.floatToRawIntBits(value));
    }

    /**
     * Read a 4 byte float.
     *
     * @return the read value
     */
    public float readFloat() {
        this.ensureReading();
        return Float.intBitsToFloat(readInt());
    }

    /**
     * Write an 8 byte double.
     *
     * @param value the value to write
     */
    public void writeDouble(double value) {
        this.ensureWriting();
        this.writeLong(Double.doubleToRawLongBits(value));
    }

    /**
     * Read an 8 byte double.
     *
     * @return the read value
     */
    public double readDouble() {
        this.ensureReading();
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Write a boolean primitive.
     *
     * @param value the value to write
     */
    public void writeBoolean(boolean value) {
        this.ensureWriting();
        this.outputStream.write(value ? (byte) 1 : 0);
    }

    /**
     * Read a boolean primitive.
     *
     * @return the read value
     */
    public boolean readBoolean() {
        this.ensureReading();
        return inputBuffer.get() == 1;
    }

    /**
     * Write a UTF-8 String.
     *
     * @param string the string to write
     */
    public void writeString(@NotNull String string) {
        this.ensureWriting();

        Preconditions.checkArgument(string != null, "string must not be null");

        this.writeByteArray(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Read a UTF-8 String.
     *
     * @return the string
     */
    @NotNull
    public String readString() {
        this.ensureReading();
        return new String(readByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Write a {@link UUID}. This will write the UUID's most significant bits
     * followed by least its significant bits.
     *
     * @param uuid the UUID to write
     */
    public void writeUUID(@NotNull UUID uuid) {
        this.ensureWriting();

        Preconditions.checkArgument(uuid != null, "uuid must not be null");

        this.writeLong(uuid.getMostSignificantBits());
        this.writeLong(uuid.getLeastSignificantBits());
    }

    /**
     * Read a {@link UUID}. This will read the next two longs and used them as
     * the UUID's most and least significant bits respectively.
     *
     * @return the read UUID
     */
    @NotNull
    public UUID readUUID() {
        this.ensureReading();
        return new UUID(readLong(), readLong());
    }

    /**
     * Write an enumerated type. This will write the enum value's ordinal to the
     * buffer as a varint (see {@link #writeVarInt(int)}).
     *
     * @param <E> the enumerated type
     * @param value the enum value to write
     */
    public <E extends Enum<E>> void writeEnum(@NotNull E value) {
        this.ensureWriting();

        Preconditions.checkArgument(value != null, "value must not be null");

        this.writeVarInt(value.ordinal());
    }

    /**
     * Read an enumerated type. This will read the next varint and map it to a value
     * in the universe of the provided enum's type.
     *
     * @param <E> the enumerated type
     * @param enumClass the type of enum to read
     *
     * @return the read enum
     *
     * @throws ArrayIndexOutOfBoundsException if the enum ordinal that was read does
     * not fit within the universe of the provided enum type
     */
    @NotNull
    public <E extends Enum<E>> E readEnum(@NotNull Class<E> enumClass) {
        Preconditions.checkArgument(enumClass != null, "enumClass must not be null");
        return enumClass.getEnumConstants()[readVarInt()];
    }

    /**
     * Write an array of bytes.
     *
     * @param bytes the bytes to write
     */
    public void writeBytes(byte @NotNull [] bytes) {
        this.ensureWriting();

        Preconditions.checkArgument(bytes != null, "bytes must not be null");

        this.outputStream.writeBytes(bytes);
    }

    /**
     * Write an array of bytes prefixed by a variable-length int.
     *
     * @param bytes the bytes to write
     */
    public void writeByteArray(byte @NotNull [] bytes) {
        this.ensureWriting();

        Preconditions.checkArgument(bytes != null, "bytes must not be null");

        this.writeVarInt(bytes.length);
        this.writeBytes(bytes);
    }

    /**
     * Read an array of bytes prefixed by a variable-length int.
     *
     * @return the byte array
     */
    public byte @NotNull [] readByteArray() {
        this.ensureReading();

        int size = readVarInt();
        byte[] bytes = new byte[size];
        this.inputBuffer.get(bytes);

        return bytes;
    }

    /**
     * Read the remaining bytes in this buffer as an array.
     *
     * @return the bytes
     */
    public byte @NotNull [] readBytes() {
        this.ensureReading();
        return readBytes(inputBuffer.remaining());
    }

    /**
     * Read a set amount of bytes from this buffer as an array.
     *
     * @param size the amount of bytes to read
     *
     * @return the bytes
     */
    public byte @NotNull [] readBytes(int size) {
        this.ensureReading();

        byte[] bytes = new byte[size];
        this.readBytes(bytes);

        return bytes;
    }

    /**
     * Read a set amount of bytes from this buffer into a destination array. This method
     * will read up to {@code destination.length} bytes.
     *
     * @param destination the array in which the bytes should be written
     */
    public void readBytes(byte @NotNull [] destination) {
        this.ensureReading();

        Preconditions.checkArgument(destination != null, "destination must not be null");

        this.inputBuffer.get(destination);
    }

    /**
     * Write a raw byte.
     *
     * @param value the value to write
     */
    public void writeByte(byte value) {
        this.ensureWriting();
        this.outputStream.write(value);
    }

    /**
     * Write a raw byte.
     *
     * @param value the value to write
     */
    public void writeByte(int value) {
        this.ensureWriting();
        this.outputStream.write((byte) value);
    }

    /**
     * Read a raw byte.
     *
     * @return the byte
     */
    public byte readByte() {
        this.ensureReading();
        return inputBuffer.get();
    }

    /**
     * Write a custom type with serialization logic provided by the MessageProtocol's
     * custom protocol data registry.
     *
     * @param object the object to write
     *
     * @throws UnsupportedOperationException if there is no known means of serializing the
     * given type
     *
     * @see MessageProtocol#registerCustomDataType(Class, BiConsumer, Function)
     * @see MessageProtocol#registerCustomDataType(Class, Function)
     */
    public void write(@NotNull Object object) {
        this.ensureWriting();
        this.protocol.customProtocolDataRegistry.serialize(this, object);
    }

    /**
     * Read a custom type with deserialization logic provided by the MessageProtocol's
     * custom protocol data registry.
     *
     * @param <T> the type to read
     * @param type the class of the type to read
     *
     * @return the object
     *
     * @throws UnsupportedOperationException if there is no known means of serializing the
     * given type
     *
     * @see MessageProtocol#registerCustomDataType(Class, BiConsumer, Function)
     * @see MessageProtocol#registerCustomDataType(Class, Function)
     */
    @NotNull
    public <T> T read(@NotNull Class<T> type) {
        this.ensureReading();
        return protocol.customProtocolDataRegistry.deserialize(this, type);
    }

    /**
     * Get this byte buffer as a byte array (for writing).
     *
     * @return the byte array
     */
    public byte @NotNull [] asByteArray() {
        this.ensureWriting();
        return outputStream.toByteArray();
    }

    private void ensureReading() {
        if (inputBuffer == null) {
            throw new IllegalStateException("Cannot read from a write-only buffer");
        }
    }

    private void ensureWriting() {
        if (outputStream == null) {
            throw new IllegalStateException("Cannot write to a read-only buffer");
        }
    }

}
