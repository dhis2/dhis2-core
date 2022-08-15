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
package org.hisp.dhis.program;

import static org.hisp.dhis.external.conf.ConfigurationKey.CHANGELOG_TRACKER;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAudit;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Abyot Asalefew
 */
@AllArgsConstructor
@Service( "org.hisp.dhis.program.ProgramStageInstanceService" )
public class DefaultProgramStageInstanceService
    implements ProgramStageInstanceService
{
    private final ProgramStageInstanceStore programStageInstanceStore;

    private final TrackedEntityDataValueAuditService dataValueAuditService;

    private final FileResourceService fileResourceService;

    private final DhisConfigurationProvider config;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        programStageInstance.setAutoFields();
        programStageInstanceStore.save( programStageInstance );
        return programStageInstance.getId();
    }

    @Override
    @Transactional
    public long addProgramStageInstance( ProgramStageInstance programStageInstance, User user )
    {
        programStageInstance.setAutoFields();
        programStageInstanceStore.save( programStageInstance, user );
        return programStageInstance.getId();
    }

    @Override
    @Transactional
    public void deleteProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        programStageInstanceStore.delete( programStageInstance );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramStageInstance getProgramStageInstance( long id )
    {
        return programStageInstanceStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramStageInstance getProgramStageInstance( String uid )
    {
        return programStageInstanceStore.getByUid( uid );
    }

    @Override
    @Transactional
    public void updateProgramStageInstance( ProgramStageInstance programStageInstance )
    {
        programStageInstance.setAutoFields();
        programStageInstanceStore.update( programStageInstance );
    }

    @Override
    @Transactional
    public void updateProgramStageInstance( ProgramStageInstance programStageInstance, User user )
    {
        programStageInstance.setAutoFields();
        programStageInstanceStore.update( programStageInstance, user );
    }

    @Override
    @Transactional
    public void updateProgramStageInstancesSyncTimestamp( List<String> programStageInstanceUIDs, Date lastSynchronized )
    {
        programStageInstanceStore.updateProgramStageInstancesSyncTimestamp( programStageInstanceUIDs,
            lastSynchronized );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean programStageInstanceExists( String uid )
    {
        return programStageInstanceStore.exists( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean programStageInstanceExistsIncludingDeleted( String uid )
    {
        return programStageInstanceStore.existsIncludingDeleted( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<String> getProgramStageInstanceUidsIncludingDeleted( List<String> uids )
    {
        return programStageInstanceStore.getUidsIncludingDeleted( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public long getProgramStageInstanceCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return programStageInstanceStore.getProgramStageInstanceCountLastUpdatedAfter( cal.getTime() );
    }

    @Override
    @Transactional
    public ProgramStageInstance createProgramStageInstance( ProgramInstance programInstance, ProgramStage programStage,
        Date enrollmentDate, Date incidentDate, OrganisationUnit organisationUnit )
    {
        ProgramStageInstance programStageInstance = null;
        Date currentDate = new Date();
        Date dateCreatedEvent;

        if ( programStage.getGeneratedByEnrollmentDate() )
        {
            dateCreatedEvent = enrollmentDate;
        }
        else
        {
            dateCreatedEvent = incidentDate;
        }

        Date dueDate = DateUtils.addDays( dateCreatedEvent, programStage.getMinDaysFromStart() );

        if ( !programInstance.getProgram().getIgnoreOverdueEvents() || dueDate.before( currentDate ) )
        {
            programStageInstance = new ProgramStageInstance();
            programStageInstance.setProgramInstance( programInstance );
            programStageInstance.setProgramStage( programStage );
            programStageInstance.setOrganisationUnit( organisationUnit );
            programStageInstance.setDueDate( dueDate );
            programStageInstance.setStatus( EventStatus.SCHEDULE );

            if ( programStage.getOpenAfterEnrollment() || programInstance.getProgram().isWithoutRegistration()
                || programStage.getPeriodType() != null )
            {
                programStageInstance.setExecutionDate( dueDate );
                programStageInstance.setStatus( EventStatus.ACTIVE );
            }

            addProgramStageInstance( programStageInstance );
        }

        return programStageInstance;
    }

    @Override
    @Transactional
    public void saveEventDataValuesAndSaveProgramStageInstance( ProgramStageInstance programStageInstance,
        Map<DataElement, EventDataValue> dataElementEventDataValueMap )
    {
        validateEventDataValues( dataElementEventDataValueMap );
        Set<EventDataValue> eventDataValues = new HashSet<>( dataElementEventDataValueMap.values() );
        programStageInstance.setEventDataValues( eventDataValues );
        addProgramStageInstance( programStageInstance );

        for ( Map.Entry<DataElement, EventDataValue> entry : dataElementEventDataValueMap.entrySet() )
        {
            entry.getValue().setAutoFields();
            createAndAddAudit( entry.getValue(), entry.getKey(), programStageInstance, AuditType.CREATE );
            handleFileDataValueSave( entry.getValue(), entry.getKey() );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // Validation
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

    // -------------------------------------------------------------------------
    // Audit
    // -------------------------------------------------------------------------

    private void createAndAddAudit( EventDataValue dataValue, DataElement dataElement,
        ProgramStageInstance programStageInstance, AuditType auditType )
    {
        if ( !config.isEnabled( CHANGELOG_TRACKER ) || dataElement == null )
        {
            return;
        }

        TrackedEntityDataValueAudit dataValueAudit = new TrackedEntityDataValueAudit( dataElement, programStageInstance,
            dataValue.getValue(), dataValue.getStoredBy(), dataValue.getProvidedElsewhere(), auditType );

        dataValueAuditService.addTrackedEntityDataValueAudit( dataValueAudit );
    }

    // -------------------------------------------------------------------------
    // File data values
    // -------------------------------------------------------------------------

    /**
     * Update FileResource with 'assigned' status.
     */
    private void handleFileDataValueSave( EventDataValue dataValue, DataElement dataElement )
    {
        if ( dataElement == null )
        {
            return;
        }

        FileResource fileResource = fetchFileResource( dataValue, dataElement );

        if ( fileResource == null )
        {
            return;
        }

        setAssigned( fileResource );
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
