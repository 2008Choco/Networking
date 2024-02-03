package wtf.choco.network.data;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import wtf.choco.network.MessageByteBuffer;

/**
 * Represents a unique key with a namespace. Keys are under the format {@code namespace:key}.
 *
 * @param namespace the namespace
 * @param key the key
 */
public record NamespacedKey(@NotNull String namespace, @NotNull String key) implements ProtocolData {

    private static final String NAMESPACE_MINECRAFT = "minecraft";

    public NamespacedKey {
        Preconditions.checkArgument(isValidNamespace(namespace), "invalid namespace: \"%s\", must match [A-Z0-9._-]", namespace);
        Preconditions.checkArgument(isValidKey(key), "invalid namespace: \"%s\", must match [A-Z0-9._-/]", key);
    }

    @Override
    public void write(@NotNull MessageByteBuffer buffer) {
        buffer.writeString(toString());
    }

    @Override
    public String toString() {
        return namespace + ":" + key;
    }

    /**
     * Create a {@link NamespacedKey} under the {@code minecraft} namespace.
     *
     * @param key the key
     *
     * @return the Minecraft namespaced key
     *
     * @throws IllegalArgumentException if the key follows an invalid format
     */
    @NotNull
    public static NamespacedKey minecraft(@NotNull String key) {
        return new NamespacedKey(NAMESPACE_MINECRAFT, key);
    }

    /**
     * Create a {@link NamespacedKey} using the given namespace and key.
     *
     * @param namespace the namespace
     * @param key the key
     *
     * @return the namespaced key
     *
     * @throws IllegalArgumentException if the namespace or key follows an invalid format
     */
    @NotNull
    public static NamespacedKey of(@NotNull String namespace, @NotNull String key) {
        return new NamespacedKey(namespace, key);
    }

    /**
     * Create a {@link NamespacedKey} from an input string. If the input does not have a valid
     * namespace, the provided namespace will be used in conjunction with the rest of the key.
     *
     * @param input the input string
     * @param defaultNamespace the namespace to use if none is present in the input string
     *
     * @return the resulting key
     *
     * @throws IllegalArgumentException if the default namespace, or namespace or key of the input
     * follows an invalid format
     */
    @NotNull
    public static NamespacedKey fromString(@NotNull String input, @NotNull String defaultNamespace) {
        String[] inputComponents = input.split(":", 2);
        return (inputComponents.length >= 2) ? new NamespacedKey(inputComponents[0], inputComponents[1]) : new NamespacedKey(defaultNamespace, input);
    }

    /**
     * Get a {@link NamespacedKey} from an input string. If the input does not have a valid
     * namespace, the Minecraft namespace will be used in conjunction with the rest of the key.
     *
     * @param input the input string
     *
     * @return the resulting key
     *
     * @throws IllegalArgumentException if the namespace or key of the input follows an invalid
     * format
     */
    @NotNull
    public static NamespacedKey fromString(@NotNull String input) {
        return fromString(input, NAMESPACE_MINECRAFT);
    }

    /**
     * Read a {@link NamespacedKey} from a {@link MessageByteBuffer}. This will read the first
     * string from the buffer and parse a NamespacedKey via {@link #fromString(String)}. If the
     * input does not have a valid namespace, the provided namespace will be used in conjunction
     * with the rest of the key.
     *
     * @param buffer the input buffer
     * @param defaultNamespace the namespace to use if none is present in the read string
     *
     * @return the resulting key
     */
    @NotNull
    public static NamespacedKey fromMesageByteBuffer(@NotNull MessageByteBuffer buffer, @NotNull String defaultNamespace) {
        return fromString(buffer.readString(), defaultNamespace);
    }

    /**
     * Read a {@link NamespacedKey} from a {@link MessageByteBuffer}. This will read the first
     * string from the buffer and parse a NamespacedKey via {@link #fromString(String)}. If the
     * input does not have a valid namespace, the Minecraft namespace will be used in conjunction
     * with the rest of the key.
     *
     * @param buffer the input buffer
     *
     * @return the resulting key
     */
    @NotNull
    public static NamespacedKey fromMesageByteBuffer(@NotNull MessageByteBuffer buffer) {
        return fromString(buffer.readString(), NAMESPACE_MINECRAFT);
    }

    private static boolean isValidNamespaceChar(char character) {
        return (character >= 'a' && character <= 'z') || (character >= '0' && character <= '9') || character == '.' || character == '_' || character == '-';
    }

    private static boolean isValidKeyChar(char character) {
        return isValidNamespaceChar(character) || character == '/';
    }

    private static boolean isValidNamespace(@Nullable String namespace) {
        if (namespace == null) {
            return false;
        }

        int length = namespace.length();
        if (length == 0) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (!isValidNamespaceChar(namespace.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidKey(@Nullable String key) {
        if (key == null) {
            return false;
        }

        int length = key.length();
        if (length == 0) {
            return false;
        }

        for (int i = 0; i < length; ++i) {
            if (!isValidKeyChar(key.charAt(i))) {
                return false;
            }
        }

        return true;
    }

}
