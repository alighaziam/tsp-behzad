package ir.tax.trustedtaxpayers.application.exception;

public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}


