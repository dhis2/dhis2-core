package org.hisp.dhis.webapi.controller;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataapproval.DataApprovalPermissions;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalStateRequest;
import org.hisp.dhis.dataapproval.DataApprovalStateRequests;
import org.hisp.dhis.dataapproval.DataApprovalStateResponse;
import org.hisp.dhis.dataapproval.DataApprovalStateResponses;
import org.hisp.dhis.dataapproval.DataApprovalStatus;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.approval.Approval;
import org.hisp.dhis.webapi.webdomain.approval.Approvals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

/**
 * This controller uses both /dataApprovals and /dataAcceptances.
 *
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataApprovalController
{
    public static final String APPROVALS_PATH = "/dataApprovals";
    private static final String STATUS_PATH = APPROVALS_PATH + "/status";
    private static final String MULTIPLE_SAVE_RESOURCE_PATH = APPROVALS_PATH + "/multiple";

    public static final String ACCEPTANCES_PATH = "/dataAcceptances";
    private static final String MULTIPLE_ACCEPTANCES_RESOURCE_PATH = ACCEPTANCES_PATH + "/multiple";

    @Autowired
    private DataApprovalService dataApprovalService;

    @Autowired
    private DataApprovalLevelService dataApprovalLevelService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager objectManager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private UserService userService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private RenderService renderService;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private ContextService contextService;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @RequestMapping( value = APPROVALS_PATH, method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public void getApprovalPermissions(
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String wf,
        @RequestParam String pe,
        @RequestParam String ou, HttpServletResponse response )
        throws IOException, WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );

        DataElementCategoryOptionCombo optionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        DataApprovalStatus status = dataApprovalService
            .getDataApprovalStatusAndPermissions( workflow, period, organisationUnit, optionCombo );

        DataApprovalPermissions permissions = status.getPermissions();
        permissions.setState( status.getState().toString() );

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), status.getPermissions() );
    }

    @RequestMapping( value = STATUS_PATH, method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getApproval(
        @RequestParam Set<String> ds,
        @RequestParam( required = false ) String pe,
        @RequestParam Date startDate,
        @RequestParam Date endDate,
        @RequestParam Set<String> ou,
        @RequestParam( required = false ) boolean children,
        HttpServletResponse response )
        throws IOException, WebMessageException
    {
        List<String> fields = Lists.newArrayList( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
            List<String> defaults = new ArrayList<>();
            defaults.add( "period[id,name,code],organisationUnit[id,name,created,lastUpdated],dataSet[code,name,created,lastUpdated,id]" );
            fields.addAll( defaults );
        }

        Set<DataSet> dataSets = parseDataSetsWithWorkflow( ds );

        Set<Period> periods = new HashSet<>();

        PeriodType periodType = periodService.getPeriodTypeByName( pe );

        if ( periodType != null )
        {
            periods.addAll( periodService.getPeriodsBetweenDates( periodType, startDate, endDate ) );
        }
        else
        {
            periods.addAll( periodService.getPeriodsBetweenDates( startDate, endDate ) );
        }

        Set<OrganisationUnit> organisationUnits = new HashSet<>();

        if ( children )
        {
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitsWithChildren( ou ) );
        }
        else
        {
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitsByUid( ou ) );
        }

        DataApprovalStateResponses dataApprovalStateResponses = new DataApprovalStateResponses();

        for ( DataSet dataSet : dataSets )
        {
            for ( OrganisationUnit organisationUnit : organisationUnits )
            {
                for ( Period period : periods )
                {
                    dataApprovalStateResponses.add(
                        getDataApprovalStateResponse( dataSet, organisationUnit, period ) );
                }
            }
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        RootNode rootNode = NodeUtils.createMetadata();

        rootNode.addChild( fieldFilterService.filter( DataApprovalStateResponse.class, dataApprovalStateResponses.getDataApprovalStateResponses(), fields ) );

        return rootNode;
    }

    private DataApprovalStateResponse getDataApprovalStateResponse( DataSet dataSet,
        OrganisationUnit organisationUnit, Period period )
    {
        DataElementCategoryOptionCombo optionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();

        DataApprovalStatus status = dataApprovalService.getDataApprovalStatusAndPermissions( dataSet.getWorkflow(), period,
            organisationUnit, optionCombo );

        Date createdDate = status.getCreated();
        String createdByUsername = status.getCreator() == null ? null : status.getCreator().getUsername();

        String state = status.getState().toString();

        return new DataApprovalStateResponse( dataSet, period, organisationUnit, state,
            createdDate, createdByUsername, status.getPermissions() );
    }

    @RequestMapping( value = APPROVALS_PATH + "/categoryOptionCombos", method = RequestMethod.GET, produces = ContextUtils.CONTENT_TYPE_JSON )
    public void getApprovalByCategoryOptionCombos(
        @RequestParam Set<String> ds,
        @RequestParam String pe,
        @RequestParam( required = false ) String ou,
        HttpServletResponse response ) throws IOException, WebMessageException
    {
        Set<DataSet> dataSets = parseDataSetsWithWorkflow( ds );

        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal period identifier: " + pe ) );
        }

        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( orgUnit != null && orgUnit.isRoot() )
        {
            orgUnit = null; // Look for all org units.
        }

        SetMap<DataApprovalWorkflow, DataElementCategoryCombo> workflowCategoryComboMap = new SetMap<>();

        for ( DataSet dataSet : dataSets )
        {
            workflowCategoryComboMap.putValue( dataSet.getWorkflow(), dataSet.getCategoryCombo() );
        }

        List<DataApprovalStatus> statusList = new ArrayList<>();

        for ( DataApprovalWorkflow workflow : workflowCategoryComboMap.keySet() )
        {
            for ( DataElementCategoryCombo attributeCombo : workflowCategoryComboMap.get( workflow ) )
            {
                statusList.addAll( dataApprovalService.getUserDataApprovalsAndPermissions( workflow, period, orgUnit, attributeCombo ) );
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();

        for ( DataApprovalStatus status : statusList )
        {
            Map<String, Object> item = new HashMap<>();

            Map<String, String> approvalLevel = new HashMap<>();

            if ( status.getApprovedLevel() != null )
            {
                approvalLevel.put( "id", status.getApprovedLevel().getUid() );
                approvalLevel.put( "level", String.valueOf( status.getApprovedLevel().getLevel() ) );
            }

            item.put( "id", status.getAttributeOptionComboUid() );
            item.put( "level", approvalLevel );
            item.put( "ou", status.getOrganisationUnitUid() );
            item.put( "ouName", status.getOrganisationUnitName() );
            item.put( "accepted", status.isAccepted() );
            item.put( "permissions", status.getPermissions() );

            list.add( item );
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );
        renderService.toJson( response.getOutputStream(), list );
    }

    // -------------------------------------------------------------------------
    // Post, approval
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_APPROVE_DATA') or hasRole('F_APPROVE_DATA_LOWER_LEVELS')" )
    @RequestMapping( value = APPROVALS_PATH, method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveApproval(
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String wf,
        @RequestParam String pe,
        @RequestParam String ou, HttpServletResponse response ) throws WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = getApprovalsAsList( dataApprovalLevel, workflow,
            period, organisationUnit, false, new Date(), user ); //TODO fix category stuff

        dataApprovalService.approveData( dataApprovalList );
    }

    @RequestMapping( value = APPROVALS_PATH + "/approvals", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveApprovalBatch( @RequestBody Approvals approvals,
        HttpServletRequest request, HttpServletResponse response ) throws WebMessageException
    {
        if ( approvals.getDs() == null || approvals.getDs().isEmpty() || approvals.getPe() == null || approvals.getPe().isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Approval must have data sets and periods" ) );
        }

        dataApprovalService.approveData( getDataApprovalList( approvals ) );
    }

    @RequestMapping( value = APPROVALS_PATH + "/unapprovals", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeApprovalBatch( @RequestBody Approvals approvals,
        HttpServletRequest request, HttpServletResponse response ) throws WebMessageException
    {
        if ( approvals.getDs() == null || approvals.getDs().isEmpty() || approvals.getPe() == null || approvals.getPe().isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Approval must have data sets and periods" ) );
        }

        dataApprovalService.unapproveData( getDataApprovalList( approvals ) );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_APPROVE_DATA') or hasRole('F_APPROVE_DATA_LOWER_LEVELS')" )
    @RequestMapping( value = MULTIPLE_SAVE_RESOURCE_PATH, method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveApprovalMultiple( @RequestBody DataApprovalStateRequests dataApprovalStateRequests,
        HttpServletResponse response ) throws WebMessageException
    {
        List<DataApproval> dataApprovalList = new ArrayList<>();

        for ( DataApprovalStateRequest approvalStateRequest : dataApprovalStateRequests )
        {
            DataApprovalWorkflow workflow = getAndValidateWorkflow( approvalStateRequest.getDs(), null );
            Period period = getAndValidatePeriod( approvalStateRequest.getPe() );
            OrganisationUnit organisationUnit = getAndValidateOrgUnit( approvalStateRequest.getOu() );
            DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );

            User user = approvalStateRequest.getAb() == null ?
                currentUserService.getCurrentUser() :
                userService.getUserCredentialsByUsername( approvalStateRequest.getAb() ).getUserInfo();

            Date approvalDate = approvalStateRequest.getAd() == null ? new Date() : approvalStateRequest.getAd();

            dataApprovalList.addAll( getApprovalsAsList( dataApprovalLevel, workflow,
                period, organisationUnit, false, approvalDate, user ) );
        }

        dataApprovalService.approveData( dataApprovalList );
    }

    // -------------------------------------------------------------------------
    // Post, acceptance
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_ACCEPT_DATA_LOWER_LEVELS')" )
    @RequestMapping( value = ACCEPTANCES_PATH, method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void acceptApproval(
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String wf,
        @RequestParam String pe,
        @RequestParam String ou, HttpServletResponse response ) throws WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = getApprovalsAsList( dataApprovalLevel, workflow,
            period, organisationUnit, false, new Date(), user );

        dataApprovalService.acceptData( dataApprovalList );
    }

    @RequestMapping( value = ACCEPTANCES_PATH + "/acceptances", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveAcceptanceBatch( @RequestBody Approvals approvals,
        HttpServletRequest request, HttpServletResponse response ) throws WebMessageException
    {
        if ( approvals.getDs() == null || approvals.getDs().isEmpty() || approvals.getPe() == null || approvals.getPe().isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Approval must have data sets and periods" ) );
        }

        dataApprovalService.acceptData( getDataApprovalList( approvals ) );
    }

    @RequestMapping( value = ACCEPTANCES_PATH + "/unacceptances", method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeAcceptancesBatch( @RequestBody Approvals approvals,
        HttpServletRequest request, HttpServletResponse response ) throws WebMessageException
    {
        if ( approvals.getDs() == null || approvals.getDs().isEmpty() || approvals.getPe() == null || approvals.getPe().isEmpty() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Approval must have data sets and periods" ) );
        }

        dataApprovalService.unacceptData( getDataApprovalList( approvals ) );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_ACCEPT_DATA_LOWER_LEVELS')" )
    @RequestMapping( value = MULTIPLE_ACCEPTANCES_RESOURCE_PATH, method = RequestMethod.POST )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void acceptApprovalMultiple( @RequestBody DataApprovalStateRequests dataApprovalStateRequests,
        HttpServletResponse response ) throws WebMessageException
    {
        List<DataApproval> dataApprovalList = new ArrayList<>();

        for ( DataApprovalStateRequest approvalStateRequest : dataApprovalStateRequests )
        {
            DataApprovalWorkflow workflow = getAndValidateWorkflow( approvalStateRequest.getDs(), null );
            Period period = getAndValidatePeriod( approvalStateRequest.getPe() );
            OrganisationUnit organisationUnit = getAndValidateOrgUnit( approvalStateRequest.getOu() );
            DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );

            User user = approvalStateRequest.getAb() == null ?
                currentUserService.getCurrentUser() : userService.getUserCredentialsByUsername( approvalStateRequest.getAb() ).getUserInfo();

            Date approvalDate = (approvalStateRequest.getAd() == null) ? new Date() : approvalStateRequest.getAd();

            dataApprovalList.addAll( getApprovalsAsList( dataApprovalLevel, workflow,
                period, organisationUnit, false, approvalDate, user ) );
        }

        dataApprovalService.acceptData( dataApprovalList );
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_APPROVE_DATA') or hasRole('F_APPROVE_DATA_LOWER_LEVELS')" )
    @RequestMapping( value = APPROVALS_PATH, method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeApproval(
        @RequestParam Set<String> ds,
        @RequestParam String pe,
        @RequestParam String ou, HttpServletResponse response ) throws WebMessageException
    {
        Set<DataSet> dataSets = parseDataSetsWithWorkflow( ds );

        if ( dataSets.size() != ds.size() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal data set identifier in this list: " + ds ) );
        }

        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = newArrayList();

        for ( DataSet dataSet : dataSets )
        {
            dataApprovalList.addAll( getApprovalsAsList( dataApprovalLevel, dataSet.getWorkflow(),
                period, organisationUnit, false, new Date(), user ) );
        }

        dataApprovalService.unapproveData( dataApprovalList );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_ACCEPT_DATA_LOWER_LEVELS')" )
    @RequestMapping( value = ACCEPTANCES_PATH, method = RequestMethod.DELETE )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void unacceptApproval(
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String wf,
        @RequestParam String pe,
        @RequestParam String ou, HttpServletResponse response ) throws WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = getApprovalsAsList( dataApprovalLevel, workflow,
            period, organisationUnit, false, new Date(), user );

        dataApprovalService.unacceptData( dataApprovalList );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<DataSet> parseDataSetsWithWorkflow( Set<String> ds ) throws WebMessageException
    {
        Set<DataSet> dataSets = new HashSet<>( objectManager.getByUid( DataSet.class, ds ) );

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet.getWorkflow() == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "DataSet does not have an approval workflow: " + dataSet.getUid() ) );
            }
        }

        return dataSets;
    }

    private List<DataApproval> getApprovalsAsList( DataApprovalLevel dataApprovalLevel, DataApprovalWorkflow workflow,
        Period period, OrganisationUnit organisationUnit, boolean accepted, Date created, User creator )
    {
        List<DataApproval> approvals = new ArrayList<>();

        DataElementCategoryOptionCombo combo = categoryService.getDefaultDataElementCategoryOptionCombo();
        period = periodService.reloadPeriod( period );

        approvals.add( new DataApproval( dataApprovalLevel, workflow, period, organisationUnit, combo, accepted, created, creator ) );

        return approvals;
    }

    private List<DataApproval> getDataApprovalList( Approvals approvals ) throws WebMessageException
    {
        List<DataSet> dataSets = objectManager.getByUid( DataSet.class, approvals.getDs() );
        List<Period> periods = PeriodType.getPeriodsFromIsoStrings( approvals.getPe() );
        periods = periodService.reloadPeriods( periods );

        User user = currentUserService.getCurrentUser();
        DataElementCategoryOptionCombo defaultOptionCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        Date date = new Date();

        Set<DataApproval> set = new HashSet<>(); // Avoid duplicates when different data sets have the same work flow

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet.getWorkflow() == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "DataSet has no approval workflow: " + dataSet.getName() ) );
            }

            Set<DataElementCategoryOptionCombo> dataSetOptionCombos = dataSet.getCategoryCombo() != null ? dataSet.getCategoryCombo().getOptionCombos() : null;

            for ( Approval approval : approvals.getApprovals() )
            {
                OrganisationUnit unit = organisationUnitService.getOrganisationUnit( approval.getOu() );
                DataElementCategoryOptionCombo optionCombo = categoryService.getDataElementCategoryOptionCombo( approval.getAoc() );
                optionCombo = ObjectUtils.firstNonNull( optionCombo, defaultOptionCombo );

                for ( Period period : periods )
                {
                    if ( dataSetOptionCombos != null && dataSetOptionCombos.contains( optionCombo ) )
                    {
                        DataApproval dataApproval = new DataApproval( null, dataSet.getWorkflow(), period, unit, optionCombo, false, date, user );
                        set.add( dataApproval );
                    }
                }
            }
        }

        return new ArrayList<>( set );
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates the input parameters and returns a data approval workflow.
     *
     * @param ds the data set identifier.
     * @param wf the data approval workflow identifier.
     * @return a DataApprovalWorkflow.
     * @throws WebMessageException if object is not found.
     */
    private DataApprovalWorkflow getAndValidateWorkflow( String ds, String wf )
        throws WebMessageException
    {
        if ( ds != null )
        {
            DataSet dataSet = dataSetService.getDataSet( ds );

            if ( dataSet == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Data set does not exist: " + ds ) );
            }

            if ( dataSet.getWorkflow() == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Data set does not have an approval workflow: " + ds ) );
            }

            return dataSet.getWorkflow();
        }
        else if ( wf != null )
        {
            DataApprovalWorkflow workflow = dataApprovalService.getWorkflow( wf );

            if ( workflow == null )
            {
                throw new WebMessageException( WebMessageUtils.conflict( "Data approval workflow does not exist: " + wf ) );
            }

            return workflow;
        }
        else
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Either data set or data approval workflow must be specified" ) );
        }
    }

    private Period getAndValidatePeriod( String pe )
        throws WebMessageException
    {
        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Illegal period identifier: " + pe ) );
        }

        return period;
    }

    private OrganisationUnit getAndValidateOrgUnit( String ou )
        throws WebMessageException
    {
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Organisation unit does not exist: " + ou ) );
        }

        return organisationUnit;
    }

    private DataApprovalLevel getAndValidateApprovalLevel( OrganisationUnit unit )
        throws WebMessageException
    {
        DataApprovalLevel dataApprovalLevel = dataApprovalLevelService.getHighestDataApprovalLevel( unit );

        if ( dataApprovalLevel == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Approval level not found for org unit: " + unit.getUid() ) );
        }

        return dataApprovalLevel;
    }
}
