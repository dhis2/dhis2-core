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
package org.hisp.dhis.scheduling;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link JobProgress} implementation that forwards the tracking to a
 * {@link Notifier}. It has no flow control and should be wrapped in a
 * {@link ControlledJobProgress} for that purpose.
 *
 * @see ControlledJobProgress
 */
@RequiredArgsConstructor
public class NotifierJobProgress implements JobProgress
{
    private final Notifier notifier;

    private final JobConfiguration jobId;

    private final AtomicBoolean hasCleared = new AtomicBoolean();

    private int stageItems;

    private int stageItem;

    @Override
    public boolean isCancellationRequested()
    {
        return false;
    }

    @Override
    public void startingProcess( String description )
    {
        String message = isNotEmpty( description )
            ? description
            : jobId.getJobType() + " process started";
        if ( hasCleared.compareAndSet( false, true ) )
        {
            notifier.clear( jobId );
        }
        notifier.notify( jobId, message + getJobParameterString() );
    }

    @Override
    public void completedProcess( String summary )
    {
        notifier.notify( jobId, summary, true );
    }

    @Override
    public void failedProcess( String error )
    {
        notifier.notify( jobId, NotificationLevel.ERROR, error, true );
    }

    @Override
    public void startingStage( String description, int workItems, FailurePolicy onFailure )
    {
        stageItems = workItems;
        stageItem = 0;
        if ( isNotEmpty( description ) )
        {
            notifier.notify( jobId, description );
        }
    }

    @Override
    public void completedStage( String summary )
    {
        if ( isNotEmpty( summary ) )
        {
            notifier.notify( jobId, summary );
        }
    }

    @Override
    public void failedStage( String error )
    {
        if ( isNotEmpty( error ) )
        {
            notifier.notify( jobId, NotificationLevel.ERROR, error, false );
        }
    }

    @Override
    public void startingWorkItem( String description, FailurePolicy onFailure )
    {
        if ( isNotEmpty( description ) )
        {
            String nOf = "[" + (stageItems > 0 ? stageItem + "/" + stageItems : "" + stageItem) + "] ";
            notifier.notify( jobId, NotificationLevel.LOOP, nOf + description, false );
        }
        stageItem++;
    }

    @Override
    public void completedWorkItem( String summary )
    {
        if ( isNotEmpty( summary ) )
        {
            String nOf = "[" + (stageItems > 0 ? stageItem + "/" + stageItems : "" + stageItem) + "] ";
            notifier.notify( jobId, NotificationLevel.LOOP, nOf + summary, false );
        }
    }

    @Override
    public void failedWorkItem( String error )
    {
        if ( isNotEmpty( error ) )
        {
            notifier.notify( jobId, NotificationLevel.ERROR, error, false );
        }
    }

    private String getJobParameterString()
    {
        JobParameters params = jobId.getJobParameters();
        if ( params == null )
        {
            return "";
        }
        try
        {
            return "\n" + new ObjectMapper().writeValueAsString( params );
        }
        catch ( Exception ex )
        {
            return "(failed to extract job parameter object)";
        }
    }
}
