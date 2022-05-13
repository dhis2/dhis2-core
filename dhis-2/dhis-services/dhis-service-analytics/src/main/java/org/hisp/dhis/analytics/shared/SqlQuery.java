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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.wrap;
import static org.springframework.util.Assert.hasText;
import static org.springframework.util.Assert.noNullElements;
import static org.springframework.util.Assert.notEmpty;

import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * @see Query
 *
 * @author maikel arabori
 */
@Getter
@EqualsAndHashCode
@Builder
public class SqlQuery implements Query
{
    /**
     * The list of columns to be queried.
     */
    private final List<Column> columns;

    /**
     * Stores the join clause.
     *
     * Example: from programinstance pi
     */
    private final String fromClause;

    /**
     * Stores the join clause.
     *
     * Example: inner join program pr on pi.programid = pr.programid left join
     * trackedentityinstance tei on pi.trackedentityinstanceid =
     * tei.trackedentityinstanceid and tei.deleted is false
     */
    private final String joinClause;

    /**
     * Stores the where clause.
     *
     * Example: where username = 'adhanom' and donator = 'bill'
     */
    private final String whereClause;

    /**
     * Stores the last clauses in the query.
     *
     * Example: order by "name" desc limit 100 offset 0
     */
    private final String closingClauses;

    /**
     * @see Query#fullStatement()
     *
     * @throws IllegalArgumentException if columns is null/empty or contain at
     *         least one null element. If the 'from' clause is null/empty/blank.
     *         If the 'where' is null/empty/blank.
     */
    @Override
    public String fullStatement()
    {
        validate();

        return "select " + join( columns, "," ) + spaced( fromClause ) + spaced( joinClause ) + spaced( whereClause )
            + spaced( whereClause );
    }

    private void validate()
    {
        notEmpty( columns, "The 'columns' must not be null/empty" );
        noNullElements( columns, "The 'columns' must not contain null elements" );
        hasText( fromClause, "Must have a 'from' clause" );
        hasText( whereClause, "Must have a 'where' clause" );
    }

    /**
     * Adds a space character before and after of the given value. ie.: "name"
     * will become " name "
     *
     * @param value
     * @return return the spaced value, or empty string if the given value is
     *         null/empty/blank
     */
    private String spaced( final String value )
    {
        return isNotBlank( value ) ? wrap( value, SPACE ) : EMPTY;
    }
}
