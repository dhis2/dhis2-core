/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.validation.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.*;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultStore;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author Stian Sandvold
 */
@Slf4j
@Repository( "org.hisp.dhis.validation.ValidationResultStore" )
public class HibernateValidationResultStore
    extends HibernateGenericStore<ValidationResult>
    implements ValidationResultStore
{
    protected CurrentUserService currentUserService;

    public HibernateValidationResultStore( SessionFactory sessionFactory, JdbcTemplate jdbcTemplate,
        ApplicationEventPublisher publisher, CurrentUserService currentUserService )
    {
        super( sessionFactory, jdbcTemplate, publisher, ValidationResult.class, true );
        checkNotNull( currentUserService );
        this.currentUserService = currentUserService;
    }

    /**
     * Allows injection (e.g. by a unit test)
     */
    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    @Override
    public List<ValidationResult> getAllUnreportedValidationResults()
    {
        return getQuery( "from ValidationResult vr where vr.notificationSent = false"
            + getRestrictions( "and" ) ).list();
    }

    @Override
    public ValidationResult getById( long id )
    {
        return getSingleResult( getQuery( "from ValidationResult vr where vr.id = :id"
            + getRestrictions( "and" ) ).setParameter( "id", id ) );
    }

    @Override
    public List<ValidationResult> query( ValidationResultQuery validationResultQuery )
    {
        Query<ValidationResult> hibernateQuery = getQuery( "from ValidationResult vr" + getRestrictions( "where" ) );

        if ( !validationResultQuery.isSkipPaging() )
        {
            Pager pager = validationResultQuery.getPager();
            hibernateQuery.setFirstResult( pager.getOffset() );
            hibernateQuery.setMaxResults( pager.getPageSize() );
        }

        return hibernateQuery.getResultList();
    }

    @Override
    public int count( ValidationResultQuery validationResultQuery )
    {
        Query<Long> hibernateQuery = getTypedQuery(
            "select count(*) from ValidationResult vr" + getRestrictions( "where" ) );

        return hibernateQuery.getSingleResult().intValue();
    }

    @Override
    public List<ValidationResult> getValidationResults( OrganisationUnit orgUnit,
        boolean includeOrgUnitDescendants, Collection<ValidationRule> validationRules, Collection<Period> periods )
    {
        if ( isEmpty( validationRules ) || isEmpty( periods ) )
        {
            return new ArrayList<>();
        }

        String orgUnitFilter = orgUnit == null ? "" : "vr.organisationUnit.path like :orgUnitPath and ";

        Query<ValidationResult> query = getQuery( "from ValidationResult vr where " + orgUnitFilter
            + "vr.validationRule in :validationRules and vr.period in :periods " );

        if ( orgUnit != null )
        {
            query.setParameter( "orgUnitPath", orgUnit.getPath()
                + (includeOrgUnitDescendants ? "%" : "") );
        }

        query.setParameter( "validationRules", validationRules );
        query.setParameter( "periods", periods );

        return query.list();
    }

    @Override
    public void save( ValidationResult validationResult )
    {
        validationResult.setCreated( new Date() );
        super.save( validationResult );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * If we should, restrict which validation results the user is entitled to
     * see, based on the user's organisation units and on the user's dimension
     * constraints if the user has them.
     * <p>
     * If the current user is null (e.g. running a system process or a JUnit
     * test) or superuser, there is no restriction.
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
                restrictions += (restrictions.length() == 0 ? " " + whereAnd + " (" : " or ")
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
            String validCategoryOptionByCategory = isReadable( "co", user ) +
                " and exists (select 'x' from Category c where co in elements(c.categoryOptions)" +
                " and c.id in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( categories ), "," )
                + ") )";

            restrictions += " " + whereAnd + " 1 = (select min(case when " + validCategoryOptionByCategory
                + " then 1 else 0 end)" +
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
            String validCategoryOptionByCategoryOptionGroup = "exists (select 'x' from CategoryOptionGroup g" +
                " join g.groupSets s" +
                " where g.id in elements(co.groups)" +
                " and s.id in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( cogsets ), "," ) + ")" +
                " and " + isReadable( "g", user ) + " )";

            restrictions += " " + whereAnd +
                " 1 = (select min(case when " + validCategoryOptionByCategoryOptionGroup + " then 1 else 0 end)" +
                " from CategoryOption co" +
                " where co in elements(vr.attributeOptionCombo.categoryOptions) )";
        }

        log.debug( "Restrictions = " + restrictions );

        return restrictions;
    }

    /**
     * Returns a HQL string that determines whether an object is readable by a
     * user.
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
