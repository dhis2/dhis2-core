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
import static org.hisp.dhis.dxf2.importsummary.ImportStatus.ERROR;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.shared.ImmutableEvent;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;

/**
 * Simple interface that provides checking capabilities on events.
 *
 * @author maikel arabori
 */
public interface EventChecking
{
    List<ImportSummary> check( final WorkContext workContext, final List<Event> events );

    Log log = getLog( EventChecking.class );

    class ValidationRunner
    {
        private final WorkContext workContext;

        private final List<Event> events;

        public ValidationRunner( WorkContext workContext, List<Event> events )
        {
            this.workContext = workContext;
            this.events = events;
        }

        /**
         * Validates the events using the supplied list of validators.
         * 
         * Only returns the ImportSummary for Events that *did* not pass validation
         * 
         * @param validators List of classes implementing the {@see Checker} interface
         * @return returns the ImportSummary for Events that did not pass validation
         */
        public List<ImportSummary> run( final List<Class<? extends Checker>> validators )
        {
            final List<ImportSummary> importSummaries = new ArrayList<>( 0 );

            for ( final Event event : events )
            {
                for ( final Class<? extends Checker> validator : validators )
                {
                    try
                    {
                        final Checker validationCheck = validator.newInstance();
                        final ImportSummary importSummary = validationCheck.check( new ImmutableEvent( event ),
                            workContext );

                        if ( importSummary.isStatus( ERROR ) )
                        {
                            importSummaries.add( importSummary );
                            break;
                        }
                    }
                    catch ( InstantiationException | IllegalAccessException e )
                    {
                        log.error( "An error occurred during Event import validation", e );
                    }
                }
            }

            return importSummaries;
        }
    }
}
