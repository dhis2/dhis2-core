package org.hisp.dhis.system.util;

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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.commons.validator.routines.DateValidator;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.datavalue.DataValue;

import java.awt.geom.Point2D;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lars Helge Overland
 */
public class ValidationUtils
{
    private static final String NUM_PAT = "((-?[0-9]+)(\\.[0-9]+)?)";

    private static final Pattern POINT_PATTERN = Pattern.compile( "\\[(.+),\\s?(.+)\\]" );
    private static final Pattern DIGIT_PATTERN = Pattern.compile( ".*\\d.*" );
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile( ".*[A-Z].*" );
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile( "^#?([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$" );
    private static final Pattern TIME_OF_DAY_PATTERN = Pattern.compile( "^([0-9]|0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$" );
    private static final Pattern BBOX_PATTERN = Pattern.compile( "^" + NUM_PAT + ",\\s*?" + NUM_PAT + ",\\s*?" + NUM_PAT + ",\\s*?" + NUM_PAT + "$" );

    private static Set<String> BOOL_FALSE_VARIANTS = Sets.newHashSet( "false", "False", "f", "F", "0" );

    private static Set<String> BOOL_TRUE_VARIANTS = Sets.newHashSet( "true", "True", "t", "T", "1" );

    private static final int VALUE_MAX_LENGTH = 50000;

    private static final int LONG_MAX = 180;

    private static final int LONG_MIN = -180;

    private static final int LAT_MAX = 90;

    private static final int LAT_MIN = -90;

    private static final ImmutableSet<Character> SQL_VALID_CHARS = ImmutableSet.of(
        '&', '|', '=', '!', '<', '>', '/', '%', '"', '\'', '*', '+', '-', '^', ',', '.' );

    public static final ImmutableSet<String> ILLEGAL_SQL_KEYWORDS = ImmutableSet.of( "alter", "before", "case",
        "commit", "copy", "create", "createdb", "createrole", "createuser", "close", "delete", "destroy", "drop",
        "escape", "insert", "select", "rename", "replace", "restore", "return", "update", "when", "write" );

    /**
     * Validates whether a filter expression contains malicious code such as SQL
     * injection attempts.
     *
     * @param filter the filter string.
     * @return true if the filter string is valid, false otherwise.
     */
    public static boolean expressionIsValidSQl( String filter )
    {
        if ( filter == null )
        {
            return true;
        }

        if ( TextUtils.containsAnyIgnoreCase( filter, ILLEGAL_SQL_KEYWORDS ) )
        {
            return false;
        }

        for ( int i = 0; i < filter.length(); i++ )
        {
            char ch = filter.charAt( i );

            if ( !(Character.isWhitespace( ch ) || Character.isLetterOrDigit( ch ) || SQL_VALID_CHARS.contains( ch )) )
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates whether an email string is valid.
     *
     * @param email the email string.
     * @return true if the email string is valid, false otherwise.
     */
    public static boolean emailIsValid( String email )
    {
        return EmailValidator.getInstance().isValid( email );
    }

    /**
     * Validates whether a date string is valid for the given Locale.
     *
     * @param date   the date string.
     * @param locale the Locale
     * @return true if the date string is valid, false otherwise.
     */
    public static boolean dateIsValid( String date, Locale locale )
    {
        return DateValidator.getInstance().isValid( date, locale );
    }

    /**
     * Validates whether a date string is valid for the default Locale.
     *
     * @param date the date string.
     * @return true if the date string is valid, false otherwise.
     */
    public static boolean dateIsValid( String date )
    {
        return dateIsValid( date, null );
    }

    /**
     * Validates whether a string is valid for the HH:mm time format.
     *
     * @param time the time string
     * @return true if the time string is valid, false otherwise
     */
    public static boolean timeIsValid( String time )
    {
        return TIME_OF_DAY_PATTERN.matcher( time ).matches();
    }

    /**
     * Validates whether an URL string is valid.
     *
     * @param url the URL string.
     * @return true if the URL string is valid, false otherwise.
     */
    public static boolean urlIsValid( String url )
    {
        return new UrlValidator().isValid( url );
    }

    /**
     * Validates whether a password is valid. A password must:
     * <p/>
     * <ul>
     * <li>Be between 8 and 80 characters long</li>
     * <li>Include at least one digit</li>
     * <li>Include at least one uppercase letter</li>
     * </ul>
     *
     * @param password the password.
     * @return true if the password is valid, false otherwise.
     */
    public static boolean passwordIsValid( String password )
    {
        if ( password == null || password.trim().length() < 8 || password.trim().length() > 35 )
        {
            return false;
        }

        return DIGIT_PATTERN.matcher( password ).matches() && UPPERCASE_PATTERN.matcher( password ).matches();
    }

    /**
     * Validates whether a coordinate is valid.
     *
     * @return true if the coordinate is valid, false otherwise.
     */
    public static boolean coordinateIsValid( String coordinate )
    {
        if ( coordinate == null || coordinate.trim().isEmpty() )
        {
            return false;
        }

        Matcher matcher = POINT_PATTERN.matcher( coordinate );

        if ( !matcher.find() )
        {
            return false;
        }

        double longitude = 0.0;
        double latitude = 0.0;

        try
        {
            longitude = Double.parseDouble( matcher.group( 1 ) );
            latitude = Double.parseDouble( matcher.group( 2 ) );
        }
        catch ( NumberFormatException ex )
        {
            return false;
        }

        return longitude >= LONG_MIN && longitude <= LONG_MAX && latitude >= LAT_MIN && latitude <= LAT_MAX;
    }

    /**
     * Validates whether a bbox string is valid and on the format:
     * <p>
     * <code>min longitude, min latitude, max longitude, max latitude</code>
     *
     * @param bbox the bbox string.
     * @return true if the bbox string is valid.
     */
    public static boolean bboxIsValid( String bbox )
    {
        if ( bbox == null || bbox.trim().isEmpty() )
        {
            return false;
        }

        Matcher matcher = BBOX_PATTERN.matcher( bbox );

        if ( !matcher.matches() )
        {
            return false;
        }

        double minLng = Double.parseDouble( matcher.group( 1 ) );
        double minLat = Double.parseDouble( matcher.group( 4 ) );
        double maxLng = Double.parseDouble( matcher.group( 7 ) );
        double maxLat = Double.parseDouble( matcher.group( 10 ) );

        if ( minLng < -180d || minLng > 180d || maxLng < -180d || maxLng > 180d )
        {
            return false;
        }

        if ( minLat < -90d || minLat > 90d || maxLat < -90d || maxLat > 90d )
        {
            return false;
        }

        return true;
    }

    /**
     * Returns the longitude from the given coordinate. Returns null if the
     * coordinate string is not valid. The coordinate is on the form
     * longitude / latitude.
     *
     * @param coordinate the coordinate string.
     * @return the longitude.
     */
    public static String getLongitude( String coordinate )
    {
        if ( coordinate == null )
        {
            return null;
        }

        Matcher matcher = POINT_PATTERN.matcher( coordinate );

        return matcher.find() ? matcher.group( 1 ) : null;
    }

    /**
     * Returns the latitude from the given coordinate. Returns null if the
     * coordinate string is not valid. The coordinate is on the form
     * longitude / latitude.
     *
     * @param coordinate the coordinate string.
     * @return the latitude.
     */
    public static String getLatitude( String coordinate )
    {
        if ( coordinate == null )
        {
            return null;
        }

        Matcher matcher = POINT_PATTERN.matcher( coordinate );

        return matcher.find() ? matcher.group( 2 ) : null;
    }

    /**
     * Returns the longitude and latitude from the given coordinate.
     *
     * @param coordinate the coordinate string.
     * @return Point2D of the coordinate.
     */
    public static Point2D getCoordinatePoint2D( String coordinate )
    {
        if ( coordinate == null )
        {
            return null;
        }

        Matcher matcher = POINT_PATTERN.matcher( coordinate );

        if ( matcher.find() && matcher.groupCount() == 2 )
        {
            return new Point2D.Double( Double.parseDouble( matcher.group( 1 ) ),
                Double.parseDouble( matcher.group( 2 ) ) );
        }
        else
            return null;
    }

    /**
     * Returns a coordinate string based on the given latitude and longitude.
     * The coordinate is on the form longitude / latitude.
     *
     * @param longitude the longitude string.
     * @param latitude  the latitude string.
     * @return a coordinate string.
     */
    public static String getCoordinate( String longitude, String latitude )
    {
        return "[" + longitude + "," + latitude + "]";
    }

    /**
     * Checks if the given data value is valid according to the value type of the
     * given data element. Considers the value to be valid if null or empty.
     * Returns a string if the valid is invalid, possible
     * values are:
     * <p/>
     * <ul>
     * <li>data_element_or_type_null_or_empty</li>
     * <li>value_length_greater_than_max_length</li>
     * <li>value_not_numeric</li>
     * <li>value_not_unit_interval</li>
     * <li>value_not_percentage</li>
     * <li>value_not_integer</li>
     * <li>value_not_positive_integer</li>
     * <li>value_not_negative_integer</li>
     * <li>value_not_bool</li>
     * <li>value_not_true_only</li>
     * <li>value_not_valid_date</li>
     * </ul>
     *
     * @param value       the data value.
     * @param dataElement the data element.
     * @return null if the value is valid, a string if not.
     */
    public static String dataValueIsValid( String value, DataElement dataElement )
    {
        if ( dataElement == null || dataElement.getValueType() == null )
        {
            return "data_element_or_type_null_or_empty";
        }

        return dataValueIsValid( value, dataElement.getValueType() );
    }

    public static String dataValueIsValid( String value, ValueType valueType )
    {
        if ( value == null || value.trim().isEmpty() )
        {
            return null;
        }

        if ( valueType == null )
        {
            return "data_element_or_type_null_or_empty";
        }

        if ( value.length() > VALUE_MAX_LENGTH )
        {
            return "value_length_greater_than_max_length";
        }

        if ( ValueType.NUMBER == valueType && !MathUtils.isNumeric( value ) )
        {
            return "value_not_numeric";
        }

        if ( ValueType.UNIT_INTERVAL == valueType && !MathUtils.isUnitInterval( value ) )
        {
            return "value_not_unit_interval";
        }

        if ( ValueType.PERCENTAGE == valueType && !MathUtils.isPercentage( value ) )
        {
            return "value_not_percentage";
        }

        if ( ValueType.INTEGER == valueType && !MathUtils.isInteger( value ) )
        {
            return "value_not_integer";
        }

        if ( ValueType.INTEGER_POSITIVE == valueType && !MathUtils.isPositiveInteger( value ) )
        {
            return "value_not_positive_integer";
        }

        if ( ValueType.INTEGER_NEGATIVE == valueType && !MathUtils.isNegativeInteger( value ) )
        {
            return "value_not_negative_integer";
        }

        if ( ValueType.INTEGER_ZERO_OR_POSITIVE == valueType && !MathUtils.isZeroOrPositiveInteger( value ) )
        {
            return "value_not_zero_or_positive_integer";
        }

        if ( ValueType.BOOLEAN == valueType && !MathUtils.isBool( value ) )
        {
            return "value_not_bool";
        }

        if ( ValueType.TRUE_ONLY == valueType && !DataValue.TRUE.equals( value ) )
        {
            return "value_not_true_only";
        }

        if ( ValueType.DATE == valueType && !DateUtils.dateIsValid( value ) )
        {
            return "value_not_valid_date";
        }

        if ( ValueType.DATETIME == valueType && !DateUtils.dateTimeIsValid( value ) )
        {
            return "value_not_valid_datetime";
        }

        if ( ValueType.FILE_RESOURCE == valueType && !CodeGenerator.isValidUid( value ) )
        {
            return "value_not_valid_file_resource_uid";
        }

        if ( ValueType.COORDINATE == valueType && !MathUtils.isCoordinate( value ) )
        {
            return "value_not_coordinate";
        }

        return null;
    }

    /**
     * Indicates whether the given value is zero and not zero significant according
     * to its data element.
     *
     * @param value       the data value.
     * @param dataElement the data element.
     */
    public static boolean dataValueIsZeroAndInsignificant( String value, DataElement dataElement )
    {
        AggregationType aggregationType = dataElement.getAggregationType();

        return dataElement.getValueType().isNumeric() && MathUtils.isZero( value ) &&
            !dataElement.isZeroIsSignificant() &&
            !(aggregationType == AggregationType.AVERAGE_SUM_ORG_UNIT || aggregationType == AggregationType.AVERAGE);
    }

    /**
     * Checks if the given comment is valid. Returns null if valid and a string
     * if invalid, possible values are:
     * </p>
     * <ul>
     * <li>comment_length_greater_than_max_length</li>
     * </ul>
     *
     * @param comment the comment.
     * @return null if the comment is valid, a string if not.
     */
    public static String commentIsValid( String comment )
    {
        if ( comment == null || comment.trim().isEmpty() )
        {
            return null;
        }

        if ( comment.length() > VALUE_MAX_LENGTH )
        {
            return "comment_length_greater_than_max_length";
        }

        return null;
    }

    /**
     * Checks if the given stored by value is valid. Returns null if valid and a string
     * if invalid, possible values are:
     * </p>
     * <ul>
     * <li>stored_by_length_greater_than_max_length</li>
     * </ul>
     *
     * @param storedBy the stored by value.
     * @return null if the stored by value is valid, a string if not.
     */
    public static String storedByIsValid( String storedBy )
    {
        if ( storedBy == null || storedBy.trim().isEmpty() )
        {
            return null;
        }

        if ( storedBy.length() > 255 )
        {
            return "stored_by_length_greater_than_max_length";
        }

        return null;
    }

    /**
     * Checks to see if given parameter is a valid hex color string (#xxx and #xxxxxx, xxx, xxxxxx).
     *
     * @param value Value to check against
     * @return true if value is a hex color string, false otherwise
     */
    public static boolean isValidHexColor( String value )
    {
        return value != null && HEX_COLOR_PATTERN.matcher( value ).matches();
    }

    /**
     * Returns a string useful for substitution.
     *
     * @param valueType the value type.
     * @return the string.
     */
    public static String getSubstitutionValue( ValueType valueType )
    {
        if ( valueType.isNumeric() || valueType.isBoolean() )
        {
            return "1";
        }
        else if ( valueType.isDate() )
        {
            return "'2000-01-01'";
        }
        else
        {
            return "'A'";
        }
    }

    /**
     * Returns normalized boolean value. Supports a set of true and false values, indicated in
     * BOOL_FALSE_VARIANTS and BOOL_TRUE_VARIANTS sets.
     *
     * @param bool input value
     * @param valueType type of value. Return boolean value if type is boolean.
     * @return normalized boolean value.
     */
    public static String normalizeBoolean( String bool, ValueType valueType )
    {
        if( ValueType.BOOLEAN_TYPES.contains( valueType ) )
        {
            if ( BOOL_FALSE_VARIANTS.contains( bool ) && valueType != ValueType.TRUE_ONLY)
            {
                return DataValue.FALSE;
            }
            else if ( BOOL_TRUE_VARIANTS.contains( bool ) )
            {
                return DataValue.TRUE;
            }
        }

        return bool;
    }
}
