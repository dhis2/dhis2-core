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

package org.hisp.dhis.dxf2.events.importer;

import static org.apache.commons.logging.LogFactory.getLog;

import java.util.List;

import org.apache.commons.logging.Log;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;

/**
 * Simple interface that provides processing capabilities on events.
 *
 * @author maikel arabori
 */
public interface EventProcessing
{
    void process( WorkContext workContext, List<Event> events );

    Log log = getLog( EventProcessing.class );

    class ProcessorRunner
    {
        private final WorkContext workContext;

        private final List<Event> events;

        public ProcessorRunner( WorkContext workContext, List<Event> events )
        {
            this.workContext = workContext;
            this.events = events;
        }

        public void run( final List<Class<? extends Processor>> processors )
        {
            for ( final Event event : events )
            {
                for ( Class<? extends Processor> processor : processors )
                {
                    try
                    {
                        final Processor pre = processor.newInstance();
                        pre.process( event, workContext );
                    }
                    catch ( InstantiationException | IllegalAccessException e )
                    {
                        log.error( "An error occurred during Event import processing", e );
                    }
                }
            }
        }
    }
}
