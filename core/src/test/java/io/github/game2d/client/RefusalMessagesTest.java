package io.github.game2d.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.game2d.protocol.RefusalReason;
import org.junit.jupiter.api.Test;

class RefusalMessagesTest {
    @Test
    void everyReasonHasAMessage() {
        for (RefusalReason reason : RefusalReason.values()) {
            assertFalse(RefusalMessages.describe(reason).isBlank(), reason.name());
        }
    }

    @Test
    void takenNameIsExplained() {
        assertEquals("Ce pseudo est déjà pris", RefusalMessages.describe(RefusalReason.NAME_TAKEN));
    }
}
