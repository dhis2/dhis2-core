/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.validation;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;

/**
 * @author Stian Sandvold
 */
public interface ValidationResultService {
  /**
   * Saves a set of ValidationResults in a bulk action.
   *
   * @param validationResults a collection of validation results.
   */
  void saveValidationResults(Collection<ValidationResult> validationResults);

  /**
   * Returns a list of all existing ValidationResults.
   *
   * @return a list of validation results.
   */
  List<ValidationResult> getAllValidationResults();

  /**
   * Returns a list of all ValidationResults where notificationSent is false
   *
   * @return a list of validation results.
   */
  List<ValidationResult> getAllUnReportedValidationResults();

  /**
   * Deletes the validationResult.
   *
   * @param validationResult the validation result.
   */
  void deleteValidationResult(ValidationResult validationResult);

  /**
   * Deletes all {@link ValidationResult}s that match the request criteria.
   *
   * @param request Criteria a {@link ValidationResult} should match to be deleted
   */
  void deleteValidationResults(ValidationResultsDeletionRequest request);

  /**
   * Updates a list of ValidationResults.
   *
   * @param validationResults validationResults to update.
   */
  void updateValidationResults(Set<ValidationResult> validationResults);

  /**
   * Returns the ValidationResult with the given id, or null if no validation result exists with
   * that id.
   *
   * @param id the validation result identifier.
   * @return a validation result.
   */
  ValidationResult getById(long id);

  List<ValidationResult> getValidationResults(ValidationResultQuery query)
      throws IllegalQueryException;

  long countValidationResults(ValidationResultQuery query) throws IllegalQueryException;

  List<ValidationResult> getValidationResults(
      OrganisationUnit orgUnit,
      boolean includeOrgUnitDescendants,
      Collection<ValidationRule> validationRules,
      Collection<Period> periods);

  /**
   * Persists the violations found by an analysis. A violation already persisted for the same rule,
   * period, org unit, attribute option combo and day in the period keeps its row and notification
   * state; its left and right side values are updated. Other violations are saved. Persisted
   * violations that were not found are left unchanged.
   *
   * @param validationResults the violations found by the analysis
   */
  void upsertValidationResults(Collection<ValidationResult> validationResults);

  /**
   * Brings the persisted results of the given scope in line with the results of an analysis of that
   * scope.
   *
   * <ul>
   *   <li>Results are upserted as by {@link #upsertValidationResults(Collection)}. A violation that
   *       was already notified for the same rule, period, org unit and attribute option combo on an
   *       earlier day of the period keeps that notification state.
   *   <li>If {@code removeResolved} is true, persisted results of the scope that the analysis did
   *       not find are deleted: resolved violations, and earlier days of ongoing violations. Pass
   *       false when the analysis was incomplete.
   * </ul>
   *
   * @param orgUnit the root of the analysed org units, or null for all org units
   * @param includeOrgUnitDescendants whether the descendants of {@code orgUnit} were analysed
   * @param validationRules the analysed rules
   * @param periods the analysed periods
   * @param currentResults the violations found by the analysis
   * @param removeResolved whether to delete persisted results that were not found
   */
  void replaceValidationResults(
      OrganisationUnit orgUnit,
      boolean includeOrgUnitDescendants,
      Collection<ValidationRule> validationRules,
      Collection<Period> periods,
      Collection<ValidationResult> currentResults,
      boolean removeResolved);
}
