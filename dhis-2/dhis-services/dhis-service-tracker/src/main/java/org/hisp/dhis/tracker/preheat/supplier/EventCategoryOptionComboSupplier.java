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
package org.hisp.dhis.tracker.preheat.supplier;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.mappers.CategoryOptionComboMapper;
import org.springframework.stereotype.Component;

/**
 * EventCategoryOptionComboSupplier adds category option combos to the preheat
 * for events with attributeCategoryOptions but no attributeOptionCombo.
 *
 * {@link ClassBasedSupplier} is responsible for adding category option combos
 * to the preheat for which identifiers are present in the event.
 *
 * An event for which the category option combo can not be found is invalid.
 * Validation will thus subsequently invalidate it.
 */
@RequiredArgsConstructor
@Component
@SupplierDependsOn( ClassBasedSupplier.class )
public class EventCategoryOptionComboSupplier extends AbstractPreheatSupplier
{
    @NonNull
    private final CategoryService categoryService;

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {

        List<Pair<CategoryCombo, Set<CategoryOption>>> events = params.getEvents().stream()
            .filter( e -> e.getAttributeOptionCombo().isBlank()
                && !StringUtils.isBlank( e.getAttributeCategoryOptions() ) )
            .map( e -> Pair.of( resolveProgram( preheat, e ), parseCategoryOptionIds( e ) ) )
            .filter( p -> p.getLeft() != null )
            .filter( p -> hasOnlyExistingCategoryOptions( preheat, p.getRight() ) )
            .map( p -> Pair.of( p.getLeft().getCategoryCombo(), toCategoryOptions( preheat, p.getRight() ) ) )
            .collect( Collectors.toList() );

        for ( Pair<CategoryCombo, Set<CategoryOption>> p : events )
        {
            if ( preheat.containsCategoryOptionCombo( p.getLeft(), p.getRight() ) )
            {
                continue;
            }

            CategoryOptionCombo aoc = CategoryOptionComboMapper.INSTANCE
                .map( categoryService.getCategoryOptionCombo( p.getLeft(), p.getRight() ) );
            preheat.putCategoryOptionCombo( p.getLeft(), p.getRight(), aoc );
        }
    }

    /**
     * Resolve the event program either via the program property in the payload
     * or via the programStage property in the payload. Property program is not
     * required but programStage is. Since the preheat phase is before
     * pre-process and validation we need to be more defensive with null-checks.
     */
    private Program resolveProgram( TrackerPreheat preheat, Event e )
    {

        Program program = preheat.getProgram( e.getProgram() );
        if ( program != null )
        {
            return program;
        }

        if ( e.getProgramStage().isBlank() )
        {
            return null;
        }
        ProgramStage programStage = preheat.get( ProgramStage.class, e.getProgramStage() );
        if ( programStage == null || programStage.getProgram() == null )
        {
            // TODO remove check for programStage.getProgram() == null once
            // metadata import is fixed
            // Program stages should always have a program! Due to
            // how metadata
            // import is currently implemented
            // it's possible that users run into the edge case that
            // a program
            // stage does not have an associated
            // program. Tell the user it's an issue with the
            // metadata and not
            // the event itself. This should be
            // fixed in the metadata import. For more see
            // https://jira.dhis2.org/browse/DHIS2-12123
            //
            // PreCheckMandatoryFieldsValidationHook.validateEvent
            // will create
            // a validation error for this edge case
            return null;
        }
        return programStage.getProgram();
    }

    private boolean hasOnlyExistingCategoryOptions( TrackerPreheat preheat, Set<String> ids )
    {
        for ( String id : ids )
        {
            if ( preheat.getCategoryOption( id ) == null )
            {
                return false;
            }
        }
        return true;
    }

    private Set<CategoryOption> toCategoryOptions( TrackerPreheat preheat, Set<String> ids )
    {
        Set<CategoryOption> categoryOptions = new HashSet<>();
        for ( String id : ids )
        {
            categoryOptions.add( preheat.getCategoryOption( id ) );
        }
        return categoryOptions;
    }

    private Set<String> parseCategoryOptionIds( Event event )
    {
        String cos = StringUtils.strip( event.getAttributeCategoryOptions() );
        if ( StringUtils.isBlank( cos ) )
        {
            return Collections.emptySet();
        }

        return TextUtils.splitToSet( cos, TextUtils.SEMICOLON );
    }
}
