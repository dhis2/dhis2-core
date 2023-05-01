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
package org.hisp.dhis.notification;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.notification.ProgramStageTemplateVariable;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * @author Halvdan Hoem Grelland
 */
@Component
public class ProgramStageNotificationMessageRenderer
    extends BaseNotificationMessageRenderer<Event>
{
    public static final ImmutableMap<TemplateVariable, Function<Event, String>> VARIABLE_RESOLVERS = new ImmutableMap.Builder<TemplateVariable, Function<Event, String>>()
        .put( ProgramStageTemplateVariable.PROGRAM_NAME,
            event -> event.getProgramStage().getProgram().getDisplayName() )
        .put( ProgramStageTemplateVariable.PROGRAM_STAGE_NAME, event -> event.getProgramStage().getDisplayName() )
        .put( ProgramStageTemplateVariable.ORG_UNIT_NAME, event -> event.getOrganisationUnit().getDisplayName() )
        .put( ProgramStageTemplateVariable.ORG_UNIT_ID, event -> event.getOrganisationUnit().getUid() )
        .put( ProgramStageTemplateVariable.ORG_UNIT_CODE, event -> event.getOrganisationUnit().getCode() )
        .put( ProgramStageTemplateVariable.DUE_DATE, event -> formatDate( event.getDueDate() ) )
        .put( ProgramStageTemplateVariable.EVENT_DATE, event -> formatDate( event.getExecutionDate() ) )
        .put( ProgramStageTemplateVariable.DAYS_SINCE_DUE_DATE, event -> daysSince( event.getDueDate() ) )
        .put( ProgramStageTemplateVariable.DAYS_UNTIL_DUE_DATE, event -> daysUntil( event.getDueDate() ) )
        .put( ProgramStageTemplateVariable.CURRENT_DATE, event -> formatDate( new Date() ) )
        .put( ProgramStageTemplateVariable.EVENT_ORG_UNIT_ID, event -> event.getOrganisationUnit().getUid() )
        .put( ProgramStageTemplateVariable.ENROLLMENT_ORG_UNIT_ID,
            event -> event.getProgramInstance().getOrganisationUnit().getUid() )
        .put( ProgramStageTemplateVariable.ENROLLMENT_ORG_UNIT_NAME,
            event -> event.getProgramInstance().getOrganisationUnit().getName() )
        .put( ProgramStageTemplateVariable.ENROLLMENT_ORG_UNIT_CODE,
            event -> event.getProgramInstance().getOrganisationUnit().getCode() )
        .put( ProgramStageTemplateVariable.PROGRAM_ID, event -> event.getProgramStage().getProgram().getUid() )
        .put( ProgramStageTemplateVariable.PROGRAM_STAGE_ID, event -> event.getProgramStage().getUid() )
        .put( ProgramStageTemplateVariable.ENROLLMENT_ID, event -> event.getProgramInstance().getUid() )
        .put( ProgramStageTemplateVariable.TRACKED_ENTITY_ID,
            event -> event.getProgramInstance().getEntityInstance().getUid() )
        .build();

    private static final Set<ExpressionType> SUPPORTED_EXPRESSION_TYPES = ImmutableSet
        .of( ExpressionType.TRACKED_ENTITY_ATTRIBUTE, ExpressionType.VARIABLE, ExpressionType.DATA_ELEMENT );

    // -------------------------------------------------------------------------
    // Singleton instance
    // -------------------------------------------------------------------------

    public static final ProgramStageNotificationMessageRenderer INSTANCE = new ProgramStageNotificationMessageRenderer();

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    @Override
    protected ImmutableMap<TemplateVariable, Function<Event, String>> getVariableResolvers()
    {
        return VARIABLE_RESOLVERS;
    }

    @Override
    protected Map<String, String> resolveTrackedEntityAttributeValues( Set<String> attributeKeys, Event entity )
    {
        if ( attributeKeys.isEmpty() )
        {
            return Maps.newHashMap();
        }

        return entity.getProgramInstance().getEntityInstance().getTrackedEntityAttributeValues().stream()
            .filter( av -> attributeKeys.contains( av.getAttribute().getUid() ) )
            .collect( Collectors.toMap( av -> av.getAttribute().getUid(),
                ProgramStageNotificationMessageRenderer::filterValue ) );
    }

    @Override
    protected Map<String, String> resolveDataElementValues( Set<String> elementKeys, Event entity )
    {
        if ( elementKeys.isEmpty() )
        {
            return Maps.newHashMap();
        }

        Map<String, DataElement> dataElementsMap = new HashMap<>();
        entity.getProgramStage().getDataElements().forEach( de -> dataElementsMap.put( de.getUid(), de ) );

        return entity.getEventDataValues().stream()
            .filter( dv -> elementKeys.contains( dv.getDataElement() ) )
            .collect( Collectors.toMap( EventDataValue::getDataElement,
                dv -> filterValue( dv, dataElementsMap.get( dv.getDataElement() ) ) ) );
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

        // If the AV has an OptionSet -> substitute value with the name of the
        // Option
        if ( av.getAttribute().hasOptionSet() )
        {
            value = av.getAttribute().getOptionSet().getOptionByCode( value ).getName();
        }

        return value != null ? value : MISSING_VALUE_REPLACEMENT;
    }

    private static String filterValue( EventDataValue dv, DataElement dataElement )
    {
        String value = dv.getValue();

        if ( value == null )
        {
            return CONFIDENTIAL_VALUE_REPLACEMENT;
        }

        // If the DV has an OptionSet -> substitute value with the name of the
        // Option
        if ( dataElement != null && dataElement.hasOptionSet() )
        {
            value = dataElement.getOptionSet().getOptionByCode( value ).getName();
        }

        return value != null ? value : MISSING_VALUE_REPLACEMENT;
    }
}
