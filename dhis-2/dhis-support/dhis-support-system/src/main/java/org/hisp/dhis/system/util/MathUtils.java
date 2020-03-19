package org.hisp.dhis.system.util;

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

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.SimpleCacheBuilder;
import org.apache.commons.math3.util.Precision;
import org.apache.commons.validator.routines.DoubleValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.system.jep.CustomFunctions;
import org.nfunk.jep.JEP;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author Lars Helge Overland
 */
public class MathUtils
{
    /**
     * Cache for JEP expression evaluation.
     */
    private static Cache<Double> EXPR_EVAL_CACHE = new SimpleCacheBuilder<Double>().forRegion( "exprEvalCache" )
        .expireAfterAccess( 1, TimeUnit.HOURS )
        .withInitialCapacity( 200 )
        .withMaximumSize( 5000 )
        .build();

    public static final Double ZERO = new Double( 0 );

    private static final Locale LOCALE = new Locale( "en" );

    private static DoubleValidator DOUBLE_VALIDATOR = new DoubleValidator();
    private static IntegerValidator INT_VALIDATOR = new IntegerValidator();

    private static final double TOLERANCE = 0.01;
    private static final int NUMBER_MAX_LENGTH = 250;

    public static final String NUMERIC_REGEXP = "^(-?0|-?[1-9]\\d*)(\\.\\d+)?$";
    public static final String NUMERIC_LENIENT_REGEXP = "^(-?[0-9]+)(\\.[0-9]+)?$";

    private static final Pattern NUMERIC_PATTERN = Pattern.compile( NUMERIC_REGEXP );
    private static final Pattern NUMERIC_LENIENT_PATTERN = Pattern.compile( NUMERIC_LENIENT_REGEXP );
    private static final Pattern INT_PATTERN = Pattern.compile( "^(0|-?[1-9]\\d*)$" );
    private static final Pattern POSITIVE_INT_PATTERN = Pattern.compile( "^[1-9]\\d*$" );
    private static final Pattern POSITIVE_OR_ZERO_INT_PATTERN = Pattern.compile( "(^0$)|(^[1-9]\\d*$)" );
    private static final Pattern NEGATIVE_INT_PATTERN = Pattern.compile( "^-[1-9]\\d*$" );
    private static final Pattern ZERO_PATTERN = Pattern.compile( "^0(\\.0*)?$" );

    /**
     * Evaluates whether an expression is true or false.
     *
     * @param leftSide  The left side of the expression.
     * @param operator  The expression operator.
     * @param rightSide The right side of the expression.
     * @return True if the expression is true, false otherwise.
     */
    public static boolean expressionIsTrue( double leftSide, Operator operator, double rightSide )
    {
        final String expression = leftSide + operator.getMathematicalOperator() + rightSide;

        return expressionIsTrue( expression );
    }

    /**
     * Evaluates whether an expression is true or false.
     *
     * @param expression the expression to evaluate.
     * @return True if the expression is true, false otherwise.
     */
    public static boolean expressionIsTrue( String expression )
    {
        final JEP parser = getJep();
        parser.parseExpression( expression );

        return isEqual( parser.getValue(), 1.0 );
    }

    /**
     * Calculates a regular mathematical expression.
     *
     * @param expression The expression to calculate.
     * @return The result of the operation.
     */
    public static double calculateExpression( String expression )
    {
        return EXPR_EVAL_CACHE.get( expression, c -> calculateExpressionInternal( expression ) ).get();
    }

    /**
     * Calculates a regular mathematical expression.
     *
     * @param expression The expression to calculate.
     * @return The result of the operation.
     */
    public static Object calculateGenericExpression( String expression )
    {
        final JEP parser = getJep();
        parser.parseExpression( expression );

        Object result = parser.getValueAsObject();

        return result;
    }

    private static double calculateExpressionInternal( String expression )
    {
        final JEP parser = getJep();
        parser.parseExpression( expression );

        return parser.getValue();
    }

    /**
     * Indicates whether the given double valid, implying it is not null, not
     * infinite and not NaN.
     *
     * @param d the double.
     * @return true if the given double is valid.
     */
    public static boolean isValidDouble( Double d )
    {
        return d != null && !Double.isInfinite( d ) && !Double.isNaN( d );
    }

    /**
     * Investigates whether the expression is valid or has errors.
     *
     * @param expression The expression to validate.
     * @return True if the expression has errors, false otherwise.
     */
    public static boolean expressionHasErrors( String expression )
    {
        final JEP parser = getJep();
        parser.parseExpression( expression.toUpperCase() );

        return parser.hasError();
    }

    /**
     * Returns the error information for an invalid expression.
     *
     * @param expression The expression to validate.
     * @return The error information for an invalid expression, null if
     * the expression is valid.
     */
    public static String getExpressionErrorInfo( String expression )
    {
        final JEP parser = getJep();
        parser.parseExpression( expression );

        return parser.getErrorInfo();
    }

    /**
     * Returns an JEP parser instance.
     */
    private static JEP getJep()
    {
        final JEP parser = new JEP();
        parser.addStandardFunctions();
        CustomFunctions.addFunctions( parser );
        return parser;
    }

    /**
     * Rounds off downwards to the next distinct value.
     *
     * @param value The value to round off
     * @return The rounded value
     */
    public static double getFloor( double value )
    {
        return Math.floor( value );
    }

    /**
     * Returns a rounded off number.
     * <p>
     * <ul>
     * <li>If value is exclusively between 1 and -1 it will have 2 decimals.</li>
     * <li>If value if greater or equal to 1 the value will have 1 decimal.</li>
     * </ul>
     *
     * @param value the value to round off.
     * @return a rounded off number.
     */
    public static double getRounded( double value )
    {
        int scale = ( value < 1d && value > -1d ) ? 2 : 1;
        
        return Precision.round( value, scale );
    }

    /**
     * Returns a rounded off number. If the value class is not Double, the value
     * is returned unchanged.
     *
     * @param value the value to return and potentially round off.
     */
    public static Object getRoundedObject( Object value )
    {
        return value != null && Double.class.equals( value.getClass() ) ? getRounded( (Double) value ) : value;
    }

    /**
     * Rounds a number, keeping at least 3 significant digits.
     * <p>
     * <ul>
     * <li>If value is >= 10 or <= -10 it will have 1 decimal.</li>
     * <li>If value is between -10 and 10 it will have three significant digits.</li>
     * </ul>
     *
     * @param value the value to round off.
     * @return a rounded off number.
     */
    public static double roundSignificant( double value )
    {
        if ( value >= 10.0 || value <= -10.0 )
        {
            return Precision.round( value, 1 );
        }
        else
        {
            return roundToSignificantDigits( value, 3 );
        }
    }

    /**
     * Rounds a number to a given number of significant decimal digits.
     * Note that the number will be left with *only* this number of
     * significant digits regardless of magnitude, e.g. 12345 to 3 digits
     * will be 12300, whereas 0.12345 will be 0.123.
     *
     * @param value the value to round off.
     * @param n     the number of significant decimal digits desired.
     * @return a rounded off number.
     */
    public static double roundToSignificantDigits( double value, int n )
    {
        if ( isEqual( value, 0.0 ) )
        {
            return 0.0;
        }

        final double d = Math.ceil( Math.log10( value < 0.0 ? -value : value ) );
        final int power = n - (int) d;

        final double magnitude = Math.pow( 10.0, power );
        final long shifted = Math.round( value * magnitude );
        return shifted / magnitude;
    }

    /**
     * Rounds the fractional part of a number to a given number of significant
     * decimal digits. Digits to the left of the decimal point will not
     * be rounded. For example, rounding 12345 to 3 digits will be 12345,
     * whereas 12.345 will be 12.3, and 0.12345 will be 0.123.
     *
     * @param value the value to round off.
     * @param n     the number of significant fraction decimal digits desired.
     * @return a rounded off number.
     */
    public static double roundFraction( double value, int n )
    {
        if ( isEqual( value, 0.0 ) )
        {
            return 0.0;
        }

        final double d = Math.ceil( Math.log10( value < 0.0 ? -value : value ) );

        if ( d >= n )
        {
            return (double) Math.round( value );
        }

        return roundToSignificantDigits( value ,n );
    }

    /**
     * Returns the given number if larger or equal to minimun, otherwise minimum.
     *
     * @param number the number.
     * @param min    the minimum.
     * @return the given number if larger or equal to minimun, otherwise minimum.
     */
    public static int getMin( int number, int min )
    {
        return number < min ? min : number;
    }

    /**
     * Returns the given number if smaller or equal to maximum, otherwise maximum.
     *
     * @param number the number.
     * @param max    the maximum.
     * @return the the given number if smaller or equal to maximum, otherwise maximum.
     */
    public static int getMax( int number, int max )
    {
        return number > max ? max : number;
    }

    /**
     * Returns the given value if between the min and max value. If lower than
     * minimum, returns minimum, if higher than maximum, returns maximum.
     *
     * @param value the value.
     * @param min   the minimum value.
     * @param max   the maximum value.
     * @return an integer value.
     */
    public static int getWithin( int value, int min, int max )
    {
        value = Math.max( value, min );
        value = Math.min( value, max );
        return value;
    }

    /**
     * Returns true if the provided string argument is to be considered numeric.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered numeric.
     */
    public static boolean isNumeric( String value )
    {
        return value != null && DOUBLE_VALIDATOR.isValid( value, LOCALE ) &&
            NUMERIC_PATTERN.matcher( value ).matches() && value.length() < NUMBER_MAX_LENGTH;
    }

    /**
     * Returns true if the provided string argument is to be considered numeric.
     * Matches using a lenient pattern where leading zeros are allowed.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered numeric.
     */
    public static boolean isNumericLenient( String value )
    {
        return value != null && DOUBLE_VALIDATOR.isValid( value, LOCALE ) && NUMERIC_LENIENT_PATTERN.matcher( value ).matches();
    }

    /**
     * Returns true if the provided string argument is to be considered a unit
     * interval, which implies that the value is numeric and inclusive between 0
     * and 1.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered a unit interval.
     */
    public static boolean isUnitInterval( String value )
    {
        if ( !isNumeric( value ) )
        {
            return false;
        }

        Double dbl = Double.parseDouble( value );

        return dbl >= 0d && dbl <= 1d;
    }

    /**
     * Returns true if the provided string argument is an integer in the inclusive
     * range of 0 to 100.
     *
     * @param value the value.
     * @return true if the provided string argument is a percentage.
     */
    public static boolean isPercentage( String value )
    {
        if ( !isInteger( value ) )
        {
            return false;
        }

        Integer integer = Integer.valueOf( value );

        return integer >= 0 && integer <= 100;
    }

    /**
     * Returns true if the provided string argument is to be considered an integer.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered an integer.
     */
    public static boolean isInteger( String value )
    {
        return value != null && INT_VALIDATOR.isValid( value ) && INT_PATTERN.matcher( value ).matches();
    }

    /**
     * Returns true if the provided string argument is to be considered a positive
     * integer.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered a positive
     * integer.
     */
    public static boolean isPositiveInteger( String value )
    {
        return value != null && INT_VALIDATOR.isValid( value ) && POSITIVE_INT_PATTERN.matcher( value ).matches();
    }

    /**
     * Returns true if the provided string argument is to be considered a positive
     * or zero integer.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered a positive
     * integer.
     */
    public static boolean isZeroOrPositiveInteger( String value )
    {
        return value != null && INT_VALIDATOR.isValid( value ) && POSITIVE_OR_ZERO_INT_PATTERN.matcher( value ).matches();
    }

    /**
     * Returns true if the provided string argument is to be considered a
     * coordinate.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered a
     * coordinate.
     */
    public static boolean isCoordinate( String value )
    {        
        if( value == null )
        {
            return false;
        }
        
        value = value.replaceAll( "\\s+", "" );
        if ( value.length() < 5 || value.indexOf( "[" ) != 0 || value.indexOf( "]" ) != value.length() - 1 )
        {
            return false;
        }
        
        try
        {
            String[] lnglat = value.substring( 1, value.length() - 1 ).split( "," );
            float lng = Float.parseFloat( lnglat[0] );
            float lat = Float.parseFloat( lnglat[1] );
            return (lng >= -180 && lng <= 180 && lat >= -90 && lat <= 90);
        }
        catch ( Exception x )
        {
            return false;
        }
    }

    /**
     * Returns true if the provided string argument is to be considered a negative
     * integer.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered a negative
     * integer.
     */
    public static boolean isNegativeInteger( String value )
    {
        return value != null && INT_VALIDATOR.isValid( value ) && NEGATIVE_INT_PATTERN.matcher( value ).matches();
    }

    /**
     * Returns true if the provided string argument is to be considered a zero.
     *
     * @param value the value.
     * @return true if the provided string argument is to be considered a zero.
     */
    public static boolean isZero( String value )
    {
        return value != null && ZERO_PATTERN.matcher( value ).matches();
    }

    /**
     * Indicates if the provided string argument is to be considered as a boolean,
     * more specifically if it equals "true" or "false".
     *
     * @param value the value.
     * @return if the provided string argument is to be considered as a boolean.
     */
    public static boolean isBool( String value )
    {
        return value != null && (value.equals( "true" ) || value.equals( "false" ));
    }

    /**
     * Tests whether the two decimal numbers are equal with a tolerance of 0.01.
     * If one or both of the numbers are null, false is returned.
     *
     * @param d1 the first value.
     * @param d2 the second value.
     * @return true if the two decimal numbers are equal with a tolerance of 0.01.
     */
    public static boolean isEqual( Double d1, Double d2 )
    {
        if ( d1 == null || d2 == null )
        {
            return false;
        }

        return Math.abs( d1 - d2 ) < TOLERANCE;
    }

    /**
     * Tests whether the two decimal numbers are equal with a tolerance of 0.01.
     *
     * @param d1 the first value.
     * @param d2 the second value.
     * @return true if the two decimal numbers are equal with a tolerance of 0.01.
     */
    public static boolean isEqual( double d1, double d2 )
    {
        return Math.abs( d1 - d2 ) < TOLERANCE;
    }

    /**
     * Tests whether the given double is equal to zero.
     *
     * @param value the value.
     * @return true or false.
     */
    public static boolean isZero( double value )
    {
        return isEqual( value, 0d );
    }

    /**
     * Returns 0d if the given value is null, the original value otherwise.
     *
     * @param value the value.
     * @return a double.
     */
    public static double zeroIfNull( Double value )
    {
        return value == null ? 0d : value;
    }

    /**
     * Returns a random int between 0 and 999.
     */
    public static int getRandom()
    {
        return new Random().nextInt( 999 );
    }

    /**
     * Returns the minimum value from the given array.
     *
     * @param array the array of numbers.
     * @return the minimum value.
     */
    public static Double getMin( double[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return null;
        }

        double min = array[0];

        for ( int i = 1; i < array.length; i++ )
        {
            if ( array[i] < min )
            {
                min = array[i];
            }
        }

        return min;
    }

    /**
     * Returns the maximum value from the given array.
     *
     * @param array the array of numbers.
     * @return the maximum value.
     */
    public static Double getMax( double[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return null;
        }

        double max = array[0];

        for ( int i = 1; i < array.length; i++ )
        {
            if ( array[i] > max )
            {
                max = array[i];
            }
        }

        return max;
    }

    /**
     * Returns the average of the given values.
     *
     * @param values the values.
     * @return the average.
     */
    public static Double getAverage( List<Double> values )
    {
        Double sum = getSum( values );

        return sum / values.size();
    }

    /**
     * Returns the sum of the given values.
     *
     * @param values the values.
     * @return the sum.
     */
    public static Double getSum( List<Double> values )
    {
        Double sum = 0.0;

        for ( Double value : values )
        {
            if ( value != null )
            {
                sum += value;
            }
        }

        return sum;
    }

    /**
     * Parses the given string and returns a double value. Returns null if the
     * given string is null or cannot be parsed as a double.
     *
     * @param value the string value.
     * @return a double value.
     */
    public static Double parseDouble( String value )
    {
        if ( value == null || value.trim().isEmpty() )
        {
            return null;
        }

        try
        {
            return Double.parseDouble( value );
        }
        catch ( NumberFormatException ex )
        {
            return null;
        }
    }

    /**
     * Parses an integer silently. Returns the Integer value of the given string.
     * Returns null if the input string is null, empty or if it cannot be parsed.
     *
     * @param string the string.
     * @return an Integer.
     */
    public static Integer parseInt( String string )
    {
        if ( string == null || string.trim().isEmpty() )
        {
            return null;
        }

        try
        {
            return Integer.parseInt( string );
        }
        catch ( NumberFormatException ex )
        {
            return null;
        }
    }

    /**
     * Returns the lower bound for the given standard deviation, number of standard
     * deviations and average.
     *
     * @param stdDev   the standard deviation.
     * @param stdDevNo the number of standard deviations.
     * @param average  the average.
     * @return a double.
     */
    public static double getLowBound( double stdDev, double stdDevNo, double average )
    {
        double deviation = stdDev * stdDevNo;
        return average - deviation;
    }

    /**
     * Returns the high bound for the given standard deviation, number of standard
     * deviations and average.
     *
     * @param stdDev       the standard deviation.
     * @param stdDevFactor the number of standard deviations.
     * @param average      the average.
     * @return a double.
     */
    public static double getHighBound( double stdDev, double stdDevFactor, double average )
    {
        double deviation = stdDev * stdDevFactor;
        return average + deviation;
    }

    /**
     * Performs a division and rounds upwards to the next integer.
     *
     * @param numerator   the numerator.
     * @param denominator the denominator.
     * @return an integer value.
     */
    public static int divideToCeil( int numerator, int denominator )
    {
        Double result = Math.ceil( (double) numerator / denominator );

        return result.intValue();
    }

    /**
     * Performs a division and rounds downwards to the next integer.
     *
     * @param numerator   the numerator.
     * @param denominator the denominator.
     * @return an integer value.
     */
    public static int divideToFloor( int numerator, int denominator )
    {
        Double result = Math.floor( (double) numerator / denominator );

        return result.intValue();
    }


}
