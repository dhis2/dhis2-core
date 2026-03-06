/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.gist;

import static java.lang.Math.abs;
import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.gist.GistQuery.Comparison.EQ;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.common.PrimaryKeyObject;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.object.ObjectOutput.Property;
import org.hisp.dhis.object.ObjectOutput.Type;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.query.Junction;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.UserSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @implNote This implementation utilizes {@link Stream}-processing to transform the DB results to
 *     JSON and directly write them to the HTTP {@link OutputStream}. This is so that we avoid
 *     materializing the entire result list in memory. While the Gist API has paging limits it is
 *     the backing implementation of potentially many request so any load that can be avoided pays
 *     of though volume of work and resource allocation avoided.
 * @author Jan Bernitt
 */
@Component
@RequiredArgsConstructor
public class GistPipeline {

  private final GistService gistService;
  private final SchemaService schemaService;

  private final Map<Class<?>, String> collectionNames = new ConcurrentHashMap<>();

  /**
   * @param in describes the slice of data to export to JSON
   * @param out target to write the JSON to
   */
  @Transactional(readOnly = true)
  public void exportAsJson(@Nonnull GistObjectList.Input in, @Nonnull Supplier<OutputStream> out)
      throws BadRequestException {
    GistQuery query = createListQuery(in);
    GistObjectList list = listObjects(in, query);
    GistOutput.toJson(createObjectListOutput(in.params(), in.elementType(), list), out.get());
  }

  @Transactional(readOnly = true)
  public void exportAsCsv(@Nonnull GistObjectList.Input in, @Nonnull Supplier<OutputStream> out)
      throws BadRequestException {
    GistQuery query = createListQuery(in).withoutTypedAttributeValues();
    GistObjectList list = listObjects(in, query);
    GistOutput.toCsv(createObjectListOutput(in.params(), in.elementType(), list), out.get());
  }

  private GistObjectList listObjects(GistObjectList.Input input, GistQuery query)
      throws BadRequestException {
    GistObjectList list = gistService.exportObjectList(query);
    return input.elementType() == OrganisationUnit.class && input.params().isOrgUnitsTree()
        ? withAncestors(query, list)
        : list;
  }

  @Transactional(readOnly = true)
  public void exportAsJson(GistObject.Input in, Supplier<OutputStream> out)
      throws BadRequestException, NotFoundException {
    GistQuery query = createObjectQuery(in);
    GistObject obj = gistService.exportObject(query);
    Object[] values = obj.values();
    if (values == null) throw new NotFoundException(in.objectType(), in.id());
    GistOutput.toJson(new GistObject.Output(obj.properties(), values), out.get());
  }

  @Transactional(readOnly = true)
  public void exportAsCsv(GistObject.Input in, Supplier<OutputStream> out)
      throws BadRequestException, NotFoundException {
    GistQuery query = createObjectQuery(in).withoutTypedAttributeValues();
    GistObject obj = gistService.exportObject(query);
    Object[] values = obj.values();
    if (values == null) throw new NotFoundException(in.objectType(), in.id());
    GistOutput.toCsv(new GistObject.Output(obj.properties(), values), out.get());
  }

  @Transactional(readOnly = true)
  public void exportPropertyAsJson(
      @Nonnull GistObjectProperty.Input in, @Nonnull Supplier<OutputStream> out)
      throws BadRequestException, NotFoundException {
    org.hisp.dhis.schema.Property property = checkObjectProperty(in);

    if (!property.isCollection()
        || !PrimaryKeyObject.class.isAssignableFrom(property.getItemKlass())) {
      // equivalent to object info with: fields=<property>
      in.params().setFields(in.property());
      exportAsJson(new GistObject.Input(in.objectType(), in.id(), in.params()), out);
    } else {
      @SuppressWarnings("unchecked")
      Class<? extends PrimaryKeyObject> elementType =
          (Class<? extends PrimaryKeyObject>) property.getItemKlass();
      GistQuery query = createPropertyListQuery(in, elementType);
      GistObjectList list = gistService.exportPropertyObjectList(query);
      GistOutput.toJson(createObjectListOutput(in.params(), elementType, list), out.get());
    }
  }

  @Transactional(readOnly = true)
  public void exportPropertyAsCsv(
      @Nonnull GistObjectProperty.Input in, @Nonnull Supplier<OutputStream> out)
      throws BadRequestException, NotFoundException {
    org.hisp.dhis.schema.Property property = checkObjectProperty(in);

    if (!property.isCollection()
        || !PrimaryKeyObject.class.isAssignableFrom(property.getItemKlass())) {
      // equivalent to object info with: fields=<property>
      in.params().setFields(in.property());
      exportAsCsv(new GistObject.Input(in.objectType(), in.id(), in.params()), out);
    } else {
      @SuppressWarnings("unchecked")
      Class<? extends PrimaryKeyObject> elementType =
          (Class<? extends PrimaryKeyObject>) property.getItemKlass();
      GistQuery query = createPropertyListQuery(in, elementType).withoutTypedAttributeValues();
      GistObjectList list = gistService.exportPropertyObjectList(query);
      GistOutput.toCsv(createObjectListOutput(in.params(), elementType, list), out.get());
    }
  }

  @Nonnull
  private org.hisp.dhis.schema.Property checkObjectProperty(GistObjectProperty.Input in)
      throws BadRequestException {
    org.hisp.dhis.schema.Property property =
        schemaService.getSchema(in.objectType()).getProperty(in.property());
    if (property == null) throw new BadRequestException("No such property: " + in.property());
    return property;
  }

  @Nonnull
  private GistObjectList.Output createObjectListOutput(
      GistObjectListParams params, Class<?> elementType, GistObjectList list) {
    return new GistObjectList.Output(
        params.isHeadless(),
        list.pager(),
        getCollectionName(params, elementType),
        list.properties(),
        list.values());
  }

  private GistQuery createObjectQuery(GistObject.Input input) {
    GistObjectParams params = input.params();
    return GistQuery.builder()
        .elementType(input.objectType())
        .autoType(params.getAuto(GistAutoType.L))
        .translationLocale(getTranslationLocale(params.getLocale()))
        .typedAttributeValues(true)
        .translate(params.isTranslate())
        .references(params.isReferences())
        .absoluteUrls(params.isAbsoluteUrls())
        .fields(GistQuery.Field.ofList(input.params().fields))
        .filters(List.of(new GistQuery.Filter("id", EQ, input.id().getValue())))
        .build();
  }

  private GistQuery createPropertyListQuery(
      GistObjectProperty.Input input, Class<? extends PrimaryKeyObject> collectionItemType)
      throws BadRequestException {
    GistObjectPropertyParams params = input.params();
    int page = abs(params.getPage());
    int size = Math.min(1000, abs(params.getPageSize()));
    return GistQuery.builder()
        .elementType(collectionItemType)
        .autoType(params.getAuto(GistAutoType.M))
        .contextRoot(input.contextRoot())
        .requestURL(input.requestURL())
        .translationLocale(getTranslationLocale(params.getLocale()))
        .typedAttributeValues(true)
        .total(params.isCountTotalPages())
        .paging(true)
        .pageSize(size)
        .pageOffset(Math.max(0, page - 1) * size)
        .translate(params.isTranslate())
        .absoluteUrls(params.isAbsoluteUrls())
        .headless(params.isHeadless())
        .references(params.isReferences())
        .inverse(params.isInverse())
        .filters(GistQuery.Filter.ofList(params.getFilter()))
        .fields(GistQuery.Field.ofList(input.params().getFields()))
        .owner(
            GistQuery.Owner.builder()
                .id(input.id().getValue())
                .type(input.objectType())
                .collectionProperty(input.property())
                .build())
        .build();
  }

  private GistQuery createListQuery(GistObjectList.Input input) throws BadRequestException {
    GistObjectListParams params = input.params();
    int page = abs(params.getPage());
    int size = Math.min(1000, abs(params.getPageSize()));
    boolean tree = params.isOrgUnitsTree();
    boolean offline = params.isOrgUnitsOffline();
    String order = tree ? "path" : params.getOrder();
    if (offline && (order == null || order.isEmpty())) order = "level,name";
    String fields = offline ? "path,displayName,children::isNotEmpty" : params.getFields();
    // ensure tree always includes path in fields
    if (tree) {
      if (fields == null || fields.isEmpty()) {
        fields = "path";
      } else if (!(fields.contains(",path,"))
          || fields.startsWith("path,")
          || fields.endsWith(",path")) fields += ",path";
    }
    return GistQuery.builder()
        .elementType(input.elementType())
        .autoType(input.params().getAuto(GistAutoType.S))
        .contextRoot(input.contextRoot())
        .requestURL(input.requestURL())
        .translationLocale(getTranslationLocale(params.getLocale()))
        .typedAttributeValues(true)
        .paging(!offline)
        .pageSize(size)
        .pageOffset(Math.max(0, page - 1) * size)
        .translate(params.isTranslate())
        .total(params.isCountTotalPages())
        .absoluteUrls(params.isAbsoluteUrls())
        .headless(params.isHeadless())
        .references(!tree && !offline && params.isReferences())
        .anyFilter(params.getRootJunction() == Junction.Type.OR)
        .fields(GistQuery.Field.ofList(fields))
        .filters(GistQuery.Filter.ofList(params.getFilter()))
        .orders(GistQuery.Order.ofList(order))
        .build();
  }

  private static Locale getTranslationLocale(String locale) {
    return !locale.isEmpty()
        ? Locale.of(locale)
        : UserSettings.getCurrentSettings().getUserDbLocale();
  }

  private String getCollectionName(GistObjectListParams params, Class<?> elementType) {
    String name = params.getPageListName();
    if (name != null) return name;
    return collectionNames.computeIfAbsent(
        elementType, key -> schemaService.getSchema(key).getCollectionName());
  }

  private static final Property MATCH = new Property("match", new Type(Boolean.class), false);

  /**
   * @implNote When listing OU with their ancestors the main query just matches using filters as
   *     normal. Then a second query is made for all ancestors of the result page and added to the
   *     matches. This creates pages of varying size because the matches might already contain some
   *     ancestors of other matches. This also means the total matches still reflect matches to the
   *     filters, not including ancestors that might be added on top. This is simply the best we can
   *     do with reasonable performance and complexity.
   */
  private GistObjectList withAncestors(GistQuery query, GistObjectList matches)
      throws BadRequestException {
    // unfortunately we cannot avoid materializing the list in memory now
    List<Object[]> orgUnits = matches.values().toList();
    // - add match true to all matches
    List<Object[]> elements = orgUnits.stream().map(e -> prependMatchElement(e, true)).toList();
    // - isolate path column as list
    int pathIndex =
        query.getFieldNames().indexOf("path") + 1; // +1 as match column is inserted at 0
    List<String> ouPaths = elements.stream().map(e -> (String) e[pathIndex]).toList();
    // - make a list of all matching IDs
    List<String> matchesIds =
        ouPaths.stream().map(path -> path.substring(path.lastIndexOf('/') + 1)).toList();
    // - make a set of all IDs in any of the paths
    Set<String> ids =
        ouPaths.stream()
            .flatMap(path -> Stream.of(path.split("/")).filter(not(String::isEmpty)))
            .collect(toSet());
    matchesIds.forEach(
        ids::remove); // we already have those, what is left are the ancestors not yet fetched

    List<Property> properties = new ArrayList<>(matches.properties());
    properties.add(0, MATCH);
    // if ancestors are missing fetch them
    if (ids.isEmpty()) return new GistObjectList(matches.pager(), properties, elements.stream());
    Stream<Object[]> ancestors =
        gistService
            .exportObjectList(
                GistQuery.builder()
                    .elementType(query.getElementType())
                    .translate(query.isTranslate())
                    .translationLocale(query.getTranslationLocale())
                    .paging(false)
                    .filters(
                        List.of(
                            new GistQuery.Filter(
                                "id", GistQuery.Comparison.IN, ids.toArray(String[]::new))))
                    .fields(query.getFields())
                    .build())
            .values()
            .map(e -> prependMatchElement(e, false));
    // - inject ancestors into elements list (ordered by path)
    return new GistObjectList(
        matches.pager(),
        properties,
        Stream.concat(elements.stream(), ancestors)
            .sorted(comparing(e -> ((String) e[pathIndex]))));
  }

  private Object[] prependMatchElement(Object row, boolean match) {
    Object[] from = row instanceof Object[] arr ? arr : new Object[] {row};
    Object[] to = new Object[from.length + 1];
    System.arraycopy(from, 0, to, 1, from.length);
    to[0] = match;
    return to;
  }
}
