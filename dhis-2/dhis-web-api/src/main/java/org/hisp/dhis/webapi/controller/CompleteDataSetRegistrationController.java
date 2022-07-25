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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.importSummary;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.jobConfigurationReport;
import static org.hisp.dhis.scheduling.JobType.COMPLETE_DATA_SET_REGISTRATION_IMPORT;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_JSON;
import static org.hisp.dhis.webapi.utils.ContextUtils.CONTENT_TYPE_XML;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.CompleteDataSetRegistration;
import org.hisp.dhis.dataset.CompleteDataSetRegistrationService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.dataset.DefaultCompleteDataSetRegistrationExchangeService;
import org.hisp.dhis.dxf2.dataset.ExportParams;
import org.hisp.dhis.dxf2.dataset.tasks.ImportCompleteDataSetRegistrationsTask;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 * @author Halvdan Hoem Grelland <halvdan@dhis2.org>
 */
@Controller
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequestMapping( value = CompleteDataSetRegistrationController.RESOURCE_PATH )
public class CompleteDataSetRegistrationController
{
    public static final String RESOURCE_PATH = "/completeDataSetRegistrations";

    @Autowired
    private CompleteDataSetRegistrationService registrationService;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private InputUtils inputUtils;

    @Autowired
    private DefaultCompleteDataSetRegistrationExchangeService registrationExchangeService;

    @Autowired
    private AsyncTaskExecutor taskExecutor;

    @Autowired
    private SessionFactory sessionFactory;

    // -------------------------------------------------------------------------
    // GET
    // -------------------------------------------------------------------------

    @GetMapping( produces = CONTENT_TYPE_XML )
    public void getCompleteRegistrationsXml(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false, name = "children" ) boolean includeChildren,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Date created,
        @RequestParam( required = false ) String createdDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_XML );

        ExportParams params = registrationExchangeService.paramsFromUrl(
            dataSet, orgUnit, orgUnitGroup, period, startDate, endDate, includeChildren, created, createdDuration,
            limit, idSchemes );

        registrationExchangeService.writeCompleteDataSetRegistrationsXml( params, response.getOutputStream() );
    }

    @GetMapping( produces = CONTENT_TYPE_JSON )
    public void getCompleteRegistrationsJson(
        @RequestParam Set<String> dataSet,
        @RequestParam( required = false ) Set<String> period,
        @RequestParam( required = false ) Date startDate,
        @RequestParam( required = false ) Date endDate,
        @RequestParam( required = false, name = "children" ) boolean includeChildren,
        @RequestParam( required = false ) Set<String> orgUnit,
        @RequestParam( required = false ) Set<String> orgUnitGroup,
        @RequestParam( required = false ) Date created,
        @RequestParam( required = false ) String createdDuration,
        @RequestParam( required = false ) Integer limit,
        IdSchemes idSchemes,
        HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );

        ExportParams params = registrationExchangeService.paramsFromUrl(
            dataSet, orgUnit, orgUnitGroup, period, startDate, endDate, includeChildren, created, createdDuration,
            limit, idSchemes );

        registrationExchangeService.writeCompleteDataSetRegistrationsJson( params, response.getOutputStream() );
    }

    // -------------------------------------------------------------------------
    // POST
    // -------------------------------------------------------------------------

    @PostMapping( consumes = CONTENT_TYPE_XML, produces = CONTENT_TYPE_XML )
    @ResponseBody
    public WebMessage postCompleteRegistrationsXml(
        ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            return asyncImport( importOptions, ImportCompleteDataSetRegistrationsTask.FORMAT_XML, request );
        }
        ImportSummary summary = registrationExchangeService
            .saveCompleteDataSetRegistrationsXml( request.getInputStream(), importOptions );
        summary.setImportOptions( importOptions );
        return importSummary( summary ).withPlainResponseBefore( DhisApiVersion.V38 );
    }

    @PostMapping( consumes = CONTENT_TYPE_JSON, produces = CONTENT_TYPE_JSON )
    @ResponseBody
    public WebMessage postCompleteRegistrationsJson(
        ImportOptions importOptions, HttpServletRequest request )
        throws IOException
    {
        if ( importOptions.isAsync() )
        {
            return asyncImport( importOptions, ImportCompleteDataSetRegistrationsTask.FORMAT_JSON, request );
        }
        ImportSummary summary = registrationExchangeService
            .saveCompleteDataSetRegistrationsJson( request.getInputStream(), importOptions );
        summary.setImportOptions( importOptions );
        return importSummary( summary ).withPlainResponseBefore( DhisApiVersion.V38 );
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @DeleteMapping
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteCompleteDataSetRegistration(
        @RequestParam Set<String> ds,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam( required = false ) boolean multiOu, HttpServletResponse response )
        throws WebMessageException
    {
        Set<DataSet> dataSets = new HashSet<>( manager.getByUid( DataSet.class, ds ) );

        if ( dataSets.size() != ds.size() )
        {
            throw new WebMessageException(
                conflict( "Illegal data set identifier in this list: " + ds ) );
        }

        Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( conflict( "Illegal period identifier: " + pe ) );
        }

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( ou );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( conflict( "Illegal organisation unit identifier: " + ou ) );
        }

        CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( cc, cp, false );

        if ( attributeOptionCombo == null )
        {
            return;
        }

        // ---------------------------------------------------------------------
        // Check locked status
        // ---------------------------------------------------------------------

        User user = currentUserService.getCurrentUser();

        List<String> lockedDataSets = new ArrayList<>();

        for ( DataSet dataSet : dataSets )
        {
            if ( !dataSetService.getLockStatus( user, dataSet, period, organisationUnit, attributeOptionCombo, null,
                multiOu ).isOpen() )
            {
                lockedDataSets.add( dataSet.getUid() );
            }
        }

        if ( lockedDataSets.size() != 0 )
        {
            throw new WebMessageException(
                conflict( "Locked Data set(s) : " + StringUtils.join( lockedDataSets, ", " ) ) );
        }

        // ---------------------------------------------------------------------
        // Un-register as completed data set
        // ---------------------------------------------------------------------

        Set<OrganisationUnit> orgUnits = new HashSet<>();
        orgUnits.add( organisationUnit );

        if ( multiOu )
        {
            orgUnits.addAll( organisationUnit.getChildren() );
        }

        unRegisterCompleteDataSet( dataSets, period, orgUnits, attributeOptionCombo );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private WebMessage asyncImport( ImportOptions importOptions, String format, HttpServletRequest request )
        throws IOException
    {
        Pair<InputStream, Path> tmpFile = saveTmpFile( request.getInputStream() );

        JobConfiguration jobId = new JobConfiguration( "inMemoryCompleteDataSetRegistrationImport",
            COMPLETE_DATA_SET_REGISTRATION_IMPORT, currentUserService.getCurrentUser().getUid(), true );

        taskExecutor.executeTask(
            new ImportCompleteDataSetRegistrationsTask(
                registrationExchangeService, sessionFactory, tmpFile.getLeft(), tmpFile.getRight(), importOptions,
                format,
                jobId ) );

        return jobConfigurationReport( jobId )
            .setLocation( "/system/tasks/" + COMPLETE_DATA_SET_REGISTRATION_IMPORT );
    }

    private Pair<InputStream, Path> saveTmpFile( InputStream in )
        throws IOException
    {
        String filename = CodeGenerator.generateCode( 6 );

        File tmpFile = File.createTempFile( filename, null );
        tmpFile.deleteOnExit();

        try ( FileOutputStream out = new FileOutputStream( tmpFile ) )
        {
            IOUtils.copy( in, out );
        }

        return Pair.of( new BufferedInputStream( new FileInputStream( tmpFile ) ), tmpFile.toPath() );
    }

    private void unRegisterCompleteDataSet( Set<DataSet> dataSets, Period period,
        Set<OrganisationUnit> orgUnits, CategoryOptionCombo attributeOptionCombo )
    {
        List<CompleteDataSetRegistration> registrations = new ArrayList<>();

        for ( OrganisationUnit unit : orgUnits )
        {
            for ( DataSet dataSet : dataSets )
            {
                if ( unit.getDataSets().contains( dataSet ) )
                {
                    CompleteDataSetRegistration registration = registrationService
                        .getCompleteDataSetRegistration( dataSet, period, unit, attributeOptionCombo );

                    if ( registration != null )
                    {
                        registrations.add( registration );
                    }
                }
            }
        }
        if ( !registrations.isEmpty() )
        {
            registrationService.deleteCompleteDataSetRegistrations( registrations );
        }
    }
}
