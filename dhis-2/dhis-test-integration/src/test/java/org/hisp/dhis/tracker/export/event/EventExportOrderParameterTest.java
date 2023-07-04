/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author rajazubair
 */
class EventExportOrderParameterTest extends TrackerTest
{
    @Autowired
    private EventService eventService;

    @Autowired
    private TrackerImportService trackerImportService;

    @Autowired
    private IdentifiableObjectManager manager;

    private OrganisationUnit orgUnit;

    private ProgramStage programStage;

    private Program program;

    private final List<String> asc = List.of( "D9PbzJY8bJM", "pTzf9KYMk72" );

    private final List<String> desc = List.of( "pTzf9KYMk72", "D9PbzJY8bJM" );

    @Override
    protected void initTest()
        throws IOException
    {
        setUpMetadata( "tracker/simple_metadata.json" );
        User userA = userService.getUser( "M5zQapPyTZI" );
        assertNoErrors(
            trackerImportService
                .importTracker( fromJson( "tracker/event_and_enrollment_with_notes.json", userA.getUid() ) ) );
        orgUnit = get( OrganisationUnit.class, "h4w96yEMlzO" );
        programStage = get( ProgramStage.class, "NpsdDv6kKSO" );
        program = programStage.getProgram();

        manager.flush();
    }

    @BeforeEach
    void setUp()
    {
        // needed as some tests are run using another user (injectSecurityContext) while most tests expect to be run by admin
        injectAdminUser();
    }

    @Test
    void shouldExportEventWithOrderOccurredAtDESC()
        throws ForbiddenException,
        BadRequestException
    {
        OrderCriteria orderCriteria = OrderCriteria.of( "occurredAt", SortDirection.DESC );

        assertEquals( desc, getEvents( getParams( orderCriteria ) ) );
    }

    @Test
    void shouldExportEventWithOrderOccurredAtASC()
        throws ForbiddenException,
        BadRequestException
    {
        OrderCriteria orderCriteria = OrderCriteria.of( "occurredAt", SortDirection.ASC );

        assertEquals( asc, getEvents( getParams( orderCriteria ) ) );
    }

    @Test
    void shouldExportEventWithOrderScheduledAtDESC()
        throws ForbiddenException,
        BadRequestException
    {
        OrderCriteria orderCriteria = OrderCriteria.of( "scheduleAt", SortDirection.DESC );

        assertEquals( desc, getEvents( getParams( orderCriteria ) ) );
    }

    @Test
    void shouldExportEventWithOrderScheduledAtASC()
        throws ForbiddenException,
        BadRequestException
    {
        OrderCriteria orderCriteria = OrderCriteria.of( "scheduleAt", SortDirection.ASC );

        assertEquals( asc, getEvents( getParams( orderCriteria ) ) );
    }

    private EventOperationParams getParams( OrderCriteria orderCriteria )
    {
        return EventOperationParams.builder().orgUnitUid( orgUnit.getUid() )
            .programUid( program.getUid() ).programStageUid( programStage.getUid() )
            .orders( List.of( orderCriteria.toOrderParam() ) ).attributeOrders( List.of( orderCriteria ) )
            .build();
    }

    private <T extends IdentifiableObject> T get( Class<T> type, String uid )
    {
        T t = manager.get( type, uid );
        assertNotNull( t, () -> String.format( "metadata with uid '%s' should have been created", uid ) );
        return t;
    }

    private List<String> getEvents( EventOperationParams params )
        throws ForbiddenException,
        BadRequestException
    {
        return eventService.getEvents( params )
            .getEvents().stream().map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() );
    }
}
