package org.hisp.dhis.dxf2.events.importer.shared.validation;

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

import static org.hisp.dhis.common.IdentifiableObjectUtils.getIdentifierBasedOnIdScheme;
import static org.hisp.dhis.dxf2.events.event.EventUtils.eventDataValuesToJson;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.importer.Checker;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author Luciano Fiandesio
 */
public class DataValueCheck implements Checker
{
    @Override
    public ImportSummary check( ImmutableEvent event, WorkContext ctx )
    {
        final Set<DataValue> dataValues = event.getDataValues();
        final ImportSummary importSummary = new ImportSummary();
        final User user = ctx.getImportOptions().getUser();

        for ( DataValue dataValue : dataValues )
        {
            if ( !checkHasValidDataElement( importSummary, ctx, dataValue )
                || !checkSerializeToJson( importSummary, ctx, dataValue ) )
            {
                importSummary.setStatus( ImportStatus.ERROR );
                importSummary.setReference( event.getUid() );
                importSummary.incrementIgnored();

                return importSummary;
            }
        }

        if ( importSummary.getConflicts().isEmpty() )
        {
            if ( doValidationOfMandatoryAttributes( user ) && isValidationRequired( event, ctx ) )
            {
                validateMandatoryAttributes( importSummary, ctx, event );
            }
        }

        if ( !importSummary.getConflicts().isEmpty() )
        {
            importSummary.setStatus( ImportStatus.ERROR );
            importSummary.setReference( event.getUid() );
            importSummary.incrementIgnored();
        }

        return importSummary;
    }

    public void validateMandatoryAttributes( ImportSummary importSummary, WorkContext ctx,
        ImmutableEvent event )
    {
        if ( StringUtils.isEmpty( event.getProgramStage() ) )
            return;

        final IdScheme programStageIdScheme = ctx.getImportOptions().getIdSchemes().getProgramStageIdScheme();
        final IdScheme dataElementIdScheme = ctx.getImportOptions().getIdSchemes().getDataElementIdScheme();
        final Map<String, Set<EventDataValue>> eventDataValueMap = ctx.getEventDataValueMap();

        final boolean allowSingleUpdates = ctx.getImportOptions().isSkipDataValueMandatoryValidationCheck();

        ProgramStage programStage = ctx.getProgramStage( programStageIdScheme, event.getProgramStage() );

        final Set<ProgramStageDataElement> mandatoryDataElements = programStage.getProgramStageDataElements();

        // Data Element IDs associated to the current event
        Set<String> dataValues = eventDataValueMap.get( event.getUid() ).stream()
            .map( EventDataValue::getDataElement )
            .collect( Collectors.toSet() );

        if ( allowSingleUpdates )
        {
            final ProgramStageInstance programStageInstance = ctx.getProgramStageInstanceMap().get( event.getUid() );

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
                importSummary.getConflicts()
                    .add( new ImportConflict( resolvedDataElementId, "value_required_but_not_provided" ) );
            }
        }
    }

    /**
     * Checks if the data value can be serialized to Json
     */
    private boolean checkSerializeToJson( ImportSummary importSummary, WorkContext ctx, DataValue dataValue )
    {
        try
        {
            eventDataValuesToJson( dataValue, ctx.getServiceDelegator().getJsonMapper() );
        }
        catch ( JsonProcessingException | SQLException e )
        {
            importSummary.getConflicts()
                .add( new ImportConflict( dataValue.getDataElement(), "Invalid data value found." ) );
        }
        return importSummary.getConflicts().isEmpty();
    }

    /**
     * Checks that the specified Data Element ID (uid/code/id) corresponds to an
     * existing Data Element
     *
     */
    private boolean checkHasValidDataElement( ImportSummary importSummary, WorkContext ctx, DataValue dataValue )
    {
        DataElement dataElement = ctx.getDataElementMap().get( dataValue.getDataElement() );

        if ( dataElement == null )
        {
            // This can happen if a wrong data element identifier is provided
            importSummary.getConflicts().add(
                new ImportConflict( "dataElement", dataValue.getDataElement() + " is not a valid data element" ) );
        }
        else
        {
            final String status = ValidationUtils.dataValueIsValid( dataValue.getValue(), dataElement );

            if ( status != null )
            {
                importSummary.getConflicts().add( new ImportConflict( dataElement.getUid(), status ) );
            }
        }

        return importSummary.getConflicts().isEmpty();
    }

    private boolean isValidationRequired( ImmutableEvent event, WorkContext ctx )
    {
        final ValidationStrategy validationStrategy = getValidationStrategy( ctx, event );

        return validationStrategy == null || validationStrategy == ValidationStrategy.ON_UPDATE_AND_INSERT
            || (validationStrategy == ValidationStrategy.ON_COMPLETE && event.getStatus() == EventStatus.COMPLETED );

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
