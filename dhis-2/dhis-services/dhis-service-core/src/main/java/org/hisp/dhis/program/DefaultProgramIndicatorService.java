package org.hisp.dhis.program;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.parser.expression.ParserUtils.castClass;
import static org.hisp.dhis.parser.expression.ParserUtils.castString;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.hisp.dhis.common.IdentifiableObjectStore;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.expression.Parser;
import org.hisp.dhis.parser.expression.ParserException;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Chau Thu Tran
 */
public class DefaultProgramIndicatorService
    implements ProgramIndicatorService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ProgramIndicatorStore programIndicatorStore;

    private ProgramStageService programStageService;

    private DataElementService dataElementService;

    private TrackedEntityAttributeService attributeService;

    private ConstantService constantService;

    private StatementBuilder statementBuilder;

    private IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore;

    private I18nManager i18nManager;

    private RelationshipTypeService relationshipTypeService;

    private static Cache<String> ANALYTICS_SQL_CACHE = new SimpleCacheBuilder<String>().forRegion( "analyticsSql" )
        .expireAfterAccess( 10, TimeUnit.HOURS )
        .withInitialCapacity( 10000 )
        .withMaximumSize( 50000 )
        .build();

    public DefaultProgramIndicatorService( ProgramIndicatorStore programIndicatorStore,
        ProgramStageService programStageService, DataElementService dataElementService,
        TrackedEntityAttributeService attributeService, ConstantService constantService, StatementBuilder statementBuilder,
        @Qualifier("org.hisp.dhis.program.ProgramIndicatorGroupStore") IdentifiableObjectStore<ProgramIndicatorGroup> programIndicatorGroupStore,
        I18nManager i18nManager, RelationshipTypeService relationshipTypeService )
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
    }



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
    @Transactional(readOnly = true)
    public ProgramIndicator getProgramIndicator( long id )
    {
        return programIndicatorStore.get( id );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicator getProgramIndicator( String name )
    {
        return programIndicatorStore.getByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicator getProgramIndicatorByUid( String uid )
    {
        return programIndicatorStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramIndicator> getAllProgramIndicators()
    {
        return programIndicatorStore.getAll();
    }


    // -------------------------------------------------------------------------
    // ProgramIndicator logic
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    @Deprecated public String getUntypedDescription( String expression )
    {
        return getDescription( expression, null );
    }

    @Override
    @Transactional(readOnly = true)
    public String getExpressionDescription( String expression )
    {
        return getDescription( expression, Double.class );
    }

    @Override
    @Transactional(readOnly = true)
    public String getFilterDescription( String expression )
    {
        return getDescription( expression, Boolean.class );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean expressionIsValid( String expression )
    {
        return isValid( expression, Double.class );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean filterIsValid( String filter )
    {
        return isValid( filter, Boolean.class );
    }

    @Override
    @Transactional(readOnly = true)
    public void validate( String expression, Class<?> clazz, Map<String, String> itemDescriptions )
    {
        ProgramValidator programExpressionValidator = new ProgramValidator(
            this, constantService, programStageService,
            dataElementService, attributeService, relationshipTypeService,
            i18nManager.getI18n(), itemDescriptions );

        castClass( clazz, Parser.visit( expression, programExpressionValidator ) );
    }

    @Override
    @Transactional(readOnly = true)
    public String getAnalyticsSql( String expression, ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        if ( expression == null )
        {
            return null;
        }

        String cacheKey = getAnalyticsSqlCacheKey( expression, programIndicator, startDate, endDate );

        return ANALYTICS_SQL_CACHE.get( cacheKey, k -> _getAnalyticsSql( expression, programIndicator, startDate, endDate ) ).orElse( null );
    }

    private String getAnalyticsSqlCacheKey( String expression, ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        return expression
            + "|" + programIndicator.getUid()
            + "|" + startDate.getTime()
            + "|" + endDate.getTime();
    }

    private String _getAnalyticsSql( String expression, ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        Set<String> dataElementAndAttributeIdentifiers = getDataElementAndAttributeIdentifiers(
            expression, programIndicator.getAnalyticsType() );

        ProgramSqlGenerator programSqlGenerator = new ProgramSqlGenerator( programIndicator, startDate, endDate,
            dataElementAndAttributeIdentifiers, constantService.getConstantMap(),
            this, statementBuilder, dataElementService, attributeService );

        return castString( Parser.visit( expression, programSqlGenerator ) );
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public ProgramIndicatorGroup getProgramIndicatorGroup( long id )
    {
        return programIndicatorGroupStore.get( id );
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramIndicatorGroup getProgramIndicatorGroup( String uid )
    {
        return programIndicatorGroupStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramIndicatorGroup> getAllProgramIndicatorGroups()
    {
        return programIndicatorGroupStore.getAll();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

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

        ProgramElementsAndAttributesCollecter listener = new ProgramElementsAndAttributesCollecter( items, analyticsType );

        Parser.listen( expression, listener );

        return items;
    }}
