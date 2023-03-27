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
package org.hisp.dhis.program;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.antlr.AntlrParserUtils.castClass;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.expression.ExpressionParams.DEFAULT_EXPRESSION_PARAMS;
import static org.hisp.dhis.jdbc.StatementBuilder.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_DESCRIPTIONS;
import static org.hisp.dhis.parser.expression.ExpressionItem.ITEM_GET_SQL;
import static org.hisp.dhis.parser.expression.ParserUtils.COMMON_EXPRESSION_ITEMS;
import static org.hisp.dhis.parser.expression.ProgramExpressionParams.DEFAULT_PROGRAM_EXPRESSION_PARAMS;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.AVG;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.A_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_CONDITION;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_COUNT_IF_CONDITION;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_COUNT_IF_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_DAYS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_HAS_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MAX_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MINUTES_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MIN_VALUE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_MONTHS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_OIZP;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_RELATIONSHIP_COUNT;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_WEEKS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_YEARS_BETWEEN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_ZING;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.D2_ZPVC;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.HASH_BRACE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MAX;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.MIN;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.PS_EVENTDATE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STAGE_OFFSET;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.SUM;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.VARIANCE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.V_BRACE;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.DimensionService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.ExpressionItemMethod;
import org.hisp.dhis.parser.expression.ProgramExpressionParams;
import org.hisp.dhis.parser.expression.function.RepeatableProgramStageOffset;
import org.hisp.dhis.parser.expression.function.VectorAvg;
import org.hisp.dhis.parser.expression.function.VectorCount;
import org.hisp.dhis.parser.expression.function.VectorMax;
import org.hisp.dhis.parser.expression.function.VectorMin;
import org.hisp.dhis.parser.expression.function.VectorStddevSamp;
import org.hisp.dhis.parser.expression.function.VectorSum;
import org.hisp.dhis.parser.expression.function.VectorVariance;
import org.hisp.dhis.parser.expression.literal.SqlLiteral;
import org.hisp.dhis.program.dataitem.ProgramItemAttribute;
import org.hisp.dhis.program.dataitem.ProgramItemPsEventdate;
import org.hisp.dhis.program.dataitem.ProgramItemStageElement;
import org.hisp.dhis.program.function.D2Condition;
import org.hisp.dhis.program.function.D2Count;
import org.hisp.dhis.program.function.D2CountIfCondition;
import org.hisp.dhis.program.function.D2CountIfValue;
import org.hisp.dhis.program.function.D2DaysBetween;
import org.hisp.dhis.program.function.D2HasValue;
import org.hisp.dhis.program.function.D2MaxValue;
import org.hisp.dhis.program.function.D2MinValue;
import org.hisp.dhis.program.function.D2MinutesBetween;
import org.hisp.dhis.program.function.D2MonthsBetween;
import org.hisp.dhis.program.function.D2Oizp;
import org.hisp.dhis.program.function.D2RelationshipCount;
import org.hisp.dhis.program.function.D2WeeksBetween;
import org.hisp.dhis.program.function.D2YearsBetween;
import org.hisp.dhis.program.function.D2Zing;
import org.hisp.dhis.program.function.D2Zpvc;
import org.hisp.dhis.program.variable.ProgramVariableItem;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;

/**
 * @author Chau Thu Tran
 */
@Service( "org.hisp.dhis.program.ProgramIndicatorService" )
public class DefaultProgramIndicatorService
    implements ProgramIndicatorService
{
    private final ProgramIndicatorStore programIndicatorStore;

    private final IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

    private final ProgramStageService programStageService;

    private final IdentifiableObjectManager idObjectManager;

    private final StatementBuilder statementBuilder;

    private final ExpressionService expressionService;

    private final DimensionService dimensionService;

    private final I18nManager i18nManager;

    private final Cache<String> analyticsSqlCache;

    public DefaultProgramIndicatorService( ProgramIndicatorStore programIndicatorStore,
        @Qualifier( "org.hisp.dhis.program.ProgramIndicatorGroupStore" ) IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore,
        ProgramStageService programStageService, IdentifiableObjectManager idObjectManager,
        StatementBuilder statementBuilder, ExpressionService expressionService, DimensionService dimensionService,
        I18nManager i18nManager, CacheProvider cacheProvider )
    {
        checkNotNull( programIndicatorStore );
        checkNotNull( programIndicatorGroupStore );
        checkNotNull( programStageService );
        checkNotNull( idObjectManager );
        checkNotNull( statementBuilder );
        checkNotNull( expressionService );
        checkNotNull( dimensionService );
        checkNotNull( i18nManager );
        checkNotNull( cacheProvider );

        this.programIndicatorStore = programIndicatorStore;
        this.programIndicatorGroupStore = programIndicatorGroupStore;
        this.programStageService = programStageService;
        this.idObjectManager = idObjectManager;
        this.statementBuilder = statementBuilder;
        this.expressionService = expressionService;
        this.dimensionService = dimensionService;
        this.i18nManager = i18nManager;
        this.analyticsSqlCache = cacheProvider.createAnalyticsSqlCache();
    }

    public static final ImmutableMap<Integer, ExpressionItem> PROGRAM_INDICATOR_ITEMS = ImmutableMap
        .<Integer, ExpressionItem> builder()

        // Common functions

        .putAll( COMMON_EXPRESSION_ITEMS )

        // Program functions

        .put( D2_CONDITION, new D2Condition() )
        .put( D2_COUNT, new D2Count() )
        .put( D2_COUNT_IF_CONDITION, new D2CountIfCondition() )
        .put( D2_COUNT_IF_VALUE, new D2CountIfValue() )
        .put( D2_DAYS_BETWEEN, new D2DaysBetween() )
        .put( D2_HAS_VALUE, new D2HasValue() )
        .put( D2_MAX_VALUE, new D2MaxValue() )
        .put( D2_MINUTES_BETWEEN, new D2MinutesBetween() )
        .put( D2_MIN_VALUE, new D2MinValue() )
        .put( D2_MONTHS_BETWEEN, new D2MonthsBetween() )
        .put( D2_OIZP, new D2Oizp() )
        .put( D2_RELATIONSHIP_COUNT, new D2RelationshipCount() )
        .put( D2_WEEKS_BETWEEN, new D2WeeksBetween() )
        .put( D2_YEARS_BETWEEN, new D2YearsBetween() )
        .put( D2_ZING, new D2Zing() )
        .put( D2_ZPVC, new D2Zpvc() )

        // Program functions for custom aggregation

        .put( AVG, new VectorAvg() )
        .put( COUNT, new VectorCount() )
        .put( MAX, new VectorMax() )
        .put( MIN, new VectorMin() )
        .put( STDDEV, new VectorStddevSamp() )
        .put( SUM, new VectorSum() )
        .put( VARIANCE, new VectorVariance() )

        // Data items

        .put( HASH_BRACE, new ProgramItemStageElement() )
        .put( A_BRACE, new ProgramItemAttribute() )
        .put( PS_EVENTDATE, new ProgramItemPsEventdate() )

        // Program variables

        .put( V_BRACE, new ProgramVariableItem() )

        // . functions
        .put( STAGE_OFFSET, new RepeatableProgramStageOffset() )

        .build();

    // -------------------------------------------------------------------------
    // ProgramIndicator CRUD
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.save( programIndicator );
        return programIndicator.getId();
    }

    @Override
    @Transactional
    public void updateProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.update( programIndicator );
    }

    @Override
    @Transactional
    public void deleteProgramIndicator( ProgramIndicator programIndicator )
    {
        programIndicatorStore.delete( programIndicator );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramIndicator getProgramIndicator( long id )
    {
        return programIndicatorStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramIndicator getProgramIndicator( String name )
    {
        return programIndicatorStore.getByName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramIndicator getProgramIndicatorByUid( String uid )
    {
        return programIndicatorStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramIndicator> getAllProgramIndicators()
    {
        return programIndicatorStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramIndicator> getProgramIndicatorsWithNoExpression()
    {
        return programIndicatorStore.getProgramIndicatorsWithNoExpression();
    }

    // -------------------------------------------------------------------------
    // ProgramIndicator logic
    // -------------------------------------------------------------------------

    @Override
    @Transactional( readOnly = true )
    @Deprecated
    public String getUntypedDescription( String expression )
    {
        return getDescription( expression, null );
    }

    @Override
    @Transactional( readOnly = true )
    public String getExpressionDescription( String expression )
    {
        return getDescription( expression, Double.class );
    }

    @Override
    @Transactional( readOnly = true )
    public String getFilterDescription( String expression )
    {
        return getDescription( expression, Boolean.class );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean expressionIsValid( String expression )
    {
        return isValid( expression, Double.class );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean filterIsValid( String filter )
    {
        return isValid( filter, Boolean.class );
    }

    @Override
    @Transactional( readOnly = true )
    public void validate( String expression, Class<?> clazz, Map<String, String> itemDescriptions )
    {
        CommonExpressionVisitor visitor = newVisitor( ITEM_GET_DESCRIPTIONS, DEFAULT_EXPRESSION_PARAMS,
            DEFAULT_PROGRAM_EXPRESSION_PARAMS );

        castClass( clazz, Parser.visit( expression, visitor ) );

        itemDescriptions.putAll( visitor.getItemDescriptions() );
    }

    @Override
    @Transactional( readOnly = true )
    public String getAnalyticsSql( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate )
    {
        return getAnalyticsSqlCached( expression, dataType, programIndicator, startDate, endDate, null );
    }

    @Override
    @Transactional( readOnly = true )
    public String getAnalyticsSql( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String tableAlias )
    {
        return getAnalyticsSqlCached( expression, dataType, programIndicator, startDate, endDate, tableAlias );
    }

    private String getAnalyticsSqlCached( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String tableAlias )
    {
        if ( expression == null )
        {
            return null;
        }

        String cacheKey = getAnalyticsSqlCacheKey( expression, dataType, programIndicator, startDate, endDate,
            tableAlias );

        return analyticsSqlCache
            .get( cacheKey, k -> _getAnalyticsSql( expression, dataType, programIndicator, startDate, endDate,
                tableAlias ) );
    }

    private String getAnalyticsSqlCacheKey( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String tableAlias )
    {
        return expression
            + "|" + dataType.name()
            + "|" + programIndicator.getUid()
            + dateIfPresent( startDate )
            + dateIfPresent( endDate )
            + "|" + (tableAlias == null ? "" : tableAlias);
    }

    /**
     * Returns the time in milliseconds if the date is present, otherwise an
     * empty string.
     *
     * @param date the date
     * @return the time in milliseconds if the date is present, otherwise an
     *         empty string.
     */
    private String dateIfPresent( Date date )
    {
        return Optional.ofNullable( date )
            .map( Date::getTime )
            .map( millis -> "|" + millis )
            .orElse( StringUtils.EMPTY );
    }

    private String _getAnalyticsSql( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String tableAlias )
    {
        // Get the uids from the expression even if this is the filter
        Set<String> uids = getDataElementAndAttributeIdentifiers( programIndicator.getExpression(),
            programIndicator.getAnalyticsType() );

        ExpressionParams params = ExpressionParams.builder()
            .dataType( dataType )
            .build();

        ProgramExpressionParams progParams = ProgramExpressionParams.builder()
            .programIndicator( programIndicator )
            .reportingStartDate( startDate )
            .reportingEndDate( endDate )
            .dataElementAndAttributeIdentifiers( uids )
            .build();

        CommonExpressionVisitor visitor = newVisitor( ITEM_GET_SQL, params, progParams );

        visitor.setExpressionLiteral( new SqlLiteral() );

        String sql = castString( Parser.visit( expression, visitor ) );

        return (tableAlias != null ? sql.replaceAll( ANALYTICS_TBL_ALIAS + "\\.", tableAlias + "\\." ) : sql);
    }

    @Override
    @Transactional( readOnly = true )
    public String getAnyValueExistsClauseAnalyticsSql( String expression, AnalyticsType analyticsType )
    {
        if ( expression == null )
        {
            return null;
        }

        try
        {
            Set<String> uids = getDataElementAndAttributeIdentifiers( expression, analyticsType );

            if ( uids.isEmpty() )
            {
                return null;
            }

            String sql = StringUtils.EMPTY;

            for ( String uid : uids )
            {
                sql += statementBuilder.columnQuote( uid ) + " is not null or ";
            }

            return TextUtils.removeLastOr( sql ).trim();
        }
        catch ( ParserException e )
        {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // ProgramIndicatorGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.save( programIndicatorGroup );
        return programIndicatorGroup.getId();
    }

    @Override
    @Transactional
    public void updateProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.update( programIndicatorGroup );
    }

    @Override
    @Transactional
    public void deleteProgramIndicatorGroup( ProgramIndicatorGroup programIndicatorGroup )
    {
        programIndicatorGroupStore.delete( programIndicatorGroup );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramIndicatorGroup getProgramIndicatorGroup( long id )
    {
        return programIndicatorGroupStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public ProgramIndicatorGroup getProgramIndicatorGroup( String uid )
    {
        return programIndicatorGroupStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public List<ProgramIndicatorGroup> getAllProgramIndicatorGroups()
    {
        return programIndicatorGroupStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private CommonExpressionVisitor newVisitor( ExpressionItemMethod itemMethod, ExpressionParams params,
        ProgramExpressionParams progParams )
    {
        return CommonExpressionVisitor.builder()
            .idObjectManager( idObjectManager )
            .dimensionService( dimensionService )
            .programIndicatorService( this )
            .programStageService( programStageService )
            .statementBuilder( statementBuilder )
            .i18nSupplier( Suppliers.memoize( i18nManager::getI18n ) )
            .constantMap( expressionService.getConstantMap() )
            .itemMap( PROGRAM_INDICATOR_ITEMS )
            .itemMethod( itemMethod )
            .params( params )
            .progParams( progParams )
            .build();
    }

    private String getDescription( String expression, Class<?> clazz )
    {
        Map<String, String> itemDescriptions = new HashMap<>();

        validate( expression, clazz, itemDescriptions );

        String description = expression;

        for ( Map.Entry<String, String> entry : itemDescriptions.entrySet() )
        {
            description = description.replace( entry.getKey(), entry.getValue() );
        }

        return description;
    }

    private boolean isValid( String expression, Class<?> clazz )
    {
        if ( expression != null )
        {
            try
            {
                validate( expression, clazz, new HashMap<>() );
            }
            catch ( ParserException e )
            {
                return false;
            }
        }

        return true;
    }

    private Set<String> getDataElementAndAttributeIdentifiers( String expression, AnalyticsType analyticsType )
    {
        Set<String> items = new HashSet<>();

        ProgramElementsAndAttributesCollecter listener = new ProgramElementsAndAttributesCollecter( items,
            analyticsType );

        Parser.listen( expression, listener );

        return items;
    }
}
