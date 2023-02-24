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
package org.hisp.dhis.fileresource;

import java.util.Optional;
import java.util.Set;

/**
 * @author Zubair Asghar.
 * @author Stian Sandvold
 */
public enum ImageFileDimension
{
    // Icon sizes. Default size for DHIS2 icons are 48. Android uses 40.
    ICON_40( "40x40" ),
    ICON_48( "48x48" ),

    // Picture sizes, primarily such as profile pictures.
    SMALL( "small" ),
    MEDIUM( "medium" ),
    LARGE( "large" ),

    // Original size, meaning no resizing / default.
    ORIGINAL( "" );

    private final String dimension;

    ImageFileDimension( String dimension )
    {
        this.dimension = dimension;
    }

    public String getDimension()
    {
        return this.dimension;
    }

    public static Optional<ImageFileDimension> from( String dimension )
    {
        for ( ImageFileDimension d : ImageFileDimension.values() )
        {
            if ( d.dimension.equalsIgnoreCase( dimension ) )
            {
                return Optional.of( d );
            }
        }

        return Optional.empty();
    }

    public static Set<ImageFileDimension> getPictureDimensions()
    {
        return Set.of( SMALL, MEDIUM, LARGE, ORIGINAL );
    }

    public static Set<ImageFileDimension> getIconDimensions()
    {
        return Set.of( ICON_40, ICON_48, ORIGINAL );
    }
}
