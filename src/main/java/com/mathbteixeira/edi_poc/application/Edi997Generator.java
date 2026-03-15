package com.mathbteixeira.edi_poc.application;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class Edi997Generator {

    public String generateAcceptedAck(String partnerId) {
        // In a 997, the sender and receiver are flipped.
        // We received the 850, so we are the sender of the 997.
        String ourCompanyId = "SUPPLIER999";

        // EDI X12 requires these fields to be exactly 15 characters, padded with spaces
        String formattedPartnerId = String.format("%-15s", partnerId.trim());
        String formattedOurId = String.format("%-15s", ourCompanyId);

        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortDate = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String time = now.format(DateTimeFormatter.ofPattern("HHmm"));

        // Build the raw X12 997 String
        // AK1 acknowledges the PO functional group
        // AK9*A indicates complete Acceptance of the transaction
        return String.format(
                "ISA*00* *00* *ZZ*%s*ZZ*%s*%s*%s*U*00401*000000002*0*P*>~" +
                        "GS*FA*%s*%s*%s*%s*2*X*004010~" +
                        "ST*997*0001~" +
                        "AK1*PO*1~" +
                        "AK9*A*1*1*1~" +
                        "SE*4*0001~" +
                        "GE*1*2~" +
                        "IEA*1*000000002~",
                formattedOurId, formattedPartnerId, shortDate, time,
                ourCompanyId, partnerId.trim(), date, time
        );
    }
}