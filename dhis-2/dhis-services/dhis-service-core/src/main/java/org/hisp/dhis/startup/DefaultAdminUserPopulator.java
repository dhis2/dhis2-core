package org.hisp.dhis.startup;

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

import com.google.common.collect.ImmutableSet;
import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;

import java.util.Set;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class DefaultAdminUserPopulator
    extends TransactionContextStartupRoutine
{
    public static final Set<String> ALL_AUTHORITIES = ImmutableSet.of(
        "ALL",
        "F_VIEW_EVENT_ANALYTICS",
        "F_METADATA_EXPORT",
        "F_METADATA_IMPORT",
        "F_EXPORT_DATA",
        "F_IMPORT_DATA",
        "F_EXPORT_EVENTS",
        "F_IMPORT_EVENTS",
        "F_SKIP_DATA_IMPORT_AUDIT",
        "F_APPROVE_DATA",
        "F_APPROVE_DATA_LOWER_LEVELS",
        "F_ACCEPT_DATA_LOWER_LEVELS",
        "F_PERFORM_MAINTENANCE",
        "F_LOCALE_ADD",
        "F_GENERATE_MIN_MAX_VALUES",
        "F_RUN_VALIDATION",
        "F_PREDICTOR_RUN",
        "F_SEND_EMAIL",
        "F_ORGANISATIONUNIT_MOVE",
        "F_INSERT_CUSTOM_JS_CSS",
        "F_VIEW_UNAPPROVED_DATA",
        "F_USER_VIEW",
        "F_REPLICATE_USER",
        "F_USERGROUP_MANAGING_RELATIONSHIPS_ADD",
        "F_USERGROUP_MANAGING_RELATIONSHIPS_VIEW",
        "F_USER_GROUPS_READ_ONLY_ADD_MEMBERS",
        "F_PROGRAM_DASHBOARD_CONFIG_ADMIN",
        "F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS",
        "F_TEI_CASCADE_DELETE",
        "F_ENROLLMENT_CASCADE_DELETE",
        "F_UNCOMPLETE_EVENT",
        "F_EDIT_EXPIRED",
        "F_IGNORE_TRACKER_REQUIRED_VALUE_VALIDATION",
        "F_TRACKER_IMPORTER_EXPERIMENTAL"
    );

    private final UserService userService;

    public DefaultAdminUserPopulator( UserService userService )
    {
        checkNotNull( userService );
        this.userService = userService;
    }

    @Override
    public void executeInTransaction()
    {
        // If there is no users in the system we assume we need a default admin user.
        if ( userService.getUserCount() > 0 )
        {
            return;
        }

        // ---------------------------------------------------------------------
        // Assumes no UserAuthorityGroup called "Superuser" in database
        // ---------------------------------------------------------------------

        String username = "admin";
        String password = "district";

        User user = new User();
        user.setUid( "M5zQapPyTZI" );
        user.setCode( "admin" );
        user.setFirstName( username );
        user.setSurname( username );

        userService.addUser( user );

        UserAuthorityGroup userAuthorityGroup = new UserAuthorityGroup();
        userAuthorityGroup.setUid( "yrB6vc5Ip3r" );
        userAuthorityGroup.setCode( "Superuser" );
        userAuthorityGroup.setName( "Superuser" );
        userAuthorityGroup.setDescription( "Superuser" );

        userAuthorityGroup.setAuthorities( ALL_AUTHORITIES );

        userService.addUserAuthorityGroup( userAuthorityGroup );

        UserCredentials userCredentials = new UserCredentials();
        userCredentials.setUid( "KvMx6c1eoYo" );
        userCredentials.setUuid( UUID.fromString( "6507f586-f154-4ec1-a25e-d7aa51de5216" ) );
        userCredentials.setCode( username );
        userCredentials.setUsername( username );
        userCredentials.setUserInfo( user );
        userCredentials.getUserAuthorityGroups().add( userAuthorityGroup );

        userService.encodeAndSetPassword( userCredentials, password );
        userService.addUserCredentials( userCredentials );

        user.setUserCredentials( userCredentials );

        userService.updateUser( user );
    }
}
