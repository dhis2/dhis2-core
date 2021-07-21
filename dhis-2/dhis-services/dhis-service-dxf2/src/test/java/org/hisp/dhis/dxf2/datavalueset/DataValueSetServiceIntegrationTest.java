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
package org.hisp.dhis.dxf2.datavalueset;

import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.hisp.dhis.DhisTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.importsummary.ImportConflicts;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.mock.MockCurrentUserService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class DataValueSetServiceIntegrationTest
    extends DhisTest
{
    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private DataValueSetService dataValueSetService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService _userService;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private PeriodType ptA;

    private DataSet dsA;

    private Period peA;

    private Period peB;

    private Period peC;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    private OrganisationUnit ouC;

    private User user;

    private InputStream in;

    @Override
    public void setUpTest()
    {
        userService = _userService;
        deA = createDataElement( 'A' );
        deB = createDataElement( 'B' );
        deC = createDataElement( 'C' );

        deA.setUid( "f7n9E0hX8qk" );
        deB.setUid( "Ix2HsbDMLea" );
        deC.setUid( "eY5ehpbEsB7" );

        idObjectManager.save( deA );
        idObjectManager.save( deB );
        idObjectManager.save( deC );

        ptA = new MonthlyPeriodType();

        dsA = createDataSet( 'A', ptA );
        dsA.setUid( "pBOMPrpg1QX" );
        dataSetService.addDataSet( dsA );

        peA = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 1, 1 ),
            getDate( 2012, 1, 31 ) );
        peB = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 2, 1 ),
            getDate( 2012, 2, 29 ) );
        peC = createPeriod( PeriodType.getByNameIgnoreCase( MonthlyPeriodType.NAME ), getDate( 2012, 3, 1 ),
            getDate( 2012, 3, 31 ) );

        periodService.addPeriod( peA );
        periodService.addPeriod( peB );
        periodService.addPeriod( peC );

        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
        ouC = createOrganisationUnit( 'C' );

        ouA.setUid( "DiszpKrYNg8" );
        ouB.setUid( "BdfsJfj87js" );
        ouC.setUid( "j7Hg26FpoIa" );

        idObjectManager.save( ouA );
        idObjectManager.save( ouB );
        idObjectManager.save( ouC );

        user = createAndInjectAdminUser();

        user.setOrganisationUnits( Sets.newHashSet( ouA, ouB, ouC ) );
        CurrentUserService currentUserService = new MockCurrentUserService( user );
        setDependency( dataValueSetService, "currentUserService", currentUserService );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Import 3 data values, then delete 3 data values.
     */
    @Test
    public void testImportDeleteValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetA.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 3, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 3, dataValueService.getAllDataValues().size() );

        // Delete values

        in = new ClassPathResource( "datavalueset/dataValueSetADeleted.xml" ).getInputStream();

        summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 3, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 0, dataValueService.getAllDataValues().size() );
    }

    /**
     * Import 12 data values.
     */
    @Test
    public void testImportValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 12, dataValueService.getAllDataValues().size() );
    }

    /**
     * Import 12 data values. Then import 6 data values, where 4 are updates.
     */
    @Test
    public void testImportUpdateValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );

        assertEquals( 12, dataValueService.getAllDataValues().size() );

        // Update

        in = new ClassPathResource( "datavalueset/dataValueSetBUpdate.xml" ).getInputStream();

        summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 4, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 14, dataValueService.getAllDataValues().size() );
    }

    /**
     * When updating a data value with a specified created date, the specified
     * created date should be used.
     *
     * When updating a data value without a specified created date, the existing
     * created date should remain unchanged.
     */
    @Test
    public void testUpdateCreatedDate()
        throws Exception
    {
        // Insert:
        // deC, peA, ouA created = 2010-01-01
        // deC, peA, ouB created = 2010-01-01

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        dataValueSetService.saveDataValueSet( in );

        // Update:
        // deC, peA, ouA created = not specified, should remain unchanged
        // deC, peA, ouB created = 2020-02-02

        in = new ClassPathResource( "datavalueset/dataValueSetBUpdate.xml" ).getInputStream();

        dataValueSetService.saveDataValueSet( in );

        CategoryOptionCombo cc = categoryService.getDefaultCategoryOptionCombo();

        DataValue dv1 = dataValueService.getDataValue( deC, peA, ouA, cc, cc );
        assertEquals( "2010-01-01", getMediumDateString( dv1.getCreated() ) );

        DataValue dv2 = dataValueService.getDataValue( deC, peA, ouB, cc, cc );
        assertEquals( "2020-02-02", getMediumDateString( dv2.getCreated() ) );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Deleted values
     * should count as imports when there are no existing non-deleted matching
     * values.
     */
    @Test
    public void testImportDeletedValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetBDeleted.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 8, dataValueService.getAllDataValues().size() );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Then import 12 data
     * values which reverse deletion of the 4 values and update the other 8
     * values.
     */
    @Test
    public void testImportReverseDeletedValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetBDeleted.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );

        assertEquals( 8, dataValueService.getAllDataValues().size() );

        // Reverse deletion and update

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 4, summary.getImportCount().getImported() );
        assertEquals( 8, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 12, dataValueService.getAllDataValues().size() );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Then import 12 data
     * values which reverse deletion of the 4 values, update 4 values and add 4
     * values.
     */
    @Test
    public void testImportAddAndReverseDeletedValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetBDeleted.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );

        assertEquals( 8, dataValueService.getAllDataValues().size() );

        // Reverse deletion and update

        in = new ClassPathResource( "datavalueset/dataValueSetBNew.xml" ).getInputStream();

        summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 8, summary.getImportCount().getImported() );
        assertEquals( 4, summary.getImportCount().getUpdated() );
        assertEquals( 0, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 16, dataValueService.getAllDataValues().size() );
    }

    /**
     * Import 12 data values. Then import 12 values where 4 are marked as
     * deleted.
     */
    @Test
    public void testDeleteValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );

        assertEquals( 12, dataValueService.getAllDataValues().size() );

        // Delete 4 values

        in = new ClassPathResource( "datavalueset/dataValueSetBDeleted.xml" ).getInputStream();

        summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( 8, summary.getImportCount().getUpdated() );
        assertEquals( 4, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 8, dataValueService.getAllDataValues().size() );
    }

    /**
     * Import 12 data values. Then import 12 values where 4 are marked as
     * deleted, 6 are updates and 2 are new.
     */
    @Test
    public void testImportAndDeleteValuesXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );

        assertEquals( 12, dataValueService.getAllDataValues().size() );

        // Delete 4 values, add 2 values

        in = new ClassPathResource( "datavalueset/dataValueSetBNewDeleted.xml" ).getInputStream();

        summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 2, summary.getImportCount().getImported() );
        assertEquals( 6, summary.getImportCount().getUpdated() );
        assertEquals( 4, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 10, dataValueService.getAllDataValues().size() );
    }

    /**
     * Import 12 data values. Then import the same 12 data values with import
     * strategy delete.
     */
    @Test
    public void testImportValuesDeleteStrategyXml()
        throws Exception
    {
        assertEquals( 0, dataValueService.getAllDataValues().size() );

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportSummary summary = dataValueSetService.saveDataValueSet( in );

        assertEquals( 12, summary.getImportCount().getImported() );

        assertEquals( 12, dataValueService.getAllDataValues().size() );

        // Import with delete strategy

        in = new ClassPathResource( "datavalueset/dataValueSetB.xml" ).getInputStream();

        ImportOptions options = new ImportOptions()
            .setStrategy( ImportStrategy.DELETE );

        summary = dataValueSetService.saveDataValueSet( in, options );

        assertEquals( 0, summary.getImportCount().getImported() );
        assertEquals( 0, summary.getImportCount().getUpdated() );
        assertEquals( 12, summary.getImportCount().getDeleted() );
        assertHasNoConflicts( summary );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );

        assertEquals( 0, dataValueService.getAllDataValues().size() );
    }

    private static void assertHasNoConflicts( ImportConflicts summary )
    {
        if ( summary.hasConflicts() )
        {
            assertEquals( summary.getConflictsDescription(), 0, summary.getConflictCount() );
        }
    }
}
