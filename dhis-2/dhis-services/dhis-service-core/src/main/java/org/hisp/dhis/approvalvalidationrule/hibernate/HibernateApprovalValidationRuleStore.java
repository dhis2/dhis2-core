package org.hisp.dhis.approvalvalidationrule.hibernate;

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

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRuleStore;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Mike Nelushi
 */
@Repository( "org.hisp.dhis.approvalvalidationrule.ApprovalValidationRuleStore" )
public class HibernateApprovalValidationRuleStore
    extends HibernateIdentifiableObjectStore<ApprovalValidationRule>
    implements ApprovalValidationRuleStore
{
    // -------------------------------------------------------------------------
    // Dependency
    // -------------------------------------------------------------------------

    // private PeriodService periodService;
    //
    // public void setPeriodService( PeriodService periodService )
    // {
    // this.periodService = periodService;
    // }

    // -------------------------------------------------------------------------
    // Implementation
    // -------------------------------------------------------------------------

    public HibernateApprovalValidationRuleStore(SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
			ApplicationEventPublisher publisher, CurrentUserService currentUserService, DeletedObjectService deletedObjectService, AclService aclService) {
		super(sessionFactory, jdbcTemplate, publisher, ApprovalValidationRule.class, currentUserService, deletedObjectService, aclService, false);
	}

	@Override
    public void save( ApprovalValidationRule approvalValidationRule )
    {
        // PeriodType periodType = periodService.reloadPeriodType(
        // approvalValidationRule.getPeriodType() );
        //
        // approvalValidationRule.setPeriodType( periodType );

        super.save( approvalValidationRule );
    }

    @Override
    public void update( ApprovalValidationRule approvalValidationRule )
    {
        // PeriodType periodType = periodService.reloadPeriodType(
        // approvalValidationRule.getPeriodType() );
        //
        // approvalValidationRule.setPeriodType( periodType );

        super.save( approvalValidationRule );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ApprovalValidationRule> getAllApprovalValidationRules()
    {
        Criteria criteria = getSharingCriteria();

        return criteria.list();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ApprovalValidationRule> getApprovalValidationRules( boolean skipApprovalValidation )
    {
        Criteria criteria = getSharingCriteria();
        criteria.add( Restrictions.eq( "skipApprovalValidation", skipApprovalValidation ) );

        return criteria.list();
    }

}
