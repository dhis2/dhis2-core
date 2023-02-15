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
package org.hisp.dhis.analytics.tei.query.context.querybuilder;

import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import lombok.Getter;

import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Table;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.analytics.tei.query.context.sql.RenderableSqlQuery;
import org.hisp.dhis.analytics.tei.query.context.sql.SqlQueryBuilder;
import org.springframework.stereotype.Service;

/**
 * this class is responsible to set the main table part of the sql
 */
@Service
public class MainTableQueryBuilder implements SqlQueryBuilder
{
    @Getter
    private final List<Predicate<DimensionIdentifier<DimensionParam>>> dimensionFilters = Collections.emptyList();

    @Getter
    private final List<Predicate<AnalyticsSortingParams>> sortingFilters = Collections.emptyList();

    @Override
    public RenderableSqlQuery buildSqlQuery( QueryContext queryContext,
        List<DimensionIdentifier<DimensionParam>> unused,
        List<AnalyticsSortingParams> unused_ )
    {
        return RenderableSqlQuery.builder()
            .mainTable( Table.ofStrings( queryContext.getMainTableName(), TEI_ALIAS ) )
            .build();
    }
}
