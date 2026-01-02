package org.hisp.dhis.webapi.filter;

import java.io.IOException;
import java.util.regex.Pattern;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class OrganisationUnitCacheFilter extends OncePerRequestFilter {

    private static final Pattern PATH_PATTERN =
        Pattern.compile("^/api(?:/\\d+)?/organisationUnits/[^/]+(?:\\.[^/]+)?$");

    private final long maxAgeSeconds;

    public OrganisationUnitCacheFilter(
        @Value("${dhis.cache.organisationunit.max-age:3600}") long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        boolean shouldCache =
            "GET".equalsIgnoreCase(request.getMethod()) && PATH_PATTERN.matcher(path).matches();

        if (shouldCache) {
            applyCacheHeaders(response);
        }

        chain.doFilter(request, response);

        if (shouldCache && !response.isCommitted()) {
            applyCacheHeaders(response);
        }
    }

    private void applyCacheHeaders(HttpServletResponse response) {
        response.setHeader(
            "Cache-Control",
            "public, max-age=" + maxAgeSeconds + ", s-maxage=" + maxAgeSeconds);
        response.setHeader("Vary", "Accept-Encoding");
        response.setDateHeader("Expires", System.currentTimeMillis() + (maxAgeSeconds * 1000L));
    }
}
