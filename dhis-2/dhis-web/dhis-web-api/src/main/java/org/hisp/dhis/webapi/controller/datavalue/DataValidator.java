package org.hisp.dhis.webapi.controller.datavalue;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.fileresource.FileResourceDomain.DATA_VALUE;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.normalizeBoolean;

import java.util.Date;
import java.util.List;

import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.dxf2.util.InputUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * This a simple component responsible for extracting and encapsulating
 * validation rules from the controller layer. This can be seen as an extension
 * of the controller.
 */
@Component
class DataValidator
{

    private final CategoryService categoryService;

    private final OrganisationUnitService organisationUnitService;

    private final DataSetService dataSetService;

    private final IdentifiableObjectManager idObjectManager;

    private final InputUtils inputUtils;

    private final FileResourceService fileResourceService;

    private final I18nManager i18nManager;

    private final CalendarService calendarService;

    private final AggregateAccessManager accessManager;

    public DataValidator( final CategoryService categoryService, final OrganisationUnitService organisationUnitService,
        final DataSetService dataSetService, final IdentifiableObjectManager idObjectManager,
        final InputUtils inputUtils, final FileResourceService fileResourceService, final I18nManager i18nManager,
        final CalendarService calendarService, final AggregateAccessManager accessManager )
    {
        checkNotNull( categoryService );
        checkNotNull( organisationUnitService );
        checkNotNull( dataSetService );
        checkNotNull( idObjectManager );
        checkNotNull( inputUtils );
        checkNotNull( fileResourceService );
        checkNotNull( i18nManager );
        checkNotNull( calendarService );
        checkNotNull( accessManager );

        this.categoryService = categoryService;
        this.organisationUnitService = organisationUnitService;
        this.dataSetService = dataSetService;
        this.idObjectManager = idObjectManager;
        this.inputUtils = inputUtils;
        this.fileResourceService = fileResourceService;
        this.i18nManager = i18nManager;
        this.calendarService = calendarService;
        this.accessManager = accessManager;
    }

    /**
     * Retrieve the respective DataElement and validates if it's accessible.
     * 
     * @param deUid the data element uid.
     * @return the DataElement object respective.
     * @throws WebMessageException if the validation fails.
     */
    DataElement getAndValidateDataElementAccess( final String deUid )
        throws WebMessageException
    {
        final DataElement dataElement = idObjectManager.get( DataElement.class, deUid );

        if ( dataElement == null )
        {
            throw new WebMessageException( conflict( "Data element not found or not accessible: " + deUid ) );
        }

        return dataElement;
    }

    /**
     * Retrieve and validate a CategoryOptionCombo based on the given coUid.
     * 
     * @param coUid the category option uid.
     * @param requireCategoryOptionCombo flag used as part of the validation.
     * @return the respective and valid CategoryOptionCombo.
     * @throws WebMessageException if the validation fails.
     */
    CategoryOptionCombo getAndValidateCategoryOptionCombo( final String coUid,
        final boolean requireCategoryOptionCombo )
        throws WebMessageException
    {
        CategoryOptionCombo categoryOptionCombo = categoryService.getCategoryOptionCombo( coUid );

        if ( categoryOptionCombo == null )
        {
            if ( requireCategoryOptionCombo )
            {
                throw new WebMessageException( conflict( "Category option combo is required but is not specified" ) );
            }
            else if ( coUid != null )
            {
                throw new WebMessageException(
                    conflict( "Category option combo not found or not accessible: " + coUid ) );
            }
            else
            {
                categoryOptionCombo = categoryService.getDefaultCategoryOptionCombo();
            }
        }

        return categoryOptionCombo;
    }

    /**
     * Retrieves and validate the respective CategoryOptionCombo (attribute option
     * combo) based on the given arguments.
     * 
     * @param ccUid the category combo identifier.
     * @param cp the category and option query string.
     * @return the valid CategoryOptionCombo (attribute option combo).
     * @throws WebMessageException if the validation fails.
     */
    CategoryOptionCombo getAndValidateAttributeOptionCombo( final String ccUid, final String cp )
        throws WebMessageException
    {
        final CategoryOptionCombo attributeOptionCombo = inputUtils.getAttributeOptionCombo( ccUid, cp, false );

        if ( attributeOptionCombo == null )
        {
            throw new WebMessageException(
                conflict( "Attribute option combo not found or not accessible: " + ccUid + " " + cp ) );
        }

        return attributeOptionCombo;
    }

    /**
     * Reads and validate the given period.
     * 
     * @param pe the period.
     * @return the validated Period.
     * @throws WebMessageException if the validation fails.
     */
    Period getAndValidatePeriod( final String pe )
        throws WebMessageException
    {
        final Period period = PeriodType.getPeriodFromIsoString( pe );

        if ( period == null )
        {
            throw new WebMessageException( conflict( "Illegal period identifier: " + pe ) );
        }

        return period;
    }

    /**
     * Validates the OrganisationUnit dates against the given period.
     * 
     * @param organisationUnit the OrganisationUnit and its dates.
     * @param period the period to be checked.
     * @throws WebMessageException if the validation fails.
     */
    void validateOrganisationUnitPeriod( final OrganisationUnit organisationUnit, final Period period )
        throws WebMessageException
    {
        final Date openingDate = organisationUnit.getOpeningDate();
        final Date closedDate = organisationUnit.getClosedDate();
        final Date startDate = period.getStartDate();
        final Date endDate = period.getEndDate();

        if ( (closedDate != null && closedDate.before( startDate )) || openingDate.after( endDate ) )
        {
            throw new WebMessageException( conflict( "Organisation unit is closed for the selected period. " ) );
        }
    }

    /**
     * Retrieves and validate an OrganisationUnit.
     * 
     * @param ouUid the organisation unit uid.
     * @return the valid OrganisationUnit.
     * @throws WebMessageException if the validation fails.
     */
    OrganisationUnit getAndValidateOrganisationUnit( final String ouUid )
        throws WebMessageException
    {
        final OrganisationUnit organisationUnit = idObjectManager.get( OrganisationUnit.class, ouUid );

        if ( organisationUnit == null )
        {
            throw new WebMessageException( conflict( "Organisation unit not found or not accessible: " + ouUid ) );
        }

        final boolean isInHierarchy = organisationUnitService.isInUserHierarchyCached( organisationUnit );

        if ( !isInHierarchy )
        {
            throw new WebMessageException(
                conflict( "Organisation unit is not in the hierarchy of the current user: " + ouUid ) );
        }

        return organisationUnit;
    }

    /**
     * Validates if the given DataSet uid exists and is accessible and if the
     * DataSet contains the informed DataElement.
     * 
     * @param dsUid the DataSet uid.
     * @param dataElement the data element to be checked in the DataSet.
     * @return the valid DataSet.
     * @throws WebMessageException if the validation fails.
     */
    DataSet getAndValidateOptionalDataSet( final String dsUid, final DataElement dataElement )
        throws WebMessageException
    {
        if ( dsUid == null )
        {
            return null;
        }

        final DataSet dataSet = dataSetService.getDataSet( dsUid );

        if ( dataSet == null )
        {
            throw new WebMessageException( conflict( "Data set not found or not accessible: " + dsUid ) );
        }

        if ( !dataSet.getDataElements().contains( dataElement ) )
        {
            throw new WebMessageException(
                conflict( "Data set: " + dsUid + " does not contain data element: " + dataElement.getUid() ) );
        }

        return dataSet;
    }

    /**
     * Validate if the is after the last future period allowed by the DataElement.
     * 
     * @param period the period to be validated.
     * @param dataElement the base DataElement.
     * @throws WebMessageException if the validation fails.
     */
    void validateInvalidFuturePeriod( final Period period, final DataElement dataElement )
        throws WebMessageException
    {
        Period latestFuturePeriod = dataElement.getLatestOpenFuturePeriod();

        if ( period.isAfter( latestFuturePeriod ) && calendarService.getSystemCalendar().isIso8601() )
        {
            throw new WebMessageException(
                conflict( "Period: " + period.getIsoDate() + " is after latest open future period: "
                    + latestFuturePeriod.getIsoDate() + " for data element: " + dataElement.getUid() ) );
        }
    }

    /**
     * Check for an invalid period withing the given CategoryOptionCombo (attribute
     * option combo).
     * 
     * @param attributeOptionCombo is the CategoryOptionCombo.
     * @param period the period to be checked.
     * @param dataSet the data set (if present) to be checked.
     * @param dataElement the data element to be checked.
     * @throws WebMessageException if the validation fails.
     */
    void validateAttributeOptionCombo( final CategoryOptionCombo attributeOptionCombo,
        final Period period, final DataSet dataSet, final DataElement dataElement )
        throws WebMessageException
    {
        for ( CategoryOption option : attributeOptionCombo.getCategoryOptions() )
        {
            if ( option.getStartDate() != null && period.getEndDate().before( option.getStartDate() ) )
            {
                throw new WebMessageException( conflict( "Period " + period.getIsoDate() + " is before start date "
                    + i18nManager.getI18nFormat().formatDate( option.getStartDate() )
                    + " for attributeOption '" + option.getName() + "'" ) );
            }

            if ( option.getEndDate() != null && period.getStartDate().after( option.getAdjustedEndDate( dataSet, dataElement ) ) )
            {
                throw new WebMessageException( conflict( "Period " + period.getIsoDate() + " is after end date "
                    + i18nManager.getI18nFormat().formatDate( option.getAdjustedEndDate( dataSet, dataElement ) )
                    + " for attributeOption '" + option.getName() + "'" ) );
            }
        }
    }

    /**
     * Validate if the DataSet or DataElement is locked based on the input
     * arguments.
     * 
     * @param user the current User.
     * @param dataElement the DataElement.
     * @param period the Period.
     * @param dataSet the DataSet.
     * @param organisationUnit the OrganisationUnit.
     * @param attributeOptionCombo the CategoryOptionCombo.
     * @throws WebMessageException if the validation fails.
     */
    void validateDataSetNotLocked( final User user, final DataElement dataElement, final Period period,
        final DataSet dataSet, final OrganisationUnit organisationUnit, final CategoryOptionCombo attributeOptionCombo )
        throws WebMessageException
    {
        if ( dataSet == null
            ? dataSetService.isLocked( user, dataElement, period, organisationUnit, attributeOptionCombo, null )
            : dataSetService.isLocked( user, dataSet, period, organisationUnit, attributeOptionCombo, null ) )
        {
            throw new WebMessageException( conflict( "Data set is locked" ) );
        }
    }

    /**
     * Validate if the period is open for the given DataSet or DataElement.
     * 
     * @param dataElement the DataElement.
     * @param dataSet the DataSet.
     * @param period the Period.
     * @throws WebMessageException if the validation fails.
     */
    void validateDataInputPeriodForDataElementAndPeriod( final DataElement dataElement, final DataSet dataSet,
        final Period period )
        throws WebMessageException
    {
        if ( !(dataSet == null ? dataElement.isDataInputAllowedForPeriodAndDate( period, new Date() )
            : dataSet.isDataInputPeriodAndDateAllowed( period, new Date() )) )
        {
            throw new WebMessageException( conflict( "Period reported is not open in data set" ) );
        }
    }

    /**
     * Validates if the given file resource uid has a valid FileResource associated
     * with.
     * 
     * @param fileResourceUid the uid of the FileResource.
     * @return a valid FileResource.
     * @throws WebMessageException if any validation fails.
     */
    FileResource validateAndSetAssigned( final String fileResourceUid )
        throws WebMessageException
    {
        final FileResource fileResource;

        if ( fileResourceUid != null )
        {
            fileResource = fileResourceService.getFileResource( fileResourceUid );

            if ( fileResource == null || fileResource.getDomain() != DATA_VALUE )
            {
                throw new WebMessageException( notFound( FileResource.class, fileResourceUid ) );
            }

            if ( fileResource.isAssigned() )
            {
                throw new WebMessageException(
                    conflict( "File resource already assigned or linked to another data value" ) );
            }

            fileResource.setAssigned( true );
        }
        else
        {
            throw new WebMessageException( conflict( "Missing parameter 'value'" ) );
        }

        return fileResource;
    }

    /**
     * Validates a comment.
     * 
     * @param comment the comment to be validated.
     * @throws WebMessageException if the validation fails.
     */
    void validateComment( final String comment )
        throws WebMessageException
    {
        final String commentValid = ValidationUtils.commentIsValid( comment );

        if ( commentValid != null )
        {
            throw new WebMessageException( conflict( "Invalid comment: " + comment ) );
        }
    }

    /**
     * Checks if the given data value is a valid association with the OptionSet.
     * 
     * @param dataValue
     * @param optionSet
     * @param dataElement
     * @throws WebMessageException if the validation fails.
     */
    void validateOptionSet( final String dataValue, final OptionSet optionSet, final DataElement dataElement )
        throws WebMessageException
    {
        if ( !isNullOrEmpty( dataValue ) && optionSet != null
            && !optionSet.getOptionCodesAsSet().contains( dataValue ) )
        {
            throw new WebMessageException( conflict(
                "Data value is not a valid option of the data element option set: " + dataElement.getUid() ) );
        }
    }

    /**
     * Validates if the given dataValue is valid for the given DataElement,
     * and normalize it if the dataValue is a boolean type.
     * 
     * @param dataValue
     * @param dataElement
     * @return the normalized boolean or the same dataValue provided
     * @throws WebMessageException if the validation fails.
     */
    String validateAndNormalizeDataValue( final String dataValue, final DataElement dataElement )
        throws WebMessageException
    {
        final String normalizedBoolean = normalizeBoolean( dataValue, dataElement.getValueType() );

        final String valueValid = dataValueIsValid( normalizedBoolean, dataElement );

        if ( valueValid != null )
        {
            throw new WebMessageException( conflict(
                "Invalid value: " + dataValue + ", must match data element type: " + dataElement.getValueType() ) );
        }
        return normalizedBoolean;
    }

    /**
     * Checks if the User has write access to the given CategoryOptionCombo.
     * 
     * @param user the User.
     * @param categoryOptionCombo the CategoryOptionCombo.
     * @throws WebMessageException if the validation fails.
     */
    void checkCategoryOptionComboAccess( final User user, final CategoryOptionCombo categoryOptionCombo )
        throws WebMessageException
    {
        final List<String> categoryOptionComboErrors = accessManager.canWriteCached( user, categoryOptionCombo );

        if ( !categoryOptionComboErrors.isEmpty() )
        {
            throw new WebMessageException( conflict( "User does not have write access to category option combo: "
                + categoryOptionCombo.getUid() + ", errors: " + categoryOptionComboErrors ) );
        }
    }

    /**
     * Checks if the User has write access to the given CategoryOptionCombo
     * (attribute option combo).
     * 
     * @param user the User.
     * @param attributeOptionCombo the CategoryOptionCombo.
     * @throws WebMessageException if the validation fails.
     */
    void checkAttributeOptionComboAccess( final User user, final CategoryOptionCombo attributeOptionCombo )
        throws WebMessageException
    {
        final List<String> attributeOptionComboErrors = accessManager.canWriteCached( user, attributeOptionCombo );

        if ( !attributeOptionComboErrors.isEmpty() )
        {
            throw new WebMessageException( conflict( "User does not have write access to attribute option combo: "
                + attributeOptionCombo.getUid() + ", errors: " + attributeOptionComboErrors ) );
        }
    }

    /**
     * Check if the respective User has read access to the given DataValue.
     * 
     * @param user the User.
     * @param dataValue the DataValue.
     * @throws WebMessageException if the validation fails.
     */
    void checkDataValueSharing( final User user, final DataValue dataValue )
        throws WebMessageException
    {
        final List<String> errors = accessManager.canRead( user, dataValue );

        if ( !errors.isEmpty() )
        {
            throw new WebMessageException( forbidden( errors.toString() ) );
        }
    }
}
