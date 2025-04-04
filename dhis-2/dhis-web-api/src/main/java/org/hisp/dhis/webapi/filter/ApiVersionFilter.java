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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Filters calls matching <code>/api/{version}/{endpoint}</code>, where version is a number between
 * 28-43 inclusive.<br>
 * When there is no match, the request gets processed as normal.<br>
 * When there is a match, it gets dispatched with the version part removed e.g. <code>
 * /api/42/icons</code> gets dispatched to <code>/api/icons</code><br>
 * Regex match examples: <br>
 * <code>/api/42/icons</code> -> match <br>
 * <code>/api/33/icons</code> -> match <br>
 * <code>/api/28/icons</code> -> match <br>
 * <br>
 * <code>/api/27/icons</code> -> no match <br>
 * <code>/api/44/icons</code> -> no match <br>
 * <code>/api/333/icons</code> -> no match <br>
 * <code>/api/test/icons</code> -> no match <br>
 * <code>/api/1/icons</code> -> no match <br>
 * <br>
 * This allows us to keep current behaviour of allowing clients to call any endpoint with a version
 * number without the need for any explicit mappings, as was the previous behaviour. This is
 * intended as a temporary measure, as we intend on removing support for calling endpoints with
 * version numbers in the future.
 *
 * @author david mackessy
 */
@Component
public class ApiVersionFilter implements Filter {

  private static final String API_VERSION_REGEX =
      "^(?<api>/api/)(?<apiversion>2[8-9]|3\\d|4[0-3])/(?<endpoint>.*)";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;

    Pattern pattern = Pattern.compile(API_VERSION_REGEX);
    Matcher matcher = pattern.matcher(req.getRequestURI());

    while (matcher.find()) {
      String api = matcher.group("api");
      String apiVersion = matcher.group("apiversion");
      String endpoint = matcher.group("endpoint");

      if (api != null && apiVersion != null && endpoint != null) {
        RequestDispatcher dispatcher = req.getRequestDispatcher(api + endpoint);
        dispatcher.forward(request, response);
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
