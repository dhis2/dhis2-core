package org.hisp.dhis.keyjsonvalue;

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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hisp.dhis.common.BaseIdentifiableObject;

/**
 * @author Stian Sandvold
 */
public class KeyJsonValue
    extends BaseIdentifiableObject
{
    /**
     * A namespace represents a collection of keys
     */
    private String namespace;

    /**
     * A key belongs to a namespace, and represent a value
     */
    private String key;

    /**
     * A value referenced by a key and namespace, JSON-formatted data stored as a string.
     */
    private String plainValue;

    /**
     * Whether or not this KeyJsonValue is encrypted or not. Default is false.
     */
    private Boolean encrypted = false;

    /**
     * Encrypted value if encrypted is set to true
     */
    private String encryptedValue;

    /**
     * Temporary variable to hold any new values set during session. Will be made into the correct type
     * when being persisted by the persistence layer (encrypted or plain).
     */
    private String value;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public KeyJsonValue()
    {
    }
    
    public KeyJsonValue( String namespace, String key, String value, Boolean encrypted )
    {
        this.namespace = namespace;
        this.key = key;
        this.value = value;
        this.encrypted = encrypted;
    }

    // -------------------------------------------------------------------------
    // Get and set methods
    // -------------------------------------------------------------------------
    
    @JsonProperty
    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace( String namespace )
    {
        this.namespace = namespace;
    }

    @JsonProperty
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    @JsonProperty
    public String getValue()
    {
        return encrypted ? encryptedValue : plainValue;
    }

    public void setValue( String value )
    {
        this.value = value;
    }

    public boolean isEncrypted()
    {
        return encrypted;
    }

    public void setEncrypted( boolean encrypted )
    {
        this.encrypted = encrypted;
    }

    public String getPlainValue()
    {
        return !this.encrypted && this.value != null ? this.value : this.plainValue;
    }

    public void setPlainValue( String plainValue )
    {
        this.plainValue = plainValue;
    }

    public String getEncryptedValue()
    {
        return this.encrypted && this.value != null ? this.value : this.encryptedValue;
    }

    public void setEncryptedValue( String encryptedValue )
    {
        this.encryptedValue = encryptedValue;
    }
}
