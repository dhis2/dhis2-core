package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Jim Grace
 */
public interface ValidationService
{
    int MAX_INTERACTIVE_ALERTS = 500;
    int MAX_SCHEDULED_ALERTS = 100000;

    /**
     * Validate DataValues.
     *
     * @param startDate         the start date.
     * @param endDate           the end date.
     * @param orgUnits          a list of organisation units.
     * @param attributeCombo    attribute category option combo (null for all).
     * @param group             validation rule group (null for all validationRules).
     * @param sendNotifications whether to send notifications upon rule violations.
     * @param format            the i18n format.
     * @return a Collection of ValidationResults for each validation violation.
     */
    Collection<ValidationResult> startInteractiveValidationAnalysis( Date startDate, Date endDate, List<OrganisationUnit> orgUnits,
        DataElementCategoryOptionCombo attributeCombo, ValidationRuleGroup group, boolean sendNotifications, I18nFormat format );

    /**
     * Validate DataValues.
     *
     * @param dataSet              the data set.
     * @param period               the period.
     * @param orgUnit              the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @return a Collection of ValidationResults for each validation violation.
     */
    Collection<ValidationResult> startInteractiveValidationAnalysis( DataSet dataSet, Period period, OrganisationUnit orgUnit, DataElementCategoryOptionCombo attributeOptionCombo );

    /**
     * Evaluates all the validation rules that could generate notifications,
     * and sends results (if any) to users who should be notified.
     */
    void startScheduledValidationAnalysis();

    /**
     * Validate that missing data values have a corresponding comment, assuming
     * that the given data set has the noValueRequiresComment property set to true.
     *
     * @param dataSet              the data set.
     * @param period               the period.
     * @param orgUnit              the organisation unit.
     * @param attributeOptionCombo the attribute option combo.
     * @return a list of operands representing missing comments.
     */
    List<DataElementOperand> validateRequiredComments( DataSet dataSet, Period period, OrganisationUnit orgUnit, DataElementCategoryOptionCombo attributeOptionCombo );
}
