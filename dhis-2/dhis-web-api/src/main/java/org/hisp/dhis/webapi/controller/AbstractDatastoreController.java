/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import lombok.Value;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.datastore.DatastoreFields;
import org.hisp.dhis.datastore.DatastoreQuery;
import org.hisp.dhis.datastore.DatastoreQuery.Field;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.webapi.JsonWriter;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDatastoreController {

  @Autowired private ObjectMapper jsonMapper;

  @Value
  @OpenApi.Shared(value = false)
  static class Pager {
    int page;
    int pageSize;
  }

  @Value
  static class EntriesResponse {

    Pager pager;
    List<Map<String, JsonNode>> entries;
  }

  @FunctionalInterface
  interface DatastoreQueryExecutor {
    boolean getEntries(DatastoreQuery query, Predicate<Stream<DatastoreFields>> transform)
        throws BadRequestException;
  }

  void writeEntries(
      HttpServletResponse response, DatastoreQuery query, DatastoreQueryExecutor runQuery)
      throws IOException, BadRequestException {
    response.setContentType(APPLICATION_JSON_VALUE);
    setNoStore(response);

    try (PrintWriter writer = response.getWriter();
        JsonWriter out = new JsonWriter(writer)) {
      try {
        List<String> members = query.getFields().stream().map(Field::getAlias).collect(toList());
        runQuery.getEntries(
            query,
            entries -> {
              if (!query.isHeadless()) {
                writer.write("{\"pager\":{");
                writer.write("\"page\":" + query.getPage() + ",");
                writer.write("\"pageSize\":" + query.getPageSize());
                writer.write("},\"entries\":");
              }
              out.writeEntries(members, entries);
              return true;
            });
        if (!query.isHeadless()) {
          writer.write("}");
        }
      } catch (RuntimeException ex) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        Throwable cause = ex.getCause();
        String msg =
            "Unknown error when running the query: "
                + (cause != null && ex.getMessage().contains("could not extract ResultSet")
                    ? cause.getMessage()
                    : ex.getMessage());
        if (cause != null
            && cause.getMessage().contains("cannot cast type ")
            && cause.getMessage().contains(" to double precision")) {
          String sortProperty = query.getOrder().getPath();
          msg =
              "Cannot use numeric sort order on property `"
                  + sortProperty
                  + "` as the property contains non-numeric values for matching entries. Use "
                  + query.getOrder().getDirection().name().substring(1)
                  + " instead or apply a filter that only matches entries with numeric values for "
                  + sortProperty
                  + ".";
        }
        jsonMapper.writeValue(writer, WebMessageUtils.badRequest(msg));
      }
    }
  }
}
