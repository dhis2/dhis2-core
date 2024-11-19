/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.webapi.utils.HttpServletRequestPaths.getServletPath;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.webapi.controller.tracker.view.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FieldFilterRequestHandler {

  private final FieldFilterService fieldFilterService;

  /**
   * Returns a page which will serialize the items under given {@code jsonKey} after applying the
   * {@code fields} filter. Previous and next page links will be generated based on the request if
   * {@link org.hisp.dhis.tracker.export.Page#getPrevPage()} or next are not null.
   */
  public <T> Page<ObjectNode> handle(
      HttpServletRequest request,
      String jsonKey,
      org.hisp.dhis.tracker.export.Page<T> page,
      FieldsRequestParam fieldsParam) {
    List<ObjectNode> objectNodes =
        fieldFilterService.toObjectNodes(page.getItems(), fieldsParam.getFields());

    String requestURL = getRequestURL(request);
    return Page.withPager(jsonKey, page.withItems(objectNodes), requestURL);
  }

  public static String getRequestURL(HttpServletRequest request) {
    StringBuilder requestURL = new StringBuilder(getServletPath(request));
    requestURL.append(request.getPathInfo());
    String queryString = request.getQueryString();
    if (queryString == null) {
      return requestURL.toString();
    }

    return requestURL.append('?').append(queryString).toString();
  }
}
