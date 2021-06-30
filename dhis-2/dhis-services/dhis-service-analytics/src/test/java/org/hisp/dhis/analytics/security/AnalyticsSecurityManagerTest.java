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
package org.hisp.dhis.analytics.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class AnalyticsSecurityManagerTest
    extends DhisSpringTest
{
    @Autowired
    private AnalyticsSecurityManager securityManager;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private CategoryOption coA;

    private CategoryOption coB;

    private Category caA;

    private DataElement deA;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private Set<OrganisationUnit> userOrgUnits;

    @Override
    public void setUpTest()
    {
        coA = createCategoryOption( 'A' );
        coB = createCategoryOption( 'B' );

        categoryService.addCategoryOption( coA );
        categoryService.addCategoryOption( coB );

        caA = createCategory( 'A', coA, coB );

        categoryService.addCategory( caA );

        Set<Category> catDimensionConstraints = Sets.newHashSet( caA );

        deA = createDataElement( 'A' );

        dataElementService.addDataElement( deA );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B', ouA );
        ouC = createOrganisationUnit( 'C', ouB );

        organisationUnitService.addOrganisationUnit( ouA );
        organisationUnitService.addOrganisationUnit( ouB );
        organisationUnitService.addOrganisationUnit( ouC );

        userOrgUnits = Sets.newHashSet( ouB, ouC );

        userService = (UserService) getBean( UserService.ID );

        createUserAndInjectSecurityContext( userOrgUnits, userOrgUnits, catDimensionConstraints, false,
            "F_VIEW_EVENT_ANALYTICS" );
    }

    @Test
    public void testDecideAccessGranted()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .withPeriods( Lists.newArrayList( createPeriod( "201801" ), createPeriod( "201802" ) ) )
            .withOrganisationUnits( Lists.newArrayList( ouB, ouC ) )
            .build();

        securityManager.decideAccess( params );
    }

    @Test( expected = IllegalQueryException.class )
    public void testDecideAccessDenied()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .withPeriods( Lists.newArrayList( createPeriod( "201801" ), createPeriod( "201802" ) ) )
            .withOrganisationUnits( Lists.newArrayList( ouA, ouB ) )
            .build();

        securityManager.decideAccess( params );
    }

    @Test
    public void testWithUserConstraintsDataQueryParams()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( deA ) )
            .withPeriods( Lists.newArrayList( createPeriod( "201801" ), createPeriod( "201802" ) ) )
            .build();

        params = securityManager.withUserConstraints( params );

        assertEquals( userOrgUnits, Sets.newHashSet( params.getFilterOrganisationUnits() ) );
        assertNotNull( params.getFilter( caA.getDimension() ) );
        assertEquals( caA.getDimension(), params.getFilter( caA.getDimension() ).getDimension() );
    }

    @Test
    public void testWithUserConstraintsAlreadyPresentDataQueryParams()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( deA ) )
            .withPeriods( Lists.newArrayList( createPeriod( "201801" ), createPeriod( "201802" ) ) )
            .withOrganisationUnits( Lists.newArrayList( ouB ) )
            .addFilter(
                new BaseDimensionalObject( caA.getDimension(), DimensionType.CATEGORY, Lists.newArrayList( coA ) ) )
            .build();

        params = securityManager.withUserConstraints( params );

        assertEquals( Lists.newArrayList( ouB ), params.getOrganisationUnits() );
        assertNotNull( params.getFilter( caA.getDimension() ) );
        assertEquals( caA.getDimension(), params.getFilter( caA.getDimension() ).getDimension() );
        assertNotNull( params.getFilter( caA.getDimension() ).getItems().get( 0 ) );
        assertEquals( coA.getDimensionItem(),
            params.getFilter( caA.getDimension() ).getItems().get( 0 ).getDimensionItem() );
    }

    @Test
    public void testWithUserConstraintsEventQueryParams()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( new QueryItem( deA ) )
            .withStartDate( getDate( 2018, 1, 1 ) )
            .withEndDate( getDate( 2018, 4, 1 ) )
            .build();

        params = securityManager.withUserConstraints( params );

        assertEquals( userOrgUnits, Sets.newHashSet( params.getFilterOrganisationUnits() ) );
        assertNotNull( params.getFilter( caA.getDimension() ) );
        assertEquals( caA.getDimension(), params.getFilter( caA.getDimension() ).getDimension() );
    }

    @Test
    public void testWithUserConstraintsAlreadyPresentEventQueryParams()
    {
        EventQueryParams params = new EventQueryParams.Builder()
            .addItem( new QueryItem( deA ) )
            .withStartDate( getDate( 2018, 1, 1 ) )
            .withEndDate( getDate( 2018, 4, 1 ) )
            .withOrganisationUnits( Lists.newArrayList( ouB ) )
            .addFilter(
                new BaseDimensionalObject( caA.getDimension(), DimensionType.CATEGORY, Lists.newArrayList( coA ) ) )
            .build();

        params = securityManager.withUserConstraints( params );

        assertEquals( Lists.newArrayList( ouB ), params.getOrganisationUnits() );
        assertNotNull( params.getFilter( caA.getDimension() ) );
        assertEquals( caA.getDimension(), params.getFilter( caA.getDimension() ).getDimension() );
        assertNotNull( params.getFilter( caA.getDimension() ).getItems().get( 0 ) );
        assertEquals( coA.getDimensionItem(),
            params.getFilter( caA.getDimension() ).getItems().get( 0 ).getDimensionItem() );
    }
}
