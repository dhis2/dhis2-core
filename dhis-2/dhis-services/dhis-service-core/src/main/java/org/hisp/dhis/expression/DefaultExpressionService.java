package org.hisp.dhis.expression;

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

import com.google.common.collect.Sets;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.RegexUtils;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.system.jep.CustomFunctions;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.validation.ValidationRule;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.hisp.dhis.expression.Expression.*;
import static org.hisp.dhis.expression.MissingValueStrategy.*;
import static org.hisp.dhis.system.util.MathUtils.*;

/**
 * The expression is a string describing a formula containing data element ids
 * and category option combo ids. The formula can potentially contain references
 * to data element totals.
 *
 * @author Margrethe Store
 * @author Lars Helge Overland
 */
public class DefaultExpressionService
    implements ExpressionService
{
    private static final Log log = LogFactory.getLog( DefaultExpressionService.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private GenericStore<Expression> expressionStore;

    public void setExpressionStore( GenericStore<Expression> expressionStore )
    {
        this.expressionStore = expressionStore;
    }

    private DataElementService dataElementService;

    public void setDataElementService( DataElementService dataElementService )
    {
        this.dataElementService = dataElementService;
    }

    private ConstantService constantService;

    public void setConstantService( ConstantService constantService )
    {
        this.constantService = constantService;
    }

    private DataElementCategoryService categoryService;

    public void setCategoryService( DataElementCategoryService categoryService )
    {
        this.categoryService = categoryService;
    }

    private OrganisationUnitGroupService organisationUnitGroupService;

    public void setOrganisationUnitGroupService( OrganisationUnitGroupService organisationUnitGroupService )
    {
        this.organisationUnitGroupService = organisationUnitGroupService;
    }

    private DimensionService dimensionService;

    public void setDimensionService( DimensionService dimensionService )
    {
        this.dimensionService = dimensionService;
    }

    private IdentifiableObjectManager idObjectManager;

    public void setIdObjectManager( IdentifiableObjectManager idObjectManager )
    {
        this.idObjectManager = idObjectManager;
    }

    // -------------------------------------------------------------------------
    // Expression CRUD operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public int addExpression( Expression expression )
    {
        return expressionStore.save( expression );
    }

    @Override
    @Transactional
    public void deleteExpression( Expression expression )
    {
        expressionStore.delete( expression );
    }

    @Override
    @Transactional
    public Expression getExpression( int id )
    {
        return expressionStore.get( id );
    }

    @Override
    @Transactional
    public void updateExpression( Expression expression )
    {
        expressionStore.update( expression );
    }

    @Override
    @Transactional
    public List<Expression> getAllExpressions()
    {
        return expressionStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Business logic
    // -------------------------------------------------------------------------
    
    @Override
    public Double getIndicatorValue( Indicator indicator, Period period,
        Map<? extends DimensionalItemObject, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap )
    {
        IndicatorValue value = getIndicatorValueObject( indicator, period, valueMap, constantMap, orgUnitCountMap );
        
        return value != null ? value.getValue() : null;
    }

    @Override
    public IndicatorValue getIndicatorValueObject( Indicator indicator, Period period,
        Map<? extends DimensionalItemObject, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap )
    {
        if ( indicator == null || indicator.getExplodedNumeratorFallback() == null
            || indicator.getExplodedDenominatorFallback() == null )
        {
            return null;
        }

        Integer days = period != null ? period.getDaysInPeriod() : null;

        final String denominatorExpression = generateExpression( indicator.getExplodedDenominatorFallback(), valueMap,
            constantMap, orgUnitCountMap, days, NEVER_SKIP );

        if ( denominatorExpression == null )
        {
            return null;
        }

        final double denominatorValue = calculateExpression( denominatorExpression );

        if ( !isEqual( denominatorValue, 0d ) )
        {
            final String numeratorExpression = generateExpression( indicator.getExplodedNumeratorFallback(), valueMap,
                constantMap, orgUnitCountMap, days, NEVER_SKIP );

            if ( numeratorExpression == null )
            {
                return null;
            }

            final double numeratorValue = calculateExpression( numeratorExpression );

            final double annualizationFactor = period != null ?
                DateUtils.getAnnualizationFactor( indicator, period.getStartDate(), period.getEndDate() ) : 1d;
            final int factor = indicator.getIndicatorType().getFactor();
            
            return new IndicatorValue()
                .setNumeratorValue( numeratorValue )
                .setDenominatorValue( denominatorValue )
                .setFactor( factor )
                .setAnnualizationFactor( annualizationFactor );
        }

        return null;
    }

    @Override
    public Double getExpressionValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days )
    {
        return getExpressionValue( expression, valueMap, constantMap, orgUnitCountMap, days, null, null );
    }

    @Override
    public Double getExpressionValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        Set<DataElementOperand> incompleteValues )
    {
        return getExpressionValue( expression, valueMap, constantMap, orgUnitCountMap, days, incompleteValues, null );
    }

    @Override
    public Double getExpressionValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        Set<DataElementOperand> incompleteValues, ListMap<String, Double> aggregateMap )
    {
        if ( aggregateMap == null )
        {
            Matcher simpleMatch = OPERAND_PATTERN.matcher( expression.getExpression() );
            
            if ( simpleMatch.matches() )
            {
                return getSimpleExpressionValue( expression, simpleMatch, valueMap );
            }
        }

        String expressionString = generateExpression( expression.getExplodedExpressionFallback(), valueMap, constantMap,
            orgUnitCountMap, days, expression.getMissingValueStrategy(),
            incompleteValues, aggregateMap );

        return expressionString != null ? calculateExpression( expressionString ) : null;
    }

    private Double getSimpleExpressionValue( Expression expression, Matcher expressionMatch,
        Map<? extends DimensionalItemObject, Double> valueMap )
    {
        String elementId = expressionMatch.group( 1 );
        String comboId = expressionMatch.group( 2 );
        DataElement dataElement = dataElementService.getDataElement( elementId );
        
        if ( comboId != null && comboId.length() == 0 )
        {
            comboId = null;
        }
        
        final DataElementCategoryCombo categoryCombo = dataElement.getCategoryCombo();
        final DataElementCategoryOptionCombo defaultCombo = categoryService.getDefaultDataElementCategoryOptionCombo();
        
        if ( comboId == null || comboId == defaultCombo.getUid() )
        {
            Double value = valueMap.get( dataElement );
            
            if ( value != null )
            {
                return value;
            }
        }
        
        if ( comboId == null )
        {
            Double sum = 0.0;
            final Set<DataElementCategoryOptionCombo> combos = categoryCombo.getOptionCombos();

            for ( DataElementCategoryOptionCombo combo : combos )
            {
                DataElementOperand deo = new DataElementOperand( elementId, combo.getUid() );
                Double value = valueMap.get( deo );

                if ( value != null )
                {
                    sum = sum + value;
                }
            }
            
            return sum;
        }
        else
        {
            DataElementOperand deo = new DataElementOperand( elementId, comboId );

            return valueMap.get( deo );
        }
    }

    @Override
    public Object getExpressionObjectValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        Set<DataElementOperand> incompleteValues, ListMap<String, Double> aggregateMap )
    {
        String expressionString = generateExpression( expression.getExplodedExpressionFallback(), valueMap, constantMap,
            orgUnitCountMap, days, expression.getMissingValueStrategy(), incompleteValues, aggregateMap );

        Object result = expressionString != null ? calculateGenericExpression( expressionString ) : null;

        return result;
    }

    @Override
    @Transactional
    public Set<DataElement> getDataElementsInExpression( String expression )
    {
        return getDataElementsInExpressionInternal( OPERAND_PATTERN, expression );
    }

    private Set<DataElement> getDataElementsInExpressionInternal( Pattern pattern, String expression )
    {
        Set<DataElement> dataElements = new HashSet<>();

        if ( expression != null )
        {
            final Matcher matcher = pattern.matcher( expression );

            while ( matcher.find() )
            {
                final DataElement dataElement = dataElementService.getDataElement( matcher.group( 1 ) );

                if ( dataElement != null )
                {
                    dataElements.add( dataElement );
                }
            }
        }

        return dataElements;
    }

    @Override
    @Transactional
    public Set<DataElementCategoryOptionCombo> getOptionCombosInExpression( String expression )
    {
        Set<DataElementCategoryOptionCombo> optionCombosInExpression = new HashSet<>();

        if ( expression != null )
        {
            final Matcher matcher = OPTION_COMBO_OPERAND_PATTERN.matcher( expression );

            while ( matcher.find() )
            {
                DataElementCategoryOptionCombo categoryOptionCombo = categoryService
                    .getDataElementCategoryOptionCombo( matcher.group( 2 ) );

                if ( categoryOptionCombo != null )
                {
                    optionCombosInExpression.add( categoryOptionCombo );
                }
            }
        }

        return optionCombosInExpression;
    }

    @Override
    @Transactional
    public Set<DataElementOperand> getOperandsInExpression( String expression )
    {
        Set<DataElementOperand> operandsInExpression = new HashSet<>();

        if ( expression != null )
        {
            final Matcher matcher = OPTION_COMBO_OPERAND_PATTERN.matcher( expression );

            while ( matcher.find() )
            {
                DataElementOperand operand = DataElementOperand.getOperand( matcher.group() );

                if ( operand.getOptionComboId() != null )
                {
                    operandsInExpression.add( operand );
                }
            }
        }

        return operandsInExpression;
    }

    @Override
    @Transactional
    public Set<BaseDimensionalItemObject> getDataInputsInExpression( String expression )
    {
        Set<BaseDimensionalItemObject> results=new HashSet<BaseDimensionalItemObject>();

        results.addAll( getDataElementsInExpression( expression ) );
        results.addAll( getOperandsInExpression( expression ) );

        return results;
    }

    @Override
    @Transactional
    public Set<String> getAggregatesInExpression( String expression )
    {
        Pattern prefix = CustomFunctions.AGGREGATE_PATTERN_PREFIX;
        Set<String> aggregates = new HashSet<>();

        if ( expression != null )
        {
            final Matcher matcher = prefix.matcher( expression );

            int scan = 0;
            int len = expression.length();

            while ( (scan < len) && (matcher.find( scan )) )
            {
                int start = matcher.end();
                int end = Expression.matchExpression( expression, start );

                if ( end < 0 )
                {
                    log.warn( "Bad expression starting at " + start + " in " + expression );
                }
                else if ( end > 0 )
                {
                    aggregates.add( expression.substring( start, end ) );
                    scan = end + 1;
                }
                else
                {
                    scan = start + 1;
                }
            }
        }

        return aggregates;
    }

    @Override
    @Transactional
    public Set<DataElement> getSampleElementsInExpression( String expression )
    {
        Set<String> aggregates = getAggregatesInExpression( expression );
        
        HashSet<DataElement> elements = new HashSet<DataElement>();
        
        if ( aggregates.size() > 0 )
        {
            for ( String aggregate_expression : aggregates )
            {
                elements.addAll( getDataElementsInExpressionInternal( 
                    OPERAND_PATTERN, aggregate_expression ) );
            }
        }
        
        return elements;
    }

    @Override
    @Transactional
    public Set<DataElement> getDataElementsInIndicators( Collection<Indicator> indicators )
    {
        Set<DataElement> dataElements = new HashSet<>();

        for ( Indicator indicator : indicators )
        {
            dataElements.addAll( getDataElementsInExpression( indicator.getNumerator() ) );
            dataElements.addAll( getDataElementsInExpression( indicator.getDenominator() ) );
        }

        return dataElements;
    }

    @Override
    @Transactional
    public Set<DataElement> getDataElementTotalsInIndicators( Collection<Indicator> indicators )
    {
        Set<DataElement> dataElements = new HashSet<>();

        for ( Indicator indicator : indicators )
        {
            dataElements
                .addAll( getDataElementsInExpressionInternal( DATA_ELEMENT_TOTAL_PATTERN, indicator.getNumerator() ) );
            dataElements.addAll(
                getDataElementsInExpressionInternal( DATA_ELEMENT_TOTAL_PATTERN, indicator.getDenominator() ) );
        }

        return dataElements;
    }

    @Override
    @Transactional
    public Set<DataElement> getDataElementWithOptionCombosInIndicators( Collection<Indicator> indicators )
    {
        Set<DataElement> dataElements = new HashSet<>();

        for ( Indicator indicator : indicators )
        {
            dataElements.addAll( getDataElementsInExpressionInternal( OPTION_COMBO_OPERAND_PATTERN, indicator.getNumerator() ) );
            dataElements.addAll( getDataElementsInExpressionInternal( OPTION_COMBO_OPERAND_PATTERN, indicator.getDenominator() ) );
        }

        return dataElements;
    }

    @Override
    public Set<DimensionalItemObject> getDimensionalItemObjectsInExpression( String expression )
    {
        Set<DimensionalItemObject> dimensionItems = Sets.newHashSet();

        if ( expression == null || expression.isEmpty() )
        {
            return dimensionItems;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String dimensionItem = matcher.group( 2 );

            DimensionalItemObject dimensionItemObject = dimensionService.getDataDimensionalItemObject( dimensionItem );

            if ( dimensionItemObject != null )
            {
                dimensionItems.add( dimensionItemObject );
            }
        }

        return dimensionItems;
    }

    @Override
    public Set<DimensionalItemObject> getDimensionalItemObjectsInIndicators( Collection<Indicator> indicators )
    {
        Set<DimensionalItemObject> items = Sets.newHashSet();

        for ( Indicator indicator : indicators )
        {
            items.addAll( getDimensionalItemObjectsInExpression( indicator.getNumerator() ) );
            items.addAll( getDimensionalItemObjectsInExpression( indicator.getDenominator() ) );
        }

        return items;
    }

    @Override
    public Set<OrganisationUnitGroup> getOrganisationUnitGroupsInExpression( String expression )
    {
        Set<OrganisationUnitGroup> groupsInExpression = new HashSet<>();

        if ( expression != null )
        {
            final Matcher matcher = OU_GROUP_PATTERN.matcher( expression );

            while ( matcher.find() )
            {
                final OrganisationUnitGroup group = organisationUnitGroupService
                    .getOrganisationUnitGroup( matcher.group( 1 ) );

                if ( group != null )
                {
                    groupsInExpression.add( group );
                }
            }
        }

        return groupsInExpression;
    }

    @Override
    public Set<OrganisationUnitGroup> getOrganisationUnitGroupsInIndicators( Collection<Indicator> indicators )
    {
        Set<OrganisationUnitGroup> groups = new HashSet<>();

        if ( indicators != null )
        {
            for ( Indicator indicator : indicators )
            {
                groups.addAll( getOrganisationUnitGroupsInExpression( indicator.getNumerator() ) );
                groups.addAll( getOrganisationUnitGroupsInExpression( indicator.getDenominator() ) );
            }
        }

        return groups;
    }

    @Override
    @Transactional
    public void filterInvalidIndicators( List<Indicator> indicators )
    {
        if ( indicators != null )
        {
            Iterator<Indicator> iterator = indicators.iterator();

            while ( iterator.hasNext() )
            {
                Indicator indicator = iterator.next();

                if ( !expressionIsValid( indicator.getNumerator() ).isValid()
                    || !expressionIsValid( indicator.getDenominator() ).isValid() )
                {
                    iterator.remove();
                    log.warn( "Indicator is invalid: " + indicator + ", " + indicator.getNumerator() + ", "
                        + indicator.getDenominator() );
                }
            }
        }
    }

    @Override
    @Transactional
    public ExpressionValidationOutcome expressionIsValid( String expression )
    {
        if ( expression == null || expression.isEmpty() )
        {
            return ExpressionValidationOutcome.EXPRESSION_IS_EMPTY;
        }

        // ---------------------------------------------------------------------
        // Operands
        // ---------------------------------------------------------------------

        StringBuffer sb = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String dimensionItem = matcher.group( 2 );

            if ( dimensionService.getDataDimensionalItemObject( dimensionItem ) == null )
            {
                return ExpressionValidationOutcome.DIMENSIONAL_ITEM_OBJECT_DOES_NOT_EXIST;
            }

            matcher.appendReplacement( sb, "1.1" );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Constants
        // ---------------------------------------------------------------------

        matcher = CONSTANT_PATTERN.matcher( expression );
        sb = new StringBuffer();

        while ( matcher.find() )
        {
            String constant = matcher.group( 1 );

            if ( idObjectManager.getNoAcl( Constant.class, constant ) == null )
            {
                return ExpressionValidationOutcome.CONSTANT_DOES_NOT_EXIST;
            }

            matcher.appendReplacement( sb, "1.1" );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Org unit groups
        // ---------------------------------------------------------------------

        matcher = OU_GROUP_PATTERN.matcher( expression );
        sb = new StringBuffer();

        while ( matcher.find() )
        {
            String group = matcher.group( 1 );

            if ( idObjectManager.getNoAcl( OrganisationUnitGroup.class, group ) == null )
            {
                return ExpressionValidationOutcome.ORG_UNIT_GROUP_DOES_NOT_EXIST;
            }

            matcher.appendReplacement( sb, "1.1" );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Days
        // ---------------------------------------------------------------------

        expression = expression.replaceAll( DAYS_EXPRESSION, "1.1" );

        // ---------------------------------------------------------------------
        // Well-formed expression
        // ---------------------------------------------------------------------

        if ( MathUtils.expressionHasErrors( expression ) )
        {
            return ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED;
        }

        return ExpressionValidationOutcome.VALID;
    }

    @Override
    @Transactional
    public String getExpressionDescription( String expression )
    {
        if ( expression == null || expression.isEmpty() )
        {
            return null;
        }

        // ---------------------------------------------------------------------
        // Operands
        // ---------------------------------------------------------------------

        StringBuffer sb = new StringBuffer();
        Matcher matcher = VARIABLE_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String dimensionItem = matcher.group( 2 );

            DimensionalItemObject dimensionItemObject = dimensionService.getDataDimensionalItemObject( dimensionItem );

            if ( dimensionItemObject == null )
            {
                throw new InvalidIdentifierReferenceException( "Identifier does not reference a dimensional item object: " + dimensionItem );
            }

            matcher.appendReplacement( sb, Matcher.quoteReplacement( dimensionItemObject.getDisplayName() ) );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Constants
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = CONSTANT_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String co = matcher.group( 1 );

            Constant constant = constantService.getConstant( co );

            if ( constant == null )
            {
                throw new InvalidIdentifierReferenceException( "Identifier does not reference a constant: " + co );
            }

            matcher.appendReplacement( sb, Matcher.quoteReplacement( constant.getDisplayName() ) );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Org unit groups
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = OU_GROUP_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String oug = matcher.group( 1 );

            OrganisationUnitGroup group = organisationUnitGroupService.getOrganisationUnitGroup( oug );

            if ( group == null )
            {
                throw new InvalidIdentifierReferenceException( "Identifier does not reference an organisation unit group: " + oug );
            }

            matcher.appendReplacement( sb, Matcher.quoteReplacement( group.getDisplayName() ) );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Days
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = DAYS_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            matcher.appendReplacement( sb, DAYS_DESCRIPTION );
        }

        expression = TextUtils.appendTail( matcher, sb );

        return expression;
    }

    @Override
    @Transactional
    public void explodeValidationRuleExpressions( Collection<ValidationRule> validationRules )
    {
        if ( validationRules != null && !validationRules.isEmpty() )
        {
            Set<String> dataElementTotals = new HashSet<>();

            for ( ValidationRule rule : validationRules )
            {
                dataElementTotals.addAll(
                    RegexUtils.getMatches( DATA_ELEMENT_TOTAL_PATTERN, rule.getLeftSide().getExpression(), 1 ) );
                dataElementTotals.addAll(
                    RegexUtils.getMatches( DATA_ELEMENT_TOTAL_PATTERN, rule.getRightSide().getExpression(), 1 ) );
            }

            if ( !dataElementTotals.isEmpty() )
            {
                final ListMap<String, String> dataElementMap = dataElementService
                    .getDataElementCategoryOptionComboMap( dataElementTotals );

                if ( !dataElementMap.isEmpty() )
                {
                    for ( ValidationRule rule : validationRules )
                    {
                        rule.getLeftSide().setExplodedExpression(
                            explodeExpression( rule.getLeftSide().getExplodedExpressionFallback(), dataElementMap ) );
                        rule.getRightSide().setExplodedExpression(
                            explodeExpression( rule.getRightSide().getExplodedExpressionFallback(), dataElementMap ) );
                    }
                }
            }
        }
    }

    private String explodeExpression( String expression, ListMap<String, String> dataElementOptionComboMap )
    {
        if ( expression == null || expression.isEmpty() )
        {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        Matcher matcher = OPERAND_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            if ( operandIsTotal( matcher ) )
            {
                final StringBuilder replace = new StringBuilder( PAR_OPEN );

                String de = matcher.group( 1 );

                List<String> cocs = dataElementOptionComboMap.get( de );

                for ( String coc : cocs )
                {
                    replace.append( EXP_OPEN ).append( de ).append( SEPARATOR ).
                        append( coc ).append( EXP_CLOSE ).append( "+" );
                }

                replace.deleteCharAt( replace.length() - 1 ).append( PAR_CLOSE );
                matcher.appendReplacement( sb, Matcher.quoteReplacement( replace.toString() ) );
            }
        }

        return TextUtils.appendTail( matcher, sb );
    }

    @Override
    @Transactional
    public String explodeExpression( String expression )
    {
        if ( expression == null || expression.isEmpty() )
        {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        Matcher matcher = OPERAND_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            if ( operandIsTotal( matcher ) )
            {
                final StringBuilder replace = new StringBuilder( PAR_OPEN );

                final DataElement dataElement = idObjectManager.getNoAcl( DataElement.class, matcher.group( 1 ) );

                final DataElementCategoryCombo categoryCombo = dataElement.getCategoryCombo();

                for ( DataElementCategoryOptionCombo categoryOptionCombo : categoryCombo.getOptionCombos() )
                {
                    replace.append( EXP_OPEN ).append( dataElement.getUid() ).append( SEPARATOR ).
                        append( categoryOptionCombo.getUid() ).append( EXP_CLOSE ).append( "+" );
                }

                replace.deleteCharAt( replace.length() - 1 ).append( PAR_CLOSE );
                matcher.appendReplacement( sb, Matcher.quoteReplacement( replace.toString() ) );
            }
        }

        return TextUtils.appendTail( matcher, sb );
    }

    @Override
    @Transactional
    public void substituteExpressions( Collection<Indicator> indicators, Integer days )
    {
        if ( indicators != null && !indicators.isEmpty() )
        {
            Map<String, Constant> constants = new CachingMap<String, Constant>()
                .load( idObjectManager.getAllNoAcl( Constant.class ), c -> c.getUid() );

            Map<String, OrganisationUnitGroup> orgUnitGroups = new CachingMap<String, OrganisationUnitGroup>()
                .load( idObjectManager.getAllNoAcl( OrganisationUnitGroup.class ), g -> g.getUid() );

            for ( Indicator indicator : indicators )
            {
                indicator.setExplodedNumerator( substituteExpression(
                    indicator.getNumerator(), constants, orgUnitGroups, days ) );
                indicator.setExplodedDenominator( substituteExpression(
                    indicator.getDenominator(), constants, orgUnitGroups, days ) );
            }
        }
    }

    private String substituteExpression( String expression, Map<String, Constant> constants,
        Map<String, OrganisationUnitGroup> orgUnitGroups, Integer days )
    {
        if ( expression == null || expression.isEmpty() )
        {
            return null;
        }

        // ---------------------------------------------------------------------
        // Constants
        // ---------------------------------------------------------------------

        StringBuffer sb = new StringBuffer();
        Matcher matcher = CONSTANT_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String co = matcher.group( 1 );

            Constant constant = constants.get( co );

            String replacement = constant != null ? String.valueOf( constant.getValue() ) : NULL_REPLACEMENT;

            matcher.appendReplacement( sb, Matcher.quoteReplacement( replacement ) );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Org unit groups
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = OU_GROUP_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String oug = matcher.group( 1 );

            OrganisationUnitGroup group = orgUnitGroups.get( oug );

            String replacement = group != null ? String.valueOf( group.getMembers().size() ) : NULL_REPLACEMENT;

            matcher.appendReplacement( sb, replacement );

            // TODO sub tree
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Days
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = DAYS_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String replacement = days != null ? String.valueOf( days ) : NULL_REPLACEMENT;

            matcher.appendReplacement( sb, replacement );
        }

        return TextUtils.appendTail( matcher, sb );
    }

    @Override
    public String generateExpression( String expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy )
    {
        return generateExpression( expression, valueMap, constantMap, orgUnitCountMap, days, missingValueStrategy, null,
            null );
    }

    private String generateExpression( String expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy, Set<DataElementOperand> incompleteValues,
        Map<String, List<Double>> aggregateMap )
    {
        if ( expression == null || expression.isEmpty() )
        {
            return null;
        }

        Map<String, Double> dimensionItemValueMap = valueMap.entrySet().stream().
            filter( e -> e.getValue() != null ).
            collect( Collectors.toMap( e -> e.getKey().getDimensionItem(), e -> e.getValue() ) );

        Set<String> incompleteItems = incompleteValues != null ?
            incompleteValues.stream().map( i -> i.getDimensionItem() ).collect( Collectors.toSet() ) : Sets.newHashSet();

        missingValueStrategy = ObjectUtils.firstNonNull( missingValueStrategy, NEVER_SKIP );

        // ---------------------------------------------------------------------
        // Substitute aggregates
        // ---------------------------------------------------------------------

        StringBuffer sb = new StringBuffer();

        Pattern prefix = CustomFunctions.AGGREGATE_PATTERN_PREFIX;
        Matcher matcher = prefix.matcher( expression );

        int scan = 0, len = expression.length(), tail = 0;

        while ( scan < len && matcher.find( scan ) )
        {
            int start = matcher.end();
            int end = Expression.matchExpression( expression, start );

            if ( end < 0 )
            {
                sb.append( expression.substring( scan, start ) );
                scan = start + 1;
                tail = start;
            }
            else if ( ( aggregateMap == null ) || (expression.charAt( start ) == '<' ) )
            {
                sb.append( expression.substring( scan, end ) );
                scan = end + 1;
                tail = end;
            }
            else
            {
                String subExpression = expression.substring( start, end );
                List<Double> samples = aggregateMap.get( subExpression );

                if ( samples == null )
                {
                    if ( SKIP_IF_ANY_VALUE_MISSING.equals( missingValueStrategy ) )
                    {
                        return null;
                    }
                }
                else
                {
                    String literal = (samples == null) ? ("[]") : (samples.toString());
                    sb.append( expression.substring( scan, start ) );
                    sb.append( literal );
                }

                scan = end;
                tail = end;
            }
        }

        sb.append( expression.substring( tail ) );
        expression = sb.toString();

        // ---------------------------------------------------------------------
        // DimensionalItemObjects
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = VARIABLE_PATTERN.matcher( expression );

        int matchCount = 0;
        int valueCount = 0;

        while ( matcher.find() )
        {
            matchCount++;

            String dimItem = matcher.group( 2 );

            final Double value = dimensionItemValueMap.get( dimItem );

            boolean missingValue = value == null || incompleteItems.contains( dimItem );

            if ( missingValue && SKIP_IF_ANY_VALUE_MISSING.equals( missingValueStrategy ) )
            {
                return null;
            }

            if ( !missingValue )
            {
                valueCount++;
            }

            String replacement = value != null ? String.valueOf( value ) : NULL_REPLACEMENT;

            matcher.appendReplacement( sb, Matcher.quoteReplacement( replacement ) );
        }

        if ( SKIP_IF_ALL_VALUES_MISSING.equals( missingValueStrategy ) && matchCount > 0 && valueCount == 0 )
        {
            return null;
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Constants
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = CONSTANT_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            final Double constant = constantMap != null ? constantMap.get( matcher.group( 1 ) ) : null;

            String replacement = constant != null ? String.valueOf( constant ) : NULL_REPLACEMENT;

            matcher.appendReplacement( sb, replacement );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Org unit groups
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = OU_GROUP_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            final Integer count = orgUnitCountMap != null ? orgUnitCountMap.get( matcher.group( 1 ) ) : null;

            String replacement = count != null ? String.valueOf( count ) : NULL_REPLACEMENT;

            matcher.appendReplacement( sb, replacement );
        }

        expression = TextUtils.appendTail( matcher, sb );

        // ---------------------------------------------------------------------
        // Days
        // ---------------------------------------------------------------------

        sb = new StringBuffer();
        matcher = DAYS_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            String replacement = days != null ? String.valueOf( days ) : NULL_REPLACEMENT;

            matcher.appendReplacement( sb, replacement );
        }

        return TextUtils.appendTail( matcher, sb );
    }

    @Override
    @Transactional
    public List<DataElementOperand> getOperandsInIndicators( List<Indicator> indicators )
    {
        final List<DataElementOperand> operands = new ArrayList<>();

        for ( Indicator indicator : indicators )
        {
            Set<DataElementOperand> temp = getOperandsInExpression( indicator.getExplodedNumerator() );
            operands.addAll( temp != null ? temp : new HashSet<DataElementOperand>() );

            temp = getOperandsInExpression( indicator.getExplodedDenominator() );
            operands.addAll( temp != null ? temp : new HashSet<DataElementOperand>() );
        }

        return operands;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private boolean operandIsTotal( Matcher matcher )
    {
        return matcher != null && StringUtils.trimToEmpty( matcher.group( 2 ) ).isEmpty();
    }
}
