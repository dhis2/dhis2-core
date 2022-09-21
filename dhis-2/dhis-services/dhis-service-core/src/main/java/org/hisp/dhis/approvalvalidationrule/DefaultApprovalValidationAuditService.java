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

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

/**
 * @author Mike Nelushi
 */
@Transactional
@Service( "org.hisp.dhis.approvalvalidationrule.ApprovalValidationAuditService" )
public class DefaultApprovalValidationAuditService
    implements ApprovalValidationAuditService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ApprovalValidationAuditStore approvalValidationAuditStore;



    public DefaultApprovalValidationAuditService(ApprovalValidationAuditStore approvalValidationAuditStore) {
    	checkNotNull( approvalValidationAuditStore );
    	
		this.approvalValidationAuditStore = approvalValidationAuditStore;
	}

	// -------------------------------------------------------------------------
    // ApprovalValidationAuditService implementation
    // -------------------------------------------------------------------------

	@Override
	public void addApprovalValidationAudit(ApprovalValidationAudit approvalValidationAudit) 
	{
		approvalValidationAuditStore.addApprovalValidationAudit(approvalValidationAudit);		
	}

	@Override
	public void deleteApprovalValidationAudits(OrganisationUnit organisationUnit) 
	{
		approvalValidationAuditStore.deleteApprovalValidationAudits(organisationUnit);
	}

	@Override
	public void deleteApprovalValidationAudits(DataSet dataSet) 
	{
		approvalValidationAuditStore.deleteApprovalValidationAudits(dataSet);;
	}

	@Override
	public void deleteApprovalValidationAudits(ApprovalValidationRule approvalValidationRule) 
	{
		approvalValidationAuditStore.deleteApprovalValidationAudits(approvalValidationRule);
	}

	@Override
	public List<ApprovalValidationAudit> getApprovalValidationAudits(ApprovalValidation approvalValidation) 
	{
		return approvalValidationAuditStore.getApprovalValidationAudits(approvalValidation);
	}

	@Override
	public List<ApprovalValidationAudit> getApprovalValidationAudits(List<DataSet> dataSets,
			List<ApprovalValidationRule> approvalValidationRules, List<Period> periods,
			List<OrganisationUnit> organisationUnits, CategoryOptionCombo attributeOptionCombo, AuditType auditType) 
	{
		return approvalValidationAuditStore.getApprovalValidationAudits(dataSets, approvalValidationRules, periods, organisationUnits, attributeOptionCombo, auditType);
	}

	@Override
	public List<ApprovalValidationAudit> getApprovalValidationAudits(List<DataSet> dataSets,
			List<ApprovalValidationRule> approvalValidationRules, List<Period> periods,
			List<OrganisationUnit> organisationUnits, CategoryOptionCombo attributeOptionCombo, AuditType auditType,
			int first, int max) 
	{
		return approvalValidationAuditStore.getApprovalValidationAudits(dataSets, approvalValidationRules, periods, organisationUnits, attributeOptionCombo, auditType, first, max);
	}

	@Override
	public int countApprovalValidationAudits(List<DataSet> dataSets,
			List<ApprovalValidationRule> approvalValidationRules, List<Period> periods,
			List<OrganisationUnit> organisationUnits, CategoryOptionCombo categoryOptionCombo,
			CategoryOptionCombo attributeOptionCombo, AuditType auditType) 
	{
		return approvalValidationAuditStore.countApprovalValidationAudits(dataSets, approvalValidationRules, periods, organisationUnits, categoryOptionCombo, attributeOptionCombo, auditType);
	}
}
