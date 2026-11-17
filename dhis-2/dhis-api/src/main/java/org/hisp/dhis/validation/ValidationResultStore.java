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
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;

/**
 * @author Stian Sandvold
 */
public interface ValidationResultStore extends GenericStore<ValidationResult> {
  List<ValidationResult> getAllUnreportedValidationResults();

  ValidationResult getById(long id);

  List<ValidationResult> query(ValidationResultQuery query);

  int count(ValidationResultQuery query);

  List<ValidationResult> getValidationResults(
      OrganisationUnit orgUnit,
      boolean includeOrgUnitDescendants,
      Collection<ValidationRule> validationRules,
      Collection<Period> periods);

  void delete(ValidationResultsDeletionRequest request);

  /**
   * Inserts the given results, or updates the left and right side values of a result already
   * persisted for the same rule, period, org unit, attribute option combo and day in the period.
   * The notification state of an existing result is kept; a new result is inserted with its own.
   *
   * @param validationResults the results, with persisted periods
   */
  void upsert(Collection<ValidationResult> validationResults);

  /**
   * Deletes the validation results with the given ids.
   *
   * @param ids the ids of the results to delete
   */
  void deleteByIds(Collection<Long> ids);

  /**
   * A persisted validation result, reduced to its key, id and notification state.
   *
   * @param id the id of the result
   * @param rule the id of its validation rule
   * @param period the id of its period
   * @param orgUnit the id of its org unit
   * @param attributeOptionCombo the id of its attribute option combo
   * @param dayInPeriod the day in the period on which it was found
   * @param notificationSent whether a notification was sent for it
   */
  record Row(
      long id,
      long rule,
      long period,
      long orgUnit,
      long attributeOptionCombo,
      int dayInPeriod,
      boolean notificationSent) {}

  /**
   * Returns the persisted results of the given scope as {@link Row}s, without loading them as
   * entities.
   *
   * @param orgUnit the root org unit, or null for all org units
   * @param includeOrgUnitDescendants whether to include the descendants of {@code orgUnit}
   * @param validationRules the rules
   * @param periods the periods, which must be persisted
   * @return the rows of the scope
   */
  List<Row> getRows(
      OrganisationUnit orgUnit,
      boolean includeOrgUnitDescendants,
      Collection<ValidationRule> validationRules,
      Collection<Period> periods);
}
