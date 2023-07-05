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
package org.hisp.dhis.webapi.controller;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalLevel;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.dataapproval.DataApprovalPermissions;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalStateResponse;
import org.hisp.dhis.dataapproval.DataApprovalStatus;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.NodeUtils;
import org.hisp.dhis.node.Preset;
import org.hisp.dhis.node.types.RootNode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.ObjectUtils;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.approval.ApprovalDto;
import org.hisp.dhis.webapi.webdomain.approval.ApprovalStatusDto;
import org.hisp.dhis.webapi.webdomain.approval.ApprovalsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This controller uses both /dataApprovals and /dataAcceptances.
 *
 * @author Lars Helge Overland
 */
@OpenApi.Tags( "data" )
@Controller
@RequestMapping
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataApprovalController
{
    private static final String APPROVALS_PATH = "/dataApprovals";

    private static final String STATUS_PATH = APPROVALS_PATH + "/status";

    private static final String ACCEPTANCES_PATH = "/dataAcceptances";

    private static final String MULTIPLE_APPROVALS_PATH = APPROVALS_PATH + "/multiple";

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
    private CurrentUserService currentUserService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private FieldFilterService fieldFilterService;

    @Autowired
    private ContextService contextService;

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @GetMapping( value = APPROVALS_PATH, produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseBody
    @ResponseStatus( HttpStatus.OK )
    public DataApprovalPermissions getApprovalPermissions(
        @OpenApi.Param( { UID.class, DataSet.class } ) @RequestParam( required = false ) String ds,
        @OpenApi.Param( { UID.class, DataApprovalWorkflow.class } ) @RequestParam( required = false ) String wf,
        @OpenApi.Param( Period.class ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String ou,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) String aoc )
        throws WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        CategoryOptionCombo optionCombo = getAndValidateAttributeOptionCombo( aoc );

        DataApprovalStatus status = dataApprovalService
            .getDataApprovalStatus( workflow, period, organisationUnit, optionCombo );

        DataApprovalPermissions permissions = status.getPermissions();
        permissions.setState( status.getState().toString() );

        return status.getPermissions();
    }

    @GetMapping( value = { MULTIPLE_APPROVALS_PATH,
        APPROVALS_PATH + "/approvals" }, produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseBody
    @ResponseStatus( HttpStatus.OK )
    public List<ApprovalStatusDto> getMultipleApprovalPermissions(
        @OpenApi.Param( { UID[].class, DataApprovalWorkflow.class } ) @RequestParam Set<String> wf,
        @OpenApi.Param( Period[].class ) @RequestParam( required = false ) Set<String> pe,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @OpenApi.Param( { UID[].class, OrganisationUnit.class } ) @RequestParam Set<String> ou,
        @OpenApi.Param( { UID[].class, CategoryOptionCombo.class } ) @RequestParam( required = false ) Set<String> aoc )
        throws WebMessageException
    {
        Set<DataApprovalWorkflow> workflows = getAndValidateWorkflows( null, wf );

        List<Period> periods = null;

        if ( pe == null )
        {
            if ( startDate == null || endDate == null )
            {
                throw new WebMessageException(
                    conflict( "Must have either pe or both startDate and endDate." ) );
            }
        }
        else
        {
            if ( startDate != null || endDate != null )
            {
                throw new WebMessageException(
                    conflict( "Cannot have both pe and startDate or endDate." ) );
            }

            periods = new ArrayList<>();

            for ( String period : pe )
            {
                Period periodObj = periodService.getPeriod( getAndValidatePeriod( period ).getIsoDate() );

                if ( periodObj != null )
                {
                    periods.add( periodObj );
                }
            }
        }

        List<OrganisationUnit> orgUnits = new ArrayList<>();

        for ( String orgUnit : ou )
        {
            orgUnits.add( getAndValidateOrgUnit( orgUnit ) );
        }

        Set<CategoryOptionCombo> attributeOptionCombos = getAndValidateAttributeOptionCombos( aoc );

        List<DataApproval> dataApprovals = new ArrayList<>();

        for ( DataApprovalWorkflow workflow : workflows )
        {
            List<Period> workflowPeriods = periods != null ? periods
                : periodService.getPeriodsBetweenDates( workflow.getPeriodType(), startDate, endDate );

            for ( Period period : workflowPeriods )
            {
                if ( period.getPeriodType().equals( workflow.getPeriodType() ) )
                {
                    for ( OrganisationUnit orgUnit : orgUnits )
                    {
                        for ( CategoryOptionCombo optionCombo : attributeOptionCombos )
                        {
                            dataApprovals.add( new DataApproval( null, workflow, period, orgUnit, optionCombo ) );
                        }
                    }
                }
            }
        }

        return dataApprovalService.getDataApprovalStatuses( dataApprovals ).entrySet().stream()
            .map( ApprovalStatusDto::from )
            .collect( toList() );
    }

    @GetMapping( value = STATUS_PATH, produces = ContextUtils.CONTENT_TYPE_JSON )
    public @ResponseBody RootNode getApproval(
        @OpenApi.Param( { UID[].class, DataSet.class } ) @RequestParam Set<String> ds,
        @OpenApi.Param( Period.class ) @RequestParam( required = false ) String pe,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @OpenApi.Param( { UID[].class, OrganisationUnit.class } ) @RequestParam Set<String> ou,
        @RequestParam( required = false ) boolean children,
        HttpServletResponse response )
        throws WebMessageException,
        BadRequestException
    {
        List<String> fields = new ArrayList<>( contextService.getParameterValues( "fields" ) );

        if ( fields.isEmpty() )
        {
            fields.addAll( Preset.ALL.getFields() );
            List<String> defaults = new ArrayList<>();
            defaults.add(
                "period[id,name,code],organisationUnit[id,name,created,lastUpdated],dataSet[code,name,created,lastUpdated,id]" );
            fields.addAll( defaults );
        }

        Set<DataSet> dataSets = parseDataSetsWithWorkflow( ds );

        Set<Period> periods = parsePeriods( pe, startDate, endDate );

        Set<OrganisationUnit> organisationUnits = new HashSet<>();

        if ( children )
        {
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitsWithChildren( ou ) );
        }
        else
        {
            organisationUnits.addAll( organisationUnitService.getOrganisationUnitsByUid( ou ) );
        }

        List<DataApprovalStateResponse> responses = new ArrayList<>();

        for ( DataSet dataSet : dataSets )
        {
            for ( OrganisationUnit organisationUnit : organisationUnits )
            {
                for ( Period period : periods )
                {
                    responses.add( getDataApprovalStateResponse( dataSet, organisationUnit, period ) );
                }
            }
        }

        response.setContentType( MediaType.APPLICATION_JSON_VALUE );

        RootNode rootNode = NodeUtils.createMetadata();

        rootNode.addChild( fieldFilterService.toCollectionNode( DataApprovalStateResponse.class,
            new FieldFilterParams( responses, fields ) ) );

        return rootNode;
    }

    private Set<Period> parsePeriods( String pe, Date startDate, Date endDate )
        throws BadRequestException
    {
        if ( startDate == null || endDate == null )
        {
            Period period = periodService.getPeriod( pe );
            if ( period == null )
            {
                throw new BadRequestException( "Either provide startDate and endDate or a valid ISO period for pe" );
            }
            return singleton( period );
        }

        PeriodType periodType = periodService.getPeriodTypeByName( pe );
        if ( periodType != null )
        {
            return new HashSet<>( periodService.getPeriodsBetweenDates( periodType, startDate, endDate ) );
        }
        return new HashSet<>( periodService.getPeriodsBetweenDates( startDate, endDate ) );
    }

    private DataApprovalStateResponse getDataApprovalStateResponse( DataSet dataSet, OrganisationUnit organisationUnit,
        Period period )
    {
        CategoryOptionCombo optionCombo = categoryService.getDefaultCategoryOptionCombo();

        DataApprovalStatus status = dataApprovalService.getDataApprovalStatus( dataSet.getWorkflow(), period,
            organisationUnit, optionCombo );

        DataApprovalPermissions permissions = status.getPermissions();
        return DataApprovalStateResponse.builder()
            .dataSet( dataSet )
            .organisationUnit( organisationUnit )
            .period( period )
            .state( status.getState().toString() )
            .createdDate( status.getCreated() )
            .createdByUsername( status.getCreator() == null ? null : status.getCreator().getUsername() )
            .permissions( permissions )
            .build();
    }

    @GetMapping( value = APPROVALS_PATH
        + "/categoryOptionCombos", produces = MediaType.APPLICATION_JSON_VALUE )
    @ResponseBody
    @ResponseStatus( HttpStatus.OK )
    public List<Map<String, Object>> getApprovalByCategoryOptionCombos(
        @OpenApi.Param( { UID[].class, DataSet.class } ) @RequestParam( required = false ) Set<String> ds,
        @OpenApi.Param( { UID[].class, DataApprovalWorkflow.class } ) @RequestParam( required = false ) Set<String> wf,
        @OpenApi.Param( Period.class ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam( required = false ) String ou,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam( required = false ) String ouFilter,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) Set<String> aoc )
        throws WebMessageException
    {
        Set<DataApprovalWorkflow> workflows = getAndValidateWorkflows( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit orgUnit = organisationUnitService.getOrganisationUnit( ou );
        OrganisationUnit orgUnitFilter = organisationUnitService.getOrganisationUnit( ouFilter );
        Set<CategoryOptionCombo> attributeOptionCombos = getAndValidateAttributeOptionCombos( aoc );

        if ( orgUnit != null && orgUnit.isRoot() )
        {
            orgUnit = null; // Look for all org units.
        }

        List<DataApprovalStatus> statusList = new ArrayList<>();

        for ( DataApprovalWorkflow workflow : workflows )
        {
            Set<CategoryCombo> attributeCombos = new HashSet<>();

            for ( DataSet dataSet : workflow.getDataSets() )
            {
                attributeCombos.add( dataSet.getCategoryCombo() );
            }

            for ( CategoryCombo attributeCombo : attributeCombos )
            {
                statusList.addAll( dataApprovalService.getUserDataApprovalsAndPermissions( workflow, period, orgUnit,
                    orgUnitFilter, attributeCombo, attributeOptionCombos ) );
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
        return list;
    }

    // -------------------------------------------------------------------------
    // Post, approval
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_APPROVE_DATA') or hasRole('F_APPROVE_DATA_LOWER_LEVELS')" )
    @PostMapping( value = APPROVALS_PATH )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveApproval(
        @OpenApi.Param( { UID.class, DataSet.class } ) @RequestParam( required = false ) String ds,
        @OpenApi.Param( { UID.class, DataApprovalWorkflow.class } ) @RequestParam( required = false ) String wf,
        @OpenApi.Param( Period.class ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String ou,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) String aoc )
        throws WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );
        CategoryOptionCombo optionCombo = getAndValidateAttributeOptionCombo( aoc );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = getApprovalsAsList( dataApprovalLevel, workflow,
            period, organisationUnit, optionCombo, false, new Date(), user );

        dataApprovalService.approveData( dataApprovalList );
    }

    @PostMapping( APPROVALS_PATH + "/approvals" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveApprovalBatch( @RequestBody ApprovalsDto approvals )
        throws WebMessageException
    {
        dataApprovalService.approveData( getDataApprovalList( approvals ) );
    }

    @PostMapping( APPROVALS_PATH + "/unapprovals" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeApprovalBatch( @RequestBody ApprovalsDto approvals )
        throws WebMessageException
    {
        dataApprovalService.unapproveData( getDataApprovalList( approvals ) );
    }

    // -------------------------------------------------------------------------
    // Post, acceptance
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_ACCEPT_DATA_LOWER_LEVELS')" )
    @PostMapping( ACCEPTANCES_PATH )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void acceptApproval(
        @OpenApi.Param( { UID.class, DataSet.class } ) @RequestParam( required = false ) String ds,
        @OpenApi.Param( { UID.class, DataApprovalWorkflow.class } ) @RequestParam( required = false ) String wf,
        @OpenApi.Param( Period.class ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String ou,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) String aoc )
        throws WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );
        CategoryOptionCombo optionCombo = getAndValidateAttributeOptionCombo( aoc );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = getApprovalsAsList( dataApprovalLevel, workflow,
            period, organisationUnit, optionCombo, false, new Date(), user );

        dataApprovalService.acceptData( dataApprovalList );
    }

    @PostMapping( ACCEPTANCES_PATH + "/acceptances" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void saveAcceptanceBatch( @RequestBody ApprovalsDto approvals )
        throws WebMessageException
    {
        dataApprovalService.acceptData( getDataApprovalList( approvals ) );
    }

    @PostMapping( ACCEPTANCES_PATH + "/unacceptances" )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeAcceptancesBatch( @RequestBody ApprovalsDto approvals )
        throws WebMessageException
    {
        dataApprovalService.unacceptData( getDataApprovalList( approvals ) );
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_APPROVE_DATA') or hasRole('F_APPROVE_DATA_LOWER_LEVELS')" )
    @DeleteMapping( APPROVALS_PATH )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void removeApproval(
        @OpenApi.Param( { UID[].class, DataSet.class } ) @RequestParam( required = false ) Set<String> ds,
        @OpenApi.Param( { UID[].class, DataApprovalWorkflow.class } ) @RequestParam( required = false ) Set<String> wf,
        @OpenApi.Param( Period.class ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String ou,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) String aoc )
        throws WebMessageException
    {
        Set<DataApprovalWorkflow> workflows = getAndValidateWorkflows( ds, wf );

        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );
        CategoryOptionCombo optionCombo = getAndValidateAttributeOptionCombo( aoc );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = new ArrayList<>();

        for ( DataApprovalWorkflow workflow : workflows )
        {
            dataApprovalList.addAll( getApprovalsAsList( dataApprovalLevel, workflow,
                period, organisationUnit, optionCombo, false, new Date(), user ) );
        }

        dataApprovalService.unapproveData( dataApprovalList );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_ACCEPT_DATA_LOWER_LEVELS')" )
    @DeleteMapping( ACCEPTANCES_PATH )
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void unacceptApproval(
        @OpenApi.Param( { UID.class, DataSet.class } ) @RequestParam( required = false ) String ds,
        @OpenApi.Param( { UID.class, DataApprovalWorkflow.class } ) @RequestParam( required = false ) String wf,
        @OpenApi.Param( Period.class ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String ou,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) String aoc )
        throws WebMessageException
    {
        DataApprovalWorkflow workflow = getAndValidateWorkflow( ds, wf );
        Period period = getAndValidatePeriod( pe );
        OrganisationUnit organisationUnit = getAndValidateOrgUnit( ou );
        DataApprovalLevel dataApprovalLevel = getAndValidateApprovalLevel( organisationUnit );
        CategoryOptionCombo optionCombo = getAndValidateAttributeOptionCombo( aoc );

        User user = currentUserService.getCurrentUser();

        List<DataApproval> dataApprovalList = getApprovalsAsList( dataApprovalLevel, workflow,
            period, organisationUnit, optionCombo, false, new Date(), user );

        dataApprovalService.unacceptData( dataApprovalList );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<DataSet> parseDataSetsWithWorkflow( Set<String> ds )
        throws WebMessageException
    {
        Set<DataSet> dataSets = new HashSet<>( objectManager.getByUid( DataSet.class, ds ) );

        for ( DataSet dataSet : dataSets )
        {
            if ( dataSet.getWorkflow() == null )
            {
                throw new WebMessageException(
                    conflict( "DataSet does not have an approval workflow: " + dataSet.getUid() ) );
            }
        }

        return dataSets;
    }

    private List<DataApproval> getApprovalsAsList( DataApprovalLevel dataApprovalLevel,
        DataApprovalWorkflow workflow, Period period, OrganisationUnit organisationUnit,
        CategoryOptionCombo optionCombo, boolean accepted, Date created, User creator )
    {
        List<DataApproval> approvals = new ArrayList<>();

        period = periodService.reloadPeriod( period );

        approvals.add( new DataApproval( dataApprovalLevel, workflow, period,
            organisationUnit, optionCombo, accepted, created, creator ) );

        return approvals;
    }

    private List<DataApproval> getDataApprovalList( ApprovalsDto approvals )
        throws WebMessageException
    {
        Set<DataApprovalWorkflow> workflows = getAndValidateWorkflows( approvals.getDs(), approvals.getWf() );
        List<Period> periods = PeriodType.getPeriodsFromIsoStrings( approvals.getPe() );
        periods = periodService.reloadPeriods( periods );

        if ( periods.isEmpty() )
        {
            throw new WebMessageException( conflict( "Approvals must have periods" ) );
        }

        User user = currentUserService.getCurrentUser();
        CategoryOptionCombo defaultOptionCombo = categoryService.getDefaultCategoryOptionCombo();
        Date date = new Date();

        List<DataApproval> dataApprovals = new ArrayList<>();

        List<CategoryOptionCombo> optionCombos = categoryService.getAllCategoryOptionCombos();

        Map<String, CategoryOptionCombo> comboMap = optionCombos.stream()
            .collect( Collectors.toMap( CategoryOptionCombo::getUid, c -> c ) );

        Map<String, OrganisationUnit> ouCache = new HashMap<>();

        for ( ApprovalDto approval : approvals.getApprovals() )
        {
            CategoryOptionCombo atributeOptionCombo = comboMap.get( approval.getAoc() );
            atributeOptionCombo = ObjectUtils.firstNonNull( atributeOptionCombo, defaultOptionCombo );
            OrganisationUnit unit = ouCache.get( approval.getOu() );

            if ( unit == null )
            {
                unit = getAndValidateOrgUnit( approval.getOu() );
                ouCache.put( approval.getOu(), unit );
            }

            for ( DataApprovalWorkflow workflow : workflows )
            {
                for ( Period period : periods )
                {
                    dataApprovals.add(
                        new DataApproval( null, workflow, period, unit, atributeOptionCombo, false, date, user ) );
                }
            }
        }

        return dataApprovals;
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    /**
     * Validates the data set and workflow parameters and returns a data
     * approval workflow.
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
                throw new WebMessageException( conflict( "Data set does not exist: " + ds ) );
            }

            if ( dataSet.getWorkflow() == null )
            {
                throw new WebMessageException(
                    conflict( "Data set does not have an approval workflow: " + ds ) );
            }

            return dataSet.getWorkflow();
        }
        else if ( wf != null )
        {
            DataApprovalWorkflow workflow = dataApprovalService.getWorkflow( wf );

            if ( workflow == null )
            {
                throw new WebMessageException(
                    conflict( "Data approval workflow does not exist: " + wf ) );
            }

            return workflow;
        }
        else
        {
            throw new WebMessageException(
                conflict( "Either data set or data approval workflow must be specified" ) );
        }
    }

    /**
     * Validates the sets of data set and workflow parameters and returns a set
     * of data approval workflows.
     *
     * @param dss the data set identifiers.
     * @param wfs the data approval workflow identifiers.
     * @return a set of DataApprovalWorkflows.
     * @throws WebMessageException if workflows are not found.
     */
    private Set<DataApprovalWorkflow> getAndValidateWorkflows( Collection<String> dss, Collection<String> wfs )
        throws WebMessageException
    {
        Set<DataApprovalWorkflow> workflows = new HashSet<>();

        if ( dss != null )
        {
            for ( String ds : dss )
            {
                workflows.add( getAndValidateWorkflow( ds, null ) );
            }
        }

        if ( wfs != null )
        {
            for ( String wf : wfs )
            {
                workflows.add( getAndValidateWorkflow( null, wf ) );
            }
        }

        if ( workflows.isEmpty() )
        {
            throw new WebMessageException(
                conflict( "Either data sets or data approval workflows must be specified" ) );
        }

        return workflows;
    }

    private Period getAndValidatePeriod( String pe )
        throws WebMessageException
    {
        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( conflict( "Illegal period identifier: " + pe ) );
        }

        return period;
    }

    private OrganisationUnit getAndValidateOrgUnit( String ou )
        throws WebMessageException
    {
        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( conflict( "Organisation unit does not exist: " + ou ) );
        }

        return organisationUnit;
    }

    private DataApprovalLevel getAndValidateApprovalLevel( OrganisationUnit unit )
        throws WebMessageException
    {
        DataApprovalLevel dataApprovalLevel = dataApprovalLevelService.getHighestDataApprovalLevel( unit );

        if ( dataApprovalLevel == null )
        {
            throw new WebMessageException(
                conflict( "Approval level not found for org unit: " + unit.getUid() ) );
        }

        return dataApprovalLevel;
    }

    private Set<CategoryOptionCombo> getAndValidateAttributeOptionCombos( Set<String> aoc )
        throws WebMessageException
    {
        Set<CategoryOptionCombo> optionCombos = new HashSet<>();

        if ( aoc == null )
        {
            optionCombos.add( categoryService.getDefaultCategoryOptionCombo() );
        }
        else
        {
            for ( String optionCombo : aoc )
            {
                optionCombos.add( getAndValidateAttributeOptionCombo( optionCombo ) );
            }
        }

        return optionCombos;
    }

    private CategoryOptionCombo getAndValidateAttributeOptionCombo( String aoc )
        throws WebMessageException
    {
        if ( aoc != null )
        {
            CategoryOptionCombo optionCombo = categoryService.getCategoryOptionCombo( aoc );

            if ( optionCombo == null )
            {
                throw new WebMessageException(
                    conflict( "Attribute option combo does not exist: " + aoc ) );
            }

            return optionCombo;
        }

        return categoryService.getDefaultCategoryOptionCombo();
    }
}
