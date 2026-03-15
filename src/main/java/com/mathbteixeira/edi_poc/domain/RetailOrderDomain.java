package com.mathbteixeira.edi_poc.domain;

import java.util.List;

public class RetailOrderDomain {

    public record PurchaseOrder(
            String poNumber,
            String orderDate,
            String partnerId,
            List<LineItem> items
    ) {}

    public record LineItem(
            String upcCode,
            int quantity,
            double price
    ) {}
}