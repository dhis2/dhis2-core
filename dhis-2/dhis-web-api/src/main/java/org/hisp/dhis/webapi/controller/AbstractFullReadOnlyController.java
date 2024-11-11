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
package org.hisp.dhis.webapi.controller;

import static org.springframework.http.CacheControl.noCache;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import com.fasterxml.jackson.dataformat.csv.CsvWriteException;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Value;
import org.hisp.dhis.attribute.AttributeService;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.Maturity;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.OpenApi.PropertyNames;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dxf2.common.OrderParams;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfilter.Defaults;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldFilterParams;
import org.hisp.dhis.fieldfiltering.FieldPreset;
import org.hisp.dhis.query.Criterion;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.query.Pagination;
import org.hisp.dhis.query.Query;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.query.Restrictions;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ReflectionUtils;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserSettingsService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.service.LinkService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.PaginationUtils;
import org.hisp.dhis.webapi.webdomain.StreamingJsonRoot;
import org.hisp.dhis.webapi.webdomain.WebMetadata;
import org.hisp.dhis.webapi.webdomain.WebOptions;
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
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
@OpenApi.Document(group = OpenApi.Document.GROUP_QUERY)
public abstract class AbstractFullReadOnlyController<T extends IdentifiableObject>
    extends AbstractGistReadOnlyController<T> {
  protected static final String DEFAULTS = "INCLUDE";

  protected static final WebOptions NO_WEB_OPTIONS = new WebOptions(new HashMap<>());

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
  protected void postProcessResponseEntities(
      List<T> entityList, WebOptions options, Map<String, String> parameters) {}

  /**
   * Override to process a single entity after it has been retrieved from storage and before it is
   * returned to the view. Entity is null-safe.
   */
  protected void postProcessResponseEntity(
      T entity, WebOptions options, Map<String, String> parameters) {}

  /**
   * Allows to append new filters to the incoming ones. Recommended only on very specific cases
   * where forcing a new filter, programmatically, make sense.
   */
  protected void forceFiltering(final WebOptions webOptions, final List<String> filters) {}

  // --------------------------------------------------------------------------
  // GET Full
  // --------------------------------------------------------------------------

  @Value
  @OpenApi.Shared(value = false)
  protected static class ObjectListResponse {
    @OpenApi.Property Pager pager;

    @OpenApi.Property(name = "path$", value = OpenApi.EntityType[].class)
    List<Object> entries;
  }

  @OpenApi.Param(name = "fields", value = String[].class)
  @OpenApi.Param(name = "filter", value = String[].class)
  @OpenApi.Params(WebOptions.class)
  @OpenApi.Response(ObjectListResponse.class)
  @GetMapping
  public @ResponseBody ResponseEntity<StreamingJsonRoot<T>> getObjectList(
      @RequestParam Map<String, String> rpParameters,
      OrderParams orderParams,
      HttpServletResponse response,
      @CurrentUser UserDetails currentUser)
      throws ForbiddenException, BadRequestException {
    return getObjectListInternal(
        rpParameters, orderParams, response, currentUser, this::getSpecialFilters);
  }

  protected final ResponseEntity<StreamingJsonRoot<T>> getObjectListInternal(
      Map<String, String> rpParameters,
      OrderParams orderParams,
      HttpServletResponse response,
      UserDetails currentUser,
      List<Criterion> additionalSpecialFilters)
      throws ForbiddenException, BadRequestException {
    return getObjectListInternal(
        rpParameters,
        orderParams,
        response,
        currentUser,
        options -> {
          List<Criterion> filters = getSpecialFilters(options);
          filters.addAll(additionalSpecialFilters);
          return filters;
        });
  }

  protected final ResponseEntity<StreamingJsonRoot<T>> getObjectListInternal(
      Map<String, String> rpParameters,
      OrderParams orderParams,
      HttpServletResponse response,
      UserDetails currentUser,
      Function<WebOptions, List<Criterion>> getSpecialFilters)
      throws ForbiddenException, BadRequestException {
    List<Order> orders = orderParams.getOrders(getSchema());
    List<String> fields = new ArrayList<>(contextService.getParameterValues("fields"));
    List<String> filters = new ArrayList<>(contextService.getParameterValues("filter"));

    if (fields.isEmpty()) {
      fields.addAll(FieldPreset.defaultPreset().getFields());
    }

    WebOptions options = new WebOptions(rpParameters);

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    forceFiltering(options, filters);

    // Note: null = no special filters, empty = no matches for special filters
    List<Criterion> specialFilters = getSpecialFilters.apply(options);

    List<T> entities = getEntityList(options, filters, orders, specialFilters);
    postProcessResponseEntities(entities, options, rpParameters);
    handleLinksAndAccess(entities, fields, false);

    Pager pager = null;
    if (options.hasPaging())
      if (options.hasPaging()) {
        long totalCount = countTotal(options, filters, orders, specialFilters);
        pager = new Pager(options.getPage(), totalCount, options.getPageSize());
      }

    linkService.generatePagerLinks(pager, getEntityClass());

    cachePrivate(response);

    return ResponseEntity.ok(
        new StreamingJsonRoot<>(
            pager,
            getSchema().getCollectionName(),
            FieldFilterParams.of(entities, fields),
            Defaults.valueOf(options.get("defaults", DEFAULTS)).isExclude()));
  }

  protected List<UID> getSpecialFilterMatches(WebOptions options) {
    return null; // no special filters used
  }

  protected List<Criterion> getSpecialFilters(WebOptions options) {
    List<Criterion> filters = new ArrayList<>();
    if (options.getOptions().containsKey("query"))
      filters.add(Restrictions.query(getSchema(), options.getOptions().get("query")));
    List<UID> matches = getSpecialFilterMatches(options);
    if (matches != null) filters.add(createIdInFilter(matches));
    return filters;
  }

  protected final Criterion createIdInFilter(@Nonnull List<UID> specialFilterMatches) {
    return Restrictions.in("id", UID.toValueList(specialFilterMatches));
  }

  @OpenApi.Param(name = "fields", value = String[].class)
  @OpenApi.Param(name = "filter", value = String[].class)
  @OpenApi.Params(WebOptions.class)
  @GetMapping(produces = {"text/csv", "application/text"})
  public ResponseEntity<String> getObjectListCsv(
      @RequestParam Map<String, String> rpParameters,
      OrderParams orderParams,
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
    List<Order> orders = orderParams.getOrders(getSchema());
    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));
    List<String> filters = Lists.newArrayList(contextService.getParameterValues("filter"));

    WebOptions options = new WebOptions(rpParameters);
    WebMetadata metadata = new WebMetadata();

    if (fields.isEmpty() || fields.contains("*") || fields.contains(":all")) {
      fields.addAll(FieldPreset.defaultPreset().getFields());
    }

    // only support metadata
    if (!getSchema().isMetadata()) {
      throw new NotFoundException(
          "Not a metadata object type: " + getEntityClass().getSimpleName());
    }

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    List<T> entities = getEntityList(options, filters, orders, List.of());

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

  @OpenApi.Param(name = "fields", value = String[].class)
  @OpenApi.Param(name = "filter", value = String[].class)
  @OpenApi.Params(WebOptions.class)
  @OpenApi.Response(OpenApi.EntityType.class)
  @GetMapping("/{uid:[a-zA-Z0-9]{11}}")
  @SuppressWarnings("unchecked")
  public @ResponseBody ResponseEntity<?> getObject(
      @OpenApi.Param(UID.class) @PathVariable("uid") String pvUid,
      @RequestParam Map<String, String> rpParameters,
      @CurrentUser UserDetails currentUser,
      HttpServletRequest request,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));
    List<String> filters = Lists.newArrayList(contextService.getParameterValues("filter"));
    forceFiltering(new WebOptions(rpParameters), filters);

    if (fields.isEmpty()) {
      fields.add("*");
    }

    cachePrivate(response);

    WebOptions options = new WebOptions(rpParameters);
    T entity = getEntity(pvUid, options);

    Query query =
        queryService.getQueryFromUrl(
            getEntityClass(),
            filters,
            new ArrayList<>(),
            getPaginationData(options),
            options.getRootJunction());
    query.setCurrentUserDetails(currentUser);
    query.setObjects(List.of(entity));
    query.setDefaults(Defaults.valueOf(options.get("defaults", DEFAULTS)));

    List<T> entities = (List<T>) queryService.query(query);

    handleLinksAndAccess(entities, fields, true);

    entities.forEach(e -> postProcessResponseEntity(e, options, rpParameters));

    return ResponseEntity.ok(
        new StreamingJsonRoot<>(
            null, null, FieldFilterParams.of(entities, fields), query.getDefaults().isExclude()));
  }

  @OpenApi.Param(name = "fields", value = String[].class)
  @OpenApi.Params(WebOptions.class)
  @GetMapping("/{uid:[a-zA-Z0-9]{11}}/{property}")
  public @ResponseBody ResponseEntity<ObjectNode> getObjectProperty(
      @OpenApi.Param(UID.class) @PathVariable("uid") String pvUid,
      @OpenApi.Param(PropertyNames.class) @PathVariable("property") String pvProperty,
      @RequestParam Map<String, String> rpParameters,
      @CurrentUser UserDetails currentUser,
      HttpServletResponse response)
      throws ForbiddenException, NotFoundException {

    if (!aclService.canRead(currentUser, getEntityClass())) {
      throw new ForbiddenException(
          "You don't have the proper permissions to read objects of this type.");
    }

    List<String> fields = Lists.newArrayList(contextService.getParameterValues("fields"));

    if (fields.isEmpty()) {
      fields.add(":all");
    }

    String fieldFilter = "[" + Joiner.on(',').join(fields) + "]";

    cachePrivate(response);

    ObjectNode objectNode =
        getObjectInternal(
            pvUid,
            rpParameters,
            Lists.newArrayList(),
            Lists.newArrayList(pvProperty + fieldFilter),
            currentUser);

    return ResponseEntity.ok(objectNode);
  }

  @SuppressWarnings("unchecked")
  private ObjectNode getObjectInternal(
      String uid,
      Map<String, String> parameters,
      List<String> filters,
      List<String> fields,
      UserDetails currentUser)
      throws NotFoundException {
    WebOptions options = new WebOptions(parameters);
    T entity = getEntity(uid, options);

    Query query =
        queryService.getQueryFromUrl(
            getEntityClass(),
            filters,
            new ArrayList<>(),
            getPaginationData(options),
            options.getRootJunction());
    query.setCurrentUserDetails(currentUser);
    query.setObjects(List.of(entity));
    query.setDefaults(Defaults.valueOf(options.get("defaults", DEFAULTS)));

    List<T> entities = (List<T>) queryService.query(query);

    handleLinksAndAccess(entities, fields, true);

    entities.forEach(e -> postProcessResponseEntity(entity, options, parameters));

    FieldFilterParams<T> filterParams = FieldFilterParams.of(entities, fields);
    List<ObjectNode> objectNodes = fieldFilterService.toObjectNodes(filterParams);

    return objectNodes.isEmpty() ? fieldFilterService.createObjectNode() : objectNodes.get(0);
  }

  private List<T> getEntityList(
      WebOptions options, List<String> filters, List<Order> orders, List<Criterion> specialFilters)
      throws BadRequestException {
    Query query =
        BadRequestException.on(
            QueryParserException.class,
            () ->
                queryService.getQueryFromUrl(
                    getEntityClass(),
                    filters,
                    orders,
                    getPaginationData(options),
                    options.getRootJunction()));
    query.add(specialFilters);

    query.setDefaultOrder();
    query.setDefaults(Defaults.valueOf(options.get("defaults", DEFAULTS)));

    @SuppressWarnings("unchecked")
    List<T> res = (List<T>) queryService.query(query);
    getEntityListPostProcess(options, res);
    return res;
  }

  protected void getEntityListPostProcess(WebOptions options, List<T> entities) {}

  private long countTotal(
      WebOptions options, List<String> filters, List<Order> orders, List<Criterion> specialFilters)
      throws BadRequestException {
    Query query =
        BadRequestException.on(
            QueryParserException.class,
            () ->
                queryService.getQueryFromUrl(
                    getEntityClass(),
                    filters,
                    orders,
                    new Pagination(),
                    options.getRootJunction()));
    query.add(specialFilters);
    return queryService.count(query);
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

  private String entityName;

  private String entitySimpleName;

  protected final String getEntityName() {
    if (entityName == null) {
      entityName = getEntityClass().getName();
    }

    return entityName;
  }

  protected final String getEntitySimpleName() {
    if (entitySimpleName == null) {
      entitySimpleName = getEntityClass().getSimpleName();
    }

    return entitySimpleName;
  }

  @Nonnull
  protected final T getEntity(String uid) throws NotFoundException {
    return getEntity(uid, NO_WEB_OPTIONS);
  }

  @Nonnull
  protected T getEntity(String uid, WebOptions options) throws NotFoundException {
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

  // --------------------------------------------------------------------------
  // Helpers
  // --------------------------------------------------------------------------

  protected final Pagination getPaginationData(WebOptions options) {
    return PaginationUtils.getPaginationData(options);
  }
}
