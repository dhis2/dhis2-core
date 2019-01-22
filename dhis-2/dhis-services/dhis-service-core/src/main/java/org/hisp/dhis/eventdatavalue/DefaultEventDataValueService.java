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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
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

    private final TrackedEntityDataValueAuditService dataValueAuditService;

    private final FileResourceService fileResourceService;

    private final ProgramStageInstanceService programStageInstanceService;

    @Autowired
    public DefaultEventDataValueService( TrackedEntityDataValueAuditService dataValueAuditService,
        FileResourceService fileResourceService, ProgramStageInstanceService programStageInstanceService )
    {
        this.dataValueAuditService = dataValueAuditService;
        this.fileResourceService = fileResourceService;
        this.programStageInstanceService = programStageInstanceService;
    }

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void auditDataValuesChangesAndHandleFileDataValues( Set<EventDataValue> newDataValues,
        Set<EventDataValue> updatedDataValues, Set<EventDataValue> removedDataValues,
        Map<String, DataElement> dataElementsCache, ProgramStageInstance programStageInstance, boolean singleValue )
    {
        Set<EventDataValue> updatedOrNewDataValues = Sets.union( newDataValues, updatedDataValues );

        if ( singleValue )
        {
            // If it is only a single value update, I don't won't to miss the values that
            // are missing in the payload but already present in the DB
            Set<EventDataValue> changedDataValues = Sets.union( updatedOrNewDataValues, removedDataValues );
            Set<EventDataValue> unchangedDataValues = Sets.difference( programStageInstance.getEventDataValues(),
                changedDataValues );

            programStageInstance.setEventDataValues( Sets.union( unchangedDataValues, updatedOrNewDataValues ) );
        }
        else
        {
            programStageInstance.setEventDataValues( updatedOrNewDataValues );
        }

        auditDataValuesChanges( newDataValues, updatedDataValues, removedDataValues, dataElementsCache,
            programStageInstance );
        handleFileDataValueChanges( newDataValues, updatedDataValues, removedDataValues, dataElementsCache );
    }

    @Override
    public void validateAuditAndHandleFilesForEventDataValuesSave( ProgramStageInstance programStageInstance,
        Map<DataElement, EventDataValue> dataElementEventDataValueMap )
    {
        validateEventDataValues( dataElementEventDataValueMap );
        auditAndHandleFilesForEventDataValuesSave( programStageInstance, dataElementEventDataValueMap );
        programStageInstance.getEventDataValues().addAll( dataElementEventDataValueMap.values() );
    }

    @Override
    public void saveEventDataValuesAndSaveProgramStageInstance( ProgramStageInstance programStageInstance,
        Map<DataElement, EventDataValue> dataElementEventDataValueMap )
    {
        validateEventDataValues( dataElementEventDataValueMap );
        programStageInstance.setEventDataValues( (Set<EventDataValue>) dataElementEventDataValueMap.values() );
        programStageInstanceService.addProgramStageInstance( programStageInstance );

        auditAndHandleFilesForEventDataValuesSave( programStageInstance, dataElementEventDataValueMap );
    }

    private void auditAndHandleFilesForEventDataValuesSave( ProgramStageInstance programStageInstance,
        Map<DataElement, EventDataValue> dataElementsAndEventDataValues )
    {
        for ( Map.Entry<DataElement, EventDataValue> entry : dataElementsAndEventDataValues.entrySet() )
        {
            entry.getValue().setAutoFields();
            createAndAddAudit( entry.getValue(), entry.getKey(), programStageInstance, AuditType.CREATE );
            handleFileDataValueSave( entry.getValue(), entry.getKey() );
        }
    }

    @Override
    public void validateAuditAndHandleFilesForEventDataValuesUpdate( ProgramStageInstance programStageInstance,
        Map<DataElement, EventDataValue> dataElementEventDataValueMap )
    {
        Map<DataElement, EventDataValue> dataElementEventDataValueMapWithEmptyValues = new HashMap<>();
        Map<DataElement, EventDataValue> filteredDataElementEventDataValueMap = new HashMap<>();
        for ( Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet() )
        {
            if ( StringUtils.isEmpty( entry.getValue() ) )
            {
                dataElementEventDataValueMapWithEmptyValues.put( entry.getKey(), entry.getValue() );
            }
            else
            {
                filteredDataElementEventDataValueMap.put( entry.getKey(), entry.getValue() );
            }
        }

        if ( !dataElementEventDataValueMapWithEmptyValues.isEmpty() )
        {
            auditAndHandleFilesForEventDataValuesDelete( programStageInstance,
                dataElementEventDataValueMapWithEmptyValues );
        }

        if ( !filteredDataElementEventDataValueMap.isEmpty() )
        {
            validateEventDataValues( filteredDataElementEventDataValueMap );

            for ( Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet() )
            {
                DataElement dataElement = entry.getKey();
                EventDataValue eventDataValue = entry.getValue();
                createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.UPDATE );
                handleFileDataValueUpdate( eventDataValue, dataElement );
            }

            // Need to do it in this way as adding elements that are already added (so
            // overwriting) is non-deterministic / not defined
            programStageInstance.getEventDataValues().removeAll( filteredDataElementEventDataValueMap.values() );
            programStageInstance.getEventDataValues().addAll( filteredDataElementEventDataValueMap.values() );
        }
    }

    @Override
    public void auditAndHandleFilesForEventDataValuesDelete( ProgramStageInstance programStageInstance,
        Map<DataElement, EventDataValue> dataElementEventDataValueMap )
    {
        for ( Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet() )
        {
            DataElement dataElement = entry.getKey();
            EventDataValue eventDataValue = entry.getValue();
            createAndAddAudit( eventDataValue, dataElement, programStageInstance, AuditType.DELETE );
            handleFileDataValueDelete( eventDataValue, dataElement );
        }

        programStageInstance.getEventDataValues().removeAll( dataElementEventDataValueMap.values() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private String validateEventDataValue( DataElement dataElement, EventDataValue eventDataValue )
    {

        if ( StringUtils.isEmpty( eventDataValue.getStoredBy() ) )
        {
            return "Stored by is null or empty";
        }

        if ( StringUtils.isEmpty( eventDataValue.getDataElement() ) )
        {
            return "Data element is null or empty";
        }

        if ( !dataElement.getUid().equals( eventDataValue.getDataElement() ) )
        {
            throw new IllegalQueryException( "DataElement " + dataElement.getUid()
                + " assigned to EventDataValues does not match with one EventDataValue: "
                + eventDataValue.getDataElement() );
        }

        String result = ValidationUtils.dataValueIsValid( eventDataValue.getValue(), dataElement.getValueType() );

        return result == null ? null : "Value is not valid:  " + result;
    }

    private void validateEventDataValues( Map<DataElement, EventDataValue> dataElementEventDataValueMap )
    {
        String result;
        for ( Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet() )
        {
            result = validateEventDataValue( entry.getKey(), entry.getValue() );
            if ( result != null )
            {
                throw new IllegalQueryException( result );
            }
        }
    }

    private void auditDataValuesChanges( Set<EventDataValue> newDataValues, Set<EventDataValue> updatedDataValues,
        Set<EventDataValue> removedDataValues, Map<String, DataElement> dataElementsCache,
        ProgramStageInstance programStageInstance )
    {

        newDataValues.forEach( dv -> createAndAddAudit( dv, dataElementsCache.get( dv.getDataElement() ),
            programStageInstance, AuditType.CREATE ) );
        updatedDataValues.forEach( dv -> createAndAddAudit( dv, dataElementsCache.get( dv.getDataElement() ),
            programStageInstance, AuditType.UPDATE ) );
        removedDataValues.forEach( dv -> createAndAddAudit( dv, dataElementsCache.get( dv.getDataElement() ),
            programStageInstance, AuditType.DELETE ) );
    }

    private void createAndAddAudit( EventDataValue dataValue, DataElement dataElement,
        ProgramStageInstance programStageInstance, AuditType auditType )
    {
        TrackedEntityDataValueAudit dataValueAudit = new TrackedEntityDataValueAudit( dataElement, programStageInstance,
            dataValue.getValue(), dataValue.getStoredBy(), dataValue.getProvidedElsewhere(), auditType );
        dataValueAuditService.addTrackedEntityDataValueAudit( dataValueAudit );
    }

    private void handleFileDataValueChanges( Set<EventDataValue> newDataValues, Set<EventDataValue> updatedDataValues,
        Set<EventDataValue> removedDataValues, Map<String, DataElement> dataElementsCache )
    {

        removedDataValues
            .forEach( dv -> handleFileDataValueDelete( dv, dataElementsCache.get( dv.getDataElement() ) ) );
        updatedDataValues
            .forEach( dv -> handleFileDataValueUpdate( dv, dataElementsCache.get( dv.getDataElement() ) ) );
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
