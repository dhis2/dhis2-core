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
package org.hisp.dhis.analytics.common.processing;

import static org.hisp.dhis.analytics.common.query.Field.of;
import static org.hisp.dhis.analytics.common.query.Field.ofUnquoted;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.analytics.common.params.CommonParams;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.TeiQueryParams;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.system.grid.ListGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link HeaderParamsHandler}.
 *
 * @author maikel arabori
 */
@ExtendWith( MockitoExtension.class )
class HeaderParamsHandlerTest
{
    private HeaderParamsHandler headerParamsHandler;

    @BeforeEach
    void setUp()
    {
        headerParamsHandler = new HeaderParamsHandler();
    }

    @Test
    void testHandleWithOneFieldColumn()
    {
        // Given
        String column = "oucode";
        Grid grid = new ListGrid();
        TeiQueryParams teiQueryParams = TeiQueryParams.builder().commonParams( CommonParams.builder().build() ).build();
        List<Field> fields = List.of( ofUnquoted( "ev", of( "anyName" ), column ) );

        // When
        headerParamsHandler.handle( grid, teiQueryParams, fields );

        // Then
        assertEquals( 1, grid.getHeaders().size() );
        assertEquals( column, grid.getHeaders().get( 0 ).getName() );
    }

    @Test
    void testHandleWithNoFieldColumn()
    {
        // Given
        Grid grid = new ListGrid();
        TeiQueryParams teiQueryParams = TeiQueryParams.builder().commonParams( CommonParams.builder().build() ).build();
        List<Field> fields = List.of();

        // When
        headerParamsHandler.handle( grid, teiQueryParams, fields );

        // Then
        assertTrue( grid.getHeaders().isEmpty() );
    }

    @Test
    void testHandleWithParamHeaders()
    {
        // Given
        Grid grid = new ListGrid();
        TeiQueryParams teiQueryParams = TeiQueryParams.builder().commonParams( stubCommonParamsWithHeaders() ).build();
        List<Field> fields = List.of(
            ofUnquoted( "ev", of( "anyName" ), "oucode" ),
            ofUnquoted( "ev", of( "anyName" ), "ouname" ),
            ofUnquoted( "ev", of( "anyName" ), "lastupdated" ) );

        // When
        headerParamsHandler.handle( grid, teiQueryParams, fields );

        // Then
        assertEquals( 3, grid.getHeaders().size() );
        assertEquals( "oucode", grid.getHeaders().get( 0 ).getName() );
        assertEquals( "ouname", grid.getHeaders().get( 1 ).getName() );
        assertEquals( "lastupdated", grid.getHeaders().get( 2 ).getName() );
    }

    @Test
    void testHandleWithNonExistingParamHeader()
    {
        // Given
        CommonParams commonParams = stubCommonParamsWithHeaders();
        commonParams.getHeaders().add( "non-existing" );

        Grid grid = new ListGrid();
        TeiQueryParams teiQueryParams = TeiQueryParams.builder().commonParams( commonParams ).build();
        List<Field> fields = List.of(
            ofUnquoted( "ev", of( "anyName" ), "oucode" ),
            ofUnquoted( "ev", of( "anyName" ), "ouname" ),
            ofUnquoted( "ev", of( "anyName" ), "lastupdated" ) );

        // When
        IllegalQueryException ex = assertThrows( IllegalQueryException.class,
            () -> headerParamsHandler.handle( grid, teiQueryParams, fields ),
            "Expected exception not thrown: handle()" );

        // Then
        assertTrue( ex.getMessage().contains( "Header param `non-existing` does not exist" ) );
    }

    private CommonParams stubCommonParamsWithHeaders()
    {
        Set<String> headers = new LinkedHashSet<>( List.of( "oucode", "ouname", "lastupdated" ) );

        return CommonParams.builder().headers( headers ).build();
    }
}
