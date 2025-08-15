package com.laura.payment_links.job;

import com.laura.payment_links.service.PaymentLinkService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExpirationJob {

    private final PaymentLinkService paymentLinkService;

    public ExpirationJob(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    // Cada minuto
    @Scheduled(cron = "0 * * * * *") // segundo 0 de cada minuto
    public void runExpiration() {
        paymentLinkService.expirePaymentLinks();
    }
}