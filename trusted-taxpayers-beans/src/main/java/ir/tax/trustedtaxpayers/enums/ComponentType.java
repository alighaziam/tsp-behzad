package ir.tax.trustedtaxpayers.enums;

import lombok.Getter;

@Getter
public enum ComponentType {

    TEXT_FIELD("TEXT_FIELD", "فیلد متنی"),
    COMBO_BOX("COMBO_BOX", "کمبوباکس"),
    DATE_PICKER("DATE_PICKER", "انتخاب تاریخ"),
    SELECT("SELECT", "لیست انتخابی");

    private final String code;
    private final String title;

    ComponentType(String code, String title) {
        this.code = code;
        this.title = title;
    }
}
