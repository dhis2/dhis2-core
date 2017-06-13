package org.hisp.dhis.webapi.webdomain;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Lars Helge Overland
 */
public class GeoFeature
{
    public static final int TYPE_POINT = 1;
    public static final int TYPE_POLYGON = 2;
    
    /**
     * Identifier.
     */
    private String id;

    /**
     * Code identifier.
     */
    private String code;

    /**
     * Name.
     */
    private String na;
    
    /**
     * Has coordinates down.
     */
    private boolean hcd;
    
    /**
     * Has coordinates up.
     */
    private boolean hcu;
    
    /**
     * Level.
     */
    private int le;
    
    /**
     * Parent graph.
     */
    private String pg;
    
    /**
     * Parent identifier.
     */
    private String pi;
    
    /**
     * Parent name.
     */
    private String pn;
    
    /**
     * Feature type.
     */
    private int ty;
    
    /**
     * Coordinates.
     */
    private String co;

    /**
     * Dimensions and dimension items.
     */
    private Map<String, String> dimensions = new HashMap<>();
    
    public GeoFeature()
    {
    }

    //--------------------------------------------------------------------------
    // Getters and setters
    //--------------------------------------------------------------------------

    @JsonProperty
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @JsonProperty
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @JsonProperty
    public String getNa()
    {
        return na;
    }

    public void setNa( String na )
    {
        this.na = na;
    }

    @JsonProperty
    public boolean isHcd()
    {
        return hcd;
    }

    public void setHcd( boolean hcd )
    {
        this.hcd = hcd;
    }

    @JsonProperty
    public boolean isHcu()
    {
        return hcu;
    }

    public void setHcu( boolean hcu )
    {
        this.hcu = hcu;
    }

    @JsonProperty
    public int getLe()
    {
        return le;
    }

    public void setLe( int le )
    {
        this.le = le;
    }

    @JsonProperty
    public String getPg()
    {
        return pg;
    }

    public void setPg( String pg )
    {
        this.pg = pg;
    }

    @JsonProperty
    public String getPi()
    {
        return pi;
    }

    public void setPi( String pi )
    {
        this.pi = pi;
    }

    @JsonProperty
    public String getPn()
    {
        return pn;
    }

    public void setPn( String pn )
    {
        this.pn = pn;
    }

    @JsonProperty
    public int getTy()
    {
        return ty;
    }

    public void setTy( int ty )
    {
        this.ty = ty;
    }

    @JsonProperty
    public String getCo()
    {
        return co;
    }

    public void setCo( String co )
    {
        this.co = co;
    }

    @JsonProperty
    public Map<String, String> getDimensions()
    {
        return dimensions;
    }

    public void setDimensions( Map<String, String> dimensions )
    {
        this.dimensions = dimensions;
    }
}
