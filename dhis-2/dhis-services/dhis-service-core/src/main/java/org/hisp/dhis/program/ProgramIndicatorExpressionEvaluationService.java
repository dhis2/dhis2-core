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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.commons.sqlfunc.ConditionalSqlFunction;
import org.hisp.dhis.commons.sqlfunc.HasValueSqlFunction;
import org.hisp.dhis.commons.sqlfunc.OneIfZeroOrPositiveSqlFunction;
import org.hisp.dhis.commons.sqlfunc.RelationshipCountSqlFunction;
import org.hisp.dhis.commons.sqlfunc.SqlFunction;
import org.hisp.dhis.commons.sqlfunc.ZeroIfNegativeSqlFunction;
import org.hisp.dhis.commons.sqlfunc.ZeroPositiveValueCountFunction;
import org.hisp.dhis.commons.util.ExpressionUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @Author Zubair Asghar.
 */
public class ProgramIndicatorExpressionEvaluationService extends BaseProgramExpressionEvaluationService
{
    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    private static final Map<String, SqlFunction> SQL_FUNC_MAP = ImmutableMap.<String, SqlFunction> builder()
        .put( ZeroIfNegativeSqlFunction.KEY, new ZeroIfNegativeSqlFunction() )
        .put( OneIfZeroOrPositiveSqlFunction.KEY, new OneIfZeroOrPositiveSqlFunction() )
        .put( ZeroPositiveValueCountFunction.KEY, new ZeroPositiveValueCountFunction() )
        .put( ConditionalSqlFunction.KEY, new ConditionalSqlFunction() )
        .put( HasValueSqlFunction.KEY, new HasValueSqlFunction() )
        .put( RelationshipCountSqlFunction.KEY, new RelationshipCountSqlFunction() ).build();

    @Override
    protected Map<String, ProgramD2Function> getD2Functions()
    {
        return D2_FUNC_MAP;
    }

    @Override
    protected Map<String, SqlFunction> getSQLFunctions()
    {
        return SQL_FUNC_MAP;
    }

    @Override
    protected Map<String, String> getSourceDataElement( String uid, Matcher matcher )
    {
        Map<String, String> resultMap = new HashMap<>();

        String de = matcher.group( 3 );

        ProgramStage programStage = programStageService.getProgramStage( uid );
        DataElement dataElement = dataElementService.getDataElement( de );

        if ( programStage != null && dataElement != null )
        {
            String programStageName = programStage.getDisplayName();
            String dataElementName = dataElement.getDisplayName();

            resultMap.put( DESCRIPTION, programStageName + SEPARATOR_ID + dataElementName );
            resultMap.put( SAMPLE_VALUE, ValidationUtils.getSubstitutionValue( dataElement.getValueType() ) );
        }
        else
        {
            resultMap.put( ERROR, ExpressionUtils.INVALID_IDENTIFIERS_IN_EXPRESSION );
            return  resultMap;
        }

        return resultMap;
    }

    @Override
    protected Map<String, String> getSourceAttribute( String uid, Matcher matcher )
    {
        Map<String, String> resultMap = new HashMap<>();

        TrackedEntityAttribute attribute = attributeService.getTrackedEntityAttribute( uid );

        if ( attribute != null )
        {
            resultMap.put( DESCRIPTION, attribute.getDisplayName() );
            resultMap.put( SAMPLE_VALUE, ValidationUtils.getSubstitutionValue( attribute.getValueType() ) );
        }
        else
        {
            resultMap.put( ERROR, ExpressionUtils.INVALID_IDENTIFIERS_IN_EXPRESSION );
            return  resultMap;
        }

        return resultMap;
    }
}
