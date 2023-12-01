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
package org.hisp.dhis.predictor;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.system.util.MathUtils.addDoubleObjects;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.period.Period;

/**
 * Generator of disaggregated predictions.
 *
 * <p>If the predictor output category option combo (COC) is null, then all disaggregations (COCs)
 * of the output data element are computed independently, one prediction for each COC.
 *
 * <p>When making disaggregated predictions, values for any data elements in the expression (not
 * within data element operands) are fetched with all category option combos instead of summed at a
 * lower level. This allows them to be summed here with the COCs filtered as needed.
 *
 * <p>For each disaggregated prediction, exclude any COC values where an option, or any other option
 * from the same category, is an option for the output data element and not present in the
 * currently-predicted COC.
 *
 * <p>Examples:
 *
 * <ol>
 *   <li>If the output data element's category combo includes the category sex which has options F
 *       and M, then input data having a category option of F or M will be included only when it
 *       matches the output option (if the other options are also compatible) and will never be
 *       included when the output option does not match.
 *   <li>Suppose the purpose of the output data element is to count only positive test results and
 *       the output data element's category combo includes a category positiveOnly that has a single
 *       option POSITIVE. Suppose the input data COC has a category called result that has two
 *       values POSITIVE and NEGATIVE. In this case data with COC containing result=POSITIVE will be
 *       included in the output (if the other options are also compatible), but data with a COC
 *       containing RESULT=NEGATIVE will not (because NEGATIVE is part of a category that also
 *       includes POSITIVE, and POSITIVE is one of the options in the output data element's category
 *       combo.)
 *   <li>Suppose the input data COC has a category called result that has two values POSITIVE and
 *       NEGATIVE, and the output data element's category combo has no categories with either
 *       POSITIVE or NEGATIVE. In that case input data values with either POSITIVE or NEGATIVE will
 *       be included in the output (when the other options in the COC are compatible.)
 * </ol>
 *
 * @author Jim Grace
 */
public class PredictionDisaggregator {
  /**
   * The items we will fetch the data for, including fetching any existing predictions.
   *
   * <p>This will be a disaggregation prediction if the output data element has a non-default
   * category combination, and the output catOptionCombo is either null or default.
   *
   * <p>If this is a disaggregation prediction, then replace every data element with a wildcard data
   * element operand, to collect all category option combo values for that data element.
   */
  @Getter private final Set<DimensionalItemObject> disaggregatedItems;

  @Getter private final CategoryOptionCombo outputCombo;

  /** Whether we are generating disaggregated predictions. */
  private final boolean disagPredictions;

  /** The output category option combos. */
  private final Set<CategoryOptionCombo> outputCocs;

  /** All the output category combo's options' UIDs. */
  private final Set<String> allOutputOptions;

  /** Whether a combination of outputCOC and inputCOC should be included. */
  private final MapMap<Long, Long, Boolean> allowedCocs = new MapMap<>();

  public PredictionDisaggregator(
      Predictor predictor,
      Collection<DimensionalItemObject> items,
      CategoryOptionCombo defaultCategoryOptionCombo) {
    CategoryCombo outputCatCombo = predictor.getOutput().getCategoryCombo();

    this.outputCocs = outputCatCombo.getOptionCombos();
    this.allOutputOptions =
        outputCatCombo.getCategoryOptions().stream().map(CategoryOption::getUid).collect(toSet());

    outputCombo =
        (!outputCatCombo.isDefault()
                && (predictor.getOutputCombo() == null || predictor.getOutputCombo().isDefault()))
            ? null
            : firstNonNull(predictor.getOutputCombo(), defaultCategoryOptionCombo);

    this.disagPredictions = (outputCombo == null);

    Set<DimensionalItemObject> inputItems =
        new ImmutableSet.Builder<DimensionalItemObject>()
            .add(new DataElementOperand(predictor.getOutput(), outputCombo))
            .addAll(items)
            .build();

    if (disagPredictions) {
      inputItems = inputItems.stream().map(this::convertDataElement).collect(toSet());
    }
    this.disaggregatedItems = inputItems;
  }

  /**
   * If this is a disaggregation prediction, then replaces each prediction context with a list of
   * prediction contexts, one for each category option combination of the output data element.
   *
   * @param contexts list of disaggregation contexts
   * @return possibly expanded list of disaggregation contexts
   */
  public List<PredictionContext> getDisaggregateContexts(List<PredictionContext> contexts) {
    if (!disagPredictions) {
      return contexts;
    }

    return contexts.stream().map(this::disagregateContext).flatMap(Collection::stream).toList();
  }

  // -------------------------------------------------------------------------
  // Supportive Methods
  // -------------------------------------------------------------------------

  /**
   * Convert any DataElement to a DataElementOperand with a null COC (acting as a wildcard) so all
   * disaggregations of the data element will be collected independently for filtering. If this is a
   * DataElementOperand, just pass it through.
   */
  private DimensionalItemObject convertDataElement(DimensionalItemObject item) {
    if (item instanceof DataElement) {
      DataElementOperand deo = new DataElementOperand((DataElement) item, null);
      deo.setQueryMods(item.getQueryMods());
      return deo;
    }

    return item;
  }

  /**
   * Disaggregates a prediction context into a list of prediction contexts, one for each output
   * category option combo.
   */
  private List<PredictionContext> disagregateContext(PredictionContext context) {
    return outputCocs.stream().map(coc -> getDisaggregatedContext(context, coc)).toList();
  }

  /**
   * Generates a disaggregated context from an existing context but with values for one particular
   * disaggregation category option combination.
   */
  private PredictionContext getDisaggregatedContext(PredictionContext c, CategoryOptionCombo coc) {
    MapMap<Period, DimensionalItemObject, Object> periodValueMap =
        disaggregatePeriodValueMap(coc, c.getPeriodValueMap());

    return new PredictionContext(
        coc, c.getAttributeOptionCombo(), c.getOutputPeriod(), periodValueMap);
  }

  /**
   * Creates a period value map with the data values that are allowed for the given output COC.
   *
   * <p>Map values are created for each allowed data element operand and also for the data element
   * to which it belongs, in case one or both are needed.
   */
  private MapMap<Period, DimensionalItemObject, Object> disaggregatePeriodValueMap(
      CategoryOptionCombo coc, MapMap<Period, DimensionalItemObject, Object> periodValueMap) {
    MapMap<Period, DimensionalItemObject, Object> disMap = new MapMap<>();

    for (Map.Entry<Period, Map<DimensionalItemObject, Object>> e1 : periodValueMap.entrySet()) {
      Period period = e1.getKey();

      for (Map.Entry<DimensionalItemObject, Object> e2 : e1.getValue().entrySet()) {
        DimensionalItemObject item = e2.getKey();
        Object value = e2.getValue();

        // All the DEs should have been converted to DEOs:
        assert (item instanceof DataElementOperand);

        DataElementOperand deo = (DataElementOperand) item;

        if (isAllowed(coc, deo.getCategoryOptionCombo())) {
          disMap.putEntry(period, item, value);
          addIntoMap(disMap, period, deo.getDataElement(), value);
        }
      }
    }

    return disMap;
  }

  /**
   * Tests whether the input value COC is allowed for this output COC.
   *
   * <p>This uses a {@link MapMap} to cache the results of whether the COC is allowed. Initially a
   * {@link CachingMap} was used with a key as the concatenation of the COC UIDs. However, this is
   * used so intensively that a substantial amount of time was spent hashing the 22-character keys
   * to see if the key existed in the map. The current code is much faster.
   */
  private boolean isAllowed(CategoryOptionCombo outCoc, CategoryOptionCombo inCoc) {
    Boolean cached = allowedCocs.getValue(outCoc.getId(), inCoc.getId());

    if (cached != null) {
      return cached;
    }

    boolean computed = computeAllowed(outCoc, inCoc);

    allowedCocs.putEntry(outCoc.getId(), inCoc.getId(), computed);

    return computed;
  }

  /**
   * Computes whether the input COC is allowed for this output COC.
   *
   * <p>For each input category, if any of the category options are found in the output, then this
   * category is subject to filtering.
   *
   * <p>If the category is subject to filtering, then only allow data where the input category
   * option is present in the output option combo.
   */
  private boolean computeAllowed(CategoryOptionCombo outCoc, CategoryOptionCombo inCoc) {
    for (Category inC : inCoc.getCategoryCombo().getCategories()) {
      if (outputIncludesAnyOptionFromCategory(inC)
          && !outputComboIncludesCategoryOption(outCoc, inC, inCoc)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Tests whether the output category options include at least one option from the given category.
   */
  private boolean outputIncludesAnyOptionFromCategory(Category inC) {
    for (CategoryOption inCo : inC.getCategoryOptions()) {
      if (allOutputOptions.contains(inCo.getUid())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Tests whether the output category option combo for this prediction contains the given option
   * from a given input category.
   */
  private boolean outputComboIncludesCategoryOption(
      CategoryOptionCombo outCoc, Category inC, CategoryOptionCombo inCoc) {
    for (CategoryOption inCo : inCoc.getCategoryOptions()) {
      // Find the input option that matches this input category:
      if (inC.getCategoryOptions().contains(inCo)) {
        return outCoc.getCategoryOptions().contains(inCo);
      }
    }

    return true;
  }

  /**
   * Adds the input value into the map. If the value already exists in the map, adds the new value
   * to the existing value.
   */
  private void addIntoMap(
      MapMap<Period, DimensionalItemObject, Object> disMap,
      Period period,
      DimensionalItemObject item,
      Object value) {
    Object valueSoFar = disMap.getValue(period, item);

    Object valueToStore = (valueSoFar == null) ? value : addDoubleObjects(value, valueSoFar);

    disMap.putEntry(period, item, valueToStore);
  }
}
