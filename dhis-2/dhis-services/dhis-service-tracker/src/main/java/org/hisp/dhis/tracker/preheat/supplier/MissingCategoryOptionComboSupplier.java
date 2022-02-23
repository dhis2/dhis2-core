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
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@SupplierDependsOn( ClassBasedSupplier.class )
public class MissingCategoryOptionComboSupplier extends AbstractPreheatSupplier
{
    @NonNull
    private final CategoryService categoryService;

    @Override
    public void preheatAdd( TrackerImportParams params, TrackerPreheat preheat )
    {
        // TODO do I need to replicate what we do in EventProgramPreProcessor?
        // for an event payload that has no program but only a program stage

        // get pairs of event program category combo and non-empty category
        // options
        List<Event> events = params.getEvents().stream()
            .filter( e -> {
                Program p = preheat.get( Program.class, e.getProgram() );
                if ( p != null && !p.getCategoryCombo().isDefault() )
                {
                    return true;
                }
                return false;
            } )
            .filter( e -> StringUtils.isBlank( e.getAttributeOptionCombo() )
                && !StringUtils.isBlank( e.getAttributeCategoryOptions() ) )
            .collect( Collectors.toList() );

        // TODO question: will the aoc.categoryOptions be non-lazy? If not how
        // to force that
        // TODO should we adapt the service so we can fetch AOCs at once? So for
        // all (category combo, category options)
        for ( Event e : events )
        {
            Program p = preheat.get( Program.class, e.getProgram() );
            CategoryOptionCombo aoc = categoryService
                .getCategoryOptionCombo( p.getCategoryCombo(), getCategoryOptions( preheat, e ) );

            // TODO should we cache that we did not find the AOC as well?
            if ( aoc != null )
            {
                preheat.putCachedEventAOCProgramCC( p, e.getAttributeCategoryOptions(), aoc );
            }

            preheat.put( params.getIdentifiers().getCategoryOptionComboIdScheme(), aoc );
        }
    }

    private Set<CategoryOption> getCategoryOptions( TrackerPreheat preheat, Event event )
    {

        Set<CategoryOption> categoryOptions = new HashSet<>();
        Set<String> categoryOptionIds = parseCategoryOptions( event );
        for ( String id : categoryOptionIds )
        {
            categoryOptions.add( preheat.getCategoryOption( id ) );
        }
        return categoryOptions;
    }

    private Set<String> parseCategoryOptions( Event event )
    {
        String cos = StringUtils.strip( event.getAttributeCategoryOptions() );
        if ( StringUtils.isBlank( cos ) )
        {
            return Collections.emptySet();
        }

        return TextUtils
            .splitToArray( cos, TextUtils.SEMICOLON );
    }
}
