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

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataSetContext;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataSetContext.DataSetContextBuilder;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataValueContext;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataValueContext.DataValueContextBuilder;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.ImportContextBuilder;
import org.hisp.dhis.dxf2.importsummary.ImportConflict;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link DataValueSetImportValidator}
 *
 * @author Jan Bernitt
 */
public class DataValueSetImportValidatorTest
{

    private AclService aclService;

    private AggregateAccessManager accessManager;

    private LockExceptionStore lockExceptionStore;

    private DataApprovalService approvalService;

    private DataValueService dataValueService;

    private DataValueSetImportValidator validator;

    @Before
    public void setUp()
    {
        aclService = mock( AclService.class );
        accessManager = mock( AggregateAccessManager.class );
        lockExceptionStore = mock( LockExceptionStore.class );
        approvalService = mock( DataApprovalService.class );
        dataValueService = mock( DataValueService.class );
        validator = new DataValueSetImportValidator( aclService, accessManager, lockExceptionStore, approvalService,
            dataValueService );
        validator.init();
        setupUserCanWriteCategoryOptions( true );
    }

    /*
     * Data Set validation (should the set be aborted)
     */

    @Test
    public void testValidateDataSetExists()
    {
        DataValueSet dataValueSet = createEmptyDataValueSet();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7600, dataValueSet.getDataSet(),
            "Data set not found or not accessible: `<object1>`", context );
    }

    @Test
    public void testValidateDataSetIsAccessibleByUser()
    {
        // simulate that user does not have access:
        when( aclService.canDataRead( any(), any() ) ).thenReturn( false );

        DataValueSet dataValueSet = createEmptyDataValueSet();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = createDataSetContext( dataValueSet ).build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7601, dataValueSet.getDataSet(),
            "User does not have write access for DataSet: `<object1>`", context );
    }

    @Test
    public void testValidateDataSetExistsStrictDataElements()
    {
        when( aclService.canDataRead( any(), any() ) ).thenReturn( true );

        DataValueSet dataValueSet = new DataValueSet();

        ImportContext context = createMinimalImportContext()
            .strictDataElements( true ).build();
        DataSetContext dataSetContext = createDataSetContext( dataValueSet ).build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7602, null, "A valid dataset is required", context );
    }

    @Test
    public void testValidateDataSetOrgUnitExists()
    {
        when( aclService.canDataRead( any(), any() ) ).thenReturn( true );

        DataValueSet dataValueSet = new DataValueSet();
        dataValueSet.setOrgUnit( CodeGenerator.generateUid() );

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7603, dataValueSet.getOrgUnit(),
            "Org unit not found or not accessible: `<object1>`", context );
    }

    @Test
    public void testValidateDataSetAttrOptionComboExists()
    {
        when( aclService.canDataRead( any(), any() ) ).thenReturn( true );

        DataValueSet dataValueSet = new DataValueSet();
        dataValueSet.setAttributeOptionCombo( CodeGenerator.generateUid() );

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7604, dataValueSet.getAttributeOptionCombo(),
            "Attribute option combo not found or not accessible: `<object1>`", context );
    }

    /*
     * Data Value validation (should the entry be skipped)
     */

    @Test
    public void testValidateDataValueDataElementExists()
    {
        DataValue dataValue = createRandomDataValue();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = DataValueContext.builder().build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7610, dataValue.getDataElement(),
            "Data element not found or not accessible: `<object1>`", context );
    }

    @Test
    public void testValidateDataValuePeriodExists()
    {
        DataValue dataValue = createRandomDataValue();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = createDataValueContext( dataValue )
            .period( null ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7611, dataValue.getPeriod(),
            "Period not valid: `<object1>`", context );
    }

    @Test
    public void testValidateDataValueOrgUnitExists()
    {
        DataValue dataValue = createRandomDataValue();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = createDataValueContext( dataValue )
            .orgUnit( null ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7612, dataValue.getOrgUnit(),
            "Organisation unit not found or not accessible: `<object1>`", context );
    }

    @Test
    public void testValidateDataValueCategoryOptionComboExists()
    {
        DataValue dataValue = createRandomDataValue();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = createDataValueContext( dataValue )
            .categoryOptionCombo( null ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7613, dataValue.getCategoryOptionCombo(),
            "Category option combo not found or not accessible for writing data: `<object1>`", context );
    }

    @Test
    public void testValidateDataValueAttrOptionComboExists()
    {
        DataValue dataValue = createRandomDataValue();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = createDataValueContext( dataValue )
            .attrOptionCombo( null ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7615, dataValue.getAttributeOptionCombo(),
            "Attribute option combo not found or not accessible for writing data: `<object1>`", context );
    }

    @Test
    public void testValidateDataValueCategoryOptionComboAccess()
    {
        DataValue dataValue = createRandomDataValue();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = createDataValueContext( dataValue ).build();

        setupUserCanWriteCategoryOptions( false );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7614, dataValue.getCategoryOptionCombo(),
            "Category option combo: `<object1>` option not accessible: `<object2>`", context );
    }

    @Test
    public void testValidateDataValueAttrOptionComboAccess()
    {
        DataValue dataValue = createRandomDataValue();
        // so that we got to later validation
        dataValue.setCategoryOptionCombo( null );

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = createDataValueContext( dataValue ).build();

        setupUserCanWriteCategoryOptions( false );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7616, dataValue.getAttributeOptionCombo(),
            "Attribute option combo: `<object1>` option not accessible: `<object2>`", context );
    }

    @Test
    public void testValidateDataValueOrgUnitInUserHierarchy()
    {
        DataValue dataValue = createRandomDataValue();

        ImportContext context = createMinimalImportContext().build();
        DataSetContext dataSetContext = DataSetContext.builder().build();
        DataValueContext valueContext = createDataValueContext( dataValue ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        String currentUserId = context.getCurrentUser().getUid();
        assertConflict( ErrorCode.E7617, dataValue.getOrgUnit(),
            "Organisation unit: `<object1>` not in hierarchy of current user: `" + currentUserId + "`",
            context );
    }

    @Test
    public void testValidateDataValueIsDefined()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        ImportContext context = createMinimalImportContext().currentOrgUnits( singleton( valueContext.getOrgUnit() ) )
            .build();
        DataSetContext dataSetContext = DataSetContext.builder().build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7618, dataValue.getDataElement(),
            "Data value or comment not specified for data element: `" + dataValue.getDataElement() + "`", context );
    }

    private static void assertConflict( ErrorCode expectedError, String expectedObject, String expectedValue,
        ImportContext actual )
    {
        ImportSummary summary = actual.getSummary();
        assertEquals( 1, summary.getConflictCount() );
        ImportConflict conflict = summary.getConflicts().iterator().next();
        assertEquals( expectedError, conflict.getErrorCode() );
        String object = conflict.getObject();
        String message = expectedValue;
        if ( object != null )
        {
            message = message.replace( "<object>", object );
        }
        Map<String, String> objects = conflict.getObjects();
        if ( objects != null )
        {
            int i = 0;
            for ( String obj : objects.values() )
            {
                if ( obj != null )
                {
                    message = message.replace( "<object" + (i + 1) + ">", obj );
                }
                i++;
            }
        }
        assertEquals( message, conflict.getValue() );
        assertEquals( expectedObject, object );
    }

    private static DataValueSet createEmptyDataValueSet()
    {
        DataValueSet dvs = new DataValueSet();
        dvs.setDataSet( CodeGenerator.generateUid() );
        return dvs;
    }

    private static DataValue createRandomDataValue()
    {
        DataValue dv = new DataValue();
        dv.setDataElement( CodeGenerator.generateUid() );
        dv.setPeriod( "2021-01" );
        dv.setOrgUnit( CodeGenerator.generateUid() );
        dv.setCategoryOptionCombo( CodeGenerator.generateUid() );
        dv.setAttributeOptionCombo( CodeGenerator.generateUid() );
        return dv;
    }

    private static ImportContextBuilder createMinimalImportContext()
    {
        User currentUser = new User();
        UserCredentials credentials = new UserCredentials();
        credentials.setUsername( "Guest" );
        currentUser.setUserCredentials( credentials );
        currentUser.setUid( CodeGenerator.generateUid() );
        return ImportContext.builder().summary( new ImportSummary() ).currentUser( currentUser )
            .singularNameForType( DataValueSetImportValidatorTest::getSingularNameForType );
    }

    private static String getSingularNameForType( Class<? extends IdentifiableObject> klass )
    {
        String singular = klass.getSimpleName();
        return singular.substring( 0, 1 ).toLowerCase().concat( singular.substring( 1 ) );
    }

    private DataSetContextBuilder createDataSetContext( DataValueSet dataValueSet )
    {
        DataSetContextBuilder builder = DataSetContext.builder();
        String dsId = dataValueSet.getDataSet();
        if ( dsId != null )
        {
            DataSet ds = new DataSet();
            ds.setUid( dsId );
            builder.dataSet( ds );
        }
        return builder;
    }

    private DataValueContextBuilder createDataValueContext( DataValue dataValue )
    {
        DataValueContextBuilder builder = DataValueContext.builder();
        String deId = dataValue.getDataElement();
        String period = dataValue.getPeriod();
        String ouId = dataValue.getOrgUnit();
        String coId = dataValue.getCategoryOptionCombo();
        String aoId = dataValue.getAttributeOptionCombo();
        if ( deId != null )
        {
            DataElement de = new DataElement();
            de.setUid( deId );
            builder.dataElement( de );
        }
        if ( period != null )
        {
            Period p = PeriodType.getPeriodFromIsoString( "2021-01" );
            builder.period( p );
        }
        if ( ouId != null )
        {
            OrganisationUnit ou = new OrganisationUnit();
            ou.setUid( ouId );
            builder.orgUnit( ou );
        }
        if ( coId != null )
        {
            builder.categoryOptionCombo( createMinimalOptionCombo( coId ) );
        }
        if ( aoId != null )
        {
            builder.attrOptionCombo( createMinimalOptionCombo( aoId ) );
        }
        return builder;
    }

    private CategoryOptionCombo createMinimalOptionCombo( String uid )
    {
        CategoryOptionCombo combo = new CategoryOptionCombo();
        combo.setUid( uid );
        CategoryOption option = new CategoryOption( "name" );
        option.setUid( CodeGenerator.generateUid() );
        combo.setCategoryOptions( singleton( option ) );
        return combo;
    }

    private void setupUserCanWriteCategoryOptions( boolean canWrite )
    {
        when( aclService.canDataWrite( any( User.class ), any( CategoryOption.class ) ) ).thenReturn( canWrite );
    }
}
