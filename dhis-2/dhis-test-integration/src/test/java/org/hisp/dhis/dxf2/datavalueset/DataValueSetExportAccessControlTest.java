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
package org.hisp.dhis.dxf2.datavalueset;

import static org.hisp.dhis.security.acl.AccessStringHelper.DATA_READ;
import static org.hisp.dhis.security.acl.AccessStringHelper.DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
@Order( 1 )
class DataValueSetExportAccessControlTest extends TransactionalIntegrationTest
{
    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private UserService _userService;

    @Autowired
    private ObjectMapper jsonMapper;

    private DataElement deA;

    private DataElement deB;

    private CategoryOption coA;

    private CategoryOption coB;

    private CategoryOption coC;

    private CategoryOption coD;

    private Category caA;

    private Category caB;

    private CategoryCombo ccA;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private CategoryOptionCombo cocC;

    private CategoryOptionCombo cocD;

    private DataSet dsA;

    private Period peA;

    private OrganisationUnit ouA;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        createAndInjectAdminUser();

        // Metadata
        PeriodType ptA = periodService.getPeriodTypeByName( MonthlyPeriodType.NAME );
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        idObjectManager.save( Lists.newArrayList( deA, deB ) );
        coA = createCategoryOption( 'A' );
        coA.getSharing().setPublicAccess( DEFAULT );
        coB = createCategoryOption( 'B' );
        coB.getSharing().setPublicAccess( DEFAULT );
        coC = createCategoryOption( 'C' );
        coC.getSharing().setPublicAccess( DEFAULT );
        coD = createCategoryOption( 'D' );
        coD.getSharing().setPublicAccess( DEFAULT );
        idObjectManager.save( coA, false );
        idObjectManager.save( coB, false );
        idObjectManager.save( coC, false );
        idObjectManager.save( coD, false );
        caA = createCategory( 'A', coA, coB );
        caB = createCategory( 'B', coC, coD );
        idObjectManager.save( Lists.newArrayList( caA, caB ) );
        ccA = createCategoryCombo( 'A', caA, caB );
        idObjectManager.save( ccA );
        cocA = createCategoryOptionCombo( ccA, coA, coC );
        cocB = createCategoryOptionCombo( ccA, coA, coD );
        cocC = createCategoryOptionCombo( ccA, coB, coC );
        cocD = createCategoryOptionCombo( ccA, coB, coD );
        idObjectManager.save( Lists.newArrayList( cocA, cocB, cocC, cocD ) );
        dsA = createDataSet( 'A', ptA, ccA );
        dsA.getSharing().setPublicAccess( DEFAULT );
        dsA.addDataSetElement( deA );
        dsA.addDataSetElement( deB );
        idObjectManager.save( dsA, false );
        peA = createPeriod( "201901" );
        idObjectManager.save( peA );
        ouA = createOrganisationUnit( 'A' );
        idObjectManager.save( ouA );
        // Data values
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocA, "1" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocB, "2" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocC, "3" ) );
        dataValueService.addDataValue( new DataValue( deA, peA, ouA, cocA, cocD, "4" ) );
    }

    /**
     * User has data read sharing access to cocA and coCB through category
     * options. Verifies that only data values for those attribute option
     * combinations are returned.
     */
    @Test
    void testExportAttributeOptionComboAccessLimitedUserA()
        throws IOException
    {
        // User
        User user = makeUser( "A" );
        userService.addUser( user );
        user.setOrganisationUnits( Sets.newHashSet( ouA ) );
        // Sharing
        enableDataSharing( user, coA, DATA_READ );
        enableDataSharing( user, coC, DATA_READ );
        enableDataSharing( user, coD, DATA_READ );
        enableDataSharing( user, dsA, DATA_READ );
        idObjectManager.update( coA );
        idObjectManager.update( coC );
        idObjectManager.update( coD );
        idObjectManager.update( dsA );

        User user1 = userService.getUser( user.getUid() );
        injectSecurityContext( user1 );

        // Test
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExportParams params = new DataExportParams().setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA ) ).setOrganisationUnits( Sets.newHashSet( ouA ) );
        dbmsManager.flushSession();
        dataValueSetService.exportDataValueSetJson( params, out );
        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );
        Set<String> expectedOptionCombos = Sets.newHashSet( cocA.getUid(), cocB.getUid() );
        assertNotNull( dvs );
        assertNotNull( dvs.getDataValues() );
        assertEquals( 2, dvs.getDataValues().size() );
        for ( org.hisp.dhis.dxf2.datavalue.DataValue dv : dvs.getDataValues() )
        {
            assertNotNull( dv );
            assertEquals( ouA.getUid(), dv.getOrgUnit() );
            assertEquals( peA.getUid(), dv.getPeriod() );
            assertTrue( expectedOptionCombos.contains( dv.getAttributeOptionCombo() ) );
        }
    }

    /**
     * User is super user. Verifies that no restriction on attribute option
     * combinations are used.
     */
    @Test
    void testExportAttributeOptionComboAccessSuperUser()
        throws IOException
    {
        User adminUser = makeUser( "A", Lists.newArrayList( "ALL" ) );
        adminUser.setOrganisationUnits( Sets.newHashSet( ouA ) );
        setCurrentUser( adminUser );

        enableDataSharing( adminUser, coA, DATA_READ );
        enableDataSharing( adminUser, coB, DATA_READ );
        idObjectManager.update( coA );
        idObjectManager.update( coB );
        // Test
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExportParams params = new DataExportParams().setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA ) ).setOrganisationUnits( Sets.newHashSet( ouA ) );
        dbmsManager.flushSession();
        dataValueSetService.exportDataValueSetJson( params, out );
        DataValueSet dvs = jsonMapper.readValue( out.toByteArray(), DataValueSet.class );
        assertNotNull( dvs );
        assertNotNull( dvs.getDataSet() );
        assertEquals( 4, dvs.getDataValues().size() );
    }

    /**
     * User does not have data read sharing access to data set. Verifies that
     * validation fails.
     */
    @Test
    void testExportDataSetAccess()
    {
        User user = makeUser( "A" );
        userService.addUser( user );
        user.setOrganisationUnits( Sets.newHashSet( ouA ) );
        enableDataSharing( user, coA, DATA_READ );
        enableDataSharing( user, coC, DATA_READ );
        idObjectManager.update( coA );
        idObjectManager.update( coC );
        idObjectManager.update( coD );
        idObjectManager.update( dsA );

        User user1 = userService.getUser( user.getUid() );
        injectSecurityContext( user1 );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExportParams params = new DataExportParams().setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA ) ).setOrganisationUnits( Sets.newHashSet( ouA ) );
        dbmsManager.flushSession();
        assertThrows( IllegalQueryException.class, () -> dataValueSetService.exportDataValueSetJson( params, out ) );
    }

    /**
     * User has no data read sharing access to cocA through category options.
     * Verifies that validation fails.
     */
    @Test
    void testExportExplicitAttributeOptionComboAccess()
    {
        User user = makeUser( "A" );
        userService.addUser( user );
        user.setOrganisationUnits( Sets.newHashSet( ouA ) );
        enableDataSharing( user, coA, DATA_READ );
        enableDataSharing( user, dsA, DATA_READ );
        idObjectManager.update( coA );
        idObjectManager.update( dsA );
        dbmsManager.flushSession();

        User user1 = userService.getUser( user.getUid() );
        injectSecurityContext( user1 );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExportParams params = new DataExportParams().setDataSets( Sets.newHashSet( dsA ) )
            .setPeriods( Sets.newHashSet( peA ) ).setOrganisationUnits( Sets.newHashSet( ouA ) )
            .setAttributeOptionCombos( Sets.newHashSet( cocA ) );
        assertThrows( IllegalQueryException.class, () -> dataValueSetService.exportDataValueSetJson( params, out ) );
    }

    /**
     * Inject current user in relevant services.
     *
     * @param user the user to inject.
     */
    private void setCurrentUser( User user )
    {
        userService.addUser( user );
        injectSecurityContext( user );
    }
}
