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
import java.util.Date;
import java.util.List;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Jim Grace
 */
public interface ValidationService {
  int MAX_INTERACTIVE_ALERTS = 500;

  int MAX_SCHEDULED_ALERTS = 100000;

  /**
   * Start a validation analysis, based on the supplied parameters. See ValidationAnalysisParams for
   * more information
   *
   * @param parameters the parameters to base the analysis on.
   * @return a collection of ValidationResults found.
   */
  List<ValidationResult> validationAnalysis(ValidationAnalysisParams parameters);

  /**
   * Get validation rule expression details for a validation run.
   *
   * @param parameters the parameters to base the analysis on.
   * @return the validation rule expression details
   */
  ValidationRuleExpressionDetails getValidationRuleExpressionDetails(
      ValidationAnalysisParams parameters);

  /**
   * Validate that missing data values have a corresponding comment, assuming that the given data
   * set has the noValueRequiresComment property set to true.
   *
   * @param dataSet the data set.
   * @param period the period.
   * @param orgUnit the organisation unit.
   * @param attributeOptionCombo the attribute option combo.
   * @return a list of operands representing missing comments.
   */
  List<DataElementOperand> validateRequiredComments(
      DataSet dataSet,
      Period period,
      OrganisationUnit orgUnit,
      CategoryOptionCombo attributeOptionCombo);

  ValidationAnalysisParams.Builder newParamsBuilder(
      Collection<ValidationRule> validationRules,
      OrganisationUnit organisationUnit,
      Collection<Period> periods);

  ValidationAnalysisParams.Builder newParamsBuilder(
      ValidationRuleGroup validationRuleGroup,
      OrganisationUnit organisationUnit,
      Date startDate,
      Date endDate);

  ValidationAnalysisParams.Builder newParamsBuilder(
      DataSet dataSet, OrganisationUnit organisationUnits, Period period);
}
