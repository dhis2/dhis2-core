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
 * @see GridAdaptor
 *
 * @author maikel arabori
 */
@Component
public class JdbcGridAdaptor implements GridAdaptor<Map<Column, List<Object>>>
{
    /**
     * @see GridAdaptor#createGrid(List, QueryResult)
     *
     * @throws IllegalArgumentException if headers is null/empty or contain at
     *         least one null element, or if the queryResult is null
     */
    @Override
    public Grid createGrid( final List<GridHeader> headers, final QueryResult<Map<Column, List<Object>>> queryResult )
    {
        notEmpty( headers, "The 'headers' must not be null/empty" );
        noNullElements( headers, "The 'headers' must not contain null elements" );
        notNull( queryResult, "The 'queryResult' must not be null" );

        final Grid grid = new ListGrid();

        if ( !queryResult.isEmpty() )
        {
            for ( final GridHeader header : headers )
            {
                // Note that the header column must match the result map key.
                grid.addHeader( header ).addColumn( queryResult.result().get( header.getColumn() ) );
            }
        }

        return grid;
    }
}
