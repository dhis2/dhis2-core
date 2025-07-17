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

import static org.springframework.http.CacheControl.noCache;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import com.fasterxml.jackson.dataformat.csv.CsvWriteException;
import com.google.common.base.Joiner;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.PropertyNames;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.query.Filter;
import org.hisp.dhis.query.Filters;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.query.GetObjectParams;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserSettingsService;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Base controller for APIs that only want to offer read only access through both Gist API and full
 * API.
 *
 * @author Jan Bernitt
 */
@Maturity.Stable
@OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
public abstract class AbstractFullReadOnlyController<
        T extends IdentifiableObject, P extends GetObjectListParams>
    extends AbstractGistReadOnlyController<T> {

  @Autowired protected IdentifiableObjectManager manager;

  @Autowired protected UserSettingsService userSettingsService;

  @Autowired protected ContextService contextService;

  @Autowired protected QueryService queryService;

  @Autowired protected FieldFilterService oldFieldFilterService;

  @Autowired protected org.hisp.dhis.fieldfiltering.FieldFilterService fieldFilterService;

  @Autowired protected LinkService linkService;

  @Autowired protected AclService aclService;

  @Autowired protected AttributeService attributeService;

  @Autowired protected CsvMapper csvMapper;

  // --------------------------------------------------------------------------
  // Hooks
  // --------------------------------------------------------------------------

  /**
   * Override to process entities after it has been retrieved from storage and before it is returned
   * to the view. Entities is null-safe.
   */
  protected void postProcessResponseEntities(List<T> entityList, P params) {}

  /**
   * Override to process a single entity after it has been retrieved from storage and before it is
   * returned to the view. Entity is null-safe.
   */
  protected void postProcessResponseEntity(T entity, GetObjectParams params) {}

  /**
   * Allows to append further filters to the incoming ones. Recommended only on very specific cases
   * where forcing a new filter, programmatically, make sense. This is usually used to ensure that
   * some filters are always present.
   */
  protected void addProgrammaticModifiers(P params) {}

  protected void addProgrammaticFilters(Consumer<String> add) {}

  // --------------------------------------------------------------------------
  // GET Full
  // --------------------------------------------------------------------------

  @OpenApi.Shared(value = false)
  public static class GetObjectListResponse {
    @OpenApi.Property Pager pager;

    @OpenApi.Property(name = "path$", value = OpenApi.EntityType[].class)
    List<Object> entries;
  }

  @OpenApi.Response(GetObjectListResponse.class)
  @GetMapping
  public @ResponseBody ResponseEntity<StreamingJsonRoot<T>> getObjectList(
      P params, HttpServletResponse response, @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException, ConflictException {
    return getObjectListInternal(params, response, currentUser, getAdditionalFilters(params));
  }

  protected final ResponseEntity<StreamingJsonRoot<T>> getObjectListWith(
      P params,
      HttpServletResponse response,
      UserDetails currentUser,
      List<Filter> additionalFilters)
      throws ForbiddenException, BadRequestException, ConflictException {
    List<Filter> filters = getAdditionalFilters(params);
    filters.addAll(additionalFilters);
    return getObjectListInternal(params, response, currentUser, filters);
  }

  protected final ResponseEntity<StreamingJsonRoot<T>> getObjectListInternal(
      P params,
      HttpServletResponse response,
      UserDetails currentUser,
      List<Filter> additionalFilters)
      throws ForbiddenException, BadRequestException {

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    addProgrammaticModifiers(params);

    // a top level restriction combined with AND that is always false always results in an empty
    // list
    boolean isAlwaysEmpty =
        params.getRootJunction() == Junction.Type.AND
            && additionalFilters.stream().anyMatch(Filter::isAlwaysFalse);
    List<T> entities = isAlwaysEmpty ? List.of() : getEntityList(params, additionalFilters);
    postProcessResponseEntities(entities, params);

    List<String> fields = params.getFieldsJsonList();
    handleLinksAndAccess(entities, fields, false);

    Pager pager = null;
    if (params.isPaging()) {
      long totalCount = isAlwaysEmpty ? 0 : countGetObjectList(params, additionalFilters);
      pager = new Pager(params.getPage(), totalCount, params.getPageSize());
      linkService.generatePagerLinks(pager, getEntityClass());
    }

    cachePrivate(response);
    return ResponseEntity.ok(
        new StreamingJsonRoot<>(
            pager,
            getSchema().getCollectionName(),
            FieldFilterParams.of(entities, fields),
            params.getDefaults().isExclude()));
  }

  /**
   * A way to incorporate additional filters to the {@link #getObjectList(GetObjectListParams,
   * HttpServletResponse, UserDetails)} endpoint that require running a separate query resulting in
   * matching ID list which then is used as filter in the standard query process.
   *
   * @param params options used
   * @return the ID of matches, nor null when no such filter is present/used
   */
  @CheckForNull
  protected List<UID> getPreQueryMatches(P params) throws ConflictException {
    return null; // no special filters used
  }

  @Nonnull
  protected List<Filter> getAdditionalFilters(P params) throws ConflictException {
    List<Filter> filters = new ArrayList<>();
    if (params.getQuery() != null && !params.getQuery().isEmpty() && getEntityClass() != User.class)
      filters.add(Filters.query(params.getQuery()));
    List<UID> matches = getPreQueryMatches(params);
    // Note: null = no special filters, empty = no matches for special filters
    if (matches != null) filters.add(createIdInFilter(matches));
    return filters;
  }

  protected final Filter createIdInFilter(@Nonnull List<UID> matches) {
    return Filters.in("id", UID.toValueList(matches));
  }

  @GetMapping(produces = {"text/csv", "application/text"})
  public ResponseEntity<String> getObjectListCsv(
      P params,
      @CurrentUser UserDetails currentUser,
      @RequestParam(defaultValue = ",") char separator,
      @RequestParam(defaultValue = ";") String arraySeparator,
      @RequestParam(defaultValue = "false") boolean skipHeader,
      HttpServletResponse response)
      throws IOException,
          NotFoundException,
          ConflictException,
          ForbiddenException,
          BadRequestException {

    // only support metadata
    if (!getSchema().isMetadata()) {
      throw new NotFoundException(
          "Not a metadata object type: " + getEntityClass().getSimpleName());
    }

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    List<T> entities = getEntityList(params, List.of());
    List<String> fields = params.getFieldsCsvList();
    try {
      String csv = applyCsvSteps(fields, entities, separator, arraySeparator, skipHeader);
      return ResponseEntity.ok(csv);
    } catch (CsvWriteException ex) {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);

      throw new ConflictException(
              "Invalid property selected. Make sure all properties are either simple or collections of refs / simple.")
          .setDevMessage(ex.getMessage());
    }
  }

  protected String applyCsvSteps(
      List<String> fields,
      List<T> entities,
      char separator,
      String arraySeparator,
      boolean skipHeader)
      throws IOException {
    CsvSchema schema;
    CsvSchema.Builder schemaBuilder = CsvSchema.builder();
    Map<String, Function<T, Object>> obj2valueByProperty = new LinkedHashMap<>();

    setupSchemaAndProperties(schemaBuilder, fields, obj2valueByProperty);

    schema =
        schemaBuilder
            .build()
            .withColumnSeparator(separator)
            .withArrayElementSeparator(arraySeparator);

    if (!skipHeader) {
      schema = schema.withHeader();
    }

    try (StringWriter strW = new StringWriter();
        SequenceWriter seqW = csvMapper.writer(schema).writeValues(strW)) {

      Object[] row = new Object[obj2valueByProperty.size()];

      for (T e : entities) {
        int i = 0;

        for (Function<T, Object> toValue : obj2valueByProperty.values()) {
          Object o = toValue.apply(e);

          if (o instanceof Collection) {
            row[i++] =
                ((Collection<?>) o)
                    .stream().map(String::valueOf).collect(Collectors.joining(arraySeparator));
          } else {
            row[i++] = o;
          }
        }

        seqW.write(row);
      }
      return strW.toString();
    }
  }

  private void setupSchemaAndProperties(
      Builder schemaBuilder,
      List<String> fields,
      Map<String, Function<T, Object>> obj2valueByProperty) {
    for (String field : fields) {
      // We just split on ',' here, we do not try and deep dive into
      // objects using [], if the client provides id,name,group[id]
      // then the group[id] part is simply ignored.
      for (String fieldName : field.split(",")) {
        Property property = getSchema().getProperty(fieldName);

        if (property == null) {
          if (CodeGenerator.isValidUid(fieldName)) {
            schemaBuilder.addColumn(fieldName);
            obj2valueByProperty.put(fieldName, obj -> getAttributeValue(obj, fieldName));
          }
          continue;
        }

        if ((property.isCollection() && property.itemIs(PropertyType.REFERENCE))) {
          schemaBuilder.addArrayColumn(property.getCollectionName());
          obj2valueByProperty.put(
              property.getCollectionName(), obj -> getCollectionValue(obj, property));
        } else if (property.isSimple()) {
          schemaBuilder.addColumn(property.getName());
          obj2valueByProperty.put(
              property.getName(),
              obj -> ReflectionUtils.invokeMethod(obj, property.getGetterMethod()));
        }
      }
    }
  }

  private static List<String> getCollectionValue(Object obj, Property property) {
    Object value = ReflectionUtils.invokeMethod(obj, property.getGetterMethod());

    @SuppressWarnings("unchecked")
    Collection<IdentifiableObject> collection = (Collection<IdentifiableObject>) value;

    return collection.stream().map(PrimaryKeyObject::getUid).toList();
  }

  private static Object getAttributeValue(Object obj, String attrId) {
    if (obj instanceof IdentifiableObject identifiableObject) {
      return identifiableObject.getAttributeValues().get(attrId);
    }
    return null;
  }

  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping("/{uid:[a-zA-Z0-9]{11}}")
  public @ResponseBody ResponseEntity<?> getObject(
      @OpenApi.Param(UID.class) @PathVariable("uid") String pvUid,
      GetObjectParams params,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    cachePrivate(response);

    T entity = getEntity(pvUid);

    GetObjectListParams listParams = params.toListParams();
    addProgrammaticFilters(listParams::addFilter); // temporary workaround
    Query<T> query = queryService.getQueryFromUrl(getEntityClass(), listParams);
    query.setCurrentUserDetails(currentUser);
    query.setObjects(List.of(entity));
    query.setDefaults(params.getDefaults());

    List<T> entities = queryService.query(query);

    List<String> fields = params.getFieldsObject();
    handleLinksAndAccess(entities, fields, true);

    entities.forEach(e -> postProcessResponseEntity(e, params));

    return ResponseEntity.ok(
        new StreamingJsonRoot<>(
            null, null, FieldFilterParams.of(entities, fields), query.getDefaults().isExclude()));
  }

  @GetMapping("/{uid:[a-zA-Z0-9]{11}}/{property}")
  public @ResponseBody ResponseEntity<ObjectNode> getObjectProperty(
      @OpenApi.Param(UID.class) @PathVariable("uid") String pvUid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String pvProperty,
      @RequestParam(required = false) List<String> fields,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    if (fields == null || fields.isEmpty()) {
      fields = List.of(":all");
    }

    String fieldFilter = "[" + Joiner.on(',').join(fields) + "]";

    cachePrivate(response);

    GetObjectParams params = new GetObjectParams();
    params.addField(pvProperty + fieldFilter);
    ObjectNode objectNode = getObjectInternal(pvUid, params, currentUser);

    return ResponseEntity.ok(objectNode);
  }

  @SuppressWarnings("unchecked")
  private ObjectNode getObjectInternal(String uid, GetObjectParams params, UserDetails currentUser)
      throws NotFoundException {
    T entity = getEntity(uid);

    Query<T> query = queryService.getQueryFromUrl(getEntityClass(), params.toListParams());
    query.setCurrentUserDetails(currentUser);
    query.setObjects(List.of(entity));
    query.setDefaults(params.getDefaults());

    List<T> entities = queryService.query(query);

    List<String> fields = params.getFieldsObject();
    handleLinksAndAccess(entities, fields, true);

    entities.forEach(e -> postProcessResponseEntity(entity, params));

    FieldFilterParams<T> filterParams = FieldFilterParams.of(entities, fields);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(filterParams);

    return objectNodes.isEmpty() ? fieldFilterService.createObjectNode() : objectNodes.get(0);
  }

  private List<T> getEntityList(P params, List<Filter> additionalFilters)
      throws BadRequestException {
    try {
      Query<T> query = queryService.getQueryFromUrl(getEntityClass(), params);
      query.add(additionalFilters);

      query.setDefaultOrder();
      query.setDefaults(params.getDefaults());

      modifyGetObjectList(params, query);

      List<T> res = queryService.query(query);
      getEntityListPostProcess(params, res);
      return res;
    } catch (QueryParserException ex) {
      throw new BadRequestException(ex.getMessage());
    }
  }

  protected void modifyGetObjectList(P params, Query<T> query) {
    // by default: nothing special to do
  }

  protected void getEntityListPostProcess(P params, List<T> entities) throws BadRequestException {}

  private long countGetObjectList(P params, List<Filter> additionalFilters)
      throws BadRequestException {
    try {
      Query<T> query = queryService.getQueryFromUrl(getEntityClass(), params);
      query.add(additionalFilters);
      modifyGetObjectList(params, query);
      return queryService.count(query);
    } catch (QueryParserException ex) {
      throw new BadRequestException(ex.getMessage());
    }
  }

  private void cachePrivate(HttpServletResponse response) {
    response.setHeader(
        ContextUtils.HEADER_CACHE_CONTROL, noCache().cachePrivate().getHeaderValue());
  }

  private boolean hasHref(List<String> fields) {
    return fieldsContains("href", fields);
  }

  private void handleLinksAndAccess(List<T> entityList, List<String> fields, boolean deep) {
    if (hasHref(fields)) {
      linkService.generateLinks(entityList, deep);
    }
  }

  private boolean fieldsContains(String match, List<String> fields) {
    for (String field : fields) {
      // for now assume href/access if * or preset is requested
      if (field.contains(match) || field.equals("*") || field.startsWith(":")) {
        return true;
      }
    }

    return false;
  }

  // --------------------------------------------------------------------------
  // Reflection helpers
  // --------------------------------------------------------------------------

  private String entitySimpleName;

  protected final String getEntitySimpleName() {
    if (entitySimpleName == null) {
      entitySimpleName = getEntityClass().getSimpleName();
    }
    return entitySimpleName;
  }

  @Nonnull
  protected T getEntity(String uid) throws NotFoundException {
    return getEntity(uid, getEntityClass())
        .orElseThrow(() -> new NotFoundException(getEntityClass(), uid));
  }

  protected final <E extends IdentifiableObject> java.util.Optional<E> getEntity(
      String uid, Class<E> entityType) {
    return java.util.Optional.ofNullable(manager.get(entityType, uid));
  }

  protected final Schema getSchema(Class<?> klass) {
    return schemaService.getDynamicSchema(klass);
  }
}
