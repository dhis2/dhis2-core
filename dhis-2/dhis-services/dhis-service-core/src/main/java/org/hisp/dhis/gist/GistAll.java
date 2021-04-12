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
package org.hisp.dhis.gist;

import static java.util.EnumSet.complementOf;
import static java.util.EnumSet.of;

import java.util.EnumSet;

import lombok.AllArgsConstructor;

import org.hisp.dhis.schema.GistTransform;
import org.hisp.dhis.schema.Property;
import org.hisp.dhis.schema.PropertyType;

/**
 * Presets to scope what {@code :all} or {@code *} fields do include.
 *
 * @author Jan Bernitt
 */
@AllArgsConstructor
public enum GistAll
{
    XL( GistTransform.ID_OBJECTS, complementOf( of( PropertyType.PASSWORD ) ) ),
    L( GistTransform.IDS, complementOf( of( PropertyType.PASSWORD ) ) ),
    M( GistTransform.SIZE, complementOf( of( PropertyType.PASSWORD ) ) ),
    S( GistTransform.NONE, complementOf( of( PropertyType.PASSWORD, PropertyType.COMPLEX ) ) ),
    XS( GistTransform.NONE, of( PropertyType.IDENTIFIER, PropertyType.TEXT, PropertyType.EMAIL ) );

    private final GistTransform defaultTransformation;

    private final EnumSet<PropertyType> includes;

    public GistTransform getDefaultTransformation()
    {
        return defaultTransformation;
    }

    public boolean isIncluded( Property p )
    {
        return includes.contains( p.getPropertyType() )
            && (!p.isCollection() || includes.contains( p.getItemPropertyType() ));
    }
}
