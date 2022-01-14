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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Attribute;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.DetachUtils;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.mappers.UserMapper;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

/**
 * @author Abyot Asalefew Gizaw <abyota@gmail.com>
 *
 */
@RequiredArgsConstructor
@Component
public class UsernameValueTypeSupplier extends AbstractPreheatSupplier
{

    @NonNull
    private final UserService userService;

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {
        List<TrackedEntityAttribute> attributes = preheat.getAll( TrackedEntityAttribute.class );

        List<String> usernameAttributes = attributes.stream()
            .filter( at -> at.getValueType() == ValueType.USERNAME )
            .map( BaseIdentifiableObject::getUid )
            .collect( Collectors.toList() );

        List<String> usernames = new ArrayList<>();

        params.getTrackedEntities()
            .forEach( te -> collectResourceIds( usernameAttributes, usernames, te.getAttributes() ) );
        params.getEnrollments()
            .forEach( en -> collectResourceIds( usernameAttributes, usernames, en.getAttributes() ) );
        List<String> usernamesFromEvents = params.getEvents().stream()
            .map( Event::getCreatedBy )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );
        usernames.addAll( usernamesFromEvents );

        List<User> users = userService.getUserCredentialsByUsernames( usernames )
            .stream()
            .map( UserCredentials::getUser )
            .collect( Collectors.toList() );

        Map<String, User> validUsers = DetachUtils.detach( UserMapper.INSTANCE, users ).stream()
            .collect( Collectors.toMap( User::getUsername, Function.identity() ) );

        preheat.setUsers( validUsers );
    }

    private void collectResourceIds( List<String> usernameAttributes, List<String> usernames,
        List<Attribute> attributes )
    {
        attributes.forEach( at -> {
            if ( usernameAttributes.contains( at.getAttribute() ) && !StringUtils.isEmpty( at.getValue() ) )
            {
                usernames.add( at.getValue() );
            }
        } );
    }
}
