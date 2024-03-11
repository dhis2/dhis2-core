package org.hisp.dhis.analytics.security;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.QueryParamsBuilder;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
public class DefaultAnalyticsSecurityManager
    implements AnalyticsSecurityManager
{
    private static final Log log = LogFactory.getLog( DefaultAnalyticsSecurityManager.class );

    private static final String AUTH_VIEW_EVENT_ANALYTICS = "F_VIEW_EVENT_ANALYTICS";

    @Autowired
    private DataApprovalLevelService approvalLevelService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private AclService aclService;

    @Autowired
    private CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // AnalyticsSecurityManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void decideAccess( DataQueryParams params )
    {
        User user = currentUserService.getCurrentUser();

        decideAccessDataViewOrganisationUnits( params, user );
        decideAccessDataReadObjects( params, user );
    }

    /**
     * Checks whether the given user has data view access to organisation units.
     *
     * @param params the data query parameters.
     * @param user the user to check.
     * @throws IllegalQueryException if user does not have access.
     */
    private void decideAccessDataViewOrganisationUnits( DataQueryParams params, User user )
        throws IllegalQueryException
    {
        List<DimensionalItemObject> queryOrgUnits = params.getDimensionOrFilterItems( DimensionalObject.ORGUNIT_DIM_ID );

        if ( queryOrgUnits.isEmpty() || user == null || !user.hasDataViewOrganisationUnit() )
        {
            return; // Allow if no
        }

        Set<OrganisationUnit> viewOrgUnits = user.getDataViewOrganisationUnits();

        for ( DimensionalItemObject object : queryOrgUnits )
        {
            OrganisationUnit queryOrgUnit = (OrganisationUnit) object;

            boolean notDescendant = !queryOrgUnit.isDescendant( viewOrgUnits );

            throwExWhenTrue( notDescendant, String.format( "User: %s is not allowed to view org unit: %s", user.getUsername(), queryOrgUnit.getUid() ) );
        }
    }

    /**
     * Checks whether the given user has data read access to all programs,
     * program stages, data sets and category options in the request.
     *
     * @param params the data query parameters.
     * @param user the user to check.
     * @throws IllegalQueryException if user does not have access.
     */
    void decideAccessDataReadObjects( DataQueryParams params, User user )
        throws IllegalQueryException
    {
        Set<IdentifiableObject> objects = new HashSet<>();
        objects.addAll( params.getAllDataSets() );
        objects.addAll( params.getProgramsInAttributesAndDataElements() );
        objects.addAll( params.getCategoryOptions() );

        if ( params.hasProgram() )
        {
            objects.add( params.getProgram() );
        }

        if ( params.hasProgramStage() )
        {
            objects.add( params.getProgramStage() );
        }

        for ( IdentifiableObject object : objects )
        {
            boolean canNotRead = !aclService.canDataRead( user, object );
            String className = TextUtils.getPrettyClassName( object.getClass() );

            if ( canNotRead ) {
                throw new IllegalQueryException( String.format( "User: %s is not allowed to read data for %s: %s", user.getUsername(), className, object.getUid()) );
            }
        }
    }

    @Override
    public void decideAccessEventQuery( EventQueryParams params )
    {
        decideAccess( params );
        decideAccessEventAnalyticsAuthority( params );

    }

    /**
     * Checks whether the current user has the {@code F_VIEW_EVENT_ANALYTICS} authority.
     *
     * @param params the {@link {@link DataQueryParams}.
     */
    private void decideAccessEventAnalyticsAuthority( EventQueryParams params )
    {
        User user = currentUserService.getCurrentUser();

        boolean notAuthorized = user != null && !user.isAuthorized( AUTH_VIEW_EVENT_ANALYTICS );

        throwExWhenTrue( notAuthorized, String.format( "User: %s is not allowed to view event analytics", user.getUsername() ) );
    }

    @Override
    public User getCurrentUser( DataQueryParams params )
    {
        return params != null && params.hasCurrentUser() ?
            params.getCurrentUser() : currentUserService.getCurrentUser();
    }

    @Override
    public DataQueryParams withDataApprovalConstraints( DataQueryParams params )
    {
        DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder( params );

        User user = currentUserService.getCurrentUser();

        boolean hideUnapprovedData = systemSettingManager.hideUnapprovedDataInAnalytics();

        boolean canViewUnapprovedData = user != null ? user.getUserCredentials().isAuthorized( DataApproval.AUTH_VIEW_UNAPPROVED_DATA ) : true;

        if ( hideUnapprovedData && user != null )
        {
            Map<OrganisationUnit, Integer> approvalLevels = null;

            if ( params.hasApprovalLevel() )
            {
                // Set approval level from query

                DataApprovalLevel approvalLevel = approvalLevelService.getDataApprovalLevel( params.getApprovalLevel() );

                throwExWhenTrue( approvalLevel == null, String.format( "Approval level does not exist: %s", params.getApprovalLevel() ) );

                approvalLevels = approvalLevelService.getUserReadApprovalLevels( approvalLevel );
            }
            else if ( !canViewUnapprovedData )
            {
                // Set approval level from user level

                approvalLevels = approvalLevelService.getUserReadApprovalLevels();
            }

            if ( approvalLevels != null && !approvalLevels.isEmpty() )
            {
                paramsBuilder.withDataApprovalLevels( approvalLevels );

                log.debug( String.format( "User: %s constrained by data approval levels: %s", user.getUsername(), approvalLevels.values() ) );
            }
        }

        return paramsBuilder.build();
    }

    @Override
    public DataQueryParams withUserConstraints( DataQueryParams params )
    {
        DataQueryParams.Builder builder = DataQueryParams.newBuilder( params );

        applyOrganisationUnitConstraint( builder, params );
        applyDimensionConstraints( builder, params );

        return builder.build();
    }

    @Override
    public EventQueryParams withUserConstraints( EventQueryParams params )
    {
        EventQueryParams.Builder builder = new EventQueryParams.Builder( params );

        applyOrganisationUnitConstraint( builder, params );
        applyDimensionConstraints( builder, params );

        return builder.build();
    }

    /**
     * Applies organisation unit security constraint.
     *
     * @param builder the query parameters builder.
     * @param params the data query parameters.
     */
    private void applyOrganisationUnitConstraint( QueryParamsBuilder builder, DataQueryParams params )
    {
        User user = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Check if current user has data view organisation units
        // ---------------------------------------------------------------------

        if ( params == null || user == null || !user.hasDataViewOrganisationUnit() )
        {
            return;
        }

        // ---------------------------------------------------------------------
        // Check if request already has organisation units specified
        // ---------------------------------------------------------------------

        if ( params.hasDimensionOrFilterWithItems( DimensionalObject.ORGUNIT_DIM_ID ) )
        {
            return;
        }

        // -----------------------------------------------------------------
        // Apply constraint as filter, and remove potential all-dimension
        // -----------------------------------------------------------------

        builder.removeDimensionOrFilter( DimensionalObject.ORGUNIT_DIM_ID );

        List<OrganisationUnit> orgUnits = new ArrayList<>( user.getDataViewOrganisationUnits() );

        DimensionalObject constraint = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, orgUnits );

        builder.addFilter( constraint );

        log.debug( String.format( "User: %s constrained by data view organisation units", user.getUsername() ) );
    }

    /**
     * Applies user security constraint.
     *
     * @param builder the query parameters builder.
     * @param params the data query parameters.
     */
    private void applyDimensionConstraints( QueryParamsBuilder builder, DataQueryParams params )
    {
        User user = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Check if current user has dimension constraints
        // ---------------------------------------------------------------------

        if ( params == null || user == null || user.getUserCredentials() == null || !user.getUserCredentials().hasDimensionConstraints() )
        {
            return;
        }

        Set<DimensionalObject> dimensionConstraints = user.getUserCredentials().getDimensionConstraints();

        for ( DimensionalObject dimension : dimensionConstraints )
        {
            // -----------------------------------------------------------------
            // Check if constraint already is specified with items
            // -----------------------------------------------------------------

            if ( params.hasDimensionOrFilterWithItems( dimension.getUid() ) )
            {
                continue;
            }

            List<DimensionalItemObject> canReadItems = dimensionService.getCanReadDimensionItems( dimension.getDimension() );

            // -----------------------------------------------------------------
            // Check if current user has access to any items from constraint
            // -----------------------------------------------------------------

            boolean hasNoReadItems =  canReadItems == null || canReadItems.isEmpty();

            throwExWhenTrue( hasNoReadItems, String.format( "Current user is constrained by a dimension but has access to no associated dimension items: %s", dimension.getDimension() ) );

            // -----------------------------------------------------------------
            // Apply constraint as filter, and remove potential all-dimension
            // -----------------------------------------------------------------

            builder.removeDimensionOrFilter( dimension.getDimension() );

            DimensionalObject constraint = new BaseDimensionalObject( dimension.getDimension(),
                dimension.getDimensionType(), null, dimension.getDisplayName(), canReadItems );

            builder.addFilter( constraint );

            log.debug( String.format( "User: %s constrained by dimension: %s", user.getUsername(), constraint.getDimension() ) );
        }
    }

    /**
     * Throws a {@link IllegalQueryException} with the given message if the
     * given condition is true.
     *
     * @param condition the condition.
     * @param message the message.
     * @throws {@link IllegalQueryException}.
     */
    private void throwExWhenTrue( boolean condition, String message )
    {
        if ( condition )
        {
            throw new IllegalQueryException( message );
        }
    }
}
