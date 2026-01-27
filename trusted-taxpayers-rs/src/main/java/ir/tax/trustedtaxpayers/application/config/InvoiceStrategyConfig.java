package ir.tax.trustedtaxpayers.application.config;

import ir.tax.trustedtaxpayers.application.service.strategy.InvoiceCalculationStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.DefaultInvoiceCalculationStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type1SalesStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type2FxStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type3GoldStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type4ContractStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type5UtilityStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type6AirStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type7ExportStrategy;
import ir.tax.trustedtaxpayers.application.service.strategy.impl.Type8to16TemplateStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InvoiceStrategyConfig {

    @Bean
    public InvoiceCalculationStrategy defaultInvoiceCalculationStrategy() {
        return new DefaultInvoiceCalculationStrategy();
    }

    @Bean
    public InvoiceCalculationStrategy type1SalesStrategy(VatConfigProperties vatConfigProperties) { return new Type1SalesStrategy(vatConfigProperties); }

    @Bean
    public InvoiceCalculationStrategy type2FxStrategy() { return new Type2FxStrategy(); }

    @Bean
    public InvoiceCalculationStrategy type3GoldStrategy() { return new Type3GoldStrategy(); }

    @Bean
    public InvoiceCalculationStrategy type4ContractStrategy() { return new Type4ContractStrategy(); }

    @Bean
    public InvoiceCalculationStrategy type5UtilityStrategy(VatConfigProperties vatConfigProperties) { return new Type5UtilityStrategy(vatConfigProperties); }

    @Bean
    public InvoiceCalculationStrategy type6AirStrategy() { return new Type6AirStrategy(); }

    @Bean
    public InvoiceCalculationStrategy type7ExportStrategy() { return new Type7ExportStrategy(); }

    @Bean
    public InvoiceCalculationStrategy type8to16TemplateStrategy() { return new Type8to16TemplateStrategy(); }
}



