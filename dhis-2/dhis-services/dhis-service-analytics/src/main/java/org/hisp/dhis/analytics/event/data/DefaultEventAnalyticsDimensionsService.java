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
package org.hisp.dhis.analytics.event.data;

import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.OperationType.AGGREGATE;
import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.OperationType.QUERY;
import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.collectDimensions;
import static org.hisp.dhis.analytics.event.data.DimensionsServiceCommon.filterByValueType;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.PrefixedDimensions.ofDataElements;
import static org.hisp.dhis.common.PrefixedDimensions.ofItemsWithProgram;
import static org.hisp.dhis.common.PrefixedDimensions.ofProgramIndicators;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.event.EventAnalyticsDimensionsService;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.PrefixedDimension;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultEventAnalyticsDimensionsService implements EventAnalyticsDimensionsService
{
    @NonNull
    private final ProgramStageService programStageService;

    @NonNull
    private final CategoryService categoryService;

    @Override
    public List<PrefixedDimension> getQueryDimensionsByProgramStageId( String programStageId )
    {
        Optional<ProgramStage> programStage = Optional.of( programStageId )
            .map( programStageService::getProgramStage );

        if ( programStage.isPresent() )
        {
            return programStage
                .map( ProgramStage::getProgram )
                .map( p -> collectDimensions(
                    List.of(
                        ofProgramIndicators( p.getProgramIndicators() ),
                        filterByValueType(
                            QUERY,
                            ofDataElements( programStage.get() ) ),
                        filterByValueType(
                            QUERY,
                            ofItemsWithProgram( p, getTeasIfRegistrationAndNotConfidential( p ) ) ),
                        ofItemsWithProgram( p, getCategoriesIfNeeded( p ) ),
                        ofItemsWithProgram( p, getAttributeCategoryOptionGroupSetsIfNeeded( p ) ) ) ) )
                .orElse( Collections.emptyList() );
        }
        return Collections.emptyList();
    }

    @Override
    public List<PrefixedDimension> getAggregateDimensionsByProgramStageId( String programStageId )
    {
        return Optional.of( programStageId )
            .map( programStageService::getProgramStage )
            .map( ps -> collectDimensions(
                List.of(
                    filterByValueType( AGGREGATE,
                        ofDataElements( ps ) ),
                    filterByValueType( AGGREGATE,
                        ofItemsWithProgram( ps.getProgram(), ps.getProgram().getTrackedEntityAttributes() ) ),
                    ofItemsWithProgram( ps.getProgram(), getCategoriesIfNeeded( ps.getProgram() ) ),
                    ofItemsWithProgram( ps.getProgram(),
                        getAttributeCategoryOptionGroupSetsIfNeeded( ps.getProgram() ) ) ) ) )
            .orElse( Collections.emptyList() );
    }

    private List<CategoryOptionGroupSet> getAttributeCategoryOptionGroupSetsIfNeeded( Program program )
    {
        return Optional.of( program )
            .filter( Program::hasNonDefaultCategoryCombo )
            .map( unused -> categoryService.getAllCategoryOptionGroupSets().stream()
                .filter( this::isTypeAttribute )
                .collect( Collectors.toList() ) )
            .orElse( Collections.emptyList() );
    }

    private boolean isTypeAttribute( CategoryOptionGroupSet categoryOptionGroupSet )
    {
        return ATTRIBUTE == categoryOptionGroupSet.getDataDimensionType();
    }

    private Collection<Category> getCategoriesIfNeeded( Program program )
    {
        return Optional.of( program )
            .filter( Program::hasNonDefaultCategoryCombo )
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
}
