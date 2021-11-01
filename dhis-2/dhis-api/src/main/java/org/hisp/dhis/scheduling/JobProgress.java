/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.util.List;
import java.util.function.Consumer;

public interface JobProgress
{
    JobProgress IGNORANT = new JobProgress()
    {
        @Override
        public boolean isCancellationRequested()
        {
            return false;
        }

        @Override
        public void nextStage( String name, int workItems )
        {

        }

        @Override
        public void completedStage( String summary )
        {

        }

        @Override
        public void failedStage( String error )
        {

        }

        @Override
        public void nextWorkItem( String name )
        {

        }

        @Override
        public void completedWorkItem( String summary )
        {

        }

        @Override
        public void failedWorkItem( String error )
        {

        }
    };

    /**
     * @return true, if the job got cancelled and requests the processing thread
     *         to terminate, else false to continue processing the job
     */
    boolean isCancellationRequested();

    /**
     *
     * @param name descriptive name for the stage that starts now
     * @param workItems number of work items in the stage, -1 if unknown
     */
    void nextStage( String name, int workItems );

    void completedStage( String summary );

    void failedStage( String error );

    void nextWorkItem( String name );

    void completedWorkItem( String summary );

    void failedWorkItem( String error );

    default void nextWorkItem( int i )
    {
        nextWorkItem( "#" + (i + 1) );
    }

    default void failedWorkItem( Exception cause )
    {
        failedWorkItem( cause.getMessage() );
    }

    // it could be part of the job config to select if the progress should also
    // be forwarded to the notifier - this allows to keep updating the notifier
    // but also to replace notifier updates with pure in-memory progress
    // tracking state

    // the job config ID does not need to be passed around as the progress
    // instance is created for a particular job and job config so it can already
    // know the ID internally

    default void run( List<Consumer<JobProgress>> items )
    {
        for ( Consumer<JobProgress> item : items )
        {
            if ( isCancellationRequested() )
            {
                return; // ends the stage immediately
            }
            try
            {
                item.accept( this );
            }
            catch ( RuntimeException ex )
            {
                failedWorkItem( ex );
            }
        }
    }
}
