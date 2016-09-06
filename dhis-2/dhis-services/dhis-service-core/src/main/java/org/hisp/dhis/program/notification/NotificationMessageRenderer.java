package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import com.google.common.collect.Maps;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.RegexUtils;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Halvdan Hoem Grelland
 *
 *
 * Template formats supported:
 *
 * TrackedEntityInstanceAttributeValues are defined as:
 *  A{uid-of-attribute}
 *
 * They are resolved from the TrackedEntityInstance of the ProgramStageInstance
 * or ProgramInstance (depending on context).
 *
 * There is also a set list of supported variable which are expressed as:
 *  V{name-of-variable}
 *
 * V{uid} A{some_name}
 *
 * The supported variables are:
 *
 * <ul>
 *     <li>program_name</li>
 *     <li>program_stage_name</li>
 *     <li>org_unit_name</li>
 *     <li>due_date</li>
 *     <li>days_since_due_date</li>
 *     <li>days_until_due_date</li>
 * </ul>
 *
 */
public class NotificationMessageRenderer
{
    private static final Log log = LogFactory.getLog( NotificationMessageRenderer.class );

    private static final int SMS_CHAR_LIMIT = 160 * 4;  // Four concatenated SMS messages
    private static final int EMAIL_CHAR_LIMIT = 10000;  // Somewhat arbitrarily chosen limits
    private static final int SUBJECT_CHAR_LIMIT = 60;   //

    private static final String CONFIDENTIAL_VALUE_REPLACEMENT = "[CONFIDENTIAL]"; // TODO reconsider this...

    private static final Pattern VARIABLE_PATTERN = Pattern.compile( "V\\{([a-z_]*)\\}" ); // Matches the variable in group 1
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile( "A\\{([A-Za-z][A-Za-z0-9]{10})}" ); // Matches the uid in group 1

    private static final String VAR_PROGRAM_NAME = "program_name";
    private static final String VAR_PROGRAM_STAGE_NAME = "program_stage_name";
    private static final String VAR_ORG_UNIT_NAME = "org_unit_name";
    private static final String VAR_DUE_DATE = "due_date";
    private static final String VAR_DAYS_SINCE_DUE_DATE = "days_since_due_date";
    private static final String VAR_DAYS_UNTIL_DUE_DATE = "days_until_due_date";

    /**
     * Maps the variable names to resolver functions.
     */
    private static final ImmutableMap<String, Function<ProgramStageInstance, String>> VARIABLE_MAP
        = new ImmutableMap.Builder<String, Function<ProgramStageInstance, String>>()
            .put( VAR_PROGRAM_NAME,         psi -> psi.getProgramStage().getProgram().getDisplayName() )
            .put( VAR_PROGRAM_STAGE_NAME,   psi -> psi.getProgramStage().getDisplayName() )
            .put( VAR_ORG_UNIT_NAME,        psi -> psi.getOrganisationUnit().getDisplayName() )
            .put( VAR_DUE_DATE,             psi -> DateUtils.getMediumDateString( psi.getDueDate() ) )
            .put( VAR_DAYS_SINCE_DUE_DATE,  psi -> String.valueOf( Days.daysBetween( new DateTime( psi.getDueDate() ), DateTime.now() ).getDays() ) )
            .put( VAR_DAYS_UNTIL_DUE_DATE,  psi -> String.valueOf( Days.daysBetween( DateTime.now(), new DateTime( psi.getDueDate() ) ).getDays() ) )
            .build();

    // -------------------------------------------------------------------------
    // Private constructor
    // -------------------------------------------------------------------------

    private NotificationMessageRenderer() {}

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    public static NotificationMessage render( ProgramStageInstance psi, ProgramNotificationTemplate template )
    {
        boolean hasSmsRecipients = template.getDeliveryChannels().contains( DeliveryChannel.SMS );

        return internalRender( psi, template, SUBJECT_CHAR_LIMIT, hasSmsRecipients ? SMS_CHAR_LIMIT : EMAIL_CHAR_LIMIT);
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private static NotificationMessage internalRender(
        ProgramStageInstance programStageInstance, ProgramNotificationTemplate template, int subjectCharLimit, int messageCharLimit )
    {
        String collatedTemplate = template.getSubjectTemplate() + " " + template.getSubjectTemplate();

        Set<String> allVariables = extractVariables( collatedTemplate );
        Set<String> allAttributes = extractAttributes( collatedTemplate );

        Map<String, String> variableToValueMap = resolveVariableValues( allVariables, programStageInstance );
        Map<String, String> teiAttributeValues = resolveTeiAttributeValues( allAttributes, programStageInstance );

        return createNotificationMessage( template, variableToValueMap, teiAttributeValues, subjectCharLimit, messageCharLimit );
    }

    private static NotificationMessage createNotificationMessage( ProgramNotificationTemplate template, Map<String, String> variableToValueMap,
        Map<String, String> teiAttributeValueMap, int subjectCharLimit, int messageCharLimit )
    {
        String subject = replaceExpressions( template.getSubjectTemplate(), variableToValueMap, teiAttributeValueMap );
        subject = chopLength( subject, subjectCharLimit );

        String message = replaceExpressions( template.getMessageTemplate(), variableToValueMap, teiAttributeValueMap );
        message = chopLength( message, messageCharLimit );

        return new NotificationMessage( subject, message );
    }

    private static String replaceExpressions( String input, Map<String, String> variableMap, Map<String, String> teiAttributeValueMap )
    {
        String output = replaceWithValues( input, VARIABLE_PATTERN, variableMap );
        output = replaceWithValues( output, ATTRIBUTE_PATTERN, teiAttributeValueMap );

        return output;
    }

    private static String replaceWithValues( String input, Pattern pattern, Map<String, String> identifierToValueMap )
    {
        Matcher matcher = pattern.matcher( input );

        StringBuffer sb = new StringBuffer( input.length() );

        while ( matcher.find() )
        {
            String uid = matcher.group( 1 );
            String value = identifierToValueMap.get( uid );
            matcher.appendReplacement( sb, value );
        }

        matcher.appendTail( sb );

        return matcher.toString();
    }

    private static Set<String> extractVariables( String input )
    {
        Map<Boolean, Set<String>> groupedVariables = RegexUtils.getMatches( VARIABLE_PATTERN, input, 1 )
            .stream().collect( Collectors.groupingBy( VARIABLE_MAP.keySet()::contains, Collectors.toSet() ) );

        if ( !groupedVariables.get( false ).isEmpty() )
        {
            Set<String> unrecognizedVariables = groupedVariables.get( false );

            log.warn( String.format( "%d unrecognized variable expressions were ignored: %s" , unrecognizedVariables.size(),
                Arrays.toString( unrecognizedVariables.toArray() ) ) );
        }

        return groupedVariables.get( true );
    }

    private static Set<String> extractAttributes( String input )
    {
        return RegexUtils.getMatches( ATTRIBUTE_PATTERN, input, 1 );
    }

    private static Map<String, String> resolveVariableValues( Set<String> variables, ProgramStageInstance programStageInstance )
    {
        return variables.stream().collect( Collectors.toMap( v -> v, v -> VARIABLE_MAP.get( v ).apply( programStageInstance ) ) );
    }

    private static Map<String, String> resolveTeiAttributeValues( Set<String> attributeUids, ProgramStageInstance programStageInstance )
    {
        if ( attributeUids.isEmpty() )
        {
            return Maps.newHashMap();
        }

        return programStageInstance
            .getProgramInstance().getEntityInstance().getTrackedEntityAttributeValues().stream()
            .filter( av -> attributeUids.contains( av.getAttribute().getUid() ) )
            .collect( Collectors.toMap( av -> av.getAttribute().getUid(), NotificationMessageRenderer::plainOrConfidential ) );
    }

    private static String plainOrConfidential( TrackedEntityAttributeValue av )
    {
        String plainValue = av.getPlainValue();

        return plainValue != null ? plainValue : CONFIDENTIAL_VALUE_REPLACEMENT;
    }

    private static String chopLength( String input, int limit )
    {
        return input.substring( 0, Math.min( input.length(), limit ) );
    }
}
