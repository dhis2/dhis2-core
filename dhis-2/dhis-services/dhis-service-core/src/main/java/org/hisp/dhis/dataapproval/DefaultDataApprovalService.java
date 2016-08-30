package org.hisp.dhis.dataapproval;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeAcceptedException;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeApprovedException;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeUnacceptedException;
import org.hisp.dhis.dataapproval.exceptions.DataMayNotBeUnapprovedException;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

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
        return workflowStore.save( workflow );
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

        Map<String, DataApprovalStatus> statusMap = getStatusMap( dataApprovalList );

        List<DataApproval> checkedList = new ArrayList<>();

        for ( DataApproval da : dataApprovalList )
        {
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

            checkedList.add ( da );
        }

        for ( DataApproval da : checkedList )
        {
            log.debug( "-> approving " + da );

            dataApprovalStore.addDataApproval( da );
        }
        
        log.info( "Approvals saved: " + checkedList.size() );
    }

    @Override
    public void unapproveData( List<DataApproval> dataApprovalList )
    {
        log.debug( "unapproveData ( " + dataApprovalList.size() + " items )" );

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

        for ( DataApproval da : checkedList )
        {
            log.debug( "unapproving " + da );

            DataApproval d = dataApprovalStore.getDataApproval( da );

            if ( d == null )
            {
                log.warn( "unapproveData: approval not found at " + da );

                throw new DataMayNotBeUnapprovedException();
            }

            dataApprovalStore.deleteDataApproval( d );
        }
        
        log.info( "Approvals deleted: " + dataApprovalList.size() );
    }

    @Override
    public void acceptData( List<DataApproval> dataApprovalList )
    {
        log.debug( "acceptData ( " + dataApprovalList.size() + " items )" );

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

        for ( DataApproval da : checkedList )
        {
            da.setAccepted( true );

            log.debug( "accepting " + da );

            DataApproval d = dataApprovalStore.getDataApproval( da );

            if ( d == null )
            {
                log.info( "acceptData: approval not found at " + da );

                throw new DataMayNotBeAcceptedException();
            }

            d.setAccepted( true );

            dataApprovalStore.updateDataApproval( d );
        }
        
        log.info( "Accepts saved: " + dataApprovalList.size() );
    }

    @Override
    public void unacceptData( List<DataApproval> dataApprovalList )
    {
        log.debug( "unacceptData ( " + dataApprovalList.size() + " items )" );

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

        for ( DataApproval da : checkedList )
        {
            log.debug( "unaccepting " + da );

            DataApproval d = dataApprovalStore.getDataApproval( da );

            if ( d == null )
            {
                log.info( "unacceptData: approval not found at " + da );

                throw new DataMayNotBeUnacceptedException();
            }

            d.setAccepted( false );

            dataApprovalStore.updateDataApproval( d );
        }
        
        log.info( "Accepts deleted: " + dataApprovalList.size() );
    }

    @Override
    public DataApproval getDataApproval( DataApproval dataApproval )
    {
        return dataApproval == null ? null : dataApprovalStore.getDataApproval( dataApproval );
    }

    @Override
    public DataApproval lowestApproval( DataApproval dataApproval )
    {
        OrganisationUnit orgUnit = dataApproval.getOrganisationUnit();

        List<DataApprovalLevel> approvalLevels = dataApproval.getWorkflow().getSortedLevels();

        Collections.reverse( approvalLevels );

        DataApproval da = null;

        for ( DataApprovalLevel approvalLevel : approvalLevels )
        {
            if ( approvalLevel.getOrgUnitLevel() <= orgUnit.getLevel() )
            {
                if ( approvalLevel.getOrgUnitLevel() < orgUnit.getLevel() )
                {
                    orgUnit = orgUnit.getAncestors().get( approvalLevel.getOrgUnitLevel() - 1 );
                }
                da = new DataApproval( approvalLevel, dataApproval.getWorkflow(),
                    dataApproval.getPeriod(), orgUnit, dataApproval.getAttributeOptionCombo() );

                break;
            }
        }

        return da;
    }

    @Override
    public boolean isApproved( DataApprovalWorkflow workflow, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        if ( workflow == null )
        {
            return false;
        }

        DataApproval da = new DataApproval ( null, workflow, period, organisationUnit, attributeOptionCombo );

        da = lowestApproval ( da );

        if ( da != null )
        {
            da = getDataApproval( da );
        }

        return da != null;
    }

    @Override
    public DataApprovalStatus getDataApprovalStatus( DataApprovalWorkflow workflow, Period period,
        OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        log.debug( "getDataApprovalStatus( " + workflow.getName() + ", "
            + period.getPeriodType().getName() + " " + period.getName() + " " + period + ", "
            + organisationUnit.getName() + ", "
            + ( attributeOptionCombo == null ? "(null)" : attributeOptionCombo.getName() ) + " )" );

        List<DataApprovalStatus> statuses = dataApprovalStore.getDataApprovals( workflow,
            periodService.reloadPeriod( period ), organisationUnit, null,
            attributeOptionCombo == null ? null : Sets.newHashSet( attributeOptionCombo ) );

        if ( statuses != null && !statuses.isEmpty() )
        {
            DataApprovalStatus status = statuses.get( 0 );

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

            return status;
        }

        return new DataApprovalStatus( DataApprovalState.UNAPPROVABLE );
    }

    @Override
    public DataApprovalStatus getDataApprovalStatusAndPermissions( DataApprovalWorkflow workflow,
        Period period, OrganisationUnit organisationUnit, DataElementCategoryOptionCombo attributeOptionCombo )
    {
        DataApprovalStatus status = getDataApprovalStatus( workflow, period, organisationUnit, attributeOptionCombo );

        status.setPermissions( makePermissionsEvaluator().getPermissions( status, organisationUnit, workflow ) );

        return status;
    }

    @Override
    public List<DataApprovalStatus> getUserDataApprovalsAndPermissions( DataApprovalWorkflow workflow,
        Period period, OrganisationUnit orgUnit, DataElementCategoryCombo attributeCombo )
    {
        List<DataApprovalStatus> statusList = dataApprovalStore.getDataApprovals( workflow, period, orgUnit, attributeCombo, null );

        DataApprovalPermissionsEvaluator permissionsEvaluator = makePermissionsEvaluator();

        for ( DataApprovalStatus status : statusList )
        {
            status.setPermissions( permissionsEvaluator.getPermissions( status, orgUnit, workflow ) );
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
     * Returns a mapping from data approval key to data approval status for the given
     * list of data approvals.
     */
    private Map<String, DataApprovalStatus> getStatusMap( List<DataApproval> dataApprovalList )
    {
        Map<String, DataApprovalStatus> statusMap = new HashMap<>();

        DataApprovalPermissionsEvaluator evaluator = makePermissionsEvaluator();

        ListMap<String, DataApproval> listMap = getIndexedListMap( dataApprovalList );
        
        for ( String key : listMap.keySet() )
        {
            List<DataApproval> dataApprovals = listMap.get( key );
            
            DataApproval da = dataApprovals.get( 0 );

            List<DataApprovalStatus> statuses = dataApprovalStore.getDataApprovals( da.getWorkflow(),
                da.getPeriod(), da.getOrganisationUnit(), null, getCategoryOptionCombos( dataApprovals ) );

            for ( DataApprovalStatus status : statuses )
            {
                status.setPermissions( evaluator.getPermissions( status, da.getOrganisationUnit(), da.getWorkflow() ) );

                statusMap.put( daKey( da, status.getAttributeOptionComboUid() ), status );
            }
        }

        return statusMap;
    }

    /**
     * Returns an indexed map where the key is based on each distinct
     * combination of organisation unit, period, and workflow.
     *
     * If multiple attributeOptionCombo values are needed for the same
     * combination of organisation unit, period, and workflow, then
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
        return approval == null ? null : approval.getOrganisationUnit().getId() +
            IdentifiableObjectUtils.SEPARATOR + approval.getPeriod().getId() +
            IdentifiableObjectUtils.SEPARATOR + approval.getWorkflow().getId();
    }

    /**
     * Returns a key consisting of statusKey + attributeOptionCombo.
     * This can identify a particular DataApproval object in the return
     * set of statuses.
     */
    private String daKey ( DataApproval approval )
    {
        return daKey( approval, approval.getAttributeOptionCombo() == null ? "null" : approval.getAttributeOptionCombo().getUid() );
    }

    private String daKey ( DataApproval approval, String attributeOptionComboUid )
    {
        return statusKey( approval ) + IdentifiableObjectUtils.SEPARATOR + attributeOptionComboUid;
    }

    /**
     * Returns a set of CategoryOptionCombos from a list of DataApprovals.
     */
    private Set<DataElementCategoryOptionCombo> getCategoryOptionCombos( List<DataApproval> dataApprovals )
    {
        Set<DataElementCategoryOptionCombo> combos = new HashSet<>();

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
