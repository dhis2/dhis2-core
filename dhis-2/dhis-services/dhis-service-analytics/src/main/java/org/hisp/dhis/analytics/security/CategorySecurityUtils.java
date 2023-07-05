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
package org.hisp.dhis.analytics.security;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.program.Program;

/**
 * Provides specific methods responsible checking or matching {@link Category} and {@link
 * DimensionalObject} objects.
 */
@NoArgsConstructor(access = PRIVATE)
public class CategorySecurityUtils {
  /**
   * Iterates through all {@link Category} in the given {@link Program} and returns the ones that do
   * not match any object in the list of {@link DimensionalObject}.
   *
   * @param program the {@link Program} where we get the categories from.
   * @param dimensionalObjects the list of {@link DimensionalObject} objects.
   * @return the list of {@link Category} matched.
   */
  public static List<Category> getConstrainedCategories(
      Program program, List<DimensionalObject> dimensionalObjects) {
    List<Program> programs =
        Optional.ofNullable(program).map(Collections::singletonList).orElse(List.of());

    return getConstrainedCategories(programs, dimensionalObjects);
  }

  /**
   * Iterates through all {@link Category} in the given list of {@link Program} and returns the ones
   * that do not match any object in the list of {@link DimensionalObject}.
   *
   * @param programs the list of {@link Program}. Where we get the categories from.
   * @param dimensionalObjects the list of {@link DimensionalObject} objects.
   * @return the list of {@link Category} matched.
   */
  public static List<Category> getConstrainedCategories(
      List<Program> programs, List<DimensionalObject> dimensionalObjects) {
    return programs.stream()
        .filter(Program::hasNonDefaultCategoryCombo)
        .map(Program::getCategoryCombo)
        .map(CategoryCombo::getCategories)
        .flatMap(Collection::stream)
        /*
         * If the user has selected a category, we do not want to apply any
         * constraints.
         */
        .filter(category -> !matchDimensionsForCategory(category, dimensionalObjects))
        .collect(toList());
  }

  /**
   * Checks if the given list of {@link DimensionalObject} has elements of type {@link
   * DimensionType.CATEGORY} and its uid matches the given {@link Category} uid. Also checks that
   * each {@link DimensionalObject} object has category options associated.
   *
   * @param category the {@link Category} to check.
   * @param dimensionalObjects the list of {@link DimensionalObject} objects.
   * @return true if any match is found in the list of {@link DimensionalObject}.
   */
  private static boolean matchDimensionsForCategory(
      Category category, List<DimensionalObject> dimensionalObjects) {
    return dimensionalObjects.stream()
        .anyMatch(dimensionalObject -> matchDimensionForCategory(category, dimensionalObject));
  }

  /**
   * Checks if the given {@link DimensionalObject} is of type {@link DimensionType.CATEGORY} and its
   * uid matches the given {@link Category} uid. Also checks that the {@link DimensionalObject} has
   * category options.
   *
   * @param category the {@link Category} to match.
   * @param dimensionalObject the {@link DimensionalObject} object to match.
   * @return true if the conditions match, false otherwise.
   */
  private static boolean matchDimensionForCategory(
      Category category, DimensionalObject dimensionalObject) {
    return dimensionalObject.getDimensionType() == DimensionType.CATEGORY
        && dimensionalObject.getUid().equals(category.getUid())
        && isNotEmpty(dimensionalObject.getItems()); // Items represent the category options.
  }
}
