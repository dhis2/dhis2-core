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
package org.hisp.dhis.webapi.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.hisp.dhis.webapi.security.config.WebMvcConfig;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reinstates DHIS2's path-extension content negotiation (e.g. {@code /api/resource.json}) after
 * Spring Framework 7.0 removed all framework support for it (suffix pattern matching on the handler
 * mapping, the {@code PathExtensionContentNegotiationStrategy} and {@code favorPathExtension}).
 *
 * <p>For a request whose path ends in a <em>registered</em> extension (see {@link
 * WebMvcConfig#MEDIA_TYPE_MAP} — {@code json}, {@code xml}, {@code adx.xml.gz}, …) this filter:
 *
 * <ol>
 *   <li>resolves the extension to a {@link MediaType} and stores it as the {@link
 *       #SUFFIX_MEDIA_TYPE_ATTRIBUTE} request attribute, which {@code
 *       SuffixMediaTypeContentNegotiationStrategy} reads back during content negotiation; and
 *   <li>strips the {@code .extension} suffix from the request path so that the (suffix-free)
 *       handler mappings still match under the {@code PathPattern} matcher.
 * </ol>
 *
 * Only registered extensions are acted upon, mirroring the previous {@code
 * setUseRegisteredSuffixPatternMatch(true)} behaviour; all other requests pass through unchanged.
 */
public class MediaTypeSuffixFilter extends OncePerRequestFilter {
  /** Request attribute holding the {@link MediaType} resolved from the URL path extension. */
  public static final String SUFFIX_MEDIA_TYPE_ATTRIBUTE =
      MediaTypeSuffixFilter.class.getName() + ".SUFFIX_MEDIA_TYPE";

  private static final char EXTENSION_SEPARATOR = '.';

  private static final char PATH_SEPARATOR = '/';

  private final Map<String, MediaType> mediaTypes;

  public MediaTypeSuffixFilter() {
    this(WebMvcConfig.MEDIA_TYPE_MAP);
  }

  public MediaTypeSuffixFilter(Map<String, MediaType> mediaTypes) {
    this.mediaTypes = mediaTypes;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();

    // Spring 7.0 removed setUseTrailingSlashMatch; normalise a single trailing slash here so
    // e.g. POST /api/categoryOptions/ still matches the /api/categoryOptions mapping.
    boolean trailingSlash = uri != null && uri.length() > 1 && uri.endsWith("/");
    String pathForExtension = trailingSlash ? uri.substring(0, uri.length() - 1) : uri;
    String extension = getRegisteredExtension(pathForExtension);

    if (!trailingSlash && extension == null) {
      filterChain.doFilter(request, response);
      return;
    }

    if (extension != null) {
      request.setAttribute(SUFFIX_MEDIA_TYPE_ATTRIBUTE, mediaTypes.get(extension));
    }
    filterChain.doFilter(
        new PathNormalizingRequestWrapper(request, extension, trailingSlash), response);
  }

  /**
   * Returns the registered extension (a key of {@link #mediaTypes}) at the end of the given URI's
   * last path segment, or {@code null} if none. Matches the multi-dot logic of the legacy {@code
   * CustomPathExtensionContentNegotiationStrategy} (the extension is everything after the first dot
   * in the last path segment, e.g. {@code adx.xml.gz}).
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

  /**
   * Wraps a request to expose the path with an optional trailing slash and/or registered {@code
   * .extension} suffix removed, so handler mapping matches the canonical mapping.
   */
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

    @Override
    public StringBuffer getRequestURL() {
      String stripped = strip(super.getRequestURI());
      StringBuffer url = super.getRequestURL();
      // Re-derive the URL from the (possibly normalised) URI suffix portion.
      String original = super.getRequestURI();
      if (stripped != null && original != null && !stripped.equals(original)) {
        url.setLength(url.length() - (original.length() - stripped.length()));
      }
      return url;
    }
  }
}
