package org.hisp.dhis.parser.expression;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.api.util.DateUtils;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.parser.common.AbstractVisitor;
import org.hisp.dhis.parser.common.Parser;
import org.hisp.dhis.parser.common.ParserException;
import org.hisp.dhis.period.Period;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hisp.dhis.expression.ExpressionService.DAYS_DESCRIPTION;
import static org.hisp.dhis.expression.ExpressionService.SYMBOL_DAYS;
import static org.hisp.dhis.parser.common.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.common.ParserUtils.castDouble;

/**
 * Expression parser using the ANTLR parser.
 *
 * @author Jim Grace
 */
public class DefaultExpressionParserService
    implements ExpressionParserService
{
    private static final Log log = LogFactory.getLog( DefaultExpressionParserService.class );

    @Autowired
    private ConstantService constantService;

    @Autowired
    private OrganisationUnitGroupService organisationUnitGroupService;

    @Autowired
    private DimensionService dimensionService;

    // -------------------------------------------------------------------------
    // Expression methods
    // -------------------------------------------------------------------------

    @Override
    public Set<DimensionalItemObject> getExpressionDimensionalItemObjects( String expression )
    {
        Set<DimensionalItemId> itemIds = getExpressionDimensionalItemIds( expression );

        return dimensionService.getDataDimensionalItemObjects( itemIds );
    }

    @Override
    public String getExpressionDescription( String expression )
    {
        if ( expression == null )
        {
            return "";
        }

        ExpressionItemsVisitor expressionItemsVisitor = newExpressionItemsGetter();

        expressionItemsVisitor.setItemDescriptions( new HashMap<>() );

        visit ( expression, expressionItemsVisitor, false );

        Map<String, String> itemDescriptions = expressionItemsVisitor.getItemDescriptions();

        String description = expression.replace( SYMBOL_DAYS, DAYS_DESCRIPTION );

        for ( Map.Entry<String, String> entry : itemDescriptions.entrySet() )
        {
            description = description.replace( entry.getKey(), entry.getValue() );
        }

        return description;
    }

    @Override
    public Set<DimensionalItemId> getExpressionDimensionalItemIds( String expression )
    {
        if ( expression == null )
        {
            return new HashSet<>();
        }

        ExpressionItemsVisitor expressionItemsVisitor = newExpressionItemsGetter();

        expressionItemsVisitor.setItemIds( new HashSet<>() );

        visit ( expression, expressionItemsVisitor, true );

        return expressionItemsVisitor.getItemIds();
    }

    @Override
    public Set<OrganisationUnitGroup> getExpressionOrgUnitGroups( String expression )
    {
        if ( expression == null )
        {
            return new HashSet<>();
        }

        ExpressionItemsVisitor expressionItemsVisitor = newExpressionItemsGetter();

        expressionItemsVisitor.setOrgUnitGroupIds( new HashSet<>() );

        visit ( expression, expressionItemsVisitor, true );

        Set<String> orgUnitGroupIds = expressionItemsVisitor.getOrgUnitGroupsIds();

        Set<OrganisationUnitGroup> orgUnitGroups = orgUnitGroupIds.stream()
            .map( id -> organisationUnitGroupService.getOrganisationUnitGroup( id ) )
            .collect( Collectors.toSet() );

        return orgUnitGroups;
    }

    @Override
    public Double getExpressionValue( String expression,
        Map<DimensionalItemObject, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy )
    {
        if ( expression == null )
        {
            return null;
        }

        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(
            valueMap, constantMap, orgUnitCountMap, days );

        Double value = visit ( expression, expressionEvaluator, true );

        int itemsFound = expressionEvaluator.getItemsFound();
        int itemValuesFound = expressionEvaluator.getItemValuesFound();

        switch ( missingValueStrategy )
        {
            case SKIP_IF_ANY_VALUE_MISSING:
                if ( itemValuesFound < itemsFound )
                {
                    return null;
                }

            case SKIP_IF_ALL_VALUES_MISSING:
                if ( itemsFound != 0 && itemValuesFound == 0 )
                {
                    return null;
                }

            case NEVER_SKIP:
                if ( value == null )
                {
                    return 0d;
                }
        }

        return value;
    }

    // -------------------------------------------------------------------------
    // Indicator expression methods
    // -------------------------------------------------------------------------

    @Override
    public Set<DimensionalItemObject> getIndicatorDimensionalItemObjects( Collection<Indicator> indicators )
    {
        Set<DimensionalItemId> itemIds = indicators.stream()
            .flatMap( i -> Stream.of( i.getNumerator(), i.getDenominator() ) )
            .map( this::getExpressionDimensionalItemIds )
            .flatMap( Set::stream )
            .collect( Collectors.toSet() );

        return dimensionService.getDataDimensionalItemObjects( itemIds );
    }

    @Override
    public Set<OrganisationUnitGroup> getIndicatorOrgUnitGroups( Collection<Indicator> indicators )
    {
        Set<OrganisationUnitGroup> groups = new HashSet<>();

        if ( indicators != null )
        {
            for ( Indicator indicator : indicators )
            {
                groups.addAll( getExpressionOrgUnitGroups( indicator.getNumerator() ) );
                groups.addAll( getExpressionOrgUnitGroups( indicator.getDenominator() ) );
            }
        }

        return groups;
    }

    public IndicatorValue getIndicatorValueObject( Indicator indicator, Period period,
        Map<DimensionalItemObject, Double> valueMap, Map<String, Double> constantMap,
        Map<String, Integer> orgUnitCountMap )
    {
        if ( indicator == null || indicator.getNumerator() == null || indicator.getDenominator() == null )
        {
            return null;
        }

        Integer days = period != null ? period.getDaysInPeriod() : null;

        Double denominatorValue = getExpressionValue( indicator.getDenominator(),
            valueMap, constantMap, orgUnitCountMap, days, MissingValueStrategy.NEVER_SKIP );

        Double numeratorValue = getExpressionValue( indicator.getNumerator(),
            valueMap, constantMap, orgUnitCountMap, days, MissingValueStrategy.NEVER_SKIP );

        if ( denominatorValue != null && denominatorValue != 0d && numeratorValue != null )
        {
            int multiplier = indicator.getIndicatorType().getFactor();

            int divisor = 1;

            if ( indicator.isAnnualized() && period != null )
            {
                final int daysInPeriod = DateUtils.daysBetween( period.getStartDate(), period.getEndDate() ) + 1;

                multiplier *= DateUtils.DAYS_IN_YEAR;

                divisor = daysInPeriod;
            }

            return new IndicatorValue()
                .setNumeratorValue( numeratorValue )
                .setDenominatorValue( denominatorValue )
                .setMultiplier( multiplier )
                .setDivisor( divisor );
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Creates a new ExpressionItemsVisitor object.
     */
    private ExpressionItemsVisitor newExpressionItemsGetter()
    {
        return new ExpressionItemsVisitor( dimensionService,
            organisationUnitGroupService, constantService );
    }

    private Double visit( String expression, AbstractVisitor visitor, boolean logWarnings )
    {
        try
        {
            return castDouble( Parser.visit( expression, visitor ) );
        }
        catch ( ParserException ex )
        {
            String message = ex.getMessage() + " parsing expression '" + expression + "'";

            if ( logWarnings )
            {
                log.warn( message );
            }
            else
            {
                throw new ParserException( message );
            }
        }

        return DOUBLE_VALUE_IF_NULL;
    }
}
