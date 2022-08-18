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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link GridHeaders}
 *
 * @author maikel arabori
 */
class GridHeadersTest
{
    @Test
    void testRetainHeadersOnGrid()
    {
        // Given
        final GridHeader headerA = new GridHeader( "headerA", "Header A" );
        final GridHeader headerB = new GridHeader( "headerB", "Header B" );
        final GridHeader headerC = new GridHeader( "headerC", "Header C" );

        final Grid grid = new ListGrid();
        grid.addHeader( headerA );
        grid.addHeader( headerB );
        grid.addHeader( headerC );
        grid.addRow().addValue( 1 ).addValue( "a" ).addValue( "a-1" );
        grid.addRow().addValue( 2 ).addValue( "b" ).addValue( "b-1" );
        grid.addRow().addValue( 3 ).addValue( "c" ).addValue( "c-1" );

        final Set<String> headers = new LinkedHashSet<>( List.of( "headerA", "headerB" ) );

        // When
        GridHeaders.retainHeadersOnGrid( grid, headers );

        // Then
        assertEquals( 2, grid.getHeaderWidth(), "Should have size of 2: getHeaderWidth()" );
        assertEquals( "headerA", grid.getHeaders().get( 0 ).getName(), "Should be named 'headerA': getName()" );
        assertEquals( "headerB", grid.getHeaders().get( 1 ).getName(), "Should be named 'headerB': getName()" );
    }

    @Test
    void testRetainHeadersOnGridWhenGridIsNull()
    {
        // Given
        final Set<String> headers = new LinkedHashSet<>( List.of( "headerA", "headerB" ) );

        // When
        final IllegalArgumentException thrown = assertThrows( IllegalArgumentException.class,
            () -> GridHeaders.retainHeadersOnGrid( null, headers ) );

        // Then
        assertEquals( "The 'grid' cannot be null", thrown.getMessage(), "Exception message does not match." );
    }
}
