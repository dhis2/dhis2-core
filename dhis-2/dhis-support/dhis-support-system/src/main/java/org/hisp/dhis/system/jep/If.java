package org.hisp.dhis.system.jep;

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

import org.nfunk.jep.ParseException;
import org.nfunk.jep.function.PostfixMathCommand;
import org.nfunk.jep.function.PostfixMathCommandI;

import java.util.Stack;

/**
 * @author Jim Grace
 *
 * The IF function takes three arguments:
 *
 * IF ( test, valueIfTrue, valueIfFalse )
 *
 * Where test should be a boolean value (represented in JEP by a
 * Double with value 1 if true and 0 if false). If test is true, the
 * valueIfTrue is returned, otherwise valueIfFalse is returned.
 */
public class If
    extends PostfixMathCommand
    implements PostfixMathCommandI
{
    public If()
    {
        numberOfParameters = 3;
    }

    // nFunk's JEP run() method uses the raw Stack type
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    public void run( Stack inStack )
        throws ParseException
    {
        checkStack( inStack );

        // First arg was pushed on the stack first, and pops last.
        Object valueIfFalse = inStack.pop();
        Object valueIfTrue = inStack.pop();
        Object test = inStack.pop();

        Object result = test instanceof Double && (Double)test != 0.0 ?
            valueIfTrue : valueIfFalse;

        inStack.push( result );
    }
}
