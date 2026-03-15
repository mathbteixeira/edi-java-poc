package com.mathbteixeira.edi_poc.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class Edi997GeneratorTest {

    private final Edi997Generator generator = new Edi997Generator();

    @Test
    void shouldPadPartnerIdToExactly15Characters() {
        String shortPartnerId = "SHORT";

        String result997 = generator.generateAcceptedAck(shortPartnerId);

        assertTrue(result997.contains("*ZZ*SHORT          *"),
                "The Partner ID must be padded to exactly 15 characters");
        assertTrue(result997.contains("AK9*A*1*1*1~"));
    }
}
