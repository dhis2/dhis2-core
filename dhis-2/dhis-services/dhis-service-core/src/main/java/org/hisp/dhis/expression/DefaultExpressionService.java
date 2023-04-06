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
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_EVALUATE;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_DESCRIPTIONS;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_EXPRESSION_INFO;
import static org.hisp.dhis.parser.expression.ParserUtils.COMMON_EXPRESSION_ITEMS;
import static org.hisp.dhis.parser.expression.ParserUtils.DOUBLE_VALUE_IF_NULL;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AGGREGATION_TYPE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AVG;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.A_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.DAYS;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.HASH_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.I_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MAX;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MAX_DATE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MEDIAN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MIN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MIN_DATE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.NORM_DIST_CUM;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.NORM_DIST_DEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.N_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ORGUNIT_ANCESTOR;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ORGUNIT_DATASET;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ORGUNIT_GROUP;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ORGUNIT_PROGRAM;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.OUG_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PERCENTILE_CONT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PERIOD_IN_YEAR;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PERIOD_OFFSET;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.R_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV_POP;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV_SAMP;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.SUB_EXPRESSION;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.SUM;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.YEARLY_PERIOD_COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.YEAR_TO_DATE;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Collection;
import java.util.Collections;
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

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.commons.collection.CachingMap;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.Constant;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.expression.dataitem.DimItemDataElementAndOperand;
import org.hisp.dhis.expression.dataitem.DimItemIndicator;
import org.hisp.dhis.expression.dataitem.DimItemProgramAttribute;
import org.hisp.dhis.expression.dataitem.DimItemProgramDataElement;
import org.hisp.dhis.expression.dataitem.DimItemProgramIndicator;
import org.hisp.dhis.expression.dataitem.DimItemReportingRate;
import org.hisp.dhis.expression.dataitem.ItemDays;
import org.hisp.dhis.expression.dataitem.ItemOrgUnitGroupCount;
import org.hisp.dhis.expression.dataitem.ItemPeriodInYear;
import org.hisp.dhis.expression.dataitem.ItemYearlyPeriodCount;
import org.hisp.dhis.expression.function.FunctionOrgUnitAncestor;
import org.hisp.dhis.expression.function.FunctionOrgUnitDataSet;
import org.hisp.dhis.expression.function.FunctionOrgUnitGroup;
import org.hisp.dhis.expression.function.FunctionOrgUnitProgram;
import org.hisp.dhis.expression.function.FunctionSubExpression;
import org.hisp.dhis.hibernate.HibernateGenericStore;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.ExpressionItemMethod;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.hisp.dhis.parser.expression.function.FunctionAggregationType;
import org.hisp.dhis.parser.expression.function.FunctionMaxDate;
import org.hisp.dhis.parser.expression.function.FunctionMinDate;
import org.hisp.dhis.parser.expression.function.FunctionNormDistCum;
import org.hisp.dhis.parser.expression.function.FunctionNormDistDen;
import org.hisp.dhis.parser.expression.function.FunctionYearToDate;
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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.util.DateUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Suppliers;
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
    private final HibernateGenericStore<Expression> expressionStore;

    private final ConstantService constantService;

    private final DimensionService dimensionService;

    private final IdentifiableObjectManager idObjectManager;

    private final StatementBuilder statementBuilder;

    private final I18nManager i18nManager;

    // -------------------------------------------------------------------------
    // Static data
    // -------------------------------------------------------------------------

    private static final ImmutableMap<Integer, ExpressionItem> BASE_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()
        .putAll( COMMON_EXPRESSION_ITEMS )
        .put( HASH_BRACE, new DimItemDataElementAndOperand() )
        .put( A_BRACE, new DimItemProgramAttribute() )
        .put( D_BRACE, new DimItemProgramDataElement() )
        .put( I_BRACE, new DimItemProgramIndicator() )
        .put( OUG_BRACE, new ItemOrgUnitGroupCount() )
        .put( R_BRACE, new DimItemReportingRate() )
        .put( DAYS, new ItemDays() )
        .build();

    private static final ImmutableMap<Integer, ExpressionItem> VALIDATION_RULE_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()
        .putAll( BASE_EXPRESSION_ITEMS )
        .put( ORGUNIT_ANCESTOR, new FunctionOrgUnitAncestor() )
        .put( ORGUNIT_DATASET, new FunctionOrgUnitDataSet() )
        .put( ORGUNIT_GROUP, new FunctionOrgUnitGroup() )
        .put( ORGUNIT_PROGRAM, new FunctionOrgUnitProgram() )
        .build();

    private static final ImmutableMap<Integer, ExpressionItem> PREDICTOR_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()
        .putAll( VALIDATION_RULE_EXPRESSION_ITEMS )
        .put( AVG, new VectorAvg() )
        .put( COUNT, new VectorCount() )
        .put( MAX, new VectorMax() )
        .put( MAX_DATE, new FunctionMaxDate() )
        .put( MEDIAN, new VectorMedian() )
        .put( MIN, new VectorMin() )
        .put( MIN_DATE, new FunctionMinDate() )
        .put( NORM_DIST_CUM, new FunctionNormDistCum() )
        .put( NORM_DIST_DEN, new FunctionNormDistDen() )
        .put( PERCENTILE_CONT, new VectorPercentileCont() )
        .put( STDDEV, new VectorStddevSamp() )
        .put( STDDEV_POP, new VectorStddevPop() )
        .put( STDDEV_SAMP, new VectorStddevSamp() )
        .put( SUM, new VectorSum() )
        .build();

    private static final ImmutableMap<Integer, ExpressionItem> INDICATOR_EXPRESSION_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()
        .putAll( BASE_EXPRESSION_ITEMS )
        .put( AGGREGATION_TYPE, new FunctionAggregationType() )
        .put( N_BRACE, new DimItemIndicator() )
        .put( MAX_DATE, new FunctionMaxDate() )
        .put( MIN_DATE, new FunctionMinDate() )
        .put( PERIOD_IN_YEAR, new ItemPeriodInYear() )
        .put( PERIOD_OFFSET, new PeriodOffset() )
        .put( SUB_EXPRESSION, new FunctionSubExpression() )
        .put( YEARLY_PERIOD_COUNT, new ItemYearlyPeriodCount() )
        .put( YEAR_TO_DATE, new FunctionYearToDate() )
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
        ConstantService constantService, DimensionService dimensionService, IdentifiableObjectManager idObjectManager,
        StatementBuilder statementBuilder, I18nManager i18nManager, CacheProvider cacheProvider )
    {
        checkNotNull( expressionStore );
        checkNotNull( constantService );
        checkNotNull( dimensionService );
        checkNotNull( idObjectManager );
        checkNotNull( statementBuilder );
        checkNotNull( i18nManager );
        checkNotNull( cacheProvider );

        this.expressionStore = expressionStore;
        this.constantService = constantService;
        this.dimensionService = dimensionService;
        this.idObjectManager = idObjectManager;
        this.statementBuilder = statementBuilder;
        this.i18nManager = i18nManager;
        this.constantMapCache = cacheProvider.createAllConstantsCache();

        FunctionSubExpression fn = (FunctionSubExpression) INDICATOR_EXPRESSION_ITEMS.get( SUB_EXPRESSION );

        if ( fn != null )
        {
            fn.init( cacheProvider );
        }
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
    public List<OrganisationUnitGroup> getOrgUnitGroupCountGroups( Collection<Indicator> indicators )
    {
        if ( indicators == null )
        {
            return Collections.emptyList();
        }

        Set<String> uids = new HashSet<>();

        for ( Indicator indicator : indicators )
        {
            uids.addAll( getOrgUnitGroupCountIds( indicator.getNumerator() ) );
            uids.addAll( getOrgUnitGroupCountIds( indicator.getDenominator() ) );
        }

        return idObjectManager.getByUid( OrganisationUnitGroup.class, uids );
    }

    @Override
    public IndicatorValue getIndicatorValueObject( Indicator indicator, List<Period> periods,
        Map<DimensionalItemId, DimensionalItemObject> itemMap, Map<DimensionalItemObject, Object> valueMap,
        Map<String, Integer> orgUnitCountMap )
    {
        if ( indicator == null || indicator.getNumerator() == null || indicator.getDenominator() == null )
        {
            return null;
        }

        Integer days = periods != null ? getDaysFromPeriods( periods ) : null;

        ExpressionParams params = ExpressionParams.builder()
            .parseType( INDICATOR_EXPRESSION )
            .itemMap( itemMap )
            .valueMap( valueMap )
            .orgUnitCountMap( orgUnitCountMap )
            .periods( periods )
            .days( days )
            .missingValueStrategy( SKIP_IF_ALL_VALUES_MISSING )
            .build();

        Double denominatorValue = castDouble( getExpressionValue( params.toBuilder()
            .expression( indicator.getDenominator() ).build() ) );

        Double numeratorValue = castDouble( getExpressionValue( params.toBuilder()
            .expression( indicator.getNumerator() ).build() ) );

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
            .load( idObjectManager.getAllNoAcl( Constant.class ), IdentifiableObject::getUid );

        Map<String, OrganisationUnitGroup> orgUnitGroups = new CachingMap<String, OrganisationUnitGroup>()
            .load( idObjectManager.getAllNoAcl( OrganisationUnitGroup.class ), IdentifiableObject::getUid );

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

        Map<String, String> itemDescriptions = getExpressionItemDescriptions( expression, parseType, dataType );

        String description = expression;

        for ( Map.Entry<String, String> entry : itemDescriptions.entrySet() )
        {
            description = description.replace( entry.getKey(), entry.getValue() );
        }

        return description;
    }

    @Override
    public Map<String, String> getExpressionItemDescriptions( String expression, ParseType parseType )
    {
        return getExpressionItemDescriptions( expression, parseType, parseType.getDataType() );
    }

    @Override
    public Map<String, String> getExpressionItemDescriptions( String expression, ParseType parseType,
        DataType dataType )
    {
        CommonExpressionVisitor visitor = newVisitor( ITEM_GET_DESCRIPTIONS, ExpressionParams.builder()
            .expression( expression )
            .parseType( parseType )
            .dataType( dataType )
            .missingValueStrategy( NEVER_SKIP )
            .build() );

        visit( expression, dataType, visitor, false );

        return visitor.getItemDescriptions();
    }

    @Override
    public ExpressionInfo getExpressionInfo( ExpressionParams params )
    {
        if ( StringUtils.isEmpty( params.getExpression() ) )
        {
            return new ExpressionInfo();
        }

        CommonExpressionVisitor visitor = newVisitor( ITEM_GET_EXPRESSION_INFO, params );

        visit( params.getExpression(), params.getDataType(), visitor, true );

        return visitor.getInfo();
    }

    @Override
    public ExpressionParams getBaseExpressionParams( ExpressionInfo info )
    {
        return ExpressionParams.builder()
            .itemMap( dimensionService.getNoAclDataDimensionalItemObjectMap( info.getAllItemIds() ) )
            .orgUnitGroupMap( getUidMap( OrganisationUnitGroup.class, info.getOrgUnitGroupIds() ) )
            .dataSetMap( getUidMap( DataSet.class, info.getOrgUnitDataSetIds() ) )
            .programMap( getUidMap( Program.class, info.getOrgUnitProgramIds() ) )
            .build();
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
    public Set<DimensionalItemId> getExpressionDimensionalItemIds( String expression, ParseType parseType )
    {
        Set<DimensionalItemId> itemIds = new HashSet<>();

        getExpressionDimensionalItemIds( expression, parseType, itemIds, itemIds );

        return itemIds;
    }

    @Override
    public Set<String> getExpressionOrgUnitGroupIds( String expression, ParseType parseType )
    {
        if ( isEmpty( expression ) )
        {
            return Collections.emptySet();
        }

        ExpressionInfo info = getExpressionInfo( ExpressionParams.builder()
            .expression( expression )
            .parseType( parseType )
            .build() );

        return info.getOrgUnitGroupIds();
    }

    // -------------------------------------------------------------------------
    // Compute the value of the expression
    // -------------------------------------------------------------------------

    @Override
    public Object getExpressionValue( ExpressionParams params )
    {
        if ( isEmpty( params.getExpression() ) )
        {
            return null;
        }

        CommonExpressionVisitor visitor = newVisitor( ITEM_EVALUATE, params );

        Object value = visit( params.getExpression(), params.getDataType(), visitor, true );

        ExpressionState state = visitor.getState();

        int itemsFound = state.getItemsFound();
        int itemValuesFound = state.getItemValuesFound();

        if ( state.isUnprotectedNullDateFound() )
        {
            return null;
        }

        switch ( params.getMissingValueStrategy() )
        {
        case SKIP_IF_ANY_VALUE_MISSING:
            if ( itemValuesFound < itemsFound )
            {
                return null;
            }
            break;

        case SKIP_IF_ALL_VALUES_MISSING:
            if ( itemsFound != 0 && itemValuesFound == 0 )
            {
                return null;
            }
            break;

        case NEVER_SKIP:
            break;
        }

        if ( value == null && state.isReplaceNulls() )
        {
            switch ( params.getDataType() )
            {
            case NUMERIC:
                return 0d;

            case BOOLEAN:
                return FALSE;

            case TEXT:
                return "";
            }
        }

        return value;
    }

    // -------------------------------------------------------------------------
    // Create a new CommonExpressionVisitor
    // -------------------------------------------------------------------------

    @Override
    public Map<String, Constant> getConstantMap()
    {
        return constantMapCache.get( "x", key -> constantService.getConstantMap() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns the OrgUnitGroupCountIds from an expression
     */
    private Set<String> getOrgUnitGroupCountIds( String expression )
    {
        if ( expression == null )
        {
            return Collections.emptySet();
        }
        try
        {
            ExpressionInfo info = getExpressionInfo( ExpressionParams.builder()
                .expression( expression )
                .parseType( INDICATOR_EXPRESSION )
                .build() );

            return info.getOrgUnitGroupCountIds();
        }
        catch ( ParserException e )
        {
            log.warn( "Parsing error in indicator expression " + expression );

            return Collections.emptySet();
        }
    }

    /**
     * Returns all non-aggregated and all aggregated dimensional item object ids
     * in the given expression
     */
    private void getExpressionDimensionalItemIds( String expression, ParseType parseType,
        Set<DimensionalItemId> itemIds,
        Set<DimensionalItemId> sampleItemIds )
    {
        if ( isEmpty( expression ) )
        {
            return;
        }

        ExpressionInfo info = getExpressionInfo( ExpressionParams.builder()
            .expression( expression )
            .parseType( parseType )
            .build() );

        itemIds.addAll( info.getItemIds() );
        sampleItemIds.addAll( info.getSampleItemIds() );
    }

    private <T extends IdentifiableObject> Map<String, T> getUidMap( Class<T> type, Collection<String> uids )
    {
        List<T> objects = idObjectManager.getNoAcl( type, uids );

        return IdentifiableObjectUtils.getIdMap( objects, IdScheme.UID );
    }

    /**
     * Creates a new {@see CommonExpressionVisitor}
     */
    private CommonExpressionVisitor newVisitor( ExpressionItemMethod itemMethod, ExpressionParams params )
    {
        return CommonExpressionVisitor.builder()
            .idObjectManager( idObjectManager )
            .dimensionService( dimensionService )
            .statementBuilder( statementBuilder )
            .i18nSupplier( Suppliers.memoize( i18nManager::getI18n ) )
            .constantMap( getConstantMap() )
            .itemMap( PARSE_TYPE_EXPRESSION_ITEMS.get( params.getParseType() ) )
            .itemMethod( itemMethod )
            .params( params )
            .info( params.getExpressionInfo() )
            .build();
    }

    /**
     * Visits an expression and returns the expected expression type.
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
     */
    private int getDaysFromPeriods( List<Period> periods )
    {
        return periods.stream()
            .filter( Objects::nonNull )
            .mapToInt( Period::getDaysInPeriod ).sum();
    }
}
