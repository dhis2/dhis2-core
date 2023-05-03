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
package org.hisp.dhis.dxf2.events.event;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.user.User;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface EventService
{
    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    Events getEvents( EventSearchParams params );

    EventRows getEventRows( EventSearchParams params );

    Grid getEventsGrid( EventSearchParams params );

    org.hisp.dhis.dxf2.events.event.Event getEvent( Event event, EventParams eventParams );

    org.hisp.dhis.dxf2.events.event.Event getEvent( Event event, boolean isSynchronizationQuery,
        boolean skipOwnershipCheck, EventParams eventParams );

    // TODO remove these 2 methods and move the logic to the front-end
    List<org.hisp.dhis.dxf2.events.event.Event> getEventsXml( InputStream inputStream )
        throws IOException;

    List<org.hisp.dhis.dxf2.events.event.Event> getEventsJson( InputStream inputStream )
        throws IOException;

    /**
     * Returns the count of anonymous event that are ready for synchronization
     * (lastUpdated > lastSynchronized)
     *
     * @param skipChangedBefore the point in time specifying which events will
     *        be synchronized and which not
     * @return the count of anonymous event that are ready for synchronization
     *         (lastUpdated > lastSynchronized)
     */
    int getAnonymousEventReadyForSynchronizationCount( Date skipChangedBefore );

    /**
     * Returns the anonymous events that are supposed to be synchronized
     * (lastUpdated > lastSynchronized)
     *
     * @param pageSize Specifies the max number for the events returned.
     * @param skipChangedBefore the point in time specifying which events will
     *        be synchronized and which not
     * @param psdesWithSkipSyncTrue Holds information about PSDEs for which the
     *        data should not be synchronized
     * @return the anonymous events that are supposed to be synchronized
     *         (lastUpdated > lastSynchronized)
     */
    Events getAnonymousEventsForSync( int pageSize, Date skipChangedBefore,
        Map<String, Set<String>> psdesWithSkipSyncTrue );

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    ImportSummary addEvent( org.hisp.dhis.dxf2.events.event.Event event, ImportOptions importOptions,
        boolean bulkImport );

    ImportSummaries addEvents( List<org.hisp.dhis.dxf2.events.event.Event> events, ImportOptions importOptions,
        boolean clearSession );

    ImportSummaries addEvents( List<org.hisp.dhis.dxf2.events.event.Event> events, ImportOptions importOptions,
        JobConfiguration jobId );

    ImportSummaries addEventsXml( InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    ImportSummaries addEventsJson( InputStream inputStream, ImportOptions importOptions )
        throws IOException;

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    /**
     * Update an existing Program Stage Instance with the data from the Event
     * object
     *
     * @param event an Event
     * @param singleValue if true, skip the Data Value mandatory check
     *        validation and allow the client to send only Data Values that it
     *        wishes to update
     * @param importOptions the Import Options
     * @param bulkUpdate TODO this can be removed
     * @return an {@see ImportSummary} containing the outcome of the operation
     */
    ImportSummary updateEvent( org.hisp.dhis.dxf2.events.event.Event event, boolean singleValue,
        ImportOptions importOptions, boolean bulkUpdate );

    /**
     *
     * @param events a List of Events to update
     * @param importOptions the Import Options
     * @param singleValue if true, skip the Data Value mandatory check
     *        validation and allow the client to send only Data Values that it
     *        wishes to update
     * @param clearSession TODO this can be removed
     * @return an {@see ImportSummary} containing the outcome of the operation
     */
    ImportSummaries updateEvents( List<org.hisp.dhis.dxf2.events.event.Event> events, ImportOptions importOptions,
        boolean singleValue,
        boolean clearSession );

    void updateEventForNote( org.hisp.dhis.dxf2.events.event.Event event );

    void updateEventForEventDate( org.hisp.dhis.dxf2.events.event.Event event );

    /**
     * Updates a last sync timestamp on specified Events
     *
     * @param eventsUIDs UIDs of Events where the lastSynchronized flag should
     *        be updated
     * @param lastSynchronized The date of last successful sync
     */
    void updateEventsSyncTimestamp( List<String> eventsUIDs, Date lastSynchronized );

    ImportSummaries processEventImport( List<org.hisp.dhis.dxf2.events.event.Event> events, ImportOptions importOptions,
        JobConfiguration jobId );

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    ImportSummary deleteEvent( String uid );

    ImportSummaries deleteEvents( List<String> uids, boolean clearSession );

    void validate( EventSearchParams params, User user );
}
