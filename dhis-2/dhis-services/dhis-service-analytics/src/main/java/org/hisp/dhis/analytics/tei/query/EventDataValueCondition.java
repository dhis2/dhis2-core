/*
 * Copyright (c) 2004-2004, University of Oslo
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

import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.EVT_ALIAS;

import org.hisp.dhis.analytics.common.ValueTypeMapping;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.common.query.BinaryConditionRenderer;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.analytics.tei.query.context.QueryContext;

public class EventDataValueCondition extends AbstractCondition
{
    private final QueryContext queryContext;

    private final DimensionIdentifier<DimensionParam> dimensionIdentifier;

    private EventDataValueCondition( DimensionIdentifier<DimensionParam> dimensionIdentifier,
        QueryContext queryContext )
    {
        super( dimensionIdentifier, queryContext );
        this.queryContext = queryContext;
        this.dimensionIdentifier = dimensionIdentifier;
    }

    public static EventDataValueCondition of(
        DimensionIdentifier<DimensionParam> dimensionIdentifier,
        QueryContext queryContext )
    {
        return new EventDataValueCondition( dimensionIdentifier, queryContext );
    }

    @Override
    protected Renderable getEventCondition()
    {
        ValueTypeMapping valueTypeMapping = ValueTypeMapping
            .fromValueType( dimensionIdentifier.getDimension().getValueType() );

        DimensionParamItem item = dimensionIdentifier.getDimension().getItems().get( 0 );
        String doUid = dimensionIdentifier.getDimension().getDimensionObjectUid();

        return BinaryConditionRenderer.of(
            RenderableDataValue.of( EVT_ALIAS, doUid, valueTypeMapping ),
            item.getOperator(),
            item.getValues(),
            valueTypeMapping,
            queryContext );
    }
}
