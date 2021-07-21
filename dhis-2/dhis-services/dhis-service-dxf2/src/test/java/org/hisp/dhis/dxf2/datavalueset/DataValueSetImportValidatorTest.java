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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataInputPeriod;
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
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.option.OptionSet;
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

    private I18n i18n;

    private DataValueSetImportValidator validator;

    @Before
    public void setUp()
    {
        aclService = mock( AclService.class );
        accessManager = mock( AggregateAccessManager.class );
        lockExceptionStore = mock( LockExceptionStore.class );
        approvalService = mock( DataApprovalService.class );
        dataValueService = mock( DataValueService.class );
        i18n = mock( I18n.class );
        validator = new DataValueSetImportValidator( aclService, accessManager, lockExceptionStore, approvalService,
            dataValueService );
        validator.init();
        setupUserCanWriteCategoryOptions( true );
        when( i18n.getString( anyString() ) ).thenAnswer( invocation -> invocation.getArgument( 0, String.class ) );
    }

    /*
     * Data Set validation (should the set be aborted)
     */

    @Test
    public void testValidateDataSetExists()
    {
        DataValueSet dataValueSet = createEmptyDataValueSet();

        ImportContext context = createMinimalImportContext( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7600, "Data set not found or not accessible: `<object1>`", context,
            dataValueSet.getDataSet() );
    }

    @Test
    public void testValidateDataSetIsAccessibleByUser()
    {
        // simulate that user does not have access:
        when( aclService.canDataRead( any(), any() ) ).thenReturn( false );

        DataValueSet dataValueSet = createEmptyDataValueSet();

        ImportContext context = createMinimalImportContext( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( dataValueSet ).build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7601, "User does not have write access for DataSet: `<object1>`", context,
            dataValueSet.getDataSet() );
    }

    @Test
    public void testValidateDataSetExistsStrictDataElements()
    {
        when( aclService.canDataRead( any(), any() ) ).thenReturn( true );

        DataValueSet dataValueSet = new DataValueSet();

        ImportContext context = createMinimalImportContext( null )
            .strictDataElements( true ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( dataValueSet ).build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7602, "A valid dataset is required", context );
    }

    @Test
    public void testValidateDataSetOrgUnitExists()
    {
        when( aclService.canDataRead( any(), any() ) ).thenReturn( true );

        DataValueSet dataValueSet = new DataValueSet();
        dataValueSet.setOrgUnit( CodeGenerator.generateUid() );

        ImportContext context = createMinimalImportContext( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7603, "Org unit not found or not accessible: `<object1>`", context,
            dataValueSet.getOrgUnit(), dataValueSet.getDataSet() );
    }

    @Test
    public void testValidateDataSetAttrOptionComboExists()
    {
        when( aclService.canDataRead( any(), any() ) ).thenReturn( true );

        DataValueSet dataValueSet = new DataValueSet();
        dataValueSet.setAttributeOptionCombo( CodeGenerator.generateUid() );

        ImportContext context = createMinimalImportContext( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();

        assertTrue( validator.abortDataSetImport( dataValueSet, context, dataSetContext ) );
        assertConflict( ErrorCode.E7604, "Attribute option combo not found or not accessible: `<object1>`", context,
            dataValueSet.getAttributeOptionCombo(), dataValueSet.getDataSet() );
    }

    /*
     * Data Value validation (should the entry be skipped)
     */

    @Test
    public void testValidateDataValueDataElementExists()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = DataValueContext.builder().build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7610, "Data element not found or not accessible: `<object1>`", context,
            dataValue.getDataElement() );
    }

    @Test
    public void testValidateDataValuePeriodExists()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue )
            .period( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7611, "Period not valid: `<object1>`", context, dataValue.getPeriod() );
    }

    @Test
    public void testValidateDataValueOrgUnitExists()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue )
            .orgUnit( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7612, "Organisation unit not found or not accessible: `<object1>`", context,
            dataValue.getOrgUnit() );
    }

    @Test
    public void testValidateDataValueCategoryOptionComboExists()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue )
            .categoryOptionCombo( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7613,
            "Category option combo not found or not accessible for writing data: `<object1>`", context,
            dataValue.getCategoryOptionCombo() );
    }

    @Test
    public void testValidateDataValueAttrOptionComboExists()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue )
            .attrOptionCombo( null ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7615,
            "Attribute option combo not found or not accessible for writing data: `<object1>`", context,
            dataValue.getAttributeOptionCombo() );
    }

    @Test
    public void testValidateDataValueCategoryOptionComboAccess()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        setupUserCanWriteCategoryOptions( false );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7614, "Category option combo: `<object1>` option not accessible: `<object2>`",
            context, dataValue.getCategoryOptionCombo(),
            valueContext.getCategoryOptionCombo().getCategoryOptions().iterator().next().getUid() );
    }

    @Test
    public void testValidateDataValueAttrOptionComboAccess()
    {
        DataValue dataValue = createRandomDataValue();
        // so that we got to later validation
        dataValue.setCategoryOptionCombo( null );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        setupUserCanWriteCategoryOptions( false );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7616, "Attribute option combo: `<object1>` option not accessible: `<object2>`",
            context, dataValue.getAttributeOptionCombo(),
            valueContext.getAttrOptionCombo().getCategoryOptions().iterator().next().getUid() );
    }

    @Test
    public void testValidateDataValueOrgUnitInUserHierarchy()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext )
            .currentOrgUnits( emptySet() )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        String currentUserId = context.getCurrentUser().getUid();
        assertConflict( ErrorCode.E7617,
            "Organisation unit: `<object1>` not in hierarchy of current user: `" + currentUserId + "`", context,
            dataValue.getOrgUnit(), currentUserId );
    }

    @Test
    public void testValidateDataValueIsDefined()
    {
        DataValue dataValue = createRandomDataValue();
        dataValue.setComment( null );
        dataValue.setValue( null );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7618,
            "Data value or comment not specified for data element: `" + dataValue.getDataElement() + "`", context,
            dataValue.getDataElement() );
    }

    @Test
    public void testValidateDataValueIsValid()
    {
        DataValue dataValue = createRandomDataValue();
        dataValue.setValue( "not-a-bool" );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7619, "Value must match data element's `<object1>` type constraints: value_not_bool",
            context, dataValue.getDataElement(), "value_not_bool" );
    }

    @Test
    public void testValidateDataValueCommentIsValid()
    {
        DataValue dataValue = createRandomDataValue();
        char[] chars = new char[50001];
        Arrays.fill( chars, 'a' );
        dataValue.setComment( new String( chars ) );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7620, "Invalid comment: comment_length_greater_than_max_length", context,
            "comment_length_greater_than_max_length" );
    }

    @Test
    public void testValidateDataValueOptionsExist()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        valueContext.getDataElement().setOptionSet( new OptionSet() );
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7621, "Data value is not a valid option of the data element option set: `<object1>`",
            context, dataValue.getDataElement() );
    }

    /*
     * DataValue Constraints
     */

    @Test
    public void testCheckDataValueCategoryOptionCombo()
    {
        DataValue dataValue = createRandomDataValue();
        dataValue.setCategoryOptionCombo( null );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext )
            .requireCategoryOptionCombo( true )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7630, "Category option combo is required but is not specified", context );
    }

    @Test
    public void testCheckDataValueAttrOptionCombo()
    {
        DataValue dataValue = createRandomDataValue();
        dataValue.setAttributeOptionCombo( null );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext )
            .requireAttrOptionCombo( true )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7631, "Attribute option combo is required but is not specified", context );
    }

    @Test
    public void testCheckDataValuePeriodType()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext )
            .strictPeriods( true )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7632, "Period type of period: `<object1>` not valid for data element: `<object2>`",
            context, dataValue.getPeriod(), dataValue.getDataElement() );
    }

    @Test
    public void testCheckDataValueStrictDataElement()
    {
        DataValue dataValue = createRandomDataValue();
        DataValueSet dataValueSet = createEmptyDataValueSet();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( dataValueSet ).build();
        ImportContext context = createMinimalImportContext( valueContext )
            .strictDataElements( true )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7633, "Data element: `<object1>` is not part of dataset: `<object2>`",
            context, dataValue.getDataElement(), dataValueSet.getDataSet() );
    }

    @Test
    public void testCheckDataValueStrictCategoryOptionCombos()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        valueContext.getDataElement().setCategoryCombo( new CategoryCombo() );
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext )
            .strictCategoryOptionCombos( true )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7634,
            "Category option combo: `<object1>` must be part of category combo of data element: `<object2>`",
            context, dataValue.getCategoryOptionCombo(), dataValue.getDataElement() );
    }

    @Test
    public void testCheckDataValueStrictAttrOptionCombos()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext )
            .strictAttrOptionCombos( true )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7635,
            "Attribute option combo: `<object1>` must be part of category combo of data sets of data element: `<object2>`",
            context, dataValue.getAttributeOptionCombo(), dataValue.getDataElement() );
    }

    @Test
    public void testCheckDataValueStrictOrgUnits()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext )
            .strictOrgUnits( true )
            .build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7636,
            "Data element: `<object2>` must be assigned through data sets to organisation unit: `<object1>`",
            context, dataValue.getOrgUnit(), dataValue.getDataElement() );
    }

    @Test
    public void testCheckDataValueIsNotZeroAndInsignificant()
    {
        DataValue dataValue = createRandomDataValue();
        dataValue.setValue( "0" );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        valueContext.getDataElement().setValueType( ValueType.INTEGER );
        valueContext.getDataElement().setZeroIsSignificant( false );
        valueContext.getDataElement().setAggregationType( AggregationType.COUNT );
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertEquals( 0, context.getSummary().getConflictCount() );
    }

    @Test
    public void testCheckDataValueStoredByIsValid()
    {
        DataValue dataValue = createRandomDataValue();
        char[] chars = new char[300];
        Arrays.fill( chars, 'x' );
        String storedBy = new String( chars );
        dataValue.setStoredBy( storedBy );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7637, "Invalid storedBy: stored_by_length_greater_than_max_length",
            context, "stored_by_length_greater_than_max_length" );
    }

    @Test
    public void testCheckDataValuePeriodWithinAttrOptionComboRange()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();
        String key = valueContext.getAttrOptionCombo().getUid() + valueContext.getDataElement().getUid();
        context.getAttrOptionComboDateRangeMap().put( key, new DateRange( new Date(), null ) );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7638,
            "Period: `<object1>` is not within date range of attribute option combo: `<object2>`",
            context, dataValue.getPeriod(), dataValue.getAttributeOptionCombo() );
    }

    @Test
    public void testCheckDataValueOrgUnitValidForAttrOptionCombo()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext().build();
        ImportContext context = createMinimalImportContext( valueContext ).build();
        String key = valueContext.getAttrOptionCombo().getUid() + valueContext.getOrgUnit().getUid();
        context.getAttrOptionComboOrgUnitMap().put( key, false );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7639,
            "Organisation unit: `<object1>` is not valid for attribute option combo: `<object2>`",
            context, dataValue.getOrgUnit(), dataValue.getAttributeOptionCombo() );
    }

    @Test
    public void testCheckDataValueTodayNotPastPeriodExpiry()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( createEmptyDataValueSet() ).build();
        ImportContext context = createMinimalImportContext( valueContext )
            .forceDataInput( false )
            .build();
        String key = dataSetContext.getDataSet().getUid() + valueContext.getPeriod().getUid()
            + valueContext.getOrgUnit().getUid();
        context.getDataSetLockedMap().put( key, true );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7640,
            "Current date is past expiry days for period: `<object1>`  and data set: `<object2>`",
            context, dataValue.getPeriod(), dataSetContext.getDataSet().getUid() );
    }

    @Test
    public void testCheckDataValueNotAfterLatestOpenFuturePeriod()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( createEmptyDataValueSet() ).build();
        ImportContext context = createMinimalImportContext( valueContext )
            .forceDataInput( false )
            .isIso8601( true )
            .build();
        context.getDataElementLatestFuturePeriodMap().put( valueContext.getDataElement().getUid(),
            PeriodType.getPeriodFromIsoString( "2020-01" ) );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7641,
            "Period: `<object1>` is after latest open future period: `202001` for data element: `<object2>`",
            context, dataValue.getPeriod(), dataValue.getDataElement() );
    }

    @Test
    public void testCheckDataValueNotAlreadyApproved()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( createEmptyDataValueSet() ).build();
        DataApprovalWorkflow workflow = new DataApprovalWorkflow();
        workflow.setUid( CodeGenerator.generateUid() );
        dataSetContext.getDataSet().setWorkflow( workflow );
        ImportContext context = createMinimalImportContext( valueContext )
            .forceDataInput( false )
            .build();
        final String workflowPeriodAoc = workflow.getUid() + valueContext.getPeriod().getUid()
            + valueContext.getAttrOptionCombo().getUid();
        String key = valueContext.getOrgUnit().getUid() + workflowPeriodAoc;
        context.getApprovalMap().put( key, true ); // already approved

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7642,
            "Data is already approved for data set: `<object4>` period: `<object2>` organisation unit: `<object1>` attribute option combo: `<object3>`",
            context, dataValue.getOrgUnit(), dataValue.getPeriod(), dataValue.getAttributeOptionCombo(),
            dataSetContext.getDataSet().getUid() );
    }

    @Test
    public void testCheckDataValuePeriodIsOpenNow()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( createEmptyDataValueSet() ).build();
        ImportContext context = createMinimalImportContext( valueContext )
            .forceDataInput( false )
            .build();
        DataInputPeriod inputPeriod = new DataInputPeriod();
        inputPeriod.setPeriod( PeriodType.getPeriodFromIsoString( "2019" ) );
        dataSetContext.getDataSet().setDataInputPeriods( singleton( inputPeriod ) );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7643,
            "Period: `<object1>` is not open for this data set at this time: `<object2>`",
            context, dataValue.getPeriod(), dataSetContext.getDataSet().getUid() );
    }

    @Test
    public void testCheckDataValueConformsToOpenPeriodsOfAssociatedDataSets()
    {
        DataValue dataValue = createRandomDataValue();

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        DataSetContext dataSetContext = createMinimalDataSetContext( createEmptyDataValueSet() ).build();
        ImportContext context = createMinimalImportContext( valueContext )
            .forceDataInput( false )
            .build();
        String key = valueContext.getDataElement().getUid() + valueContext.getPeriod().getIsoDate();
        context.getPeriodOpenForDataElement().put( key, false );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7644,
            "Period: `<object1>` does not conform to the open periods of associated data sets",
            context, dataValue.getPeriod() );
    }

    @Test
    public void testCheckDataValueFileResourceExists()
    {
        DataValue dataValue = createRandomDataValue();
        dataValue.setValue( CodeGenerator.generateUid() );

        DataValueContext valueContext = createDataValueContext( dataValue ).build();
        valueContext.getDataElement().setValueType( ValueType.FILE_RESOURCE );
        DataSetContext dataSetContext = createMinimalDataSetContext( createEmptyDataValueSet() ).build();
        ImportContext context = createMinimalImportContext( valueContext )
            .forceDataInput( false )
            .strategy( ImportStrategy.DELETE )
            .build();

        when( dataValueService.getDataValue( any( DataElement.class ), any( Period.class ),
            any( OrganisationUnit.class ), any( CategoryOptionCombo.class ), any( CategoryOptionCombo.class ) ) )
                .thenReturn( null );

        assertTrue( validator.skipDataValue( dataValue, context, dataSetContext, valueContext ) );
        assertConflict( ErrorCode.E7645,
            "No data value for file resource exist for the given combination for data element: `<object1>`",
            context, dataValue.getDataElement() );
    }

    private static void assertConflict( ErrorCode expectedError, String expectedValue, ImportContext context,
        String... expectedObjects )
    {
        ImportSummary summary = context.getSummary();
        assertEquals( 1, summary.getConflictCount() );
        ImportConflict conflict = summary.getConflicts().iterator().next();

        String object = conflict.getObject();
        assertEquals( "unexpected conflict type: ", expectedError, conflict.getErrorCode() );
        if ( expectedObjects.length > 0 )
        {
            assertEquals( "unexpected object ID: ", expectedObjects[0], object );
        }
        Map<String, String> objects = conflict.getObjects();
        assertEquals( "unexpected number of object IDs: ", expectedObjects.length, objects.size() );
        Iterator<String> actualObjectsIter = objects.values().iterator();
        for ( int i = 0; i < expectedObjects.length; i++ )
        {
            assertEquals( "unexpected object ID for object " + i + ": ", expectedObjects[i], actualObjectsIter.next() );
        }
        assertEquals( substituteObjectPlaceholders( expectedValue, conflict ), conflict.getValue() );
    }

    private static String substituteObjectPlaceholders( String expectedValue, ImportConflict conflict )
    {
        String message = expectedValue;
        String object = conflict.getObject();
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
        return message;
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
        dv.setComment( "comment" );
        dv.setValue( "true" );
        return dv;
    }

    private DataSetContextBuilder createMinimalDataSetContext()
    {
        return createMinimalDataSetContext( null );
    }

    private DataSetContextBuilder createMinimalDataSetContext( DataValueSet dataValueSet )
    {
        DataSetContextBuilder builder = DataSetContext.builder();
        if ( dataValueSet != null )
        {
            String dsId = dataValueSet.getDataSet();
            if ( dsId != null )
            {
                DataSet ds = new DataSet();
                ds.setUid( dsId );
                builder.dataSet( ds );
            }
        }
        return builder;
    }

    private ImportContextBuilder createMinimalImportContext( DataValueContext valueContext )
    {
        User currentUser = new User();
        UserCredentials credentials = new UserCredentials();
        credentials.setUsername( "Guest" );
        currentUser.setUserCredentials( credentials );
        currentUser.setUid( CodeGenerator.generateUid() );
        return ImportContext.builder()
            .summary( new ImportSummary() )
            .strategy( ImportStrategy.CREATE )
            .currentUser( currentUser )
            .i18n( i18n )
            .currentOrgUnits( valueContext == null ? null : singleton( valueContext.getOrgUnit() ) )
            .singularNameForType( DataValueSetImportValidatorTest::getSingularNameForType );
    }

    private static String getSingularNameForType( Class<? extends IdentifiableObject> klass )
    {
        String singular = klass.getSimpleName();
        return singular.substring( 0, 1 ).toLowerCase().concat( singular.substring( 1 ) );
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
            de.setValueType( ValueType.BOOLEAN );
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
