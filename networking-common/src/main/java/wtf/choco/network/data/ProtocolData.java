package wtf.choco.network.data;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.MessageByteBuffer;

/**
 * Represents a type that can be written to a {@link MessageByteBuffer}.
 */
public interface ProtocolData {

    /**
     * Write this type to the provided {@link MessageByteBuffer}.
     *
     * @param buffer the buffer to write to
     */
    public void write(@NotNull MessageByteBuffer buffer);

}
