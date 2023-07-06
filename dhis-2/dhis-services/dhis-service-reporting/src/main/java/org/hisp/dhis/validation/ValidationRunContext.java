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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.lang3.Validate;
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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * This class keeps track of a validation analysis. It contains information about the initial params
 * of the analysis, the current state of the analysis and the final results of the analysis.
 *
 * @author Stian Sandvold
 */
@Component("org.hisp.dhis.validation.ValidationRunContext")
@Scope("prototype")
public class ValidationRunContext {
  public static final int ORG_UNITS_PER_TASK = 500;

  private Queue<ValidationResult> validationResults;

  private List<OrganisationUnit> orgUnits;

  private List<PeriodTypeExtended> periodTypeXs;

  private Set<CategoryOptionGroup> cogDimensionConstraints;

  private Set<CategoryOption> coDimensionConstraints;

  private Map<DimensionalItemId, DimensionalItemObject> itemMap;

  private ExpressionParams baseExParams;

  // -------------------------------------------------------------------------
  // Properties to configure analysis
  // -------------------------------------------------------------------------

  private CategoryOptionCombo attributeCombo;

  private CategoryOptionCombo defaultAttributeCombo;

  private int maxResults = 0;

  private boolean sendNotifications = false;

  private boolean persistResults = false;

  private MapMapMap<OrganisationUnit, ValidationRule, Period, List<ValidationResult>>
      initialValidationResults = new MapMapMap<>();

  private ValidationRunContext() {
    validationResults = new ConcurrentLinkedQueue<>();
  }

  // -------------------------------------------------------------------------
  // Return expression details if requested
  // -------------------------------------------------------------------------

  private ValidationRuleExpressionDetails validationRuleExpressionDetails;

  // -------------------------------------------------------------------------
  // Id-to-Object Caches
  // -------------------------------------------------------------------------

  private Map<Long, Period> periodIdMap = new ConcurrentHashMap<>();

  private Map<Long, CategoryOptionCombo> aocIdMap = new ConcurrentHashMap<>();

  private Map<String, CategoryOptionCombo> aocUidMap = new ConcurrentHashMap<>();

  // -------------------------------------------------------------------------
  // Setter method
  // -------------------------------------------------------------------------

  public void setValidationRuleExpressionDetails(
      ValidationRuleExpressionDetails validationRuleExpressionDetails) {
    this.validationRuleExpressionDetails = validationRuleExpressionDetails;
  }

  // -------------------------------------------------------------------------
  // Getter methods
  // -------------------------------------------------------------------------

  public CategoryOptionCombo getAttributeCombo() {
    return attributeCombo;
  }

  public CategoryOptionCombo getDefaultAttributeCombo() {
    return defaultAttributeCombo;
  }

  public int getMaxResults() {
    return maxResults;
  }

  public List<OrganisationUnit> getOrgUnits() {
    return orgUnits;
  }

  public List<PeriodTypeExtended> getPeriodTypeXs() {
    return periodTypeXs;
  }

  public Set<CategoryOptionGroup> getCogDimensionConstraints() {
    return cogDimensionConstraints;
  }

  public Set<CategoryOption> getCoDimensionConstraints() {
    return coDimensionConstraints;
  }

  public Map<DimensionalItemId, DimensionalItemObject> getItemMap() {
    return itemMap;
  }

  public ExpressionParams getBaseExParams() {
    return baseExParams;
  }

  public boolean isSendNotifications() {
    return sendNotifications;
  }

  public boolean isPersistResults() {
    return persistResults;
  }

  public Queue<ValidationResult> getValidationResults() {
    return validationResults;
  }

  public Map<Long, Period> getPeriodIdMap() {
    return periodIdMap;
  }

  public Map<Long, CategoryOptionCombo> getAocIdMap() {
    return aocIdMap;
  }

  public Map<String, CategoryOptionCombo> getAocUidMap() {
    return aocUidMap;
  }

  public ValidationRuleExpressionDetails getValidationRuleExpressionDetails() {
    return validationRuleExpressionDetails;
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

  // -------------------------------------------------------------------------
  // Builder
  // -------------------------------------------------------------------------

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    private final ValidationRunContext context;

    public Builder() {
      this.context = new ValidationRunContext();
    }

    /**
     * Builds the actual ValidationRunContext object configured with the builder
     *
     * @return a new ValidationParam based on the builders configuration
     */
    public ValidationRunContext build() {
      Validate.notNull(this.context.periodTypeXs, "Missing required property 'periodTypeXs'");
      Validate.notNull(this.context.orgUnits, "Missing required property 'orgUnits'");
      Validate.notNull(
          this.context.defaultAttributeCombo, "Missing required property 'defaultAttributeCombo'");

      // Preload the caches:
      context.aocIdMap.put(context.defaultAttributeCombo.getId(), context.defaultAttributeCombo);
      context.aocUidMap.put(context.defaultAttributeCombo.getUid(), context.defaultAttributeCombo);

      for (PeriodTypeExtended periodTypeX : context.periodTypeXs) {
        for (Period p : periodTypeX.getPeriods()) {
          context.periodIdMap.putIfAbsent(p.getId(), p);
        }
      }

      return this.context;
    }

    // -------------------------------------------------------------------------
    // Setter methods
    // -------------------------------------------------------------------------

    public Builder withOrgUnits(List<OrganisationUnit> orgUnits) {
      this.context.orgUnits = orgUnits;
      return this;
    }

    public Builder withPeriodTypeXs(List<PeriodTypeExtended> periodTypeXs) {
      this.context.periodTypeXs = periodTypeXs;
      return this;
    }

    /**
     * This is an optional constraint to which attributeCombo we should check
     *
     * @param attributeCombo
     */
    public Builder withAttributeCombo(CategoryOptionCombo attributeCombo) {
      this.context.attributeCombo = attributeCombo;
      return this;
    }

    /**
     * This is the default attributeOptionCombo which should always be present
     *
     * @param defaultAttributeCombo
     */
    public Builder withDefaultAttributeCombo(CategoryOptionCombo defaultAttributeCombo) {
      this.context.defaultAttributeCombo = defaultAttributeCombo;
      return this;
    }

    /**
     * Sets the max results to look for before concluding analysis.
     *
     * @param maxResults 0 means unlimited
     */
    public Builder withMaxResults(int maxResults) {
      this.context.maxResults = maxResults;
      return this;
    }

    public Builder withSendNotifications(boolean sendNotifications) {
      this.context.sendNotifications = sendNotifications;
      return this;
    }

    public Builder withCogDimensionConstraints(Set<CategoryOptionGroup> cogDimensionConstraints) {
      this.context.cogDimensionConstraints = cogDimensionConstraints;
      return this;
    }

    public Builder withCoDimensionConstraints(Set<CategoryOption> coDimensionConstraints) {
      this.context.coDimensionConstraints = coDimensionConstraints;
      return this;
    }

    public Builder withPersistResults(boolean persistResults) {
      this.context.persistResults = persistResults;
      return this;
    }

    public Builder withItemMap(Map<DimensionalItemId, DimensionalItemObject> itemMap) {
      this.context.itemMap = itemMap;
      return this;
    }

    public Builder withBaseExParams(ExpressionParams baseExParams) {
      this.context.baseExParams = baseExParams;
      return this;
    }

    public Builder withInitialResults(Collection<ValidationResult> results) {
      this.context.validationResults.addAll(results);

      results.forEach(
          validationResult -> {
            List<ValidationResult> res =
                context.initialValidationResults.getValue(
                    validationResult.getOrganisationUnit(),
                    validationResult.getValidationRule(),
                    validationResult.getPeriod());

            if (res == null) {
              res = new ArrayList<>();
            }

            res.add(validationResult);

            context.initialValidationResults.putEntry(
                validationResult.getOrganisationUnit(),
                validationResult.getValidationRule(),
                validationResult.getPeriod(),
                res);
          });

      return this;
    }
  }
}
