package wtf.choco.network;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageDirectionTest {

    @Test
    void testIsClientbound() {
        this.testDirection(MessageDirection.CLIENTBOUND, MessageDirection::isClientbound);
    }

    @Test
    void testIsServerbound() {
        this.testDirection(MessageDirection.SERVERBOUND, MessageDirection::isServerbound);
    }

    private void testDirection(MessageDirection direction, Predicate<MessageDirection> predicate) {
        assertTrue(predicate.test(direction));

        for (MessageDirection other : MessageDirection.values()) {
            if (other == direction) {
                continue;
            }

            assertFalse(predicate.test(other));
        }
    }

}
