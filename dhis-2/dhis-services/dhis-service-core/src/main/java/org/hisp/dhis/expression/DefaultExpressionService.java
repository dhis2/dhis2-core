package org.hisp.dhis.expression;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.GenericStore;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.common.exception.InvalidIdentifierReferenceException;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
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
import org.hisp.dhis.system.util.ExpressionUtils;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        expressionStore.save( expression );

        return expression.getId();
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
        if ( indicator == null || indicator.getNumerator() == null || indicator.getDenominator() == null )
        {
            return null;
        }

        Integer days = period != null ? period.getDaysInPeriod() : null;

        final String denominatorExpression = generateExpression( indicator.getDenominator(), valueMap,
            constantMap, orgUnitCountMap, days, NEVER_SKIP );

        if ( denominatorExpression == null )
        {
            return null;
        }

        final double denominatorValue = calculateExpression( denominatorExpression );

        if ( !isEqual( denominatorValue, 0d ) )
        {
            final String numeratorExpression = generateExpression( indicator.getNumerator(), valueMap,
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
        return getExpressionValue( expression, valueMap, constantMap, orgUnitCountMap, days, null );
    }

    @Override
    public Double getExpressionValue( Expression expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        ListMap<String, Double> aggregateMap )
    {
        String expressionString = generateExpression( expression.getExpression(),
            valueMap, constantMap, orgUnitCountMap, days, expression.getMissingValueStrategy(),
            aggregateMap );

        return expressionString != null ? calculateExpression( expressionString ) : null;
    }

    @Override
    public Set<DataElement> getDataElementsInExpression( String expression )
    {
        return getIdObjectsInExpression( OPERAND_PATTERN, expression,
            ( m ) -> dataElementService.getDataElement( m.group( GROUP_DATA_ELEMENT ) ) );
    }

    @Override
    public Set<DataElementCategoryOptionCombo> getOptionCombosInExpression( String expression )
    {
        return getIdObjectsInExpression( CATEGORY_OPTION_COMBO_OPERAND_PATTERN, expression, 
            ( m ) -> categoryService.getDataElementCategoryOptionCombo( m.group( GROUP_CATEGORORY_OPTION_COMBO ) ) );
    }

    @Override
    public Set<OrganisationUnitGroup> getOrganisationUnitGroupsInExpression( String expression )
    {
        return getIdObjectsInExpression( OU_GROUP_PATTERN, expression, 
            ( m ) -> organisationUnitGroupService.getOrganisationUnitGroup( m.group( GROUP_ID ) ) );
    }

    /**
     * Returns a set of identifiable objects which are referenced in
     * the given expression based on the given regular expression pattern.
     * 
     * @param pattern the regular expression pattern to match identifiable objects on.
     * @param expression the expression where identifiable objects are referenced.
     * @param provider the provider of identifiable objects, accepts a matcher and 
     *        provides the object.
     * @return a set of identifiable objects.
     */
    private <T extends IdentifiableObject> Set<T> getIdObjectsInExpression( Pattern pattern, String expression, Function<Matcher, T> provider )
    {
        Set<T> objects = new HashSet<>();
        
        if ( expression == null )
        {
            return  objects;
        }
        
        final Matcher matcher = pattern.matcher( expression );

        while ( matcher.find() )
        {
            final T object = provider.apply( matcher );
            
            if ( object != null )
            {
                objects.add( object );
            }
        }
        
        return objects;
    }

    @Override
    @Transactional
    public Set<DataElementOperand> getOperandsInExpression( String expression )
    {
        Set<DataElementOperand> operandsInExpression = new HashSet<>();

        if ( expression != null )
        {
            final Matcher matcher = OPERAND_PATTERN.matcher( expression );

            while ( matcher.find() )
            {
                String dataElementUid = StringUtils.trimToNull( matcher.group( GROUP_DATA_ELEMENT ) );
                String optionComboUid = StringUtils.trimToNull( matcher.group( GROUP_CATEGORORY_OPTION_COMBO ) );

                DataElement dataElement = dataElementService.getDataElement( dataElementUid );

                DataElementCategoryOptionCombo optionCombo = optionComboUid == null ? null :
                    categoryService.getDataElementCategoryOptionCombo( optionComboUid );

                operandsInExpression.add ( new DataElementOperand( dataElement, optionCombo ) );
            }
        }

        return operandsInExpression;
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
    public Set<String> getDataElementIdsInExpression( String expression )
    {
        Set<String> dataElementIds = new HashSet<>();

        if ( expression == null || expression.isEmpty() )
        {
            return dataElementIds;
        }

        Matcher matcher = OPERAND_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            dataElementIds.add( matcher.group( 1 ) );
        }

        return dataElementIds;
    }

    @Override
    public SetMap<Class<? extends DimensionalItemObject>, String> getDimensionalItemIdsInExpression( String expression )
    {
        SetMap<Class<? extends DimensionalItemObject>, String> dimensionItemIdentifiers = new SetMap<>();

        if ( expression == null || expression.isEmpty() )
        {
            return dimensionItemIdentifiers;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher( expression );

        while ( matcher.find() )
        {
            dimensionItemIdentifiers.putValue( VARIABLE_TYPES.get( matcher.group( 1 ) ), matcher.group( 2 ) );
        }

        return dimensionItemIdentifiers;
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
            String dimensionItem = matcher.group( GROUP_ID );
            
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
            String dimensionItem = matcher.group( GROUP_ID );

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
            String constant = matcher.group( GROUP_ID );

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
            String group = matcher.group( GROUP_ID );

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
            String dimensionItem = matcher.group( GROUP_ID );

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
            String co = matcher.group( GROUP_ID );

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
            String oug = matcher.group( GROUP_ID );

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
            String co = matcher.group( GROUP_ID );

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
            String oug = matcher.group( GROUP_ID );

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
        return generateExpression( expression, valueMap, constantMap, orgUnitCountMap, days, missingValueStrategy, null );
    }

    /**
     * Generates an expression based on the given data maps.
     * 
     * @param expression the expression.
     * @param valueMap the value map.
     * @param constantMap the constant map.
     * @param orgUnitCountMap the organisation unit count map.
     * @param days the number of days.
     * @param missingValueStrategy the missing value strategy.
     * @param aggregateMap the aggregate map.
     * @return an expression.
     */
    private String generateExpression( String expression, Map<? extends DimensionalItemObject, Double> valueMap,
        Map<String, Double> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy, 
        Map<String, List<Double>> aggregateMap )
    {
        if ( expression == null || expression.isEmpty() )
        {
            return null;
        }
        
        expression = ExpressionUtils.normalizeExpression( expression );

        Map<String, Double> dimensionItemValueMap = valueMap.entrySet().stream().
            filter( e -> e.getValue() != null ).
            collect( Collectors.toMap( e -> e.getKey().getDimensionItem(), e -> e.getValue() ) );

        missingValueStrategy = ObjectUtils.firstNonNull( missingValueStrategy, NEVER_SKIP );

        // ---------------------------------------------------------------------
        // Aggregates
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
            else if ( aggregateMap == null || expression.charAt( start ) == '<' )
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

            String dimItem = matcher.group( GROUP_ID );

            final Double value = dimensionItemValueMap.get( dimItem );

            boolean missingValue = value == null;

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
            final Double constant = constantMap != null ? constantMap.get( matcher.group( GROUP_ID ) ) : null;

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
            final Integer count = orgUnitCountMap != null ? orgUnitCountMap.get( matcher.group( GROUP_ID ) ) : null;

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
}
