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
package org.hisp.dhis;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EndpointTracker
{
    private static List<Coverage> coverageList;

    public static List<Coverage> getCoverageList()
    {
        return coverageList;
    }

    public static void add( String method, String url )
    {
        if ( coverageList == null )
        {
            coverageList = new ArrayList<>();
        }

        Coverage existing = coverageList.stream()
            .filter( p -> p.method.equalsIgnoreCase( method ) && p.url.equalsIgnoreCase( url ) )
            .findFirst()
            .orElse( null );

        if ( existing != null )

        {
            existing.occurrences += 1;
        }

        else
        {
            coverageList.add( new Coverage( method, url ) );
        }
    }

    public static class Coverage
    {
        private String method;

        private String url;

        private Integer occurrences;

        public Coverage( String method, String url )
        {
            this.method = method;
            this.url = url;
            this.occurrences = 1;
        }

        public String getMethod()
        {
            return method;
        }

        public void setMethod( String method )
        {
            this.method = method;
        }

        public String getUrl()
        {
            return this.url;
        }

        public void setUrl( String url )
        {
            this.url = url;
        }

        public Integer getOccurrences()
        {
            return occurrences;
        }

        public void setOccurrences( int occurrences )
        {
            this.occurrences = occurrences;
        }
    }
}
