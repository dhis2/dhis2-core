/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.util.Constants.RANDOM_GENERATION_CHUNK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class RandomGeneratorServiceTest
{

    @InjectMocks
    private RandomGeneratorService randomGeneratorService;

    @Test
    public void shouldGenerateRandomInteger()
        throws Exception
    {
        int randomLength = 1;

        String textPattern = IntStream.range( 0, randomLength ).mapToObj( i -> "#" ).collect( Collectors.joining() );

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        assertEquals( RANDOM_GENERATION_CHUNK, randomList.size() );

        Pattern pattern = Pattern.compile( "-?\\d+(\\.\\d+)?" );

        randomList.forEach( r -> {
            assertTrue( pattern.matcher( r ).matches() );
            assertEquals( randomLength, r.length() );
        } );
    }

    @Test
    public void shouldGenerateRandomIntegers()
        throws Exception
    {
        int randomLength = 5;

        String textPattern = IntStream.range( 0, randomLength ).mapToObj( i -> "#" ).collect( Collectors.joining() );

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        assertEquals( RANDOM_GENERATION_CHUNK * 2 + 1, randomList.size() );

        Pattern pattern = Pattern.compile( "-?\\d+(\\.\\d+)?" );

        randomList.forEach( r -> {
            assertTrue( pattern.matcher( r ).matches() );
            assertEquals( randomLength, r.length() );
        } );
    }

    @Test
    public void shouldGenerateRandomLowerCases()
        throws Exception
    {
        int randomLength = 5;

        String textPattern = IntStream.range( 0, randomLength ).mapToObj( i -> "x" ).collect( Collectors.joining() );

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        randomList.forEach( r -> {
            assertEquals( r, r.toLowerCase() );
            assertEquals( randomLength, r.length() );
        } );
    }

    @Test
    public void shouldGenerateRandomUpperCases()
        throws Exception
    {
        int randomLength = 5;

        String textPattern = IntStream.range( 0, randomLength ).mapToObj( i -> "X" ).collect( Collectors.joining() );

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        randomList.forEach( r -> {
            assertEquals( r, r.toUpperCase() );
            assertEquals( randomLength, r.length() );
        } );
    }

    @Test
    public void shouldGenerateRandomMixedUpperLoweCaseLetters()
        throws Exception
    {
        String textPattern = "xxxXXX";

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        randomList.forEach( r -> {
            IntStream.range( 0, 3 ).forEach( i -> assertTrue( Character.isLowerCase( r.charAt( i ) ) ) );
            IntStream.range( 3, 6 ).forEach( i -> assertTrue( Character.isUpperCase( r.charAt( i ) ) ) );
        } );
    }

    @Test
    public void shouldGenerateRandomMixedLettersAndIntegers()
        throws Exception
    {
        String textPattern = "xx###";

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        assertEquals( RANDOM_GENERATION_CHUNK * 3, randomList.size() );

        randomList.forEach( r -> {
            IntStream.range( 0, 2 ).forEach( i -> assertTrue( Character.isLowerCase( r.charAt( i ) ) ) );
            IntStream.range( 2, 5 ).forEach( i -> assertTrue( Character.isDigit( r.charAt( i ) ) ) );
        } );
    }

    @Test
    public void shouldGenerateRandomAll()
        throws Exception
    {
        String textPattern = "***XXX###";

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        randomList.forEach( r -> {
            IntStream.range( 0, 3 ).forEach(
                i -> assertTrue( Character.isLetter( r.charAt( i ) ) || Character.isDigit( r.charAt( i ) ) ) );
            IntStream.range( 3, 6 ).forEach( i -> assertTrue( Character.isUpperCase( r.charAt( i ) ) ) );
            IntStream.range( 6, 9 ).forEach( i -> assertTrue( Character.isDigit( r.charAt( i ) ) ) );
        } );
    }

    @Test
    public void shouldGenerateMixed()
        throws Exception
    {
        String textPattern = "XXxx##*";

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        randomList.forEach( r -> {
            IntStream.range( 0, 2 ).forEach(
                i -> assertTrue( Character.isUpperCase( r.charAt( i ) ) ) );
            IntStream.range( 2, 4 ).forEach( i -> assertTrue( Character.isLowerCase( r.charAt( i ) ) ) );
            IntStream.range( 4, 6 ).forEach( i -> assertTrue( Character.isDigit( r.charAt( i ) ) ) );
            IntStream.range( 6, 7 ).forEach(
                i -> assertTrue( Character.isLetter( r.charAt( i ) ) || Character.isDigit( r.charAt( i ) ) ) );
        } );
    }

    @Test
    public void shouldGenerateFromLongSegment()
        throws Exception
    {
        String textPattern = String.join( "", Collections.nCopies( 50, "x" ) );

        randomGeneratorService.setSegmentParameter( textPattern );

        List<String> randomList = randomGeneratorService.call();

        assertEquals( randomList.get( 0 ).length(), 50 );
    }
}
