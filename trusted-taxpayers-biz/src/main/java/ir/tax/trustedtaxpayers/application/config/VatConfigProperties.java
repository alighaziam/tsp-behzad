package ir.tax.trustedtaxpayers.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vat")
public class VatConfigProperties {
    private Type1 type1 = new Type1();
    private Utilities utilities = new Utilities();

    public Type1 getType1() { return type1; }
    public Utilities getUtilities() { return utilities; }

    public static class Type1 {
        private int b2cRate = 9;
        public int getB2cRate() { return b2cRate; }
        public void setB2cRate(int b2cRate) { this.b2cRate = b2cRate; }
    }
    public static class Utilities {
        private int b2cRate = 6;
        public int getB2cRate() { return b2cRate; }
        public void setB2cRate(int b2cRate) { this.b2cRate = b2cRate; }
    }
}


