package wtf.choco.network.bukkit;

import com.google.common.base.Preconditions;

import java.util.function.Supplier;

import org.bukkit.Art;
import org.bukkit.Fluid;
import org.bukkit.GameEvent;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.block.Biome;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Frog;
import org.bukkit.entity.Villager;
import org.bukkit.generator.structure.Structure;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.loot.LootTables;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import wtf.choco.network.MessageByteBuffer;
import wtf.choco.network.MessageProtocol;
import wtf.choco.network.ProtocolConfiguration;

/**
 * A {@link ProtocolConfiguration} implementation for Bukkit-based servers that will
 * register necessary proxied receivers for {@link PluginMessageRecipient
 * PluginMessageRecipients} (players, worlds, and the server), as well as some of the
 * more common Bukkit data types that might be sent across the network.
 * <p>
 * Applying this configuration will allow callers to send messages directly to Player,
 * World, and Server instances:
 * <pre>
 * ModProtocol protocol = // ...
 * Player player = Bukkit.getPlayer("2008Choco");
 *
 * protocol.sendMessageToClient(player, new MyMessage()); // The player
 * protocol.sendMessageToClient(player.getWorld(), new MyMessage()); // All players in the world
 * protocol.sendMessageToClient(Bukkit.getServer(), new MyMessage()); // All players on the server
 * </pre>
 */
public class BukkitProtocolConfiguration implements ProtocolConfiguration {

    private final Plugin plugin;

    /**
     * Construct a new {@link BukkitProtocolConfiguration}.
     *
     * @param plugin the plugin instance used to send messages
     */
    public BukkitProtocolConfiguration(@NotNull Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "plugin must not be null");
        this.plugin = plugin;
    }

    @Override
    public void configure(@NotNull MessageProtocol<?, ?> protocol) {
        protocol.registerProxiedReceiver(PluginMessageRecipient.class, (recipient, channel, message) -> recipient.sendPluginMessage(plugin, channel.toString(), message));

        protocol.registerCustomDataType(Vector.class, this::writeVector, this::readVector);
        protocol.registerCustomDataType(BlockVector.class, this::writeBlockVector, this::readBlockVector);
        protocol.registerCustomDataType(NamespacedKey.class, this::writeNamespacedKey, this::readNamespacedKey);

        // Registry-backed Keyed types
        this.registerKeyed(protocol, () -> Advancement.class, () -> Registry.ADVANCEMENT);
        this.registerKeyed(protocol, () -> Art.class, () -> Registry.ART);
        this.registerKeyed(protocol, () -> Biome.class, () -> Registry.BIOME);
        this.registerKeyed(protocol, () -> Cat.Type.class, () -> Registry.CAT_VARIANT);
        this.registerKeyed(protocol, () -> Enchantment.class, () -> Registry.ENCHANTMENT);
        this.registerKeyed(protocol, () -> EntityType.class, () -> Registry.ENTITY_TYPE);
        this.registerKeyed(protocol, () -> Fluid.class, () -> Registry.FLUID);
        this.registerKeyed(protocol, () -> Frog.Variant.class, () -> Registry.FROG_VARIANT);
        this.registerKeyed(protocol, () -> GameEvent.class, () -> Registry.GAME_EVENT);
        this.registerKeyed(protocol, () -> LootTables.class, () -> Registry.LOOT_TABLES);
        this.registerKeyed(protocol, () -> Material.class, () -> Registry.MATERIAL);
        this.registerKeyed(protocol, () -> MusicInstrument.class, () -> Registry.INSTRUMENT);
        this.registerKeyed(protocol, () -> Particle.class, () -> Registry.PARTICLE_TYPE);
        this.registerKeyed(protocol, () -> PatternType.class, () -> Registry.BANNER_PATTERN);
        this.registerKeyed(protocol, () -> PotionEffectType.class, () -> Registry.EFFECT);
        this.registerKeyed(protocol, () -> PotionType.class, () -> Registry.POTION);
        this.registerKeyed(protocol, () -> Sound.class, () -> Registry.SOUNDS);
        this.registerKeyed(protocol, () -> Statistic.class, () -> Registry.STATISTIC);
        this.registerKeyed(protocol, () -> Structure.class, () -> Registry.STRUCTURE);
        this.registerKeyed(protocol, () -> StructureType.class, () -> Registry.STRUCTURE_TYPE);
        this.registerKeyed(protocol, () -> TrimMaterial.class, () -> Registry.TRIM_MATERIAL);
        this.registerKeyed(protocol, () -> TrimPattern.class, () -> Registry.TRIM_PATTERN);
        this.registerKeyed(protocol, () -> Villager.Profession.class, () -> Registry.VILLAGER_PROFESSION);
        this.registerKeyed(protocol, () -> Villager.Type.class, () -> Registry.VILLAGER_TYPE);
    }

    private void writeVector(Vector vector, MessageByteBuffer buffer) {
        buffer.writeDouble(vector.getX());
        buffer.writeDouble(vector.getY());
        buffer.writeDouble(vector.getZ());
    }

    private Vector readVector(MessageByteBuffer buffer) {
        return new Vector(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private void writeBlockVector(BlockVector vector, MessageByteBuffer buffer) {
        buffer.writeVarInt(vector.getBlockX());
        buffer.writeVarInt(vector.getBlockY());
        buffer.writeVarInt(vector.getBlockZ());
    }

    private BlockVector readBlockVector(MessageByteBuffer buffer) {
        return new BlockVector(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    private void writeNamespacedKey(NamespacedKey key, MessageByteBuffer buffer) {
        buffer.writeString(key.toString());
    }

    private NamespacedKey readNamespacedKey(MessageByteBuffer buffer) {
        return NamespacedKey.fromString(buffer.readString());
    }

    private void writeKeyed(Keyed keyed, MessageByteBuffer buffer) {
        buffer.write(keyed.getKey());
    }

    private <T extends Keyed> T readRegistered(MessageByteBuffer buffer, Registry<T> registry) {
        return registry.get(buffer.read(NamespacedKey.class));
    }

    // Using a Supplier<Registry<T>> because VeinMiner, the primary user of this library, supports 1.17.x where some of these registries do not exist
    private <T extends Keyed> void registerKeyed(MessageProtocol<?, ?> protocol, Supplier<Class<T>> typeSupplier, Supplier<Registry<T>> registrySupplier) {
        try {
            Class<T> type = typeSupplier.get();
            Registry<T> registry = registrySupplier.get();
            protocol.registerCustomDataType(type, this::writeKeyed, buffer -> readRegistered(buffer, registry));
        } catch (Throwable ignore) { }
    }

}
