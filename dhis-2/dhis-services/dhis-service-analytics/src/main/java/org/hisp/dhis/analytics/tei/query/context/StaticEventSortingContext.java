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
package org.hisp.dhis.analytics.tei.query.context;

import static org.hisp.dhis.analytics.common.query.BinaryConditionRenderer.fieldsEqual;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.ENR_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.PI_UID;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;
import static org.hisp.dhis.analytics.tei.query.context.ContextUtils.enrollmentSelect;
import static org.hisp.dhis.analytics.tei.query.context.ContextUtils.eventSelect;
import static org.hisp.dhis.commons.util.TextUtils.SPACE;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.RenderableDimensionIdentifier;
import org.hisp.dhis.trackedentity.TrackedEntityType;

@RequiredArgsConstructor( staticName = "of" )
class StaticEventSortingContext
{
    private final AnalyticsSortingParams param;

    private final int sequence;

    private final TrackedEntityType trackedEntityType;

    private final QueryContext.ParameterManager parameterManager;

    public SortingContext.PrivateBuilder getSortingContextBuilder()
    {
        // For example asc=pUid.psUid.ouname
        DimensionIdentifier<DimensionParam> di = param.getOrderBy();
        DimensionParam sortingDimension = di.getDimension();
        String uniqueAlias = doubleQuote( sortingDimension.getUid() + "_" + sequence );
        String enrollmentAlias = ENR_ALIAS + "_" + sequence;
        String render = doubleQuote( RenderableDimensionIdentifier.of( di ).render() );

        return SortingContext.builder()
            .field( Field.of( uniqueAlias, sortingDimension::getUid, render ) )
            .order( () -> render + SPACE + param.getSortDirection().name() )
            .leftJoin( Pair.of(
                () -> "(" + enrollmentSelect( di.getProgram(), trackedEntityType, parameterManager ) + ") "
                    + enrollmentAlias,
                fieldsEqual( TEI_ALIAS, TEI_UID, enrollmentAlias, TEI_UID ) ) )
            .leftJoin( Pair.of(
                () -> "("
                    + eventSelect( di.getProgram(), di.getProgramStage(), trackedEntityType, parameterManager )
                    + ") "
                    + uniqueAlias,
                fieldsEqual( enrollmentAlias, PI_UID, uniqueAlias, PI_UID ) ) );
    }
}
