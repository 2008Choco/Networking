package wtf.choco.network.fabric;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;

import wtf.choco.network.MessageProtocol;
import wtf.choco.network.ProtocolConfiguration;
import wtf.choco.network.data.NamespacedKey;

/**
 * A {@link ProtocolConfiguration} implementation for Fabric-based clients and servers
 * that will register necessary proxied receivers for (players, worlds, and the server).
 * <p>
 * Applying this configuration will allow callers to send messages directly to ServerPlayer,
 * Level, and MinecraftServer instances.
 */
public class FabricProtocolConfiguration implements ProtocolConfiguration {

    private final boolean client;

    /**
     * Construct a new {@link FabricProtocolConfiguration}.
     *
     * @param client true if on the client, false if on the server
     */
    public FabricProtocolConfiguration(boolean client) {
        this.client = client;
    }

    @Override
    public void configure(@NotNull MessageProtocol<?, ?> protocol) {
        this.configureCommon(protocol);

        if (client) {
            this.configureClient(protocol);
        } else {
            this.configureServer(protocol);
        }
    }

    /**
     * Configure the given {@link MessageProtocol} for both the client and server.
     *
     * @param protocol the protocol to configure
     */
    protected void configureCommon(@NotNull MessageProtocol<?, ?> protocol) {
        // TODO: Register common types that might want to be serialized
    }

    /**
     * Configure the given {@link MessageProtocol} for the client.
     *
     * @param protocol the protocol to configure
     */
    protected void configureClient(@NotNull MessageProtocol<?, ?> protocol) {
        // TODO: Register client types that might want to be serialized
    }

    /**
     * Configure the given {@link MessageProtocol} for the server.
     *
     * @param protocol the protocol to configure
     */
    protected void configureServer(@NotNull MessageProtocol<?, ?> protocol) {
        // TODO: Register server types that might want to be serialized

        protocol.registerProxiedReceiver(Player.class, (receiver, channel, message) -> {
            if (!(receiver instanceof ServerPlayer serverPlayer)) {
                return;
            }

            this.sendMessageToServerPlayer(serverPlayer, channel, message);
        });

        protocol.registerProxiedReceiver(Level.class, (receiver, channel, message) -> {
            receiver.players().forEach(player -> {
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    return;
                }

                this.sendMessageToServerPlayer(serverPlayer, channel, message);
            });
        });

        protocol.registerProxiedReceiver(MinecraftServer.class, (receiver, channel, message) -> {
            receiver.getPlayerList().getPlayers().forEach(player -> sendMessageToServerPlayer(player, channel, message));
        });
    }

    private void sendMessageToServerPlayer(ServerPlayer player, NamespacedKey channel, byte[] message) {
        FriendlyByteBuf byteBuf = PacketByteBufs.create();
        byteBuf.writeBytes(message);
        ServerPlayNetworking.send(player, new ResourceLocation(channel.namespace(), channel.key()), byteBuf);
    }

}
