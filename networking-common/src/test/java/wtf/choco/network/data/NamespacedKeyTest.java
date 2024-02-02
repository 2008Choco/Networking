package wtf.choco.network.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NamespacedKeyTest {

    @Test
    void testNamespacedKey() {
        assertEquals("namespace:key", new NamespacedKey("namespace", "key").toString());

        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("namespace", "key!"));
        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("namespace", "Key"));
        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("namespace", ""));
        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("name!space", "key"));
        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("name/space", "key"));
        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("Namespace", "key"));
        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("", "key"));
        assertThrows(IllegalArgumentException.class, () -> new NamespacedKey("", "key"));

        assertDoesNotThrow(() -> new NamespacedKey("valid_namespace.example-1", "valid_key/example-1"));
    }

    @Test
    void testMinecraft() {
        assertEquals("minecraft:example", NamespacedKey.minecraft("example").toString());
    }

    @Test
    void testOf() {
        assertEquals("third_party:example", NamespacedKey.of("third_party", "example").toString());
    }

    @Test
    void testFromString() {
        assertEquals("namespace:key", NamespacedKey.fromString("namespace:key", "namespace").toString());
        assertEquals("namespace:key", NamespacedKey.fromString("namespace:key", "minecraft").toString());
        assertEquals("namespace:key", NamespacedKey.fromString("namespace:key").toString());
        assertEquals("namespace:key", NamespacedKey.fromString("key", "namespace").toString());
        assertEquals("minecraft:key", NamespacedKey.fromString("key", "minecraft").toString());
        assertEquals("minecraft:key", NamespacedKey.fromString("key").toString());
    }

    @Test
    void testEquals() {
        NamespacedKey a = NamespacedKey.minecraft("example"), b = NamespacedKey.minecraft("example");
        assertTrue(a.equals(b) && a != b);

        assertEquals(new NamespacedKey("minecraft", "example"), NamespacedKey.minecraft("example"));
        assertEquals(new NamespacedKey("namespace", "key"), NamespacedKey.of("namespace", "key"));
        assertEquals(NamespacedKey.fromString("example:value"), NamespacedKey.of("example", "value"));
    }

}
