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
package org.hisp.dhis.tracker.export.event;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.user.User;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class EventUtils
{
    private EventUtils()
    {
        throw new UnsupportedOperationException( "Utility class" );
    }

    public static UserInfoSnapshot jsonToUserInfo( String userInfoAsString, ObjectMapper mapper )
    {
        try
        {
            if ( StringUtils.isNotEmpty( userInfoAsString ) )
            {
                return mapper.readValue( userInfoAsString, UserInfoSnapshot.class );
            }
            return null;
        }
        catch ( IOException e )
        {
            log.error( "Parsing UserInfoSnapshot json string failed. String value: " + userInfoAsString );
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * Checks whether the user can access the requested org unit or, depending
     * on the case, any of its offspring
     *
     * @param program the program the user wants to access to
     * @param user the user to check the access of
     * @param orgUnitDescendants function to retrieve org units, in case ou mode
     *        is descendants
     * @return true if there's at least one accessible org unit, false otherwise
     */
    public static boolean isOrgUnitAccessible( Program program, User user, OrganisationUnit orgUnit,
        OrganisationUnitSelectionMode orgUnitMode,
        Function<String, List<OrganisationUnit>> orgUnitDescendants )
    {
        List<OrganisationUnit> orgUnits = getRequestedOrgUnits( orgUnit, orgUnitMode, orgUnitDescendants );

        if ( program != null
            && (program.isClosed() || program.isProtected()) )
        {
            return orgUnits.stream().anyMatch( ou -> user.getOrganisationUnits().contains( ou ) );
        }
        else
        {
            return orgUnits.stream().anyMatch( ou -> user.getTeiSearchOrganisationUnitsWithFallback().contains( ou ) );
        }
    }

    /**
     * Gets all the descendants/children of a particular org unit
     *
     * @param orgUnit the org unit to get the descendants or children of
     * @param orgUnitMode the org unit mode to be used to get the offspring
     * @param orgUnitDescendants function to retrieve org units, in case ou mode
     *        is descendants
     * @return a list of the offspring of the supplied org unit
     */
    public static List<OrganisationUnit> getRequestedOrgUnits( OrganisationUnit orgUnit,
        OrganisationUnitSelectionMode orgUnitMode,
        Function<String, List<OrganisationUnit>> orgUnitDescendants )
    {
        if ( orgUnitMode == null || orgUnit == null )
        {
            return Collections.emptyList();
        }

        return switch ( orgUnitMode )
        {
        case DESCENDANTS -> orgUnitDescendants.apply( orgUnit.getUid() );
        case CHILDREN -> orgUnit.getChildren().stream().toList();
        default -> Collections.emptyList();
        };
    }
}
