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

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.analytics.shared.Column;
import org.hisp.dhis.analytics.shared.Query;
import org.hisp.dhis.analytics.shared.QueryGenerator;
import org.hisp.dhis.analytics.shared.SqlQuery;
import org.hisp.dhis.analytics.shared.component.ColumnComponent;
import org.hisp.dhis.analytics.shared.component.PredicateComponent;
import org.hisp.dhis.analytics.shared.component.TableComponent;
import org.hisp.dhis.analytics.shared.component.builder.FromClauseComponentBuilder;
import org.hisp.dhis.analytics.shared.component.builder.SelectClauseComponentBuilder;
import org.hisp.dhis.analytics.shared.component.builder.WhereClauseComponentBuilder;
import org.hisp.dhis.analytics.shared.component.element.Element;
import org.hisp.dhis.analytics.shared.visitor.FromClauseElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.FromElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.SelectClauseElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.SelectElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.WhereClauseElementVisitor;
import org.hisp.dhis.analytics.shared.visitor.WhereElementVisitor;
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
            .columns( getColumns( teiParams ) )
            .fromClause( getFromClause( teiParams ) )
            .joinClause( getJoinClause( teiParams ) )
            .whereClause( getWhereClause( teiParams ) )
            .closingClauses( getClosingClause( teiParams ) )
            .build();
    }

    private String getClosingClause( TeiParams teiParams )
    {
        return "";
    }

    private String getWhereClause( TeiParams teiParams )
    {
        List<Element<WhereElementVisitor>> elements = WhereClauseComponentBuilder.builder().withTeiParams( teiParams )
            .build();

        final Element<WhereElementVisitor> predicateComponent = new PredicateComponent( elements );

        List<String> predicates = new ArrayList<>();

        WhereElementVisitor whereElementVisitor = new WhereClauseElementVisitor( predicates );

        predicateComponent.accept( whereElementVisitor );

        return String.join( " AND ", predicates );
    }

    private String getJoinClause( TeiParams teiParams )
    {
        return "";
    }

    private String getFromClause( TeiParams teiParams )
    {
        List<Element<FromElementVisitor>> elements = FromClauseComponentBuilder.builder().withTeiParams( teiParams )
            .build();

        final Element<FromElementVisitor> tableComponent = new TableComponent( elements );

        List<String> tables = new ArrayList<>();

        FromElementVisitor fromElementVisitor = new FromClauseElementVisitor( tables );

        tableComponent.accept( fromElementVisitor );

        return String.join( ",", tables );
    }

    private List<Column> getColumns( TeiParams teiParams )
    {
        List<Element<SelectElementVisitor>> elements = SelectClauseComponentBuilder.builder().withTeiParams( teiParams )
            .build();

        final Element<SelectElementVisitor> columnComponent = new ColumnComponent( elements );

        List<Column> columns = new ArrayList<>();

        SelectElementVisitor columnVisitor = new SelectClauseElementVisitor( columns );

        columnComponent.accept( columnVisitor );

        return columns;
    }
}
