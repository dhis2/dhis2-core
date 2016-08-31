package org.hisp.dhis.web.ohie.fred.webapi.v1.domain;

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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.hibernate.validator.constraints.Length;
import org.hisp.dhis.web.ohie.fred.webapi.v1.validation.constraint.annotation.ValidProperties;
import org.hisp.dhis.web.ohie.fred.webapi.v1.validation.group.Update;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JsonPropertyOrder( value = { "uuid", "name", "active", "href", "createdAt", "updatedAt", "coordinates", "identifiers", "properties" } )
public class Facility
{
    // Internal system identifier
    @NotNull( groups = Update.class )
    @Length( min = 36, max = 36 )
    private String uuid;

    // Name of the facility
    @NotNull
    @Length( min = 2, max = 160 )
    private String name;

    // Active = true/false indicates whether the facility is active or not
    private Boolean active = true;

    // URL link to the unique ID API resource for the facility
    private String href;

    // ISO 8601 timestamp, including timezone, of when the facility was created
    private Date createdAt;

    // ISO 8601 timestamp, including timezone, of when the facility was last updated
    private Date updatedAt;

    // Geo-location represented by latitude and longitude coordinates in that order
    @NotNull
    private List<Double> coordinates = new ArrayList<>();

    // External Facility Identifiers
    private List<Identifier> identifiers = new ArrayList<>();

    // Implementation specific custom properties
    @ValidProperties
    private Map<String, Object> properties = new TreeMap<>();

    public Facility()
    {
    }

    public Facility( String name )
    {
        this.name = name;
    }

    public Facility( String name, Boolean active )
    {
        this.name = name;
        this.active = active;
    }

    public Facility( String name, Boolean active, List<Double> coordinates )
    {
        this.name = name;
        this.active = active;
        this.coordinates = coordinates;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid( String uuid )
    {
        this.uuid = uuid;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public Boolean getActive()
    {
        return active;
    }

    public void setActive( Boolean active )
    {
        this.active = active;
    }

    public String getHref()
    {
        return href;
    }

    public void setHref( String href )
    {
        this.href = href;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt( Date createdAt )
    {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    public void setUpdatedAt( Date updatedAt )
    {
        this.updatedAt = updatedAt;
    }

    public List<Double> getCoordinates()
    {
        return coordinates;
    }

    public void setCoordinates( List<Double> coordinates )
    {
        this.coordinates = coordinates;
    }

    public List<Identifier> getIdentifiers()
    {
        return identifiers;
    }

    public void setIdentifiers( List<Identifier> identifiers )
    {
        this.identifiers = identifiers;
    }

    public Map<String, Object> getProperties()
    {
        return properties;
    }

    public void setProperties( Map<String, Object> properties )
    {
        this.properties = properties;
    }

    @Override
    public String toString()
    {
        return "Facility{" +
            "id='" + uuid + '\'' +
            ", name='" + name + '\'' +
            ", active=" + active +
            ", url='" + href + '\'' +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", coordinates=" + coordinates +
            ", identifiers=" + identifiers +
            ", properties=" + properties +
            '}';
    }
}
