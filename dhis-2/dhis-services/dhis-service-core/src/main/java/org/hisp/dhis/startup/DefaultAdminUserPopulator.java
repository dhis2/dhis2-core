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
package org.hisp.dhis.startup;

import static org.hisp.dhis.user.DefaultUserService.TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME;

import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.system.startup.TransactionContextStartupRoutine;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@RequiredArgsConstructor
public class DefaultAdminUserPopulator
    extends TransactionContextStartupRoutine
{
    /**
     * Authorities which are not part of schema descriptors/associated with
     * metadata CRUD operations.
     */
    public static final Set<String> ALL_AUTHORITIES = Set.of(
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
        "F_PERFORM_ANALYTICS_EXPLAIN",
        "F_LOCALE_ADD",
        "F_GENERATE_MIN_MAX_VALUES",
        "F_RUN_VALIDATION",
        "F_PREDICTOR_RUN",
        "F_SEND_EMAIL",
        "F_ORGANISATIONUNIT_MOVE",
        "F_ORGANISATION_UNIT_SPLIT",
        "F_ORGANISATION_UNIT_MERGE",
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
        "F_TRACKER_IMPORTER_EXPERIMENTAL",
        "F_VIEW_SERVER_INFO",
        "F_ORG_UNIT_PROFILE_ADD",
        "F_TRACKED_ENTITY_MERGE",
        "F_DATAVALUE_ADD",
        "F_DATAVALUE_DELETE" );

    public static final Set<String> ALL_RESTRICTIONS = Set.of( TWO_FACTOR_AUTH_REQUIRED_RESTRICTION_NAME );

    private final UserService userService;

    private final TransactionTemplate transactionTemplate;

    @Override
    protected TransactionTemplate getTransactionTemplate()
    {
        return this.transactionTemplate;
    }

    @Override
    public void executeInTransaction()
    {
        // If there is no users in the system we assume we need a default admin
        // user.
        if ( userService.getUserCount() > 0 )
        {
            return;
        }

        // ---------------------------------------------------------------------
        // Assumes no UserRole called "Superuser" in database
        // ---------------------------------------------------------------------

        String username = "admin";
        String password = "district";

        User user = new User();
        user.setUid( "M5zQapPyTZI" );
        user.setUuid( UUID.fromString( "6507f586-f154-4ec1-a25e-d7aa51de5216" ) );
        user.setUsername( username );
        user.setCode( username );
        user.setFirstName( username );
        user.setSurname( username );

        userService.addUser( user );

        UserRole userRole = new UserRole();
        userRole.setUid( "yrB6vc5Ip3r" );
        userRole.setCode( "Superuser" );
        userRole.setName( "Superuser" );
        userRole.setDescription( "Superuser" );
        userRole.setAuthorities( ALL_AUTHORITIES );

        userService.addUserRole( userRole );

        user.getUserRoles().add( userRole );

        userService.encodeAndSetPassword( user, password );

        userService.addUser( user );

        UserRole twoFactorRole = new UserRole();
        twoFactorRole.setUid( "jcK4oq1Ol8x" );
        twoFactorRole.setCode( "TwoFactor" );
        twoFactorRole.setName( "TwoFactor" );
        twoFactorRole.setDescription( "TwoFactor" );
        twoFactorRole.setRestrictions( ALL_RESTRICTIONS );
    }
}
