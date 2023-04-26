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

import static org.hisp.dhis.system.deletion.DeletionVeto.ACCEPT;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceParam;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.system.deletion.DeletionVeto;
import org.hisp.dhis.system.deletion.IdObjectDeletionHandler;
import org.springframework.stereotype.Component;

/**
 * @author Zubair Asghar
 */

@Component
@RequiredArgsConstructor
public class ProgramNotificationInstanceDeletionHandler extends IdObjectDeletionHandler<ProgramNotificationInstance>
{
    private final ProgramNotificationInstanceService programNotificationInstanceService;

    @Override
    protected void registerHandler()
    {
        whenDeleting( ProgramInstance.class, this::deleteProgramInstance );
        whenDeleting( Event.class, this::deleteProgramStageInstance );
        whenVetoing( ProgramInstance.class, this::allowDeleteProgramInstance );
        whenVetoing( Event.class, this::allowDeleteProgramStageInstance );
    }

    private void deleteProgramInstance( ProgramInstance programInstance )
    {
        List<ProgramNotificationInstance> notificationInstances = programNotificationInstanceService
            .getProgramNotificationInstances(
                ProgramNotificationInstanceParam.builder().programInstance( programInstance ).build() );

        notificationInstances.forEach( programNotificationInstanceService::delete );
    }

    private void deleteProgramStageInstance( Event event )
    {
        List<ProgramNotificationInstance> notificationInstances = programNotificationInstanceService
            .getProgramNotificationInstances(
                ProgramNotificationInstanceParam.builder().event( event ).build() );

        notificationInstances.forEach( programNotificationInstanceService::delete );
    }

    private DeletionVeto allowDeleteProgramInstance( ProgramInstance programInstance )
    {
        List<ProgramNotificationInstance> instances = programNotificationInstanceService
            .getProgramNotificationInstances(
                ProgramNotificationInstanceParam.builder().programInstance( programInstance ).build() );

        return instances == null || instances.isEmpty() ? ACCEPT : VETO;
    }

    private DeletionVeto allowDeleteProgramStageInstance( Event event )
    {
        List<ProgramNotificationInstance> instances = programNotificationInstanceService
            .getProgramNotificationInstances(
                ProgramNotificationInstanceParam.builder().event( event ).build() );

        return instances == null || instances.isEmpty() ? ACCEPT : VETO;
    }
}
