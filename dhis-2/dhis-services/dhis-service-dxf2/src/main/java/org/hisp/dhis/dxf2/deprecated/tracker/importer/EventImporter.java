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
package org.hisp.dhis.dxf2.deprecated.tracker.importer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.partition;
import static java.lang.System.nanoTime;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.dxf2.metadata.feedback.ImportReportMode.ERRORS;
import static org.hisp.dhis.system.notification.NotificationLevel.INFO;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.event.Event;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContext;
import org.hisp.dhis.dxf2.deprecated.tracker.importer.context.WorkContextLoader;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventImporter
{
    private static final int BATCH_SIZE = 100;

    private final EventManager eventManager;

    private final WorkContextLoader workContextLoader;

    private final Notifier notifier;

    public EventImporter( final EventManager eventManager, final WorkContextLoader workContextLoader,
        final Notifier notifier )
    {
        checkNotNull( eventManager );
        checkNotNull( workContextLoader );
        checkNotNull( notifier );

        this.eventManager = eventManager;
        this.workContextLoader = workContextLoader;
        this.notifier = notifier;
    }

    public ImportSummaries importAll( final List<Event> events, final ImportOptions importOptions,
        final JobConfiguration jobConfiguration )
    {
        assert importOptions != null;

        final ImportSummaries importSummaries = new ImportSummaries();

        if ( events.size() == 0 )
        {
            return importSummaries;
        }

        notifier.clear( jobConfiguration ).notify( jobConfiguration, "Importing events" );
        final Clock clock = new Clock( log ).startClock();

        long now = nanoTime();

        final WorkContext context = workContextLoader.load( importOptions, events );

        log.debug( "::: event tracker import context load took : " + (nanoTime() - now) );

        final List<List<Event>> partitions = partition( events, BATCH_SIZE );

        for ( final List<Event> batch : partitions )
        {
            final ImportStrategyAccumulator accumulator = new ImportStrategyAccumulator().partitionEvents( batch,
                importOptions.getImportStrategy(), context.getProgramStageInstanceMap() );

            importSummaries.addImportSummaries( eventManager.addEvents( accumulator.getCreate(), context ) );
            importSummaries.addImportSummaries( eventManager.updateEvents( accumulator.getUpdate(), context ) );
            importSummaries.addImportSummaries( eventManager.deleteEvents( accumulator.getDelete(), context ) );
        }

        if ( jobConfiguration != null )
        {
            notifier.notify( jobConfiguration, INFO, "Import done. Completed in " + clock.time() + ".", true )
                .addJobSummary( jobConfiguration, importSummaries, ImportSummaries.class );
        }
        else
        {
            clock.logTime( "Import done" );
        }

        if ( ERRORS == importOptions.getReportMode() && isNotEmpty( importSummaries.getImportSummaries() ) )
        {
            importSummaries.getImportSummaries().removeIf( is -> !is.hasConflicts() );
        }

        return importSummaries;
    }
}
