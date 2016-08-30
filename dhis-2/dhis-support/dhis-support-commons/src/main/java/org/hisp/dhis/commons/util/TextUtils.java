package org.hisp.dhis.commons.util;

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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class with methods for managing strings.
 * 
 * @author Lars Helge Overland
 */
public class TextUtils
{
    public static final TextUtils INSTANCE = new TextUtils();
    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String SEP = "-";
    public static final String LN = System.getProperty( "line.separator" );
    public static final String SEMICOLON = ";";
    
    private static final Pattern LINK_PATTERN = Pattern.compile( "((http://|https://|www\\.).+?)($|\\n|\\r|\\r\\n| )" );
    private static final String DELIMITER = ", ";
    
    /**
     * Performs the htmlNewline(String) and htmlLinks(String) methods against
     * the given text.
     * 
     * @param text the text to substitute.
     * @return the substituted text.
     */
    public static String htmlify( String text )
    {
        text = htmlLinks( text );
        text = htmlNewline( text );
        return text;
    }
        
    /**
     * Substitutes links in the given text with valid HTML mark-up. For instance, 
     * http://dhis2.org is replaced with <a href="http://dhis2.org">http://dhis2.org</a>,
     * and www.dhis2.org is replaced with <a href="http://dhis2.org">www.dhis2.org</a>.
     * 
     * @param text the text to substitute links for.
     * @return the substituted text.
     */
    public static String htmlLinks( String text )
    {
        if ( text == null || text.trim().isEmpty() )
        {
            return null;
        }
        
        Matcher matcher = LINK_PATTERN.matcher( text );

        StringBuffer buffer = new StringBuffer();
        
        while ( matcher.find() )
        {
            String url = matcher.group( 1 );            
            String suffix = matcher.group( 3 );
            
            String ref = url.startsWith( "www." ) ? "http://" + url : url;

            url = "<a href=\"" + ref + "\">" + url + "</a>" + suffix;
            
            matcher.appendReplacement( buffer, url );
        }
        
        return matcher.appendTail( buffer ).toString();
    }

    /**
     * Replaces common newline characters like \n, \r, \r\n to the HTML line
     * break tag br.
     * 
     * @param text the text to substitute.
     * @return the substituted text.
     */
    public static String htmlNewline( String text )
    {
        if ( text == null || text.trim().isEmpty() )
        {
            return null;
        }
        
        return text.replaceAll( "(\n|\r|\r\n)", "<br>" );
    }
    
    /**
     * Returns a list of tokens based on the given string.
     * 
     * @param string the string.
     * @return the list of tokens.
     */
    public static List<String> getTokens( String string )
    {
        if ( string == null )
        {
            return null;
        }
        
        return new ArrayList<>( Arrays.asList( string.split( "\\s" ) ) );
    }
    
    /**
     * Gets the sub string of the given string. If the beginIndex is larger than
     * the length of the string, the empty string is returned. If the beginIndex +
     * the length is larger than the length of the string, the part of the string
     * following the beginIndex is returned. Method is out-of-range safe.
     * 
     * @param string the string.
     * @param beginIndex the zero-based begin index.
     * @param length the length of the sub string starting at the begin index.
     * @return the sub string of the given string.
     */
    public static String subString( String string, int beginIndex, int length )
    {
        if ( string == null )
        {
            return null;
        }
        
        final int endIndex = beginIndex + length;
        
        if ( beginIndex >= string.length()  )
        {
            return EMPTY;
        }
        
        if ( endIndex > string.length() )
        {
            return string.substring( beginIndex, string.length() );
        }
        
        return string.substring( beginIndex, endIndex );
    }
    
    /**
     * Removes the last given number of characters from the given string. Returns
     * null if the string is null. Returns an empty string if characters is less
     * than zero or greater than the length of the string.
     * 
     * @param string the string.
     * @param characters number of characters to remove.
     * @return the substring.
     */
    public static String removeLast( String string, int characters )
    {
        if ( string == null )
        {
            return null;
        }
        
        if ( characters < 0 || characters > string.length() )
        {
            return EMPTY;
        }
        
        return string.substring( 0, string.length() - characters );
    }
    
    /**
     * Removes the last occurrence of the word "or" from the given string,
     * including potential trailing spaces, case-insensitive.
     * 
     * @param string the string.
     * @return the chopped string.
     */
    public static String removeLastOr( String string )
    {
        string = StringUtils.stripEnd( string, " " );
        
        return StringUtils.removeEndIgnoreCase( string, "or" );
    }

    /**
     * Removes the last occurrence of the word "and" from the given string,
     * including potential trailing spaces, case-insensitive.
     * 
     * @param string the string.
     * @return the chopped string.
     */
    public static String removeLastAnd( String string )
    {
        string = StringUtils.stripEnd( string, " " );
        
        return StringUtils.removeEndIgnoreCase( string, "and" );
    }

    /**
     * Removes the last occurrence of comma (",") from the given string,
     * including potential trailing spaces.
     * 
     * @param string the string.
     * @return the chopped string.
     */
    public static String removeLastComma( String string )
    {
        string = StringUtils.stripEnd( string, " " );
        
        return StringUtils.removeEndIgnoreCase( string, "," );
    }

    /**
     * Removes the last occurrence of the the given string, including potential 
     * trailing spaces.
     * 
     * @param string the string.
     * @param remove the text to remove.
     * @return the chopped string.
     */
    public static String removeLast( String string, String remove )
    {
        string = StringUtils.stripEnd( string, " " );
        
        return StringUtils.removeEndIgnoreCase( string,  remove );
    }
    
    /**
     * Removes line breaks form the given string.
     * 
     * @param string the string.
     * @return the chopped string.
     */
    public static String removeNewlines( String string )
    {
        return string.replaceAll( "\r", EMPTY ).replaceAll( "\n", EMPTY );
    }
    
    /**
     * Trims the given string from the end.
     * 
     * @param value the value to trim.
     * @param length the number of characters to trim.
     * @return the trimmed value, empty if given value is null or length is higher
     *         than the value length.
     */
    public static String trimEnd( String value, int length )
    {
        if ( value == null || length > value.length() )
        {
            return EMPTY;
        }
        
        return value.substring( 0, value.length() - length );
    }
    
    /**
     * Returns an empty string if the given argument is true, the string 
     * otherwise. This is a convenience method.
     * 
     * @param string the string.
     * @param emptyString whether to return an empty string.
     * @return a string.
     */
    public static String getString( String string, boolean emptyString )
    {
        return emptyString ? EMPTY : string;
    }

    /**
     * Joins the elements of the provided array into a single String containing 
     * the provided list of elements.
     * 
     * @param <T> type.
     * @param list the list of objects to join.
     * @param separator the separator string.
     * @param nullReplacement the value to replace nulls in list with.
     * @return the joined string.
     */
    public static <T> String join( List<T> list, String separator, T nullReplacement )
    {
        if ( list == null )
        {
            return null;
        }
        
        List<T> objects = new ArrayList<>( list );
        
        if ( nullReplacement != null )
        {
            Collections.replaceAll( objects, null, nullReplacement );
        }
        
        return StringUtils.join( objects, separator );
    }
    
    /**
     * Transforms a collection of Integers into a comma delimited String. If the
     * given collection of elements are null or is empty, an empty String is
     * returned.
     * 
     * @param elements the collection of Integers
     * @return a comma delimited String.
     */
    public static String getCommaDelimitedString( Collection<?> elements )
    {
        final StringBuilder builder = new StringBuilder();
        
        if ( elements != null && !elements.isEmpty() )
        {
            for ( Object element : elements )
            {
                builder.append( element.toString() ).append( DELIMITER );
            }
            
            return builder.substring( 0, builder.length() - DELIMITER.length() );
        }
        
        return builder.toString();
    }
    
    /**
     * Transforms a collection of Integers into a comma delimited String. If the
     * given collection of elements are null or is empty, an empty String is
     * returned.
     * 
     * @param delimitPrefix whether to prefix the string with a delimiter.
     * @param delimitSuffix whether to suffix the string with a delimiter.
     * @param elements the collection of Integers
     * @return a comma delimited String.
     */
    public static String getCommaDelimitedString( Collection<?> elements, boolean delimitPrefix, boolean delimitSuffix )
    {
        final StringBuilder builder = new StringBuilder();
        
        if ( elements != null && !elements.isEmpty() )
        {
            if ( delimitPrefix )
            {
                builder.append( DELIMITER );
            }
            
            builder.append( getCommaDelimitedString( elements ) );
            
            if ( delimitSuffix )
            {
                builder.append( DELIMITER );
            }
        }
        
        return builder.toString();
    }

    /**
     * Transforms a collection of strings into a comma delimited string, where
     * each component get single-quoted.
     * 
     * @param elements the collection of Integers
     * @return a comma delimited String.
     */
    public static String getQuotedCommaDelimitedString( Collection<String> elements )
    {
        if ( elements != null && elements.size() > 0 )
        {
            final StringBuffer buffer = new StringBuffer();        
        
            for ( String element : elements )
            {
                buffer.append( "'" ).append( element.toString() ).append( "', " );
            }
            
            return buffer.substring( 0, buffer.length() - ", ".length() );
        }
        
        return null;
    }
        
    /**
     * Checks the two strings for equality.
     * 
     * @param s1 string 1.
     * @param s2 string 2.
     * @return true if strings are equal, false otherwise.
     */
    public static boolean equalsNullSafe( String s1, String s2 )
    {
        return s1 == null ? s2 == null : s1.equals( s2 );
    }
    
    /**
     * Returns the string value of the given boolean. Returns null if argument
     * is null.
     * 
     * @param value the boolean.
     * @return the string value.
     */
    public static String valueOf( Boolean value )
    {
        return value != null ? String.valueOf( value ) : null;
    }
    
    /**
     * Returns the boolean value of the given string. Returns null if argument
     * is null.
     * 
     * @param value the string value.
     * @return the boolean.
     */
    public static Boolean valueOf( String value )
    {
        return value != null ? Boolean.valueOf( value ) : null;
    }
    
    /**
     * Null-safe method for converting the given string to lower-case.
     * 
     * @param string the string.
     * @return the string in lower-case.
     */
    public static String lower( String string )
    {
        return string != null ? string.toLowerCase() : null;
    }
    
    /**
     * Null-safe method for writing the items of a string array out as a string
     * separated by the given char separator.
     * 
     * @param array the array.
     * @param separator the separator of the array items.
     * @return a string.
     */
    public static String toString( String[] array, String separator )
    {
        StringBuilder builder = new StringBuilder();
        
        if ( array != null && array.length > 0 )
        {
            for ( String string : array )
            {
                builder.append( string ).append( separator );
            }
            
            builder.deleteCharAt( builder.length() - 1 );
        }
        
        return builder.toString();
    }
    
    /**
     * Returns the string representation of the object, or null if the object is
     * null.
     * 
     * @param object the object.
     * @return the string representation.
     */
    public static String toString( Object object )
    {
        return object != null ? object.toString() : null;
    }

    /**
     * Returns the empty string if the given string is equal to the given test,
     * the string if not.
     * 
     * @param string the string.
     * @param test the test to check the string for equality.
     * @return a string.
     */
    public static String emptyIfEqual( String string, String test )
    {
        return test != null && test.equals( string ) ? EMPTY : string;
    }
    
    /**
     * Invokes append tail on matcher with the given string buffer, and returns
     * the string buffer as a string.
     * 
     * @param matcher the matcher.
     * @param sb the string buffer.
     * @return a string.
     */
    public static String appendTail( Matcher matcher, StringBuffer sb )
    {
        matcher.appendTail( sb );
        return sb.toString();
    }
    
    /**
     * Gets the string at the given index of the array produced by splitting
     * the given string on the given separator. Returns null if the given string
     * is null or if the given index is out of bounds of the array.
     * 
     * @param string the string to split.
     * @param separator the character to split on.
     * @param index the index of the string in the resulting array to return.
     * @return a string.
     */
    public static String splitSafe( String string, String separator, int index )
    {
        if ( string == null )
        {
            return null;
        }
        
        String[] split = string.split( separator );
        
        if ( index >= 0 && split.length > index && split[index] != null )
        {
            return String.valueOf( split[index] );
        }
        
        return null;
    }
        
    /**
     * Indicates whether the given string contains any of the given search
     * strings. The operation ignores case and leading and trailing blanks.
     * 
     * @param string the string to check, can be null.
     * @param searchStrings the strings to check against.
     * @return true or false.
     */
    public static boolean containsAnyIgnoreCase( String string, Collection<String> searchStrings )
    {
        if ( string == null || searchStrings == null )
        {
            return false;
        }
        
        for ( String searchString : searchStrings )
        {
            if ( string.trim().toLowerCase().contains( searchString.trim().toLowerCase() ) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Splits the given string value into independent values using a given
     * separator.
     *
     * @param value the string to be splitted.
     * @param separator for splitting value 
     * @return the list of independent values.
     */
    public static Set<String> splitToArray( String value, String separator )
    {
        if ( value == null || value.isEmpty() )
        {
            return null;
        }

        String[] values = value.split( separator );

        return new HashSet<>( Arrays.asList( values ) );
    }
}
