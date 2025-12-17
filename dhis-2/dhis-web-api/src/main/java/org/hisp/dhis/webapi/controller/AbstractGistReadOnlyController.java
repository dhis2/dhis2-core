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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.webapi.utils.ContextUtils.getRequestURL;
import static org.hisp.dhis.webapi.utils.ContextUtils.getRootPath;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;
import lombok.Value;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.PropertyNames;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.gist.GistObject;
import org.hisp.dhis.gist.GistObjectList;
import org.hisp.dhis.gist.GistObjectListParams;
import org.hisp.dhis.gist.GistObjectParams;
import org.hisp.dhis.gist.GistObjectProperty;
import org.hisp.dhis.gist.GistObjectPropertyParams;
import org.hisp.dhis.gist.GistPager;
import org.hisp.dhis.gist.GistPipeline;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.jsontree.JsonValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Base controller for APIs that only want to offer read-only access though Gist API.
 *
 * @author Jan Bernitt
 */
@Maturity.Stable
@OpenApi.EntityType(OpenApi.EntityType.class)
@OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
public abstract class AbstractGistReadOnlyController<T extends PrimaryKeyObject> {

  @Autowired private GistPipeline gistPipeline;

  @OpenApi.Response(value = OpenApi.EntityType.class)
  @GetMapping(value = "/{uid}/gist", produces = "application/json")
  public void getObjectGist(
      @OpenApi.Param(UID.class) @PathVariable("uid") UID uid,
      GistObjectParams params,
      HttpServletResponse response)
      throws NotFoundException, BadRequestException {
    GistObject.Input input = new GistObject.Input(getEntityClass(), uid, params);
    gistPipeline.exportAsJson(input, lazyOutputStream("application/json", response));
  }

  @OpenApi.Response(String.class)
  @GetMapping(
      value = {"/{uid}/gist", "/{uid}/gist.csv"},
      produces = "text/csv")
  public void getObjectGistAsCsv(
      @OpenApi.Param(UID.class) @PathVariable("uid") UID uid,
      GistObjectParams params,
      HttpServletResponse response)
      throws BadRequestException, NotFoundException {
    GistObject.Input input = new GistObject.Input(getEntityClass(), uid, params);
    gistPipeline.exportAsCsv(input, lazyOutputStream("text/csv", response));
  }

  @Value
  @OpenApi.Shared(value = false)
  private static class GistListResponse {
    @OpenApi.Property GistPager pager;

    @OpenApi.Property(name = "path$", value = OpenApi.EntityType[].class)
    JsonObject[] entries = null;
  }

  @OpenApi.Response({GistListResponse.class, OpenApi.EntityType[].class})
  @GetMapping(value = "/gist", produces = "application/json")
  public void getObjectListGist(
      GistObjectListParams params, HttpServletRequest request, HttpServletResponse response)
      throws BadRequestException {
    GistObjectList.Input input =
        new GistObjectList.Input(
            getEntityClass(), getRootPath(request), getRequestURL(request), params);
    gistPipeline.exportAsJson(input, lazyOutputStream("application/json", response));
  }

  @OpenApi.Response(value = String.class)
  @GetMapping(
      value = {"/gist", "/gist.csv"},
      produces = "text/csv")
  public void getObjectListGistAsCsv(
      GistObjectListParams params, HttpServletRequest request, HttpServletResponse response)
      throws BadRequestException {
    GistObjectList.Input input =
        new GistObjectList.Input(
            getEntityClass(), getRootPath(request), getRequestURL(request), params);
    gistPipeline.exportAsCsv(input, lazyOutputStream("text/csv", response));
  }

  @OpenApi.Response(JsonValue.class)
  @GetMapping(value = "/{uid}/{property}/gist", produces = "application/json")
  public void getObjectPropertyGist(
      @OpenApi.Param(UID.class) @PathVariable("uid") UID uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      GistObjectPropertyParams params,
      HttpServletResponse response)
      throws BadRequestException, NotFoundException {
    GistObjectProperty.Input input =
        new GistObjectProperty.Input(getEntityClass(), uid, property, params);
    gistPipeline.exportPropertyAsJson(input, lazyOutputStream("application/json", response));
  }

  @OpenApi.Response(String.class)
  @GetMapping(
      value = {"/{uid}/{property}/gist", "/{uid}/{property}/gist.csv"},
      produces = "text/csv")
  public void getObjectPropertyGistAsCsv(
      @OpenApi.Param(UID.class) @PathVariable("uid") UID uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      GistObjectPropertyParams params,
      HttpServletResponse response)
      throws BadRequestException, NotFoundException {
    GistObjectProperty.Input input =
        new GistObjectProperty.Input(getEntityClass(), uid, property, params);
    gistPipeline.exportPropertyAsCsv(input, lazyOutputStream("text/csv", response));
  }

  /**
   * Defer the output changes after validation by wrapping it so the called code can do this as late
   * as possible
   */
  private static Supplier<OutputStream> lazyOutputStream(
      String contentType, HttpServletResponse response) {
    return () -> {
      response.setContentType(contentType);
      setNoStore(response);
      try {
        return response.getOutputStream();
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    };
  }

  private Class<T> entityClass;

  @SuppressWarnings("unchecked")
  protected final Class<T> getEntityClass() {
    if (entityClass == null) {
      Type[] actualTypeArguments =
          ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments();
      entityClass = (Class<T>) actualTypeArguments[0];
    }
    return entityClass;
  }
}
