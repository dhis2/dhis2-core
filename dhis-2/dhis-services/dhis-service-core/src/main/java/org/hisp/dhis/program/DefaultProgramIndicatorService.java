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
package org.hisp.dhis.program;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.antlr.AntlrParserUtils.castClass;
import static org.hisp.dhis.antlr.AntlrParserUtils.castString;
import static org.hisp.dhis.jdbc.StatementBuilder.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.parser.expression.ParserUtils.COMMON_EXPRESSION_ITEMS;
import static org.hisp.dhis.parser.expression.ParserUtils.DEFAULT_SAMPLE_PERIODS;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_DESCRIPTIONS;
import static org.hisp.dhis.parser.expression.ParserUtils.ITEM_GET_SQL;
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
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.STDDEV;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.SUM;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.VARIANCE;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.V_BRACE;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.antlr.Parser;
import org.hisp.dhis.antlr.ParserException;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionItem;
import org.hisp.dhis.parser.expression.ExpressionItemMethod;
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
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableMap;

/**
 * @author Chau Thu Tran
 */
@Service( "org.hisp.dhis.program.ProgramIndicatorService" )
public class DefaultProgramIndicatorService
    implements ProgramIndicatorService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final ProgramIndicatorStore programIndicatorStore;

    private final ProgramStageService programStageService;

    private final DataElementService dataElementService;

    private final TrackedEntityAttributeService attributeService;

    private final ConstantService constantService;

    private final StatementBuilder statementBuilder;

    private final IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

    private final I18nManager i18nManager;

    private final RelationshipTypeService relationshipTypeService;

    private final Cache<String> analyticsSqlCache;

    public DefaultProgramIndicatorService( ProgramIndicatorStore programIndicatorStore,
        ProgramStageService programStageService, DataElementService dataElementService,
        TrackedEntityAttributeService attributeService, ConstantService constantService,
        StatementBuilder statementBuilder,
        @Qualifier( "org.hisp.dhis.program.ProgramIndicatorGroupStore" ) IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore,
        I18nManager i18nManager, RelationshipTypeService relationshipTypeService, CacheProvider cacheProvider )
    {
        checkNotNull( programIndicatorStore );
        checkNotNull( programStageService );
        checkNotNull( dataElementService );
        checkNotNull( attributeService );
        checkNotNull( constantService );
        checkNotNull( statementBuilder );
        checkNotNull( programIndicatorGroupStore );
        checkNotNull( i18nManager );
        checkNotNull( relationshipTypeService );

        this.programIndicatorStore = programIndicatorStore;
        this.programStageService = programStageService;
        this.dataElementService = dataElementService;
        this.attributeService = attributeService;
        this.constantService = constantService;
        this.statementBuilder = statementBuilder;
        this.programIndicatorGroupStore = programIndicatorGroupStore;
        this.i18nManager = i18nManager;
        this.relationshipTypeService = relationshipTypeService;
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
        CommonExpressionVisitor visitor = newVisitor( ITEM_GET_DESCRIPTIONS, NUMERIC );

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
        Date startDate, Date endDate,
        String tableAlias )
    {
        return getAnalyticsSqlCached( expression, dataType, programIndicator, startDate, endDate, tableAlias );
    }

    private String getAnalyticsSqlCached( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate,
        Date endDate, String tableAlias )
    {
        if ( expression == null )
        {
            return null;
        }

        String cacheKey = getAnalyticsSqlCacheKey( expression, dataType, programIndicator, startDate, endDate,
            tableAlias );

        return analyticsSqlCache
            .get( cacheKey, k -> _getAnalyticsSql( expression, dataType, programIndicator, startDate, endDate,
                tableAlias ) )
            .orElse( null );
    }

    private String getAnalyticsSqlCacheKey( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String tableAlias )
    {
        return expression
            + "|" + dataType.name()
            + "|" + programIndicator.getUid()
            + "|" + startDate.getTime()
            + "|" + endDate.getTime()
            + "|" + (tableAlias == null ? "" : tableAlias);
    }

    private String _getAnalyticsSql( String expression, DataType dataType, ProgramIndicator programIndicator,
        Date startDate, Date endDate, String tableAlias )
    {
        // Get the uids from the expression even if this is the filter
        Set<String> uids = getDataElementAndAttributeIdentifiers( programIndicator.getExpression(),
            programIndicator.getAnalyticsType() );

        CommonExpressionVisitor visitor = newVisitor( ITEM_GET_SQL, dataType );

        visitor.setExpressionLiteral( new SqlLiteral() );
        visitor.setProgramIndicator( programIndicator );
        visitor.setReportingStartDate( startDate );
        visitor.setReportingEndDate( endDate );
        visitor.setDataElementAndAttributeIdentifiers( uids );

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

    private CommonExpressionVisitor newVisitor( ExpressionItemMethod itemMethod, DataType dataType )
    {
        return CommonExpressionVisitor.newBuilder()
            .withItemMap( PROGRAM_INDICATOR_ITEMS )
            .withItemMethod( itemMethod )
            .withDataType( dataType )
            .withConstantMap( constantService.getConstantMap() )
            .withProgramIndicatorService( this )
            .withProgramStageService( programStageService )
            .withDataElementService( dataElementService )
            .withAttributeService( attributeService )
            .withRelationshipTypeService( relationshipTypeService )
            .withStatementBuilder( statementBuilder )
            .withI18n( i18nManager.getI18n() )
            .withSamplePeriods( DEFAULT_SAMPLE_PERIODS )
            .buildForProgramIndicatorExpressions();
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
