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

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static org.springframework.http.CacheControl.noCache;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Value;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.PropertyNames;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.gist.GistAutoType;
import org.hisp.dhis.gist.GistPager;
import org.hisp.dhis.gist.GistParams;
import org.hisp.dhis.gist.GistQuery;
import org.hisp.dhis.gist.GistQuery.Comparison;
import org.hisp.dhis.gist.GistQuery.Filter;
import org.hisp.dhis.gist.GistQuery.Owner;
import org.hisp.dhis.gist.GistService;
import org.hisp.dhis.jsontree.JsonValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.UserSettings;
import org.hisp.dhis.webapi.CsvBuilder;
import org.hisp.dhis.webapi.JsonBuilder;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Base controller for APIs that only want to offer read-only access though Gist API.
 *
 * @author Jan Bernitt
 */
@Maturity.Stable
@OpenApi.EntityType(OpenApi.EntityType.class)
@OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
public abstract class AbstractGistReadOnlyController<T extends PrimaryKeyObject> {
  @Autowired protected ObjectMapper jsonMapper;

  @Autowired protected SchemaService schemaService;

  @Autowired private GistService gistService;

  // --------------------------------------------------------------------------
  // GET Gist
  // --------------------------------------------------------------------------

  @OpenApi.Response(value = OpenApi.EntityType.class)
  @GetMapping(value = "/{uid}/gist", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<JsonNode> getObjectGist(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid, GistParams params)
      throws NotFoundException, BadRequestException {
    return gistToJsonObjectResponse(
        uid,
        createGistQuery(params, getEntityClass(), GistAutoType.L)
            .withFilter(new Filter("id", Comparison.EQ, uid)));
  }

  @OpenApi.Response(String.class)
  @GetMapping(
      value = {"/{uid}/gist", "/{uid}/gist.csv"},
      produces = "text/csv")
  public void getObjectGistAsCsv(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      GistParams params,
      HttpServletResponse response)
      throws IOException, BadRequestException {
    gistToCsvResponse(
        response,
        createGistQuery(params, getEntityClass(), GistAutoType.L)
            .withFilter(new Filter("id", Comparison.EQ, uid))
            .toBuilder()
            .typedAttributeValues(false)
            .build());
  }

  @Value
  @OpenApi.Shared(value = false)
  private static class GistListResponse {
    @OpenApi.Property GistPager pager;

    @OpenApi.Property(name = "path$", value = OpenApi.EntityType[].class)
    ObjectNode[] entries = null;
  }

  @OpenApi.Response({GistListResponse.class, OpenApi.EntityType[].class})
  @GetMapping(value = "/gist", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<JsonNode> getObjectListGist(
      GistParams params, HttpServletRequest request) throws BadRequestException {
    return gistToJsonArrayResponse(
        request, params, createGistQuery(params, getEntityClass(), GistAutoType.S), getSchema());
  }

  @OpenApi.Response(value = String.class)
  @GetMapping(
      value = {"/gist", "/gist.csv"},
      produces = "text/csv")
  public void getObjectListGistAsCsv(GistParams params, HttpServletResponse response)
      throws IOException, BadRequestException {
    gistToCsvResponse(
        response,
        createGistQuery(params, getEntityClass(), GistAutoType.S).toBuilder()
            .typedAttributeValues(false)
            .build());
  }

  @OpenApi.Response(JsonValue.class)
  @GetMapping(value = "/{uid}/{property}/gist", produces = APPLICATION_JSON_VALUE)
  public @ResponseBody ResponseEntity<JsonNode> getObjectPropertyGist(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      GistParams params,
      HttpServletRequest request)
      throws BadRequestException, NotFoundException {
    Property objProperty = getSchema().getProperty(property);
    if (objProperty == null) {
      throw new BadRequestException("No such property: " + property);
    }

    if (!objProperty.isCollection()
        || !PrimaryKeyObject.class.isAssignableFrom(objProperty.getItemKlass())) {
      return gistToJsonObjectResponse(
          uid,
          createGistQuery(params, getEntityClass(), GistAutoType.L)
              .withFilter(new Filter("id", Comparison.EQ, uid))
              .withField(property));
    }

    return gistToJsonArrayResponse(
        request,
        params,
        createPropertyQuery(uid, property, params, objProperty),
        schemaService.getDynamicSchema(objProperty.getItemKlass()));
  }

  @OpenApi.Response(String.class)
  @GetMapping(
      value = {"/{uid}/{property}/gist", "/{uid}/{property}/gist.csv"},
      produces = "text/csv")
  public void getObjectPropertyGistAsCsv(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String property,
      GistParams params,
      HttpServletResponse response)
      throws BadRequestException, IOException {
    Property objProperty = getSchema().getProperty(property);
    if (objProperty == null) {
      throw new BadRequestException("No such property: " + property);
    }
    gistToCsvResponse(
        response,
        createPropertyQuery(uid, property, params, objProperty).toBuilder()
            .typedAttributeValues(false)
            .build());
  }

  @SuppressWarnings("unchecked")
  private GistQuery createPropertyQuery(
      @OpenApi.Param(UID.class) @PathVariable("uid") String uid,
      @PathVariable("property") String property,
      GistParams params,
      Property objProperty)
      throws BadRequestException {
    return createGistQuery(
            params, (Class<IdentifiableObject>) objProperty.getItemKlass(), GistAutoType.M)
        .withOwner(
            Owner.builder().id(uid).type(getEntityClass()).collectionProperty(property).build());
  }

  private GistQuery createGistQuery(
      GistParams params, Class<? extends PrimaryKeyObject> elementType, GistAutoType autoDefault)
      throws BadRequestException {
    Locale translationLocale =
        !params.getLocale().isEmpty()
            ? Locale.forLanguageTag(params.getLocale())
            : UserSettings.getCurrentSettings().getUserDbLocale();
    return GistQuery.builder()
        .elementType(elementType)
        .autoType(params.getAuto(autoDefault))
        .contextRoot(ContextUtils.getRootPath(ContextUtils.getRequest()))
        .requestURL(ContextUtils.getRequestURL())
        .translationLocale(translationLocale)
        .typedAttributeValues(true)
        .build()
        .with(params);
  }

  private ResponseEntity<JsonNode> gistToJsonObjectResponse(String uid, GistQuery query)
      throws NotFoundException {
    if (query.isDescribe()) {
      return gistDescribeToJsonObjectResponse(query);
    }
    query = gistService.plan(query);
    List<?> elements = gistService.gist(query);
    JsonNode body =
        new JsonBuilder(jsonMapper).skipNullOrEmpty().toArray(query.getFieldNames(), elements);
    if (body.isEmpty()) {
      throw new NotFoundException(getEntityClass(), uid);
    }
    return ResponseEntity.ok().cacheControl(noCache().cachePrivate()).body(body.get(0));
  }

  private ResponseEntity<JsonNode> gistToJsonArrayResponse(
      HttpServletRequest request, GistParams params, GistQuery query, Schema schema) {
    if (query.isDescribe()) {
      return gistDescribeToJsonObjectResponse(query);
    }
    query = gistService.plan(query);
    JsonBuilder responseBuilder = new JsonBuilder(jsonMapper);
    List<String> fieldNames = query.getFieldNames();
    List<?> matches = gistService.gist(query);
    List<?> elements = matches;
    if (params.isOrgUnitsTree() && query.getElementType() == OrganisationUnit.class) {
      fieldNames = new ArrayList<>(fieldNames);
      fieldNames.add("match");
      elements = gistToJsonOrgUnitTreeResponse(query, matches);
    }
    JsonNode body = responseBuilder.skipNullOrEmpty().toArray(fieldNames, elements);
    if (!query.isHeadless()) {
      String property =
          params.getPageListName() == null ? schema.getPlural() : params.getPageListName();
      body =
          query.isPaging()
              ? responseBuilder.toObject(
                  List.of("pager", property),
                  gistService.pager(query, matches, request.getParameterMap()),
                  body)
              : responseBuilder.toObject(List.of(property), body);
    }
    return ResponseEntity.ok().cacheControl(noCache().cachePrivate()).body(body);
  }

  private List<?> gistToJsonOrgUnitTreeResponse(GistQuery query, List<?> matches) {
    // - add match true to all matches
    List<Object[]> elements = matches.stream().map(e -> appendMatchElement(e, true)).toList();
    // - isolate path column as list
    int pathIndex = query.getFieldNames().indexOf("path");
    List<String> paths = elements.stream().map(e -> (String) ((Object[]) e)[pathIndex]).toList();
    // - make a list of all matching IDs
    List<String> matchesIds =
        paths.stream().map(path -> path.substring(path.lastIndexOf('/') + 1)).toList();
    // - make a set of all IDs in any of the paths
    Set<String> ids =
        paths.stream()
            .flatMap(path -> Stream.of(path.split("/")).filter(not(String::isEmpty)))
            .collect(toSet());
    matchesIds.forEach(
        ids::remove); // we already have those, what is left are the ancestors not yet fetched
    // if ancestors are missing fetch them
    if (ids.isEmpty()) return elements;
    List<Object[]> ancestors =
        gistService
            .gist(
                GistQuery.builder()
                    .elementType(query.getElementType())
                    .translationLocale(query.getTranslationLocale())
                    .paging(false)
                    .filters(List.of(new Filter("id", Comparison.IN, ids.toArray(String[]::new))))
                    .fields(query.getFields())
                    .build())
            .stream()
            .map(e -> appendMatchElement(e, false))
            .toList();
    // - inject ancestors into elements list (ordered by path)
    return Stream.concat(elements.stream(), ancestors.stream())
        .sorted(comparing(e -> ((String) e[pathIndex])))
        .toList();
  }

  private Object[] appendMatchElement(Object arr, boolean match) {
    Object[] from = (Object[]) arr;
    Object[] to = Arrays.copyOf(from, from.length + 1);
    to[from.length] = match;
    return to;
  }

  private ResponseEntity<JsonNode> gistDescribeToJsonObjectResponse(GistQuery query) {
    return ResponseEntity.ok()
        .cacheControl(noCache().cachePrivate())
        .body(new JsonBuilder(jsonMapper).skipNullMembers().toObject(gistService.describe(query)));
  }

  private void gistToCsvResponse(HttpServletResponse response, GistQuery query) throws IOException {
    query = gistService.plan(query).toBuilder().references(false).build();
    response.addHeader(HttpHeaders.CONTENT_TYPE, "text/csv");
    new CsvBuilder(response.getWriter())
        .withLocale(query.getTranslationLocale())
        .skipHeaders(query.isHeadless())
        .toRows(query.getFieldNames(), gistService.gist(query));
  }

  // --------------------------------------------------------------------------
  // Reflection helpers
  // --------------------------------------------------------------------------

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

  private Schema schema;

  protected final Schema getSchema() {
    if (schema == null) {
      schema = schemaService.getDynamicSchema(getEntityClass());
    }
    return schema;
  }
}
