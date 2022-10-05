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
import org.hisp.dhis.analytics.shared.query.AndCondition;
import org.hisp.dhis.analytics.shared.query.BaseRenderable;
import org.hisp.dhis.analytics.shared.query.Renderable;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

@RequiredArgsConstructor( staticName = "of" )
public class OrganisationUnitCondition extends BaseRenderable
{
    private static final String OU_FIELD = "ou";

    private final DimensionIdentifier<Program, ProgramStage, DimensionParam> dimensionIdentifier;

    private final QueryContext queryContext;

    @Override
    public String render()
    {
        if ( dimensionIdentifier.hasProgram() && dimensionIdentifier.hasProgramStage() )
        {
            // TODO: Event OU filter DHIS2-13781
            return "1 = 1 /* EVENT OU FILTER PENDING IMPLEMENTATION */";
        }
        if ( dimensionIdentifier.hasProgram() && !dimensionIdentifier.hasProgramStage() )
        {
            // TODO: Enrollment OU filter DHIS2-13863
            return "1 = 1 /* ENROLLMENT OU FILTER PENDING IMPLEMENTATION */";
        }
        if ( !dimensionIdentifier.hasProgram() && !dimensionIdentifier.hasProgramStage() )
        {
            List<Renderable> orgUnitConditions = new ArrayList<>();
            for ( DimensionParamItem item : dimensionIdentifier.getDimension().getItems() )
            {
                BinaryConditionRenderer condition = BinaryConditionRenderer.of(
                    OU_FIELD,
                    QueryOperator.IN,
                    item.getValues(),
                    ValueTypeMapping.STRING,
                    queryContext );
                orgUnitConditions.add( condition );
            }
            return AndCondition.of( orgUnitConditions ).render();
        }
        return "";
    }
}
