/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.io.IOException;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Subclass of {@link org.springframework.web.filter.ShallowEtagHeaderFilter} which allows exclusion
 * of URIs matching a regex.
 *
 * <p>The regex is given as the init-param named 'excludeUriRegex' in the filter configuration.
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * <filter>
 *     <filter-name>ShallowEtagHeaderFilter</filter-name>
 *     <filter-class>org.hisp.dhis.webapi.filter.ExcludableShallowEtagHeaderFilter</filter-class>
 *     <init-param>
 *         <param-name>excludeUriRegex</param-name>
 *         <param-value>/api/dataValues|/api/dataValues/files</param-value>
 *     </init-param>
 * </filter>
 * }</pre>
 *
 * <p>The example exactly matches and excludes any request to the '/api/dataValues' and
 * '/api/dataValues/files' from the filter.
 *
 * @author Lars Helge Overland
 * @author Halvdan Hoem Grelland
 */
@Slf4j
@WebFilter(
    urlPatterns = {"/api/*"},
    initParams = {
      @WebInitParam(name = "excludeUriRegex", value = ExcludableShallowEtagHeaderFilter.ENDPOINTS)
    })
public class ExcludableShallowEtagHeaderFilter extends ShallowEtagHeaderFilter {
  private static final String EXCLUDE_URI_REGEX_VAR_NAME = "excludeUriRegex";

  static final String ENDPOINTS =
      "/api/(\\d{2}/)?dataValueSets|"
          + "/api/(\\d{2}/)?dataValues|"
          + "/api/(\\d{2}/)?fileResources|"
          + "/api/(\\d{2}/)?dataEntry/metadata";

  private Pattern pattern = null;

  @Override
  protected void initFilterBean() throws ServletException {
    FilterConfig filterConfig = getFilterConfig();

    String excludeRegex =
        filterConfig != null ? filterConfig.getInitParameter(EXCLUDE_URI_REGEX_VAR_NAME) : null;

    Assert.notNull(
        excludeRegex,
        String.format(
            excludeRegex,
            "Parameter '%s' must be specified for ExcludableShallowEtagHeaderFilter",
            EXCLUDE_URI_REGEX_VAR_NAME));

    pattern = Pattern.compile(excludeRegex);

    log.debug(
        String.format(
            "ExcludableShallowEtagHeaderFilter initialized with %s: '%s'",
            EXCLUDE_URI_REGEX_VAR_NAME, excludeRegex));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String uri = request.getRequestURI();

    boolean match = pattern.matcher(uri).find();

    if (match) {
      // Proceed without invoking this filter
      filterChain.doFilter(request, response);
    } else {
      // Invoke this filter
      super.doFilterInternal(request, response, filterChain);
    }
  }
}
