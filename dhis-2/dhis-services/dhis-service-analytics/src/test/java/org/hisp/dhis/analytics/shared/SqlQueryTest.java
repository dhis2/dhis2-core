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

import static org.hisp.dhis.analytics.ColumnDataType.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * // TODO: Improve unit tests and coverage
 *
 * Unit tests for {@link SqlQuery}
 *
 * @author maikel arabori
 */
class SqlQueryTest
{
    @Test
    void testFullStatementSuccessfully()
    {
        // Given
        final List<Column> columns = List.of( mockColumn( "a" ), mockColumn( "b" ) );
        final String from = "from table programstageinstance psi";
        final String join = "inner join programinstance pi on psi.programinstanceid = pi.programinstanceid";
        final String where = "where psi.status in ('COMPLETED', 'ACTIVE', 'SCHEDULE')";
        final String closing = "order by a_alias desc limit 101 offset 0";

        final SqlQuery sqlQuery = SqlQuery.builder().columns( columns ).fromClause( from ).joinClause( join )
            .whereClause( where ).closingClauses( closing ).build();

        // When
        final String fullStatement = sqlQuery.fullStatement();

        // Then
        assertEquals(
            "select a_alias,b_alias from table programstageinstance psi  "
                + "inner join programinstance pi on psi.programinstanceid = pi.programinstanceid  "
                + "where psi.status in ('COMPLETED', 'ACTIVE', 'SCHEDULE')  "
                + "order by a_alias desc limit 101 offset 0 ",
            fullStatement );
    }

    private final Column mockColumn( final String prefix )
    {
        return Column.builder().dataType( TEXT ).alias( prefix + "_alias" ).hidden( false ).meta( false )
            .name( prefix + "_name" ).build();
    }
}