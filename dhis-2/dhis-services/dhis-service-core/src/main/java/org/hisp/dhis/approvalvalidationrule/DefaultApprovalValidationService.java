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

import org.hisp.dhis.approvalvalidationrule.ApprovalValidation;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationService;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationStore;
import org.hisp.dhis.approvalvalidationrule.comparator.ApprovalValidationQuery;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValueAudit;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Mike Nelushi
 */
@Transactional
@Service( "org.hisp.dhis.approvalvalidationrule.ApprovalValidationService" )
public class DefaultApprovalValidationService
    implements ApprovalValidationService
{
    /*@Autowired
    private ApprovalValidationStore approvalValidationStore;

    @Autowired
    private PeriodService periodService;*/
	
	private final ApprovalValidationStore approvalValidationStore;
	
	private final ApprovalValidationAuditService approvalValidationAuditService;

	private final PeriodService periodService;
	
    public DefaultApprovalValidationService(ApprovalValidationStore approvalValidationStore,
			ApprovalValidationAuditService approvalValidationAuditService, PeriodService periodService) {
        checkNotNull( approvalValidationStore );
        checkNotNull( approvalValidationAuditService );
        checkNotNull( periodService );

		this.approvalValidationStore = approvalValidationStore;
		this.approvalValidationAuditService = approvalValidationAuditService;
		this.periodService = periodService;
	}

    @Override
    public void saveApprovalValidations( Collection<ApprovalValidation> approvalValidations )
    {
    	approvalValidations.forEach( approvalValidation -> {
    		approvalValidation.setPeriod( periodService.reloadPeriod( approvalValidation.getPeriod() ) );
    		ApprovalValidationAudit approvalValidationAudit = new ApprovalValidationAudit( approvalValidation, approvalValidation.getStoredBy(), AuditType.CREATE);
        	approvalValidationAuditService.addApprovalValidationAudit(approvalValidationAudit);
            approvalValidationStore.save( approvalValidation );
        } );
    }


	public List<ApprovalValidation> getAllApprovalValidations()
    {
        return approvalValidationStore.getAll();
    }

    @Override
    public List<ApprovalValidation> getAllUnReportedApprovalValidations()
    {
        return approvalValidationStore.getAllUnreportedApprovalValidations();
    }

    @Override
    public void deleteApprovalValidation( ApprovalValidation approvalValidation )
    {
    	ApprovalValidationAudit approvalValidationAudit = new ApprovalValidationAudit( approvalValidation, approvalValidation.getStoredBy(), AuditType.DELETE);
    	approvalValidationAuditService.addApprovalValidationAudit(approvalValidationAudit);
    	approvalValidationStore.delete( approvalValidation );
    }

    @Override
    public void updateApprovalValidations( Set<ApprovalValidation> approvalValidations )
    {
    	approvalValidations.forEach( vr -> approvalValidationStore.update( vr ) );
    }

    @Override
    public ApprovalValidation getById( long id )
    {
        return approvalValidationStore.getById( id );
    }

    @Override
    public List<ApprovalValidation> getApprovalValidations( ApprovalValidationQuery query )
    {
        return approvalValidationStore.query( query );
    }

    @Override
    public int countApprovalValidations( ApprovalValidationQuery query )
    {
        return approvalValidationStore.count( query );
    }

    @Override
    public List<ApprovalValidation> getApprovalValidations(DataSet dataSet, OrganisationUnit orgUnit,
        boolean includeOrgUnitDescendants, Collection<ApprovalValidationRule> approvalValidationRules, Collection<Period> periods )
    {
        List<Period> persistedPeriods = periodService.reloadPeriods( new ArrayList<>( periods ) );
        return approvalValidationStore.getApprovalValidations(dataSet, orgUnit, includeOrgUnitDescendants, approvalValidationRules, persistedPeriods );
    }
}
