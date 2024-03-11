package org.hisp.dhis.dataapproval;

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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dataapproval.exceptions.DataApprovalNotFound;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeAcceptedException;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeApprovedException;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeUnacceptedException;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeUnapprovedException;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hisp.dhis.dataapproval.DataApprovalAction.*;

/**
 * @author Jim Grace
 */
@Transactional
public class DefaultDataApprovalService
    implements DataApprovalService
{
    private final static Log log = LogFactory.getLog( DefaultDataApprovalService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private DataApprovalStore dataApprovalStore;

    public void setDataApprovalStore( DataApprovalStore dataApprovalStore )
    {
        this.dataApprovalStore = dataApprovalStore;
    }

    private DataApprovalAuditStore dataApprovalAuditStore;

    public void setDataApprovalAuditStore( DataApprovalAuditStore dataApprovalAuditStore )
    {
        this.dataApprovalAuditStore = dataApprovalAuditStore;
    }

    private DataApprovalWorkflowStore workflowStore;

    public void setWorkflowStore( DataApprovalWorkflowStore workflowStore )
    {
        this.workflowStore = workflowStore;
    }
    
    private DataApprovalLevelService dataApprovalLevelService;

    public void setDataApprovalLevelService( DataApprovalLevelService dataApprovalLevelService )
    {
        this.dataApprovalLevelService = dataApprovalLevelService;
    }

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private PeriodService periodService;

    public void setPeriodService( PeriodService periodService )
    {
        this.periodService = periodService;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    // -------------------------------------------------------------------------
    // Data approval workflow
    // -------------------------------------------------------------------------

    @Override
    public int addWorkflow( DataApprovalWorkflow workflow )
    {
        workflowStore.save( workflow );

        return workflow.getId();
    }

    @Override
    public void updateWorkflow( DataApprovalWorkflow dataApprovalWorkflow )
    {
        workflowStore.update( dataApprovalWorkflow );
    }

    @Override
    public void deleteWorkflow( DataApprovalWorkflow workflow )
    {
        workflowStore.delete( workflow );
    }

    @Override
    public DataApprovalWorkflow getWorkflow( int id )
    {
        return workflowStore.get( id );
    }

    @Override
    public DataApprovalWorkflow getWorkflow( String uid )
    {
        return workflowStore.getByUid( uid );
    }

    @Override
    public List<DataApprovalWorkflow> getAllWorkflows()
    {
        return workflowStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Data approval logic
    // -------------------------------------------------------------------------

    @Override
    public void approveData( List<DataApproval> dataApprovalList )
    {
        log.debug( "approveData ( " + dataApprovalList.size() + " items )" );

        boolean accepted = ! (Boolean) systemSettingManager.getSystemSetting( SettingKey.ACCEPTANCE_REQUIRED_FOR_APPROVAL );

        User currentUser = currentUserService.getCurrentUser();

        validateAttributeOptionCombos( dataApprovalList );

        Map<String, DataApprovalStatus> statusMap = getStatusMap( dataApprovalList );

        List<DataApproval> checkedList = new ArrayList<>();

        for ( DataApproval da : dataApprovalList )
        {
            if ( !da.getPeriod().getPeriodType().equals( da.getWorkflow().getPeriodType() ) )
            {
                log.info( "approveData: data may not be approved, approval period type = "
                    + da.getPeriod().getPeriodType().getName() + ", workflow period type = "
                    + da.getWorkflow().getPeriodType().getName() + " " + da );

                throw new DataMayNotBeApprovedException();
            }

            DataApprovalStatus status = statusMap.get( daKey( da ) );

            if ( status == null )
            {
                log.info( "approveData: data may not be approved, status = null " + " " + da );

                throw new DataMayNotBeApprovedException();
            }
            else if ( status.getState().isApproved() )
            {
                if ( da.getDataApprovalLevel() != null && status.getActionLevel() != null &&
                    da.getDataApprovalLevel().getLevel() >= status.getActionLevel().getLevel() )
                {
                    continue; // Already approved at or above the level requested
                }

                DataApprovalLevel nextLevel = nextHigherLevel( status.getActionLevel(), da.getWorkflow() );

                if ( da.getDataApprovalLevel() == null )
                {
                    da.setDataApprovalLevel( nextLevel );
                }
                else if ( da.getDataApprovalLevel().getLevel() != nextLevel.getLevel() )
                {
                    log.info( "approveData: data approved under workflow " + da.getWorkflow().getName()
                        + " at level " + status.getActionLevel().getLevel()
                        + " is ready for level " + nextLevel.getLevel() + ", not level " + da.getDataApprovalLevel().getLevel() );

                    throw new DataMayNotBeApprovedException();
                }
            }
            else if ( da.getDataApprovalLevel() == null )
            {
                da.setDataApprovalLevel( status.getActionLevel() );
            }
            else if ( status.getActionLevel() == null )
            {
                log.info( "approveData: unapproved data in workflow " + da.getWorkflow().getName()
                    + " does not have actionable level. " );

                throw new DataMayNotBeApprovedException();
            }
            else if ( da.getDataApprovalLevel().getLevel() != status.getActionLevel().getLevel() )
            {
                log.info( "approveData: unapproved data in workflow " + da.getWorkflow().getName()
                    + " must first be approved at level " + status.getActionLevel().getLevel() );

                throw new DataMayNotBeApprovedException();
            }

            if ( !status.getPermissions().isMayApprove() )
            {
                log.info( "approveData: data may not be approved, state " +
                    status.getState().name() + " " + da );

                throw new DataMayNotBeApprovedException();
            }

            if ( da.getOrganisationUnit().getLevel() != da.getDataApprovalLevel().getOrgUnitLevel() )
            {
                log.info( "approveData: org unit " + da.getOrganisationUnit().getUid() + " '" + da.getOrganisationUnit().getName() +
                    "' has wrong org unit level " + da.getOrganisationUnit().getLevel() +
                    " for approval level " + da.getDataApprovalLevel().getLevel() );

                throw new DataMayNotBeApprovedException();
            }

            da.setAccepted( accepted );

            checkedList.add ( da );
        }

        for ( DataApproval da : checkedList )
        {
            log.debug( "-> approving " + da );

            audit( da, currentUser, APPROVE );

            dataApprovalStore.addDataApproval( da );
        }
        
        log.info( "Approvals saved: " + checkedList.size() );
    }

    @Override
    public void unapproveData( List<DataApproval> dataApprovalList )
    {
        log.debug( "unapproveData ( " + dataApprovalList.size() + " items )" );

        User currentUser = currentUserService.getCurrentUser();

        Map<String, DataApprovalStatus> statusMap = getStatusMap( dataApprovalList );

        List<DataApproval> checkedList = new ArrayList<>();

        for ( DataApproval da : dataApprovalList )
        {
            DataApprovalStatus status = statusMap.get( daKey( da ) );

            if ( da.getDataApprovalLevel() == null )
            {
                da.setDataApprovalLevel( status.getActionLevel() );
            }

            if ( status == null || !status.getPermissions().isMayUnapprove() )
            {
                log.info( "unapproveData: data may not be unapproved, state " + ( status == null ? "(null)" : status.getState().name() ) + " " + da );

                throw new DataMayNotBeUnapprovedException();
            }

            if ( !status.getState().isApproved() || ( da.getDataApprovalLevel() != null &&
                da.getDataApprovalLevel().getLevel() < status.getApprovedLevel().getLevel() ) )
            {
                continue; // Already unapproved at or below this level
            }

            checkedList.add ( da );
        }

        List<DataApproval> foundApprovals = getPresentApprovals( checkedList, "unapprove" );

        for ( DataApproval da : foundApprovals )
        {
            log.debug( "unapproving " + da );

            audit( da, currentUser, UNAPPROVE );

            dataApprovalStore.deleteDataApproval( da );
        }
        
        log.info( "Approvals deleted: " + dataApprovalList.size() );
    }

    @Override
    public void acceptData( List<DataApproval> dataApprovalList )
    {
        log.debug( "acceptData ( " + dataApprovalList.size() + " items )" );

        User currentUser = currentUserService.getCurrentUser();

        Map<String, DataApprovalStatus> statusMap = getStatusMap( dataApprovalList );

        List<DataApproval> checkedList = new ArrayList<>();

        for ( DataApproval da : dataApprovalList )
        {
            DataApprovalStatus status = statusMap.get( daKey( da ) );

            if ( da.getDataApprovalLevel() == null )
            {
                da.setDataApprovalLevel( status.getActionLevel() );
            }

            if ( status != null && status.getState() != null && status.getApprovedLevel() != null &&
                ( status.getState().isAccepted() && da.getDataApprovalLevel().getLevel() == status.getApprovedLevel().getLevel() ||
                da.getDataApprovalLevel().getLevel() > status.getApprovedLevel().getLevel() ) )
            {
                continue; // Already accepted at, or approved above, this level
            }

            if ( status == null || !status.getPermissions().isMayAccept() )
            {
                log.info( "acceptData: data may not be accepted, state " + ( status == null ? "(null)" : status.getState().name() ) + " " + da );

                throw new DataMayNotBeAcceptedException();
            }

            checkedList.add ( da );
        }

        List<DataApproval> presentApprovals = getPresentApprovals( checkedList, "accept"  );

        for ( DataApproval da : presentApprovals )
        {
            log.debug( "accepting " + da );

            da.setAccepted( true );

            audit( da, currentUser, ACCEPT );

            dataApprovalStore.updateDataApproval( da );
        }
        
        log.info( "Accepts saved: " + dataApprovalList.size() );
    }

    @Override
    public void unacceptData( List<DataApproval> dataApprovalList )
    {
        log.debug( "unacceptData ( " + dataApprovalList.size() + " items )" );

        User currentUser = currentUserService.getCurrentUser();

        Map<String, DataApprovalStatus> statusMap = getStatusMap( dataApprovalList );

        List<DataApproval> checkedList = new ArrayList<>();

        for ( DataApproval da : dataApprovalList )
        {
            DataApprovalStatus status = statusMap.get( daKey( da ) );

            if ( da.getDataApprovalLevel() == null )
            {
                da.setDataApprovalLevel( status.getActionLevel() );
            }

            if ( status == null || ( !status.getState().isAccepted() && da.getDataApprovalLevel() != null && da.getDataApprovalLevel().getLevel() == status.getApprovedLevel().getLevel() ) ||
                da.getDataApprovalLevel().getLevel() < status.getApprovedLevel().getLevel() )
            {
                continue; // Already unaccepted at or not approved up to this level
            }

            if ( !status.getPermissions().isMayUnaccept() )
            {
                log.info( "unacceptData: data may not be unaccepted, state " + status.getState().name() + " " + da + " " + status.getPermissions() );

                throw new DataMayNotBeUnacceptedException();
            }

            checkedList.add ( da );
        }

        List<DataApproval> presentApprovals = getPresentApprovals( checkedList, "unaccept"  );

        for ( DataApproval da : presentApprovals )
        {
            log.debug( "unaccepting " + da );

            da.setAccepted( false );

            audit( da, currentUser, UNACCEPT );

            dataApprovalStore.updateDataApproval( da );
        }
        
        log.info( "Accepts deleted: " + dataApprovalList.size() );
    }

    @Override
    public DataApproval getDataApproval( DataApproval dataApproval )
    {
        return dataApproval == null ? null : dataApprovalStore.getDataApproval( dataApproval );
    }

    @Override
    public boolean isApproved( DataApprovalWorkflow workflow, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo )
    {
        if ( workflow == null )
        {
            return false;
        }

        DataApproval da = new DataApproval( null, workflow, period, organisationUnit, attributeOptionCombo );

        da = DataApproval.getLowestApproval( da );

        return da != null ? dataApprovalStore.dataApprovalExists( da ) : false;
    }

    @Override
    public Map<DataApproval, DataApprovalStatus> getDataApprovalStatuses( List<DataApproval> dataApprovalList )
    {
        Map<String, DataApprovalStatus> statusMap = getStatusMap( dataApprovalList );

        DataApprovalPermissionsEvaluator permissionsEvaluator = makePermissionsEvaluator();

        Map<DataApproval, DataApprovalStatus> returnMap = new HashMap<>();

        for ( DataApproval da : dataApprovalList )
        {
            DataApprovalStatus status = statusMap.get( daKey( da ) );

            if ( status != null )
            {
                permissionsEvaluator.evaluatePermissions( status, da.getWorkflow() );
            }

            returnMap.put( da, statusMap.get( daKey( da ) ) );
        }

        return returnMap;
    }

    @Override
    public DataApprovalStatus getDataApprovalStatus( DataApprovalWorkflow workflow, Period period,
        OrganisationUnit organisationUnit, CategoryOptionCombo attributeOptionCombo )
    {
        log.debug( "getDataApprovalStatus( " + workflow.getName() + ", "
            + period.getPeriodType().getName() + " " + period.getName() + " " + period + ", "
            + organisationUnit.getName() + ", "
            + ( attributeOptionCombo == null ? "(null)" : attributeOptionCombo.getName() ) + " )" );

        DataApprovalStatus status;

        List<DataApprovalStatus> statuses = dataApprovalStore.getDataApprovalStatuses( workflow,
            periodService.reloadPeriod( period ), Lists.newArrayList( organisationUnit ),
            organisationUnit.getHierarchyLevel(), null,
            attributeOptionCombo == null ? null : Sets.newHashSet( attributeOptionCombo ) );

        if ( statuses == null || statuses.isEmpty() )
        {
            status = new DataApprovalStatus( DataApprovalState.UNAPPROVABLE );
        }
        else
        {
            status = statuses.get( 0 );

            if ( status.getApprovedLevel() != null )
            {
                OrganisationUnit approvedOrgUnit = organisationUnitService.getOrganisationUnit( status.getApprovedOrgUnitId() );

                DataApproval da = dataApprovalStore.getDataApproval( status.getActionLevel(),
                    workflow, period, approvedOrgUnit, attributeOptionCombo );

                if ( da != null )
                {
                    status.setCreated( da.getCreated() );
                    status.setCreator( da.getCreator() );
                }
            }
        }

        makePermissionsEvaluator().evaluatePermissions( status, workflow );

        return status;
    }

    @Override
    public List<DataApprovalStatus> getUserDataApprovalsAndPermissions( DataApprovalWorkflow workflow,
        Period period, OrganisationUnit orgUnit, CategoryCombo attributeCombo )
    {
        List<DataApprovalStatus> statusList = dataApprovalStore.getDataApprovalStatuses( workflow, period,
            orgUnit == null ? null : Lists.newArrayList( orgUnit ),
            orgUnit == null ? 0 : orgUnit.getHierarchyLevel(),
            attributeCombo, null );

        DataApprovalPermissionsEvaluator permissionsEvaluator = makePermissionsEvaluator();

        for ( DataApprovalStatus status : statusList )
        {
            permissionsEvaluator.evaluatePermissions( status, workflow );
        }

        return statusList;
    }
    
    @Override
    public void deleteDataApprovals( OrganisationUnit organisationUnit )
    {
        dataApprovalStore.deleteDataApprovals( organisationUnit );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Audits a data approval action.
     *
     * @param da the details of the action.
     * @param currentUser the current user.
     * @param action the data approval action to audit.
     */
    private void audit( DataApproval da, User currentUser, DataApprovalAction action )
    {
        DataApprovalAudit audit = new DataApprovalAudit( da, action );

        audit.setCreated( new Date() );
        audit.setCreator( currentUser );

        dataApprovalAuditStore.save( audit );
    }

    /**
     * Returns the next higher (lower number) level within a workflow.
     */
    private DataApprovalLevel nextHigherLevel( DataApprovalLevel level, DataApprovalWorkflow workflow )
    {
        List<DataApprovalLevel> sortedLevels = workflow.getSortedLevels();

        for ( int i = 0; i < sortedLevels.size(); i++ )
        {
            if ( i > 0 && sortedLevels.get( i ).getLevel() == level.getLevel() )
            {
                return sortedLevels.get( i - 1 );
            }
        }
        
        return level;
    }

    /**
     * Makes sure that for any approval we enter into the database, the
     * attributeOptionCombo appears in the set of optionCombos for at least
     * one of the data sets in the workflow.
     *
     * @param dataApprovalList list of data approvals to test.
     */
    private void validateAttributeOptionCombos( List<DataApproval> dataApprovalList )
    {
        for ( DataApproval da : dataApprovalList )
        {
            validAttributeOptionCombo( da.getAttributeOptionCombo(), da.getWorkflow() );
        }
    }

    /**
     * Makes sure that an attribute option combo is valid for a workflow.
     *
     * @param attributeOptionCombo attribute option combo to test.
     * @param workflow workflow to check against.
     */
    private void validAttributeOptionCombo( CategoryOptionCombo attributeOptionCombo, DataApprovalWorkflow workflow )
    {
        for ( DataSet ds : workflow.getDataSets() )
        {
            if ( ds.getCategoryCombo().getOptionCombos().contains( attributeOptionCombo ) )
            {
                return;
            }
        }

        log.info( "validateAttributeOptionCombos: attribuetOptionCombo "
            + attributeOptionCombo.getUid() + " not valid for workflow "
            + workflow.getUid() );

        throw new DataMayNotBeApprovedException();
    }

    /**
     * Returns a mapping from data approval key to data approval status for the given
     * list of data approvals.
     */
    private Map<String, DataApprovalStatus> getStatusMap( List<DataApproval> dataApprovalList )
    {
        Map<String, DataApprovalStatus> statusMap = new HashMap<>();

        DataApprovalPermissionsEvaluator evaluator = makePermissionsEvaluator();

        ListMap<String, DataApproval> listMap = getIndexedListMap( dataApprovalList );
        
        for ( Map.Entry<String, List<DataApproval>> entry : listMap.entrySet() )
        {
            List<DataApproval> dataApprovals = entry.getValue();

            Set<OrganisationUnit> orgUnits = dataApprovals.stream().map( DataApproval::getOrganisationUnit ).collect( Collectors.toSet() );

            DataApproval da = dataApprovals.get( 0 );

            List<DataApprovalStatus> statuses = dataApprovalStore.getDataApprovalStatuses( da.getWorkflow(),
                da.getPeriod(), orgUnits, da.getOrganisationUnit().getHierarchyLevel(), null, getCategoryOptionCombos( dataApprovals ) );

            for ( DataApprovalStatus status : statuses )
            {
                evaluator.evaluatePermissions( status, da.getWorkflow() );

                statusMap.put( daKey( da, status.getOrganisationUnitUid(), status.getAttributeOptionComboUid() ), status );
            }
        }

        return statusMap;
    }

    /**
     * Returns an indexed map where the key is based on each distinct
     * combination of organisation unit level, period, and workflow.
     *
     * If multiple attributeOptionCombo values are needed for the same
     * combination of organisation unit level, period, and workflow, then
     * these are fetched at the same time time, for better performance.
     */
    private ListMap<String, DataApproval> getIndexedListMap( List<DataApproval> dataApprovalList )
    {
        ListMap<String, DataApproval> map = new ListMap<>();

        for ( DataApproval approval : dataApprovalList )
        {
            map.putValue( statusKey( approval ), approval );
        }

        return map;
    }

    /**
     * Returns a key consisting of organisation unit, period, and workflow.
     * Approval status with these three values in common can be fetched in
     * one call for many values of attributeOptionCombo.
     */
    private String statusKey( DataApproval approval )
    {
        return approval == null ? null :
            approval.getOrganisationUnit().getId() +
                IdentifiableObjectUtils.SEPARATOR + approval.getPeriod().getId() +
                IdentifiableObjectUtils.SEPARATOR + approval.getWorkflow().getId();
    }

    /**
     * Looks up which data approvals are present in the database, and returns
     * a list of database approval objects (with approval id).
     *
     * Throws an exception if any approval is not present in the database.
     *
     * This is done by making a single call to the database level instead of
     * one call per data approval, to speed performance in the case where
     * there are many data approvals.
     *
     * @param approvals the list of approvals to check.
     * @param operation operation (for error logging).
     * @return the list of those approvals actually present.
     */
    private List<DataApproval> getPresentApprovals ( List<DataApproval> approvals, String operation )
    {
        Set<DataApprovalLevel> levels = new HashSet<>();
        Set<DataApprovalWorkflow> workflows = new HashSet<>();
        Set<Period> periods = new HashSet<>();
        Set<OrganisationUnit> organisationUnits = new HashSet<>();
        Set<CategoryOptionCombo> attributeOptionCombos = new HashSet<>();

        for ( DataApproval a : approvals )
        {
            levels.add( a.getDataApprovalLevel() );
            workflows.add( a.getWorkflow() );
            periods.add( a.getPeriod() );
            organisationUnits.add( a.getOrganisationUnit() );
            attributeOptionCombos.add( a.getAttributeOptionCombo() );
        }

        List<DataApproval> foundList = dataApprovalStore.getDataApprovals( levels,
            workflows, periods, organisationUnits, attributeOptionCombos );

        Map<String, DataApproval> foundMap = foundList.stream().collect( Collectors.toMap( a -> approvalKey( a ), a -> a ) );

        List<DataApproval> presentApprovals = new ArrayList<>();

        for ( DataApproval a : approvals )
        {
            DataApproval present = foundMap.get( approvalKey( a ) );

            if ( present != null )
            {
                presentApprovals.add( present );
            }
            else
            {
                log.info( operation + ": approval not found at " + a );

                throw new DataApprovalNotFound( operation + " " + a.toString() );
            }
        }

        return presentApprovals;
    }

    /**
     * Returns a key for a data approval object, consisting of the UIDs
     * of all the approval object dimensions that uniquely define it.
     *
     * @param da the data approval object.
     * @return a key for the object.
     */
    private String approvalKey( DataApproval da )
    {
        return da.getDataApprovalLevel().getUid()
            + da.getWorkflow().getUid()
            + da.getPeriod().getCode()
            + da.getOrganisationUnit().getUid()
            + da.getAttributeOptionCombo().getUid();
    }

    /**
     * Returns a key consisting of statusKey + attributeOptionCombo.
     * This can identify a particular DataApproval object in the return
     * set of statuses.
     */
    private String daKey ( DataApproval da )
    {
        return daKey( da, da.getOrganisationUnit().getUid(),
            da.getAttributeOptionCombo() == null ? "null" : da.getAttributeOptionCombo().getUid() );
    }

    private String daKey ( DataApproval da, String orgUnitUid, String attributeOptionComboUid )
    {
        return da.getWorkflow().getUid()
            + da.getPeriod().getCode()
            + orgUnitUid
            + attributeOptionComboUid;
    }

    /**
     * Returns a set of CategoryOptionCombos from a list of DataApprovals.
     */
    private Set<CategoryOptionCombo> getCategoryOptionCombos( List<DataApproval> dataApprovals )
    {
        Set<CategoryOptionCombo> combos = new HashSet<>();

        for ( DataApproval da : dataApprovals )
        {
            combos.add( da.getAttributeOptionCombo() );
        }

        return combos;
    }

    /**
     * Makes a DataApprovalPermissionsEvaluator object for the current user.
     */
    private DataApprovalPermissionsEvaluator makePermissionsEvaluator()
    {
        return DataApprovalPermissionsEvaluator.makePermissionsEvaluator(
            currentUserService, organisationUnitService, systemSettingManager, dataApprovalLevelService );
    }
}
