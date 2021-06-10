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

package org.hisp.dhis.helpers.matchers;

import com.google.gson.JsonObject;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.beans.HasProperty;
import org.hisp.dhis.helpers.JsonParserUtils;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class HasPropertyEqualTo extends TypeSafeDiagnosingMatcher<Object>
{
    private String propertyName;
    private String expected;
    public HasPropertyEqualTo( String propertyName, String expected ) {
        this.propertyName = propertyName;
        this.expected = expected;
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendValue( String.format( "Property `%s`to equal `%s`", propertyName, expected));
    }
    public static HasPropertyEqualTo hasPropertyEqualTo( String property, String expectedValue) {
        return new HasPropertyEqualTo( property, expectedValue );
    }

    @Override
    protected boolean matchesSafely( Object item, Description mismatchDescription )
    {
        JsonObject object = JsonParserUtils.toJsonObject( item );

        if (!object.has( propertyName )) {
            mismatchDescription.appendText( String.format( "Expected %s, but property wasn't found", expected) );
            return false;
        }

        String value = object.get( propertyName ).getAsString();
        if (!value.equals( expected )) {
            mismatchDescription.appendText( String.format( "Expected property %s to equal to %s, but found %s", propertyName, expected, value));
            return false;
        }

        return true;
    }
}
