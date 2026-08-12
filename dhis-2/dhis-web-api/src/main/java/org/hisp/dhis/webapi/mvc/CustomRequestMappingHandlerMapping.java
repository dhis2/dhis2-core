/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.webapi.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.lang.reflect.Method;
import java.util.Map;
import org.hisp.dhis.webapi.security.config.WebMvcConfig;
import org.hisp.dhis.webapi.view.SuffixMediaTypeContentNegotiationStrategy;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Request mapping for DHIS2 controllers.
 *
 * <p>Historically this class expanded versioned API mappings. It still defaults method-less
 * mappings to GET.
 *
 * <p>It also reinstates path-extension and trailing-slash matching without relying on Spring's
 * removed/deprecated handler-mapping flags ({@code setUseSuffixPatternMatch} / {@code
 * setUseTrailingSlashMatch} / {@code favorPathExtension}). For each request that ends with a
 * registered media-type extension (or trailing slash), the suffix-stripped path is matched first so
 * content negotiation works and generic {@code /{property}} handlers do not swallow names like
 * {@code metadata.json}. If the stripped lookup finds no handler, or finds one whose {@code
 * produces} cannot satisfy the extension-derived media type ({@code 406}), we fall back in order:
 *
 * <ol>
 *   <li>slash-only strip when both a trailing slash and a registered extension were present (so
 *       literal maps like {@code /api/analytics.xlsx} still match {@code /api/analytics.xlsx/})
 *   <li>the original path (literal-extension controllers such as OpenAPI's {@code
 *       /openapi/openapi.json})
 * </ol>
 *
 * Rethrow the 406 only when no fallback mapping exists. The resolved media type is recorded for
 * {@link SuffixMediaTypeContentNegotiationStrategy}.
 *
 * <p>Forward-compatible on Spring 6.2 (Spring 7 readiness PR-F).
 */
public class CustomRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
  private static final char EXTENSION_SEPARATOR = '.';

  private static final char PATH_SEPARATOR = '/';

  private static final Map<String, MediaType> MEDIA_TYPES = WebMvcConfig.MEDIA_TYPE_MAP;

  @Override
  protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

    if (info == null) {
      return null;
    }

    // Default a method-less mapping to GET (legacy DHIS2 behaviour).
    RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();

    if (methodsCondition.getMethods().isEmpty()) {
      return info.mutate().methods(RequestMethod.GET).build();
    }

    return info;
  }

  @Override
  protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
    // When the path ends with a registered media-type extension (e.g. .json / .json.zip), prefer
    // matching the suffix-stripped path so content negotiation works and generic /{property}
    // handlers do not swallow "metadata.json" as a property name. If that fails (literal-extension
    // controller mappings such as /openapi/openapi.json), or the stripped path matches a handler
    // whose produces cannot satisfy the extension-derived media type (406 - common for analytics
    // download endpoints that map .xml/.csv/... literally next to a JSON produces handler), fall
    // back toward the original path. When both a trailing slash and an extension were present,
    // try keeping the extension first (slash-only strip) before the raw original URI.
    //
    // Paths without a registered extension: match as-is, then fall back to trailing-slash strip.
    HttpServletRequest normalized = normalize(request);
    if (normalized == request) {
      return super.getHandlerInternal(request);
    }

    HttpMediaTypeNotAcceptableException notAcceptable = null;

    clearAndReparsePathCaches(normalized);
    try {
      HandlerMethod stripped = super.getHandlerInternal(normalized);
      if (stripped != null) {
        return stripped;
      }
    } catch (HttpMediaTypeNotAcceptableException ex) {
      // Stripped path matched a handler, but its produces does not include the media type forced
      // by the path extension. Prefer a literal-suffix mapping when one exists; otherwise rethrow
      // so the client still gets 406 rather than a misleading 404.
      notAcceptable = ex;
    }

    // /api/analytics.xlsx/ fully strips to /api/analytics (JSON produces → 406). Retry with only
    // the trailing slash removed so the literal /api/analytics.xlsx mapping can still win.
    HttpServletRequest slashOnly = trailingSlashOnly(request);
    if (slashOnly != null) {
      clearAndReparsePathCaches(slashOnly);
      try {
        HandlerMethod literalWithExtension = super.getHandlerInternal(slashOnly);
        if (literalWithExtension != null) {
          return literalWithExtension;
        }
      } catch (HttpMediaTypeNotAcceptableException ex) {
        if (notAcceptable == null) {
          notAcceptable = ex;
        }
      }
    }

    clearAndReparsePathCaches(request);
    HandlerMethod original = super.getHandlerInternal(request);
    if (original != null) {
      return original;
    }
    if (notAcceptable != null) {
      throw notAcceptable;
    }
    return null;
  }

  /**
   * When the original URI ends with a registered media-type extension <em>and</em> a trailing
   * slash, returns a request with only the trailing slash removed (extension kept). Otherwise
   * {@code null}.
   */
  private HttpServletRequest trailingSlashOnly(HttpServletRequest request) {
    String uri = request.getRequestURI();
    if (uri == null || uri.length() <= 1 || uri.charAt(uri.length() - 1) != PATH_SEPARATOR) {
      return null;
    }
    String withoutSlash = uri.substring(0, uri.length() - 1);
    if (getRegisteredExtension(withoutSlash) == null) {
      return null;
    }
    return new PathNormalizingRequestWrapper(request, null, true);
  }

  private static void clearAndReparsePathCaches(HttpServletRequest request) {
    request.removeAttribute(HandlerMapping.LOOKUP_PATH);
    request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);
    ServletRequestPathUtils.clearParsedRequestPath(request);
    // Re-parse from the (possibly wrapped) request so later filters/handlers that require a
    // parsed RequestPath still succeed. Clearing alone causes BAD_REQUEST:
    // "Expected parsed RequestPath in request attribute ...PATH".
    ServletRequestPathUtils.parseAndCache(request);
  }

  /**
   * Returns a request exposing the path with a single trailing slash and/or a trailing registered
   * media-type extension removed (recording the extension's media type for content negotiation), or
   * the original request when there is nothing to normalise.
   */
  private HttpServletRequest normalize(HttpServletRequest request) {
    String uri = request.getRequestURI();
    boolean trailingSlash = uri != null && uri.length() > 1 && uri.endsWith("/");
    String pathForExtension = trailingSlash ? uri.substring(0, uri.length() - 1) : uri;
    String extension = getRegisteredExtension(pathForExtension);

    if (!trailingSlash && extension == null) {
      return request;
    }
    if (extension != null) {
      request.setAttribute(
          SuffixMediaTypeContentNegotiationStrategy.SUFFIX_MEDIA_TYPE_ATTRIBUTE,
          MEDIA_TYPES.get(extension));
    }
    return new PathNormalizingRequestWrapper(request, extension, trailingSlash);
  }

  /**
   * Returns the registered extension (a key of {@link #MEDIA_TYPES}) at the end of the given URI's
   * last path segment (everything after the first dot, e.g. {@code adx.xml.gz}), or {@code null}.
   */
  private String getRegisteredExtension(String uri) {
    if (uri == null) {
      return null;
    }
    int lastSeparator = uri.lastIndexOf(PATH_SEPARATOR);
    int dotIndex = uri.indexOf(EXTENSION_SEPARATOR, lastSeparator + 1);
    if (dotIndex == -1) {
      return null;
    }
    String extension = uri.substring(dotIndex + 1);
    return MEDIA_TYPES.containsKey(extension) ? extension : null;
  }

  /** Wraps a request to expose the path with a trailing slash and/or extension removed. */
  private static final class PathNormalizingRequestWrapper extends HttpServletRequestWrapper {
    private final String suffix;
    private final boolean trailingSlash;

    PathNormalizingRequestWrapper(
        HttpServletRequest request, String extension, boolean trailingSlash) {
      super(request);
      this.suffix = extension != null ? EXTENSION_SEPARATOR + extension : null;
      this.trailingSlash = trailingSlash;
    }

    private String strip(String path) {
      if (path == null) {
        return null;
      }
      if (trailingSlash && path.length() > 1 && path.charAt(path.length() - 1) == PATH_SEPARATOR) {
        path = path.substring(0, path.length() - 1);
      }
      if (suffix != null && path.endsWith(suffix)) {
        path = path.substring(0, path.length() - suffix.length());
      }
      return path;
    }

    @Override
    public String getRequestURI() {
      return strip(super.getRequestURI());
    }

    @Override
    public String getServletPath() {
      return strip(super.getServletPath());
    }

    @Override
    public String getPathInfo() {
      return strip(super.getPathInfo());
    }
  }
}
