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
package org.hisp.dhis.eventvisualization;

import static java.util.Arrays.stream;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DxfNamespaces.DXF_2_0;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import lombok.Data;

import org.hisp.dhis.common.DimensionType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * This is used to represents dimensions that are needed by clients but do not
 * actually exists in the back-end/codebase.
 *
 * It holds dimensions that do not have a concrete entity representation, and
 * are simple enough so they can be represented by Strings.
 *
 * It should hold only the necessary data for the respective event
 * visualization.
 *
 * @author maikel arabori
 */
@Data
public class SimpleDimension implements Serializable
{
    public enum Type
    {
        EVENT_DATE( "eventDate", PERIOD ),
        ENROLLMENT_DATE( "enrollmentDate", PERIOD ),
        INCIDENT_DATE( "incidentDate", PERIOD ),
        SCHEDULE_DATE( "scheduledDate", PERIOD ),
        LAST_UPDATE_DATE( "lastUpdatedDate", PERIOD );

        private final String dimension;

        private final DimensionType parentType;

        Type( final String dimension, final DimensionType parentType )
        {
            this.dimension = dimension;
            this.parentType = parentType;
        }

        public String getDimension()
        {
            return dimension;
        }

        public DimensionType getParentType()
        {
            return parentType;
        }

        public static boolean contains( final String dimension )
        {
            return stream( Type.values() )
                .anyMatch( t -> t.getDimension().equals( dimension ) );
        }

        public static Type from( final String dimension )
        {
            return stream( Type.values() )
                .filter( t -> t.getDimension().equals( dimension ) ).findFirst()
                .orElseThrow( () -> new NoSuchElementException( "Invalid dimension: " + dimension ) );
        }
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private Attribute parent;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private String dimension;

    @JsonProperty
    @JacksonXmlProperty( namespace = DXF_2_0 )
    private List<String> values = new ArrayList<>();

    boolean belongsTo( final Attribute parent )
    {
        return this.parent.equals( parent );
    }
}
