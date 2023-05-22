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

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hisp.dhis.analytics.common.ValueTypeMapping.DATE;
import static org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifierHelper.getPrefix;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.common.params.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.params.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.AndCondition;
import org.hisp.dhis.analytics.common.query.BaseRenderable;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.ConstantValuesRenderer;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.tei.query.context.sql.QueryContext;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.util.DateUtils;

public class PeriodCondition extends BaseRenderable
{
    private final QueryContext queryContext;

    private final Pair<Date, Date> interval;

    private final TimeField timeField;

    private final String prefix;

    private PeriodCondition( DimensionIdentifier<DimensionParam> dimensionIdentifier,
        QueryContext queryContext )
    {
        this.queryContext = queryContext;
        this.prefix = getPrefix( dimensionIdentifier );

        Date minDate = dimensionIdentifier.getDimension().getDimensionalObject().getItems().stream()
            .map( Period.class::cast )
            .map( Period::getStartDate )
            .reduce( DateUtils::min ).orElse( null );

        Date maxDate = dimensionIdentifier.getDimension().getDimensionalObject().getItems().stream()
            .map( Period.class::cast )
            .map( Period::getEndDate )
            .map( this::nextDay )
            .reduce( DateUtils::max ).orElse( null );

        this.interval = Pair.of( minDate, maxDate );
        this.timeField = TimeField.valueOf(
            ((Period) dimensionIdentifier
                .getDimension()
                .getDimensionalObject()
                .getItems()
                .get( 0 ))
                .getDateField() );
    }

    private Date nextDay( Date date )
    {
        return Date.from( date.toInstant().plus( 1, DAYS ) );
    }

    public static PeriodCondition of( DimensionIdentifier<DimensionParam> dimensionIdentifier,
        QueryContext queryContext )
    {
        return new PeriodCondition( dimensionIdentifier, queryContext );
    }

    @Override
    public String render()
    {
        return getCondition().render();
    }

    private AndCondition getCondition()
    {
        return AndCondition.of(
            List.of(
                BinaryConditionRenderer.of(
                    Field.of( prefix, timeField::getField, EMPTY ),
                    QueryOperator.GE,
                    ConstantValuesRenderer.of(
                        getMediumDateString( interval.getLeft() ),
                        DATE, queryContext ) ),
                BinaryConditionRenderer.of(
                    Field.of( prefix, timeField::getField, EMPTY ),
                    QueryOperator.LT,
                    ConstantValuesRenderer.of(
                        getMediumDateString( interval.getRight() ),
                        DATE, queryContext ) ) ) );
    }
}
