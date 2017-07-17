package org.hisp.dhis.validation.hibernate;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.dataelement.CategoryOptionGroupSet;
import org.hisp.dhis.dataelement.DataElementCategory;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationResultStore;
import org.hisp.dhis.validation.comparator.ValidationResultQuery;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Stian Sandvold
 */
public class HibernateValidationResultStore
    extends HibernateGenericStore<ValidationResult>
    implements ValidationResultStore
{
    private static final Log log = LogFactory.getLog( HibernateValidationResultStore.class );

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ValidationResult> getAllUnreportedValidationResults()
    {
        return getQuery( "from ValidationResult vr where vr.notificationSent = false"
            + getRestrictions( "and") ).list();
    }

    @Override
    public ValidationResult getById( int id )
    {
        return (ValidationResult) getQuery( "from ValidationResult vr where vr.id = :id"
            + getRestrictions( "and") ).setInteger( "id", id ).uniqueResult();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public List<ValidationResult> query( ValidationResultQuery validationResultQuery )
    {
        Query hibernateQuery = getQuery( "from ValidationResult vr" + getRestrictions( "where" ) );

        if ( !validationResultQuery.isSkipPaging() )
        {
            Pager pager = validationResultQuery.getPager();
            hibernateQuery.setFirstResult( pager.getOffset() );
            hibernateQuery.setMaxResults( pager.getPageSize() );
        }

        return hibernateQuery.list();
    }

    @Override
    public int count( ValidationResultQuery validationResultQuery )
    {
        Query hibernateQuery = getQuery( "from ValidationResult vr" + getRestrictions( "where" ) );

        return hibernateQuery.list().size();
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

        Set<DataElementCategory> categories = user.getUserCredentials().getCatDimensionConstraints();

        if ( !CollectionUtils.isEmpty( categories ) )
        {
            String validCategoryOptionByCategory =
                isReadable( "co", user ) +
                " and exists (select 'x' from DataElementCategory c where co in elements(c.categoryOptions)" +
                " and c.id in (" + StringUtils.join( IdentifiableObjectUtils.getIdentifiers( categories ), "," ) + ") )";

            restrictions += " " + whereAnd + " 1 = (select min(case when " +  validCategoryOptionByCategory + " then 1 else 0 end)" +
                " from DataElementCategoryOption co" +
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
                " from DataElementCategoryOption co" +
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
