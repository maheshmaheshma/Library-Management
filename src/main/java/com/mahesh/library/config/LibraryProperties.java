package com.mahesh.library.config;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="library")
public record LibraryProperties(int loanPeriodDays, BigDecimal finePerDay) {}