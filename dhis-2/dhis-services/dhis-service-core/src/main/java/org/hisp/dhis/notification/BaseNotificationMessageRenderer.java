package org.hisp.dhis.notification;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.RegexUtils;
import org.hisp.dhis.program.message.DeliveryChannel;
import org.hisp.dhis.system.util.DateUtils;
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
 * @author Halvdan Hoem Grelland
 */
public abstract class BaseNotificationMessageRenderer<T>
    implements NotificationMessageRenderer<T>
{
    private static Log log = LogFactory.getLog( BaseNotificationMessageRenderer.class );

    protected static final int SMS_CHAR_LIMIT = 160 * 4;  // Four concatenated SMS messages
    protected static final int EMAIL_CHAR_LIMIT = 10000;  // Somewhat arbitrarily chosen limits
    protected static final int SUBJECT_CHAR_LIMIT = 100;

    protected static final String CONFIDENTIAL_VALUE_REPLACEMENT = "[CONFIDENTIAL]"; // TODO reconsider this...
    protected static final String MISSING_VALUE_REPLACEMENT = "[N/A]";

    private static final Pattern VAR_CONTENT_PATTERN = Pattern.compile( "^[A-Za-z0-9_]+$" );
    private static final Pattern ATTR_CONTENT_PATTERN = Pattern.compile( "[A-Za-z][A-Za-z0-9]{10}" );

    private static final Pattern VARIABLE_PATTERN  = Pattern.compile( "V\\{([a-z_]*)}" ); // Matches the variable in group 1
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile( "A\\{([A-Za-z][A-Za-z0-9]{10})}" ); // Matches the uid in group 1

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    public NotificationMessage render( T entity, NotificationTemplate template )
    {
        String collatedTemplate = template.getSubjectTemplate() + " " + template.getMessageTemplate();

        Set<String> variables = extractVariables( collatedTemplate );
        Set<String> attributes = extractAttributes( collatedTemplate );

        Map<String, String> varToValueMap = resolveVariableValues( variables, entity );
        Map<String, String> attributeToValueMap = resolveAttributeValues( attributes, entity );

        return createNotificationMessage( template, varToValueMap, attributeToValueMap );
    }

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    protected abstract TemplateVariable fromVariableName( String name );

    protected abstract ImmutableMap<TemplateVariable, Function<T, String>> getVariableResolvers();

    protected abstract Map<String, String> resolveAttributeValues( Set<String> attributeKeys, T entity );

    protected abstract boolean isValidVariableName( String variableName );

    // -------------------------------------------------------------------------
    // Internal methods
    // -------------------------------------------------------------------------

    private Map<String, String> resolveVariableValues( Set<String> variables, T entity )
    {
        return variables.stream()
            .collect( Collectors.toMap(
                v -> v,
                v -> getVariableResolvers().get( fromVariableName( v ) ).apply( entity )
            ) );
    }

    private NotificationMessage createNotificationMessage(
        NotificationTemplate template, Map<String, String> variableToValueMap, Map<String, String> attributeToValueMap )
    {
        String subject = replaceExpressions( template.getSubjectTemplate(), variableToValueMap, attributeToValueMap);
        subject = chop( subject, SUBJECT_CHAR_LIMIT );

        boolean hasSmsRecipients = template.getDeliveryChannels().contains( DeliveryChannel.SMS );

        String message = replaceExpressions( template.getMessageTemplate(), variableToValueMap, attributeToValueMap );
        message = chop( message, hasSmsRecipients ? SMS_CHAR_LIMIT : EMAIL_CHAR_LIMIT );

        return new NotificationMessage( subject, message );
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

    private static Set<String> extractAttributes( String templateString )
    {
        return RegexUtils.getMatches( ATTRIBUTE_PATTERN, templateString, 1 );
    }

    private Set<String> extractVariables( String templateString )
    {
        Map<Boolean, Set<String>> groupedVariables = RegexUtils.getMatches( VARIABLE_PATTERN, templateString, 1 )
            .stream().collect( Collectors.groupingBy( this::isValidVariableName, Collectors.toSet() ) );

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

    // -------------------------------------------------------------------------
    // Util methods
    // -------------------------------------------------------------------------

    // Simple limiter. No space wasted on ellipsis etc.
    protected static String chop( String input, int limit )
    {
        return input.substring( 0, Math.min( input.length(), limit ) );
    }

    protected static String daysUntil( Date date )
    {
        return String.valueOf( Days.daysBetween( DateTime.now(), new DateTime( date ) ).getDays() );
    }

    protected static String daysSince( Date date )
    {
        return String.valueOf( Days.daysBetween( new DateTime( date ) , DateTime.now() ).getDays() );
    }

    protected static String formatDate( Date date )
    {
        return DateUtils.getMediumDateString( date );
    }
}
