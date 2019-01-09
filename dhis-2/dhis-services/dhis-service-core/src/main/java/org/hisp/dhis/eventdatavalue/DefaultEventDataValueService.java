package org.hisp.dhis.eventdatavalue;
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.google.common.collect.Sets;

/**
 * @author David Katuscak
 */
@Transactional
public class DefaultEventDataValueService implements EventDataValueService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityDataValueAuditService dataValueAuditService;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private DataElementService dataElementService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    //TODO: All (or almost all) the logic can be simplified to directly call the SQL queries. This could/should increase performance but
    // can make a mess with Hibernate cache. Therefore, should be discussed first.

    @Override
    public void persistDataValues( Set<EventDataValue> newDataValues, Set<EventDataValue> updatedDataValues,
        Set<EventDataValue> removedDataValues, Map<String, DataElement> dataElementsCache, ProgramStageInstance programStageInstance,
        boolean singleValue ) {

        //TODO: See the Todo on the top of the class (Can be done with use of concatenate (||) parameter)

        Set<EventDataValue> updatedOrNewDataValues = Sets.union( newDataValues, updatedDataValues );

        if ( singleValue ) {
            //If it is only a single value update, I don't won't to miss the values that are missing in the payload but already present in the DB
            Set<EventDataValue> changedDataValues = Sets.union( updatedOrNewDataValues, removedDataValues );
            Set<EventDataValue> unchangedDataValues = Sets.difference( programStageInstance.getEventDataValues(), changedDataValues );

            programStageInstance.setEventDataValues( Sets.union( unchangedDataValues, updatedOrNewDataValues ) );
        }
        else {
            programStageInstance.setEventDataValues( updatedOrNewDataValues );
        }

        auditDataValuesChanges( newDataValues, updatedDataValues, removedDataValues, dataElementsCache, programStageInstance );
        handleFileDataValueChanges( newDataValues, updatedDataValues, removedDataValues, dataElementsCache );
        programStageInstanceService.updateProgramStageInstance( programStageInstance );
    }


    @Override
    public void saveEventDataValue( ProgramStageInstance programStageInstance, EventDataValue eventDataValue )
    {
        //TODO: See the Todo on the top of the class (Can be done with use of concatenate (||) parameter)

        if ( !StringUtils.isEmpty( eventDataValue.getValue() ) )
        {
            eventDataValue.setAutoFields();

            String result = validateEventDataValue( eventDataValue );
            if ( result != null )
            {
                throw new IllegalQueryException( result );
            }

            DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
            handleFileDataValueSave( eventDataValue, dataElement );

            programStageInstance.getEventDataValues().add( eventDataValue );
            createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.CREATE );

            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }
    }

    @Override
    public void saveEventDataValues( ProgramStageInstance programStageInstance, Set<EventDataValue> eventDataValues )
    {
        //TODO: See the Todo on the top of the class (Can be done with use of concatenate (||) parameter)

        Set<EventDataValue> filteredEventDataValues = filterOutEmptyDataValues( eventDataValues );

        if( !filteredEventDataValues.isEmpty() )
        {
            validateEventDataValues( filteredEventDataValues );

            for ( EventDataValue eventDataValue : filteredEventDataValues )
            {
                DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
                createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.CREATE );
                handleFileDataValueSave( eventDataValue, dataElement );
            }

            programStageInstance.getEventDataValues().addAll( filteredEventDataValues );
            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }
    }

    @Override
    public void updateEventDataValue( ProgramStageInstance programStageInstance, EventDataValue eventDataValue )
    {
        //TODO: See the Todo on the top of the class (Can be done with use of concatenate (||) parameter -> overrides existed value)

        if ( StringUtils.isEmpty( eventDataValue.getValue() ) )
        {
            deleteEventDataValue( programStageInstance, eventDataValue );
        }
        else {
            eventDataValue.setAutoFields();

            String result = validateEventDataValue( eventDataValue );
            if ( result != null )
            {
                throw new IllegalQueryException( result );
            }

            DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
            handleFileDataValueUpdate( eventDataValue, dataElement );

            programStageInstance.getEventDataValues().remove( eventDataValue );
            programStageInstance.getEventDataValues().add( eventDataValue );
            createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.UPDATE );

            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }
    }

    @Override public void updateEventDataValues( ProgramStageInstance programStageInstance, Set<EventDataValue> eventDataValues )
    {
        Set<EventDataValue> eventDataValuesWithEmptyValues = eventDataValues.stream().filter( dv -> StringUtils.isEmpty( dv.getValue() ) ).collect( Collectors.toSet());
        deleteEventDataValues( programStageInstance, eventDataValuesWithEmptyValues );

        Set<EventDataValue> filteredEventDataValues = filterOutEmptyDataValues( eventDataValues );

        if( !filteredEventDataValues.isEmpty() )
        {
            validateEventDataValues( filteredEventDataValues );

            for ( EventDataValue eventDataValue : filteredEventDataValues )
            {
                DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
                createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.UPDATE );
                handleFileDataValueUpdate( eventDataValue, dataElement );
            }

            //Need to do this as adding elements that are already added (so overwriting) is non-deterministic / not defined
            programStageInstance.getEventDataValues().removeAll( filteredEventDataValues );
            programStageInstance.getEventDataValues().addAll( filteredEventDataValues );
            programStageInstanceService.updateProgramStageInstance( programStageInstance );
        }
    }

    @Override public void deleteEventDataValue( ProgramStageInstance programStageInstance, EventDataValue eventDataValue )
    {
        //TODO: See the Todo on the top of the class (Can be done with use of delete (-) parameter)

        if ( StringUtils.isEmpty( eventDataValue.getDataElement() ) )
        {
            throw new  IllegalQueryException( "Data element is null or empty" );
        }

        DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
        if ( dataElement == null ) {
            throw new  IllegalQueryException( "Given data element (" +  eventDataValue.getDataElement() + ") does not exist" );
        }

        createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.DELETE );
        handleFileDataValueDelete( eventDataValue, dataElement );
        programStageInstance.getEventDataValues().remove( eventDataValue );
        programStageInstanceService.updateProgramStageInstance( programStageInstance );
    }

    @Override public void deleteEventDataValues( ProgramStageInstance programStageInstance, Set<EventDataValue> eventDataValues )
    {
        //TODO: See the Todo on the top of the class (Can be done with use of delete (-) parameter)

        for ( EventDataValue eventDataValue : eventDataValues ) {
            if ( StringUtils.isEmpty( eventDataValue.getDataElement() ) )
            {
                throw new  IllegalQueryException( "Data element is null or empty" );
            }

            DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
            if ( dataElement == null ) {
                throw new  IllegalQueryException( "Given data element (" +  eventDataValue.getDataElement() + ") does not exist" );
            }
        }

        for ( EventDataValue eventDataValue : eventDataValues ) {
            DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
            createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.DELETE );
            handleFileDataValueDelete( eventDataValue, dataElement );
        }

        programStageInstance.getEventDataValues().removeAll( eventDataValues );
        programStageInstanceService.updateProgramStageInstance( programStageInstance );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Set<EventDataValue> filterOutEmptyDataValues( Set<EventDataValue> eventDataValues ) {
        return eventDataValues.stream().filter( dv -> !StringUtils.isEmpty( dv.getValue() ) ).collect( Collectors.toSet());
    }

    private String validateEventDataValue( EventDataValue eventDataValue ) {

        if ( StringUtils.isEmpty( eventDataValue.getStoredBy() ) )
        {
            eventDataValue.setStoredBy( currentUserService.getCurrentUsername() );
        }

        if ( StringUtils.isEmpty( eventDataValue.getDataElement() ) )
        {
            return "Data element is null or empty";
        }

        DataElement dataElement = dataElementService.getDataElement( eventDataValue.getDataElement() );
        if ( dataElement == null ) {
            return "Given data element (" +  eventDataValue.getDataElement() + ") does not exist" ;
        }

        String result = ValidationUtils.dataValueIsValid( eventDataValue.getValue(), dataElement.getValueType() );

        return result == null ? null : "Value is not valid:  " + result;
    }

    private void validateEventDataValues( Set<EventDataValue> eventDataValues ) {

        String result;
        for ( EventDataValue eventDataValue : eventDataValues ) {
            result = validateEventDataValue( eventDataValue );
            if ( result != null) {
                throw new IllegalQueryException( result );
            }
        }
    }

    private void auditDataValuesChanges( Set<EventDataValue> newDataValues, Set<EventDataValue> updatedDataValues,
        Set<EventDataValue> removedDataValues, Map<String, DataElement> dataElementsCache, ProgramStageInstance programStageInstance ) {

        newDataValues.forEach( dv -> createAndAddAudit( dv, dataElementsCache.get( dv.getDataElement() ), programStageInstance, AuditType.CREATE ) );
        updatedDataValues.forEach( dv -> createAndAddAudit( dv, dataElementsCache.get( dv.getDataElement() ), programStageInstance, AuditType.UPDATE ) );
        removedDataValues.forEach( dv -> createAndAddAudit( dv, dataElementsCache.get( dv.getDataElement() ), programStageInstance, AuditType.DELETE ) );
    }

    private void createAndAddAudit( EventDataValue dataValue, DataElement dataElement, ProgramStageInstance programStageInstance,
        AuditType auditType )
    {
        TrackedEntityDataValueAudit dataValueAudit = new TrackedEntityDataValueAudit( dataElement, programStageInstance,
            dataValue.getValue(), dataValue.getStoredBy(), dataValue.getProvidedElsewhere(), auditType );
        dataValueAuditService.addTrackedEntityDataValueAudit( dataValueAudit );
    }

    private void handleFileDataValueChanges ( Set<EventDataValue> newDataValues, Set<EventDataValue> updatedDataValues,
        Set<EventDataValue> removedDataValues, Map<String, DataElement> dataElementsCache ) {

        removedDataValues.forEach( dv -> handleFileDataValueDelete( dv, dataElementsCache.get( dv.getDataElement() ) ) );
        updatedDataValues.forEach( dv -> handleFileDataValueUpdate( dv, dataElementsCache.get( dv.getDataElement() ) ) );
        newDataValues.forEach( dv -> handleFileDataValueSave( dv, dataElementsCache.get( dv.getDataElement() ) ) );
    }

    private void handleFileDataValueUpdate( EventDataValue dataValue, DataElement dataElement )
    {
        String previousFileResourceUid = dataValue.getAuditValue();

        if ( previousFileResourceUid == null || previousFileResourceUid.equals( dataValue.getValue() ) )
        {
            return;
        }

        FileResource fileResource = fetchFileResource( dataValue, dataElement );

        if ( fileResource == null )
        {
            return;
        }

        fileResourceService.deleteFileResource( previousFileResourceUid );

        setAssigned( fileResource );
    }

    /**
     * Update FileResource with 'assigned' status.
     */
    private void handleFileDataValueSave( EventDataValue dataValue, DataElement dataElement )
    {
        FileResource fileResource = fetchFileResource( dataValue, dataElement );

        if ( fileResource == null )
        {
            return;
        }

        setAssigned( fileResource );
    }

    /**
     * Delete associated FileResource if it exists.
     */
    private void handleFileDataValueDelete( EventDataValue dataValue, DataElement dataElement )
    {
        FileResource fileResource = fetchFileResource( dataValue, dataElement );

        if ( fileResource == null )
        {
            return;
        }

        fileResourceService.deleteFileResource( fileResource.getUid() );
    }

    private FileResource fetchFileResource( EventDataValue dataValue, DataElement dataElement )
    {
        if ( !dataElement.isFileType() )
        {
            return null;
        }

        return fileResourceService.getFileResource( dataValue.getValue() );
    }

    private void setAssigned( FileResource fileResource )
    {
        fileResource.setAssigned( true );
        fileResourceService.updateFileResource( fileResource );
    }
}
