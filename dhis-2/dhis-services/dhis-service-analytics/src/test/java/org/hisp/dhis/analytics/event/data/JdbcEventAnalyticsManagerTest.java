/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.junit.Assert.assertThat;
import static org.mockito.junit.MockitoJUnit.rule;

import java.util.List;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.common.collect.Lists;

public class JdbcEventAnalyticsManagerTest
{

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private StatementBuilder statementBuilder;

    @Mock
    private ProgramIndicatorService programIndicatorService;

    @Rule
    public MockitoRule mockitoRule = rule();

    private JdbcEventAnalyticsManager jdbcEventAnalyticsManager;

    @Before
    public void setUp()
    {
        jdbcEventAnalyticsManager = new JdbcEventAnalyticsManager( jdbcTemplate, statementBuilder,
            programIndicatorService );
    }

    @Test
    public void testWhereClauseWhenThereAreNoDimensionsAndOneCategory()
    {
        // Given
        final CategoryOption aCategoryOptionA = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption aCategoryOptionB = stubCategoryOption( "cat-option-B", "uid-opt-B" );

        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A",
            newArrayList( aCategoryOptionA, aCategoryOptionB ) );

        final EventQueryParams theEventQueryParams = stubEventQueryParamsWithoutDimensions(
            newArrayList( aCategoryA ) );

        final String theExpectedSql = " and ax.\"uid-cat-A\" in ('uid-opt-A', 'uid-opt-B') ";

        // When
        final String actualSql = jdbcEventAnalyticsManager.getWhereClause( theEventQueryParams );

        // Then
        assertThat( actualSql, containsString( theExpectedSql ) );
    }

    @Test
    public void testWhereClauseWhenThereAreNoDimensionsAndMultipleCategories()
    {
        // Given
        final CategoryOption aCategoryOptionA = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption aCategoryOptionB = stubCategoryOption( "cat-option-B", "uid-opt-B" );

        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A",
            newArrayList( aCategoryOptionA, aCategoryOptionB ) );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B",
            newArrayList( aCategoryOptionA, aCategoryOptionB ) );

        final EventQueryParams theEventQueryParams = stubEventQueryParamsWithoutDimensions(
            newArrayList( aCategoryA, aCategoryB ) );

        final String theExpectedSql = " and ax.\"uid-cat-A\" in ('uid-opt-A', 'uid-opt-B')  or ax.\"uid-cat-B\" in ('uid-opt-A', 'uid-opt-B') ";

        // When
        final String actualSql = jdbcEventAnalyticsManager.getWhereClause( theEventQueryParams );

        // Then
        assertThat( actualSql, containsString( theExpectedSql ) );
    }

    @Test
    public void testQueryOnlyAllowedCategoryOptionEvents()
    {
        // Given
        final CategoryOption aCategoryOptionA = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption aCategoryOptionB = stubCategoryOption( "cat-option-B", "uid-opt-B" );

        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A",
            newArrayList( aCategoryOptionA, aCategoryOptionB ) );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B",
            newArrayList( aCategoryOptionA, aCategoryOptionB ) );

        final String theExpectedSql = " and ax.\"uid-cat-A\" in ('uid-opt-A', 'uid-opt-B')  or ax.\"uid-cat-B\" in ('uid-opt-A', 'uid-opt-B') ";

        // When
        final String actualSql = jdbcEventAnalyticsManager
            .queryOnlyAllowedCategoryOptionEvents( newArrayList( aCategoryA, aCategoryB ) );

        // Then
        assertThat( actualSql, containsString( theExpectedSql ) );
    }

    @Test
    public void testQueryOnlyAllowedCategoryOptionEventsWithNoCategories()
    {
        // Given
        final List<Category> emptyCategoryList = newArrayList();

        // When
        final String actualSql = jdbcEventAnalyticsManager.queryOnlyAllowedCategoryOptionEvents( emptyCategoryList );

        // Then
        assertThat( actualSql, isEmptyString() );
    }

    @Test
    public void testQueryOnlyAllowedCategoryOptionEventsWithNoCategoryOptions()
    {
        // Given
        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A", newArrayList() );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B", newArrayList() );

        // When
        final String actualSql = jdbcEventAnalyticsManager
            .queryOnlyAllowedCategoryOptionEvents( newArrayList( aCategoryA, aCategoryB ) );

        // Then
        assertThat( actualSql, isEmptyString() );
    }

    private Category stubCategory( final String name, final String uid, final List<CategoryOption> categoryOptions )
    {
        final Category category = new Category( name, ATTRIBUTE );
        category.setCategoryOptions( newArrayList( categoryOptions ) );
        category.setUid( uid );
        return category;
    }

    private CategoryOption stubCategoryOption( final String name, final String uid )
    {
        final CategoryOption categoryOption = new CategoryOption( name );
        categoryOption.setUid( uid );
        return categoryOption;
    }

    private EventQueryParams stubEventQueryParamsWithoutDimensions( final List<Category> categories )
    {
        final DimensionalObject doA = new BaseDimensionalObject( ORGUNIT_DIM_ID, ORGANISATION_UNIT, newArrayList() );
        final DimensionalObject doC = new BaseDimensionalObject( "Cz3WQznvrCM", PROGRAM_ATTRIBUTE, newArrayList() );

        final Period period = PeriodType.getPeriodFromIsoString( "2019Q2" );

        final CategoryCombo categoryCombo = new CategoryCombo( "cat-combo", ATTRIBUTE );
        categoryCombo.setCategories( categories );

        final Program program = new Program( "program", "a program" );
        program.setCategoryCombo( categoryCombo );

        final DataQueryParams dataQueryParams = DataQueryParams.newBuilder().addDimension( doA ).addDimension( doC )
            .withPeriods( Lists.newArrayList( period ) ).withPeriodType( period.getPeriodType().getIsoFormat() )
            .build();

        final EventQueryParams.Builder eventQueryParamsBuilder = new EventQueryParams.Builder( dataQueryParams );
        final EventQueryParams eventQueryParams = eventQueryParamsBuilder.withProgram( program ).build();

        return eventQueryParams;
    }
}
