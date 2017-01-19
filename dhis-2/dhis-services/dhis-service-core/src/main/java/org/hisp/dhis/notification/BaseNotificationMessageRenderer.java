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
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.RegexUtils;
import org.hisp.dhis.system.util.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Template formats supported:
 *  A{uid-of-attribute}
 *  V{name-of-variable}
 *
 * The implementing superclass defines how these are resolved.
 *
 * @param <T> the type of the root object used for resolving expression values.
 *
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
    protected static final String VALUE_ON_ERROR = "[SERVER ERROR]";

    protected static final Pattern VAR_CONTENT_PATTERN = Pattern.compile( "^[A-Za-z0-9_]+$" );
    protected static final Pattern ATTR_CONTENT_PATTERN = Pattern.compile( "[A-Za-z][A-Za-z0-9]{10}" );

    private static final Pattern VARIABLE_PATTERN  = Pattern.compile( "V\\{([a-z_]*)}" ); // Matches the variable in group 1
    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile( "A\\{([A-Za-z][A-Za-z0-9]{10})}" ); // Matches the uid in group 1

    private ImmutableMap<ExpressionType, BiFunction<T, Set<String>, Map<String, String>>> EXPRESSION_TO_VALUE_RESOLVERS =
        new ImmutableMap.Builder<ExpressionType, BiFunction<T, Set<String>, Map<String, String>>>()
            .put( ExpressionType.VARIABLE, (entity, keys) -> resolveVariableValues( keys, entity ) )
            .put( ExpressionType.ATTRIBUTE, (entity, keys) -> resolveAttributeValues( keys, entity ) )
            .build();

    protected enum ExpressionType
    {
        VARIABLE ( VARIABLE_PATTERN, VAR_CONTENT_PATTERN ),
        ATTRIBUTE ( ATTRIBUTE_PATTERN, ATTR_CONTENT_PATTERN );

        private final Pattern expressionPattern;
        private final Pattern contentPattern;

        ExpressionType( Pattern expressionPattern, Pattern contentPattern )
        {
            this.expressionPattern = expressionPattern;
            this.contentPattern = contentPattern;
        }

        public Pattern getExpressionPattern()
        {
            return expressionPattern;
        }

        boolean isValidExpressionContent( String content )
        {
            return content != null && contentPattern.matcher( content ).matches();
        }
    }

    // -------------------------------------------------------------------------
    // Public methods
    // -------------------------------------------------------------------------

    public NotificationMessage render( T entity, NotificationTemplate template )
    {
        final String collatedTemplate = template.getSubjectTemplate() + " " + template.getMessageTemplate();

        Map<String, String> expressionToValueMap =
            extractExpressionsByType( collatedTemplate ).entrySet().stream()
                .map( entry -> resolveValuesFromExpressions( entry.getValue(), entry.getKey(), entity ) )
                .collect( HashMap::new, Map::putAll, Map::putAll );

        return createNotificationMessage( template, expressionToValueMap );
    }

    // -------------------------------------------------------------------------
    // Overrideable logic
    // -------------------------------------------------------------------------

    protected boolean isValidExpressionContent( String content, ExpressionType type )
    {
        return content != null && getSupportedExpressionTypes().contains( type ) && type.isValidExpressionContent( content );
    }

    // -------------------------------------------------------------------------
    // Abstract methods
    // -------------------------------------------------------------------------

    /**
     * Gets a Map of variable resolver functions, keyed by the Template Variable.
     * The returned Map should not be mutable.
     */
    protected abstract Map<TemplateVariable, Function<T, String>> getVariableResolvers();

    /**
     * Resolves values for the given attribute UIDs.
     *
     * @param attributeKeys the Set of attribute UIDs.
     * @param entity the entity to resolve the values from/for.
     * @return a Map of values, keyed by the corresponding attribute UID.
     */
    protected abstract Map<String, String> resolveAttributeValues( Set<String> attributeKeys, T entity );

    /**
     * Converts a string to the TemplateVariable supported by the implementor.
     */
    protected abstract TemplateVariable fromVariableName( String name );

    /**
     * Returns the set of ExpressionTypes supported by the implementor.
     */
    protected abstract Set<ExpressionType> getSupportedExpressionTypes();

    // -------------------------------------------------------------------------
    // Internal methods
    // -------------------------------------------------------------------------

    private Map<String, String> resolveValuesFromExpressions( Set<String> expressions, ExpressionType type, T entity )
    {
        return EXPRESSION_TO_VALUE_RESOLVERS.getOrDefault( type, (e, s) -> Maps.newHashMap() ).apply( entity, expressions );
    }

    private Map<String, String> resolveVariableValues( Set<String> variables, T entity )
    {
        return variables.stream()
            .collect( Collectors.toMap(
                v -> v,
                v -> resolveValue( v, entity )
            ) );
    }

    private String resolveValue( String variableName, T entity )
    {
        Function<T, String> resolver = getVariableResolvers().get( fromVariableName( variableName ) );

        if ( resolver == null )
        {
            log.warn( String.format( "Cannot resolve value for expression '%s': no resolver", variableName ) );

            return StringUtils.EMPTY;
        }

        if ( entity == null )
        {
            log.warn( String.format( "Cannot resolve value for expression '%s': entity is null", variableName ) );

            return StringUtils.EMPTY;
        }

        String value;

        try
        {
            value = resolver.apply( entity );
        }
        catch ( Exception ex )
        {
            log.warn( "Caught exception when running value resolver for variable: " + variableName , ex );
            value = VALUE_ON_ERROR;
        }

        return value != null ? value : StringUtils.EMPTY;
    }

    private NotificationMessage createNotificationMessage( NotificationTemplate template, Map<String, String> expressionToValueMap )
    {
        String subject = replaceExpressions( template.getSubjectTemplate(), expressionToValueMap );
        subject = chop( subject, SUBJECT_CHAR_LIMIT );

        boolean hasSmsRecipients = template.getDeliveryChannels().contains( DeliveryChannel.SMS );

        String message = replaceExpressions( template.getMessageTemplate(), expressionToValueMap );
        message = chop( message, hasSmsRecipients ? SMS_CHAR_LIMIT : EMAIL_CHAR_LIMIT );

        return new NotificationMessage( subject, message );
    }

    private static String replaceExpressions( String input, final Map<String, String> expressionToValueMap )
    {
        if ( StringUtils.isEmpty( input ) )
        {
            return StringUtils.EMPTY;
        }

        return Stream.of( ExpressionType.values() )
            .map( ExpressionType::getExpressionPattern )
            .reduce(
                input,
                ( str, pattern ) -> {
                    StringBuffer sb = new StringBuffer( str.length() );
                    Matcher matcher = pattern.matcher( str );

                    while ( matcher.find() )
                    {
                        String key = matcher.group( 1 );
                        String value = expressionToValueMap.getOrDefault( key, MISSING_VALUE_REPLACEMENT );
                        value = StringUtils.defaultIfBlank( value, StringUtils.EMPTY );

                        matcher.appendReplacement( sb, value );
                    }

                    return matcher.appendTail( sb ).toString();
                },
                ( oldStr, newStr ) -> newStr
            );
    }

    private Map<ExpressionType, Set<String>> extractExpressionsByType( String template )
    {
        return Arrays.stream( ExpressionType.values() )
            .collect( Collectors.toMap( Function.identity(), type -> extractExpressions( template, type ) ) );
    }

    private Set<String> extractExpressions( String template, ExpressionType type )
    {
        Map<Boolean, Set<String>> groupedExpressions = RegexUtils.getMatches( type.getExpressionPattern(), template, 1 )
            .stream().collect( Collectors.groupingBy( expr -> isValidExpressionContent( expr, type ), Collectors.toSet() ) );

        warnOfUnrecognizedExpressions( groupedExpressions.get( false ), type );

        Set<String> expressions = groupedExpressions.get( true );

        if ( expressions == null || expressions.isEmpty() )
        {
            return Collections.emptySet();
        }

        return expressions;
    }

    private static void warnOfUnrecognizedExpressions( Set<String> unrecognized, ExpressionType type )
    {
        if ( unrecognized != null && !unrecognized.isEmpty() )
        {
            log.warn( String.format( "%d unrecognized expressions of type %s were ignored: %s",
                unrecognized.size(), type.name(), Arrays.toString( unrecognized.toArray() ) ) );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

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
