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
import static org.hisp.dhis.analytics.event.EventsAnalyticsDimensionalItems.EMPTY_ANALYTICS_DIMENSIONAL_ITEMS;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.event.EventAnalyticsDimensionalItemService;
import org.hisp.dhis.analytics.event.EventsAnalyticsDimensionalItems;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@Service
@RequiredArgsConstructor
class DefaultEventAnalyticsDimensionalItemService implements EventAnalyticsDimensionalItemService
{

    final static Collection<ValueType> QUERY_DISALLOWED_VALUE_TYPES = ImmutableSet.of(
        IMAGE,
        FILE_RESOURCE,
        TRACKER_ASSOCIATE );

    final static Collection<ValueType> AGGREGATE_ALLOWED_VALUE_TYPES = ImmutableSet.of(
        NUMBER,
        UNIT_INTERVAL,
        PERCENTAGE,
        INTEGER,
        INTEGER_POSITIVE,
        INTEGER_NEGATIVE,
        INTEGER_ZERO_OR_POSITIVE,
        BOOLEAN,
        TRUE_ONLY );

    private final Map<OperationType, Predicate<ValueType>> OPERATION_FILTER = ImmutableMap.of(
        OperationType.QUERY, not( QUERY_DISALLOWED_VALUE_TYPES::contains ),
        OperationType.AGGREGATE, AGGREGATE_ALLOWED_VALUE_TYPES::contains );

    @NonNull
    private final ProgramStageService programStageService;

    @NonNull
    private final CategoryService categoryService;

    @Override
    public EventsAnalyticsDimensionalItems getQueryDimensionalItemsByProgramStageId( String programStageId )
    {
        return Optional.of( programStageId )
            .map( programStageService::getProgramStage )
            .map( ProgramStage::getProgram )
            .map( p -> EventsAnalyticsDimensionalItems.builder()
                .programIndicators( p.getProgramIndicators() )
                .dataElements(
                    filterByValueType(
                        OperationType.QUERY,
                        p.getDataElements(),
                        DataElement::getValueType ) )
                .trackedEntityAttributes(
                    filterByValueType(
                        OperationType.QUERY,
                        getTeasIfRegistrationAndNotConfidential( p ),
                        TrackedEntityAttribute::getValueType ) )
                .comboCategories( getCategoriesIfNeeded( p ) )
                .attributeCategoryOptionGroupSets( getAttributeCategoryOptionGroupSetsIfNeeded( p ) )
                .build() )
            .orElse( EMPTY_ANALYTICS_DIMENSIONAL_ITEMS );
    }

    @Override
    public EventsAnalyticsDimensionalItems getAggregateDimensionalItemsByProgramStageId( String programStageId )
    {
        return Optional.of( programStageId )
            .map( programStageService::getProgramStage )
            .map( ps -> EventsAnalyticsDimensionalItems.builder()
                .programIndicators( null )
                .dataElements(
                    filterByValueType( OperationType.AGGREGATE,
                        ps.getDataElements(),
                        DataElement::getValueType ) )
                .trackedEntityAttributes(
                    filterByValueType( OperationType.AGGREGATE,
                        ps.getProgram().getTrackedEntityAttributes(),
                        TrackedEntityAttribute::getValueType ) )
                .comboCategories( getCategoriesIfNeeded( ps.getProgram() ) )
                .attributeCategoryOptionGroupSets( getAttributeCategoryOptionGroupSetsIfNeeded( ps.getProgram() ) )
                .build() )
            .orElse( EMPTY_ANALYTICS_DIMENSIONAL_ITEMS );
    }

    private List<CategoryOptionGroupSet> getAttributeCategoryOptionGroupSetsIfNeeded( Program program )
    {
        return Optional.of( program )
            .filter( Program::hasCategoryCombo )
            .map( unused -> categoryService.getAllCategoryOptionGroupSets().stream()
                .filter( this::isTypeAttribute )
                .collect( Collectors.toList() ) )
            .orElse( Collections.emptyList() );
    }

    private boolean isTypeAttribute( CategoryOptionGroupSet categoryOptionGroupSet )
    {
        return categoryOptionGroupSet.getDataDimensionType().equals( DataDimensionType.ATTRIBUTE );
    }

    private Collection<Category> getCategoriesIfNeeded( Program program )
    {
        return Optional.of( program )
            .filter( Program::hasCategoryCombo )
            .map( Program::getCategoryCombo )
            .map( CategoryCombo::getCategories )
            .orElse( Collections.emptyList() );
    }

    private Collection<TrackedEntityAttribute> getTeasIfRegistrationAndNotConfidential( Program program )
    {
        return Optional.of( program )
            .filter( Program::isRegistration )
            .map( Program::getTrackedEntityAttributes )
            .orElse( Collections.emptyList() )
            .stream()
            .filter( this::isNotConfidential )
            .collect( Collectors.toList() );
    }

    private boolean isNotConfidential( TrackedEntityAttribute trackedEntityAttribute )
    {
        return !trackedEntityAttribute.isConfidentialBool();
    }

    private <T extends BaseDimensionalItemObject> Collection<T> filterByValueType( OperationType operationType,
        Collection<T> elements, Function<T, ValueType> valueTypeProvider )
    {
        return elements.stream()
            .filter( t -> OPERATION_FILTER.get( operationType ).test( valueTypeProvider.apply( t ) ) )
            .collect( Collectors.toList() );
    }

    private enum OperationType
    {
        QUERY,
        AGGREGATE
    }
}
