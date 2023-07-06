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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;

/**
 * @author Margrethe Store
 */
public interface ValidationRuleService extends ValidationRuleDataIntegrityProvider {
  String ID = ValidationRuleService.class.getName();

  // -------------------------------------------------------------------------
  // ValidationRule
  // -------------------------------------------------------------------------

  /**
   * Save a ValidationRule to the database.
   *
   * @param validationRule the ValidationRule to save.
   * @return the generated unique identifier for the ValidationRule.
   */
  long saveValidationRule(ValidationRule validationRule);

  /**
   * Update a ValidationRule to the database.
   *
   * @param validationRule the ValidationRule to update.
   */
  void updateValidationRule(ValidationRule validationRule);

  /**
   * Delete a validation rule with the given identifiers from the database.
   *
   * @param validationRule the ValidationRule to delete.
   */
  void deleteValidationRule(ValidationRule validationRule);

  /**
   * Get ValidationRule with the given identifier.
   *
   * @param id the unique identifier of the ValidationRule.
   * @return the ValidationRule or null if it doesn't exist.
   */
  ValidationRule getValidationRule(long id);

  /**
   * Get ValidationRule with the given uid.
   *
   * @param uid the unique identifier of the ValidationRule.
   * @return the ValidationRule or null if it doesn't exist.
   */
  ValidationRule getValidationRule(String uid);

  /**
   * Get all validation rules.
   *
   * @return a List of ValidationRule or null if there are no validation rules.
   */
  List<ValidationRule> getAllValidationRules();

  /**
   * Get all validation rules for form validation.
   *
   * @return a List of ValidationRule or null if there are none for form validation.
   */
  List<ValidationRule> getAllFormValidationRules();

  /**
   * Get a validation rule with the given name.
   *
   * @param name the name of the validation rule.
   */
  ValidationRule getValidationRuleByName(String name);

  /**
   * Get data elements part of the left side and right side expressions of the given validation
   * rule.
   *
   * @param validationRule the validation rule.
   * @return a set of data elements.
   */
  Set<DataElement> getDataElements(ValidationRule validationRule);

  /**
   * Returns all form validation rules for validating a data set.
   *
   * @param dataSet the data set to validate.
   * @return all validation rules which apply to that data set.
   */
  Collection<ValidationRule> getValidationRulesForDataSet(DataSet dataSet);

  /**
   * Returns all ValidationRules which have associated ValidationNotificationTemplates.
   *
   * @return a List of ValidationRule.
   */
  List<ValidationRule> getValidationRulesWithNotificationTemplates();

  // -------------------------------------------------------------------------
  // ValidationRuleGroup
  // -------------------------------------------------------------------------

  /**
   * Adds a ValidationRuleGroup to the database.
   *
   * @param validationRuleGroup the ValidationRuleGroup to add.
   * @return the generated unique identifier for the ValidationRuleGroup.
   */
  long addValidationRuleGroup(ValidationRuleGroup validationRuleGroup);

  /**
   * Delete a ValidationRuleGroup with the given identifiers from the database.
   *
   * @param validationRuleGroup the ValidationRuleGroup to delete.
   */
  void deleteValidationRuleGroup(ValidationRuleGroup validationRuleGroup);

  /**
   * Update a ValidationRuleGroup with the given identifiers.
   *
   * @param validationRuleGroup the ValidationRule to update.
   */
  void updateValidationRuleGroup(ValidationRuleGroup validationRuleGroup);

  /**
   * Get ValidationRuleGroup with the given identifier.
   *
   * @param id the unique identifier of the ValidationRuleGroup.
   * @return the ValidationRuleGroup or null if it doesn't exist.
   */
  ValidationRuleGroup getValidationRuleGroup(long id);

  /**
   * Get ValidationRuleGroup with the given uid.
   *
   * @param uid the unique identifier of the ValidationRuleGroup.
   * @return the ValidationRuleGroup or null if it doesn't exist.
   */
  ValidationRuleGroup getValidationRuleGroup(String uid);

  /**
   * Get all ValidationRuleGroups.
   *
   * @return a List of ValidationRuleGroup or null if it there are no ValidationRuleGroups.
   */
  List<ValidationRuleGroup> getAllValidationRuleGroups();

  /**
   * Get a ValidationRuleGroup with the given name.
   *
   * @param name the name of the ValidationRuleGroup.
   */
  ValidationRuleGroup getValidationRuleGroupByName(String name);

  List<ValidationRule> getValidationRulesBetween(int first, int max);

  List<ValidationRule> getValidationRulesBetweenByName(String name, int first, int max);

  List<ValidationRule> getValidationRulesByUid(Collection<String> uids);

  int getValidationRuleCount();

  int getValidationRuleCountByName(String name);

  List<ValidationRuleGroup> getValidationRuleGroupsBetween(int first, int max);

  List<ValidationRuleGroup> getValidationRuleGroupsBetweenByName(String name, int first, int max);

  int getValidationRuleGroupCount();

  int getValidationRuleGroupCountByName(String name);
}
