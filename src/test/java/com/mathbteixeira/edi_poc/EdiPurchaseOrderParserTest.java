package com.mathbteixeira.edi_poc;

import com.mathbteixeira.edi_poc.model.RetailOrderDomain;
import com.mathbteixeira.edi_poc.service.EdiPurchaseOrderParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EdiPurchaseOrderParserTest {

    private final EdiPurchaseOrderParser parser = new EdiPurchaseOrderParser();

    @Test
    void shouldSuccessfullyParseValidX12String() throws Exception {
        // Arrange
        String rawEdi = "ISA*00* *00* *ZZ*RETAILER123    *ZZ*SUPPLIER999    *260309*1530*U*00401*000000001*0*P*>~" +
                "BEG*00*SA*PO-987654**20260309~" +
                "PO1*1*100*EA*12.50**UP*012345678905~";

        // Act
        RetailOrderDomain.PurchaseOrder result = parser.parseX12Order(rawEdi);

        // Assert
        assertNotNull(result);
        assertEquals("PO-987654", result.poNumber());
        assertEquals("20260309", result.orderDate());
        assertEquals("RETAILER123", result.partnerId());
        assertEquals(1, result.items().size());
        assertEquals("012345678905", result.items().get(0).upcCode());
        assertEquals(100, result.items().get(0).quantity());
        assertEquals(12.50, result.items().get(0).price());
    }

    @Test
    void shouldThrowExceptionWhenBegSegmentIsMissing() {
        // Arrange - An EDI string with no BEG segment
        String invalidEdi = "ISA*00* *00* *ZZ*RETAILER123    *ZZ*SUPPLIER999    *260309*1530*U*00401*000000001*0*P*>~" +
                "PO1*1*100*EA*12.50**UP*012345678905~";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            parser.parseX12Order(invalidEdi);
        });

        assertTrue(exception.getMessage().contains("Missing BEG segment"));
    }
}