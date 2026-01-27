package ir.tax.trustedtaxpayers.beans.model;

import java.util.UUID;

public class Buyer {
    private final UUID id;
    private final String buyerType;
    private final String nationalId;
    private final String name;
    private final String taxpayerNo;
    private final boolean active;

    public Buyer(UUID id, String buyerType, String nationalId, String name) {
        this(id, buyerType, nationalId, name, null, true);
    }

    public Buyer(UUID id, String buyerType, String nationalId, String name, String taxpayerNo, boolean active) {
        this.id = id;
        this.buyerType = buyerType;
        this.nationalId = nationalId;
        this.name = name;
        this.taxpayerNo = taxpayerNo;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getBuyerType() { return buyerType; }
    public String getNationalId() { return nationalId; }
    public String getName() { return name; }
    public String getTaxpayerNo() { return taxpayerNo; }
    public boolean isActive() { return active; }
}


