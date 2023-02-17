/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import lombok.Data;

import org.hisp.dhis.fieldfiltering.FieldFilterParser;
import org.hisp.dhis.fieldfiltering.FieldFilterService;
import org.hisp.dhis.fieldfiltering.FieldPath;
import org.hisp.dhis.fieldfiltering.FieldPathHelper;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;

class FieldFilterServiceTest extends DhisControllerConvenienceTest
{

    @Autowired
    FieldFilterService fieldFilterService;

    @Autowired
    FieldPathHelper fieldPathHelper;

    @Test
    void shouldIncludeAllPathsGivenFilterContainsPresetAll()
    {
        Root root = new Root( new First( new Second( new Third() ) ) );
        List<FieldPath> filter = FieldFilterParser.parse( "*" );

        assertAll( "filterIncludes should match what's included in the filtered JSON",
            () -> assertJSONIncludes( fieldFilterService.toObjectNode( root, filter ), "first.second.third" ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first" ) ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first.second" ) ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first.second.third" ) ) );
    }

    @Test
    void shouldIncludeChildPathsGivenFilterContainsParent()
    {
        Root root = new Root( new First( new Second( new Third() ) ) );
        List<FieldPath> filter = FieldFilterParser.parse( "first" );

        assertAll( "filterIncludes should match what's included in the filtered JSON",
            () -> assertJSONIncludes( fieldFilterService.toObjectNode( root, filter ), "first.second.third" ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first" ) ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first.second" ) ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first.second.third" ) ) );
    }

    @Test
    void shouldExcludePathAsExclusionOverrulesInclusion()
    {
        Root root = new Root( new First( new Second( new Third() ) ) );
        List<FieldPath> filter = FieldFilterParser.parse( "!first,first" );

        assertAll( "filterIncludes should match what's included in the filtered JSON",
            () -> assertJSONExcludes( fieldFilterService.toObjectNode( root, filter ), "first" ),
            () -> assertFalse( fieldFilterService.filterIncludes( Root.class, filter, "first" ) ),
            () -> assertFalse( fieldFilterService.filterIncludes( Root.class, filter, "first.second" ) ),
            () -> assertFalse( fieldFilterService.filterIncludes( Root.class, filter, "first.second.third" ) ) );
    }

    @Test
    void shouldExcludePathGivenFilterContainsExplicitExclusionOfPathDespitePresetAll()
    {
        Root root = new Root( new First( new Second( new Third() ) ) );
        List<FieldPath> filter = FieldFilterParser.parse( "*,first[second[!third]]" );

        assertAll( "filterIncludes should match what's included in the filtered JSON",
            () -> assertJSONIncludes( fieldFilterService.toObjectNode( root, filter ), "first.second" ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first" ) ),
            () -> assertTrue( fieldFilterService.filterIncludes( Root.class, filter, "first.second" ) ),
            () -> assertJSONExcludes( fieldFilterService.toObjectNode( root, filter ), "first.second.third" ),
            () -> assertFalse( fieldFilterService.filterIncludes( Root.class, filter, "first.second.third" ) ) );
    }

    @Test
    void shouldExcludeChildPathGivenFilterContainsExclusionOfAParentDespiteDirectParentBeingIncluded()
    {
        Root root = new Root( new First( new Second( new Third() ) ) );
        List<FieldPath> filter = FieldFilterParser.parse( "!first,first[second]" );

        assertAll( "filterIncludes should match what's included in the filtered JSON",
            () -> assertJSONExcludes( fieldFilterService.toObjectNode( root, filter ), "first" ),
            () -> assertFalse( fieldFilterService.filterIncludes( Root.class, filter, "first" ) ),
            () -> assertFalse( fieldFilterService.filterIncludes( Root.class, filter, "first.second" ) ),
            () -> assertFalse( fieldFilterService.filterIncludes( Root.class, filter, "first.second.third" ) ) );
    }

    void assertJSONIncludes( ObjectNode json, String path )
    {
        String jsonPtr = toJSONPointer( path );
        assertFalse( json.at( jsonPtr ).isMissingNode(),
            () -> String.format( "Path '%s' (JSON ptr '%s') not found in JSON %s", path, jsonPtr, json ) );
    }

    void assertJSONExcludes( ObjectNode json, String path )
    {
        String jsonPtr = toJSONPointer( path );
        assertTrue( json.at( jsonPtr ).isMissingNode(),
            () -> String.format( "Path '%s' (JSON ptr '%s') found in JSON %s", path, jsonPtr, json ) );
    }

    private static String toJSONPointer( String path )
    {
        return "/" + path.replace( ".", "/" );
    }

    /**
     * Sample classes used to build nested JSON we can filter using the
     * {@link FieldFilterService}.
     */
    @Data
    private static class Root
    {
        @JsonProperty
        private final First first;

    }

    @Data
    private static class First
    {
        @JsonProperty
        private final Second second;
    }

    @Data
    private static class Second
    {
        @JsonProperty
        private final Third third;
    }

    @Data
    private static class Third
    {
        @JsonProperty
        private final String value = "3";
    }
}