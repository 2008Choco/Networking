package wtf.choco.network.fabric;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public final class RawDataPayload implements CustomPacketPayload {

    public static final StreamCodec<FriendlyByteBuf, RawDataPayload> CODEC = StreamCodec.ofMember(
            (payload, buffer) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] bytes = new byte[buffer.readableBytes()];
                buffer.readBytes(bytes);
                return new RawDataPayload(bytes);
            }
    );

    private static CustomPacketPayload.Type<RawDataPayload> type;

    @ApiStatus.Internal
    static void setType(@NotNull CustomPacketPayload.Type<RawDataPayload> type) {
        RawDataPayload.type = type;
    }

    @ApiStatus.Internal
    static CustomPacketPayload.Type<RawDataPayload> getType() {
        return type;
    }

    private final byte[] data;

    public RawDataPayload(byte[] data) {
        this.data = data;
    }

    public byte[] data() {
        return data;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

}
