package org.hisp.dhis.approvalvalidationrule;

import java.util.List;

import org.hisp.dhis.category.CategoryOptionCombo;

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

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * @author Mike Nelushi
 */

public interface ApprovalValidationAuditService
{
    String ID = ApprovalValidationAuditService.class.getName();

    /**
     * Adds a ApprovalValidationAudit.
     *
     * @param approvalValidationAudit the ApprovalValidationAudit to add.
     */
    void addApprovalValidationAudit( ApprovalValidationAudit approvalValidationAudit );

    /**
     * Deletes all approval validation audits for the given organisation unit.
     *
     * @param organisationUnit the organisation unit.
     */
    void deleteApprovalValidationAudits( OrganisationUnit organisationUnit );

    /**
     * Deletes all data value audits for the given data set.
     *
     * @param dataSet the data set.
     */
    void deleteApprovalValidationAudits( DataSet dataSet );
    
    /**
     * Deletes all approval validation rule approvalValidationRule value audits for the given data set.
     *
     * @param approvalValidationRule the approvalValidation rule.
     */
    void deleteApprovalValidationAudits( ApprovalValidationRule approvalValidationRule );
    
    
    /**
     * Returns all ApprovalValidationAudit for the given ApprovalValidation.
     *
     * @param approvalValidation the ApprovalValidation to get ApprovalValidationAudit for.
     * @return a list of ApprovalValidationAudits which match the given ApprovalValidation,
     * or an empty collection if there are no matches.
     */
    List<ApprovalValidationAudit> getApprovalValidationAudits( ApprovalValidation approvalValidation );

    /**
     * Returns all ApprovalValidationAudits for the given DataSet, ApprovalValidationRule, Period,
     * OrganisationUnit and CategoryOptionCombo.
     *
     * @param dataSets         	   		the DataSet of the ApprovalValidationAudits.
     * @param approvalValidationRules   the ApprovalValidationRules of the ApprovalValidationAudits.
     * @param periods              		the Period of the ApprovalValidationAudits.
     * @param organisationUnits    		the OrganisationUnit of the ApprovalValidationAudits.
     * @param attributeOptionCombo 		the attribute option combo.
     * @return a list of ApprovalValidationAudits which matches the given DataSet, ApprovalValidationRule, Period,
     * OrganisationUnit and CategoryOptionCombo, or an empty collection if
     * there are not matches.
     */
    List<ApprovalValidationAudit> getApprovalValidationAudits( List<DataSet> dataSets, List<ApprovalValidationRule> approvalValidationRules, List<Period> periods, List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo attributeOptionCombo, AuditType auditType );

    List<ApprovalValidationAudit> getApprovalValidationAudits( List<DataSet> dataSets, List<ApprovalValidationRule> approvalValidationRules, List<Period> periods, List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo attributeOptionCombo, AuditType auditType, int first, int max );

    int countApprovalValidationAudits( List<DataSet> dataSets, List<ApprovalValidationRule> approvalValidationRules, List<Period> periods, List<OrganisationUnit> organisationUnits,
        CategoryOptionCombo categoryOptionCombo, CategoryOptionCombo attributeOptionCombo, AuditType auditType );
}