package org.hisp.dhis.dxf2.events.eventdatavalue;
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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.TrackerAccessManager;
import org.hisp.dhis.dxf2.events.event.AbstractEventService;
import org.hisp.dhis.dxf2.events.event.DataValue;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ValidationStrategy;
import org.hisp.dhis.programrule.engine.DataValueUpdatedEvent;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.collect.Sets;

/**
 * @author David Katuscak
 */
@Transactional
public class DefaultEventDataValueService implements EventDataValueService
{
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    protected org.hisp.dhis.eventdatavalue.EventDataValueService eventDataValueService;

    @Autowired
    protected TrackerAccessManager trackerAccessManager;

    @Override
    public void processDataValues( ProgramStageInstance programStageInstance, Event event, boolean isUpdate,
        boolean singleValue, ImportOptions importOptions, ImportSummary importSummary, Map<String, DataElement> dataElementsCache ) {

        Map<String, EventDataValue> dataElementValueMap = getDataElementToEventDataValueMap( programStageInstance.getEventDataValues() );

        boolean validateMandatoryAttributes = doValidationOfMandatoryAttributes( importOptions.getUser() );
        if ( validateMandatoryAttributes )
        {
            if ( !validatePresenceOfMandatoryDataElements( event, programStageInstance, importSummary, singleValue ) )
            {
                importSummary.setStatus( ImportStatus.ERROR );
                importSummary.incrementIgnored();

                return;
            }
        }

        Set<EventDataValue> newDataValues = new HashSet<>();
        Set<EventDataValue> updatedDataValues = new HashSet<>();
        Set<EventDataValue> removedDataValuesDueToEmptyValue = new HashSet<>();
        String fallbackStoredBy =
            AbstractEventService.getValidUsername( event.getStoredBy(), importSummary, importOptions.getUser() != null ? importOptions.getUser().getUsername() : "[Unknown]" );

        for ( DataValue dataValue : event.getDataValues() )
        {
            String storedBy = !StringUtils.isEmpty( dataValue.getStoredBy() ) ? dataValue.getStoredBy() : fallbackStoredBy;
            DataElement dataElement = dataElementsCache.get( dataValue.getDataElement() );

            if ( dataElement == null ) {
                // This can happen if a wrong data element identifier is provided
                importSummary.getConflicts().add( new ImportConflict( "dataElement", dataValue.getDataElement() + " is not a valid data element" ) );
            }
            else if ( validateDataValue( programStageInstance, importOptions.getUser(), dataElement, dataValue.getValue(), importSummary )
                && !importOptions.isDryRun())
            {
                prepareDataValueForStorage( dataElementValueMap, programStageInstance, dataValue, dataElement, newDataValues,
                    updatedDataValues, removedDataValuesDueToEmptyValue, storedBy );
            }
        }

        eventDataValueService.persistDataValues( newDataValues, updatedDataValues, newDataValues, dataElementsCache, programStageInstance, singleValue );

        if ( isUpdate && !importOptions.isSkipNotifications() )
        {
            eventPublisher.publishEvent( new DataValueUpdatedEvent( this, programStageInstance ) );
        }
    }

    private void prepareDataValueForStorage( Map<String, EventDataValue> dataElementToValueMap, ProgramStageInstance programStageInstance,
        DataValue dataValue, DataElement dataElement, Set<EventDataValue> newDataValues, Set<EventDataValue> updatedDataValues,
        Set<EventDataValue> removedDataValuesDueToEmptyValue, String storedBy ) {

        EventDataValue eventDataValue;

        // The data value for this element was already saved so make an update
        if ( dataElementToValueMap.containsKey( dataValue.getDataElement() ) )
        {
            eventDataValue = dataElementToValueMap.get( dataValue.getDataElement() );
            eventDataValue.setStoredBy( storedBy );

            if ( dataValue.getValue() != null && !dataValue.getValue().trim().isEmpty() )
            {
                eventDataValue.setValue( dataValue.getValue() );
                eventDataValue.setLastUpdated( new Date() );
                eventDataValue.setProvidedElsewhere( dataValue.getProvidedElsewhere() );

                updatedDataValues.add( eventDataValue );
            }
            else {
                // dataValue was present in the payload but was empty, I consider it as it should be removed from DB. This special case
                // is used only when it is a singleValue update. If it is regular update, then just not including it in
                // updatedOrNewDataValues is enough
                removedDataValuesDueToEmptyValue.add( eventDataValue );
            }
        }
        // Value is not present in DB so consider it a new and save if it is valid
        else if ( dataValue.getValue() != null && !dataValue.getValue().trim().isEmpty() )
        {
            eventDataValue = new EventDataValue( dataElement.getUid(), dataValue.getValue() );
            eventDataValue.setAutoFields();
            eventDataValue.setStoredBy( storedBy );
            eventDataValue.setProvidedElsewhere( dataValue.getProvidedElsewhere() );

            newDataValues.add( eventDataValue );
        }
    }

    private Map<String, EventDataValue> getDataElementToEventDataValueMap(
        Collection<EventDataValue> dataValues )
    {
        return dataValues.stream().collect( Collectors.toMap( dv -> dv.getDataElement(), dv -> dv ) );
    }

    private boolean doValidationOfMandatoryAttributes( User user )
    {
        return user == null || !user.isAuthorized( Authorities.F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION.getAuthority() );
    }

    private boolean validatePresenceOfMandatoryDataElements(Event event, ProgramStageInstance programStageInstance, ImportSummary importSummary, boolean isSingleValueUpdate) {
        ValidationStrategy validationStrategy = programStageInstance.getProgramStage().getValidationStrategy();

        if ( validationStrategy == ValidationStrategy.ON_UPDATE_AND_INSERT ||
            (validationStrategy == ValidationStrategy.ON_COMPLETE && event.getStatus() == EventStatus.COMPLETED) )
        {
            //I am filling the set only if I know that I will do the validation. Otherwise, it would be waste of resources
            Set<String>  mandatoryDataElements = programStageInstance.getProgramStage().getProgramStageDataElements().stream()
                .filter( psde -> psde.isCompulsory() )
                .map( psde -> psde.getDataElement().getUid() )
                .collect( Collectors.toSet() );

            //Collect all data elements with valid data values present in the payload
            Set<String> presentDataElements = event.getDataValues().stream()
                .filter( dv -> dv != null && dv.getValue() != null && !dv.getValue().trim().isEmpty() && !dv.getValue().trim().equals( "null" ) )
                .map( dv -> dv.getDataElement() )
                .collect( Collectors.toSet());

            // When the request is update, then only changed data values can be in the payload and so I should take into
            // account also already stored data values in order to make correct decision. Basically, this situation happens when
            // only 1 dataValue is updated and /events/{uid}/{dataElementUid} endpoint is leveraged.
            if ( isSingleValueUpdate ) {
                presentDataElements.addAll(
                    programStageInstance.getEventDataValues().stream()
                        .filter( dv -> !StringUtils.isEmpty( dv.getValue().trim() ))
                        .map( EventDataValue::getDataElement )
                        .collect( Collectors.toSet()));
            }

            Set<String> notPresentMandatoryDataElements = Sets.difference( mandatoryDataElements, presentDataElements );

            if ( notPresentMandatoryDataElements.size() > 0 )
            {
                notPresentMandatoryDataElements.forEach( deUid -> importSummary.getConflicts().add( new ImportConflict( deUid, "value_required_but_not_provided" ) ) );
                return false;
            }
        }

        return true;
    }

    private boolean validateDataValue( ProgramStageInstance programStageInstance, User user, DataElement dataElement,
        String value, ImportSummary importSummary )
    {
        String status = ValidationUtils.dataValueIsValid( value, dataElement );
        boolean validationPassed = true;

        if ( status != null )
        {
            importSummary.getConflicts().add( new ImportConflict( dataElement.getUid(), status ) );
            validationPassed = false;
        }

        List<String> errors = trackerAccessManager.canWrite( user, programStageInstance, dataElement );

        if ( !errors.isEmpty() )
        {
            errors.forEach( error -> importSummary.getConflicts().add( new ImportConflict( dataElement.getUid(), error ) ) );
            validationPassed = false;
        }

        return validationPassed;
    }
}
