package com.mathbteixeira.edi_poc.support;

import com.mathbteixeira.edi_poc.application.EdiPurchaseOrderParser;
import com.mathbteixeira.edi_poc.domain.RetailOrderDomain;

public class EdiDemo {
    public static void main(String[] args) {
        // A standard (simplified) X12 850 payload
        String sampleX12 =
                "ISA*00* *00* *ZZ*RETAILER123    *ZZ*SUPPLIER999    *260309*1530*U*00401*000000001*0*P*>~" +
                        "GS*PO*RETAILER123*SUPPLIER999*20260309*1530*1*X*004010~" +
                        "ST*850*0001~" +
                        "BEG*00*SA*PO-987654**20260309~" +
                        "PO1*1*100*EA*12.50**UP*012345678905~" +
                        "PO1*2*50*EA*8.75**UP*098765432109~" +
                        "CTT*2~" +
                        "SE*7*0001~" +
                        "GE*1*1~" +
                        "IEA*1*000000001~";

        EdiPurchaseOrderParser parser = new EdiPurchaseOrderParser();

        try {
            RetailOrderDomain.PurchaseOrder order = parser.parseX12Order(sampleX12);
            System.out.println("Successfully parsed order from: " + order.partnerId());
            System.out.println("PO Number: " + order.poNumber());
            System.out.println("Date: " + order.orderDate());
            System.out.println("Items Ordered:");

            for (RetailOrderDomain.LineItem item : order.items()) {
                System.out.println(" - UPC: " + item.upcCode() + " | Qty: " + item.quantity() + " | Price: $" + item.price());
            }

        } catch (Exception e) {
            System.err.println("Failed to parse EDI: " + e.getMessage());
        }
    }
}