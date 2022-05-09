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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.tracker.TrackerIdSchemeParam;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.domain.Event;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;

class EventCategoryOptionComboSupplierTest extends DhisConvenienceTest
{

    private TrackerPreheat preheat;

    private CategoryService categoryService;

    private EventCategoryOptionComboSupplier supplier;

    @BeforeEach
    void setUp()
    {

        preheat = mock( TrackerPreheat.class );
        categoryService = mock( CategoryService.class );
        supplier = new EventCategoryOptionComboSupplier( categoryService );
    }

    @Test
    void shouldPreheatEventAOCIfNotProvided()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .program( MetadataIdentifier.ofUid( program ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgram( event.getProgram() ) ).thenReturn( program );
        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.toMetadataIdentifier( o ) ) )
                .thenReturn( o ) );
        when( preheat.containsCategoryOptionCombo( program.getCategoryCombo(), aoc.getCategoryOptions() ) )
            .thenReturn( false );
        when( categoryService.getCategoryOptionCombo( categoryCombo, aoc.getCategoryOptions() ) )
            .thenReturn( aoc );

        supplier.preheatAdd( params, preheat );

        verify( preheat, times( 1 ) ).putCategoryOptionCombo( program.getCategoryCombo(), options, aoc );
    }

    @Test
    void shouldPreheatEventAOCIfNotProvidedAndEventHasProgramStageButNoProgram()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .programIdScheme( TrackerIdSchemeParam.CODE )
            .programStageIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        program.setCode( "PROGRAMA" );
        ProgramStage stage = createProgramStage( 'A', program );
        stage.setCode( "STAGEA" );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .programStage( identifierParams.toMetadataIdentifier( stage ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( stage );
        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.toMetadataIdentifier( o ) ) )
                .thenReturn( o ) );
        when( preheat.containsCategoryOptionCombo( program.getCategoryCombo(), aoc.getCategoryOptions() ) )
            .thenReturn( false );
        when( categoryService.getCategoryOptionCombo( program.getCategoryCombo(), aoc.getCategoryOptions() ) )
            .thenReturn( aoc );

        supplier.preheatAdd( params, preheat );

        verify( preheat, times( 1 ) ).putCategoryOptionCombo( program.getCategoryCombo(), options, aoc );
    }

    @Test
    void shouldPreheatEventAOCIfNotProvidedOnlyIfNotAlreadyFetched()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .program( MetadataIdentifier.ofUid( program ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .build();
        List<Event> events = List.of( event, event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgram( event.getProgram() ) ).thenReturn( program );
        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.toMetadataIdentifier( o ) ) )
                .thenReturn( o ) );
        when( preheat.containsCategoryOptionCombo( program.getCategoryCombo(), aoc.getCategoryOptions() ) )
            .thenReturn( false ) // first event will not have its AOC in the
                                 // preheat
            .thenReturn( true ); // second event should see AOC from preheat
        when( categoryService.getCategoryOptionCombo( program.getCategoryCombo(), aoc.getCategoryOptions() ) )
            .thenReturn( aoc );

        supplier.preheatAdd( params, preheat );

        verify( categoryService, times( 1 ) ).getCategoryOptionCombo( categoryCombo, aoc.getCategoryOptions() );
        verify( preheat, times( 1 ) ).putCategoryOptionCombo( program.getCategoryCombo(), options, aoc );
    }

    @Test
    void shouldPreheatEventAOCEvenIfNotFound()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .program( MetadataIdentifier.ofUid( program ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .build();
        List<Event> events = List.of( event, event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgram( event.getProgram() ) ).thenReturn( program );
        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.toMetadataIdentifier( o ) ) )
                .thenReturn( o ) );
        when( preheat.containsCategoryOptionCombo( program.getCategoryCombo(), aoc.getCategoryOptions() ) )
            .thenReturn( false ) // first event will not have the result of
                                 // fetching the AOC stored
            .thenReturn( true ); // second event will have it
        when( categoryService.getCategoryOptionCombo( categoryCombo, aoc.getCategoryOptions() ) )
            .thenReturn( null ); // AOC cannot be found

        supplier.preheatAdd( params, preheat );

        verify( categoryService, times( 1 ) ).getCategoryOptionCombo( categoryCombo, aoc.getCategoryOptions() );
        verify( preheat, times( 1 ) ).putCategoryOptionCombo( program.getCategoryCombo(), options, null );
    }

    @Test
    void shouldNotPreheatEventAOCIfNotProvidedAndCONotFound()
    {
        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .program( MetadataIdentifier.ofUid( program ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgram( event.getProgram() ) ).thenReturn( program );
        when( categoryService.getCategoryOptionCombo( categoryCombo, aoc.getCategoryOptions() ) )
            .thenReturn( aoc );

        supplier.preheatAdd( params, preheat );

        verifyNoInteractions( categoryService );
        verify( preheat, times( 0 ) ).putCategoryOptionCombo( program.getCategoryCombo(), options, aoc );
    }

    @Test
    void shouldNotPreheatEventAOCIfEventHasNoProgram()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( aoc ) )
            .programStage( MetadataIdentifier.EMPTY_UID )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        supplier.preheatAdd( params, preheat );

        verifyNoInteractions( categoryService );
        verify( preheat, times( 0 ) ).putCategoryOptionCombo( any(), eq( options ), eq( aoc ) );
    }

    @Test
    void shouldNotPreheatEventAOCIfEventHasNoProgramAndNoProgramStage()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .programIdScheme( TrackerIdSchemeParam.CODE )
            .programStageIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .programStage( MetadataIdentifier.EMPTY_UID )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.getCategoryOptionIdScheme().getIdentifier( o ) ) )
                .thenReturn( o ) );

        supplier.preheatAdd( params, preheat );

        verifyNoInteractions( categoryService );
        verify( preheat, times( 0 ) ).putCategoryOptionCombo( any(), eq( options ), eq( aoc ) );
    }

    @Test
    void shouldNotPreheatEventAOCIfEventHasNoProgramAndItsProgramStageHasNoProgram()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .programIdScheme( TrackerIdSchemeParam.CODE )
            .programStageIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        ProgramStage stage = createProgramStage( 'A', (Program) null );
        stage.setCode( "STAGEA" );
        CategoryCombo categoryCombo = categoryCombo();
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .programStage( identifierParams.toMetadataIdentifier( stage ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgramStage( event.getProgramStage() ) ).thenReturn( stage );
        options.forEach(
            o -> when( preheat.getCategoryOption( identifierParams.getCategoryOptionIdScheme().getIdentifier( o ) ) )
                .thenReturn( o ) );

        supplier.preheatAdd( params, preheat );

        verifyNoInteractions( categoryService );
        verify( preheat, times( 0 ) ).putCategoryOptionCombo( any(), eq( options ), eq( aoc ) );
    }

    @Test
    void shouldNotPreheatEventAOCIfAOCAndCOsAreProvided()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );
        Set<CategoryOption> options = aoc.getCategoryOptions();

        Event event = Event.builder()
            .program( MetadataIdentifier.ofUid( program.getUid() ) )
            .attributeCategoryOptions( categoryOptionIds( identifierParams, options ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( aoc ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgram( event.getProgram() ) ).thenReturn( program );

        supplier.preheatAdd( params, preheat );

        verifyNoInteractions( categoryService );
        verify( preheat, times( 0 ) ).putCategoryOptionCombo( program.getCategoryCombo(), options, aoc );
    }

    @Test
    void shouldNotPreheatEventAOCIfAOCIsProvided()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );
        CategoryOptionCombo aoc = firstCategoryOptionCombo( categoryCombo );

        Event event = Event.builder()
            .program( MetadataIdentifier.ofUid( program ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( aoc ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgram( event.getProgram() ) ).thenReturn( program );

        supplier.preheatAdd( params, preheat );

        verifyNoInteractions( categoryService );
        verify( preheat, times( 0 ) ).putCategoryOptionCombo( program.getCategoryCombo(), aoc.getCategoryOptions(),
            aoc );
    }

    @Test
    void shouldNotPreheatEventAOCIfNoCategoryOptionsProvided()
    {

        TrackerIdSchemeParams identifierParams = TrackerIdSchemeParams.builder()
            .categoryOptionComboIdScheme( TrackerIdSchemeParam.CODE )
            .categoryOptionIdScheme( TrackerIdSchemeParam.UID )
            .build();

        Program program = createProgram( 'A' );
        CategoryCombo categoryCombo = categoryCombo();
        program.setCategoryCombo( categoryCombo );

        Event event = Event.builder()
            .program( MetadataIdentifier.ofUid( program ) )
            .attributeOptionCombo( identifierParams.toMetadataIdentifier( (CategoryOptionCombo) null ) )
            .build();
        List<Event> events = List.of( event );
        TrackerImportParams params = TrackerImportParams.builder()
            .idSchemes( identifierParams )
            .events( events )
            .build();

        when( preheat.getProgram( event.getProgram() ) ).thenReturn( program );

        supplier.preheatAdd( params, preheat );

        verifyNoInteractions( categoryService );
        verify( preheat, times( 0 ) ).putCategoryOptionCombo( any(), any(), any() );
    }

    private Set<MetadataIdentifier> categoryOptionIds( TrackerIdSchemeParams params, Set<CategoryOption> options )
    {
        return options.stream()
            .map( params::toMetadataIdentifier )
            .collect( Collectors.toSet() );
    }

    private CategoryCombo categoryCombo()
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