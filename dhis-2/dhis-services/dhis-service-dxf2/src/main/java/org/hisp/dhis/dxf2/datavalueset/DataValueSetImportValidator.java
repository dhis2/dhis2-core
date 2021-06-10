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

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;

import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.dataapproval.DataApproval;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.datavalue.AggregateAccessManager;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataSetContext;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataValueContext;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * Validates {@link DataValueSet} import.
 *
 * @author Jan Bernitt
 */
@Component
@AllArgsConstructor
public class DataValueSetImportValidator
{

    private final AclService aclService;

    private final AggregateAccessManager accessManager;

    private final LockExceptionStore lockExceptionStore;

    private final DataApprovalService approvalService;

    private final DataValueService dataValueService;

    /**
     * Validation on the {@link DataSet} level
     */
    interface DataSetValidation
    {
        void validate( DataValueSet dataValueSet, ImportContext context, DataSetContext dataSetContext );
    }

    /**
     * Validation on the {@link DataValue} level
     */
    interface DataValueValidation
    {
        void validate( DataValue dataValue, ImportContext context, DataSetContext dataSetContext,
            DataValueContext valueContext );
    }

    /**
     * Sequence of validations to perform on a {@link DataValueSet}
     */
    private final List<DataSetValidation> dataSetValidations = new ArrayList<>();

    /**
     * Sequence of validations to perform on each {@link DataValue} in a
     * {@link DataValueSet}.
     */
    private final List<DataValueValidation> dataValueValidations = new ArrayList<>();

    private void register( DataSetValidation validation )
    {
        dataSetValidations.add( validation );
    }

    private void register( DataValueValidation validation )
    {
        dataValueValidations.add( validation );
    }

    @PostConstruct
    public void init()
    {
        // OBS! Order is important as validation occurs in order of registration

        // DataSet Validations
        register( DataValueSetImportValidator::validateDataSetExists );
        register( this::validateDataSetIsAccessibleByUser );
        register( DataValueSetImportValidator::validateDataSetExistsStrictDataElements );
        register( DataValueSetImportValidator::validateDataSetOrgUnitExists );
        register( DataValueSetImportValidator::validateDataSetAttrOptionComboExists );

        // DataValue Validations
        register( DataValueSetImportValidator::validateDataValueDataElementExists );
        register( DataValueSetImportValidator::validateDataValuePeriodExists );
        register( DataValueSetImportValidator::validateDataValueOrgUnitExists );
        register( DataValueSetImportValidator::validateDataValueCategoryOptionComboExists );
        register( this::validateDataValueCategoryOptionComboAccess );
        register( DataValueSetImportValidator::validateDataValueAttrOptionComboExists );
        register( this::validateDataValueAttrOptionComboAccess );
        register( DataValueSetImportValidator::validateDataValueOrgUnitInUserHierarchy );
        register( DataValueSetImportValidator::validateDataValueIsDefined );
        register( DataValueSetImportValidator::validateDataValueIsValid );
        register( DataValueSetImportValidator::validateDataValueCommentIsValid );
        register( DataValueSetImportValidator::validateDataValueOptionsExist );

        // DataValue Constraints
        register( DataValueSetImportValidator::checkDataValueCategoryOptionCombo );
        register( DataValueSetImportValidator::checkDataValueAttrOptionCombo );
        register( DataValueSetImportValidator::checkDataValuePeriodType );
        register( DataValueSetImportValidator::checkDataValueStrictDataElement );
        register( DataValueSetImportValidator::checkDataValueStrictCategoryOptionCombos );
        register( DataValueSetImportValidator::checkDataValueStrictAttrOptionCombos );
        register( DataValueSetImportValidator::checkDataValueStrictOrgUnits );
        register( DataValueSetImportValidator::checkDataValueIsNotZeroAndInsignificant );
        register( DataValueSetImportValidator::checkDataValueStoredByIsValid );
        register( DataValueSetImportValidator::checkDataValuePeriodWithinAttrOptionComboRange );
        register( DataValueSetImportValidator::checkDataValueOrgUnitValidForAttrOptionCombo );
        register( this::checkDataValueTodayNotPastPeriodExpiry );
        register( DataValueSetImportValidator::checkDataValueNotAfterLatestOpenFuturePeriod );
        register( this::checkDataValueNotAlreadyApproved );
        register( DataValueSetImportValidator::checkDataValuePeriodIsOpenNow );
        register( DataValueSetImportValidator::checkDataValueConformsToOpenPeriodsOfAssociatedDataSets );
        register( this::checkDataValueFileResourceExists );
    }

    /*
     * DataSet validation
     */

    /**
     * Validate {@link DataSet} level of the import.
     *
     * @return true when there are data set validation errors and the import
     *         should be aborted, else false
     */
    public boolean abortDataSetImport( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        for ( DataSetValidation validation : dataSetValidations )
        {
            validation.validate( dataValueSet, context, dataSetContext );
        }
        return context.getSummary().isStatus( ImportStatus.ERROR );
    }

    private static void validateDataSetExists( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        if ( dataSetContext.getDataSet() == null && trimToNull( dataValueSet.getDataSet() ) != null )
        {
            context.error().addConflict( dataValueSet.getDataSet(), "Data set not found or not accessible" );
        }
    }

    private void validateDataSetIsAccessibleByUser( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        DataSet dataSet = dataSetContext.getDataSet();
        if ( dataSet != null && !aclService.canDataWrite( context.getCurrentUser(), dataSet ) )
        {
            context.error().addConflict( dataValueSet.getDataSet(),
                "User does not have write access for DataSet: " + dataSet.getUid() );
        }
    }

    private static void validateDataSetExistsStrictDataElements( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        if ( dataSetContext.getDataSet() == null && context.isStrictDataElements() )
        {
            context.error().addConflict( "DATA_IMPORT_STRICT_DATA_ELEMENTS", "A valid dataset is required" );
        }
    }

    private static void validateDataSetOrgUnitExists( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        if ( dataSetContext.getOuterOrgUnit() == null && trimToNull( dataValueSet.getOrgUnit() ) != null )
        {
            context.error().addConflict( dataValueSet.getOrgUnit(), "Org unit not found or not accessible" );
        }
    }

    private static void validateDataSetAttrOptionComboExists( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        if ( dataSetContext.getOuterAttrOptionCombo() == null
            && trimToNull( dataValueSet.getAttributeOptionCombo() ) != null )
        {
            context.error().addConflict( dataValueSet.getAttributeOptionCombo(),
                "Attribute option combo not found or not accessible" );
        }
    }

    /*
     * DataValue validation
     */

    public boolean skipDataValue( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        ImportSummary summary = context.getSummary();
        int skippedBefore = summary.skippedValueCount();
        int totalConflictsBefore = summary.getTotalConflictCount();
        for ( DataValueValidation validation : dataValueValidations )
        {
            validation.validate( dataValue, context, dataSetContext, valueContext );
            if ( summary.skippedValueCount() > skippedBefore
                || summary.getTotalConflictCount() > totalConflictsBefore )
            {
                return true;
            }
        }
        return false;
    }

    private static void validateDataValueDataElementExists( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getDataElement() == null )
        {
            context.addConflict( dataValue.getDataElement(), "Data element not found or not accessible" );
        }
    }

    private static void validateDataValuePeriodExists( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getPeriod() == null )
        {
            context.addConflict( dataValue.getPeriod(), "Period not valid" );
        }
    }

    private static void validateDataValueOrgUnitExists( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getOrgUnit() == null )
        {
            context.addConflict( dataValue.getOrgUnit(), "Organisation unit not found or not accessible" );
        }
    }

    private static void validateDataValueCategoryOptionComboExists( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getCategoryOptionCombo() == null
            && trimToNull( dataValue.getCategoryOptionCombo() ) != null )
        {
            context.addConflict( dataValue.getCategoryOptionCombo(),
                "Category option combo not found or not accessible for writing data" );
        }
    }

    private void validateDataValueCategoryOptionComboAccess( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getCategoryOptionCombo() != null )
        {
            List<String> errors = accessManager.canWrite( context.getCurrentUser(),
                valueContext.getCategoryOptionCombo() );

            if ( !errors.isEmpty() )
            {
                context.addConflicts( "dataValueSet", errors );
            }
        }
    }

    private static void validateDataValueAttrOptionComboExists( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getAttrOptionCombo() == null
            && trimToNull( dataValue.getAttributeOptionCombo() ) != null )
        {
            context.addConflict( dataValue.getAttributeOptionCombo(),
                "Attribute option combo not found or not accessible for writing data" );
        }
    }

    private void validateDataValueAttrOptionComboAccess( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getAttrOptionCombo() != null )
        {
            List<String> errors = accessManager.canWrite( context.getCurrentUser(),
                valueContext.getAttrOptionCombo() );

            if ( !errors.isEmpty() )
            {
                context.addConflicts( "dataValueSet", errors );
            }
        }
    }

    private static void validateDataValueOrgUnitInUserHierarchy( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        boolean inUserHierarchy = context.getOrgUnitInHierarchyMap().get( valueContext.getOrgUnit().getUid(),
            () -> valueContext.getOrgUnit().isDescendant( context.getCurrentOrgUnits() ) );

        if ( !inUserHierarchy )
        {
            context.addConflict( valueContext.getOrgUnit().getUid(),
                "Organisation unit not in hierarchy of current user: " + context.getCurrentUserName() );
        }
    }

    private static void validateDataValueIsDefined( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( dataValue.isNullValue() && !dataValue.isDeletedValue() )
        {
            context.addConflict( "Value",
                "Data value or comment not specified for data element: " + valueContext.getDataElement().getUid() );
        }
    }

    private static void validateDataValueIsValid( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        dataValue.setValueForced(
            ValidationUtils.normalizeBoolean( dataValue.getValue(),
                valueContext.getDataElement().getValueType() ) );

        String valueValid = ValidationUtils.dataValueIsValid( dataValue.getValue(), valueContext.getDataElement() );

        if ( valueValid != null )
        {
            context.addConflict( dataValue.getValue(),
                context.getI18n().getString( valueValid ) + ", must match data element type: "
                    + valueContext.getDataElement().getUid() );
        }
    }

    private static void validateDataValueCommentIsValid( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        String commentValid = ValidationUtils.commentIsValid( dataValue.getComment() );

        if ( commentValid != null )
        {
            context.addConflict( "Comment", context.getI18n().getString( commentValid ) );
        }
    }

    private static void validateDataValueOptionsExist( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        Optional<Set<String>> optionCodes = context.getDataElementOptionsMap().get(
            valueContext.getDataElement().getUid(),
            () -> valueContext.getDataElement().hasOptionSet()
                ? Optional.of( valueContext.getDataElement().getOptionSet().getOptionCodesAsSet() )
                : Optional.empty() );

        if ( optionCodes.isPresent() && !optionCodes.get().contains( dataValue.getValue() ) )
        {
            context.addConflict( dataValue.getValue(),
                "Data value is not a valid option of the data element option set: "
                    + valueContext.getDataElement().getUid() );
        }
    }

    /*
     * DataValue Constraints
     */

    private static void checkDataValueCategoryOptionCombo( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getCategoryOptionCombo() == null )
        {
            if ( context.isRequireCategoryOptionCombo() )
            {
                context.addConflict( dataValue.getValue(),
                    "Category option combo is required but is not specified" );
            }
            else
            {
                valueContext.setCategoryOptionCombo( dataSetContext.getFallbackCategoryOptionCombo() );
            }
        }
    }

    private static void checkDataValueAttrOptionCombo( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getAttrOptionCombo() == null )
        {
            if ( context.isRequireAttrOptionCombo() )
            {
                context.addConflict( dataValue.getValue(),
                    "Attribute option combo is required but is not specified" );
            }
            else
            {
                valueContext.setAttrOptionCombo( dataSetContext.getFallbackCategoryOptionCombo() );
            }
        }
    }

    private static void checkDataValuePeriodType( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictPeriods() && !context.getDataElementPeriodTypesMap()
            .get( valueContext.getDataElement().getUid(),
                valueContext.getDataElement()::getPeriodTypes )
            .contains( valueContext.getPeriod().getPeriodType() ) )
        {
            context.addConflict( dataValue.getPeriod(),
                "Period type of period: " + valueContext.getPeriod().getIsoDate() + " not valid for data element: "
                    + valueContext.getDataElement().getUid() );
        }
    }

    private static void checkDataValueStrictDataElement( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictDataElements()
            && !dataSetContext.getDataSetDataElements().contains( valueContext.getDataElement() ) )
        {
            context.addConflict( "DATA_IMPORT_STRICT_DATA_ELEMENTS",
                "Data element: " + dataValue.getDataElement() + " is not part of dataset: "
                    + dataSetContext.getDataSet().getUid() );
        }
    }

    private static void checkDataValueStrictCategoryOptionCombos( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictCategoryOptionCombos()
            && !context.getDataElementCategoryOptionComboMap().get( valueContext.getDataElement().getUid(),
                valueContext.getDataElement()::getCategoryOptionCombos )
                .contains( valueContext.getCategoryOptionCombo() ) )
        {
            context.addConflict( valueContext.getCategoryOptionCombo().getUid(),
                "Category option combo: " + valueContext.getCategoryOptionCombo().getUid()
                    + " must be part of category combo of data element: "
                    + valueContext.getDataElement().getUid() );
        }
    }

    private static void checkDataValueStrictAttrOptionCombos( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictAttrOptionCombos()
            && !context.getDataElementAttrOptionComboMap().get( valueContext.getDataElement().getUid(),
                valueContext.getDataElement()::getDataSetCategoryOptionCombos )
                .contains( valueContext.getAttrOptionCombo() ) )
        {
            context.addConflict( valueContext.getAttrOptionCombo().getUid(),
                "Attribute option combo: " + valueContext.getAttrOptionCombo().getUid()
                    + " must be part of category combo of data sets of data element: "
                    + valueContext.getDataElement().getUid() );
        }
    }

    private static void checkDataValueStrictOrgUnits( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictOrgUnits()
            && BooleanUtils
                .isFalse( context.getDataElementOrgUnitMap().get(
                    valueContext.getDataElement().getUid() + valueContext.getOrgUnit().getUid(),
                    () -> valueContext.getOrgUnit().hasDataElement( valueContext.getDataElement() ) ) ) )
        {
            context.addConflict( valueContext.getOrgUnit().getUid(),
                "Data element: " + valueContext.getDataElement().getUid()
                    + " must be assigned through data sets to organisation unit: "
                    + valueContext.getOrgUnit().getUid() );
        }
    }

    private static void checkDataValueIsNotZeroAndInsignificant( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        boolean zeroAndInsignificant = ValidationUtils.dataValueIsZeroAndInsignificant( dataValue.getValue(),
            valueContext.getDataElement() );

        if ( zeroAndInsignificant )
        {
            // Ignore value
            context.getSummary().skipValue();
        }
    }

    private static void checkDataValueStoredByIsValid( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        String storedByValid = ValidationUtils.storedByIsValid( dataValue.getStoredBy() );

        if ( storedByValid != null )
        {
            context.addConflict( dataValue.getStoredBy(), context.getI18n().getString( storedByValid ) );
        }
    }

    private static void checkDataValuePeriodWithinAttrOptionComboRange( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        final CategoryOptionCombo aoc = valueContext.getAttrOptionCombo();

        DateRange aocDateRange = dataSetContext.getDataSet() != null
            ? context.getAttrOptionComboDateRangeMap().get(
                valueContext.getAttrOptionCombo().getUid() + dataSetContext.getDataSet().getUid(),
                () -> aoc.getDateRange( dataSetContext.getDataSet() ) )
            : context.getAttrOptionComboDateRangeMap().get(
                valueContext.getAttrOptionCombo().getUid() + valueContext.getDataElement().getUid(),
                () -> aoc.getDateRange( valueContext.getDataElement() ) );

        if ( (aocDateRange.getStartDate() != null
            && aocDateRange.getStartDate().after( valueContext.getPeriod().getEndDate() ))
            || (aocDateRange.getEndDate() != null
                && aocDateRange.getEndDate().before( valueContext.getPeriod().getStartDate() )) )
        {
            context.addConflict( valueContext.getOrgUnit().getUid(),
                "Period: " + valueContext.getPeriod().getIsoDate()
                    + " is not within date range of attribute option combo: "
                    + valueContext.getAttrOptionCombo().getUid() );
        }
    }

    private static void checkDataValueOrgUnitValidForAttrOptionCombo( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( !context.getAttrOptionComboOrgUnitMap()
            .get( valueContext.getAttrOptionCombo().getUid() + valueContext.getOrgUnit().getUid(),
                () -> isOrgUnitValidForAttrOptionCombo( valueContext ) ) )
        {
            context.addConflict( valueContext.getOrgUnit().getUid(),
                "Organisation unit: " + valueContext.getOrgUnit().getUid()
                    + " is not valid for attribute option combo: "
                    + valueContext.getAttrOptionCombo().getUid() );
        }
    }

    private static boolean isOrgUnitValidForAttrOptionCombo( DataValueContext valueContext )
    {
        Set<OrganisationUnit> aocOrgUnits = valueContext.getAttrOptionCombo().getOrganisationUnits();
        return aocOrgUnits == null || valueContext.getOrgUnit().isDescendant( aocOrgUnits );
    }

    private void checkDataValueTodayNotPastPeriodExpiry( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        final DataSet approvalDataSet = context.getApprovalDataSet( dataSetContext, valueContext );

        // Data element is assigned to at least one data set
        if ( approvalDataSet != null && !context.isForceDataInput() )
        {
            String key = approvalDataSet.getUid() + valueContext.getPeriod().getUid()
                + valueContext.getOrgUnit().getUid();
            if ( context.getDataSetLockedMap().get( key,
                () -> isLocked( context.getCurrentUser(), approvalDataSet, valueContext.getPeriod(),
                    valueContext.getOrgUnit(), context.isSkipLockExceptionCheck() ) ) )
            {
                context.addConflict( valueContext.getPeriod().getIsoDate(),
                    "Current date is past expiry days for period " + valueContext.getPeriod().getIsoDate()
                        + " and data set: " + approvalDataSet.getUid() );
            }
        }
    }

    private static void checkDataValueNotAfterLatestOpenFuturePeriod( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        final DataSet approvalDataSet = context.getApprovalDataSet( dataSetContext, valueContext );

        // Data element is assigned to at least one data set
        if ( approvalDataSet != null && !context.isForceDataInput() )
        {

            Period latestFuturePeriod = context.getDataElementLatestFuturePeriodMap().get(
                valueContext.getDataElement().getUid(), valueContext.getDataElement()::getLatestOpenFuturePeriod );

            if ( valueContext.getPeriod().isAfter( latestFuturePeriod ) && context.isIso8601() )
            {
                context.addConflict( valueContext.getPeriod().getIsoDate(),
                    "Period: " + valueContext.getPeriod().getIsoDate() + " is after latest open future period: "
                        + latestFuturePeriod.getIsoDate() + " for data element: "
                        + valueContext.getDataElement().getUid() );
            }
        }
    }

    private void checkDataValueNotAlreadyApproved( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        final DataSet approvalDataSet = context.getApprovalDataSet( dataSetContext, valueContext );

        // Data element is assigned to at least one data set
        if ( approvalDataSet != null && !context.isForceDataInput() )
        {
            DataApprovalWorkflow workflow = approvalDataSet.getWorkflow();

            if ( workflow != null )
            {
                final String workflowPeriodAoc = workflow.getUid() + valueContext.getPeriod().getUid()
                    + valueContext.getAttrOptionCombo().getUid();

                if ( context.getApprovalMap().get( valueContext.getOrgUnit().getUid() + workflowPeriodAoc, () -> {
                    DataApproval lowestApproval = DataApproval
                        .getLowestApproval( new DataApproval( null, workflow, valueContext.getPeriod(),
                            valueContext.getOrgUnit(), valueContext.getAttrOptionCombo() ) );

                    return lowestApproval != null && context.getLowestApprovalLevelMap().get(
                        lowestApproval.getDataApprovalLevel().getUid()
                            + lowestApproval.getOrganisationUnit().getUid() + workflowPeriodAoc,
                        () -> approvalService.getDataApproval( lowestApproval ) != null );
                } ) )
                {
                    context.addConflict( valueContext.getOrgUnit().getUid(),
                        "Data is already approved for data set: " + approvalDataSet.getUid() + " period: "
                            + valueContext.getPeriod().getIsoDate()
                            + " organisation unit: " + valueContext.getOrgUnit().getUid()
                            + " attribute option combo: "
                            + valueContext.getAttrOptionCombo().getUid() );
                }
            }
        }
    }

    private static void checkDataValuePeriodIsOpenNow( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        final DataSet approvalDataSet = context.getApprovalDataSet( dataSetContext, valueContext );

        if ( approvalDataSet != null && !context.isForceDataInput()
            && !approvalDataSet.isDataInputPeriodAndDateAllowed( valueContext.getPeriod(), new Date() ) )
        {
            context.addConflict( valueContext.getOrgUnit().getUid(),
                "Period: " + valueContext.getPeriod().getIsoDate() + " is not open for this data set at this time: "
                    + approvalDataSet.getUid() );
        }
    }

    private static void checkDataValueConformsToOpenPeriodsOfAssociatedDataSets( DataValue dataValue,
        ImportContext context, DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( !context.isForceDataInput()
            && !context.getPeriodOpenForDataElement().get(
                valueContext.getDataElement().getUid() + valueContext.getPeriod().getIsoDate(),
                () -> valueContext.getDataElement().isDataInputAllowedForPeriodAndDate( valueContext.getPeriod(),
                    new Date() ) ) )
        {
            context.addConflict( valueContext.getOrgUnit().getUid(),
                "Period " + valueContext.getPeriod().getName()
                    + " does not conform to the open periods of associated data sets" );
        }
    }

    private void checkDataValueFileResourceExists( DataValue dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.getStrategy().isDelete() && valueContext.getDataElement().isFileType()
            && valueContext.getActualDataValue( dataValueService ) == null )
        {
            context.addConflict( valueContext.getDataElement().getUid(),
                "No data value for file resource exist for the given combination" );
        }
    }

    /**
     * Checks whether the given data set is locked.
     *
     * @param dataSet the data set.
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @param skipLockExceptionCheck whether to skip lock exception check.
     */
    private boolean isLocked( User user, DataSet dataSet, Period period, OrganisationUnit organisationUnit,
        boolean skipLockExceptionCheck )
    {
        return dataSet.isLocked( user, period, null )
            && (skipLockExceptionCheck || lockExceptionStore.getCount( dataSet, period, organisationUnit ) == 0L);
    }
}
