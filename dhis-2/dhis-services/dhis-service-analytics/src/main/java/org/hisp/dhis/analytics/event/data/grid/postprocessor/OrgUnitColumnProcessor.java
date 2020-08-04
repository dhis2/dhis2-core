package org.hisp.dhis.analytics.event.data.grid.postprocessor;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.springframework.stereotype.Component;

/**
 * @author Luciano Fiandesio
 */
@Component
public class OrgUnitColumnProcessor implements ColumnProcessor
{
    private final OrganisationUnitStore organisationUnitStore;

    public OrgUnitColumnProcessor( OrganisationUnitStore organisationUnitStore )
    {
        checkNotNull( organisationUnitStore );

        this.organisationUnitStore = organisationUnitStore;
    }

    @Override
    public List<List<Object>> process( List<GridHeader> headers, List<List<Object>> rows )
    {
        // based on the Grid headers, fetch the Grid's indexes for the ORGANISATION_UNIT type
        int[] indexes = getGridIndexesByType( headers, ValueType.ORGANISATION_UNIT );

        if ( indexes.length > 0 )
        {
            // Collect all the Org Units UIDs.
            Map<Integer, Map<Integer, String>> orgUnitUid = collectUids( indexes, rows );

            if ( !orgUnitUid.isEmpty() )
            {
                // Map the Org Units UID to the Org Unit names
                Map<String, String> orgUnitUidNameMap = organisationUnitStore.getOrganisationUnitUidNameMap(
                    orgUnitUid.values().stream().flatMap( m -> m.values().stream() ).collect( Collectors.toSet() ) );

                // Only loop on the rows which contain Org Unit Uids
                for ( Integer index : orgUnitUid.keySet() )
                {
                    final List<Object> row = rows.get( index );
                    // Get the UIDs
                    final Map<Integer, String> orgUnitUids = orgUnitUid.get( index );
                    for ( Integer rowIndex : orgUnitUids.keySet() )
                    {
                        row.set( rowIndex, orgUnitUidNameMap.get( row.get( rowIndex ) ) );
                    }
                }
            }
        }

        return rows;
    }

    /**
     * Get the Org Unit UID for each row of the grid
     * 
     * The returned Map has:
     * 
     * - the row number as key
     * 
     * - a Map where the key is the row index and the value is the actual UID
     */
    private Map<Integer, Map<Integer, String>> collectUids( int[] indexes, List<List<Object>> rows )
    {
        Map<Integer, Map<Integer, String>> orgUnitUid = new HashMap<>();
        for ( int i = 0; i < rows.size(); i++ )
        {
            Map<Integer, String> uids = getUidFromRow( indexes, rows.get( i ) );
            if ( !uids.isEmpty() )
            {
                orgUnitUid.put( i, uids );
            }

        }
        return orgUnitUid;
    }

    /**
     *
     */
    private Map<Integer, String> getUidFromRow( int[] indexes, List<Object> row )
    {
        Map<Integer, String> indexToUidMap = new HashMap<>( indexes.length );
        for ( int index : indexes )
        {
            // TODO check if it's a String to avoid classcastexc?
            final String orgUnitUid = (String) row.get( index );
            if ( StringUtils.isNotEmpty( orgUnitUid ) )
            {
                indexToUidMap.put( index, orgUnitUid );
            }
        }
        return indexToUidMap;
    }

    private int[] removeElements( int[] arr, int key )
    {
        // Move all other elements to beginning
        int index = 0;
        for ( int i = 0; i < arr.length; i++ )
            if ( arr[i] != key )
                arr[index++] = arr[i];

        // Create a copy of arr[]
        return Arrays.copyOf( arr, index );
    }

    private int[] getGridIndexesByType( List<GridHeader> headers, ValueType valueType )
    {
        int[] indexes = new int[headers.size()];

        // Get index by type
        for ( int i = 0; i < headers.size(); i++ )
        {
            if ( headers.get( i ).getValueType().equals( valueType ) )
            {
                indexes[i] = i;
            }
            else
            {
                indexes[i] = -1;
            }
        }

        return removeElements( indexes, -1 );
    }
}
