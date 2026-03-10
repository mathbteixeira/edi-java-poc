package com.mathbteixeira.edi_poc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Edi997GeneratorTest {

    private final Edi997Generator generator = new Edi997Generator();

    @Test
    void shouldPadPartnerIdToExactly15Characters() {
        // Arrange: A short partner ID (5 characters)
        String shortPartnerId = "SHORT";

        // Act
        String result997 = generator.generateAcceptedAck(shortPartnerId);

        // Assert: It should contain the padded ID (SHORT + 10 spaces)
        // Notice the exact spacing between the asterisks
        assertTrue(result997.contains("*ZZ*SHORT          *"),
                "The Partner ID must be padded to exactly 15 characters");

        // Assert: It should contain the acceptance segment
        assertTrue(result997.contains("AK9*A*1*1*1~"));
    }
}
