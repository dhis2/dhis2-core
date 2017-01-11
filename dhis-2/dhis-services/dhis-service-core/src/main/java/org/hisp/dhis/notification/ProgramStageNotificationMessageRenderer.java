package org.hisp.dhis.notification;

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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.notification.ProgramStageTemplateVariable;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 */
public class ProgramStageNotificationMessageRenderer
    extends BaseNotificationMessageRenderer<ProgramStageInstance>
{
    private static final ImmutableMap<TemplateVariable, Function<ProgramStageInstance, String>> VARIABLE_RESOLVERS =
        new ImmutableMap.Builder<TemplateVariable, Function<ProgramStageInstance, String>>()
            .put( ProgramStageTemplateVariable.PROGRAM_NAME,         psi -> psi.getProgramStage().getProgram().getDisplayName() )
            .put( ProgramStageTemplateVariable.PROGRAM_STAGE_NAME,   psi -> psi.getProgramStage().getDisplayName() )
            .put( ProgramStageTemplateVariable.ORG_UNIT_NAME,        psi -> psi.getOrganisationUnit().getDisplayName() )
            .put( ProgramStageTemplateVariable.DUE_DATE,             psi -> formatDate( psi.getDueDate() ) )
            .put( ProgramStageTemplateVariable.DAYS_SINCE_DUE_DATE,  psi -> daysSince( psi.getDueDate() ) )
            .put( ProgramStageTemplateVariable.DAYS_UNTIL_DUE_DATE,  psi -> daysUntil( psi.getDueDate() ) )
            .put( ProgramStageTemplateVariable.CURRENT_DATE,         psi -> formatDate( new Date() ) )
            .build();

    private static final Set<ExpressionType> SUPPORTED_EXPRESSION_TYPES =
        ImmutableSet.of( ExpressionType.ATTRIBUTE, ExpressionType.VARIABLE );

    // -------------------------------------------------------------------------
    // Singleton instance
    // -------------------------------------------------------------------------

    public static final ProgramStageNotificationMessageRenderer INSTANCE = new ProgramStageNotificationMessageRenderer();

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    private ProgramStageNotificationMessageRenderer()
    {
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    @Override
    protected ImmutableMap<TemplateVariable, Function<ProgramStageInstance, String>> getVariableResolvers()
    {
        return VARIABLE_RESOLVERS;
    }

    @Override
    protected Map<String, String> resolveAttributeValues( Set<String> attributeKeys, ProgramStageInstance entity )
    {
        if ( attributeKeys.isEmpty() )
        {
            return Maps.newHashMap();
        }

        return entity.getProgramInstance().getEntityInstance().getTrackedEntityAttributeValues().stream()
            .filter( av -> attributeKeys.contains( av.getAttribute().getUid() ) )
            .collect( Collectors.toMap( av -> av.getAttribute().getUid(), ProgramStageNotificationMessageRenderer::filterValue ) );
    }

    @Override
    protected TemplateVariable fromVariableName( String name )
    {
        return ProgramStageTemplateVariable.fromVariableName( name );
    }

    @Override
    protected Set<ExpressionType> getSupportedExpressionTypes()
    {
        return SUPPORTED_EXPRESSION_TYPES;
    }

    // -------------------------------------------------------------------------
    // Internal methods
    // -------------------------------------------------------------------------

    private static String filterValue( TrackedEntityAttributeValue av )
    {
        String value = av.getPlainValue();

        if ( value == null )
        {
            return CONFIDENTIAL_VALUE_REPLACEMENT;
        }

        // If the AV has an OptionSet -> substitute value with the name of the Option
        if ( av.getAttribute().hasOptionSet() )
        {
            value = av.getAttribute().getOptionSet().getOptionByCode( value ).getName();
        }

        return value != null ? value : MISSING_VALUE_REPLACEMENT;
    }
}
