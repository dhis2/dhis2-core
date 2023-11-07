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
package org.hisp.dhis.webapi.dimension;

import static org.hisp.dhis.commons.collection.CollectionUtils.mapToList;
import static org.hisp.dhis.hibernate.HibernateProxyUtils.getRealClass;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.PrefixedDimension;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DimensionMapperService {
  private final Collection<DimensionMapper> mappers;

  public List<DimensionResponse> toDimensionResponse(
      Collection<PrefixedDimension> dimensions, PrefixStrategy prefixStrategy) {
    return toDimensionResponse(dimensions, prefixStrategy, false);
  }

  public List<DimensionResponse> toDimensionResponse(
      Collection<PrefixedDimension> dimensions, PrefixStrategy prefixStrategy, boolean distinct) {

    UnaryOperator<List<DimensionResponse>> distinctFunction =
        distinct ? this::distinctByUid : UnaryOperator.identity();

    return distinctFunction.apply(
        mapToList(
            dimensions,
            pDimension -> toDimensionResponse(pDimension, prefixStrategy.apply(pDimension))));
  }

  private List<DimensionResponse> distinctByUid(List<DimensionResponse> dimensionResponses) {
    return dimensionResponses.stream().filter(distinctBy(DimensionResponse::getUid)).toList();
  }

  private static <T> Predicate<T> distinctBy(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }

  private DimensionResponse toDimensionResponse(PrefixedDimension dimension, String prefix) {
    return mappers.stream()
        .filter(dimensionMapper -> dimensionMapper.supports(dimension.getItem()))
        .findFirst()
        .map(dimensionMapper -> dimensionMapper.map(dimension, prefix))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unsupported dimension type: " + getRealClass(dimension.getItem())));
  }
}
