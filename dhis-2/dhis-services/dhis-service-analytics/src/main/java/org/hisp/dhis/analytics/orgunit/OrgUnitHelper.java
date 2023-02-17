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
package org.hisp.dhis.analytics.orgunit;

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;

import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.organisationunit.OrganisationUnit;

/**
 * Helper class for organisation unit handling
 */
public class OrgUnitHelper
{
    private OrgUnitHelper()
    {
        throw new UnsupportedOperationException( "helper" );
    }

    /**
     * Consolidate organisation unit list by intersection with grid rows data.
     * If the intersection is empty return all requested organisation units.
     *
     * @param grid response grid {@link Grid}
     * @param organisationUnits organisation unit collection
     *        {@link List<OrganisationUnit>}
     * @return organisation unit collection with units present in grid rows
     *         (data) or (no data in grid) incoming org units
     */
    public static List<OrganisationUnit> getActiveOrganisationUnits( Grid grid,
        List<OrganisationUnit> organisationUnits )
    {
        if ( grid == null || organisationUnits == null )
        {
            return organisationUnits;
        }

        int orgUnitIndex = -1;

        for ( int i = 0; i < grid.getHeaders().size(); i++ )
        {
            if ( ORGUNIT_DIM_ID.equalsIgnoreCase( grid.getHeaders().get( i ).getName() ) )
            {
                orgUnitIndex = i;
                break;
            }
        }

        if ( orgUnitIndex >= 0 )
        {
            final int i = orgUnitIndex;

            List<String> orgUidList = grid.getRows().stream().map( r -> String.valueOf( r.get( i ) ) )
                .distinct()
                .collect( Collectors.toList() );

            List<OrganisationUnit> activated = organisationUnits.stream()
                .distinct()
                .filter( org -> orgUidList.stream().anyMatch( uid -> org.getUid().equals( uid ) ) )
                .collect( Collectors.toList() );

            return activated.isEmpty() ? organisationUnits : activated;
        }

        return organisationUnits;
    }
}
