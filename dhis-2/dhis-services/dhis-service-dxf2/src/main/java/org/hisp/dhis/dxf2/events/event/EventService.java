package org.hisp.dhis.dxf2.events.event;

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

import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.report.EventRows;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.query.Order;
import org.hisp.dhis.scheduling.TaskId;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public interface EventService
{
    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    Events getEvents( EventSearchParams params );

    Events getEvents( Collection<String> uids );

    EventRows getEventRows( EventSearchParams params );

    EventSearchParams getFromUrl( String program, String programStage, ProgramStatus programStatus, Boolean followUp, String orgUnit,
        OrganisationUnitSelectionMode orgUnitSelectionMode, String trackedEntityInstance, Date startDate, Date endDate,
        EventStatus status, Date lastUpdated, DataElementCategoryOptionCombo attributeCoc, IdSchemes idSchemes, Integer page,
        Integer pageSize, boolean totalPages, boolean skipPaging, List<Order> orders, boolean includeAttributes, Set<String> events );

    Event getEvent( String uid );

    Event getEvent( ProgramStageInstance programStageInstance );

    List<Event> getEventsXml( InputStream inputStream ) throws IOException;

    List<Event> getEventsJson( InputStream inputStream ) throws IOException;

    int getAnonymousEventValuesCountLastUpdatedAfter( Date lastSuccessTime );

    Events getAnonymousEventValuesLastUpdatedAfter( Date lastSuccessTime );

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    ImportSummary addEvent( Event event, ImportOptions importOptions );

    ImportSummaries addEvents( List<Event> events, ImportOptions importOptions );

    ImportSummaries addEvents( List<Event> events, ImportOptions importOptions, TaskId taskId );

    ImportSummaries addEventsXml( InputStream inputStream, ImportOptions importOptions ) throws IOException;

    ImportSummaries addEventsXml( InputStream inputStream, TaskId taskId, ImportOptions importOptions ) throws IOException;

    ImportSummaries addEventsJson( InputStream inputStream, ImportOptions importOptions ) throws IOException;

    ImportSummaries addEventsJson( InputStream inputStream, TaskId taskId, ImportOptions importOptions ) throws IOException;

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    ImportSummary updateEvent( Event event, boolean singleValue );

    ImportSummary updateEvent( Event event, boolean singleValue, ImportOptions importOptions );

    ImportSummaries updateEvents( List<Event> events, boolean singleValue );

    void updateEventForNote( Event event );

    void updateEventForEventDate( Event event );

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    ImportSummary deleteEvent( String uid );

    ImportSummaries deleteEvents( List<String> uids );

    void validate( EventSearchParams params );

}
