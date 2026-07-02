package pe.edu.upc.medibridge.healthmonitoring.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalAccessFilter extends OncePerRequestFilter {

    private final String headerName;
    private final String internalToken;

    public InternalAccessFilter(
            @Value("${services.internal.header-name:X-Internal-Token}") String headerName,
            @Value("${services.internal.token:${INTERNAL_SERVICE_TOKEN:local-internal-token}}") String internalToken) {
        this.headerName = headerName;
        this.internalToken = internalToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String providedToken = request.getHeader(headerName);
        if (internalToken.equals(providedToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpStatus.FORBIDDEN.value(), "Direct access is not allowed. Use the API Gateway.");
    }
}
