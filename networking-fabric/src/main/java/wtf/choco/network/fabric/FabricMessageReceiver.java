package wtf.choco.network.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.data.NamespacedKey;
import wtf.choco.network.receiver.MessageReceiver;

public interface FabricMessageReceiver extends MessageReceiver {

    public void sendMessage(@NotNull RawDataPayload payload);

    /**
     * {@inheritDoc}
     *
     * @deprecated you don't need to implement this for Fabric types. Implement
     * {@link #sendMessage(RawDataPayload)} instead
     */
    @Deprecated
    @Override
    default void sendMessage(@NotNull NamespacedKey channel, byte @NotNull [] message) {
        FriendlyByteBuf byteBuf = PacketByteBufs.create();
        byteBuf.writeBytes(message);
        this.sendMessage(new RawDataPayload(byteBuf.array()));
    }

}
