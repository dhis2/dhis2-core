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

import static com.google.common.collect.Iterables.toArray;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.common.CodeGenerator.UID_CODE_SIZE;
import static org.hisp.dhis.common.DimensionConstants.DATA_COLLAPSED_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_WILDCARD;
import static org.hisp.dhis.feedback.ErrorCode.E4078;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.common.ErrorCodeException;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.util.Assert;

/**
 * Data handler for program indicator disaggregation
 *
 * @author Jim Grace
 */
public class PiDisagDataHandler {
  PiDisagDataHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Adds the CategoryOptionCombo and AttributeOptionCombo to a list of row values if this is a
   * program indicator and the values are to be output ot a data value set.
   *
   * <p>Returns true if ANY of the following conditions are met:
   *
   * <ul>
   *   <li>This is not a program indicator
   *   <li>This is a program indicator that has only default COC and AOC disaggregations
   *   <li>This is a disaggregated program indicator and the non-default COC and/or AOC can be found
   *       (these dimensions added to the value if data value set output)
   * </ul>
   *
   * <p>Returns false if BOTH of the following conditions are met:
   *
   * <ul>
   *   <li>This is a program indicator with non-default COC and/or AOC disaggregations
   *   <li>The disaggregated COC and/or AOC cannot be found using the disaggregation filters
   * </ul>
   *
   * @param params the {@link EventQueryParams}
   * @param grid the grid
   * @param row the values to which to add COC and AOC
   * @param rowSet the {@link SqlRowSet} with SQL query values
   * @return true if the row is valid to store in the grid
   */
  public static boolean addCocAndAoc(
      EventQueryParams params, Grid grid, List<Object> row, SqlRowSet rowSet) {
    PiDisagInfo info = params.getPiDisagInfo();
    if (info == null || (info.cocResolver.isEmpty() && info.aocResolver.isEmpty())) {
      return true;
    }

    ProgramIndicator pi = params.getProgramIndicator();
    String coc = getCocOrAoc(params, rowSet, pi.getCategoryCombo(), info.getCocResolver());
    String aoc = getCocOrAoc(params, rowSet, pi.getAttributeCombo(), info.getAocResolver());

    // Don't store rows if they are missing coc or aoc
    if (coc == null || aoc == null) {
      return false;
    }

    // If we are producing a DataValueSet, add the coc and aoc to the row
    if (params.isOutputFormat(DATA_VALUE_SET)) {
      addCocAndAocToRow(grid, row, coc, aoc);
    }

    return true;
  }

  /**
   * Gets the COC or AOC for the row. If the resolver is empty, this means that the COC or AOC is
   * not disaggregated so return the wildcard. Otherwise, get the COC or AOC (if present) from the
   * category column(s) in the rowset.
   */
  private static String getCocOrAoc(
      EventQueryParams params,
      SqlRowSet rowSet,
      CategoryCombo categoryCombo,
      Map<String, String> resolver) {
    return resolver.isEmpty()
        ? SYMBOL_WILDCARD
        : getOptionCombo(params, rowSet, categoryCombo, resolver);
  }

  /** Adds the CategoryOptionCombo and AttributeOptionCombo to the row values */
  private static void addCocAndAocToRow(Grid grid, List<Object> row, String coc, String aoc) {
    int dxInx = grid.getIndexOfHeader(DATA_COLLAPSED_DIM_ID);
    Assert.isTrue(dxInx >= 0, "Data dimension index must be zero or positive");

    String compositeVal =
        row.get(dxInx)
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + coc
            + COMPOSITE_DIM_OBJECT_PLAIN_SEP
            + aoc;
    row.set(dxInx, compositeVal);
  }

  /** Gets a disaggregated categoryOptionCombo or attributeOptionCombo */
  private static String getOptionCombo(
      EventQueryParams params,
      SqlRowSet rowSet,
      CategoryCombo categoryCombo,
      Map<String, String> resolver) {
    PiDisagInfo piDisagInfo = params.getPiDisagInfo();
    Assert.notNull(piDisagInfo, "piDisagInfo is null within getOptionCombo");

    String orderedOptionCombos = getOrderedOptionCombos(params, rowSet, categoryCombo);
    if (orderedOptionCombos == null) {
      return null;
    }

    String optionComboUid = resolver.get(orderedOptionCombos);
    Assert.notNull(
        optionComboUid,
        String.format(
            "Couldn't resolve orderedOptionCombo %s for PI %s, catCombo %s, resolver %s",
            orderedOptionCombos,
            params.getProgramIndicator().getUid(),
            categoryCombo.getUid(),
            resolver));

    return optionComboUid;
  }

  /**
   * Gets the ordered category combos from SQL query return information. This consists of the
   * category option UIDs appended in their sorted order.
   */
  private static String getOrderedOptionCombos(
      EventQueryParams params, SqlRowSet rowSet, CategoryCombo categoryCombo) {
    List<String> options = new ArrayList<>();
    for (Category category : categoryCombo.getCategories()) {
      String option = rowSet.getString(category.getUid());
      if (StringUtils.isEmpty(option)) {
        return null;
      }
      if (option.length() > UID_CODE_SIZE) {
        String[] multiOptions =
            toArray(Splitter.fixedLength(UID_CODE_SIZE).split(option), String.class);
        throw new ErrorCodeException(
            E4078,
            String.join(",", multiOptions),
            params.getProgramIndicator().getUid(),
            params.getProgramIndicator().getName(),
            categoryCombo.getUid(),
            categoryCombo.getName(),
            category.getUid(),
            category.getName());
      }
      options.add(option);
    }
    return options.stream().sorted().collect(Collectors.joining());
  }
}
