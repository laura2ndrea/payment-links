package com.laura.payment_links.service;

import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReferenceGenerator {
    private final AtomicInteger counter = new AtomicInteger(0);
    private final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    public String generateReference() {
        String year = LocalDate.now().format(yearFormatter);
        int sequence = counter.incrementAndGet();
        return String.format("PL-%s-%06d", year, sequence);
    }
}