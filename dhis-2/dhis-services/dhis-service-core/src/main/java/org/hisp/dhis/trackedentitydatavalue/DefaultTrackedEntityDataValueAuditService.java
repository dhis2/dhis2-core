package org.hisp.dhis.trackedentitydatavalue;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import com.google.common.collect.Lists;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.ProgramStageInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Transactional
public class DefaultTrackedEntityDataValueAuditService
    implements TrackedEntityDataValueAuditService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private TrackedEntityDataValueAuditStore trackedEntityDataValueAuditStore;

    // -------------------------------------------------------------------------
    // Implementation methods
    // -------------------------------------------------------------------------

    @Override
    public void addTrackedEntityDataValueAudit( TrackedEntityDataValueAudit trackedEntityDataValueAudit )
    {
        trackedEntityDataValueAuditStore.addTrackedEntityDataValueAudit( trackedEntityDataValueAudit );
    }

    @Override
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( TrackedEntityDataValue trackedEntityDataValue )
    {
        return getTrackedEntityDataValueAudits( trackedEntityDataValue.getDataElement(), trackedEntityDataValue.getProgramStageInstance() );
    }

    @Override
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( DataElement dataElement, ProgramStageInstance programStageInstance )
    {
        return getTrackedEntityDataValueAudits( Lists.newArrayList( dataElement ), Lists.newArrayList( programStageInstance ) );
    }

    @Override
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( List<DataElement> dataElements, List<ProgramStageInstance> programStageInstances )
    {
        return trackedEntityDataValueAuditStore.getTrackedEntityDataValueAudits( dataElements, programStageInstances, null );
    }

    @Override
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( List<DataElement> dataElements,
        List<ProgramStageInstance> programStageInstances, AuditType auditType )
    {
        return trackedEntityDataValueAuditStore.getTrackedEntityDataValueAudits( dataElements, programStageInstances, auditType );
    }

    @Override
    public List<TrackedEntityDataValueAudit> getTrackedEntityDataValueAudits( List<DataElement> dataElements,
        List<ProgramStageInstance> programStageInstances, AuditType auditType, int first, int max )
    {
        return trackedEntityDataValueAuditStore.getTrackedEntityDataValueAudits( dataElements, programStageInstances, auditType, first, max );
    }

    @Override
    public int countTrackedEntityDataValueAudits( List<DataElement> dataElements, List<ProgramStageInstance> programStageInstances, AuditType auditType )
    {
        return trackedEntityDataValueAuditStore.countTrackedEntityDataValueAudits( dataElements, programStageInstances, auditType );
    }
    
    @Override
    public void deleteTrackedEntityDataValueAudits( ProgramStageInstance programStageInstance )
    {
        trackedEntityDataValueAuditStore.deleteTrackedEntityDataValueAudits( programStageInstance );
    }
}
