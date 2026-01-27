package ir.tax.trustedtaxpayers.interfaces.rest.util;

import ir.tax.trustedtaxpayers.interfaces.rest.dto.CreatedIdResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ExistsResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.BaseApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public final class ResponseFactory {
    private ResponseFactory() {}

    public static ResponseEntity<CreatedIdResponse> created(UUID id) {
        String idString = EntityDtoMapper.uuidToString(id);
        return ResponseEntity.ok(new CreatedIdResponse(idString));
    }
    
    public static ResponseEntity<CreatedIdResponse> created(String idString) {
        return ResponseEntity.ok(new CreatedIdResponse(idString));
    }

    public static ResponseEntity<ExistsResponse> exists(boolean exists) {
        return ResponseEntity.ok(new ExistsResponse(exists));
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    public static <T> ResponseEntity<BaseApiResponse<T>> okEnvelope(T body) {
        return ResponseEntity.ok(BaseApiResponse.ok(body));
    }

    public static ResponseEntity<BaseApiResponse<Object>> errorEnvelope(String message, String traceId, int httpStatus) {
        var envelope = BaseApiResponse.error(message, traceId);
        return ResponseEntity.status(httpStatus).body(envelope);
    }
}


