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
package org.hisp.dhis.expression;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Boolean.FALSE;
import static org.hisp.dhis.antlr.AntlrParserUtils.castBoolean;
import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.MissingValueStrategy.SKIP_IF_ALL_VALUES_MISSING;
import static org.hisp.dhis.expression.ParseType.INDICATOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_EXPRESSION;
import static org.hisp.dhis.expression.ParseType.PREDICTOR_SKIP_TEST;
import static org.hisp.dhis.expression.ParseType.SIMPLE_TEST;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.hisp.dhis.parser.expression.ParserUtils.COMMON_EXPRESSION_ITEMS;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_SAMPLE_PERIODS;
import static org.hisp.dhis.parser.expression.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_EVALUATE;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_DESCRIPTIONS;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_IDS;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_ORG_UNIT_GROUPS;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AVG;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.A_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.DAYS;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.HASH_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.I_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MAX;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MEDIAN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MIN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.N_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.OUG_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PERCENTILE_CONT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PERIOD_OFFSET;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.R_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV_POP;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV_SAMP;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.SUM;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.dataitem.DimItemDataElementAndOperand;
import org.hisp.dhis.expression.dataitem.DimItemIndicator;
import org.hisp.dhis.expression.dataitem.DimItemProgramAttribute;
import org.hisp.dhis.expression.dataitem.DimItemProgramDataElement;
import org.hisp.dhis.expression.dataitem.DimItemProgramIndicator;
import org.hisp.dhis.expression.dataitem.DimItemReportingRate;
import org.hisp.dhis.expression.dataitem.ItemDays;
import org.hisp.dhis.expression.dataitem.ItemOrgUnitGroup;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitGroupService;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.ExpressionItemMethod;
import org.hisp.dhis.parser.expression.function.PeriodOffset;
import org.hisp.dhis.parser.expression.function.VectorAvg;
import org.hisp.dhis.parser.expression.function.VectorCount;
import org.hisp.dhis.parser.expression.function.VectorMax;
import org.hisp.dhis.parser.expression.function.VectorMedian;
import org.hisp.dhis.parser.expression.function.VectorMin;
import org.hisp.dhis.parser.expression.function.VectorPercentileCont;
import org.hisp.dhis.parser.expression.function.VectorStddevPop;
import org.hisp.dhis.parser.expression.function.VectorStddevSamp;
import org.hisp.dhis.parser.expression.function.VectorSum;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;

/**
 * The expression is a string describing a formula containing data element ids
 * and category option combo ids. The formula can potentially contain references
 * to data element totals.
 *
 * @author Margrethe Store
 * @author Lars Helge Overland
 * @author Jim Grace
 */
@Slf4j
@Service( "org.hisp.dhis.expression.ExpressionService" )
public class DefaultExpressionService
    implements ExpressionService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final HibernateGenericStore<Expression> expressionStore;

    private final DataElementService dataElementService;

    private final ConstantService constantService;

    private final OrganisationUnitGroupService organisationUnitGroupService;

    private final DimensionService dimensionService;

    private final IdentifiableObjectManager idObjectManager;

    // -------------------------------------------------------------------------
    // Static data
    // -------------------------------------------------------------------------

    private static final ImmutableMap<Integer, ExpressionItem> VALIDATION_RULE_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()
        .putAll( COMMON_EXPRESSION_ITEMS )
        .put( HASH_BRACE, new DimItemDataElementAndOperand() )
        .put( A_BRACE, new DimItemProgramAttribute() )
        .put( D_BRACE, new DimItemProgramDataElement() )
        .put( I_BRACE, new DimItemProgramIndicator() )
        .put( OUG_BRACE, new ItemOrgUnitGroup() )
        .put( R_BRACE, new DimItemReportingRate() )
        .put( DAYS, new ItemDays() )
        .build();

    private static final ImmutableMap<Integer, ExpressionItem> PREDICTOR_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()
        .putAll( VALIDATION_RULE_EXPRESSION_ITEMS )
        .put( AVG, new VectorAvg() )
        .put( COUNT, new VectorCount() )
        .put( MAX, new VectorMax() )
        .put( MEDIAN, new VectorMedian() )
        .put( MIN, new VectorMin() )
        .put( PERCENTILE_CONT, new VectorPercentileCont() )
        .put( STDDEV, new VectorStddevSamp() )
        .put( STDDEV_POP, new VectorStddevPop() )
        .put( STDDEV_SAMP, new VectorStddevSamp() )
        .put( SUM, new VectorSum() )
        .build();

    private static final ImmutableMap<Integer, ExpressionItem> INDICATOR_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()
        .putAll( VALIDATION_RULE_EXPRESSION_ITEMS )
        .put( N_BRACE, new DimItemIndicator() )
        .put( PERIOD_OFFSET, new PeriodOffset() )
        .build();

    private static final ImmutableMap<ParseType, ImmutableMap<Integer, ExpressionItem>> PARSE_TYPE_EXPRESSION_ITEMS = ImmutableMap
        .<ParseType, ImmutableMap<Integer, ExpressionItem>> builder()
        .put( INDICATOR_EXPRESSION, INDICATOR_EXPRESSION_ITEMS )
        .put( VALIDATION_RULE_EXPRESSION, VALIDATION_RULE_EXPRESSION_ITEMS )
        .put( PREDICTOR_EXPRESSION, PREDICTOR_EXPRESSION_ITEMS )
        .put( PREDICTOR_SKIP_TEST, PREDICTOR_EXPRESSION_ITEMS )
        .put( SIMPLE_TEST, COMMON_EXPRESSION_ITEMS )
        .build();

    private static final String CONSTANT_EXPRESSION = "C\\{(?<id>[a-zA-Z]\\w{10})\\}";

    private static final String OU_GROUP_EXPRESSION = "OUG\\{(?<id>[a-zA-Z]\\w{10})\\}";

    private static final String GROUP_ID = "id";

    private static final String NULL_REPLACEMENT = "0";

    /**
     * Constant pattern. Contains the named group {@code id}.
     */
    private static final Pattern CONSTANT_PATTERN = Pattern.compile( CONSTANT_EXPRESSION );

    /**
     * Organisation unit groups pattern. Contains the named group {@code id}.
     */
    private static final Pattern OU_GROUP_PATTERN = Pattern.compile( OU_GROUP_EXPRESSION );

    // -------------------------------------------------------------------------
    // Cache
    // -------------------------------------------------------------------------

    /**
     * Cache for the constant map.
     */
    private final Cache<Map<String, Constant>> constantMapCache;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public DefaultExpressionService(
        @Qualifier( "org.hisp.dhis.expression.ExpressionStore" ) HibernateGenericStore<Expression> expressionStore,
        DataElementService dataElementService, ConstantService constantService,
        OrganisationUnitGroupService organisationUnitGroupService, DimensionService dimensionService,
        IdentifiableObjectManager idObjectManager, CacheProvider cacheProvider )
    {
        checkNotNull( expressionStore );
        checkNotNull( dataElementService );
        checkNotNull( constantService );
        checkNotNull( organisationUnitGroupService );
        checkNotNull( dimensionService );
        checkNotNull( cacheProvider );

        this.expressionStore = expressionStore;
        this.dataElementService = dataElementService;
        this.constantService = constantService;
        this.organisationUnitGroupService = organisationUnitGroupService;
        this.dimensionService = dimensionService;
        this.idObjectManager = idObjectManager;
        this.constantMapCache = cacheProvider.createAllConstantsCache();
    }

    // -------------------------------------------------------------------------
    // Expression CRUD operations
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addExpression( Expression expression )
    {
        expressionStore.save( expression );

        return expression.getId();
    }

    @Override
    @Transactional
    public void updateExpression( Expression expression )
    {
        expressionStore.update( expression );
    }

    @Override
    @Transactional
    public void deleteExpression( Expression expression )
    {
        expressionStore.delete( expression );
    }

    @Override
    @Transactional( readOnly = true )
    public Expression getExpression( long id )
    {
        return expressionStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public List<Expression> getAllExpressions()
    {
        return expressionStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Indicator expression logic
    // -------------------------------------------------------------------------

    @Override
    public Map<DimensionalItemId, DimensionalItemObject> getIndicatorDimensionalItemMap(
        Collection<Indicator> indicators )
    {
        Set<DimensionalItemId> itemIds = indicators.stream()
            .flatMap( i -> Stream.of( i.getNumerator(), i.getDenominator() ) )
            .map( e -> getExpressionDimensionalItemIds( e, INDICATOR_EXPRESSION ) )
            .flatMap( Set::stream )
            .collect( Collectors.toSet() );

        return dimensionService.getDataDimensionalItemObjectMap( itemIds );
    }

    @Override
    public Set<OrganisationUnitGroup> getIndicatorOrgUnitGroups( Collection<Indicator> indicators )
    {
        Set<OrganisationUnitGroup> groups = new HashSet<>();

        if ( indicators != null )
        {
            for ( Indicator indicator : indicators )
            {
                try
                {
                    groups.addAll( getExpressionOrgUnitGroups( indicator.getNumerator(), INDICATOR_EXPRESSION ) );
                }
                catch ( Exception e )
                {
                    log.warn( "Parsing error in indicator " + indicator.getUid() + " numerator '"
                        + indicator.getNumerator() + "': " + e.toString() );
                }
                try
                {
                    groups.addAll( getExpressionOrgUnitGroups( indicator.getDenominator(), INDICATOR_EXPRESSION ) );
                }
                catch ( Exception e )
                {
                    log.warn( "Parsing error in indicator " + indicator.getUid() + " denominator '"
                        + indicator.getDenominator() + "': " + e.toString() );
                }
            }
        }

        return groups;
    }

    @Override
    public IndicatorValue getIndicatorValueObject( Indicator indicator, List<Period> periods,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, Map<DimensionalItemObject, Object> valueMap,
        Map<String, Constant> constantMap, Map<String, Integer> orgUnitCountMap )
    {
        if ( indicator == null || indicator.getNumerator() == null || indicator.getDenominator() == null )
        {
            return null;
        }

        Integer days = periods != null ? getDaysFromPeriods( periods ) : null;

        Double denominatorValue = getExpressionValue( indicator.getDenominator(), INDICATOR_EXPRESSION,
            itemMap, valueMap, constantMap, orgUnitCountMap, days, SKIP_IF_ALL_VALUES_MISSING );

        Double numeratorValue = getExpressionValue( indicator.getNumerator(), INDICATOR_EXPRESSION,
            itemMap, valueMap, constantMap, orgUnitCountMap, days, SKIP_IF_ALL_VALUES_MISSING );

        if ( denominatorValue != null && denominatorValue != 0d && numeratorValue != null )
        {
            int multiplier = indicator.getIndicatorType().getFactor();

            int divisor = 1;

            if ( indicator.isAnnualized() && periods != null )
            {
                final int daysInPeriod = getDaysFromPeriods( periods );

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

    @Override
    @Transactional
    public void substituteIndicatorExpressions( Collection<Indicator> indicators )
    {
        if ( indicators == null || indicators.isEmpty() )
        {
            return;
        }

        Map<String, Constant> constants = new CachingMap<String, Constant>()
            .load( idObjectManager.getAllNoAcl( Constant.class ), c -> c.getUid() );

        Map<String, OrganisationUnitGroup> orgUnitGroups = new CachingMap<String, OrganisationUnitGroup>()
            .load( idObjectManager.getAllNoAcl( OrganisationUnitGroup.class ), g -> g.getUid() );

        for ( Indicator indicator : indicators )
        {
            indicator.setExplodedNumerator(
                regenerateIndicatorExpression( indicator.getNumerator(), constants, orgUnitGroups ) );
            indicator.setExplodedDenominator(
                regenerateIndicatorExpression( indicator.getDenominator(), constants, orgUnitGroups ) );
        }
    }

    // -------------------------------------------------------------------------
    // Get information about the expression
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ExpressionValidationOutcome expressionIsValid( String expression, ParseType parseType )
    {
        try
        {
            getExpressionDescription( expression, parseType );

            return ExpressionValidationOutcome.VALID;
        }
        catch ( IllegalStateException e )
        {
            return ExpressionValidationOutcome.EXPRESSION_IS_NOT_WELL_FORMED;
        }
    }

    @Override
    public String getExpressionDescription( String expression, ParseType parseType )
    {
        return getExpressionDescription( expression, parseType, parseType.getDataType() );
    }

    @Override
    public String getExpressionDescription( String expression, ParseType parseType, DataType dataType )
    {
        if ( isEmpty( expression ) )
        {
            return "";
        }

        CommonExpressionVisitor visitor = newVisitor( parseType, ITEM_GET_DESCRIPTIONS,
            DEFAULT_SAMPLE_PERIODS, getConstantMap(), NEVER_SKIP );

        visit( expression, dataType, visitor, false );

        Map<String, String> itemDescriptions = visitor.getItemDescriptions();

        String description = expression;

        for ( Map.Entry<String, String> entry : itemDescriptions.entrySet() )
        {
            description = description.replace( entry.getKey(), entry.getValue() );
        }

        return description;
    }

    @Override
    public Set<String> getExpressionElementAndOptionComboIds( String expression, ParseType parseType )
    {
        return getExpressionDimensionalItemIds( expression, parseType ).stream()
            .filter( DimensionalItemId::isDataElementOrOperand )
            .map( i -> i.getId0() + (i.getId1() == null ? "" : Expression.SEPARATOR + i.getId1()) )
            .collect( Collectors.toSet() );
    }

    @Override
    public Set<String> getExpressionDataElementIds( String expression, ParseType parseType )
    {
        return getExpressionDimensionalItemIds( expression, parseType ).stream()
            .filter( DimensionalItemId::isDataElementOrOperand )
            .map( DimensionalItemId::getId0 )
            .collect( Collectors.toSet() );
    }

    @Override
    public Set<String> getExpressionOptionComboIds( String expression, ParseType parseType )
    {
        Set<String> categoryOptionComboIds = new HashSet<>();

        for ( DimensionalItemId itemId : getExpressionDimensionalItemIds( expression, parseType ) )
        {
            if ( itemId.getDimensionItemType() == DATA_ELEMENT_OPERAND )
            {
                if ( itemId.getId1() != null )
                {
                    categoryOptionComboIds.add( itemId.getId1() );
                }
                if ( itemId.getId2() != null )
                {
                    categoryOptionComboIds.add( itemId.getId2() );
                }
            }
        }

        return categoryOptionComboIds;
    }

    @Override
    public void getExpressionDimensionalItemMaps( String expression, ParseType parseType, DataType dataType,
        Map<DimensionalItemId, DimensionalItemObject> itemMap,
        Map<DimensionalItemId, DimensionalItemObject> sampleItemMap )
    {
        Set<DimensionalItemId> itemIds = new HashSet<>();
        Set<DimensionalItemId> sampleItemIds = new HashSet<>();

        getExpressionDimensionalItemIds( expression, parseType, itemIds, sampleItemIds );

        itemMap.putAll( dimensionService.getDataDimensionalItemObjectMap( itemIds ) );
        sampleItemMap.putAll( dimensionService.getDataDimensionalItemObjectMap( sampleItemIds ) );
    }

    @Override
    public Set<DimensionalItemId> getExpressionDimensionalItemIds( String expression, ParseType parseType )
    {
        Set<DimensionalItemId> itemIds = new HashSet<>();

        getExpressionDimensionalItemIds( expression, parseType, itemIds, itemIds );

        return itemIds;
    }

    @Override
    public Set<OrganisationUnitGroup> getExpressionOrgUnitGroups( String expression, ParseType parseType )
    {
        if ( isEmpty( expression ) )
        {
            return new HashSet<>();
        }

        CommonExpressionVisitor visitor = newVisitor( INDICATOR_EXPRESSION, ITEM_GET_ORG_UNIT_GROUPS,
            DEFAULT_SAMPLE_PERIODS, getConstantMap(), NEVER_SKIP );

        visit( expression, parseType.getDataType(), visitor, true );

        Set<String> orgUnitGroupIds = visitor.getOrgUnitGroupIds();

        return orgUnitGroupIds.stream()
            .map( organisationUnitGroupService::getOrganisationUnitGroup )
            .filter( Objects::nonNull )
            .collect( Collectors.toSet() );
    }

    // -------------------------------------------------------------------------
    // Compute the value of the expression
    // -------------------------------------------------------------------------

    @Override
    public Object getExpressionValue( String expression, ParseType parseType )
    {
        return getExpressionValue( expression, parseType,
            new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(),
            null, NEVER_SKIP, DEFAULT_SAMPLE_PERIODS, new MapMap<>(), parseType.getDataType() );
    }

    @Override
    public Double getExpressionValue( String expression, ParseType parseType,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, Map<DimensionalItemObject, Object> valueMap,
        Map<String, Constant> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy )
    {
        return castDouble( getExpressionValue( expression, parseType, itemMap, valueMap,
            constantMap, orgUnitCountMap, days, missingValueStrategy,
            DEFAULT_SAMPLE_PERIODS, new MapMap<>(), parseType.getDataType() ) );
    }

    @Override
    public Object getExpressionValue( String expression, ParseType parseType,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, Map<DimensionalItemObject, Object> valueMap,
        Map<String, Constant> constantMap, Map<String, Integer> orgUnitCountMap, Integer days,
        MissingValueStrategy missingValueStrategy, List<Period> samplePeriods,
        MapMap<Period, DimensionalItemObject, Object> periodValueMap, DataType dataType )
    {
        if ( isEmpty( expression ) )
        {
            return null;
        }

        CommonExpressionVisitor visitor = newVisitor( parseType, ITEM_EVALUATE,
            samplePeriods, constantMap, missingValueStrategy );

        visitor.setDimItemMap( itemMap );
        visitor.setItemValueMap( valueMap );
        visitor.setPeriodItemValueMap( periodValueMap );
        visitor.setOrgUnitCountMap( orgUnitCountMap );

        if ( days != null )
        {
            visitor.setDays( Double.valueOf( days ) );
        }

        Object value = visit( expression, dataType, visitor, true );

        int itemsFound = visitor.getItemsFound();
        int itemValuesFound = visitor.getItemValuesFound();

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
                switch ( dataType )
                {
                case NUMERIC:
                    return 0d;

                case BOOLEAN:
                    return FALSE;

                case TEXT:
                    return "";
                }
            }
        }

        return value;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Gets the (possibly cached) constant map.
     *
     * @return the constant map.
     */
    private Map<String, Constant> getConstantMap()
    {
        return constantMapCache.get( "x", key -> constantService.getConstantMap() )
            .orElse( Collections.emptyMap() );
    }

    /**
     * Creates a new ExpressionItemsVisitor object.
     */
    private CommonExpressionVisitor newVisitor( ParseType parseType,
        ExpressionItemMethod itemMethod, List<Period> samplePeriods,
        Map<String, Constant> constantMap, MissingValueStrategy missingValueStrategy )
    {
        return CommonExpressionVisitor.newBuilder()
            .withItemMap( PARSE_TYPE_EXPRESSION_ITEMS.get( parseType ) )
            .withItemMethod( itemMethod )
            .withConstantMap( constantMap )
            .withDimensionService( dimensionService )
            .withOrganisationUnitGroupService( organisationUnitGroupService )
            .withSamplePeriods( samplePeriods )
            .withMissingValueStrategy( missingValueStrategy )
            .withParseType( parseType )
            .buildForExpressions();
    }

    /**
     * Returns all non-aggregated and all aggregated dimensional item object ids
     * in the given expression.
     *
     * @param expression the expression to parse.
     * @param parseType the type of expression to parse.
     * @param itemIds Set to insert the itemIds into.
     * @param sampleItemIds Set to insert the aggregatedItemIds into.
     */
    private void getExpressionDimensionalItemIds( String expression, ParseType parseType,
        Set<DimensionalItemId> itemIds,
        Set<DimensionalItemId> sampleItemIds )
    {
        if ( isEmpty( expression ) )
        {
            return;
        }

        CommonExpressionVisitor visitor = newVisitor( parseType, ITEM_GET_IDS,
            DEFAULT_SAMPLE_PERIODS, getConstantMap(), NEVER_SKIP );

        visitor.setItemIds( itemIds );
        visitor.setSampleItemIds( sampleItemIds );

        visit( expression, parseType.getDataType(), visitor, true );
    }

    /**
     * Visits an expression and returns the expected expression type.
     *
     * @param expression the expresion to visit.
     * @param dataType the expected data type of the expression value.
     * @param visitor the visitor to use.
     * @param logWarnings whether to log warnings or not.
     * @return the expression value.
     */
    private Object visit( String expression, DataType dataType, CommonExpressionVisitor visitor, boolean logWarnings )
    {
        try
        {
            Object result = Parser.visit( expression, visitor );

            switch ( dataType )
            {
            case NUMERIC:
                return castDouble( result );

            case BOOLEAN:
                return castBoolean( result );

            case TEXT:
                return castString( result );
            }
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

    /**
     * Regenerates an expression from the parse tree, with values substituted
     * for constants and orgUnitCounts.
     *
     * @param expression the expresion to regenerate.
     * @param constants map of constants to use for calculation.
     * @param orgUnitGroups map of organisation unit groups.
     * @return the regenerated expression string.
     */
    private String regenerateIndicatorExpression( String expression,
        Map<String, Constant> constants, Map<String, OrganisationUnitGroup> orgUnitGroups )
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

        return TextUtils.appendTail( matcher, sb );
    }

    /**
     * Finds the total number of days in a list of periods.
     *
     * @param periods the periods.
     * @return the total number of days.
     */
    private int getDaysFromPeriods( List<Period> periods )
    {
        return periods.stream()
            .filter( Objects::nonNull )
            .mapToInt( Period::getDaysInPeriod ).sum();
    }
}
