package com.mathbteixeira.edi_poc.application;

import com.mathbteixeira.edi_poc.domain.RetailOrderDomain;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class EdiPurchaseOrderParser {

    // Standard X12 Delimiters
    private static final char SEGMENT_TERMINATOR = '~';
    private static final String ELEMENT_SEPARATOR = "\\*";

    public RetailOrderDomain.PurchaseOrder parseX12Order(String rawEdiPayload) throws Exception {
        String poNumber = null;
        String orderDate = null;
        String partnerId = null;
        List<RetailOrderDomain.LineItem> items = new ArrayList<>();

        // Use BufferedReader to handle potentially massive EDI files efficiently
        try (BufferedReader reader = new BufferedReader(new StringReader(rawEdiPayload))) {
            StringBuilder segmentBuilder = new StringBuilder();
            int character;

            while ((character = reader.read()) != -1) {
                char c = (char) character;

                if (c == SEGMENT_TERMINATOR) { // End of segment reached
                    String segment = segmentBuilder.toString().trim();
                    segmentBuilder.setLength(0); // Reset for the next segment

                    if (segment.isEmpty()) continue;

                    String[] elements = segment.split(ELEMENT_SEPARATOR);
                    String segmentId = elements[0];

                    // Route the logic based on the Segment ID
                    switch (segmentId) {
                        case "ISA":
                            // Interchange Control Header: Get the Sender ID
                            if (elements.length > 6) {
                                partnerId = elements[6].trim();
                            }
                            break;
                        case "BEG":
                            // Beginning Segment: Contains PO Number and Date
                            if (elements.length > 5) {
                                poNumber = elements[3];
                                orderDate = elements[5];
                            }
                            break;
                        case "PO1":
                            // Baseline Item Data: Quantity, Price, and UPC
                            if (elements.length > 7) {
                                int qty = Integer.parseInt(elements[2]);
                                double price = Double.parseDouble(elements[4]);
                                String upc = elements[7]; // Assuming UP is the qualifier in element 6
                                items.add(new RetailOrderDomain.LineItem(upc, qty, price));
                            }
                            break;
                    }
                } else {
                    // Ignore newlines (often added for human readability but invalid in strict EDI)
                    if (c != '\n' && c != '\r') {
                        segmentBuilder.append(c);
                    }
                }
            }
        }

        if (poNumber == null) {
            throw new IllegalArgumentException("Invalid EDI Document: Missing BEG segment (PO Number)");
        }

        return new RetailOrderDomain.PurchaseOrder(poNumber, orderDate, partnerId, items);
    }
}
