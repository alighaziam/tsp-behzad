package ir.tax.trustedtaxpayers.enums;

public enum FieldRequirementType {
    REQUIRED("REQUIRED", "اجباری"),
    CONDITIONALLY_REQUIRED("CONDITIONALLY_REQUIRED", "در شرایط خاص اجباری"),
    OPTIONAL("OPTIONAL", "اختیاری"),
    IGNORE("IGNORE", "نادیده گرفته شود");
    
    private final String code;
    private final String title;
    
    FieldRequirementType(String code, String title) {
        this.code = code;
        this.title = title;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getTitle() {
        return title;
    }
}

