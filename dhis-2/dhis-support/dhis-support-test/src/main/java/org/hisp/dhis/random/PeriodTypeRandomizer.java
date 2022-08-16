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
package org.hisp.dhis.random;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.hisp.dhis.period.PeriodType;
import org.jeasy.random.api.Randomizer;

/**
 * @author Luciano Fiandesio
 */
public class PeriodTypeRandomizer
    implements
    Randomizer<PeriodType>
{

    private static final Random RANDOM = ThreadLocalRandom.current();

    private List<PeriodType> periodTypes = Arrays.asList(
        PeriodType.getPeriodTypeFromIsoString( "2011" ),
        PeriodType.getPeriodTypeFromIsoString( "201101" ),
        PeriodType.getPeriodTypeFromIsoString( "2011W1" ),
        PeriodType.getPeriodTypeFromIsoString( "2011W32" ),
        PeriodType.getPeriodTypeFromIsoString( "20110101" ),
        PeriodType.getPeriodTypeFromIsoString( "2011Q3" ),
        PeriodType.getPeriodTypeFromIsoString( "201101B" ),
        PeriodType.getPeriodTypeFromIsoString( "2011S1" ),
        PeriodType.getPeriodTypeFromIsoString( "2011AprilS1" ),
        PeriodType.getPeriodTypeFromIsoString( "2011April" ),
        PeriodType.getPeriodTypeFromIsoString( "2011July" ),
        PeriodType.getPeriodTypeFromIsoString( "2011Oct" ) );

    @Override
    public PeriodType getRandomValue()
    {
        return periodTypes.get( RANDOM.nextInt( periodTypes.size() - 1 ) );
    }
}
