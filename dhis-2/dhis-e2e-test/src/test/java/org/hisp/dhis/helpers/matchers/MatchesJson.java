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
package org.hisp.dhis.helpers.matchers;

import com.google.gson.JsonObject;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hisp.dhis.helpers.JsonParserUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class MatchesJson
    extends TypeSafeDiagnosingMatcher<Object>
{

    private final String expectedJSON;

    private JSONCompareMode jsonCompareMode;

    private String mismatch;

    public MatchesJson( final String expectedJSON )
    {
        this.expectedJSON = expectedJSON;
        this.jsonCompareMode = JSONCompareMode.LENIENT;
        this.mismatch = "";
    }

    private static String toJSONString( final Object o )
    {
        return o instanceof JsonObject ? o.toString() : JsonParserUtils.toJsonObject( o ).toString();
    }

    public static MatchesJson matchesJSON( final String expectedJSON )
    {
        return new MatchesJson( expectedJSON );
    }

    public static MatchesJson matchesJSON( final Object obj )
    {
        return new MatchesJson( toJSONString(
            obj ) );
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendValue( mismatch );
    }

    @Override
    protected boolean matchesSafely( Object actual, Description mismatchDescription )
    {
        final String actualJSON = toJSONString( actual );
        try
        {
            JSONCompareResult result = JSONCompare.compareJSON( expectedJSON, actualJSON, jsonCompareMode );
            if ( result.failed() )
            {
                mismatch = result.getMessage();
            }

            return result.passed();
        }
        catch ( JSONException e )
        {
            return false;
        }
    }

}