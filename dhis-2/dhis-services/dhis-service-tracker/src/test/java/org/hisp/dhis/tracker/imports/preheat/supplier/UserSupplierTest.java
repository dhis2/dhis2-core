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
package org.hisp.dhis.tracker.imports.preheat.supplier;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.random.BeanRandomizer;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.domain.User;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Luciano Fiandesio
 */
@ExtendWith( MockitoExtension.class )
class UserSupplierTest
{

    @InjectMocks
    private UserSupplier supplier;

    @Mock
    private UserService userService;

    @Mock
    private IdentifiableObjectManager manager;

    private final BeanRandomizer rnd = BeanRandomizer.create( Event.class, "assignedUser" );

    private List<org.hisp.dhis.user.User> users;

    private List<Event> events;

    @BeforeEach
    void setup()
    {
        events = rnd.objects( Event.class, 5 ).collect( Collectors.toList() );
        events.forEach( e -> e.setAssignedUser(
            User.builder()
                .uid( CodeGenerator.generateUid() )
                .username( RandomStringUtils.random( 10 ) )
                .build() ) );
        users = rnd.objects( org.hisp.dhis.user.User.class, 5 ).collect( Collectors.toList() );

        IntStream.range( 0, 5 )
            .forEach( i -> users.get( i ).setUid( events.get( i ).getAssignedUser().getUid() ) );
        IntStream.range( 0, 5 )
            .forEach( i -> users.get( i ).setUsername( events.get( i ).getAssignedUser().getUsername() ) );
    }

    @Test
    void verifyUserSupplierByUid()
    {
        final List<String> userIds = events.stream().map( Event::getAssignedUser )
            .map( User::getUid )
            .collect( Collectors.toList() );

        when( manager.getByUid( eq( org.hisp.dhis.user.User.class ),
            argThat( t -> t.containsAll( userIds ) ) ) ).thenReturn( users );

        final TrackerImportParams params = TrackerImportParams.builder()
            .events( events )
            .build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        for ( String userUid : userIds )
        {
            assertThat( preheat.getUserByUid( userUid ).orElseGet( null ), is( notNullValue() ) );
        }
    }

    @Test
    void verifyUserSupplierByUsername()
    {
        final List<String> usernames = events.stream().map( Event::getAssignedUser )
            .map( User::getUsername )
            .collect( Collectors.toList() );

        when( userService.getUsersByUsernames( argThat( t -> t.containsAll( usernames ) ) ) ).thenReturn( users );

        final TrackerImportParams params = TrackerImportParams.builder()
            .events( events )
            .build();

        TrackerPreheat preheat = new TrackerPreheat();
        this.supplier.preheatAdd( params, preheat );

        for ( String username : usernames )
        {
            assertThat( preheat.getUserByUsername( username ).orElseGet( null ), is( notNullValue() ) );
        }
    }
}