package ir.tax.trustedtaxpayers.interfaces.rest.advice;

import ir.tax.trustedtaxpayers.interfaces.rest.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "ir.tax.trustedtaxpayers")
public class ApiResponseEnvelopeAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) { return true; }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            ApiResponse<Object> resp = ApiResponse.success(null);
            String traceId = request.getHeaders().getFirst("X-Correlation-Id");
            if (traceId != null && !traceId.isBlank()) resp.setTraceId(traceId);
            return resp;
        }
        if (body instanceof ApiResponse<?>) return body;
        if (body instanceof ResponseEntity<?> re) {
            Object inner = re.getBody();
            if (inner instanceof ApiResponse<?>) return re;
            // اگر پاسخ باینری/فایل/غیر JSON است، wrap نکن
            MediaType ct = re.getHeaders().getContentType();
            if (ct != null && !MediaType.APPLICATION_JSON.includes(ct)) {
                return re;
            }
            if (inner instanceof byte[] || inner instanceof org.springframework.core.io.Resource ||
                    inner instanceof org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody) {
                return re;
            }
            boolean errorStatus = re.getStatusCode().isError();
            ApiResponse<Object> resp = buildEnvelope(inner, errorStatus);
            applyTraceId(resp, request);
            return ResponseEntity.status(re.getStatusCode()).headers(re.getHeaders()).body(resp);
        }
        // اگر content-type غیر JSON است، wrap نکن
        if (selectedContentType != null && !MediaType.APPLICATION_JSON.includes(selectedContentType)) {
            return body;
        }
        if (body instanceof byte[] || body instanceof org.springframework.core.io.Resource ||
                body instanceof org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody) {
            return body;
        }
        ApiResponse<Object> resp = buildEnvelope(body, false);
        applyTraceId(resp, request);
        return resp;
    }

    private ApiResponse<Object> buildEnvelope(Object body, boolean forceError) {
        boolean isErrorPayload = forceError || body instanceof ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
        ApiResponse<Object> resp;
        if (isErrorPayload) {
            resp = new ApiResponse<>();
            resp.setSuccess(false);
            if (body instanceof ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse error) {
                resp.setMessage(error.getMessage());
            } else {
                resp.setMessage("Error");
            }
            resp.setData(body);
        } else {
            resp = ApiResponse.success(body);
        }
        return resp;
    }

    private void applyTraceId(ApiResponse<Object> resp, ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst("X-Correlation-Id");
        if (traceId != null && !traceId.isBlank()) {
            resp.setTraceId(traceId);
        }
    }
}


