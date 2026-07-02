package pe.edu.upc.medibridge.medicationmanagement.infrastructure.acl;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfiguration {

    @Bean
    public RequestInterceptor internalTokenRequestInterceptor(
            @Value("${services.internal.header-name:X-Internal-Token}") String headerName,
            @Value("${services.internal.token:${INTERNAL_SERVICE_TOKEN:local-internal-token}}") String internalToken) {
        return template -> template.header(headerName, internalToken);
    }
}
