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
package org.hisp.dhis.reservedvalue;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Generate random values from a pattern using pseudo-randomness to pick letters
 * and digits.
 *
 * Pattern:
 *
 * <pre>
 *  x = lower case
 *  X = upper case
 *  # = digit
 *  * = digit or lower case or upper case
 * </pre>
 *
 * @author Jan Bernitt
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
public class RandomPatternValueGenerator
{
    private static final String STAR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final AtomicLong NEXT_SEED = new AtomicLong( currentTimeMillis() );

    /**
     * Create n random values based on a provided pattern. Each value will have
     * same length as the pattern and use a random letter or digit matching the
     * type requested by the pattern.
     *
     * @param pattern a pattern using {@code xX#*} characters only, at least
     *        length 1
     * @param n number of iterations, also the minimum number of returned values
     *        but more might be returned when at least two digits
     * @return the generated random values
     */
    public static List<String> generateRandomValues( String pattern, int n )
    {
        RandomXorOshiro128 rnd = new RandomXorOshiro128(
            NEXT_SEED.updateAndGet( seed -> max( seed + 1, currentTimeMillis() ) ) );
        List<String> values = new ArrayList<>();
        long digits = pattern.chars().filter( c -> c == '#' ).count();
        boolean allDigits = digits == pattern.length();
        boolean multiDigit = digits >= 2;
        int len = pattern.length();
        StringBuilder digitsRandom = new StringBuilder( len );
        StringBuilder digitsLeadingZero = new StringBuilder( len );
        StringBuilder digitsAlwaysZero = new StringBuilder( len );
        for ( int ni = 0; ni < n; ni++ )
        {
            boolean leadingZeros = true;
            for ( int pi = 0; pi < len; pi++ )
            {
                char p = pattern.charAt( pi );
                char c;
                switch ( p )
                {
                    case 'x':
                        c = (char) ('a' + rnd.nextInt( 26 ));
                        break;
                    case 'X':
                        c = (char) ('A' + rnd.nextInt( 26 ));
                        break;
                    case '#':
                        c = (char) ('0' + rnd.nextInt( 10 ));
                        break;
                    case '*':
                        c = STAR.charAt( rnd.nextInt( STAR.length() ) );
                        break;
                    default:
                        throw new IllegalArgumentException( "Not a valid pattern symbol: " + p );
                }
                digitsRandom.append( c );
                if ( multiDigit )
                {
                    boolean isDigit = p == '#';
                    digitsLeadingZero.append( isDigit && leadingZeros ? '0' : c );
                    if ( !allDigits )
                    {
                        digitsAlwaysZero.append( isDigit ? '0' : c );
                    }
                    leadingZeros = !isDigit || leadingZeros && rnd.nextInt( 1 ) == 1;
                }
            }
            values.add( digitsRandom.toString() );
            digitsRandom.setLength( 0 );
            if ( multiDigit )
            {
                values.add( digitsLeadingZero.toString() );
                digitsLeadingZero.setLength( 0 );
                if ( !allDigits )
                {
                    values.add( digitsAlwaysZero.toString() );
                    digitsAlwaysZero.setLength( 0 );
                }
            }
        }
        if ( allDigits && multiDigit )
        {
            values.add( "0".repeat( len ) );
        }
        return values;
    }

    /**
     * A port of Blackman and Vigna's xoroshiro128+ generator;
     * <p>
     * Extracted from <a href=
     * "https://raw.githubusercontent.com/SquidPony/SquidLib/master/squidlib-util/src/main/java/squidpony/squidmath/XoRoRNG.java">XoRoRNG.java</a>
     * <p>
     * Original authors of xoroshiro128+ and java implementation of
     * xoroshiro128+:
     *
     * @author Sebastiano Vigna
     * @author David Blackman
     * @author Tommy Ettinger
     */
    private static final class RandomXorOshiro128
    {
        private long state0;

        private long state1;

        RandomXorOshiro128( long seed )
        {
            long state = seed + 0x9E3779B97F4A7C15L;
            long z = state;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            state0 = z ^ (z >>> 31);

            state += 0x9E3779B97F4A7C15L;
            z = state;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            state1 = z ^ (z >>> 31);
        }

        int next( int bits )
        {
            final long s0 = state0;
            long s1 = state1;
            final int result = (int) (s0 + s1) >>> (32 - bits);
            s1 ^= s0;
            state0 = (s0 << 55 | s0 >>> 9) ^ s1 ^ (s1 << 14); // a, b
            state1 = (s1 << 36 | s1 >>> 28); // c
            return result;
        }

        int nextInt( int bound )
        {
            if ( bound <= 0 )
                throw new IllegalArgumentException( "bound must be positive" );

            int r = next( 31 );
            int m = bound - 1;
            if ( (bound & m) == 0 ) // i.e., bound is a power of 2
                r = (int) ((bound * (long) r) >> 31);
            else
            {
                for ( int u = r; u - (r = u % bound) + m < 0; u = next( 31 ) )
                    ;
            }
            return r;
        }
    }
}
