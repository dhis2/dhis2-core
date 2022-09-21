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

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Nelushi
 */
public interface ApprovalValidationRuleService
{
    String ID = ApprovalValidationRuleService.class.getName();

    // -------------------------------------------------------------------------
    // ValidationRule
    // -------------------------------------------------------------------------

    /**
     * Save a ApprovalValidationRule to the database.
     *
     * @param approvalValidationRule the ApprovalValidationRule to save.
     * @return the generated unique identifier for the ApprovalValidationRule.
     */
    long saveApprovalValidationRule( ApprovalValidationRule approvalValidationRule );

    /**
     * Update a ApprovalValidationRule to the database.
     *
     * @param approvalValidationRule the ApprovalValidationRule to update.
     */
    void updateApprovalValidationRule( ApprovalValidationRule approvalValidationRule );

    /**
     * Delete a approval validation rule with the given identifiers from the database.
     *
     * @param approvalValidationRule the ApprovalValidationRule to delete.
     */
    void deleteApprovalValidationRule( ApprovalValidationRule approvalValidationRule );

    /**
     * Get ApprovalValidationRule with the given identifier.
     *
     * @param id the unique identifier of the ApprovalValidationRule.
     * @return the ApprovalValidationRule or null if it doesn't exist.
     */
    ApprovalValidationRule getApprovalValidationRule( long id );

    /**
     * Get ApprovalValidationRule with the given uid.
     *
     * @param uid the unique identifier of the ApprovalValidationRule.
     * @return the ApprovalValidationRule or null if it doesn't exist.
     */
    ApprovalValidationRule getApprovalValidationRule( String uid );

    /**
     * Get all approval validation rules.
     *
     * @return a List of ApprovalValidationRule or null if there are no validation rules.
     */
    List<ApprovalValidationRule> getAllApprovalValidationRules();

    /**
     * Get a approval validation rule with the given name.
     *
     * @param name the name of the approval validation rule.
     */
    ApprovalValidationRule getApprovalValidationRuleByName( String name );
    
    /**
     * Returns ApprovalValidationRules objects (if any) for given collections of skipApprovalValidation
     *
     * @param skipApprovalValidation is skipApprovalValidation property
     */
    List<ApprovalValidationRule> getApprovalValidationRules( boolean skipApprovalValidation );


}
