package org.hisp.dhis.analytics.security;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DefaultAnalyticsSecurityManager
    implements AnalyticsSecurityManager
{
    private static final Log log = LogFactory.getLog( DefaultAnalyticsSecurityManager.class );
    
    @Autowired
    private DataApprovalLevelService approvalLevelService;
    
    @Autowired
    private SystemSettingManager systemSettingManager;
    
    @Autowired
    private DimensionService dimensionService;

    @Autowired
    private CurrentUserService currentUserService;
    
    // -------------------------------------------------------------------------
    // AnalyticsSecurityManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void decideAccess( DataQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Check current user data view access to org units
        // ---------------------------------------------------------------------
        
        User user = currentUserService.getCurrentUser();
        
        List<DimensionalItemObject> queryOrgUnits = params.getDimensionOrFilterItems( DimensionalObject.ORGUNIT_DIM_ID );
        
        if ( queryOrgUnits.isEmpty() || user == null || !user.hasDataViewOrganisationUnit() )
        {
            return; // Allow if no 
        }
        
        Set<OrganisationUnit> viewOrgUnits = user.getDataViewOrganisationUnits();
                
        for ( DimensionalItemObject object : queryOrgUnits )
        {
            OrganisationUnit queryOrgUnit = (OrganisationUnit) object;
            
            if ( !queryOrgUnit.isDescendant( viewOrgUnits ) )
            {
                throw new IllegalQueryException( "User: " + user.getUsername() + " is not allowed to view org unit: " + queryOrgUnit.getUid() );
            }
        }
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

        boolean hideUnapprovedData = (Boolean) systemSettingManager.getSystemSetting( SettingKey.HIDE_UNAPPROVED_DATA_IN_ANALYTICS );
        
        boolean canViewUnapprovedData = user != null ? user.getUserCredentials().isAuthorized( DataApproval.AUTH_VIEW_UNAPPROVED_DATA ) : true;
        
        if ( hideUnapprovedData && user != null )
        {
            Map<OrganisationUnit, Integer> approvalLevels = null;
            
            if ( params.hasApprovalLevel() ) 
            {
                // Set approval level from query
                
                DataApprovalLevel approvalLevel = approvalLevelService.getDataApprovalLevel( params.getApprovalLevel() );
                
                if ( approvalLevel == null )
                {
                    throw new IllegalQueryException( "Approval level does not exist:" + params.getApprovalLevel() );
                }
                
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
                
                log.debug( "User: " + user.getUsername() + " constrained by data approval levels: " + approvalLevels.values() );
            }
        }
        
        return paramsBuilder.build();
    }
    
    @Override
    public DataQueryParams withDimensionConstraints( DataQueryParams params )
    {
        DataQueryParams.Builder builder = DataQueryParams.newBuilder( params );
        
        applyOrganisationUnitConstraint( builder, params );
        applyUserConstraints( builder, params );
        
        return builder.build();
    }

    private void applyOrganisationUnitConstraint( DataQueryParams.Builder builder, DataQueryParams params )
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

        log.debug( "User: " + user.getUsername() + " constrained by data view organisation units" );        
    }
    
    private void applyUserConstraints( DataQueryParams.Builder builder, DataQueryParams params )
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

            if ( canReadItems == null || canReadItems.isEmpty() )
            {
                throw new IllegalQueryException( "Current user is constrained by a dimension but has access to no associated dimension items: " + dimension.getDimension() );
            }

            // -----------------------------------------------------------------
            // Apply constraint as filter, and remove potential all-dimension
            // -----------------------------------------------------------------

            builder.removeDimensionOrFilter( dimension.getDimension() );
            
            DimensionalObject constraint = new BaseDimensionalObject( dimension.getDimension(), 
                dimension.getDimensionType(), null, dimension.getDisplayName(), canReadItems );
            
            builder.addFilter( constraint );

            log.debug( "User: " + user.getUsername() + " constrained by dimension: " + constraint.getDimension() );
        }        
    }
}
