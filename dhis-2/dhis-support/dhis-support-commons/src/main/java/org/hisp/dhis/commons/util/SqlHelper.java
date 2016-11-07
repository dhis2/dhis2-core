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

/**
 * Class with utility methods for constructing SQL strings.
 * 
 * @author Lars Helge Overland
 */
public class SqlHelper
{
    private boolean invoked = false;
    
    private boolean isBetweenInvoked = false;

    private boolean includeSpaces = false;

    private boolean havingInvoked = false;

    public SqlHelper()
    {
    }

    public SqlHelper( boolean includeSpaces )
    {
        this.includeSpaces = includeSpaces;
    }

    /**
     * Returns "where" the first time it is invoked, then "and" for subsequent
     * invocations.
     * 
     * @return "where" or "and".
     */
    public String whereAnd()
    {
        String str = invoked ? "and" : "where";

        invoked = true;

        return includeSpaces ? " " + str + " " : str;
    }

    /**
     *
     */
    public String havingAnd()
    {
        String str = havingInvoked ? "and" : "having";

        havingInvoked = true;

        return includeSpaces ? " " + str + " " : str;
    }

    /**
     * Returns "where" the first time it is invoked, then "or" for subsequent
     * invocations.
     * 
     * @return "where" or "or".
     */
    public String whereOr()
    {
        String str = invoked ? "or" : "where";

        invoked = true;

        return includeSpaces ? " " + str + " " : str;
    }

    /**
     * Returns "" the first time it is invoked, then "and" for subsequent
     * invocations.
     * 
     * @return empty or "and".
     */
    public String and()
    {
        String str = invoked ? "and" : "";

        invoked = true;

        return includeSpaces ? " " + str + " " : str;
    }

    /**
     * Returns "" the first time it is invoked, then "or" for subsequent
     * invocations.
     * 
     * @return empty or "or".
     */
    public String or()
    {
        String str = invoked ? "or" : "";

        invoked = true;

        return includeSpaces ? " " + str + " " : str;
    }
    
    public String betweenAnd()
    {
        String str = isBetweenInvoked ? "and" : "BETWEEN";

        isBetweenInvoked = true;

        return includeSpaces ? " " + str + " " : str;
    }
}
