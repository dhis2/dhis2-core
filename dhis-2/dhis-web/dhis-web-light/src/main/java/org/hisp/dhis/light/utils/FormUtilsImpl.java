package org.hisp.dhis.light.utils;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hisp.dhis.dataanalysis.DataAnalysisService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.filter.OrganisationUnitWithDataSetsFilter;
import org.hisp.dhis.system.filter.PastAndCurrentPeriodFilter;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.validation.ValidationResult;
import org.hisp.dhis.validation.ValidationRule;
import org.hisp.dhis.validation.ValidationRuleService;
import org.joda.time.DateTime;

import com.google.common.collect.Sets;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class FormUtilsImpl
    implements FormUtils
{
    public static final Integer DEFAULT_MAX_PERIODS = 10;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }

    private DataValueService dataValueService;

    public void setDataValueService( DataValueService dataValueService )
    {
        this.dataValueService = dataValueService;
    }

    private DataSetService dataSetService;

    public void setDataSetService( DataSetService dataSetService )
    {
        this.dataSetService = dataSetService;
    }

    private DataAnalysisService stdDevOutlierAnalysisService;

    public void setStdDevOutlierAnalysisService( DataAnalysisService stdDevOutlierAnalysisService )
    {
        this.stdDevOutlierAnalysisService = stdDevOutlierAnalysisService;
    }

    private DataAnalysisService minMaxOutlierAnalysisService;

    public void setMinMaxOutlierAnalysisService( DataAnalysisService minMaxOutlierAnalysisService )
    {
        this.minMaxOutlierAnalysisService = minMaxOutlierAnalysisService;
    }

    private SystemSettingManager systemSettingManager;

    public void setSystemSettingManager( SystemSettingManager systemSettingManager )
    {
        this.systemSettingManager = systemSettingManager;
    }

    private ValidationRuleService validationRuleService;

    public void setValidationRuleService( ValidationRuleService validationRuleService )
    {
        this.validationRuleService = validationRuleService;
    }

    private ExpressionService expressionService;

    public void setExpressionService( ExpressionService expressionService )
    {
        this.expressionService = expressionService;
    }

    // -------------------------------------------------------------------------
    // Utils
    // -------------------------------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, DeflatedDataValue> getValidationViolations( OrganisationUnit organisationUnit,
        Collection<DataElement> dataElements, Period period )
    {
        Map<String, DeflatedDataValue> validationErrorMap = new HashMap<>();

        Double factor = (Double) systemSettingManager.getSystemSetting( SettingKey.FACTOR_OF_DEVIATION );

        Date from = new DateTime( period.getStartDate() ).minusYears( 2 ).toDate();
        
        Collection<DeflatedDataValue> stdDevs = stdDevOutlierAnalysisService.analyse(
            Sets.newHashSet( organisationUnit ), dataElements, Sets.newHashSet( period ), factor, from );

        Collection<DeflatedDataValue> minMaxs = minMaxOutlierAnalysisService.analyse(
            Sets.newHashSet( organisationUnit ), dataElements, Sets.newHashSet( period ), null, from );

        Collection<DeflatedDataValue> deflatedDataValues = CollectionUtils.union( stdDevs, minMaxs );

        for ( DeflatedDataValue deflatedDataValue : deflatedDataValues )
        {
            String key = String.format( "DE%dOC%d", deflatedDataValue.getDataElementId(),
                deflatedDataValue.getCategoryOptionComboId() );
            validationErrorMap.put( key, deflatedDataValue );
        }

        return validationErrorMap;
    }

    @Override
    public List<String> getValidationRuleViolations( OrganisationUnit organisationUnit, DataSet dataSet, Period period )
    {
        List<ValidationResult> validationRuleResults = new ArrayList<>( validationRuleService.validate(
            dataSet, period, organisationUnit, null ) );

        List<String> validationRuleViolations = new ArrayList<>( validationRuleResults.size() );

        for ( ValidationResult result : validationRuleResults )
        {
            ValidationRule rule = result.getValidationRule();

            StringBuilder sb = new StringBuilder();
            sb.append( expressionService.getExpressionDescription( rule.getLeftSide().getExpression() ) );
            sb.append( " " ).append( rule.getOperator().getMathematicalOperator() ).append( " " );
            sb.append( expressionService.getExpressionDescription( rule.getRightSide().getExpression() ) );

            validationRuleViolations.add( sb.toString() );
        }

        return validationRuleViolations;
    }

    @Override
    public Map<String, String> getDataValueMap( OrganisationUnit organisationUnit, DataSet dataSet, Period period )
    {
        Map<String, String> dataValueMap = new HashMap<>();
        List<DataValue> values = new ArrayList<>( dataValueService.getDataValues( dataSet.getDataElements(), 
            Sets.newHashSet( period ), Sets.newHashSet( organisationUnit ) ) );

        for ( DataValue dataValue : values )
        {
            DataElement dataElement = dataValue.getDataElement();
            DataElementCategoryOptionCombo optionCombo = dataValue.getCategoryOptionCombo();

            String key = String.format( "DE%dOC%d", dataElement.getId(), optionCombo.getId() );
            String value = dataValue.getValue();

            dataValueMap.put( key, value );
        }

        return dataValueMap;
    }

    @Override
    public List<OrganisationUnit> organisationUnitWithDataSetsFilter( Collection<OrganisationUnit> organisationUnits )
    {
        List<OrganisationUnit> ous = new ArrayList<>( organisationUnits );
        FilterUtils.filter( ous, new OrganisationUnitWithDataSetsFilter() );

        return ous;
    }

    @Override
    public List<OrganisationUnit> getSortedOrganisationUnitsForCurrentUser()
    {
        User user = currentUserService.getCurrentUser();
        Validate.notNull( user );

        List<OrganisationUnit> organisationUnits = new ArrayList<>( user.getOrganisationUnits() );
        Collections.sort( organisationUnits );

        return organisationUnitWithDataSetsFilter( organisationUnits );
    }

    @Override
    public List<DataSet> getDataSetsForCurrentUser( Integer organisationUnitId )
    {
        Validate.notNull( organisationUnitId );

        OrganisationUnit organisationUnit = organisationUnitService.getOrganisationUnit( organisationUnitId );

        List<DataSet> dataSets = new ArrayList<>( organisationUnit.getDataSets() );

        UserCredentials userCredentials = currentUserService.getCurrentUser().getUserCredentials();

        if ( !userCredentials.isSuper() )
        {
            dataSets.retainAll( userCredentials.getAllDataSets() );
        }

        return dataSets;
    }

    @Override
    public List<Period> getPeriodsForDataSet( Integer dataSetId )
    {
        return getPeriodsForDataSet( dataSetId, 0, DEFAULT_MAX_PERIODS );
    }

    @Override
    public List<Period> getPeriodsForDataSet( Integer dataSetId, int first, int max )
    {
        Validate.notNull( dataSetId );

        DataSet dataSet = dataSetService.getDataSet( dataSetId );

        CalendarPeriodType periodType;

        if ( dataSet.getPeriodType().getName().equalsIgnoreCase( "Yearly" ) )
        {
            periodType = new YearlyPeriodType();
        }
        else
        {
            periodType = (CalendarPeriodType) dataSet.getPeriodType();
        }

        //TODO implement properly
        
        if ( dataSet.getOpenFuturePeriods() > 0 )
        {
            List<Period> periods = periodType.generatePeriods( new Date() );
            Collections.reverse( periods );
            return periods;
        }
        else
        {
            List<Period> periods = periodType.generateLast5Years( new Date() );
            FilterUtils.filter( periods, new PastAndCurrentPeriodFilter() );
            Collections.reverse( periods );

            if ( periods.size() > (first + max) )
            {
                periods = periods.subList( first, max );
            }

            return periods;
        }
    }
}
