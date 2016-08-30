package org.hisp.dhis.trackedentitydatavalue;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.fileresource.FileResource;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.user.CurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsValid;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Abyot Asalefew Gizaw
 */
@Transactional
public class DefaultTrackedEntityDataValueService
    implements TrackedEntityDataValueService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityDataValueStore dataValueStore;

    @Autowired
    private TrackedEntityDataValueAuditService dataValueAuditService;

    @Autowired
    private FileResourceService fileResourceService;

    @Autowired
    private CurrentUserService currentUserService;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void saveTrackedEntityDataValue( TrackedEntityDataValue trackedEntityDataValue )
    {
        trackedEntityDataValue.setAutoFields();

        if ( !StringUtils.isEmpty( trackedEntityDataValue.getValue() ) )
        {
            if ( StringUtils.isEmpty( trackedEntityDataValue.getStoredBy() ) )
            {
                trackedEntityDataValue.setStoredBy( currentUserService.getCurrentUsername() );
            }

            if ( trackedEntityDataValue.getDataElement() == null || trackedEntityDataValue.getDataElement().getValueType() == null )
            {
                throw new IllegalQueryException( "Data element or type is null or empty" );
            }

            String result = dataValueIsValid( trackedEntityDataValue.getValue(), trackedEntityDataValue.getDataElement().getValueType() );

            if ( result != null )
            {
                throw new IllegalQueryException( "Value is not valid:  " + result );
            }

            if ( trackedEntityDataValue.getDataElement().isFileType() )
            {
                handleFileDataValueSave( trackedEntityDataValue );
            }

            dataValueStore.saveVoid( trackedEntityDataValue );
        }
    }

    @Override
    public void updateTrackedEntityDataValue( TrackedEntityDataValue trackedEntityDataValue )
    {
        trackedEntityDataValue.setAutoFields();

        if ( StringUtils.isEmpty( trackedEntityDataValue.getValue() ) )
        {
            deleteTrackedEntityDataValue( trackedEntityDataValue );
        }
        else
        {
            if ( StringUtils.isEmpty( trackedEntityDataValue.getStoredBy() ) )
            {
                trackedEntityDataValue.setStoredBy( currentUserService.getCurrentUsername() );
            }

            if ( trackedEntityDataValue.getDataElement() == null || trackedEntityDataValue.getDataElement().getValueType() == null )
            {
                throw new IllegalQueryException( "Data element or type is null or empty" );
            }

            String result = dataValueIsValid( trackedEntityDataValue.getValue(), trackedEntityDataValue.getDataElement().getValueType() );

            if ( result != null )
            {
                throw new IllegalQueryException( "Value is not valid:  " + result );
            }

            createAndAddAudit( trackedEntityDataValue, trackedEntityDataValue.getStoredBy(), AuditType.UPDATE );
            handleFileDataValueUpdate( trackedEntityDataValue );

            dataValueStore.update( trackedEntityDataValue );
        }
    }

    @Override
    public void deleteTrackedEntityDataValue( TrackedEntityDataValue dataValue )
    {
        createAndAddAudit( dataValue, currentUserService.getCurrentUsername(), AuditType.DELETE );

        handleFileDataValueDelete( dataValue );

        dataValueStore.delete( dataValue );
    }

    @Override
    public void deleteTrackedEntityDataValue( ProgramStageInstance programStageInstance )
    {
        List<TrackedEntityDataValue> dataValues = dataValueStore.get( programStageInstance );
        String username = currentUserService.getCurrentUsername();

        for ( TrackedEntityDataValue dataValue : dataValues )
        {
            createAndAddAudit( dataValue, username, AuditType.DELETE );
            handleFileDataValueDelete( dataValue );
        }

        dataValueStore.delete( programStageInstance );
    }

    @Override
    public List<TrackedEntityDataValue> getTrackedEntityDataValues( ProgramStageInstance programStageInstance )
    {
        return dataValueStore.get( programStageInstance );
    }

    @Override
    public List<TrackedEntityDataValue> getTrackedEntityDataValues( ProgramStageInstance programStageInstance,
        Collection<DataElement> dataElements )
    {
        return dataValueStore.get( programStageInstance, dataElements );
    }

    @Override
    public List<TrackedEntityDataValue> getTrackedEntityDataValues( Collection<ProgramStageInstance> programStageInstances )
    {
        return dataValueStore.get( programStageInstances );
    }

    @Override
    public List<TrackedEntityDataValue> getTrackedEntityDataValues( DataElement dataElement )
    {
        return dataValueStore.get( dataElement );
    }

    @Override
    public List<TrackedEntityDataValue> getTrackedEntityDataValues( TrackedEntityInstance entityInstance,
        Collection<DataElement> dataElements, Date startDate, Date endDate )
    {
        return dataValueStore.get( entityInstance, dataElements, startDate, endDate );
    }

    @Override
    public TrackedEntityDataValue getTrackedEntityDataValue( ProgramStageInstance programStageInstance,
        DataElement dataElement )
    {
        return dataValueStore.get( programStageInstance, dataElement );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void createAndAddAudit( TrackedEntityDataValue dataValue, String username, AuditType auditType )
    {
        TrackedEntityDataValueAudit dataValueAudit = new TrackedEntityDataValueAudit( dataValue, dataValue.getAuditValue(), username, auditType );
        dataValueAuditService.addTrackedEntityDataValueAudit( dataValueAudit );
    }

    private void handleFileDataValueUpdate( TrackedEntityDataValue dataValue )
    {
        String previousFileResourceUid = dataValue.getAuditValue();

        if ( previousFileResourceUid == null || previousFileResourceUid.equals( dataValue.getValue() ) )
        {
            return;
        }

        FileResource fileResource = fetchFileResource( dataValue );

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
    private void handleFileDataValueSave( TrackedEntityDataValue dataValue )
    {
        FileResource fileResource = fetchFileResource( dataValue );

        if ( fileResource == null )
        {
            return;
        }

        setAssigned( fileResource );
    }

    /**
     * Delete associated FileResource if it exists.
     */
    private void handleFileDataValueDelete( TrackedEntityDataValue dataValue )
    {
        FileResource fileResource = fetchFileResource( dataValue );

        if ( fileResource == null )
        {
            return;
        }

        fileResourceService.deleteFileResource( fileResource.getUid() );
    }

    private FileResource fetchFileResource( TrackedEntityDataValue dataValue )
    {
        if ( !dataValue.getDataElement().isFileType() )
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
