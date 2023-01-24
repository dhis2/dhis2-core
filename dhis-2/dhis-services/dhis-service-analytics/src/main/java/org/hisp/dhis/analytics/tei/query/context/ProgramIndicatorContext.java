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

import static lombok.AccessLevel.PACKAGE;
import static org.hisp.dhis.analytics.common.query.BinaryConditionRenderer.fieldsEqual;
import static org.hisp.dhis.analytics.common.query.QuotingUtils.doubleQuote;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_ALIAS;
import static org.hisp.dhis.analytics.tei.query.QueryContextConstants.TEI_UID;
import static org.hisp.dhis.analytics.tei.query.context.SortingContextUtils.enrollmentSelect;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Singular;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.analytics.common.dimension.DimensionIdentifier;
import org.hisp.dhis.analytics.common.dimension.DimensionParam;
import org.hisp.dhis.analytics.common.query.Field;
import org.hisp.dhis.analytics.common.query.Renderable;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.trackedentity.TrackedEntityType;

@Builder( access = PACKAGE, builderClassName = "PrivateBuilder", toBuilder = true )
public class ProgramIndicatorContext
{
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
    public static class ProgramIndicatorContextBuilder
    {
        private final List<DimensionIdentifier<Program, ProgramStage, DimensionParamProgramIndicatorQuery>> params;

        private final TrackedEntityType trackedEntityType;

        private final QueryContext.ParameterManager parameterManager;

        private final AtomicInteger counter = new AtomicInteger( 0 );

        public ProgramIndicatorContext build()
        {
            ProgramIndicatorContext.PrivateBuilder builder = ProgramIndicatorContext.builder();

            for ( DimensionIdentifier<Program, ProgramStage, DimensionParamProgramIndicatorQuery> param : params )
            {
                String assignedAlias = doubleQuote(param.toString() + "_" + counter.getAndIncrement());

                builder.field(Field.ofUnquotedField("", () -> "coalesce("
                        + param.getDimension().getProgramIndicatorQueryProvider().apply(assignedAlias) + ", double precision 'NaN')",
                        param.getDimension().getUid()));
                builder.leftJoin(
                        Pair.of(
                                () -> "(" + enrollmentSelect( param.getProgram()
                                        , trackedEntityType,
                                        parameterManager ) + ") "
                                        + assignedAlias,
                                fieldsEqual( TEI_ALIAS, TEI_UID, assignedAlias, TEI_UID ) ) );
            }
            return builder.build();
        }
    }

    @Data
    @RequiredArgsConstructor( staticName = "of")
    static class DimensionParamProgramIndicatorQuery implements UidObject {
        private final DimensionParam dimensionParam;
        private final Function<String, String> programIndicatorQueryProvider;

        @Override
        public String getUid() {
            return dimensionParam.getUid();
        }
    }

}
