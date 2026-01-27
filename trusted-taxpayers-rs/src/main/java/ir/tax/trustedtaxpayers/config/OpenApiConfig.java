package ir.tax.trustedtaxpayers.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration("openApiCustomizerConfig")
@Profile("!test")
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer addStandardResponsesToAllOperations() {
        return new OpenApiCustomizer() {
            @Override
            public void customise(OpenAPI openApi) {
                if (openApi.getPaths() == null) {
                    return;
                }

                openApi.getPaths().values().forEach(pathItem -> {
                    // Apply to all HTTP operations if present
                    apply(pathItem.getGet());
                    apply(pathItem.getPost());
                    apply(pathItem.getPut());
                    apply(pathItem.getDelete());
                    apply(pathItem.getPatch());
                    apply(pathItem.getHead());
                    apply(pathItem.getOptions());
                    apply(pathItem.getTrace());
                });
            }

            private void apply(io.swagger.v3.oas.models.Operation operation) {
                if (operation == null) {
                    return;
                }
                ApiResponses responses = operation.getResponses();
                if (responses == null) {
                    responses = new ApiResponses();
                    operation.setResponses(responses);
                }

                // Only add if not already present on the operation
                addIfAbsent(responses, "401", "عدم احراز هویت");
                addIfAbsent(responses, "403", "عدم دسترسی");
                addIfAbsent(responses, "500", "خطای داخلی سرور");

                // Add simple JSON example for request body if missing
                if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
                    Content rbContent = operation.getRequestBody().getContent();
                    rbContent.forEach((mediaTypeKey, mediaType) -> addExampleIfJson(mediaTypeKey, mediaType, true));
                }

                // Add simple JSON examples for all responses if missing
                if (responses != null) {
                    responses.values().forEach(apiResp -> {
                        Content content = apiResp.getContent();
                        if (content != null) {
                            content.forEach((mediaTypeKey, mediaType) -> addExampleIfJson(mediaTypeKey, mediaType, false));
                        }
                    });
                }
            }

            private void addIfAbsent(ApiResponses responses, String code, String descriptionFa) {
                if (!responses.containsKey(code)) {
                    ApiResponse apiResponse = new ApiResponse()
                            .description(descriptionFa)
                            // Keep empty content to avoid schema name collisions across modules
                            .content(new Content());
                    responses.addApiResponse(code, apiResponse);
                }
            }

            private void addExampleIfJson(String mediaTypeKey, MediaType mediaType, boolean isRequest) {
                if (mediaType == null) {
                    return;
                }
                if (mediaTypeKey != null && mediaTypeKey.toLowerCase().contains("application/json")) {
                    if (mediaType.getExample() == null && (mediaType.getExamples() == null || mediaType.getExamples().isEmpty())) {
                        Object example = isRequest ?
                                java.util.Map.of(
                                        "sample", "test",
                                        "note", "نمونه بدنه درخواست"
                                ) :
                                java.util.Map.of(
                                        "status", "ok",
                                        "note", "نمونه پاسخ"
                                );
                        mediaType.setExample(example);
                    }
                }
            }
        };
    }
}


