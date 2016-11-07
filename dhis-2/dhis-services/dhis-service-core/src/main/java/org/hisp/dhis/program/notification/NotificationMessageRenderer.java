package org.hisp.dhis.program.notification;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.RegexUtils;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
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
 * The variable expression names are defined in {@link ProgramStageTemplateVariable}.
 *
 * @author Halvdan Hoem Grelland
 */
public class NotificationMessageRenderer
{
    private static final Log log = LogFactory.getLog( NotificationMessageRenderer.class );

    private static final int SMS_CHAR_LIMIT = 160 * 4;  // Four concatenated SMS messages
    private static final int EMAIL_CHAR_LIMIT = 10000;  // Somewhat arbitrarily chosen limits
    private static final int SUBJECT_CHAR_LIMIT = 100;   //

    private static final String CONFIDENTIAL_VALUE_REPLACEMENT = "[CONFIDENTIAL]"; // TODO reconsider this...
    private static final String MISSING_VALUE_REPLACEMENT = "[N/A]";

    private static final Pattern VARIABLE_PATTERN  = Pattern.compile( "V\\{([a-z_]*)\\}" ); // Matches the variable in group 1
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile( "A\\{([A-Za-z][A-Za-z0-9]{10})}" ); // Matches the uid in group 1

    /**
     * Maps ProgramStageInstance variable names to resolver functions.
     */
    private static final ImmutableMap<TemplateVariable, Function<ProgramStageInstance, String>> EVENT_VARIABLE_RESOLVERS
        = new ImmutableMap.Builder<TemplateVariable, Function<ProgramStageInstance, String>>()
            .put( ProgramStageTemplateVariable.PROGRAM_NAME,         psi -> psi.getProgramStage().getProgram().getDisplayName() )
            .put( ProgramStageTemplateVariable.PROGRAM_STAGE_NAME,   psi -> psi.getProgramStage().getDisplayName() )
            .put( ProgramStageTemplateVariable.ORG_UNIT_NAME,        psi -> psi.getOrganisationUnit().getDisplayName() )
            .put( ProgramStageTemplateVariable.DUE_DATE,             psi -> DateUtils.getMediumDateString( psi.getDueDate() ) ) // TODO Figure out formatting to use for Date
            .put( ProgramStageTemplateVariable.DAYS_SINCE_DUE_DATE,  psi -> daysSinceDue( psi ) )
            .put( ProgramStageTemplateVariable.DAYS_UNTIL_DUE_DATE,  psi -> daysUntilDue( psi ) )
            .put( ProgramStageTemplateVariable.CURRENT_DATE,         psi -> DateUtils.getMediumDateString( new Date() ) )
            .build();

    /**
     * Maps ProgramInstance variable names to resolver functions.
     */
    private static final ImmutableMap<TemplateVariable, Function<ProgramInstance, String>> ENROLLMENT_VARIABLE_RESOLVERS
        = new ImmutableMap.Builder<TemplateVariable, Function<ProgramInstance, String>>()
            .put( ProgramTemplateVariable.PROGRAM_NAME,     ps -> ps.getProgram().getDisplayName() )
            .put( ProgramTemplateVariable.ORG_UNIT_NAME,    ps -> ps.getOrganisationUnit().getDisplayName() )
            .put( ProgramTemplateVariable.CURRENT_DATE,     ps -> DateUtils.getMediumDateString( new Date() ) )
            .build();

    // -------------------------------------------------------------------------
    // Private constructor
    // -------------------------------------------------------------------------

    private NotificationMessageRenderer() {}

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    public static NotificationMessage render( ProgramStageInstance programStageInstance, ProgramNotificationTemplate template )
    {
        String collatedTemplate = template.getSubjectTemplate() + " " + template.getMessageTemplate();

        Set<String> variables = extractProgramStageVariables( collatedTemplate );
        Set<String> attributes = extractTeiAttributes( collatedTemplate );

        TrackedEntityInstance tei = programStageInstance.getProgramInstance().getEntityInstance();

        Map<String, String> variableToValueMap = resolveVariableValues( variables, programStageInstance );
        Map<String, String> teiAttributeValues = resolveTeiAttributeValues( attributes, tei );

        return createNotificationMessage( template, variableToValueMap, teiAttributeValues );
    }

    public static NotificationMessage render( ProgramInstance programInstance, ProgramNotificationTemplate template )
    {
        String collatedTemplate = template.getSubjectTemplate() + " " + template.getMessageTemplate();

        Set<String> variables = extractProgramVariables( collatedTemplate );
        Set<String> attributes = extractTeiAttributes( collatedTemplate );

        TrackedEntityInstance tei = programInstance.getEntityInstance();

        Map<String, String> variableToValueMap = resolveVariableValues( variables, programInstance );
        Map<String, String> teiAttributeValues = resolveTeiAttributeValues( attributes, tei );

        return createNotificationMessage( template, variableToValueMap, teiAttributeValues );
    }

    // -------------------------------------------------------------------------
    // Internal methods
    // -------------------------------------------------------------------------

    private static String replaceWithValues( String input, Pattern pattern, Map<String, String> identifierToValueMap )
    {
        Matcher matcher = pattern.matcher( input );

        StringBuffer sb = new StringBuffer( input.length() );

        while ( matcher.find() )
        {
            String uid = matcher.group( 1 );

            String value = identifierToValueMap.getOrDefault( uid, MISSING_VALUE_REPLACEMENT );

            matcher.appendReplacement( sb, value );
        }

        matcher.appendTail( sb );

        return sb.toString();
    }

    private static Set<String> extractProgramStageVariables( String input )
    {
        Map<Boolean, Set<String>> groupedVariables = RegexUtils.getMatches( VARIABLE_PATTERN, input, 1 ).stream()
            .collect( Collectors.groupingBy( ProgramStageTemplateVariable::isValidVariableName, Collectors.toSet() ) );

        warnOfUnrecognizedVariables( groupedVariables.get( false ) );

        Set<String> variables = groupedVariables.get( true );

        return variables != null ? variables : Sets.newHashSet();
    }

    private static Set<String> extractProgramVariables( String input )
    {
        Map<Boolean, Set<String>> groupedVariables = RegexUtils.getMatches( VARIABLE_PATTERN, input, 1 ).stream()
            .collect( Collectors.groupingBy( ProgramTemplateVariable::isValidVariableName, Collectors.toSet() ) );

        warnOfUnrecognizedVariables( groupedVariables.get( false ) );

        Set<String> variables = groupedVariables.get( true );

        return variables != null ? variables : Sets.newHashSet();
    }

    private static void warnOfUnrecognizedVariables( Set<String> unrecognizedVariables )
    {
        if ( unrecognizedVariables != null && !unrecognizedVariables.isEmpty() )
        {
            log.warn( String.format( "%d unrecognized variable expressions were ignored: %s" ,
                unrecognizedVariables.size(), Arrays.toString( unrecognizedVariables.toArray() ) ) );
        }
    }

    private static Set<String> extractTeiAttributes( String input )
    {
        return RegexUtils.getMatches( ATTRIBUTE_PATTERN, input, 1 );
    }

    private static Map<String, String> resolveVariableValues( Set<String> variables, ProgramStageInstance programStageInstance )
    {
        return variables.stream()
            .collect( Collectors.toMap(
                v -> v,
                v -> EVENT_VARIABLE_RESOLVERS.get( ProgramStageTemplateVariable.fromVariableName( v ) ).apply( programStageInstance ) )
            );
    }

    private static Map<String, String> resolveVariableValues( Set<String> variables, ProgramInstance programInstance )
    {
        return variables.stream()
            .collect( Collectors.toMap(
                v -> v,
                v -> ENROLLMENT_VARIABLE_RESOLVERS.get( ProgramTemplateVariable.fromVariableName( v ) ).apply( programInstance ) )
            );
    }

    private static Map<String, String> resolveTeiAttributeValues( Set<String> attributeUids, TrackedEntityInstance tei )
    {
        if ( attributeUids.isEmpty() )
        {
            return Maps.newHashMap();
        }

        return tei.getTrackedEntityAttributeValues().stream()
            .filter( av -> attributeUids.contains( av.getAttribute().getUid() ) )
            .collect( Collectors.toMap( av -> av.getAttribute().getUid(), NotificationMessageRenderer::value ) );
    }

    private static String replaceExpressions( String input, Map<String, String> variableMap, Map<String, String> teiAttributeValueMap )
    {
        if ( StringUtils.isEmpty( input ) )
        {
            return "";
        }

        String substitutedVariables = replaceWithValues( input, VARIABLE_PATTERN, variableMap );
        String substitutedAttributes = replaceWithValues( substitutedVariables, ATTRIBUTE_PATTERN, teiAttributeValueMap );

        return substitutedAttributes;
    }

    private static NotificationMessage createNotificationMessage( ProgramNotificationTemplate template, Map<String, String> variableToValueMap,
        Map<String, String> teiAttributeValueMap )
    {
        String subject = replaceExpressions( template.getSubjectTemplate(), variableToValueMap, teiAttributeValueMap );
        subject = chop( subject, SUBJECT_CHAR_LIMIT );

        boolean hasSmsRecipients = template.getDeliveryChannels().contains( DeliveryChannel.SMS );

        String message = replaceExpressions( template.getMessageTemplate(), variableToValueMap, teiAttributeValueMap );
        message = chop( message, hasSmsRecipients ? SMS_CHAR_LIMIT : EMAIL_CHAR_LIMIT );

        return new NotificationMessage( subject, message );
    }

    private static String daysUntilDue( ProgramStageInstance psi )
    {
        return String.valueOf( Days.daysBetween( DateTime.now(), new DateTime( psi.getDueDate() ) ).getDays() );
    }

    private static String daysSinceDue( ProgramStageInstance psi )
    {
        return String.valueOf( Days.daysBetween( new DateTime( psi.getDueDate() ), DateTime.now() ).getDays() );
    }

    private static String value( TrackedEntityAttributeValue av )
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

    // Simple limiter. No space wasted on ellipsis etc.
    private static String chop( String input, int limit )
    {
        return input.substring( 0, Math.min( input.length(), limit ) );
    }
}
