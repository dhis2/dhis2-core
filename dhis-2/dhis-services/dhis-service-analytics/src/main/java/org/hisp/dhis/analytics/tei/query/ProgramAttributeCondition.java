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

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.dimension.DimensionParamItem;
import org.hisp.dhis.analytics.shared.ValueTypeMapping;
import org.hisp.dhis.analytics.shared.query.BaseRenderable;
import org.hisp.dhis.analytics.shared.query.OrCondition;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

@RequiredArgsConstructor( staticName = "of" )
public class ProgramAttributeCondition extends BaseRenderable
{

    private final DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier;

    private final QueryContext queryContext;

    @Override
    public String render()
    {
        List<BinaryConditionRenderer> renderers = new ArrayList<>();

        ValueTypeMapping valueTypeMapping = ValueTypeMapping
            .fromValueType( dimensionIdentifier.getDimension().getValueType() );

        for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
        {

            BinaryConditionRenderer binaryConditionRenderer = BinaryConditionRenderer.of(
                dimensionIdentifier.getDimension().getUid(),
                item.getOperator(),
                item.getValues(),
                valueTypeMapping,
                queryContext );

            renderers.add( binaryConditionRenderer );
        }

        return OrCondition.of( renderers ).render();
    }

}
