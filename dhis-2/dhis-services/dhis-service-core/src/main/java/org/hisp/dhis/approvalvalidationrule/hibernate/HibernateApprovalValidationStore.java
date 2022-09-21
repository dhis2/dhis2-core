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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.deletedobject.DeletedObjectService;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidation;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationRule;
import org.hisp.dhis.approvalvalidationrule.ApprovalValidationStore;
import org.hisp.dhis.approvalvalidationrule.comparator.ApprovalValidationQuery;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultStore;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * @author Mike Nelushi
 */
@Repository( "org.hisp.dhis.approvalvalidationrule.ApprovalValidationStore" )
public class HibernateApprovalValidationStore
    extends HibernateGenericStore<ApprovalValidation>
    implements ApprovalValidationStore
{
	@Autowired
    public HibernateApprovalValidationStore(SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
			ApplicationEventPublisher publisher) {
		super(sessionFactory, jdbcTemplate, publisher, ApprovalValidation.class, false );
	}
    
    /*@Autowired
    public HibernateAttributeStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate, ApplicationEventPublisher publisher,
        CurrentUserService currentUserService, DeletedObjectService deletedObjectService, AclService aclService )
    {
        super( sessionFactory, jdbcTemplate, publisher, Attribute.class, currentUserService, deletedObjectService, aclService,
            true );
    }*/


	private static final Log log = LogFactory.getLog( HibernateApprovalValidationStore.class );

    @Autowired
    protected CurrentUserService currentUserService;

    /**
     * Allows injection (e.g. by a unit test)
     */
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ApprovalValidation> getAllUnreportedApprovalValidations()
    {
        return getQuery( "from ApprovalValidation vr where vr.notificationSent = false"
            + getRestrictions( "and") ).list();
    }

    @Override
    public ApprovalValidation getById( long id )
    {
        return (ApprovalValidation) getQuery( "from ApprovalValidation vr where vr.id = :id"
            + getRestrictions( "and") ).setLong( "id", id ).uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ApprovalValidation> query( ApprovalValidationQuery approvalValidationQuery )
    {
        Query hibernateQuery = getQuery( "from ApprovalValidation vr" + getRestrictions( "where" ) );

        if ( !approvalValidationQuery.isSkipPaging() )
        {
            Pager pager = approvalValidationQuery.getPager();
            hibernateQuery.setFirstResult( pager.getOffset() );
            hibernateQuery.setMaxResults( pager.getPageSize() );
        }

        return hibernateQuery.list();
    }

    @Override
    public int count( ApprovalValidationQuery approvalValidationQuery )
    {
        Query hibernateQuery = getQuery( "from ApprovalValidation vr" + getRestrictions( "where" ) );

        return hibernateQuery.list().size();
    }

    @Override
    public List<ApprovalValidation> getApprovalValidations(DataSet dataSet, OrganisationUnit orgUnit,
        boolean includeOrgUnitDescendants, Collection<ApprovalValidationRule> approvalValidationRules, Collection<Period> periods )
    {
        if ( isEmpty( approvalValidationRules ) || isEmpty( periods ) )
        {
            return new ArrayList<>();
        }

        String orgUnitFilter = orgUnit == null ? "" : "vr.organisationUnit.path like :orgUnitPath and ";

        Query query = getQuery( "from ApprovalValidation vr where " + orgUnitFilter + "vr.approvalValidationRule in :approvalValidationRules and vr.period in :periods and vr.dataSet in :dataSet" );

        if ( orgUnit != null )
        {
            query.setParameter( "orgUnitPath", orgUnit.getPath()
                + ( includeOrgUnitDescendants ? "%" : "" ) );
        }

        query.setParameter( "approvalValidationRules", approvalValidationRules );
        query.setParameter( "periods", periods );
        query.setParameter( "dataSet", dataSet );

        return query.list();
    }

    @Override
    public void save( ApprovalValidation approvalValidation )
    {
    	approvalValidation.setCreated( new Date() );
        super.save( approvalValidation );
    }
    
    @Override
    public void update( ApprovalValidation approvalValidation )
    {
    	approvalValidation.setCreated( new Date() );
        super.save( approvalValidation );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * If we should, restrict which validation results the user is entitled
     * to see, based on the user's organisation units and on the user's
     * dimension constraints if the user has them.
     * <p>
     * If the current user is null (e.g. running a system process or
     * a JUnit test) or superuser, there is no restriction.
     *
     * @param whereAnd "where" or "and", to add restrictions to where clause.
     * @return String to add restrictions to the HQL query.
     */
    private String getRestrictions( String whereAnd )
    {
        String restrictions = "";

        final User user = currentUserService.getCurrentUser();

        if ( user == null || currentUserService.currentUserIsSuper() )
        {
            return restrictions;
        }

        // ---------------------------------------------------------------------
        // Restrict by the user's organisation unit sub-trees, if any
        // ---------------------------------------------------------------------

        Set<OrganisationUnit> userOrgUnits = user.getDataViewOrganisationUnitsWithFallback();

        if ( !userOrgUnits.isEmpty() )
        {
            for ( OrganisationUnit ou : userOrgUnits )
            {
                restrictions += ( restrictions.length() == 0 ? " " + whereAnd + " (" : " or " )
                    + "locate('" + ou.getUid() + "',vr.organisationUnit.path) <> 0";
            }
            restrictions += ")";
            whereAnd = "and";
        }

        // ---------------------------------------------------------------------
        // Restrict by the user's category dimension constraints, if any
        // ---------------------------------------------------------------------

        Set<Category> categories = user.getUserCredentials().getCatDimensionConstraints();

        if ( !CollectionUtils.isEmpty( categories ) )
        {
            String validCategoryOptionByCategory =
                isReadable( "co", user ) +
                " and exists (select 'x' from Category c where co in elements(c.categoryOptions)" +
                " and c.id in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( categories ), "," ) + ") )";

            restrictions += " " + whereAnd + " 1 = (select min(case when " +  validCategoryOptionByCategory + " then 1 else 0 end)" +
                " from CategoryOption co" +
                " where co in elements(vr.attributeOptionCombo.categoryOptions) )";

            whereAnd = "and";
        }

        // ---------------------------------------------------------------------
        // Restrict by the user's cat option group dimension constraints, if any
        // ---------------------------------------------------------------------

        Set<CategoryOptionGroupSet> cogsets = user.getUserCredentials().getCogsDimensionConstraints();

        if ( !CollectionUtils.isEmpty( cogsets ) )
        {
            String validCategoryOptionByCategoryOptionGroup =
                "exists (select 'x' from CategoryOptionGroup g" +
                    " join g.groupSets s" +
                    " where g.id in elements(co.groups)" +
                    " and s.id in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( cogsets ), "," ) + ")" +
                    " and " + isReadable( "g", user ) + " )";

            restrictions += " " + whereAnd +
                " 1 = (select min(case when " +  validCategoryOptionByCategoryOptionGroup + " then 1 else 0 end)" +
                " from CategoryOption co" +
                " where co in elements(vr.attributeOptionCombo.categoryOptions) )";
        }

        log.debug( "Restrictions = " + restrictions );

        return restrictions;
    }

    /**
     * Returns a HQL string that determines whether an object is readable
     * by a user.
     *
     * @param x the object to test for readability.
     * @param u the user who might be able to read the object.
     * @return HQL that evaluates to true or false depending on readability.
     */
    private String isReadable( String x, User u )
    {
        return "( " + x + ".publicAccess is null" +
            " or substring(" + x + ".publicAccess, 0, 1) = 'r'" +
            " or " + x + ".user is not null and " + x + ".user.id = " + u.getId() +
            " or exists (select 'x' from UserGroupAccess a join a.userGroup.members u" +
            " where a in elements(" + x + ".userGroupAccesses) and u.id = " + u.getId() + ") )";
    }
}
