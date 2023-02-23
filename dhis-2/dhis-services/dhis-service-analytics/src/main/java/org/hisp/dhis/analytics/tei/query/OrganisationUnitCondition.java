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
package org.hisp.dhis.analytics.tei.query;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.STRING;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ENR_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.EVT_ALIAS;
import static org.hisp.dhis.common.QueryOperator.IN;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.common.query.AndCondition;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;

/**
 * Provides methods responsible for generating SQL statements on top of
 * organization units, for events, enrollments and teis.
 */
public class OrganisationUnitCondition extends AbstractCondition
{
    private static final String OU_FIELD = "ou";

    private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

    private final QueryContext queryContext;

    private OrganisationUnitCondition( DimensionIdentifier<DimensionParam> dimensionIdentifier,
        QueryContext queryContext )
    {
        super( dimensionIdentifier, queryContext );
        this.queryContext = queryContext;
        this.dimensionIdentifier = dimensionIdentifier;
    }

    public static OrganisationUnitCondition of(
        DimensionIdentifier<DimensionParam> dimensionIdentifier, QueryContext queryContext )
    {
        return new OrganisationUnitCondition( dimensionIdentifier, queryContext );
    }

    /**
     * Renders the org. unit SQL conditions for a given enrollment. The SQL
     * output will look like:
     *
     * "ou" = :1
     *
     * @return the SQL statement
     */
    @Override
    protected Renderable getTeiCondition()
    {
        List<Renderable> orgUnitConditions = new ArrayList<>();

        for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
        {
            BinaryConditionRenderer condition = BinaryConditionRenderer.of(
                Field.ofFieldName( OU_FIELD ),
                IN,
                item.getValues(),
                STRING,
                queryContext );
            orgUnitConditions.add( condition );
        }

        return AndCondition.of( orgUnitConditions );
    }

    /**
     * Renders the org. unit SQL conditions for a given enrollment. The SQL
     * output will look like:
     *
     * exists (select 1 from analytics_tei_enrollments_t2d3uj69rab enr where
     * enr.trackedentityinstanceuid = t_1.trackedentityinstanceuid and
     * enr.programuid = :1 and enr.ou in (:2) order by enr.enrollmentdate desc
     * limit 1 offset 0)
     *
     * @return the SQL statement
     */
    @Override
    protected Renderable getEnrollmentCondition()
    {
        List<Renderable> orgUnitConditions = new ArrayList<>();

        for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
        {
            BinaryConditionRenderer condition = BinaryConditionRenderer.of(
                Field.of( ENR_ALIAS, () -> OU_FIELD, EMPTY, EMPTY ),
                IN,
                item.getValues(),
                STRING,
                queryContext );
            orgUnitConditions.add( condition );
        }

        return AndCondition.of( orgUnitConditions );
    }

    /**
     * Renders the org. unit SQL conditions for a given event. The SQL output
     * will look like:
     *
     * exists (select 1 from analytics_tei_enrollments_t2d3uj69rab enr where
     * enr.trackedentityinstanceuid = t_1.trackedentityinstanceuid and
     * enr.programuid = :1 and exists (select 1 from
     * analytics_tei_events_t2d3uj69rab evt where evt.programinstanceuid =
     * enr.programinstanceuid and evt.programstageuid = :2 and enr.ou in (:3)
     * order by executiondate desc limit 1 offset 0) order by enr.enrollmentdate
     * desc limit 1 offset 0)
     *
     * @return the SQL statement
     */
    @Override
    protected Renderable getEventCondition()
    {
        List<Renderable> orgUnitConditions = new ArrayList<>();

        for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
        {
            BinaryConditionRenderer condition = BinaryConditionRenderer.of(
                Field.of( EVT_ALIAS, () -> OU_FIELD, EMPTY, EMPTY ).render(),
                IN,
                item.getValues(),
                STRING,
                queryContext );
            orgUnitConditions.add( condition );
        }

        return AndCondition.of( orgUnitConditions );
    }
}
