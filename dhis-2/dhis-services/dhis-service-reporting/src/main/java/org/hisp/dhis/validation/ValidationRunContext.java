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
package org.hisp.dhis.validation;

import static java.util.Objects.requireNonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.*;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryOptionGroup;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * This class keeps track of a validation analysis. It contains information about the initial params
 * of the analysis, the current state of the analysis and the final results of the analysis.
 *
 * @author Jim Grace (original)
 * @author Stian Sandvold (persistence)
 */
@Getter
@Builder(setterPrefix = "with", builderClassName = "Builder", builderMethodName = "newBuilder")
public class ValidationRunContext {
  public static final int ORG_UNITS_PER_TASK = 500;

  private final Queue<ValidationResult> validationResults = new ConcurrentLinkedQueue<>();

  private final List<OrganisationUnit> orgUnits;

  private final List<PeriodTypeExtended> periodTypeXs;

  private final Set<CategoryOptionGroup> cogDimensionConstraints;

  private final Set<CategoryOption> coDimensionConstraints;

  private final Map<DimensionalItemId, DimensionalItemObject> itemMap;

  private final ExpressionParams baseExParams;

  // -------------------------------------------------------------------------
  // Properties to configure analysis
  // -------------------------------------------------------------------------

  private final CategoryOptionCombo attributeCombo;

  private final CategoryOptionCombo defaultAttributeCombo;

  @lombok.Builder.Default private int maxResults = 0;

  @lombok.Builder.Default private boolean sendNotifications = false;

  @lombok.Builder.Default private boolean persistResults = false;

  private final MapMapMap<OrganisationUnit, ValidationRule, Period, List<ValidationResult>>
      initialValidationResults = new MapMapMap<>();

  // -------------------------------------------------------------------------
  // Return expression details if requested
  // -------------------------------------------------------------------------

  @Setter private ValidationRuleExpressionDetails validationRuleExpressionDetails;

  // -------------------------------------------------------------------------
  // Id-to-Object Caches
  // -------------------------------------------------------------------------

  private final Map<Long, Period> periodIdMap = new ConcurrentHashMap<>();

  private final Map<Long, CategoryOptionCombo> aocIdMap = new ConcurrentHashMap<>();

  private final Map<String, CategoryOptionCombo> aocUidMap = new ConcurrentHashMap<>();

  private ValidationRunContext(
      List<OrganisationUnit> orgUnits,
      List<PeriodTypeExtended> periodTypeXs,
      Set<CategoryOptionGroup> cogDimensionConstraints,
      Set<CategoryOption> coDimensionConstraints,
      Map<DimensionalItemId, DimensionalItemObject> itemMap,
      ExpressionParams baseExParams,
      CategoryOptionCombo attributeCombo,
      CategoryOptionCombo defaultAttributeCombo,
      int maxResults,
      boolean sendNotifications,
      boolean persistResults,
      ValidationRuleExpressionDetails validationRuleExpressionDetails) {
    this.orgUnits = orgUnits;
    this.periodTypeXs = periodTypeXs;
    this.cogDimensionConstraints = cogDimensionConstraints;
    this.coDimensionConstraints = coDimensionConstraints;
    this.itemMap = itemMap;
    this.baseExParams = baseExParams;
    this.attributeCombo = attributeCombo;
    this.defaultAttributeCombo = defaultAttributeCombo;
    this.maxResults = maxResults;
    this.sendNotifications = sendNotifications;
    this.persistResults = persistResults;
    this.validationRuleExpressionDetails = validationRuleExpressionDetails;

    requireNonNull(periodTypeXs, "Missing required property 'periodTypeXs'");
    requireNonNull(orgUnits, "Missing required property 'orgUnits'");
    requireNonNull(defaultAttributeCombo, "Missing required property 'defaultAttributeCombo'");

    preloadCaches();
  }

  private void preloadCaches() {
    aocIdMap.put(defaultAttributeCombo.getId(), defaultAttributeCombo);
    aocUidMap.put(defaultAttributeCombo.getUid(), defaultAttributeCombo);

    for (PeriodTypeExtended periodTypeX : periodTypeXs) {
      for (Period p : periodTypeX.getPeriods()) {
        periodIdMap.putIfAbsent(p.getId(), p);
      }
    }
  }

  // -------------------------------------------------------------------------
  // Logic
  // -------------------------------------------------------------------------

  public boolean skipValidationOfTuple(
      OrganisationUnit organisationUnit,
      ValidationRule validationRule,
      Period period,
      String attributeOptionCombo,
      int dayInPeriod) {
    List<ValidationResult> validationResultList =
        initialValidationResults.getValue(organisationUnit, validationRule, period);

    if (validationResultList != null) {
      for (ValidationResult vr : validationResultList) {
        if (vr.getAttributeOptionCombo().getUid().equals(attributeOptionCombo)
            && vr.getDayInPeriod() == dayInPeriod) {
          return true;
        }
      }
    }

    return false;
  }

  public int getNumberOfTasks() {
    return (orgUnits.size() + ORG_UNITS_PER_TASK - 1) / ORG_UNITS_PER_TASK;
  }

  public boolean isAnalysisComplete() {
    return validationResults.size() >= maxResults;
  }

  public boolean processExpressionDetails() {
    return validationRuleExpressionDetails != null;
  }

  public ValidationRunContext addInitialResults(Collection<ValidationResult> results) {
    validationResults.addAll(results);

    results.forEach(
        validationResult -> {
          List<ValidationResult> res =
              initialValidationResults.getValue(
                  validationResult.getOrganisationUnit(),
                  validationResult.getValidationRule(),
                  validationResult.getPeriod());

          if (res == null) {
            res = new ArrayList<>();
          }

          res.add(validationResult);

          initialValidationResults.putEntry(
              validationResult.getOrganisationUnit(),
              validationResult.getValidationRule(),
              validationResult.getPeriod(),
              res);
        });

    return this;
  }
}
