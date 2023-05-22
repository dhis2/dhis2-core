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
package org.hisp.dhis.webapi.controller.tracker.export;

import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils.parseQueryItem;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests
 * {@link org.hisp.dhis.webapi.controller.tracker.export.RequestParamUtils}.
 */
class RequestParamUtilsTest
{

    private static final String TEA_1_UID = "TvjwTPToKHO";

    private static final String TEA_2_UID = "cy2oRh2sNr6";

    private Map<String, TrackedEntityAttribute> attributes;

    @BeforeEach
    void setUp()
    {
        attributes = Map.of(
            TEA_1_UID, trackedEntityAttribute( TEA_1_UID ),
            TEA_2_UID, trackedEntityAttribute( TEA_2_UID ) );
    }

    @Test
    void testParseQueryItem()
        throws BadRequestException
    {
        String param = TEA_1_UID + ":lt:20:gt:10";

        QueryItem item = parseQueryItem( param, id -> new QueryItem( attributes.get( id ) ) );

        assertNotNull( item );
        assertAll(
            () -> assertEquals( attributes.get( TEA_1_UID ), item.getItem() ),
            // QueryItem equals() does not take the QueryFilter into account, so
            // we need to assert on filters separately
            () -> assertEquals( List.of(
                new QueryFilter( QueryOperator.LT, "20" ),
                new QueryFilter( QueryOperator.GT, "10" ) ), item.getFilters() ) );
    }

    @Test
    void testParseQueryItemWithOnlyIdentifier()
        throws BadRequestException
    {
        QueryItem item = parseQueryItem( TEA_1_UID, id -> new QueryItem( attributes.get( id ) ) );

        assertNotNull( item );
        assertAll(
            () -> assertEquals( attributes.get( TEA_1_UID ), item.getItem() ),
            () -> assertIsEmpty( item.getFilters() ) );
    }

    @Test
    void testParseQueryItemWithIdentifierAndTrailingColon()
        throws BadRequestException
    {
        String param = TEA_1_UID + ":";

        QueryItem item = parseQueryItem( param, id -> new QueryItem( attributes.get( id ) ) );

        assertNotNull( item );
        assertAll(
            () -> assertEquals( attributes.get( TEA_1_UID ), item.getItem() ),
            () -> assertIsEmpty( item.getFilters() ) );
    }

    @Test
    void testParseQueryItemWithMissingValue()
    {
        String param = TEA_1_UID + ":lt";

        Exception exception = assertThrows( BadRequestException.class,
            () -> parseQueryItem( param, id -> new QueryItem( attributes.get( id ) ) ) );
        assertEquals( "Query item or filter is invalid: " + param, exception.getMessage() );
    }

    @Test
    void testParseQueryItemWithMissingValueAndTrailingColon()
    {
        String param = TEA_1_UID + ":lt:";

        Exception exception = assertThrows( BadRequestException.class,
            () -> parseQueryItem( param, id -> new QueryItem( attributes.get( id ) ) ) );
        assertEquals( "Query item or filter is invalid: " + param, exception.getMessage() );
    }

    @Test
    void testParseAttributeQueryItemWhenNoTEAExist()
    {
        String param = TEA_1_UID + ":eq:2";
        Map<String, TrackedEntityAttribute> attributes = Collections.emptyMap();

        Exception exception = assertThrows( BadRequestException.class,
            () -> RequestParamUtils.parseAttributeQueryItem( param, attributes ) );
        assertEquals( "Attribute does not exist: " + TEA_1_UID, exception.getMessage() );
    }

    @Test
    void testParseAttributeQueryWhenTEAInFilterDoesNotExist()
    {
        String param = "JM5zWuf1mkb:eq:2";

        Exception exception = assertThrows( BadRequestException.class,
            () -> RequestParamUtils.parseAttributeQueryItem( param, attributes ) );
        assertEquals( "Attribute does not exist: JM5zWuf1mkb", exception.getMessage() );
    }

    @Test
    void shouldCreateQueryFiltersWhenQueryHasOperatorAndValueWithDelimiter()
        throws BadRequestException
    {
        assertEquals( new QueryFilter( QueryOperator.LIKE, "project:x" ),
            RequestParamUtils.parseQueryFilter( "like:project\\:x" ) );
    }

    private TrackedEntityAttribute trackedEntityAttribute( String uid )
    {
        TrackedEntityAttribute tea = new TrackedEntityAttribute();
        tea.setUid( uid );
        return tea;
    }
}