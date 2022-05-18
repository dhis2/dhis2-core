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
package org.hisp.dhis.analytics.tei;

import static org.springframework.util.Assert.notNull;

import java.util.List;

import org.hisp.dhis.analytics.shared.Column;
import org.hisp.dhis.analytics.shared.Query;
import org.hisp.dhis.analytics.shared.QueryGenerator;
import org.hisp.dhis.analytics.shared.SqlQuery;
import org.hisp.dhis.analytics.shared.component.FromComponent;
import org.hisp.dhis.analytics.shared.component.SelectComponent;
import org.hisp.dhis.analytics.shared.component.WhereComponent;
import org.hisp.dhis.analytics.shared.component.builder.FromComponentBuilder;
import org.hisp.dhis.analytics.shared.component.builder.SelectComponentBuilder;
import org.hisp.dhis.analytics.shared.component.builder.WhereComponentBuilder;
import org.hisp.dhis.analytics.shared.visitor.from.FromElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.from.FromVisitor;
import org.hisp.dhis.analytics.shared.visitor.select.SelectElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.select.SelectVisitor;
import org.hisp.dhis.analytics.shared.visitor.where.WhereElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.where.WhereVisitor;
import org.springframework.stereotype.Component;

/**
 * @see QueryGenerator
 *
 * @author maikel arabori
 */
@Component
public class TeiJdbcQuery implements QueryGenerator<TeiParams>
{
    /**
     * @see QueryGenerator#from(Object)
     *
     * @param teiParams
     * @return the built Query object
     * @throws IllegalArgumentException if the given teiParams is null
     */
    @Override
    public Query from( final TeiParams teiParams )
    {
        notNull( teiParams, "The 'teiParams' must not be null" );

        // TODO: build objects below from the teiParams.
        // Probably merge Giuseppe's work in
        // https://github.com/dhis2/dhis2-core/pull/10503/files

        return SqlQuery
            .builder()
            .columns( getSelectClause( teiParams ) )
            .fromClause( " FROM " + getFromClause( teiParams ) )
            .joinClause( getJoinClause( teiParams ) )
            .whereClause( " WHERE" + getWhereClause( teiParams ) )
            .closingClauses( getClosingClause( teiParams ) )
            .build();
    }

    private String getClosingClause( TeiParams teiParams )
    {
        return "";
    }

    private String getWhereClause( TeiParams teiParams )
    {
        WhereComponent component = WhereComponentBuilder.builder().withTeiParams( teiParams )
            .build();

        WhereVisitor whereVisitor = new WhereElementVisitor();

        component.accept( whereVisitor );

        return String.join( " AND ", whereVisitor.getPredicates() );
    }

    private String getJoinClause( TeiParams teiParams )
    {
        return "";
    }

    private String getFromClause( TeiParams teiParams )
    {
        FromComponent component = FromComponentBuilder.builder().withTeiParams( teiParams )
            .build();

        FromVisitor fromVisitor = new FromElementVisitor();

        component.accept( fromVisitor );

        return String.join( ",", fromVisitor.getTables() );
    }

    private List<Column> getSelectClause( TeiParams teiParams )
    {
        SelectComponent component = SelectComponentBuilder.builder().withTeiParams( teiParams )
            .build();

        SelectVisitor columnVisitor = new SelectElementVisitor();

        component.accept( columnVisitor );

        return columnVisitor.getColumns();
    }
}
