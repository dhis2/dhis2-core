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

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.BooleanUtils;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DateRange;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataapproval.DataApprovalService;
import org.hisp.dhis.dataapproval.DataApprovalWorkflow;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.LockExceptionStore;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.dxf2.datavalue.DataValue;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataSetContext;
import org.hisp.dhis.dxf2.datavalueset.ImportContext.DataValueContext;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummary;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
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
@RequiredArgsConstructor
public class DataValueSetImportValidator
{
    private final AclService aclService;

    private final LockExceptionStore lockExceptionStore;

    private final DataApprovalService approvalService;

    private final DataValueService dataValueService;

    private final OrganisationUnitService organisationUnitService;

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
        void validate( DataValueEntry dataValue, ImportContext context, DataSetContext dataSetContext,
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
        register( this::validateDataValueOrgUnitInUserHierarchy );
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
        register( DataValueSetImportValidator::checkDataValueStoredByIsValid );
        register( DataValueSetImportValidator::checkDataValuePeriodWithinAttrOptionComboRange );
        register( this::checkDataValueOrgUnitValidForAttrOptionCombo );
        register( this::checkDataValueDataSetIsNotLocked );
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
            context.error().addConflict( DataValueSetImportConflict.DATASET_NOT_FOUND, dataValueSet.getDataSet() );
        }
    }

    private void validateDataSetIsAccessibleByUser( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        DataSet dataSet = dataSetContext.getDataSet();
        if ( dataSet != null && !aclService.canDataWrite( context.getCurrentUser(), dataSet ) )
        {
            context.error().addConflict( DataValueSetImportConflict.DATASET_NOT_ACCESSIBLE, dataValueSet.getDataSet() );
        }
    }

    private static void validateDataSetExistsStrictDataElements( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        if ( dataSetContext.getDataSet() == null && context.isStrictDataElements() )
        {
            context.error().addConflict( DataValueSetImportConflict.DATASET_NOT_VALID );
        }
    }

    private static void validateDataSetOrgUnitExists( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        if ( dataSetContext.getOuterOrgUnit() == null && trimToNull( dataValueSet.getOrgUnit() ) != null )
        {
            context.error().addConflict( DataValueSetImportConflict.ORG_UNIT_NOT_FOUND,
                dataValueSet.getOrgUnit(), dataValueSet.getDataSet() );
        }
    }

    private static void validateDataSetAttrOptionComboExists( DataValueSet dataValueSet, ImportContext context,
        DataSetContext dataSetContext )
    {
        if ( dataSetContext.getOuterAttrOptionCombo() == null
            && trimToNull( dataValueSet.getAttributeOptionCombo() ) != null )
        {
            context.error().addConflict( DataValueSetImportConflict.ATTR_OPTION_COMBO_NOT_FOUND,
                dataValueSet.getAttributeOptionCombo(), dataValueSet.getDataSet() );
        }
    }

    /*
     * DataValue validation
     */

    public boolean skipDataValue( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        ImportSummary summary = context.getSummary();
        int skippedBefore = summary.skippedValueCount();
        int totalConflictsBefore = summary.getTotalConflictOccurrenceCount();
        for ( DataValueValidation validation : dataValueValidations )
        {
            validation.validate( dataValue, context, dataSetContext, valueContext );
            if ( summary.skippedValueCount() > skippedBefore
                || summary.getTotalConflictOccurrenceCount() > totalConflictsBefore )
            {
                return true;
            }
        }
        return false;
    }

    private static void validateDataValueDataElementExists( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getDataElement() == null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.DATA_ELEMENT_NOT_FOUND, dataValue.getDataElement() );
        }
    }

    private static void validateDataValuePeriodExists( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getPeriod() == null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.PERIOD_NOT_VALID, dataValue.getPeriod() );
        }
    }

    private static void validateDataValueOrgUnitExists( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getOrgUnit() == null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.ORG_UNIT_NOT_FOUND, dataValue.getOrgUnit() );
        }
    }

    private static void validateDataValueCategoryOptionComboExists( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getCategoryOptionCombo() == null
            && trimToNull( dataValue.getCategoryOptionCombo() ) != null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.CATEGORY_OPTION_COMBO_NOT_FOUND, dataValue.getCategoryOptionCombo() );
        }
    }

    private void validateDataValueCategoryOptionComboAccess( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getCategoryOptionCombo() != null )
        {
            for ( CategoryOption option : valueContext.getCategoryOptionCombo().getCategoryOptions() )
            {
                if ( !aclService.canDataWrite( context.getCurrentUser(), option ) )
                {
                    context.addConflict( valueContext.getIndex(),
                        DataValueImportConflict.CATEGORY_OPTION_COMBO_NOT_ACCESSIBLE,
                        dataValue.getCategoryOptionCombo(), option.getUid() );
                }
            }
        }
    }

    private static void validateDataValueAttrOptionComboExists( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getAttrOptionCombo() == null
            && trimToNull( dataValue.getAttributeOptionCombo() ) != null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.ATTR_OPTION_COMBO_NOT_FOUND, dataValue.getAttributeOptionCombo() );
        }
    }

    private void validateDataValueAttrOptionComboAccess( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getAttrOptionCombo() != null )
        {
            for ( CategoryOption option : valueContext.getAttrOptionCombo().getCategoryOptions() )
            {
                if ( !aclService.canDataWrite( context.getCurrentUser(), option ) )
                {
                    context.addConflict( valueContext.getIndex(),
                        DataValueImportConflict.ATTR_OPTION_COMBO_NOT_ACCESSIBLE,
                        dataValue.getAttributeOptionCombo(), option.getUid() );
                }
            }
        }
    }

    private void validateDataValueOrgUnitInUserHierarchy( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        User currentUser = context.getCurrentUser();
        if ( currentUser == null )
        {
            return;
        }
        boolean inUserHierarchy = context.getOrgUnitInHierarchyMap().get( valueContext.getOrgUnit().getUid(),
            () -> organisationUnitService.isDescendant( valueContext.getOrgUnit(), context.getCurrentOrgUnits() ) );

        if ( !inUserHierarchy )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.ORG_UNIT_NOT_IN_USER_HIERARCHY,
                dataValue.getOrgUnit(), currentUser.getUid() );
        }
    }

    private static void validateDataValueIsDefined( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( dataValue.isNullValue() && !dataValue.isDeletedValue() && !context.getStrategy().isDelete() )
        {
            context.addConflict( valueContext.getIndex(), DataValueImportConflict.DATA_ELEMENT_VALUE_NOT_DEFINED,
                dataValue.getDataElement() );
        }
    }

    private static void validateDataValueIsValid( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        String value = ValidationUtils.normalizeBoolean( dataValue.getValue(),
            valueContext.getDataElement().getValueType() );

        String errorKey = ValidationUtils.valueIsValid( value, valueContext.getDataElement(), false );

        if ( errorKey != null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.DATA_ELEMENT_VALUE_NOT_VALID,
                dataValue.getDataElement(), errorKey );
        }
    }

    private static void validateDataValueCommentIsValid( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        String errorKey = ValidationUtils.commentIsValid( dataValue.getComment() );

        if ( errorKey != null )
        {
            context.addConflict( valueContext.getIndex(), DataValueImportConflict.COMMENT_NOT_VALID, errorKey );
        }
    }

    private static void validateDataValueOptionsExist( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        DataElement de = valueContext.getDataElement();
        if ( !de.hasOptionSet() )
        {
            return;
        }
        Set<String> optionCodes = context.getDataElementOptionsMap().get( de.getUid(),
            () -> de.getOptionSet().getOptionCodesAsSet() );
        ValueType valueType = de.getValueType();
        String value = dataValue.getValue();
        boolean invalid = valueType != ValueType.MULTI_TEXT
            ? !optionCodes.contains( value )
            : !optionCodes.containsAll( ValueType.splitMultiText( value ) );
        if ( invalid )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.DATA_ELEMENT_INVALID_OPTION, dataValue.getDataElement() );
        }
    }

    /*
     * DataValue Constraints
     */

    private static void checkDataValueCategoryOptionCombo( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getCategoryOptionCombo() == null )
        {
            if ( context.isRequireCategoryOptionCombo() )
            {
                context.addConflict( valueContext.getIndex(),
                    DataValueImportConflict.CATEGORY_OPTION_COMBO_NOT_SPECIFIED );
            }
            else
            {
                valueContext.setCategoryOptionCombo( dataSetContext.getFallbackCategoryOptionCombo() );
            }
        }
    }

    private static void checkDataValueAttrOptionCombo( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( valueContext.getAttrOptionCombo() == null )
        {
            if ( context.isRequireAttrOptionCombo() )
            {
                context.addConflict( valueContext.getIndex(),
                    DataValueImportConflict.ATTR_OPTION_COMBO_NOT_SPECIFIED );
            }
            else
            {
                valueContext.setAttrOptionCombo( dataSetContext.getFallbackCategoryOptionCombo() );
            }
        }
    }

    private static void checkDataValuePeriodType( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictPeriods() && !context.getDataElementPeriodTypesMap()
            .get( valueContext.getDataElement().getUid(),
                valueContext.getDataElement()::getPeriodTypes )
            .contains( valueContext.getPeriod().getPeriodType() ) )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.PERIOD_TYPE_NOT_VALID_FOR_DATA_ELEMENT,
                dataValue.getPeriod(), dataValue.getDataElement() );
        }
    }

    private static void checkDataValueStrictDataElement( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( !context.isStrictDataElements() )
        {
            return;
        }
        List<DataSet> targets = context.getTargetDataSets( dataSetContext, valueContext );
        if ( targets.stream()
            .noneMatch( dataSet -> dataSet.getDataElements().contains( valueContext.getDataElement() ) ) )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.DATA_ELEMENT_STRICT,
                dataValue.getDataElement(), dataSetContext.getDataSet().getUid() );
        }
    }

    private static void checkDataValueStrictCategoryOptionCombos( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictCategoryOptionCombos()
            && !context.getDataElementCategoryOptionComboMap().get( valueContext.getDataElement().getUid(),
                valueContext.getDataElement()::getCategoryOptionCombos )
                .contains( valueContext.getCategoryOptionCombo() ) )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.CATEGORY_OPTION_COMBO_STRICT,
                dataValue.getCategoryOptionCombo(), dataValue.getDataElement() );
        }
    }

    private static void checkDataValueStrictAttrOptionCombos( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictAttrOptionCombos()
            && !context.getDataElementAttrOptionComboMap().get( valueContext.getDataElement().getUid(),
                valueContext.getDataElement()::getDataSetCategoryOptionCombos )
                .contains( valueContext.getAttrOptionCombo() ) )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.ATTR_OPTION_COMBO_STRICT,
                dataValue.getAttributeOptionCombo(), dataValue.getDataElement() );
        }
    }

    private static void checkDataValueStrictOrgUnits( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.isStrictOrgUnits()
            && BooleanUtils
                .isFalse( context.getDataElementOrgUnitMap().get(
                    valueContext.getDataElement().getUid() + valueContext.getOrgUnit().getUid(),
                    () -> valueContext.getOrgUnit().hasDataElement( valueContext.getDataElement() ) ) ) )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.ORG_UNIT_STRICT, dataValue.getOrgUnit(), dataValue.getDataElement() );
        }
    }

    private static void checkDataValueStoredByIsValid( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        String errorKey = ValidationUtils.storedByIsValid( dataValue.getStoredBy() );

        if ( errorKey != null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.STORED_BY_NOT_VALID, errorKey );
        }
    }

    private static void checkDataValuePeriodWithinAttrOptionComboRange( DataValueEntry dataValue, ImportContext context,
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
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.PERIOD_NOT_VALID_FOR_ATTR_OPTION_COMBO,
                dataValue.getPeriod(), dataValue.getAttributeOptionCombo() );
        }
    }

    private void checkDataValueOrgUnitValidForAttrOptionCombo( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( !context.getAttrOptionComboOrgUnitMap()
            .get( valueContext.getAttrOptionCombo().getUid() + valueContext.getOrgUnit().getUid(),
                () -> isOrgUnitValidForAttrOptionCombo( valueContext ) ) )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.ORG_UNIT_NOT_VALID_FOR_ATTR_OPTION_COMBO,
                dataValue.getOrgUnit(), dataValue.getAttributeOptionCombo() );
        }
    }

    private boolean isOrgUnitValidForAttrOptionCombo( DataValueContext valueContext )
    {
        Set<OrganisationUnit> aocOrgUnits = valueContext.getAttrOptionCombo().getOrganisationUnits();
        return aocOrgUnits == null || organisationUnitService.isDescendant( valueContext.getOrgUnit(), aocOrgUnits );
    }

    private static void checkDataValueTargetDataSets( ImportContext context, DataSetContext dataSetContext,
        DataValueContext valueContext, boolean strict, Consumer<DataSet> validation )
    {
        if ( context.isForceDataInput() )
            return; // skip this validation
        List<DataSet> dataSets = context.getTargetDataSets( dataSetContext, valueContext );
        if ( dataSets.isEmpty() )
        {
            return; // if there are no data sets data input is valid as well
        }

        context.stageConflicts();
        for ( DataSet dataSet : dataSets )
        {
            int conflictsBefore = context.getStagedConflictsCount();

            validation.accept( dataSet );

            if ( !strict && conflictsBefore == context.getStagedConflictsCount() )
            {
                context.discardConflicts();
                return; // found a data set that had none of the above issues =>
                       // OK
            }
        }
        context.commitConflicts();
    }

    private void checkDataValueDataSetIsNotLocked( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        checkDataValueTargetDataSets( context, dataSetContext, valueContext, context.isStrictDataSetLocking(),
            dataSet -> {
                String key = dataSet.getUid() + valueContext.getPeriod().getUid() + valueContext.getOrgUnit().getUid();
                if ( context.getDataSetLockedMap().get( key,
                    () -> isLocked( context.getCurrentUser(), dataSet, valueContext.getPeriod(),
                        valueContext.getOrgUnit(), context.isSkipLockExceptionCheck() ) ) )
                {
                    context.addConflict( valueContext.getIndex(),
                        DataValueImportConflict.PERIOD_EXPIRED, dataValue.getPeriod(), dataSet.getUid() );
                }
            } );
    }

    private static void checkDataValueNotAfterLatestOpenFuturePeriod( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        checkDataValueTargetDataSets( context, dataSetContext, valueContext, true, dataSet -> {
            // is the period within the range of future periods for the dataset?
            PeriodType periodType = dataSet.getPeriodType();
            Period latestFuturePeriod = context.getDataSetLatestFuturePeriodMap().get( dataSet.getUid(),
                () -> periodType.getFuturePeriod( dataSet.getOpenFuturePeriods() ) );
            if ( context.isIso8601() && valueContext.getPeriod().isAfter( latestFuturePeriod ) )
            {
                context.addConflict( valueContext.getIndex(), DataValueImportConflict.PERIOD_AFTER_DATA_ELEMENT_PERIODS,
                    dataValue.getPeriod(), dataValue.getDataElement(), dataSet.getUid(),
                    latestFuturePeriod.getIsoDate() );
            }
        } );
    }

    private void checkDataValueNotAlreadyApproved( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        checkDataValueTargetDataSets( context, dataSetContext, valueContext, context.isStrictDataSetApproval(),
            dataSet -> {
                // is the dataset not already approved?
                DataApprovalWorkflow workflow = dataSet.getWorkflow();
                if ( workflow != null )
                { // no workflow => approval not used => OK to add data
                    final String workflowPeriodAoc = workflow.getUid() + valueContext.getPeriod().getUid()
                        + valueContext.getAttrOptionCombo().getUid();

                    if ( context.getApprovalMap().get( valueContext.getOrgUnit().getUid() + workflowPeriodAoc,
                        () -> approvalService.isApproved( workflow, valueContext.getPeriod(),
                            valueContext.getOrgUnit(), valueContext.getAttrOptionCombo() ) ) )
                    {
                        context.addConflict( valueContext.getIndex(), DataValueImportConflict.VALUE_ALREADY_APPROVED,
                            dataValue.getOrgUnit(), dataValue.getPeriod(), dataValue.getAttributeOptionCombo(),
                            dataSet.getUid() );
                    }
                }
            } );
    }

    private static void checkDataValuePeriodIsOpenNow( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        checkDataValueTargetDataSets( context, dataSetContext, valueContext, context.isStrictDataSetInputPeriods(),
            dataSet -> {
                // is data input allowed now? (data of "past" periods cannot be entered any more)
                if ( !dataSet.isDataInputPeriodAndDateAllowed( valueContext.getPeriod(), new Date() ) )
                {
                    context.addConflict( valueContext.getIndex(), DataValueImportConflict.PERIOD_NOT_OPEN_FOR_DATA_SET,
                        dataValue.getPeriod(), dataSet.getUid() );
                }
            } );
    }

    private static void checkDataValueConformsToOpenPeriodsOfAssociatedDataSets( DataValueEntry dataValue,
        ImportContext context, DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( !context.isForceDataInput()
            && !context.getPeriodOpenForDataElement().get(
                valueContext.getDataElement().getUid() + valueContext.getPeriod().getIsoDate(),
                () -> valueContext.getDataElement().isDataInputAllowedForPeriodAndDate( valueContext.getPeriod(),
                    new Date() ) ) )
        {
            context.addConflict( valueContext.getIndex(), DataValueImportConflict.PERIOD_NOT_CONFORM_TO_OPEN_PERIODS,
                dataValue.getPeriod() );
        }
    }

    private void checkDataValueFileResourceExists( DataValueEntry dataValue, ImportContext context,
        DataSetContext dataSetContext, DataValueContext valueContext )
    {
        if ( context.getStrategy().isDelete() && valueContext.getDataElement().isFileType()
            && valueContext.getActualDataValue( dataValueService ) == null )
        {
            context.addConflict( valueContext.getIndex(),
                DataValueImportConflict.FILE_RESOURCE_NOT_FOUND, dataValue.getDataElement() );
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
