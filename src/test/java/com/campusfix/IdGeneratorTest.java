package com.campusfix;

import com.campusfix.exception.ValidationException;
import com.campusfix.util.IdGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdGeneratorTest {

    @Test
    void formatsWithPrefixAndPadding() {
        assertEquals("CF-000012", IdGenerator.formatComplaintId(12));
        assertEquals("CF-123456", IdGenerator.formatComplaintId(123456));
    }

    @Test
    void parsesAllAcceptedForms() {
        assertEquals(12, IdGenerator.parseComplaintId("CF-000012"));
        assertEquals(12, IdGenerator.parseComplaintId("cf-12"));
        assertEquals(12, IdGenerator.parseComplaintId(" 12 "));
    }

    @Test
    void rejectsInvalidIds() {
        assertThrows(ValidationException.class, () -> IdGenerator.parseComplaintId(null));
        assertThrows(ValidationException.class, () -> IdGenerator.parseComplaintId(""));
        assertThrows(ValidationException.class, () -> IdGenerator.parseComplaintId("abc"));
        assertThrows(ValidationException.class, () -> IdGenerator.parseComplaintId("-5"));
        assertThrows(ValidationException.class, () -> IdGenerator.parseComplaintId("0"));
        assertThrows(ValidationException.class, () -> IdGenerator.parseComplaintId("1; DROP TABLE users"));
    }
}
