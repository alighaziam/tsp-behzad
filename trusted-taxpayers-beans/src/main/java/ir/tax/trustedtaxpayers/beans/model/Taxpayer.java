package ir.tax.trustedtaxpayers.beans.model;

import java.util.UUID;

public class Taxpayer {
    private UUID id;
    private String taxpayerNo;
    private String name;

    public Taxpayer(UUID id, String taxpayerNo, String name) {
        this.id = id;
        this.taxpayerNo = taxpayerNo;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getTaxpayerNo() {
        return taxpayerNo;
    }

    public String getName() {
        return name;
    }
}


