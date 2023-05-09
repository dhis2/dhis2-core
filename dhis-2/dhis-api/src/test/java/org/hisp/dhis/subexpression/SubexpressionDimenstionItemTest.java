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
package org.hisp.dhis.subexpression;

import static org.hisp.dhis.analytics.AggregationType.AVERAGE;
import static org.hisp.dhis.analytics.AggregationType.MAX;
import static org.hisp.dhis.common.DimensionItemType.SUBEXPRESSION_DIMENSION_ITEM;
import static org.hisp.dhis.subexpression.SubexpressionDimensionItem.getItemColumnName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Set;

import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SubexpressionDimensionItem}.
 *
 * @author Jim Grace
 */
class SubexpressionDimenstionItemTest
{
    @Test
    void testConstructor()
    {
        String subexSql = "dummy SQL";
        Set<DimensionalItemObject> items = Set.of( new DataElement( "DE Name" ) );
        QueryModifiers queryMods = QueryModifiers.builder().aggregationType( AVERAGE ).build();

        SubexpressionDimensionItem target = new SubexpressionDimensionItem( subexSql, items, queryMods );

        assertEquals( subexSql, target.getSubexSql() );
        assertEquals( items, target.getItems() );
        assertEquals( queryMods, target.getQueryMods() );
        assertNotNull( target.getUid() );
        assertEquals( 11, target.getUid().length() );
        assertEquals( AVERAGE, target.getAggregationType() );
        assertEquals( SUBEXPRESSION_DIMENSION_ITEM, target.getDimensionItemType() );
    }

    @Test
    void testGetItemColumnName()
    {
        // Test for coc and aoc = null when missing
        assertEquals( "\"de\"", getItemColumnName( "de", null, null, null ) );
        assertEquals( "\"de_co\"", getItemColumnName( "de", "co", null, null ) );
        assertEquals( "\"de_co_ao\"", getItemColumnName( "de", "co", "ao", null ) );
        assertEquals( "\"de__ao\"", getItemColumnName( "de", null, "ao", null ) );

        QueryModifiers mods = QueryModifiers.builder().aggregationType( MAX ).build();

        // Test for coc and aoc = empty string when missing
        assertEquals( "\"deMAX\"", getItemColumnName( "de", "", "", mods ) );
        assertEquals( "\"de_coMAX\"", getItemColumnName( "de", "co", "", mods ) );
        assertEquals( "\"de_co_aoMAX\"", getItemColumnName( "de", "co", "ao", mods ) );
        assertEquals( "\"de__aoMAX\"", getItemColumnName( "de", "", "ao", mods ) );
    }
}
