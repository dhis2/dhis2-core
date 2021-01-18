package org.hisp.dhis.resourcetable.table;

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

import static org.apache.commons.lang.StringUtils.countMatches;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.RandomStringUtils;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOptionGroupSet;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DataDimensionType;
import org.hisp.dhis.indicator.IndicatorGroupSet;
import org.junit.Test;

public class UniqueNameVerifierTest
{
    @Test
    public void verifyResourceTableColumnNameAreUniqueWhenComputingShortName()
    {
        // Category short name will be shorten to 49 chars, and create 3 identical
        // short-names
        String categoryName = RandomStringUtils.randomAlphabetic( 50 );

        String cogsName = RandomStringUtils.randomAlphabetic( 49 );

        final List<Category> categories = IntStream.of( 1, 2, 3 )
            .mapToObj( i -> new Category( categoryName + i, DataDimensionType.ATTRIBUTE ) )
            .peek( c -> c.setUid( CodeGenerator.generateUid() ) ).collect( Collectors.toList() );

        final List<CategoryOptionGroupSet> categoryOptionGroupSets = IntStream.of( 1, 2, 3 )
            .mapToObj( i -> new CategoryOptionGroupSet( cogsName + i ) )
            .peek( c -> c.setUid( CodeGenerator.generateUid() ) ).collect( Collectors.toList() );

        CategoryResourceTable categoryResourceTable = new CategoryResourceTable( categories, categoryOptionGroupSets );

        final String sql = categoryResourceTable.getCreateTempTableStatement();

        assertEquals( countMatches( sql, "\"" + categories.get( 0 ).getShortName() + "\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + categories.get( 0 ).getShortName() + "1\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + categories.get( 0 ).getShortName() + "2\"" ), 1 );

        assertEquals( countMatches( sql, "\"" + cogsName + "1\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + cogsName + "2\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + cogsName + "3\"" ), 1 );

    }

    @Test
    public void verifyResourceTableColumnNameAreUniqueWhenComputingShortName2()
    {
        // Category short name will be shorten to 49 chars, and create 3 identical
        // short-names
        String indicatorGroupSetName = RandomStringUtils.randomAlphabetic( 50 );

        final List<IndicatorGroupSet> indicatorGroupSets = IntStream.of( 1, 2, 3 )
            .mapToObj( i -> new IndicatorGroupSet( indicatorGroupSetName + 1 ) )
            .peek( c -> c.setUid( CodeGenerator.generateUid() ) ).collect( Collectors.toList() );

        IndicatorGroupSetResourceTable indicatorGroupSetResourceTable = new IndicatorGroupSetResourceTable(
            indicatorGroupSets );

        final String sql = indicatorGroupSetResourceTable.getCreateTempTableStatement();

        assertEquals( countMatches( sql, "\"" + indicatorGroupSets.get( 0 ).getName() + "\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + indicatorGroupSets.get( 0 ).getName() + "1\"" ), 1 );
        assertEquals( countMatches( sql, "\"" + indicatorGroupSets.get( 0 ).getName() + "2\"" ), 1 );

    }
}