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

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.rank.Percentile.EstimationType;
import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.function.PostfixMathCommandI;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Base abstract class for percentile functions
 * <p/>
 * All percentile function take two arguments:
 * <p/>
 * percentile... ( fraction, valueList )
 * <p/>
 * The percentile is computed according to the EstimationType of the subclass.
 *
 * @author Jim Grace
 */
public abstract class PercentileBase
    extends PostfixMathCommand
    implements PostfixMathCommandI
{
    private final Percentile percentile = new Percentile().withEstimationType( getEstimationType() );

    public PercentileBase()
    {
        numberOfParameters = 2;
    }

    /**
     * Each subclass defines its percentile estimation type.
     *
     * @return the percentile estimation type.
     */
    protected abstract EstimationType getEstimationType();

    // nFunk's JEP run() method uses the raw Stack type
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void run( Stack inStack )
        throws ParseException
    {
        checkStack( inStack );

        // First arg was pushed on the stack first, and pops last.
        Object valueList = inStack.pop();
        Object fractionObject = inStack.pop();

        List<Double> valList = CustomFunctions.checkVector( valueList );
        Double fraction = CustomFunctions.checkDouble( fractionObject );

        if ( valList.size() == 0 || fraction < 0d || fraction > 1d )
        {
            throw new NoValueException();
        }
        else
        {
            Collections.sort( valList );

            if ( fraction == 0d )
            {
                inStack.push( valList.get( 0 ) );
            }
            else
            {
                double[] vals = ArrayUtils.toPrimitive( valList.toArray( new Double[0] ) );

                Double result = percentile.evaluate( vals, fraction * 100. );

                inStack.push( result );
            }
        }
    }
}
