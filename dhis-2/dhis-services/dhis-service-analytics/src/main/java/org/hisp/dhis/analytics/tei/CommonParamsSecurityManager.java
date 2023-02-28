/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.tei;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.DATA_ELEMENT;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_INDICATOR;
import static org.hisp.dhis.analytics.security.CategorySecurityUtils.getCategoriesWithoutRestrictions;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType;
import org.hisp.dhis.analytics.common.dimension.ElementWithOffset;
import org.hisp.dhis.analytics.tei.query.context.querybuilder.OrgUnitQueryBuilder;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for checking that the current user has access to
 * the given {@link CommonParams}. It will check that the user has access to the
 * given org units, programs, programStages and all dimensionalObjects in the
 * query.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommonParamsSecurityManager
{
    private static final Collection<DimensionParamObjectType> SECURITY_CHECK_SKIP_TYPES = List.of(
        PROGRAM_ATTRIBUTE,
        DATA_ELEMENT,
        PROGRAM_INDICATOR );

    private final AnalyticsSecurityManager securityManager;

    private final CurrentUserService currentUserService;

    private final DimensionService dimensionService;

    /**
     * Checks that the current user has access to the given
     * {@link CommonParams}. It will check that the user has access to the given
     * org units, programs, programStages and all dimensionalObjects in the
     * query.
     *
     * @param commonParams the {@link CommonParams} to check.
     */
    void decideAccess( CommonParams commonParams, Collection<IdentifiableObject> extraObjects )
    {
        List<OrganisationUnit> queryOrgUnits = commonParams
            .getDimensionIdentifiers().stream()
            .filter( OrgUnitQueryBuilder::isOu )
            .map( DimensionIdentifier::getDimension )
            .map( DimensionParam::getDimensionalObject )
            .filter( Objects::nonNull )
            .map( DimensionalObject::getItems )
            .flatMap( Collection::stream )
            .map( OrganisationUnit.class::cast )
            .collect( Collectors.toList() );

        Set<IdentifiableObject> objects = new HashSet<>();
        objects.addAll( extraObjects );

        // DimensionalObjects from TeiQueryParams
        objects.addAll( commonParams
            .getDimensionIdentifiers().stream()
            .filter( not( OrgUnitQueryBuilder::isOu ) )
            .map( DimensionIdentifier::getDimension )
            // TEAs/Program Attribute are not data shareable, so access depends on the program/TET
            .filter( not( CommonParamsSecurityManager::shouldSkipCheck ) )
            .map( DimensionParam::getDimensionalObject )
            .filter( Objects::nonNull )
            .map( DimensionalObject::getItems )
            .flatMap( List::stream )
            .collect( Collectors.toSet() ) );

        // DimensionalItemObjects from TeiQueryParams -> QueryItems
        objects.addAll( commonParams
            .getDimensionIdentifiers().stream()
            // We don't want to add the org units to the objects since they are
            // already checked in the queryOrgUnits list
            .filter( not( OrgUnitQueryBuilder::isOu ) )
            .map( DimensionIdentifier::getDimension )
            // TEAs/Program Attribute are not data shareable, so access depends on the program/TET
            .filter( not( CommonParamsSecurityManager::shouldSkipCheck ) )
            .map( DimensionParam::getQueryItem )
            .filter( Objects::nonNull )
            .map( QueryItem::getItem )
            .collect( Collectors.toSet() ) );

        // Programs
        objects.addAll( commonParams.getPrograms() );

        // Program Stages
        objects.addAll( commonParams
            .getDimensionIdentifiers().stream()
            .filter( DimensionIdentifier::hasProgramStage )
            .map( DimensionIdentifier::getProgramStage )
            .map( ElementWithOffset::getElement )
            .collect( Collectors.toSet() ) );

        securityManager.decideAccess( queryOrgUnits, objects );
        securityManager.decideAccessEventAnalyticsAuthority();
    }

    private static boolean shouldSkipCheck( DimensionParam dimensionParam )
    {
        return SECURITY_CHECK_SKIP_TYPES.contains( dimensionParam.getDimensionParamObjectType() );
    }

    /**
     * Transforms the given {@link CommonParams}, checking that all OrgUnits
     * specified in the query are accessible to the current user, based on
     * user's DataViewOrganisationUnits.
     *
     * @param commonParams the {@link CommonParams}.
     */
    void applyOrganisationUnitConstraint( CommonParams commonParams )
    {
        User user = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Check if current user has data view organisation units
        // ---------------------------------------------------------------------
        if ( user == null || !user.hasDataViewOrganisationUnit() )
        {
            return;
        }

        // ---------------------------------------------------------------------
        // Check if request already has organisation units specified
        // ---------------------------------------------------------------------
        boolean hasOrgUnit = commonParams
            .getDimensionIdentifiers().stream()
            .anyMatch( OrgUnitQueryBuilder::isOu );

        if ( hasOrgUnit )
        {
            return;
        }

        // -----------------------------------------------------------------
        // Apply constraint as filter, and remove potential all-dimension
        // -----------------------------------------------------------------
        List<DimensionIdentifier<DimensionParam>> orgUnitDimensions = commonParams
            .getDimensionIdentifiers().stream()
            .filter( OrgUnitQueryBuilder::isOu )
            .collect( Collectors.toList() );

        Set<OrganisationUnit> userDataViewOrganisationUnits = user.getDataViewOrganisationUnits();

        for ( DimensionIdentifier<DimensionParam> orgUnitDimension : orgUnitDimensions )
        {
            List<DimensionalItemObject> orgUnitItems = orgUnitDimension.getDimension().getDimensionalObject()
                .getItems();

            Set<OrganisationUnit> orgUnitFromRequest = orgUnitItems.stream()
                .map( OrganisationUnit.class::cast )
                .collect( Collectors.toSet() );

            Collection<OrganisationUnit> intersection = CollectionUtils.intersection( userDataViewOrganisationUnits,
                orgUnitFromRequest );

            orgUnitItems.clear();
            orgUnitItems.addAll( intersection );

        }

        log.debug( String.format( "User: '%s' constrained by data view organisation units", user.getUsername() ) );
    }

    /**
     * Transforms the given {@link CommonParams}, checking that all
     * DimensionalObjects specified in the query are readable to the current
     * user
     *
     * @param commonParams the {@link CommonParams}.
     */
    void applyDimensionConstraints( CommonParams commonParams )
    {
        User user = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Check if current user has dimension constraints
        // ---------------------------------------------------------------------
        if ( user == null )
        {
            return;
        }

        List<DimensionalObject> dimensionalObjects = commonParams
            .getDimensionIdentifiers().stream()
            .map( DimensionIdentifier::getDimension )
            .map( DimensionParam::getDimensionalObject )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );

        // Categories the user is constrained to
        Collection<Category> categories = currentUserService.currentUserIsSuper() ? emptyList()
            : getCategoriesWithoutRestrictions(
                commonParams.getPrograms(),
                dimensionalObjects );

        // union of user and category constraints
        Set<DimensionalObject> dimensionConstraints = Stream.concat(
            user.getDimensionConstraints().stream(),
            categories.stream() )
            .collect( Collectors.toSet() );

        if ( dimensionConstraints.isEmpty() ) // if no constraints
        {
            return; // nothing to do - no filters added to query
        }

        for ( DimensionalObject dimension : dimensionConstraints )
        {
            // -----------------------------------------------------------------
            // Check if dimension constraint already is specified with items
            // -----------------------------------------------------------------
            if ( hasDimensionOrFilterWithItems( commonParams, dimension.getUid() ) )
            {
                continue;
            }

            List<DimensionalItemObject> canReadItems = dimensionService.getCanReadDimensionItems(
                dimension.getDimension() );

            // -----------------------------------------------------------------
            // Check if current user has access to any items from constraint
            // -----------------------------------------------------------------
            if ( canReadItems.isEmpty() )
            {
                throwIllegalQueryEx( ErrorCode.E7123, dimension.getDimension() );
            }

            // -----------------------------------------------------------------
            // Apply constraint as filter, and remove potential all-dimension
            // -----------------------------------------------------------------
            dimension.getItems().clear();
            dimension.getItems().addAll( canReadItems );

            log.debug( String.format( "User: '%s' constrained by dimension: '%s'", user.getUsername(),
                dimension.getDimension() ) );
        }
    }

    /**
     * Returns true if the given dimensionUid in the {@link CommonParams} has a
     * dimension or filter with items. False otherwise.
     *
     * @param commonParams the {@link CommonParams}.
     * @param dimensionUid the dimension uid.
     * @return true if the given dimensionUid in the {@link CommonParams} has a
     *         dimension or filter
     */
    private boolean hasDimensionOrFilterWithItems( CommonParams commonParams, String dimensionUid )
    {
        return commonParams.getDimensionIdentifiers().stream()
            .map( DimensionIdentifier::getDimension )
            .map( DimensionParam::getDimensionalObject )
            .filter( Objects::nonNull )
            .filter( dimensionalObject -> dimensionalObject.getDimension().equals( dimensionUid ) )
            .anyMatch( d -> !d.getItems().isEmpty() );
    }
}
