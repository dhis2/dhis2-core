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

import static org.hisp.dhis.util.DateUtils.getMediumDateString;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

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
import org.hisp.dhis.user.CurrentUserServiceTarget;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class DataValueSetServiceIntegrationTest extends DhisTest
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

    @Autowired
    private CurrentUserService currentUserService;

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
        setDependency( CurrentUserServiceTarget.class, CurrentUserServiceTarget::setCurrentUserService,
            currentUserService, dataValueSetService );
    }

    @Override
    public void tearDownTest()
    {
        setDependency( CurrentUserServiceTarget.class, CurrentUserServiceTarget::setCurrentUserService,
            currentUserService, dataValueSetService );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    /**
     * Import 1 data value.
     */
    @Test
    void testImportValueJson()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );
    }

    /**
     * Import 1 data value, then delete it by using import mode DELETE
     */
    @Test
    void testImportDeleteValueJson()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );
        ImportOptions options = ImportOptions.getDefaultImportOptions();
        options.setImportStrategy( ImportStrategy.DELETE );
        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ), options ) );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDeleteValueJson_OmittingValue()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );
        ImportOptions options = ImportOptions.getDefaultImportOptions();
        options.setImportStrategy( ImportStrategy.DELETE );
        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1, dataValueSetService
            .importDataValueSetJson( readFile( "datavalueset/dataValueSetJDeleteNoValue.json" ), options ) );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDeleteValueJson_NewValue()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );
        ImportOptions options = ImportOptions.getDefaultImportOptions();
        options.setImportStrategy( ImportStrategy.DELETE );
        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1, dataValueSetService
            .importDataValueSetJson( readFile( "datavalueset/dataValueSetJDeleteNewValue.json" ), options ) );
        assertDataValuesCount( 0 );
    }

    @Test
    void testImportDeleteValueJson_ZeroValue()
    {
        assertDataValuesCount( 0 );
        assertSuccessWithImportedUpdatedDeleted( 1, 0, 0,
            dataValueSetService.importDataValueSetJson( readFile( "datavalueset/dataValueSetJ.json" ) ) );
        assertDataValuesCount( 1 );
        assertSuccessWithImportedUpdatedDeleted( 0, 0, 1,
            dataValueSetService
                .importDataValueSetJson( readFile( "datavalueset/dataValueSetJDeleteZeroValue.json" ) ) );
        assertDataValuesCount( 0 );
    }

    /**
     * Import 3 data values, then delete 3 data values.
     */
    @Test
    void testImportDeleteValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetA.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 3, 0, 0, summary );
        assertDataValuesCount( 3 );
        // Delete values
        in = readFile( "datavalueset/dataValueSetADeleted.xml" );
        summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 0, 0, 3, summary );
        assertDataValuesCount( 0 );
    }

    /**
     * Import 12 data values.
     */
    @Test
    void testImportValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetB.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertDataValuesCount( 12 );
    }

    /**
     * Import 12 data values. Then import 6 data values, where 4 are updates.
     */
    @Test
    void testImportUpdateValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetB.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );
        // Update
        in = readFile( "datavalueset/dataValueSetBUpdate.xml" );
        summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 2, 4, 0, summary );
        assertDataValuesCount( 14 );
    }

    /**
     * When updating a data value with a specified created date, the specified
     * created date should be used.
     *
     * When updating a data value without a specified created date, the existing
     * created date should remain unchanged.
     */
    @Test
    void testUpdateCreatedDate()
    {
        // Insert:
        // deC, peA, ouA created = 2010-01-01
        // deC, peA, ouB created = 2010-01-01
        in = readFile( "datavalueset/dataValueSetB.xml" );
        dataValueSetService.importDataValueSetXml( in );
        // Update:
        // deC, peA, ouA created = not specified, should remain unchanged
        // deC, peA, ouB created = 2020-02-02
        in = readFile( "datavalueset/dataValueSetBUpdate.xml" );
        dataValueSetService.importDataValueSetXml( in );
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
    void testImportDeletedValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetBDeleted.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 12, 0, 0, summary );
        assertDataValuesCount( 8 );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Then import 12 data
     * values which reverse deletion of the 4 values and update the other 8
     * values.
     */
    @Test
    void testImportReverseDeletedValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetBDeleted.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 8 );
        // Reverse deletion and update
        in = readFile( "datavalueset/dataValueSetB.xml" );
        summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 4, 8, 0, summary );
        assertDataValuesCount( 12 );
    }

    /**
     * Import 12 data values where 4 are marked as deleted. Then import 12 data
     * values which reverse deletion of the 4 values, update 4 values and add 4
     * values.
     */
    @Test
    void testImportAddAndReverseDeletedValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetBDeleted.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 8 );
        // Reverse deletion and update
        in = readFile( "datavalueset/dataValueSetBNew.xml" );
        summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 8, 4, 0, summary );
        assertDataValuesCount( 16 );
    }

    /**
     * Import 12 data values. Then import 12 values where 4 are marked as
     * deleted.
     */
    @Test
    void testDeleteValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetB.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );
        // Delete 4 values
        in = readFile( "datavalueset/dataValueSetBDeleted.xml" );
        summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 0, 8, 4, summary );
        assertDataValuesCount( 8 );
    }

    /**
     * Import 12 data values. Then import 12 values where 4 are marked as
     * deleted, 6 are updates and 2 are new.
     */
    @Test
    void testImportAndDeleteValuesXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetB.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );
        // Delete 4 values, add 2 values
        in = readFile( "datavalueset/dataValueSetBNewDeleted.xml" );
        summary = dataValueSetService.importDataValueSetXml( in );
        assertSuccessWithImportedUpdatedDeleted( 2, 6, 4, summary );
        assertDataValuesCount( 10 );
    }

    /**
     * Import 12 data values. Then import the same 12 data values with import
     * strategy delete.
     */
    @Test
    void testImportValuesDeleteStrategyXml()
    {
        assertDataValuesCount( 0 );
        in = readFile( "datavalueset/dataValueSetB.xml" );
        ImportSummary summary = dataValueSetService.importDataValueSetXml( in );
        assertEquals( 12, summary.getImportCount().getImported() );
        assertDataValuesCount( 12 );
        // Import with delete strategy
        in = readFile( "datavalueset/dataValueSetB.xml" );
        ImportOptions options = new ImportOptions().setStrategy( ImportStrategy.DELETE );
        summary = dataValueSetService.importDataValueSetXml( in, options );
        assertSuccessWithImportedUpdatedDeleted( 0, 0, 12, summary );
        assertDataValuesCount( 0 );
    }

    private InputStream readFile( String filename )
    {
        try
        {
            return new ClassPathResource( filename ).getInputStream();
        }
        catch ( IOException ex )
        {
            throw new UncheckedIOException( ex );
        }
    }

    private void assertDataValuesCount( int expected )
    {
        assertEquals( expected, dataValueService.getAllDataValues().size() );
    }

    private static void assertHasNoConflicts( ImportConflicts summary )
    {
        if ( summary.hasConflicts() )
        {
            assertEquals( 0, summary.getConflictCount(), summary.getConflictsDescription() );
        }
    }

    private static void assertSuccessWithImportedUpdatedDeleted( int imported, int updated, int deleted,
        ImportSummary summary )
    {
        assertHasNoConflicts( summary );
        assertEquals( imported, summary.getImportCount().getImported(), "unexpected import count" );
        assertEquals( updated, summary.getImportCount().getUpdated(), "unexpected update count" );
        assertEquals( deleted, summary.getImportCount().getDeleted(), "unexpected deleted count" );
        assertEquals( ImportStatus.SUCCESS, summary.getStatus() );
    }
}
