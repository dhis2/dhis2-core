package org.hisp.dhis.system.jep;

/*
 * Copyright (c) 2004-2019, University of Oslo
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

import java.util.Arrays;
import java.util.Stack;

/**
 * Base abstract class for percentile tests.
 *
 * @author Jim Grace
 */

public abstract class PercentileTest
{
    private final PercentileBase percentileToTest = getPercentileToTest();

    protected final static double DELTA = 1e-15;

    /**
     * Each test subclass defines its percentile function to test.
     *
     * @return the percentile function to test.
     */
    protected abstract PercentileBase getPercentileToTest();

    /**
     * Evaluates the percentile function to test
     *
     * @param fraction the percentile fraction
     * @param values the set of values
     * @return the percentile value
     * @throws org.nfunk.jep.ParseException
     */
    protected Double eval( double fraction, Double ... values )
        throws org.nfunk.jep.ParseException
    {
        Stack inStack = new Stack();
        inStack.push( fraction );

        if ( values != null )
        {
            inStack.push( Arrays.asList( values ) );
        }
        else
        {
            inStack.push( Double.valueOf( 0d ) );
        }

        percentileToTest.run( inStack );

        return (Double) inStack.pop();
    }
}
