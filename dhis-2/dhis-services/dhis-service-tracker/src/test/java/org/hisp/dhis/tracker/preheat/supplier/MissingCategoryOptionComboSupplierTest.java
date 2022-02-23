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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.hisp.dhis.tracker.TrackerIdentifierParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

// TODO fetch AOC also with default program if COs are set
// this is so we can make sure that the validation hook can expect
// rely on there always being an event.AOC
// it then has to check if AOC and COs match

// TODO what about when AOC is given? In that case I assume it is already
// fetched from the DB and in the preheat by the classbasedsupplier
// validation hook should still check for existence
// validation
// * no aoc, no cos => is this event in a program with default cc
// * aoc => does it exist?
// * cos => do they exist?
// * aoc, cos => do they match?
// I think that is what will be left
class MissingCategoryOptionComboSupplierTest extends DhisConvenienceTest
{

    private TrackerPreheat preheat;

    private CategoryService categoryService;

    private MissingCategoryOptionComboSupplier supplier;

    @BeforeEach
    void setUp()
    {

        preheat = mock( TrackerPreheat.class );
        categoryService = mock( CategoryService.class );
        supplier = new MissingCategoryOptionComboSupplier( categoryService );
    }

    @Test
    void shouldPreheatEventAOCIfNotProvided()
    {

        TrackerIdentifierParams identifierParams = TrackerIdentifierParams.builder()
            .categoryOptionComboIdScheme( TrackerIdentifier.CODE )
            .categoryOptionIdScheme( TrackerIdentifier.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryComboWithTwoCategories();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        List<CategoryOption> options = categoryOptions( aoc );

        Event event = Event.builder()
            .program( program.getUid() )
            .attributeCategoryOptions( concatCategoryOptions( identifierParams.getCategoryOptionIdScheme(), options ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .identifiers( identifierParams )
            .events( events )
            .build();

        when( preheat.get( Program.class, event.getProgram() ) ).thenReturn( program );
        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.getCategoryOptionIdScheme().getIdentifier( o ) ) )
                .thenReturn( o ) );
        when( categoryService.getCategoryOptionCombo( categoryCombo, aoc.getCategoryOptions() ) ).thenReturn( aoc );

        supplier.preheatAdd( params, preheat );

        verify( preheat, times( 1 ) ).putCachedEventAOCProgramCC( program, event.getAttributeCategoryOptions(), aoc );
    }

    @Test
    void shouldNotPreheatEventAOCIfNotProvidedAndNotFound()
    {

        TrackerIdentifierParams identifierParams = TrackerIdentifierParams.builder()
            .categoryOptionComboIdScheme( TrackerIdentifier.CODE )
            .categoryOptionIdScheme( TrackerIdentifier.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryComboWithTwoCategories();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );

        List<CategoryOption> options = categoryOptions( aoc ).subList( 0, 1 );

        Event event = Event.builder()
            .program( program.getUid() )
            .attributeCategoryOptions( concatCategoryOptions( identifierParams.getCategoryOptionIdScheme(), options ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .identifiers( identifierParams )
            .events( events )
            .build();

        when( preheat.get( Program.class, event.getProgram() ) ).thenReturn( program );
        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.getCategoryOptionIdScheme().getIdentifier( o ) ) )
                .thenReturn( o ) );
        when( categoryService.getCategoryOptionCombo( categoryCombo, aoc.getCategoryOptions() ) ).thenReturn( aoc );

        supplier.preheatAdd( params, preheat );

        verify( preheat, times( 0 ) ).putCachedEventAOCProgramCC( program, event.getAttributeCategoryOptions(), aoc );
    }

    @Test
    void shouldNotPreheatEventAOCIfAOCAndCOsAreProvided()
    {

        TrackerIdentifierParams identifierParams = TrackerIdentifierParams.builder()
            .categoryOptionComboIdScheme( TrackerIdentifier.CODE )
            .categoryOptionIdScheme( TrackerIdentifier.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryComboWithTwoCategories();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        List<CategoryOption> options = categoryOptions( aoc );

        Event event = Event.builder()
            .program( program.getUid() )
            .attributeCategoryOptions( concatCategoryOptions( identifierParams.getCategoryOptionIdScheme(), options ) )
            .attributeOptionCombo( identifierParams.getCategoryOptionComboIdScheme().getIdentifier( aoc ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .identifiers( identifierParams )
            .events( events )
            .build();

        when( preheat.get( Program.class, event.getProgram() ) ).thenReturn( program );

        supplier.preheatAdd( params, preheat );

        verify( preheat, times( 0 ) ).putCachedEventAOCProgramCC( program, event.getAttributeCategoryOptions(), aoc );
    }

    @Test
    void shouldNotPreheatEventAOCIfAOCIsProvided()
    {

        TrackerIdentifierParams identifierParams = TrackerIdentifierParams.builder()
            .categoryOptionComboIdScheme( TrackerIdentifier.CODE )
            .categoryOptionIdScheme( TrackerIdentifier.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryComboWithTwoCategories();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );

        Event event = Event.builder()
            .program( program.getUid() )
            .attributeOptionCombo( identifierParams.getCategoryOptionComboIdScheme().getIdentifier( aoc ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .identifiers( identifierParams )
            .events( events )
            .build();

        when( preheat.get( Program.class, event.getProgram() ) ).thenReturn( program );

        supplier.preheatAdd( params, preheat );

        verify( preheat, times( 0 ) ).putCachedEventAOCProgramCC( program, event.getAttributeCategoryOptions(), aoc );
    }

    @Test
    void shouldNotPreheatEventAOCIfNoCategoryOptionsProvided()
    {

        TrackerIdentifierParams identifierParams = TrackerIdentifierParams.builder()
            .categoryOptionComboIdScheme( TrackerIdentifier.CODE )
            .categoryOptionIdScheme( TrackerIdentifier.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryComboWithTwoCategories();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );

        List<CategoryOption> options = categoryOptions( aoc ).subList( 0, 1 );

        Event event = Event.builder()
            .program( program.getUid() )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .identifiers( identifierParams )
            .events( events )
            .build();

        when( preheat.get( Program.class, event.getProgram() ) ).thenReturn( program );

        supplier.preheatAdd( params, preheat );

        verify( preheat, times( 0 ) ).putCachedEventAOCProgramCC( program, event.getAttributeCategoryOptions(), aoc );
    }

    private String concatCategoryOptions( TrackerIdentifier identifier, List<CategoryOption> options )
    {
        return String.join( ";", options.stream()
            .map( co -> identifier.getIdentifier( co ) )
            .collect( Collectors.toList() ) );
    }

    private List<CategoryOption> categoryOptions( CategoryOptionCombo aoc )
    {
        return new ArrayList<>( aoc.getCategoryOptions() );
    }

    private CategoryCombo categoryCombo()
    {
        return categoryCombo( 'A' );
    }

    private CategoryCombo categoryCombo( char uniqueIdentifier )
    {
        CategoryOption co1 = createCategoryOption( uniqueIdentifier );
        CategoryOption co2 = createCategoryOption( uniqueIdentifier );
        Category ca = createCategory( uniqueIdentifier, co1, co2 );
        CategoryCombo cc = createCategoryCombo( uniqueIdentifier, ca );
        cc.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        CategoryOptionCombo aoc1 = createCategoryOptionCombo( cc, co1 );
        CategoryOptionCombo aoc2 = createCategoryOptionCombo( cc, co2 );
        cc.setOptionCombos( Sets.newHashSet( aoc1, aoc2 ) );
        return cc;
    }

    private CategoryCombo categoryComboWithTwoCategories()
    {
        char uniqueIdentifier = 'A';
        CategoryOption co1 = createCategoryOption( uniqueIdentifier );
        CategoryOption co2 = createCategoryOption( uniqueIdentifier );
        Category ca1 = createCategory( uniqueIdentifier, co1, co2 );
        CategoryOption co3 = createCategoryOption( uniqueIdentifier );
        Category ca2 = createCategory( uniqueIdentifier, co3 );
        CategoryCombo cc = createCategoryCombo( uniqueIdentifier, ca1, ca2 );
        cc.setDataDimensionType( DataDimensionType.ATTRIBUTE );
        CategoryOptionCombo aoc1 = createCategoryOptionCombo( cc, co1, co3 );
        CategoryOptionCombo aoc2 = createCategoryOptionCombo( cc, co2, co3 );
        cc.setOptionCombos( Sets.newHashSet( aoc1, aoc2 ) );
        return cc;
    }

    private CategoryOptionCombo firstCategoryOptionCombo( CategoryCombo categoryCombo )
    {
        assertNotNull( categoryCombo.getOptionCombos() );
        assertFalse( categoryCombo.getOptionCombos().isEmpty() );

        return categoryCombo.getSortedOptionCombos().get( 0 );
    }
}