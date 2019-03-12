package org.hisp.dhis.parser.program;

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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.jdbc.StatementBuilder;
import org.hisp.dhis.parser.common.Parser;
import org.hisp.dhis.parser.common.ParserException;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.relationship.RelationshipTypeService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hisp.dhis.parser.common.ParserUtils.*;

/**
 * Parses program indicators/filters/rules using the ANTLR parser.
 *
 * @author Chau Thu Tran
 * @author Jim Grace
 */
public class DefaultProgramParserService
    implements ProgramParserService
{
    private static final Log log = LogFactory.getLog( DefaultProgramParserService.class );

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private StatementBuilder statementBuilder;

    @Autowired
    private I18nManager i18nManager;

    private static Cache<String, ParseTree> PROGRAM_PARSE_TREES = Caffeine.newBuilder()
        .expireAfterAccess( 10, TimeUnit.MINUTES ).initialCapacity( 10000 )
        .maximumSize( 50000 ).build();

    @Override
    @Transactional
    @Deprecated public String getUntypedDescription( String expression )
    {
        return getDescription( expression, null );
    }

    @Override
    @Transactional
    public String getExpressionDescription( String expression )
    {
        return getDescription( expression, Double.class );
    }

    @Override
    @Transactional
    public String getFilterDescription( String expression )
    {
        return getDescription( expression, Boolean.class );
    }

    @Override
    @Transactional
    public boolean expressionIsValid( String expression )
    {
        return isValid( expression, Double.class );
    }

    @Override
    @Transactional
    public boolean filterIsValid( String filter )
    {
        return isValid( filter, Boolean.class );
    }

    @Override
    public void validate( String expression, Class clazz, Map<String, String> itemDescriptions )
    {
        ProgramValidator programExpressionValidator = new ProgramValidator(
        this, manager, constantService, programStageService,
            dataElementService, attributeService, relationshipTypeService,
            i18nManager.getI18n(), itemDescriptions );

        castClass( clazz, Parser.visit( expression, programExpressionValidator ) );
    }

    @Override
    public String getExpressionAnalyticsSql( ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        return getAnalyticsSql( programIndicator.getExpression(), programIndicator, startDate, endDate, true );
    }

    @Override
    public String getFilterAnalyticsSql( ProgramIndicator programIndicator, Date startDate, Date endDate )
    {
        return getAnalyticsSql( programIndicator.getFilter(), programIndicator, startDate, endDate, false );
    }

    @Override
    public String getAnalyticsSql( String expression, ProgramIndicator programIndicator, Date startDate, Date endDate, boolean ignoreMissingValues )
    {
        if ( expression == null )
        {
            return null;
        }

        Set<String> dataElementAndAttributeIdentifiers = getDataElementAndAttributeIdentifiers(
            expression, programIndicator.getAnalyticsType() );

        ProgramSqlGenerator programSqlGenerator = new ProgramSqlGenerator( programIndicator, startDate, endDate,
            ignoreMissingValues, dataElementAndAttributeIdentifiers, constantService.getConstantMap(),
            this, statementBuilder, dataElementService, attributeService );

        return castString( Parser.visit( expression, programSqlGenerator ) );
    }

    @Override
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
    // Supportive methods
    // -------------------------------------------------------------------------

    private String getDescription( String expression, Class clazz )
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

    private boolean isValid( String expression, Class clazz )
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
    }
}
