package com.github.lkqm.spring.api.version.config;

import com.github.lkqm.spring.api.version.ApiVersion;
import com.github.lkqm.spring.api.version.ApiVersionProperties;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

@Slf4j
@AllArgsConstructor
public class VersionedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    /**
     * 多版本配置属性
     */
    private ApiVersionProperties apiVersionProperties;

    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        // 扫描类上的 @ApiVersion
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
        return createRequestCondition(apiVersion);
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        // 扫描方法上的 @ApiVersion
        ApiVersion apiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);
        return createRequestCondition(apiVersion);
    }

    private RequestCondition<ApiVersionRequestCondition> createRequestCondition(ApiVersion apiVersion) {
        if (apiVersion == null) return null;
        int value = apiVersion.value();
        Assert.isTrue(value >= 1, "Api Version Must be greater than or equal to 1");
        return new ApiVersionRequestCondition(value, apiVersionProperties);
    }

    //--------------------- 动态注册URI -----------------------//
    @Override
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = this.createRequestMappingInfo(method);
        if (info != null) {
            RequestMappingInfo typeInfo = this.createRequestMappingInfo(handlerType);
            if (typeInfo != null) {
                info = typeInfo.combine(info);
            }

            // 指定URL前缀
            if (apiVersionProperties.getType() == ApiVersionProperties.Type.URI) {
                ApiVersion apiVersion = AnnotationUtils.getAnnotation(method, ApiVersion.class);
                if (apiVersion == null) {
                    apiVersion = AnnotationUtils.getAnnotation(handlerType, ApiVersion.class);
                }
                if (apiVersion != null) {
                    String prefix = "/v" + apiVersion.value();
                    if (!StringUtils.isEmpty(apiVersionProperties.getUriPrefix())) {
                        prefix = apiVersionProperties.getUriPrefix().trim() + prefix;
                    }
                    info = RequestMappingInfo.paths(new String[]{prefix}).build().combine(info);
                }
            }
        }

        return info;
    }

    private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        RequestCondition<?> condition = element instanceof Class ? this.getCustomTypeCondition((Class) element) : this.getCustomMethodCondition((Method) element);
        return requestMapping != null ? this.createRequestMappingInfo(requestMapping, condition) : null;
    }


}