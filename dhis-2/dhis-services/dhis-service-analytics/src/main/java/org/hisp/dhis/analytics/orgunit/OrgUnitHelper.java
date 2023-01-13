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
    }

    /**
     * Consolidate organisation unit list by intersection with grid rows data
     *
     * @param grid
     * @param organisationUnits
     * @return
     */
    public static List<OrganisationUnit> getGridRelevantOrganisationUnits( Grid grid,
        List<OrganisationUnit> organisationUnits )
    {
        if ( grid == null )
        {
            return organisationUnits;
        }

        int index = -1;

        for ( int i = 0; i < grid.getHeaders().size(); i++ )
        {
            if ( ORGUNIT_DIM_ID.equalsIgnoreCase( grid.getHeaders().get( i ).getName() ) )
            {
                index = i;
                break;
            }
        }

        if ( index >= 0 )
        {
            final int i = index;

            List<String> orgUidList = grid.getRows().stream().map( r -> String.valueOf( r.get( i ) ) )
                .distinct()
                .collect( Collectors.toList() );

            return organisationUnits.stream()
                .distinct()
                .filter( org -> orgUidList.stream().anyMatch( uid -> org.getUid().equals( uid ) ) )
                .collect( Collectors.toList() );
        }

        return organisationUnits;
    }

    public static boolean isOrganisationUnitInGridRows( OrganisationUnit organisationUnit, Grid grid )
    {
        if ( grid == null )
        {
            return false;
        }

        int index = -1;

        for ( int i = 0; i < grid.getHeaders().size(); i++ )
        {
            if ( ORGUNIT_DIM_ID.equalsIgnoreCase( grid.getHeaders().get( i ).getName() ) )
            {
                index = i;
                break;
            }
        }

        if ( index >= 0 )
        {
            final int i = index;

            return grid.getRows().stream()
                .anyMatch( r -> String.valueOf( r.get( i ) ).equalsIgnoreCase( organisationUnit.getUid() ) );
        }

        return false;
    }
}
