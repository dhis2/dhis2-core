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
package org.hisp.dhis.analytics.common.dimension;

import static org.hisp.dhis.common.DimensionalObject.DIMENSION_IDENTIFIER_SEP;

import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import org.hisp.dhis.common.UidObject;

/**
 * Class to identify a Dimension in analytics In TEI CPL, a dimension can be
 * composed by up to 3 coordinates: Program, Stage and dimensionId
 */
@Data
@AllArgsConstructor( staticName = "of" )
public class DimensionIdentifier<P extends UidObject, S extends UidObject, D extends UidObject>
{
    private final ElementWithOffset<P> program;

    private final ElementWithOffset<S> programStage;

    private final D dimension;

    public boolean hasProgram()
    {
        return program != null && program.isPresent();
    }

    public boolean hasProgramStage()
    {
        return programStage != null && programStage.isPresent();
    }

    @Override
    public String toString()
    {
        String string = "";
        if ( program.isPresent() )
        {
            string += program + DIMENSION_IDENTIFIER_SEP;
        }
        if ( programStage.isPresent() )
        {
            string += programStage + DIMENSION_IDENTIFIER_SEP;
        }
        return string + dimension.getUid();
    }

    @Data
    @RequiredArgsConstructor( staticName = "of" )
    public static class ElementWithOffset<T extends UidObject>
    {
        private final T element;

        private final String offset;

        public boolean hasOffset()
        {
            return Objects.nonNull( offset );
        }

        public boolean isPresent()
        {
            return Objects.nonNull( element );
        }

        @Override
        public String toString()
        {
            if ( isPresent() )
            {
                if ( hasOffset() )
                {
                    return element.getUid() + "[" + offset + "]";
                }
                return element.getUid();
            }
            return "";
        }
    }
}
