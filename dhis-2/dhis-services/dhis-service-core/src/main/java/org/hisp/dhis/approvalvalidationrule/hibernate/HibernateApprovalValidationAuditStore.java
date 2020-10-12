package org.hisp.dhis.approvalvalidationrule.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidation;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationAudit;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationAuditStore;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataapproval.DataApprovalAudit;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

import com.google.common.collect.Lists;

/**
 * @author Mike Nelushi
 */
@Repository( "org.hisp.dhis.approvalvalidationrule.ApprovalValidationAuditStore" )
public class HibernateApprovalValidationAuditStore
	extends HibernateGenericStore<ApprovalValidationAudit>
    implements ApprovalValidationAuditStore
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------





//	private SessionFactory sessionFactory;

//    public void setSessionFactory( SessionFactory sessionFactory )
//    {
//        this.sessionFactory = sessionFactory;
//    }

//    private PeriodStore periodStore;
//
//    public void setPeriodStore( PeriodStore periodStore )
//    {
//        this.periodStore = periodStore;
//    }
    
	private final PeriodStore periodStore;
	
	public HibernateApprovalValidationAuditStore(SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
			ApplicationEventPublisher publisher, PeriodStore periodStore) {
		super(sessionFactory, jdbcTemplate, publisher,  ApprovalValidationAudit.class, false );

		checkNotNull( periodStore );

        this.periodStore = periodStore;
	}
	



    // -------------------------------------------------------------------------
    // ApprovalValidationAuditStore implementation
    // -------------------------------------------------------------------------

	@Override
	public void addApprovalValidationAudit(ApprovalValidationAudit approvalValidationAudit) 
	{
		Session session = sessionFactory.getCurrentSession();
        session.save( approvalValidationAudit );
	}

	@Override
	public void deleteApprovalValidationAudits(OrganisationUnit organisationUnit) 
	{
		String hql = "delete from approvalvalidationaudit d where d.organisationUnit = :unit";
        
        sessionFactory.getCurrentSession().createQuery( hql ).
            setEntity( "unit", organisationUnit ).executeUpdate();
	}

	@Override
	public void deleteApprovalValidationAudits(DataSet dataSet) 
	{
		String hql = "delete from approvalvalidationaudit d where d.dataSet = :dataSet";

        sessionFactory.getCurrentSession().createQuery( hql )
            .setEntity( "dataSet", dataSet ).executeUpdate();
	}

	@Override
	public void deleteApprovalValidationAudits(ApprovalValidationRule approvalValidationRule) 
	{
		String hql = "delete from approvalvalidationaudit d where d.approvalValidationRule = :approvalValidationRule";

        sessionFactory.getCurrentSession().createQuery( hql )
            .setEntity( "approvalValidationRule", approvalValidationRule ).executeUpdate();
	}

	@Override
	public List<ApprovalValidationAudit> getApprovalValidationAudits(ApprovalValidation approvalValidation) 
	{
		return getApprovalValidationAudits( Lists.newArrayList( approvalValidation.getDataSet() ), Lists.newArrayList( approvalValidation.getApprovalValidationRule() ), Lists.newArrayList( approvalValidation.getPeriod() ),
	            Lists.newArrayList( approvalValidation.getOrganisationUnit() ), approvalValidation.getAttributeOptionCombo(), null );
	}

	@Override
	public List<ApprovalValidationAudit> getApprovalValidationAudits(List<DataSet> dataSets,
			List<ApprovalValidationRule> approvalValidationRules, List<Period> periods,
			List<OrganisationUnit> organisationUnits, CategoryOptionCombo attributeOptionCombo, AuditType auditType) 
	{
		Criteria criteria = getApprovalValidationAuditCriteria( dataSets, approvalValidationRules, periods, organisationUnits, attributeOptionCombo, auditType );
        criteria.addOrder( Order.desc( "created" ) );

        return criteria.list();
	}

	@Override
	public List<ApprovalValidationAudit> getApprovalValidationAudits(List<DataSet> dataSets,
			List<ApprovalValidationRule> approvalValidationRules, List<Period> periods,
			List<OrganisationUnit> organisationUnits, CategoryOptionCombo attributeOptionCombo, AuditType auditType,
			int first, int max) 
	{
		Criteria criteria = getApprovalValidationAuditCriteria( dataSets, approvalValidationRules, periods, organisationUnits, attributeOptionCombo, auditType );
        criteria.addOrder( Order.desc( "created" ) );
        criteria.setFirstResult( first );
        criteria.setMaxResults( max );

        return criteria.list();
	}

	@Override
	public int countApprovalValidationAudits(List<DataSet> dataSets,
			List<ApprovalValidationRule> approvalValidationRules, List<Period> periods,
			List<OrganisationUnit> organisationUnits, CategoryOptionCombo categoryOptionCombo,
			CategoryOptionCombo attributeOptionCombo, AuditType auditType) 
	{
		return ((Number) getApprovalValidationAuditCriteria( dataSets, approvalValidationRules, periods, organisationUnits, attributeOptionCombo, auditType )
	            .setProjection( Projections.countDistinct( "id" ) ).uniqueResult()).intValue();
	}
	
	private Criteria getApprovalValidationAuditCriteria( List<DataSet> dataSets,
			List<ApprovalValidationRule> approvalValidationRules, List<Period> periods, List<OrganisationUnit> organisationUnits, 
	        CategoryOptionCombo attributeOptionCombo, AuditType auditType )
	    {
	        Session session = sessionFactory.getCurrentSession();
	        List<Period> storedPeriods = new ArrayList<>();

	        if ( !periods.isEmpty() )
	        {
	            for ( Period period : periods )
	            {
	                Period storedPeriod = periodStore.reloadPeriod( period );

	                if ( storedPeriod != null )
	                {
	                    storedPeriods.add( storedPeriod );
	                }
	            }
	        }

	        Criteria criteria = session.createCriteria( ApprovalValidationAudit.class );

	        if ( !dataSets.isEmpty() )
	        {
	            criteria.add( Restrictions.in( "dataSets", dataSets ) );
	        }
	        
	        if ( !approvalValidationRules.isEmpty() )
	        {
	            criteria.add( Restrictions.in( "approvalValidationRules", approvalValidationRules ) );
	        }

	        if ( !storedPeriods.isEmpty() )
	        {
	            criteria.add( Restrictions.in( "period", storedPeriods ) );
	        }

	        if ( !organisationUnits.isEmpty() )
	        {
	            criteria.add( Restrictions.in( "organisationUnit", organisationUnits ) );
	        }

	        if ( attributeOptionCombo != null )
	        {
	            criteria.add( Restrictions.eq( "attributeOptionCombo", attributeOptionCombo ) );
	        }

	        if ( auditType != null )
	        {
	            criteria.add( Restrictions.eq( "auditType", auditType ) );
	        }

	        return criteria;
	    }
}
