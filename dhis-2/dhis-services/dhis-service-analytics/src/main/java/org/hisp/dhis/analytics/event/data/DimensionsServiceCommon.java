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
package org.hisp.dhis.analytics.event.data;

import static java.util.function.Predicate.not;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.FILE_RESOURCE;
import static org.hisp.dhis.common.ValueType.IMAGE;
import static org.hisp.dhis.common.ValueType.INTEGER;
import static org.hisp.dhis.common.ValueType.INTEGER_NEGATIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_POSITIVE;
import static org.hisp.dhis.common.ValueType.INTEGER_ZERO_OR_POSITIVE;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.PERCENTAGE;
import static org.hisp.dhis.common.ValueType.TRACKER_ASSOCIATE;
import static org.hisp.dhis.common.ValueType.TRUE_ONLY;
import static org.hisp.dhis.common.ValueType.UNIT_INTERVAL;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.ValueType;

public interface DimensionsServiceCommon
{

    Collection<ValueType> QUERY_DISALLOWED_VALUE_TYPES = Set.of(
        IMAGE,
        FILE_RESOURCE,
        TRACKER_ASSOCIATE );

    Collection<ValueType> AGGREGATE_ALLOWED_VALUE_TYPES = Set.of(
        NUMBER,
        UNIT_INTERVAL,
        PERCENTAGE,
        INTEGER,
        INTEGER_POSITIVE,
        INTEGER_NEGATIVE,
        INTEGER_ZERO_OR_POSITIVE,
        BOOLEAN,
        TRUE_ONLY );

    Map<OperationType, Predicate<ValueType>> OPERATION_FILTER = Map.of(
        OperationType.QUERY, not( QUERY_DISALLOWED_VALUE_TYPES::contains ),
        OperationType.AGGREGATE, AGGREGATE_ALLOWED_VALUE_TYPES::contains );

    enum OperationType
    {
        QUERY,
        AGGREGATE
    }

    default List<BaseIdentifiableObject> collectDimensions(
        Collection<Collection<? extends BaseIdentifiableObject>> dimensionCollections )
    {
        return dimensionCollections.stream()
            .flatMap( Collection::stream )
            .collect( Collectors.toList() );
    }

    default <T extends BaseIdentifiableObject> Collection<T> filterByValueType(
        DimensionsServiceCommon.OperationType operationType,
        Collection<T> elements, Function<T, ValueType> valueTypeProvider )
    {
        return elements.stream()
            .filter( t -> OPERATION_FILTER.get( operationType ).test( valueTypeProvider.apply( t ) ) )
            .collect( Collectors.toList() );
    }
}
