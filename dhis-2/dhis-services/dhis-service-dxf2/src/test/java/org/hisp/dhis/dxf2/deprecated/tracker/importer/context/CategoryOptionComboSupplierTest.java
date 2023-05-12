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
package org.hisp.dhis.dxf2.deprecated.tracker.importer.context;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Luciano Fiandesio
 */
@MockitoSettings( strictness = Strictness.LENIENT )
class CategoryOptionComboSupplierTest extends AbstractSupplierTest<CategoryOptionCombo>
{

    private CategoryOptionComboSupplier subject;

    @Mock
    private ProgramSupplier programSupplier;

    @Mock
    private AttributeOptionComboLoader attributeOptionComboLoader;

    @Mock
    private ProgramStageInstanceSupplier programStageInstanceSupplier;

    private Map<String, Program> programMap;

    private Map<String, Event> programStageInstanceMap;

    private org.hisp.dhis.dxf2.deprecated.tracker.event.Event event;

    private Program program;

    private Event programStageInstance;

    private CategoryOptionCombo coc;

    @BeforeEach
    void setUp()
    {
        this.subject = new CategoryOptionComboSupplier( jdbcTemplate, programSupplier, attributeOptionComboLoader,
            programStageInstanceSupplier );
        programMap = new HashMap<>();
        programStageInstanceMap = new HashMap<>();
        // create a Program for the ProgramSupplier
        program = new Program();
        program.setId( 999L );
        program.setUid( "prabcde" );
        programMap.put( "prabcde", program );
        // create a Event for the ProgramStageInstanceSupplier
        coc = new CategoryOptionCombo();
        coc.setUid( "coc1" );
        programStageInstance = new Event();
        programStageInstance.setId( 888L );
        programStageInstance.setUid( "psuid1" );
        programStageInstance.setAttributeOptionCombo( coc );
        programStageInstanceMap.put( "psuid1", programStageInstance );
        // create event to import
        event = new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setAttributeOptionCombo( "aoc1234" );
        event.setProgram( program.getUid() );
    }

    @Test
    void handleNullEvents()
    {
        assertNotNull( subject.get( ImportOptions.getDefaultImportOptions(), null ) );
    }

    public void verifySupplier()
    {
        /*
         * Case 1: Event has 'attributeOptionCombo' value set
         */
        case1( ImportOptions.getDefaultImportOptions() );
        /*
         * Case 2: Event has 'attributeOptionCombo' value set, but it's not
         * found -> fetch default coc
         */
        case2( ImportOptions.getDefaultImportOptions() );
        /*
         * Case 3: Event has 'attributeCategoryOptions' value set
         */
        case3( ImportOptions.getDefaultImportOptions() );
        /*
         * Case 4: Event has no 'attributeCategoryOptions' or
         * 'attributeOptionCombo' values set -> fetch default coc
         */
        case4( ImportOptions.getDefaultImportOptions() );
        /*
         * Case 5: Event has both 'attributeCategoryOptions' and
         * 'attributeOptionCombo' values set attributeOptionCombo is used to
         * fetch the coc
         */
        case5( ImportOptions.getDefaultImportOptions() );
        /*
         * Case 6: Event does not have 'attributeCategoryOptions' and program
         * stage instance (persisted event) is used to fetch the coc
         */
        case6( ImportOptions.getDefaultImportOptions() );
    }

    private void case1( ImportOptions importOptions )
    {
        when( programSupplier.get( eq( importOptions ), anyList() ) ).thenReturn( programMap );
        CategoryOptionCombo coc = new CategoryOptionCombo();
        coc.setUid( event.getAttributeOptionCombo() );
        when( attributeOptionComboLoader.getCategoryOptionCombo(
            importOptions.getIdSchemes().getCategoryOptionComboIdScheme(), event.getAttributeOptionCombo() ) )
                .thenReturn( coc );
        Map<String, CategoryOptionCombo> map = subject.get( importOptions, singletonList( event ) );
        CategoryOptionCombo categoryOptionCombo = map.get( event.getUid() );
        assertThat( categoryOptionCombo, is( notNullValue() ) );
    }

    private void case2( ImportOptions importOptions )
    {
        when( programSupplier.get( eq( importOptions ), anyList() ) ).thenReturn( programMap );
        CategoryOptionCombo coc = new CategoryOptionCombo();
        coc.setUid( "def123" );
        coc.setName( "default " );
        when( attributeOptionComboLoader.getCategoryOptionCombo(
            importOptions.getIdSchemes().getCategoryOptionComboIdScheme(), event.getAttributeOptionCombo() ) )
                .thenReturn( null );
        when( attributeOptionComboLoader.getDefault() ).thenReturn( coc );
        Map<String, CategoryOptionCombo> map = subject.get( importOptions, singletonList( event ) );
        CategoryOptionCombo categoryOptionCombo = map.get( event.getUid() );
        assertThat( categoryOptionCombo, is( notNullValue() ) );
        assertThat( categoryOptionCombo.getName(), is( coc.getName() ) );
    }

    private void case3( ImportOptions importOptions )
    {
        when( programSupplier.get( eq( importOptions ), anyList() ) ).thenReturn( programMap );
        CategoryCombo catCombo = new CategoryCombo();
        catCombo.setUid( CodeGenerator.generateUid() );
        program.setCategoryCombo( catCombo );
        // create event to import
        org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setAttributeCategoryOptions( "abcded;fghilm" );
        event.setProgram( program.getUid() );
        CategoryOptionCombo coc = new CategoryOptionCombo();
        coc.setUid( CodeGenerator.generateUid() );
        when( attributeOptionComboLoader.getAttributeOptionCombo( catCombo, event.getAttributeCategoryOptions(),
            event.getAttributeOptionCombo(), importOptions.getIdSchemes().getCategoryOptionComboIdScheme() ) )
                .thenReturn( coc );
        Map<String, CategoryOptionCombo> map = subject.get( importOptions, singletonList( event ) );
        CategoryOptionCombo categoryOptionCombo = map.get( event.getUid() );
        assertThat( categoryOptionCombo, is( notNullValue() ) );
        assertThat( categoryOptionCombo.getUid(), is( coc.getUid() ) );
    }

    private void case4( ImportOptions importOptions )
    {
        when( programSupplier.get( eq( importOptions ), anyList() ) ).thenReturn( programMap );
        // create event to import
        org.hisp.dhis.dxf2.deprecated.tracker.event.Event event = new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
        event.setUid( CodeGenerator.generateUid() );
        event.setProgram( program.getUid() );
        CategoryOptionCombo coc = new CategoryOptionCombo();
        coc.setUid( CodeGenerator.generateUid() );
        coc.setName( "default " );
        when( attributeOptionComboLoader.getDefault() ).thenReturn( coc );
        Map<String, CategoryOptionCombo> map = subject.get( importOptions, singletonList( event ) );
        CategoryOptionCombo categoryOptionCombo = map.get( event.getUid() );
        assertThat( categoryOptionCombo, is( notNullValue() ) );
        assertThat( categoryOptionCombo.getUid(), is( coc.getUid() ) );
    }

    private void case5( ImportOptions importOptions )
    {
        when( programSupplier.get( eq( importOptions ), anyList() ) ).thenReturn( programMap );
        event.setAttributeCategoryOptions( "abcde;fghilm" );
        CategoryOptionCombo coc = new CategoryOptionCombo();
        coc.setUid( CodeGenerator.generateUid() );
        when( attributeOptionComboLoader.getCategoryOptionCombo(
            importOptions.getIdSchemes().getCategoryOptionComboIdScheme(), event.getAttributeOptionCombo() ) )
                .thenReturn( coc );
        Map<String, CategoryOptionCombo> map = subject.get( importOptions, singletonList( event ) );
        CategoryOptionCombo categoryOptionCombo = map.get( event.getUid() );
        assertThat( categoryOptionCombo, is( notNullValue() ) );
        assertThat( categoryOptionCombo.getUid(), is( coc.getUid() ) );
    }

    private void case6( ImportOptions importOptions )
    {
        when( programSupplier.get( eq( importOptions ), anyList() ) ).thenReturn( programMap );
        when( programStageInstanceSupplier.get( eq( importOptions ), anyList() ) )
            .thenReturn( programStageInstanceMap );
        when( attributeOptionComboLoader.getCategoryOptionCombo(
            importOptions.getIdSchemes().getCategoryOptionComboIdScheme(),
            programStageInstance.getAttributeOptionCombo().getUid() ) ).thenReturn( coc );
        org.hisp.dhis.dxf2.deprecated.tracker.event.Event eventWithoutAoc = new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
        eventWithoutAoc.setUid( "psuid1" );
        eventWithoutAoc.setProgram( program.getUid() );
        Map<String, CategoryOptionCombo> map = subject.get( importOptions, singletonList( eventWithoutAoc ) );
        CategoryOptionCombo categoryOptionCombo = map.get( eventWithoutAoc.getUid() );
        assertThat( categoryOptionCombo, is( notNullValue() ) );
        assertThat( categoryOptionCombo.getUid(), is( coc.getUid() ) );
    }
}
