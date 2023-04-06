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
package org.hisp.dhis.webapi.controller.datavalue;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.webapi.utils.ContextUtils.setNoStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.responses.FileResourceWebMessageResponse;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
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
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.openapi.SchemaGenerators.UID;
import org.hisp.dhis.webapi.utils.FileResourceUtils;
import org.hisp.dhis.webapi.utils.HeaderUtils;
import org.hisp.dhis.webapi.webdomain.DataValueFollowUpRequest;
import org.hisp.dhis.webapi.webdomain.DataValuesFollowUpRequest;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueCategoryDto;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueDto;
import org.hisp.dhis.webapi.webdomain.datavalue.DataValueQueryParams;
import org.jclouds.rest.AuthorizationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Tags( "data" )
@RestController
@RequestMapping( value = DataValueController.RESOURCE_PATH )
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
@RequiredArgsConstructor
public class DataValueController
{
    public static final String RESOURCE_PATH = "/dataValues";

    public static final String FILE_PATH = "/file";

    // ---------------------------------------------------------------------
    // Dependencies
    // ---------------------------------------------------------------------

    private final DataValueService dataValueService;

    private final SystemSettingManager systemSettingManager;

    private final InputUtils inputUtils;

    private final FileResourceService fileResourceService;

    private final DataValidator dataValidator;

    private final FileResourceUtils fileResourceUtils;

    private final DhisConfigurationProvider dhisConfig;

    // ---------------------------------------------------------------------
    // POST
    // ---------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @PostMapping( params = { "de", "pe", "ou" } )
    @ResponseStatus( HttpStatus.CREATED )
    public void saveDataValue(
        @OpenApi.Param( { UID.class, DataElement.class } ) @RequestParam String de,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) String co,
        @OpenApi.Param( { UID.class, CategoryCombo.class } ) @RequestParam( required = false ) String cc,
        @OpenApi.Param( { UID.class, CategoryOption.class } ) @RequestParam( required = false ) String cp,
        @OpenApi.Param( Period.class ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String ou,
        @OpenApi.Param( { UID.class, DataSet.class } ) @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String value,
        @RequestParam( required = false ) String comment,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) boolean force,
        @CurrentUser User currentUser )
        throws WebMessageException
    {
        DataValueCategoryDto attribute = dataValidator.getDataValueCategoryDto( cc, cp );

        DataValueDto dataValue = new DataValueDto()
            .setDataElement( de )
            .setCategoryOptionCombo( co )
            .setAttribute( attribute )
            .setPeriod( pe )
            .setOrgUnit( ou )
            .setDataSet( ds )
            .setValue( value )
            .setComment( comment )
            .setFollowUp( followUp )
            .setForce( force );

        saveDataValueInternal( dataValue, currentUser );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @PostMapping( consumes = "application/json" )
    @ResponseStatus( HttpStatus.CREATED )
    public void saveDataValueWithBody( @RequestBody DataValueDto dataValue,
        @CurrentUser User currentUser )
        throws WebMessageException
    {
        saveDataValueInternal( dataValue, currentUser );
    }

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @PostMapping( FILE_PATH )
    public WebMessage saveFileDataValue(
        @OpenApi.Param( { UID.class, DataElement.class } ) @RequestParam String de,
        @OpenApi.Param( { UID.class, CategoryOptionCombo.class } ) @RequestParam( required = false ) String co,
        @OpenApi.Param( { UID.class, CategoryCombo.class } ) @RequestParam( required = false ) String cc,
        @OpenApi.Param( { UID.class, CategoryOption.class } ) @RequestParam( required = false ) String cp,
        @OpenApi.Param( { UID.class, Period.class } ) @RequestParam String pe,
        @OpenApi.Param( { UID.class, OrganisationUnit.class } ) @RequestParam String ou,
        @OpenApi.Param( { UID.class, DataSet.class } ) @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) String comment,
        @RequestParam( required = false ) Boolean followUp,
        @RequestParam( required = false ) boolean force,
        @RequestParam( required = false ) MultipartFile file,
        @CurrentUser User currentUser )
        throws WebMessageException,
        IOException
    {
        DataValueCategoryDto attribute = dataValidator.getDataValueCategoryDto( cc, cp );

        FileResource fileResource = file == null
            ? null
            : fileResourceUtils.saveFileResource( file, FileResourceDomain.DATA_VALUE );

        DataValueDto dataValue = new DataValueDto()
            .setDataElement( de )
            .setCategoryOptionCombo( co )
            .setAttribute( attribute )
            .setPeriod( pe )
            .setOrgUnit( ou )
            .setDataSet( ds )
            .setValue( fileResource == null ? null : fileResource.getUid() )
            .setComment( comment )
            .setFollowUp( followUp )
            .setForce( force );

        saveDataValueInternal( dataValue, currentUser );

        WebMessage webMessage = new WebMessage( Status.OK, HttpStatus.ACCEPTED );
        if ( fileResource != null )
        {
            webMessage.setResponse( new FileResourceWebMessageResponse( fileResource ) );
        }
        return webMessage;
    }

    private void saveDataValueInternal( DataValueDto dataValue, User currentUser )
        throws WebMessageException
    {
        String value = dataValue.getValue();

        DataValueCategoryDto attribute = dataValue.getAttribute();

        boolean strictPeriods = systemSettingManager
            .getBoolSetting( SettingKey.DATA_IMPORT_STRICT_PERIODS );

        boolean strictCategoryOptionCombos = systemSettingManager
            .getBoolSetting( SettingKey.DATA_IMPORT_STRICT_CATEGORY_OPTION_COMBOS );

        boolean strictOrgUnits = systemSettingManager
            .getBoolSetting( SettingKey.DATA_IMPORT_STRICT_ORGANISATION_UNITS );

        boolean requireCategoryOptionCombo = systemSettingManager
            .getBoolSetting( SettingKey.DATA_IMPORT_REQUIRE_CATEGORY_OPTION_COMBO );

        FileResourceRetentionStrategy retentionStrategy = systemSettingManager
            .getSystemSetting( SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.class );

        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = dataValidator.getAndValidateDataElement( dataValue.getDataElement() );

        CategoryOptionCombo categoryOptionCombo = dataValidator.getAndValidateCategoryOptionCombo(
            dataValue.getCategoryOptionCombo(), requireCategoryOptionCombo );

        CategoryOptionCombo attributeOptionCombo = dataValidator.getAndValidateAttributeOptionCombo( attribute );

        Period period = dataValidator.getAndValidatePeriod( dataValue.getPeriod() );

        OrganisationUnit organisationUnit = dataValidator
            .getAndValidateOrganisationUnit( dataValue.getOrgUnit() );

        dataValidator.validateOrganisationUnitPeriod( organisationUnit, period );

        DataSet dataSet = dataValidator.getAndValidateOptionalDataSet( dataValue.getDataSet(), dataElement );

        dataValidator.validateInvalidFuturePeriod( period, dataElement );

        dataValidator.validateAttributeOptionCombo( attributeOptionCombo, period, dataSet, dataElement );

        value = dataValidator.validateAndNormalizeDataValue( dataValue.getValue(), dataElement );

        dataValidator.validateComment( dataValue.getComment() );

        dataValidator.validateOptionSet( value, dataElement.getOptionSet(), dataElement );

        dataValidator.checkCategoryOptionComboAccess( currentUser, categoryOptionCombo );

        dataValidator.checkCategoryOptionComboAccess( currentUser, attributeOptionCombo );

        // ---------------------------------------------------------------------
        // Optional constraints
        // ---------------------------------------------------------------------

        if ( strictPeriods && !dataElement.getPeriodTypes().contains( period.getPeriodType() ) )
        {
            throw new WebMessageException( conflict(
                "Period type of period: " + period.getIsoDate() + " not valid for data element: "
                    + dataElement.getUid() ) );
        }

        if ( strictCategoryOptionCombos && !dataElement.getCategoryOptionCombos().contains( categoryOptionCombo ) )
        {
            throw new WebMessageException( conflict(
                "Category option combo: " + categoryOptionCombo.getUid()
                    + " must be part of category combo of data element: " + dataElement.getUid() ) );
        }

        if ( strictOrgUnits && !organisationUnit.hasDataElement( dataElement ) )
        {
            throw new WebMessageException( conflict(
                "Data element: " + dataElement.getUid() + " must be assigned through data sets to organisation unit: "
                    + organisationUnit.getUid() ) );
        }

        // ---------------------------------------------------------------------
        // Locking validation
        // ---------------------------------------------------------------------

        if ( !inputUtils.canForceDataInput( currentUser, dataValue.isForce() ) )
        {
            dataValidator.validateDataSetNotLocked(
                dataElement, period, dataSet, organisationUnit, attributeOptionCombo, currentUser );
        }

        // ---------------------------------------------------------------------
        // Period validation
        // ---------------------------------------------------------------------

        dataValidator.validateDataInputPeriodForDataElementAndPeriod( dataElement, dataSet, period );

        // ---------------------------------------------------------------------
        // Assemble and save data value
        // ---------------------------------------------------------------------

        String storedBy = currentUser.getUsername();

        Date now = new Date();

        DataValue persistedDataValue = dataValueService.getDataValue(
            dataElement, period, organisationUnit, categoryOptionCombo, attributeOptionCombo );

        FileResource fileResource = null;

        if ( persistedDataValue == null )
        {
            // ---------------------------------------------------------------------
            // Deal with file resource
            // ---------------------------------------------------------------------

            if ( dataElement.getValueType().isFile() && value != null )
            {
                String fileResourceOwner = getFileResourceOwner( dataElement.getUid(), categoryOptionCombo.getUid(),
                    attributeOptionCombo.getUid(), period.getUid(), organisationUnit.getUid() );

                fileResource = dataValidator.validateAndSetAssigned( value,
                    dataElement.getValueType(),
                    dataElement.getValueTypeOptions(), fileResourceOwner );
            }

            DataValue newValue = new DataValue( dataElement, period, organisationUnit, categoryOptionCombo,
                attributeOptionCombo, StringUtils.trimToNull( value ), storedBy, now,
                StringUtils.trimToNull( dataValue.getComment() ) );
            newValue.setFollowup( dataValue.getFollowUp() );

            dataValueService.addDataValue( newValue );
        }
        else
        {
            if ( value == null && dataValue.getComment() == null && dataValue.getFollowUp() == null
                && ValueType.TRUE_ONLY.equals( dataElement.getValueType() ) )
            {
                dataValueService.deleteDataValue( persistedDataValue );
                return;
            }

            // ---------------------------------------------------------------------
            // Deal with file resource
            // ---------------------------------------------------------------------

            if ( dataElement.isFileType() )
            {
                if ( value != null )
                {
                    String fileResourceOwner = getFileResourceOwner( dataElement.getUid(), categoryOptionCombo.getUid(),
                        attributeOptionCombo.getUid(), period.getUid(), organisationUnit.getUid() );

                    fileResource = dataValidator.validateAndSetAssigned( value,
                        dataElement.getValueType(),
                        dataElement.getValueTypeOptions(), fileResourceOwner );
                }
                else if ( retentionStrategy == FileResourceRetentionStrategy.NONE )
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
            }

            // -----------------------------------------------------------------
            // Value and comment are sent individually, so null checks must be
            // made for each. Empty string is sent for clearing a value.
            // -----------------------------------------------------------------

            if ( value != null )
            {
                persistedDataValue.setValue( StringUtils.trimToNull( value ) );
            }

            if ( dataValue.getComment() != null )
            {
                persistedDataValue.setComment( StringUtils.trimToNull( dataValue.getComment() ) );
            }

            if ( dataValue.getFollowUp() != null )
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

    private String getFileResourceOwner( String de, String co, String ao, String pe, String ou )
    {
        List<String> fileResourceOwnerIds = new ArrayList<>();
        fileResourceOwnerIds.add( de );
        fileResourceOwnerIds.add( co );
        fileResourceOwnerIds.add( ao );
        fileResourceOwnerIds.add( pe );
        fileResourceOwnerIds.add( ou );

        return String.join( TextUtils.SEP, fileResourceOwnerIds );
    }

    // ---------------------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------------------

    @PreAuthorize( "hasRole('ALL') or hasRole('F_DATAVALUE_ADD')" )
    @DeleteMapping
    @ResponseStatus( HttpStatus.NO_CONTENT )
    public void deleteDataValue(
        DataValueQueryParams params,
        @OpenApi.Param( { UID.class, DataSet.class } ) @RequestParam( required = false ) String ds,
        @RequestParam( required = false ) boolean force,
        @CurrentUser User currentUser,
        HttpServletResponse response )
        throws WebMessageException
    {
        FileResourceRetentionStrategy retentionStrategy = systemSettingManager
            .getSystemSetting( SettingKey.FILE_RESOURCE_RETENTION_STRATEGY, FileResourceRetentionStrategy.class );

        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = dataValidator.getAndValidateDataElement( params.getDe() );

        CategoryOptionCombo categoryOptionCombo = dataValidator.getAndValidateCategoryOptionCombo( params.getCo(),
            false );

        CategoryOptionCombo attributeOptionCombo = dataValidator.getAndValidateAttributeOptionCombo(
            params.getCc(), params.getCp() );

        Period period = dataValidator.getAndValidatePeriod( params.getPe() );

        OrganisationUnit organisationUnit = dataValidator.getAndValidateOrganisationUnit( params.getOu() );

        DataSet dataSet = dataValidator.getAndValidateOptionalDataSet( ds, dataElement );

        // ---------------------------------------------------------------------
        // Locking validation
        // ---------------------------------------------------------------------

        if ( !inputUtils.canForceDataInput( currentUser, force ) )
        {
            dataValidator.validateDataSetNotLocked(
                dataElement, period, dataSet, organisationUnit, attributeOptionCombo, currentUser );
        }

        // ---------------------------------------------------------------------
        // Period validation
        // ---------------------------------------------------------------------

        dataValidator.validateDataInputPeriodForDataElementAndPeriod( dataElement, dataSet, period );

        // ---------------------------------------------------------------------
        // Delete data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException(
                conflict( "Data value cannot be deleted because it does not exist" ) );
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
    public List<String> getDataValue(
        DataValueQueryParams params,
        @CurrentUser User currentUser,
        HttpServletResponse response )
        throws WebMessageException
    {
        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = dataValidator.getAndValidateDataElement( params.getDe() );

        CategoryOptionCombo categoryOptionCombo = dataValidator.getAndValidateCategoryOptionCombo(
            params.getCo(), false );

        CategoryOptionCombo attributeOptionCombo = dataValidator.getAndValidateAttributeOptionCombo(
            params.getCc(), params.getCp() );

        Period period = dataValidator.getAndValidatePeriod( params.getPe() );

        OrganisationUnit organisationUnit = dataValidator.getAndValidateOrganisationUnit( params.getOu() );

        // ---------------------------------------------------------------------
        // Get data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException( conflict( "Data value does not exist" ) );
        }

        // ---------------------------------------------------------------------
        // Data Sharing check
        // ---------------------------------------------------------------------

        dataValidator.checkDataValueSharing( currentUser, dataValue );

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

        DataValue dataValue = dataValidator.getAndValidateDataValueFollowUp( request );
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

        for ( DataValueFollowUpRequest value : values )
        {
            DataValue dataValue = dataValidator.getAndValidateDataValueFollowUp( value );
            dataValue.setFollowup( value.getFollowup() );
        }

        dataValueService.updateDataValues( dataValues );
    }

    // ---------------------------------------------------------------------
    // GET file
    // ---------------------------------------------------------------------

    @OpenApi.Response( byte[].class )
    @GetMapping( "/files" )
    public void getDataValueFile(
        DataValueQueryParams params,
        @RequestParam( defaultValue = "original" ) String dimension,
        HttpServletResponse response, HttpServletRequest request )
        throws WebMessageException
    {
        // ---------------------------------------------------------------------
        // Input validation
        // ---------------------------------------------------------------------

        DataElement dataElement = dataValidator.getAndValidateDataElement( params.getDe() );

        if ( !dataElement.isFileType() )
        {
            throw new WebMessageException( conflict( "DataElement must be of type file" ) );
        }

        CategoryOptionCombo categoryOptionCombo = dataValidator.getAndValidateCategoryOptionCombo(
            params.getCo(), false );

        CategoryOptionCombo attributeOptionCombo = dataValidator.getAndValidateAttributeOptionCombo(
            params.getCc(), params.getCp() );

        Period period = dataValidator.getAndValidatePeriod( params.getPe() );

        OrganisationUnit organisationUnit = dataValidator.getAndValidateOrganisationUnit( params.getOu() );

        dataValidator.validateOrganisationUnitPeriod( organisationUnit, period );

        // ---------------------------------------------------------------------
        // Get data value
        // ---------------------------------------------------------------------

        DataValue dataValue = dataValueService.getDataValue( dataElement, period, organisationUnit, categoryOptionCombo,
            attributeOptionCombo );

        if ( dataValue == null )
        {
            throw new WebMessageException( conflict( "Data value does not exist" ) );
        }

        // ---------------------------------------------------------------------
        // Get file resource
        // ---------------------------------------------------------------------

        String uid = dataValue.getValue();

        FileResource fileResource = fileResourceService.getFileResource( uid );

        if ( fileResource == null || fileResource.getDomain() != FileResourceDomain.DATA_VALUE )
        {
            throw new WebMessageException(
                notFound( "A data value file resource with id " + uid + " does not exist." ) );
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
            throw new WebMessageException( conflict(
                "The content is being processed and is not available yet. Try again later.",
                "The content requested is in transit to the file store and will be available at a later time." )
                    .setResponse( new FileResourceWebMessageResponse( fileResource ) ) );
        }

        response.setContentType( fileResource.getContentType() );
        response.setHeader( HttpHeaders.CONTENT_DISPOSITION, "filename=" + fileResource.getName() );
        response.setHeader( HttpHeaders.CONTENT_LENGTH,
            String.valueOf( fileResourceService.getFileResourceContentLength( fileResource ) ) );

        HeaderUtils.setSecurityHeaders( response, dhisConfig.getProperty( ConfigurationKey.CSP_HEADER_VALUE ) );
        setNoStore( response );

        try
        {
            fileResourceService.copyFileResourceContent( fileResource, response.getOutputStream() );
        }
        catch ( IOException e )
        {
            throw new WebMessageException( error( "Failed fetching the file from storage",
                "There was an exception when trying to fetch the file from the storage backend, could be network or filesystem related" ) );
        }

    }
}
