package org.hisp.dhis.dxf2.events.enrollment;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.common.DxfNamespaces;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "enrollments", namespace = DxfNamespaces.DXF_2_0 )
public class Enrollments
{
    private List<Enrollment> enrollments = new ArrayList<>();

    public Enrollments()
    {
    }

    @JsonProperty( "enrollments" )
    @JacksonXmlElementWrapper( localName = "enrollments", useWrapping = false, namespace = DxfNamespaces.DXF_2_0 )
    @JacksonXmlProperty( localName = "enrollment", namespace = DxfNamespaces.DXF_2_0 )
    public List<Enrollment> getEnrollments()
    {
        return enrollments;
    }

    public void setEnrollments( List<Enrollment> enrollments )
    {
        this.enrollments = enrollments;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        Enrollments that = (Enrollments) o;

        if ( enrollments != null ? !enrollments.equals( that.enrollments ) : that.enrollments != null )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 0;
        result = 31 * result + ( enrollments != null ? enrollments.hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "Enrollments{" +
            "enrollments=" + enrollments +
            '}';
    }
}
