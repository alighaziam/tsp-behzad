package ir.tax.trustedtaxpayers.interfaces.rest.config;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import ir.tax.trustedtaxpayers.core.util.UuidStringConverter;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * Argument Resolver برای تبدیل String (encoded UUID) به UUID در @PathVariable و @RequestParam
 * این resolver به Spring اجازه می‌دهد که encoded UUID strings را به UUID تبدیل کند
 */
@Component
public class UuidArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UUID.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {
        
        String paramName = parameter.getParameterName();
        if (paramName == null) {
            return null;
        }
        
        // برای @PathVariable از path variable استفاده می‌کنیم
        org.springframework.web.bind.annotation.PathVariable pathVar = 
            parameter.getParameterAnnotation(org.springframework.web.bind.annotation.PathVariable.class);
        
        String value;
        if (pathVar != null) {
            // @PathVariable - از URI path variable استفاده می‌کنیم
            String pathVarName = pathVar.value().isEmpty() ? paramName : pathVar.value();
            value = (String) webRequest.getAttribute(pathVarName, NativeWebRequest.SCOPE_REQUEST);
            if (value == null) {
                // اگر در attribute نبود، از path variable map استفاده می‌کنیم
                java.util.Map<String, String> pathVariables = 
                    (java.util.Map<String, String>) webRequest.getAttribute(
                        org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                        NativeWebRequest.SCOPE_REQUEST);
                if (pathVariables != null) {
                    value = pathVariables.get(pathVarName);
                }
            }
        } else {
            // @RequestParam - از query parameter استفاده می‌کنیم
            value = webRequest.getParameter(paramName);
        }
        
        if (value == null || value.isBlank()) {
            return null;
        }
        
        // ابتدا سعی می‌کنیم به صورت encoded UUID decode کنیم
        try {
            return UuidStringConverter.decode(value);
        } catch (IllegalArgumentException e) {
            // اگر decode نشد، سعی می‌کنیم به صورت استاندارد UUID parse کنیم
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                    "Invalid UUID format: " + value + ". Expected encoded UUID or standard UUID format.", ex);
            }
        }
    }
}

