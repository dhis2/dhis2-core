/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.webapi.controller.datavalue;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceDomain;
import org.hisp.dhis.fileresource.FileResourceRetentionStrategy;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.fileresource.FileResourceStorageStatus;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.webdomain.DataValueFollowUpRequest;
import org.hisp.dhis.webapi.webdomain.DataValuesFollowUpRequest;
import org.jclouds.rest.AuthorizationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = DataValueController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class DataValueController
{
    public static final String RESOURCE_PATH = "/dataValues";

    public static final String FILE_PATH = "/file";

    // ---------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------

    private final CurrentUserService currentUserService;

    private final DataValueService dataValueService;

    private final SystemSettingManager systemSettingManager;

    private final InputUtils inputUtils;

    private final FileResourceService fileResourceService;

    private final DataValidator dataValueValidation;

    private final FileResourceUtils fileResourceUtils;

    public DataValueController( final CurrentUserService currentUserService, final DataValueService dataValueService,
        final SystemSettingManager systemSettingManager, final InputUtils inputUtils,
        final FileResourceService fileResourceService, final DataValidator dataValueValidation,
        final FileResourceUtils fileResourceUtils )
    {
        checkNotNull( currentUserService );
        checkNotNull( dataValueService );
        checkNotNull( systemSettingManager );
        checkNotNull( inputUtils );
        checkNotNull( fileResourceService );
        checkNotNull( dataValueValidation );
        checkNotNull( fileResourceUtils );

        this.currentUserService = currentUserService;
        this.dataValueService = dataValueService;
        this.systemSettingManager = systemSettingManager;
        this.inputUtils = inputUtils;
        this.fileResourceService = fileResourceService;
        this.dataValueValidation = dataValueValidation;
        this.fileResourceUtils = fileResourceUtils;
    }

    // ---------------------------------------------------------------------
    // POST
    // ---------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @PostMapping
    @ResponseStatus( HttpStatus.CREATED )
    public void saveDataValue(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) String comment,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) boolean force, HttpServletResponse response )
        throws WebMessageException
    {
        saveDataValueInternal( de, co, cc, cp, pe, ou, ds, value, comment, followUp, force );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @PostMapping( FILE_PATH )
    public @ResponseBody WebMessage saveFileDataValue(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String comment,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) boolean force,
        @RequestParam MultipartFile file )
        throws WebMessageException,
        IOException
    {
        FileResource fileResource = fileResourceUtils.saveFileResource( file, FileResourceDomain.DATA_VALUE );

        saveDataValueInternal( de, co, cc, cp, pe, ou, ds, fileResource.getUid(), comment, followUp, force );

        WebMessage webMessage = new WebMessage( Status.OK, HttpStatus.ACCEPTED );
        webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );

        return webMessage;
    }

    private void saveDataValueInternal( String de, String co, String cc,
        String cp, String pe, String ou, String ds, String value,
        String comment, Boolean followUp, boolean force )
        throws WebMessageException
    {
        boolean strictPeriods = (Boolean) systemSettingManager
            .getSystemSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS );

        boolean strictCategoryOptionCombos = (Boolean) systemSettingManager
            .getSystemSetting( SettingKey.DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS );

        boolean strictOrgUnits = (Boolean) systemSettingManager
            .getSystemSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );

        boolean requireCategoryOptionCombo = (Boolean) systemSettingManager
            .getSystemSetting( SettingKey.DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO );

        FileResourceRetentionStrategy retentionStrategy = (FileResourceRetentionStrategy) systemSettingManager
            .getSystemSetting( SettingKey.FILE_RESOURCE_RETENTION_STRATEGY );

        User currentUser = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = dataValueValidation.getAndValidateDataElement( de );

        CategoryOptionCombo categoryOptionCombo = dataValueValidation.getAndValidateCategoryOptionCombo( co,
            requireCategoryOptionCombo );

        CategoryOptionCombo attributeOptionCombo = dataValueValidation.getAndValidateAttributeOptionCombo( cc, cp );

        Period period = dataValueValidation.getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = dataValueValidation.getAndValidateOrganisationUnit( ou );

        dataValueValidation.validateOrganisationUnitPeriod( organisationUnit, period );

        DataSet dataSet = dataValueValidation.getAndValidateOptionalDataSet( ds, dataElement );

        dataValueValidation.validateInvalidFuturePeriod( period, dataElement );

        dataValueValidation.validateAttributeOptionCombo( attributeOptionCombo, period, dataSet, dataElement );

        value = dataValueValidation.validateAndNormalizeDataValue( value, dataElement );

        dataValueValidation.validateComment( comment );

        dataValueValidation.validateOptionSet( value, dataElement.getOptionSet(), dataElement );

        dataValueValidation.checkCategoryOptionComboAccess( currentUser, categoryOptionCombo );

        dataValueValidation.checkCategoryOptionComboAccess( currentUser, attributeOptionCombo );

        // ---------------------------------------------------------------------
        // Optional constraints
        // ---------------------------------------------------------------------

        if ( strictPeriods && !dataElement.getPeriodTypes().contains( period.getPeriodType() ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "Period type of period: " + period.getIsoDate() + " not valid for data element: "
                    + dataElement.getUid() ) );
        }

        if ( strictCategoryOptionCombos && !dataElement.getCategoryOptionCombos().contains( categoryOptionCombo ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "Category option combo: " + categoryOptionCombo.getUid()
                    + " must be part of category combo of data element: " + dataElement.getUid() ) );
        }

        if ( strictOrgUnits && !organisationUnit.hasDataElement( dataElement ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict(
                "Data element: " + dataElement.getUid() + " must be assigned through data sets to organisation unit: "
                    + organisationUnit.getUid() ) );
        }

        // ---------------------------------------------------------------------
        // Locking validation
        // ---------------------------------------------------------------------

        if ( !inputUtils.canForceDataInput( currentUser, force ) )
        {
            dataValueValidation.validateDataSetNotLocked( currentUser, dataElement, period, dataSet, organisationUnit,
                attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Period validation
        // ---------------------------------------------------------------------

        dataValueValidation.validateDataInputPeriodForDataElementAndPeriod( dataElement, dataSet, period );

        // ---------------------------------------------------------------------
        // Assemble and save data value
        // ---------------------------------------------------------------------

        String storedBy = currentUserService.getCurrentUsername();

        Date now = new Date();

        DataValue persistedDataValue = dataValueService.getDataValue( dataElement, period, organisationUnit,
            categoryOptionCombo, attributeOptionCombo );

        FileResource fileResource = null;

        if ( persistedDataValue == null )
        {
            // ---------------------------------------------------------------------
            // Deal with file resource
            // ---------------------------------------------------------------------

            if ( dataElement.getValueType().isFile() )
            {
                fileResource = dataValueValidation.validateAndSetAssigned( value, dataElement.getValueType(),
                    dataElement.getValueTypeOptions() );
            }

            DataValue newValue = new DataValue( dataElement, period, organisationUnit, categoryOptionCombo,
                attributeOptionCombo,
                StringUtils.trimToNull( value ), storedBy, now, StringUtils.trimToNull( comment ) );
            newValue.setFollowup( followUp );

            dataValueService.addDataValue( newValue );
        }
        else
        {
            if ( value == null && comment == null && followUp == null
                && ValueType.TRUE_ONLY.equals( dataElement.getValueType() ) )
            {
                dataValueService.deleteDataValue( persistedDataValue );
                return;
            }

            // ---------------------------------------------------------------------
            // Deal with file resource
            // ---------------------------------------------------------------------

            if ( dataElement.getValueType().isFile() )
            {
                fileResource = dataValueValidation.validateAndSetAssigned( value, dataElement.getValueType(),
                    dataElement.getValueTypeOptions() );
            }

            if ( dataElement.isFileType() && retentionStrategy == FileResourceRetentionStrategy.NONE )
            {
                try
                {
                    fileResourceService.deleteFileResource( persistedDataValue.getValue() );
                }
                catch ( AuthorizationException exception )
                {
                    // If we fail to delete the fileResource now, mark it as
                    // unassigned for removal later
                    fileResourceService.getFileResource( persistedDataValue.getValue() ).setAssigned( false );
                }

                persistedDataValue.setValue( StringUtils.EMPTY );
            }

            // -----------------------------------------------------------------
            // Value and comment are sent individually, so null checks must be
            // made for each. Empty string is sent for clearing a value.
            // -----------------------------------------------------------------

            if ( value != null )
            {
                persistedDataValue.setValue( StringUtils.trimToNull( value ) );
            }

            if ( comment != null )
            {
                persistedDataValue.setComment( StringUtils.trimToNull( comment ) );
            }

            if ( followUp != null )
            {
                persistedDataValue.toggleFollowUp();
            }

            persistedDataValue.setLastUpdated( now );
            persistedDataValue.setStoredBy( storedBy );

            dataValueService.updateDataValue( persistedDataValue );
        }

        if ( fileResource != null )
        {
            fileResourceService.updateFileResource( fileResource );
        }
    }

    // ---------------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_DELETE')" )
    @DeleteMapping
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteDataValue(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) boolean force, HttpServletResponse response )
        throws WebMessageException
    {
        FileResourceRetentionStrategy retentionStrategy = (FileResourceRetentionStrategy) systemSettingManager
            .getSystemSetting( SettingKey.FILE_RESOURCE_RETENTION_STRATEGY );

        User currentUser = currentUserService.getCurrentUser();

        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = dataValueValidation.getAndValidateDataElement( de );

        CategoryOptionCombo categoryOptionCombo = dataValueValidation.getAndValidateCategoryOptionCombo( co, false );

        CategoryOptionCombo attributeOptionCombo = dataValueValidation.getAndValidateAttributeOptionCombo( cc, cp );

        Period period = dataValueValidation.getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = dataValueValidation.getAndValidateOrganisationUnit( ou );

        DataSet dataSet = dataValueValidation.getAndValidateOptionalDataSet( ds, dataElement );

        // ---------------------------------------------------------------------
        // Locking validation
        // ---------------------------------------------------------------------

        if ( !inputUtils.canForceDataInput( currentUser, force ) )
        {
            dataValueValidation.validateDataSetNotLocked( currentUser, dataElement, period, dataSet, organisationUnit,
                attributeOptionCombo );
        }

        // ---------------------------------------------------------------------
        // Period validation
        // ---------------------------------------------------------------------

        dataValueValidation.validateDataInputPeriodForDataElementAndPeriod( dataElement, dataSet, period );

        // ---------------------------------------------------------------------
        // Delete data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException(
                WebMessageUtils.conflict( "Data value cannot be deleted because it does not exist" ) );
        }

        if ( dataValue.getDataElement().isFileType() && retentionStrategy == FileResourceRetentionStrategy.NONE )
        {
            fileResourceService.deleteFileResource( dataValue.getValue() );
        }

        dataValueService.deleteDataValue( dataValue );
    }

    // ---------------------------------------------------------------------
    // GET
    // ---------------------------------------------------------------------

    @GetMapping
    public @ResponseBody List<String> getDataValue(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        Model model, HttpServletResponse response )
        throws WebMessageException
    {
        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        User currentUser = currentUserService.getCurrentUser();

        DataElement dataElement = dataValueValidation.getAndValidateDataElement( de );

        CategoryOptionCombo categoryOptionCombo = dataValueValidation.getAndValidateCategoryOptionCombo( co, false );

        CategoryOptionCombo attributeOptionCombo = dataValueValidation.getAndValidateAttributeOptionCombo( cc, cp );

        Period period = dataValueValidation.getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = dataValueValidation.getAndValidateOrganisationUnit( ou );

        // ---------------------------------------------------------------------
        // Get data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data value does not exist" ) );
        }

        // ---------------------------------------------------------------------
        // Data Sharing check
        // ---------------------------------------------------------------------

        dataValueValidation.checkDataValueSharing( currentUser, dataValue );

        List<String> value = new ArrayList<>();
        value.add( dataValue.getValue() );

        setNoStore( response );
        return value;
    }

    // ---------------------------------------------------------------------
    // Follow-up
    // ---------------------------------------------------------------------

    @PutMapping( value = "/followup" )
    @ResponseStatus( value = HttpStatus.OK )
    public void setDataValueFollowUp( @RequestBody DataValueFollowUpRequest request )
    {
        if ( request == null || request.getFollowup() == null )
        {
            throw new IllegalQueryException( ErrorCode.E2033 );
        }

        DataValue dataValue = dataValueValidation.getAndValidateDataValue( request );
        dataValue.setFollowup( request.getFollowup() );
        dataValueService.updateDataValue( dataValue );
    }

    @PutMapping( value = "/followups" )
    @ResponseStatus( value = HttpStatus.OK )
    public void setDataValuesFollowUp( @RequestBody DataValuesFollowUpRequest request )
    {
        List<DataValueFollowUpRequest> values = request == null ? null : request.getValues();
        if ( values == null || values.isEmpty() || values.stream().anyMatch( e -> e.getFollowup() == null ) )
        {
            throw new IllegalQueryException( ErrorCode.E2033 );
        }

        List<DataValue> dataValues = new ArrayList<>();
        for ( DataValueFollowUpRequest e : values )
        {
            DataValue dataValue = dataValueValidation.getAndValidateDataValue( e );
            dataValue.setFollowup( e.getFollowup() );
        }
        dataValueService.updateDataValues( dataValues );
    }

    // ---------------------------------------------------------------------
    // GET file
    // ---------------------------------------------------------------------

    @GetMapping( "/files" )
    public void getDataValueFile(
        @RequestParam String de,
        @RequestParam( required = false ) String co,
        @RequestParam( required = false ) String cc,
        @RequestParam( required = false ) String cp,
        @RequestParam String pe,
        @RequestParam String ou,
        @RequestParam( defaultValue = "original" ) String dimension,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = dataValueValidation.getAndValidateDataElement( de );

        if ( !dataElement.isFileType() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "DataElement must be of type file" ) );
        }

        CategoryOptionCombo categoryOptionCombo = dataValueValidation.getAndValidateCategoryOptionCombo( co, false );

        CategoryOptionCombo attributeOptionCombo = dataValueValidation.getAndValidateAttributeOptionCombo( cc, cp );

        Period period = dataValueValidation.getAndValidatePeriod( pe );

        OrganisationUnit organisationUnit = dataValueValidation.getAndValidateOrganisationUnit( ou );

        dataValueValidation.validateOrganisationUnitPeriod( organisationUnit, period );

        // ---------------------------------------------------------------------
        // Get data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Data value does not exist" ) );
        }

        // ---------------------------------------------------------------------
        // Get file resource
        // ---------------------------------------------------------------------

        String uid = dataValue.getValue();

        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
        {
            throw new WebMessageException(
                WebMessageUtils.notFound( "A data value file resource with id " + uid + " does not exist." ) );
        }

        FileResourceStorageStatus storageStatus = fileResource.getStorageStatus();

        if ( storageStatus != FileResourceStorageStatus.STORED )
        {
            // Special case:
            // The FileResource exists and has been tied to this DataValue,
            // however, the underlying file
            // content is still not stored to the (most likely external) file
            // store provider.

            // HTTP 409, for lack of a more suitable status code
            WebMessage webMessage = WebMessageUtils.conflict(
                "The content is being processed and is not available yet. Try again later.",
                "The content requested is in transit to the file store and will be available at a later time." );
            webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );

            throw new WebMessageException( webMessage );
        }

        response.setContentType( fileResource.getContentType() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );
        response.setHeader( HttpHeaders.CONTENT_LENGTH,
            String.valueOf( fileResourceService.getFileResourceContentLength( fileResource ) ) );
        setNoStore( response );
        try
        {
            fileResourceService.copyFileResourceContent( fileResource, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend, could be network or filesystem related" ) );
        }

    }
}
