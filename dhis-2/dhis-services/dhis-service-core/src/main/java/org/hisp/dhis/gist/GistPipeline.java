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

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.UserSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Comparator.comparing;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

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
   * @param input describes the slice of data to export to JSON
   * @param out target to write the JSON to
   */
  @Transactional(readOnly = true)
  public void listAsJson(@Nonnull GistObjectList.Input input, @Nonnull Supplier<OutputStream> out)
      throws BadRequestException {
    GistQuery query = createGistQuery(input);
    GistObjectList list = listObjects(input, query);
    GistOutput.toJson(createObjectListOutput(input, list), out.get());
  }

  @Transactional(readOnly = true)
  public void listAsCsv(@Nonnull GistObjectList.Input input, @Nonnull Supplier<OutputStream> out)
      throws BadRequestException {
    GistQuery query = createGistQuery(input).toBuilder().typedAttributeValues(false).build();
    GistObjectList list = listObjects(input, query);
    GistOutput.toCsv(createObjectListOutput(input, list), out.get());
  }

  private GistObjectList listObjects(GistObjectList.Input input, GistQuery query) {
    GistObjectList list = gistService.listObjects(query);
    return input.elementType() == OrganisationUnit.class && input.params().isOrgUnitsTree()
        ? withAncestors(query, list)
        : list;
  }

  @Nonnull
  private GistObjectList.Output createObjectListOutput(
      GistObjectList.Input input, GistObjectList list) {
    return new GistObjectList.Output(
        input.params().headless,
        list.pager(),
        getCollectionName(input),
        list.paths(),
        list.valueTypes(),
        list.values());
  }

  @Transactional(readOnly = true)
  public void listPropertyAsJson(@Nonnull OutputStream out) {

  }

  @Transactional(readOnly = true)
  public void listPropertyAsCsv(@Nonnull OutputStream out) {

  }

  private GistQuery createGistQuery(GistObjectList.Input input) throws BadRequestException {
    String locale = input.params().getLocale();
    Locale translationLocale =
        !locale.isEmpty()
            ? Locale.forLanguageTag(locale)
            : UserSettings.getCurrentSettings().getUserDbLocale();
    return GistQuery.builder()
        .elementType(input.elementType())
        .autoType(input.params().getAuto(input.autoDefault()))
        .contextRoot(input.contextRoot())
        .requestURL(input.requestURL())
        .translationLocale(translationLocale)
        .typedAttributeValues(true)
        .build()
        .with(input.params());
  }

  private String getCollectionName(GistObjectList.Input input) {
    String name = input.params().getPageListName();
    if (name != null) return name;
    return collectionNames.computeIfAbsent(
        input.elementType(), key -> schemaService.getSchema(key).getCollectionName());
  }

  /**
   * @implNote When listing OU with their ancestors the main query just matches using filters as
   *     normal. Then a second query is made for all ancestors of the result page and added to the
   *     matches. This creates pages of varying size because the matches might already contain some
   *     ancestors of other matches. This also mean the total matches still reflect matches to the
   *     filters, not including ancestors that might be added on top. This is simply the best we can
   *     do with reasonable performance and complexity.
   */
  private GistObjectList withAncestors(GistQuery query, GistObjectList matches) {
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

    List<String> paths = new ArrayList<>(matches.paths());
    paths.add(0, "match");
    List<GistObjectList.Type> valueTypes = new ArrayList<>(matches.valueTypes());
    valueTypes.add(0, new GistObjectList.Type(Boolean.class));
    // if ancestors are missing fetch them
    if (ids.isEmpty()) return new GistObjectList(matches.pager(), paths, valueTypes, elements.stream());
    List<Object[]> ancestors =
        gistService
            .gist(
                GistQuery.builder()
                    .elementType(query.getElementType())
                    .translate(query.isTranslate())
                    .translationLocale(query.getTranslationLocale())
                    .paging(false)
                    .filters(List.of(new GistQuery.Filter("id", GistQuery.Comparison.IN, ids.toArray(String[]::new))))
                    .fields(query.getFields())
                    .build())
            .map(e -> prependMatchElement(e, false))
            .toList();
    // - inject ancestors into elements list (ordered by path)
    return new GistObjectList(
        matches.pager(),
        paths,
        valueTypes,
        Stream.concat(elements.stream(), ancestors.stream())
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
