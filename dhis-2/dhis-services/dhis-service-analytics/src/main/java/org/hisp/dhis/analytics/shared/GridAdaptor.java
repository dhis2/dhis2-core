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
package org.hisp.dhis.analytics.shared;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.util.Assert.noNullElements;
import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.system.grid.ListGrid;
import org.springframework.stereotype.Component;

/**
 * Component that provides operations for generation or manipulation of Grid
 * objects. It basically encapsulates any required Grid logic that is not
 * supported by the Grid itself.
 *
 * @author maikel arabori
 */
@Component
public class GridAdaptor
{

    /**
     * /** Based on the given headers and result map, this method takes care of
     * the logic needed to create a valid Grid object.
     *
     * @param headers
     * @param resultMap
     * @return the Grid object
     *
     * @throws IllegalArgumentException if headers is null/empty or contain at
     *         least one null element, or if the queryResult is null
     */
    public Grid createGrid( final List<GridHeader> headers, final Map<Column, List<Object>> resultMap )
    {
        notEmpty( headers, "The 'headers' must not be null/empty" );
        noNullElements( headers, "The 'headers' must not contain null elements" );
        notEmpty( resultMap, "The 'resultMap' must not be null/empty" );
        notNull( resultMap, "The 'queryResult' must not be null" );

        final Grid grid = new ListGrid();

        boolean rowsAdded = false;

        for ( final GridHeader header : headers )
        {
            final List<Object> columnRows = resultMap.get( Column.builder().alias( header.getName() ).build() );

            if ( !rowsAdded && isNotEmpty( columnRows ) )
            {
                columnRows.forEach( c -> grid.addRow() );
                rowsAdded = true;
            }

            // Note that the header column must match the result map key.
            grid.addHeader( header ).addColumn( columnRows );
        }

        return grid;
    }
}
