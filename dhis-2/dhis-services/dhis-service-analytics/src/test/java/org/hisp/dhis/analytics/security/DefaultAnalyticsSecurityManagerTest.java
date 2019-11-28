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

package org.hisp.dhis.analytics.security;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hisp.dhis.common.DataDimensionType.ATTRIBUTE;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Lists;

public class DefaultAnalyticsSecurityManagerTest
{

    @Mock
    private AclService aclService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DefaultAnalyticsSecurityManager defaultAnalyticsSecurityManager;

    @Before
    public void setUp()
    {
        initMocks( this );
    }

    @Test
    public void testExcludeNonAuthorizedCategoryOptionsWhenOneCategoryOptionIsNotAllowed()
    {
        // Given
        final CategoryOption aCategoryOptionNotAllowed = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption aCategoryOptionAllowed = stubCategoryOption( "cat-option-B", "uid-opt-B" );

        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A",
            newArrayList( aCategoryOptionNotAllowed, aCategoryOptionAllowed ) );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B",
            newArrayList( aCategoryOptionNotAllowed, aCategoryOptionAllowed ) );
        final List<Category> categories = newArrayList( aCategoryA, aCategoryB );

        final boolean canDataReadFalse = false;
        final boolean canDataReadTrue = true;
        final User stubUser = new User();

        // When
        when( currentUserService.getCurrentUser() ).thenReturn( stubUser );
        when( aclService.canDataRead( currentUserService.getCurrentUser(), aCategoryOptionNotAllowed ) )
            .thenReturn( canDataReadFalse );
        when( aclService.canDataRead( currentUserService.getCurrentUser(), aCategoryOptionAllowed ) )
            .thenReturn( canDataReadTrue );
        defaultAnalyticsSecurityManager.excludeNonAuthorizedCategoryOptions( categories );

        // Then
        assertThat( aCategoryA.getCategoryOptions(), hasItems( aCategoryOptionAllowed ) );
        assertThat( aCategoryB.getCategoryOptions(), hasItems( aCategoryOptionAllowed ) );
        assertThat( aCategoryA.getCategoryOptions(), not( hasItems( aCategoryOptionNotAllowed ) ) );
        assertThat( aCategoryB.getCategoryOptions(), not( hasItems( aCategoryOptionNotAllowed ) ) );
    }

    @Test
    public void testExcludeNonAuthorizedCategoryOptionsWhenAllCategoryOptionsAreAllowed()
    {
        // Given
        final CategoryOption aCategoryOptionAllowed = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption anotherCategoryOptionAllowed = stubCategoryOption( "cat-option-B", "uid-opt-B" );

        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A",
            newArrayList( aCategoryOptionAllowed, anotherCategoryOptionAllowed ) );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B",
            newArrayList( aCategoryOptionAllowed, anotherCategoryOptionAllowed ) );
        final List<Category> categories = newArrayList( aCategoryA, aCategoryB );

        final boolean canDataReadTrue = true;
        final User aStubUser = new User();

        // When
        when( currentUserService.getCurrentUser() ).thenReturn( aStubUser );
        when( aclService.canDataRead( currentUserService.getCurrentUser(), aCategoryOptionAllowed ) )
            .thenReturn( canDataReadTrue );
        when( aclService.canDataRead( currentUserService.getCurrentUser(), anotherCategoryOptionAllowed ) )
            .thenReturn( canDataReadTrue );
        defaultAnalyticsSecurityManager.excludeNonAuthorizedCategoryOptions( categories );

        // Then
        assertThat( aCategoryA.getCategoryOptions(), hasItems( aCategoryOptionAllowed, anotherCategoryOptionAllowed ) );
        assertThat( aCategoryB.getCategoryOptions(), hasItems( aCategoryOptionAllowed, anotherCategoryOptionAllowed ) );
    }

    @Test
    public void testDecideAccessDataReadObjects()
    {
        // Given
        final CategoryOption aCategoryOptionA = stubCategoryOption( "cat-option-A", "uid-opt-A" );
        final CategoryOption aCategoryOptionB = stubCategoryOption( "cat-option-B", "uid-opt-B" );

        final Category aCategoryA = stubCategory( "cat-A", "uid-cat-A",
            newArrayList( aCategoryOptionA, aCategoryOptionB ) );
        final Category aCategoryB = stubCategory( "cat-B", "uid-cat-B",
            newArrayList( aCategoryOptionA, aCategoryOptionB ) );

        final DataQueryParams theEventQueryParams = stubEventQueryParamsWithProgramCategoryCombo(
            newArrayList( aCategoryA, aCategoryB ) );
        final User aStubUser = new User();
        final boolean canDataReadTrue = true;

        // When
        when( currentUserService.getCurrentUser() ).thenReturn( aStubUser );
        when( aclService.canDataRead( currentUserService.getCurrentUser(), aCategoryA ) ).thenReturn( canDataReadTrue );
        when( aclService.canDataRead( currentUserService.getCurrentUser(), aCategoryB ) ).thenReturn( canDataReadTrue );
        when( aclService.canDataRead( currentUserService.getCurrentUser(), theEventQueryParams.getProgram() ) )
            .thenReturn( canDataReadTrue );
        defaultAnalyticsSecurityManager.decideAccessDataReadObjects( theEventQueryParams, aStubUser );

        // Then
        final int oneTimeForEachCategoryOptionIteration = 2;
        verify( aclService, times( oneTimeForEachCategoryOptionIteration ) ).canDataRead( aStubUser, aCategoryOptionA );
        verify( aclService, times( oneTimeForEachCategoryOptionIteration ) ).canDataRead( aStubUser, aCategoryOptionB );
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
        categoryOption.setShortName( "short-" + name );
        categoryOption.setDescription( "description-" + name );
        categoryOption.setCode( "code-" + uid );
        return categoryOption;
    }

    private DataQueryParams stubEventQueryParamsWithProgramCategoryCombo( final List<Category> categories )
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
