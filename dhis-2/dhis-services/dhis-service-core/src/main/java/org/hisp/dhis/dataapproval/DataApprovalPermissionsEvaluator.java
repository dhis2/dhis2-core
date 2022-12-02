/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.dataapproval;

import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;

/**
 * This package private class holds the context for deciding on data approval
 * permissions. The context contains both system settings and some qualities of
 * the user.
 * <p>
 * This class is especially efficient if the settings are set once and then used
 * several times to generate ApprovalPermissions for different DataApproval
 * objects.
 *
 * @author Jim Grace
 */
@Slf4j
class DataApprovalPermissionsEvaluator
{
    private DataApprovalLevelService dataApprovalLevelService;

    private IdentifiableObjectManager idObjectManager;

    private User user;

    private boolean acceptanceRequiredForApproval;

    private boolean authorizedToApprove;

    private boolean authorizedToApproveAtLowerLevels;

    private boolean authorizedToAcceptAtLowerLevels;

    private boolean mayViewLowerLevelUnapprovedData;

    private DataApprovalPermissionsEvaluator()
    {
    }

    private static Cache<DataApprovalLevel> USER_APPROVAL_LEVEL_CACHE = new SimpleCacheBuilder<DataApprovalLevel>()
        .forRegion( "userApprovalLevelCache" )
        .expireAfterAccess( 10, TimeUnit.MINUTES )
        .withInitialCapacity( 10000 )
        .withMaximumSize( 50000 )
        .build();

    /**
     * Clears the user approval level cache, for unit testing when the same user
     * ID may have different approval levels in quick succession.
     */
    public static void invalidateCache()
    {
        USER_APPROVAL_LEVEL_CACHE.invalidateAll();
    }

    /**
     * Allocates and populates the context for determining user permissions on
     * one or more DataApproval objects.
     *
     * @param currentUserService Current user service
     * @param organisationUnitService OrganisationUnit service
     * @param systemSettingManager System setting manager
     * @param dataApprovalLevelService Data approval level service
     * @return context for determining user permissions
     */
    public static DataApprovalPermissionsEvaluator makePermissionsEvaluator( CurrentUserService currentUserService,
        IdentifiableObjectManager idObjectManager, SystemSettingManager systemSettingManager,
        DataApprovalLevelService dataApprovalLevelService )
    {
        DataApprovalPermissionsEvaluator ev = new DataApprovalPermissionsEvaluator();

        ev.idObjectManager = idObjectManager;
        ev.dataApprovalLevelService = dataApprovalLevelService;

        ev.user = currentUserService.getCurrentUser();

        ev.acceptanceRequiredForApproval = systemSettingManager
            .getBoolSetting( SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL );
        boolean hideUnapprovedData = systemSettingManager.hideUnapprovedDataInAnalytics();

        ev.authorizedToApprove = ev.user.isAuthorized( DataApproval.AUTH_APPROVE );
        ev.authorizedToApproveAtLowerLevels = ev.user
            .isAuthorized( DataApproval.AUTH_APPROVE_LOWER_LEVELS );
        ev.authorizedToAcceptAtLowerLevels = ev.user
            .isAuthorized( DataApproval.AUTH_ACCEPT_LOWER_LEVELS );
        Boolean authorizedToViewUnapprovedData = ev.user
            .isAuthorized( DataApproval.AUTH_VIEW_UNAPPROVED_DATA );

        ev.mayViewLowerLevelUnapprovedData = !hideUnapprovedData || authorizedToViewUnapprovedData;

        log.debug( "makePermissionsEvaluator acceptanceRequiredForApproval " + ev.acceptanceRequiredForApproval
            + " hideUnapprovedData " + hideUnapprovedData + " authorizedToApprove " + ev.authorizedToApprove
            + " authorizedToAcceptAtLowerLevels " + ev.authorizedToAcceptAtLowerLevels
            + " authorizedToViewUnapprovedData " + authorizedToViewUnapprovedData );

        return ev;
    }

    /**
     * Evaluates approval permissions in the approval status according to the
     * context of system settings and user information.
     * <p>
     * Also adjusts the approval state as necessary if acceptances are not
     * configured.
     * <p>
     * If there is a data permissions state, also takes this into account.
     * <p>
     * It is assumed that the org units have been filtered already for only the
     * org units that a user may see (read).
     *
     * @param status the data approval status (if any)
     * @param workflow the data approval workflow
     */
    public void evaluatePermissions( DataApprovalStatus status, DataApprovalWorkflow workflow )
    {
        DataApprovalState state = status.getState();

        DataApprovalPermissions permissions = new DataApprovalPermissions();

        status.setPermissions( permissions );

        if ( status.getOrganisationUnitUid() == null )
        {
            log.debug( "getPermissions organisationUnitUid null for user "
                + (user == null ? "(null)" : user.getUsername()) + " orgUnit [null]" );

            permissions.setMayReadData( true );

            return; // No approval permissions set.
        }

        DataApprovalLevel userApprovalLevel = getUserApprovalLevelWithCache( status.getOrganisationUnitUid(),
            workflow );

        if ( userApprovalLevel == null )
        {
            log.debug( "getPermissions userApprovalLevel null for user "
                + (user == null ? "(null)" : user.getUsername()) + " orgUnit " + status.getOrganisationUnitUid() );

            permissions.setMayReadData( true );

            return; // Can't find user approval level, so no approval
                   // permissions are set.
        }

        int userLevelIndex = getWorkflowLevelIndex( userApprovalLevel, workflow );

        DataApprovalLevel dal = status.getActionLevel();
        int dataLevelIndex = (dal != null ? getWorkflowLevelIndex( dal, workflow ) : workflow.getLevels().size() - 1);

        boolean approvableAtNextHigherLevel = state.isApproved() && dal != null && dataLevelIndex > 0;

        // Level index (if any) at which data could next be approved.
        int approveLevelIndex = approvableAtNextHigherLevel ? dataLevelIndex - 1 : dataLevelIndex;

        boolean mayApprove = false;
        boolean mayUnapprove = false;
        boolean mayAccept = false;
        boolean mayUnaccept = false;

        if ( ((authorizedToApprove && userLevelIndex == approveLevelIndex)
            || (authorizedToApproveAtLowerLevels && userLevelIndex < approveLevelIndex))
            && (!state.isApproved()
                || (approvableAtNextHigherLevel && (state.isAccepted() || !acceptanceRequiredForApproval))) )
        {
            // (If approved at one level, may approve for the next higher
            // level.)
            mayApprove = state.isApprovable() || approvableAtNextHigherLevel;
        }

        if ( (authorizedToApprove && userLevelIndex == dataLevelIndex
            && (!state.isAccepted() || !acceptanceRequiredForApproval))
            || (authorizedToApproveAtLowerLevels && userLevelIndex < dataLevelIndex) )
        {
            mayUnapprove = state.isUnapprovable();
        }

        if ( authorizedToAcceptAtLowerLevels && (userLevelIndex == dataLevelIndex - 1
            || (authorizedToApproveAtLowerLevels && userLevelIndex < dataLevelIndex)) )
        {
            mayAccept = state.isAcceptable();
            mayUnaccept = state.isUnacceptable();

            if ( state.isUnapprovable() )
            {
                mayUnapprove = true;
            }
        }

        boolean mayReadData = mayApprove || mayUnapprove || mayAccept || mayUnaccept ||
            (userLevelIndex >= dataLevelIndex || mayViewLowerLevelUnapprovedData);

        if ( !acceptanceRequiredForApproval )
        {
            mayAccept = false;
            mayUnaccept = false;

            if ( state == DataApprovalState.ACCEPTED_HERE )
            {
                state = DataApprovalState.APPROVED_HERE;
                status.setState( state );
            }
        }

        permissions.setMayApprove( mayApprove );
        permissions.setMayUnapprove( mayUnapprove );
        permissions.setMayAccept( mayAccept );
        permissions.setMayUnaccept( mayUnaccept );
        permissions.setMayReadData( mayReadData );
        permissions.setMayReadAcceptedBy( acceptanceRequiredForApproval
            && (state == DataApprovalState.ACCEPTED_HERE || state == DataApprovalState.APPROVED_ABOVE)
            && (userLevelIndex < dataLevelIndex) );
    }

    private DataApprovalLevel getUserApprovalLevelWithCache( String orgUnitUid, DataApprovalWorkflow workflow )
    {
        DataApprovalLevel userApprovalLevel;

        final String organisationUnitUid = orgUnitUid;

        final DataApprovalWorkflow dataApprovalWorkflow = workflow;

        userApprovalLevel = USER_APPROVAL_LEVEL_CACHE.get( user.getId() + "-" + organisationUnitUid,
            c -> dataApprovalLevelService.getUserApprovalLevel( user,
                idObjectManager.get( OrganisationUnit.class, organisationUnitUid ),
                dataApprovalWorkflow.getSortedLevels() ) );

        return userApprovalLevel;
    }

    private int getWorkflowLevelIndex( DataApprovalLevel level, DataApprovalWorkflow workflow )
    {
        List<DataApprovalLevel> workflowSortedLevels = workflow.getSortedLevels();

        for ( int i = 0; i < workflowSortedLevels.size(); i++ )
        {
            if ( level.getLevel() == workflowSortedLevels.get( i ).getLevel() )
            {
                return i;
            }
        }

        return workflowSortedLevels.size() - 1;
    }
}
