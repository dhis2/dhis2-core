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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.validation;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifierBasedOnIdScheme;
import static org.hisp.dhis.dxf2.deprecated.tracker.event.EventUtils.eventDataValuesToJson;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.deprecated.tracker.event.DataValue;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.Checker;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableSet;

/**
 * @author Luciano Fiandesio
 */
@Component
public class DataValueCheck implements Checker
{
    private static final Set<String> VALID_IMAGE_FORMATS = ImmutableSet.<String> builder().add(
        ImageIO.getReaderFormatNames() ).build();

    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        final Set<DataValue> dataValues = event.getDataValues();
        final ImportSummary importSummary = new ImportSummary();
        final User user = ctx.getImportOptions().getUser();

        for ( DataValue dataValue : dataValues )
        {
            if ( !checkHasValidDataElement( importSummary, ctx, dataValue, event )
                || !checkSerializeToJson( importSummary, ctx, dataValue ) )
            {
                importSummary.setStatus( ImportStatus.ERROR );
                importSummary.setReference( event.getUid() );
                importSummary.incrementIgnored();

                return importSummary;
            }
        }

        if ( !importSummary.hasConflicts() )
        {
            if ( doValidationOfMandatoryAttributes( user ) && isValidationRequired( event, ctx ) )
            {
                validateMandatoryAttributes( importSummary, ctx, event );
            }
        }

        if ( importSummary.hasConflicts() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.setReference( event.getUid() );
            importSummary.incrementIgnored();
        }

        return importSummary;
    }

    public void validateMandatoryAttributes( ImportConflicts importConflicts, WorkContext ctx,
        ImmutableEvent event )
    {
        if ( StringUtils.isEmpty( event.getProgramStage() ) )
            return;

        final IdScheme programStageIdScheme = ctx.getImportOptions().getIdSchemes().getProgramStageIdScheme();
        final IdScheme dataElementIdScheme = ctx.getImportOptions().getIdSchemes().getDataElementIdScheme();
        final Map<String, Set<EventDataValue>> eventDataValueMap = ctx.getEventDataValueMap();

        final boolean allowSingleUpdates = ctx.getImportOptions().isMergeDataValues();

        ProgramStage programStage = ctx.getProgramStage( programStageIdScheme, event.getProgramStage() );

        final Set<ProgramStageDataElement> mandatoryDataElements = getMandatoryProgramStageDataElements( programStage );

        // Data Element IDs associated to the current event
        Set<String> dataValues = eventDataValueMap.get( event.getUid() ).stream()
            .map( EventDataValue::getDataElement )
            .collect( Collectors.toSet() );

        if ( allowSingleUpdates )
        {
            final Event programStageInstance = ctx.getProgramStageInstanceMap().get( event.getUid() );

            dataValues.addAll( programStageInstance.getEventDataValues().stream()
                .filter( dv -> !StringUtils.isEmpty( dv.getValue().trim() ) ).map( EventDataValue::getDataElement )
                .collect( Collectors.toSet() ) );
        }

        for ( ProgramStageDataElement mandatoryDataElement : mandatoryDataElements )
        {
            String resolvedDataElementId = getIdentifierBasedOnIdScheme( mandatoryDataElement.getDataElement(),
                dataElementIdScheme );
            if ( !dataValues.contains( resolvedDataElementId ) )
            {
                importConflicts.addConflict( resolvedDataElementId, "value_required_but_not_provided" );
            }
        }
    }

    private Set<ProgramStageDataElement> getMandatoryProgramStageDataElements( ProgramStage programStage )
    {
        return Optional.ofNullable( programStage )
            .map( ProgramStage::getProgramStageDataElements )
            .orElse( Collections.emptySet() )
            .stream()
            .filter( ProgramStageDataElement::isCompulsory )
            .collect( Collectors.toSet() );
    }

    /**
     * Checks if the data value can be serialized to Json
     */
    private boolean checkSerializeToJson( ImportConflicts importConflicts, WorkContext ctx, DataValue dataValue )
    {
        try
        {
            eventDataValuesToJson( dataValue, ctx.getServiceDelegator().getJsonMapper() );
        }
        catch ( JsonProcessingException | SQLException e )
        {
            importConflicts.addConflict( dataValue.getDataElement(), "Invalid data value found." );
        }
        return !importConflicts.hasConflicts();
    }

    /**
     * Checks that the specified Data Element ID (uid/code/id) corresponds to an
     * existing Data Element
     *
     */
    private boolean checkHasValidDataElement( ImportConflicts importConflicts, WorkContext ctx, DataValue dataValue,
        ImmutableEvent event )
    {
        DataElement dataElement = ctx.getDataElementMap().get( dataValue.getDataElement() );

        if ( dataElement == null )
        {
            // This can happen if a wrong data element identifier is provided
            importConflicts.addConflict( "dataElement", dataValue.getDataElement() + " is not a valid data element" );
        }
        else
        {
            String status = null;

            if ( dataElement.hasOptionSet() )
            {
                status = validateOptionDataValue( dataElement, dataValue );
            }
            else if ( ValueType.FILE_RESOURCE == dataElement.getValueType() )
            {
                status = validateFileResourceDataValue( dataValue, ctx, event );
            }
            else if ( ValueType.IMAGE == dataElement.getValueType() )
            {
                status = validateImageDataValue( dataValue, ctx, event );
            }
            else if ( ValueType.ORGANISATION_UNIT == dataElement.getValueType() )
            {
                status = validateOrgUnitDataValue( dataValue, ctx );
            }
            else
            {
                status = ValidationUtils.valueIsValid( dataValue.getValue(), dataElement );
            }

            if ( status != null )
            {
                importConflicts.addConflict( dataElement.getUid(), status );
            }
        }

        return !importConflicts.hasConflicts();
    }

    private String validateOptionDataValue( DataElement dataElement, DataValue dataValue )
    {
        String value = dataValue.getValue();

        OptionSet optionSet = dataElement.getOptionSet();

        if ( isNullOrEmpty( value ) || optionSet == null )
        {
            return null;
        }

        boolean isValid = true;

        if ( dataElement.getValueType().isMultiText() )
        {
            isValid = dataElement.getOptionSet().hasAllOptions( ValueType.splitMultiText( value ) );
        }
        else
        {
            isValid = dataElement.getOptionSet().getOptionByCode( value ) != null;
        }

        return !isValid ? "Value '" + value + "' is not a valid option code of option set: " + optionSet.getUid()
            : null;
    }

    private String validateFileResourceDataValue( DataValue dataValue, WorkContext ctx, ImmutableEvent event )
    {
        String value = dataValue.getValue();

        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        FileResource fileResource = ctx.getServiceDelegator().getFileResourceService().getFileResource( value );

        if ( fileResource == null )
        {
            return "Value is not a valid file resource: " + value;
        }

        return validateFileResourceOwnership( fileResource, event, ctx );
    }

    private String validateImageDataValue( DataValue dataValue, WorkContext ctx, ImmutableEvent event )
    {
        String value = dataValue.getValue();

        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        FileResource fileResource = ctx.getServiceDelegator().getFileResourceService().getFileResource( value );

        if ( fileResource == null || !VALID_IMAGE_FORMATS.contains( fileResource.getFormat() ) )
        {
            return "Value is not a valid image file resource: " + value;
        }

        return validateFileResourceOwnership( fileResource, event, ctx );

    }

    private String validateFileResourceOwnership( FileResource fileResource, ImmutableEvent event, WorkContext ctx )
    {
        if ( fileResource.getFileResourceOwner() != null
            && !fileResource.getFileResourceOwner().equals( event.getEvent() ) )
        {
            return "File resource is assigned to another item";
        }

        if ( fileResource.getFileResourceOwner() == null && ctx.getImportOptions().getImportStrategy().isCreate()
            && fileResource.isAssigned() )
        {
            return "File resource is assigned to another item";
        }

        return null;
    }

    private String validateOrgUnitDataValue( DataValue dataValue, WorkContext ctx )
    {
        String value = dataValue.getValue();

        if ( isNullOrEmpty( value ) )
        {
            return null;
        }

        return ctx.getServiceDelegator().getOrganisationUnitService().getOrganisationUnit( value ) == null
            ? "Value is not a valid organisation unit: " + value
            : null;
    }

    private boolean isValidationRequired( ImmutableEvent event, WorkContext ctx )
    {
        final ValidationStrategy validationStrategy = getValidationStrategy( ctx, event );

        return validationStrategy == null || validationStrategy == ValidationStrategy.ON_UPDATE_AND_INSERT
            || (validationStrategy == ValidationStrategy.ON_COMPLETE && event.getStatus() == EventStatus.COMPLETED);

    }

    private boolean doValidationOfMandatoryAttributes( User user )
    {
        return user == null
            || !user.isAuthorized( Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() );
    }

    private ValidationStrategy getValidationStrategy( WorkContext ctx, ImmutableEvent event )
    {
        if ( StringUtils.isNotEmpty( event.getProgramStage() ) )
        {
            return ctx
                .getProgramStage( ctx.getImportOptions().getIdSchemes().getProgramStageIdScheme(),
                    event.getProgramStage() )
                .getValidationStrategy();
        }
        return null;
    }
}
