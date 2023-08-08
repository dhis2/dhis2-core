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
}
