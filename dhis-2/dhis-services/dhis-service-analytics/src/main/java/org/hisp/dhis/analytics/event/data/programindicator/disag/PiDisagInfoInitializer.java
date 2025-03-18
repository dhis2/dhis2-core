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
package org.hisp.dhis.analytics.event.data.programindicator.disag;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.hisp.dhis.commons.collection.ListUtils.union;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.program.ProgramCategoryMapping;
import org.hisp.dhis.program.ProgramCategoryMappingValidator;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.query.QueryException;
import org.springframework.stereotype.Component;

/**
 * Methods to initialize the program indicator disaggregation info {@link PiDisagInfo}
 *
 * @author Jim Grace
 */
@Component
@RequiredArgsConstructor
public class PiDisagInfoInitializer {

  private final ProgramCategoryMappingValidator mappingValidator;

  /**
   * Returns {@link EventQueryParams} with additional information to process program indicator
   * disaggregation if needed and available
   *
   * @param params the {@link EventQueryParams}
   * @return params with additional information for program indicator disaggregation if needed
   */
  public EventQueryParams getParamsWithDisaggregationInfo(EventQueryParams params) {
    if (!params.hasProgramIndicatorDimension()
        || disaggregatedCategoriesAreNotNeeded(params)
        || params.hasPiDisagInfo()) {
      return params;
    }

    PiDisagInfo info = getPiDisagInfo(params);
    if (info.categoryMappings.isEmpty()) {
      return params;
    }

    return new EventQueryParams.Builder(params).withPiDisagInfo(info).build();
  }

  /**
   * Checks if disaggregated categories *may* be needed: either if they are requested in the params,
   * or if the program indicator has a non-default category combo and/or attribute combo. This just
   * checks if they may be needed, not if the program indicator can supply them.
   *
   * @param params the {@link EventQueryParams}
   * @return whether disaggregated categories are not needed
   */
  public boolean disaggregatedCategoriesAreNotNeeded(EventQueryParams params) {
    ProgramIndicator pi = params.getProgramIndicator();
    CategoryCombo cc = pi.getCategoryCombo();
    CategoryCombo ac = pi.getAttributeCombo();

    return !params.hasCategories() && cc.isDefault() && ac.isDefault();
  }

  /**
   * Creates all the info we will need during analytics query runs to process program indicator
   * disaggregations.
   *
   * @param params the {@link EventQueryParams}
   * @return the {@link PiDisagInfo}
   */
  public PiDisagInfo getPiDisagInfo(EventQueryParams params) {
    ProgramIndicator pi = params.getProgramIndicator();

    Set<ProgramCategoryMapping> allMappings;
    try {
      allMappings = mappingValidator.getAndValidateProgramIndicatorCategoryMappings(pi);
    } catch (ConflictException e) {
      throw new QueryException(e.getMessage());
    }

    Set<String> dimensionCategories = getDimensionCategories(params, allMappings);
    List<String> cocCategories = getCocCategories(params);
    Map<String, ProgramCategoryMapping> categoryMappings =
        allMappings.stream()
            .filter(
                m ->
                    dimensionCategories.contains(m.getCategoryId())
                        || cocCategories.contains(m.getCategoryId()))
            .collect(toMap(ProgramCategoryMapping::getCategoryId, identity()));

    CategoryCombo cc = pi.getCategoryCombo();
    CategoryCombo ac = pi.getAttributeCombo();

    Map<String, String> cocResolver = cc.isDefault() ? emptyMap() : getResolver(cc);
    Map<String, String> aocResolver = ac.isDefault() ? emptyMap() : getResolver(ac);

    return PiDisagInfo.builder()
        .dimensionCategories(dimensionCategories)
        .cocCategories(cocCategories)
        .categoryMappings(categoryMappings)
        .cocResolver(cocResolver)
        .aocResolver(aocResolver)
        .build();
  }

  /**
   * Generates the set of categories that will be served for dimensions by pi disaggregation
   *
   * @param params the {@link EventQueryParams}
   * @param allMappings All the category mappings
   * @return the set of categories that will be served for dimensions by pi disaggregation
   */
  private Set<String> getDimensionCategories(
      EventQueryParams params, Set<ProgramCategoryMapping> allMappings) {

    return allMappings.stream()
        .map(ProgramCategoryMapping::getCategoryId)
        .filter(params::hasDimensionId)
        .collect(toUnmodifiableSet());
  }

  /**
   * Finds all the categories that are needed for COC and/or AOC generation
   *
   * @param params the {@link EventQueryParams}
   * @return all the categories that are needed for COC and/or AOC generation
   */
  public List<String> getCocCategories(EventQueryParams params) {

    ProgramIndicator pi = params.getProgramIndicator();
    CategoryCombo cc = pi.getCategoryCombo();
    CategoryCombo ac = pi.getAttributeCombo();

    Set<String> allCocCategories =
        union(
                cc.isDefault() ? emptyList() : cc.getCategories(),
                ac.isDefault() ? emptyList() : ac.getCategories())
            .stream()
            .map(Category::getUid)
            .collect(toSet());

    return allCocCategories.stream().filter(id -> !params.hasDimensionId(id)).toList();
  }

  /**
   * Creates a resolver that resolves category option combos for every option combo in a category
   * combo. This maps:
   *
   * <pre>{@code
   * key: a concatenated string of the category option combo UIDs in sorted order
   * value: the UID of the category option combo
   *
   * For example:
   * CategoryOptionCombo UID: "RieKaipo0ie"
   * Option 1 UID: "rom0yeJ7oiw"
   * Option 2 UID: "kcoo9uow2Ee"
   * Key->value: "kcoo9uow2Eerom0yeJ7oiw" -> "RieKaipo0ie"
   *
   * }</pre>
   */
  public Map<String, String> getResolver(CategoryCombo cc) {
    return cc.getOptionCombos().stream()
        .collect(
            toMap(
                coc ->
                    coc.getCategoryOptions().stream()
                        .map(CategoryOption::getUid)
                        .sorted()
                        .collect(joining()),
                CategoryOptionCombo::getUid));
  }
}
