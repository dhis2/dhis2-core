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

import static java.util.Collections.emptyList;
import static lombok.AccessLevel.PACKAGE;
import static org.hisp.dhis.analytics.common.dimension.DimensionIdentifier.ElementWithOffset.emptyElementWithOffset;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.DATA_ELEMENT;
import static org.hisp.dhis.analytics.common.dimension.DimensionParamObjectType.PROGRAM_INDICATOR;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.commons.util.TextUtils.EMPTY;
import static org.hisp.dhis.commons.util.TextUtils.SPACE;
import static org.hisp.dhis.feedback.ErrorCode.E2037;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.AnalyticsSortingParams;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.ErrorMessage;
import org.hisp.dhis.trackedentity.TrackedEntityType;

@Builder( access = PACKAGE, builderClassName = "PrivateBuilder", toBuilder = true )
public class SortingContext
{
    private final static PrivateBuilder EMPTY_CONTEXT_BUILDER = SortingContext.builder()
        .fields( emptyList() )
        .orders( emptyList() )
        .leftJoins( emptyList() );

    @Getter
    @Singular
    private final List<Field> fields;

    @Getter
    @Singular
    private final List<Pair<Renderable, Renderable>> leftJoins;

    @Getter
    @Singular
    private final List<Renderable> orders;

    @RequiredArgsConstructor( staticName = "of" )
    public static class SortingContextBuilder
    {
        private final List<AnalyticsSortingParams> params;

        private final TrackedEntityType trackedEntityType;

        private final QueryContext.ParameterManager parameterManager;

        private final AtomicInteger counter = new AtomicInteger( 0 );

        public SortingContext build()
        {
            SortingContext.PrivateBuilder builder = SortingContext.builder();

            for ( AnalyticsSortingParams param : params )
            {
                switch ( param.getOrderBy().getDimensionIdentifierType() )
                {
                case TEI:
                    builder = enrichWithTeiDimension( builder, param );
                    break;
                case ENROLLMENT:
                    builder = enrichWithEnrollmentDimension( builder, param );
                    break;
                case EVENT:
                    builder = enrichWithEventDimension( builder, param );
                    break;
                default:
                    throw new IllegalArgumentException(
                        "Unsupported dimension identifier type: " + param.getOrderBy().getDimensionIdentifierType() );
                }
            }

            return builder.build();
        }

        static PrivateBuilder mergeContexts( PrivateBuilder builder, PrivateBuilder second )
        {
            second.fields.forEach( builder::field );
            second.leftJoins.forEach( builder::leftJoin );
            second.orders.forEach( builder::order );

            return builder;
        }

        private PrivateBuilder enrichWithEventDimension( PrivateBuilder builder, AnalyticsSortingParams param )
        {
            DimensionParam dimension = param.getOrderBy().getDimension();
            // Here we can assume that param is in the form asc=pUid.psUid.dimension (or desc=pUid.psUid.dimension)
            if ( dimension.isStaticDimension() )
            {
                return mergeContexts( builder,
                    StaticEventSortingContext
                        .of( param, counter.getAndIncrement(), trackedEntityType, parameterManager )
                        .getSortingContextBuilder() );
            } // it is either data element
            else if ( dimension.isOfType( DATA_ELEMENT ) )
            {
                return mergeContexts( builder,
                    EventDataValuesSortingContext
                        .of( param, counter.getAndIncrement(), trackedEntityType, parameterManager )
                        .getSortingContextBuilder() );
            } // or Program Indicator
            else if ( dimension.isOfType( PROGRAM_INDICATOR ) )
            {
                // support for order by Program Indicators is provided in ProgramIndicatorContext
                return mergeContexts( builder, EMPTY_CONTEXT_BUILDER );
            }
            else
            {
                throw new IllegalQueryException( new ErrorMessage( E2037, dimension
                    .getDimensionParamObjectType() ) );
            }
        }

        private PrivateBuilder enrichWithEnrollmentDimension( PrivateBuilder builder, AnalyticsSortingParams param )
        {
            // Here, we can assume that param is either a ProgramAttribute or a
            // static dimension in the form asc=pUid.dimension (or desc=pUid.dimension).
            if ( param.getOrderBy().getDimension().isStaticDimension() )
            {
                return mergeContexts( builder, StaticEnrollmentSortingContext
                    .of( param, counter.getAndIncrement(), trackedEntityType, parameterManager )
                    .getSortingContextBuilder() );
            } // It should be a ProgramAttribute then.
            AnalyticsSortingParams teiParam = AnalyticsSortingParams.builder()
                .sortDirection( param.getSortDirection() )
                .orderBy( DimensionIdentifier.of( emptyElementWithOffset(), emptyElementWithOffset(),
                    param.getOrderBy().getDimension() ) )
                .build();

            return enrichWithTeiDimension( builder, teiParam );
        }

        private PrivateBuilder enrichWithTeiDimension( PrivateBuilder builder, AnalyticsSortingParams param )
        {
            // Here we can assume that param is either a static dimension or
            // a TEI/Program attribute in the form asc=pUid.dimension (or desc=pUid.dimension)
            String column = doubleQuote( param.getOrderBy().getDimension().getUid() );

            return builder.order( () -> Field.ofUnquotedField( TEI_ALIAS,
                () -> column + SPACE + param.getSortDirection().name(), EMPTY ).render() );
        }
    }
}
