package org.hisp.dhis.approvalvalidationrule;
/*
 * Copyright (c) 2004-2019, University of Oslo
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

import org.hisp.dhis.approvalvalidationrule.comparator.ApprovalValidationQuery;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Nelushi
 */
public interface ApprovalValidationService
{
    /**
     * Saves a set of ApprovalValidation in a bulk action.
     * 
     * @param ApprovalValidations a collection of approval validation.
     */
    void saveApprovalValidations( Collection<ApprovalValidation> approvalValidations );

    /**
     * Returns a list of all existing ApprovalValidations.
     * 
     * @return a list of validation results.
     */
    List<ApprovalValidation> getAllApprovalValidations();

    /**
     * Returns a list of all ApprovalValidations where notificationSent is false
     * @return a list of validation results.
     */
    List<ApprovalValidation> getAllUnReportedApprovalValidations();

    /**
     * Deletes the approvalValidation.
     * 
     * @param validationResult the validation result.
     */
    void deleteApprovalValidation( ApprovalValidation approvalValidation );

    /**
     * Updates a list of ApprovalValidations.
     * 
     * @param validationResults validationResults to update.
     */
    void updateApprovalValidations( Set<ApprovalValidation> approvalValidations );

    /**
     * Returns the ApprovalValidation with the given id, or null if no validation result exists with that id.
     * 
     * @param id the validation result identifier.
     * @return a validation result.
     */
    ApprovalValidation getById( long id );

    List<ApprovalValidation> getApprovalValidations( ApprovalValidationQuery query );

    int countApprovalValidations( ApprovalValidationQuery query );

    List<ApprovalValidation> getApprovalValidations(DataSet dataSet, OrganisationUnit orgUnit,
        boolean includeOrgUnitDescendants, Collection<ApprovalValidationRule> approvalValidationRules, Collection<Period> periods );
}
