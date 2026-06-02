/*
 * Copyright (c) 2004-2022, University of Oslo
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * This class used to create ~50k API mappings for all versions of the App for each controller
 * method e.g. <br>
 * <code>/api/39/icons</code> <br>
 * <code>/api/39/icons/{key}</code> <br>
 * <code>/api/40/icons</code> <br>
 * <code>/api/40/icons/{key}</code> <br>
 * <br>
 * It now provides a mapping for any endpoint starting with `/api/`, allowing any version from 28 to
 * 43. It also still allows the endpoint without any version e.g. <br>
 * <code>/api/icons/{key}</code> <br>
 * which is what we want clients to use exclusively in the future.
 *
 * <p>It also reinstates the path-extension and trailing-slash matching that Spring Framework 7.0
 * removed from the handler mapping (the {@code setUseSuffixPatternMatch} / {@code
 * setUseTrailingSlashMatch} flags). For each request the canonical (literal) path is matched first
 * — so controllers that map an extension literally (e.g. {@code OpenApiController}'s {@code
 * /openapi/openapi.json}) keep working — and only if that has no handler do we fall back to
 * matching the path with a trailing slash and/or a registered media-type extension removed (e.g.
 * {@code /api/dataElements.json} → {@code /api/dataElements}), recording the resolved media type
 * for {@link SuffixMediaTypeContentNegotiationStrategy}.
 */
public class CustomRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
  private static final char EXTENSION_SEPARATOR = '.';

  private static final char PATH_SEPARATOR = '/';

  private final Map<String, MediaType> mediaTypes = WebMvcConfig.MEDIA_TYPE_MAP;

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
    // 1) Match the path as-is so literal-extension mappings (e.g. /openapi/openapi.json) win.
    HandlerMethod handlerMethod = super.getHandlerInternal(request);
    if (handlerMethod != null) {
      return handlerMethod;
    }
    // 2) Fall back to a normalised path (trailing slash and/or registered extension removed).
    HttpServletRequest normalized = normalize(request);
    return normalized != request ? super.getHandlerInternal(normalized) : null;
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
          mediaTypes.get(extension));
    }
    return new PathNormalizingRequestWrapper(request, extension, trailingSlash);
  }

  /**
   * Returns the registered extension (a key of {@link #mediaTypes}) at the end of the given URI's
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
    return mediaTypes.containsKey(extension) ? extension : null;
  }

  /** Wraps a request to expose the path with a trailing slash and/or {@code .extension} removed. */
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
