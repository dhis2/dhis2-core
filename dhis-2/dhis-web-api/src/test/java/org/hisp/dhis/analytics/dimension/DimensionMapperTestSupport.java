/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.analytics.dimension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.common.BaseIdentifiableObject;

public class DimensionMapperTestSupport
{
    public static <T extends BaseIdentifiableObject> void asserter(
        DimensionMapper dimensionMapper,
        Supplier<T> instanceSupplier,
        List<Consumer<T>> instanceSetters,
        List<Pair<Function<DimensionResponse, Object>, Object>> assertingPairs )
    {

        T item = getBaseIdentifiableObject( instanceSupplier, instanceSetters );

        assertAll( assertingPairs.stream()
            .map( functionObjectPair -> Pair.<Supplier<?>, Object> of(
                () -> functionObjectPair.getKey().apply( dimensionMapper.map( item ) ),
                functionObjectPair.getRight() ) )
            .collect( Collectors.toList() ) );
    }

    private static <T extends BaseIdentifiableObject> T getBaseIdentifiableObject( Supplier<T> instanceSupplier,
        List<Consumer<T>> setters )
    {
        T baseIdentifiableObject = instanceSupplier.get();
        setters.forEach( setter -> setter.accept( baseIdentifiableObject ) );
        return baseIdentifiableObject;
    }

    private static void assertAll( Collection<Pair<Supplier<?>, Object>> assertions )
    {
        assertions.forEach( DimensionMapperTestSupport::assertOne );
    }

    private static void assertOne( Pair<Supplier<?>, Object> assertingPair )
    {
        assertThat( assertingPair.getKey().get().toString(), is( assertingPair.getValue().toString() ) );
    }
}
