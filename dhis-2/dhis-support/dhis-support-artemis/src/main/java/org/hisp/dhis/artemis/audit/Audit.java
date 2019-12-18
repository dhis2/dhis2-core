package org.hisp.dhis.artemis.audit;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hisp.dhis.artemis.Message;
import org.hisp.dhis.artemis.MessageType;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.common.IdentifiableObject;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@JacksonXmlRootElement( localName = "audit", namespace = DxfNamespaces.DXF_2_0 )
public class Audit implements Message
{
    private AuditType auditType;

    private AuditScope auditScope;

    private LocalDateTime createdAt;

    private String createdBy;

    private String klass;

    private String uid;

    private String code;

    private Object data;

    public Audit()
    {
    }

    public Audit( AuditType auditType, AuditScope auditScope, LocalDateTime createdAt, String createdBy )
    {
        this.auditType = auditType;
        this.auditScope = auditScope;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    // TODO use lombok @Builder
    public static AuditBuilder builder()
    {
        return new AuditBuilder();
    }

    @Override
    public MessageType getMessageType()
    {
        return MessageType.AUDIT;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AuditType getAuditType()
    {
        return auditType;
    }

    public void setAuditType( AuditType auditType )
    {
        this.auditType = auditType;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public AuditScope getAuditScope()
    {
        return auditScope;
    }

    public void setAuditScope( AuditScope auditScope )
    {
        this.auditScope = auditScope;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt( LocalDateTime createdAt )
    {
        this.createdAt = createdAt;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCreatedBy()
    {
        return createdBy;
    }

    public void setCreatedBy( String createdBy )
    {
        this.createdBy = createdBy;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getKlass()
    {
        return klass;
    }

    public void setKlass( String klass )
    {
        this.klass = klass;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getUid()
    {
        return uid;
    }

    public void setUid( String uid )
    {
        this.uid = uid;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public String getCode()
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public Object getData()
    {
        return data;
    }

    public void setData( Object data )
    {
        this.data = data;
    }

    public static final class AuditBuilder
    {
        private AuditType auditType;

        private AuditScope auditScope;

        private LocalDateTime createdAt = LocalDateTime.now();

        private String createdBy;

        private Class<?> klass;

        private String uid;

        private String code;

        private Object data;

        private AuditBuilder()
        {
        }

        public AuditBuilder withAuditType( AuditType auditType )
        {
            this.auditType = auditType;
            return this;
        }

        public AuditBuilder withAuditScope( AuditScope auditScope )
        {
            this.auditScope = auditScope;
            return this;
        }

        public AuditBuilder withCreatedAt( LocalDateTime createdAt )
        {
            this.createdAt = createdAt;
            return this;
        }

        public AuditBuilder withCreatedBy( String createdBy )
        {
            this.createdBy = createdBy;
            return this;
        }

        public AuditBuilder withObject( Object o )
        {
            if ( o == null )
            {
                return this;
            }

            klass = o.getClass();

            if ( o instanceof IdentifiableObject )
            {
                uid = ((IdentifiableObject) o).getUid();
                code = ((IdentifiableObject) o).getCode();
            }

            return this;
        }

        public AuditBuilder withClass( Class<?> klass )
        {
            this.klass = klass;
            return this;
        }

        public AuditBuilder withUid( String uid )
        {
            this.uid = uid;
            return this;
        }

        public AuditBuilder withCode( String code )
        {
            this.code = code;
            return this;
        }

        public AuditBuilder withData( Object data )
        {
            this.data = data;
            return this;
        }

        public Audit build()
        {
            Assert.notNull( auditType, "AuditType is required." );
            Assert.notNull( auditScope, "AuditScope is required." );
            Assert.notNull( createdAt, "CreatedAt is required." );
            Assert.notNull( createdBy, "CreatedBy is required." );

            Audit audit = new Audit( auditType, auditScope, createdAt, createdBy );

            audit.setKlass( klass.getName() );
            audit.setUid( uid );
            audit.setCode( code );
            audit.setData( data );

            return audit;
        }
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

        Audit audit = (Audit) o;

        return auditType == audit.auditType && auditScope == audit.auditScope
            && Objects.equals( createdAt, audit.createdAt ) && Objects.equals( createdBy, audit.createdBy )
            && Objects.equals( klass, audit.klass ) && Objects.equals( uid, audit.uid )
            && Objects.equals( code, audit.code ) && Objects.equals( data, audit.data );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( auditType, auditScope, createdAt, createdBy, klass, uid, code, data );
    }

    @Override
    public String toString()
    {
        return "Audit{" +
            "auditType=" + auditType +
            ", auditScope=" + auditScope +
            ", createdAt=" + createdAt +
            ", createdBy='" + createdBy + '\'' +
            ", klass='" + klass + '\'' +
            ", uid='" + uid + '\'' +
            ", code='" + code + '\'' +
            ", data=" + data +
            '}';
    }

    /**
     * Converts the AMQP Audit object to a DAO Audit object.
     * The data property will only be set if data == string.
     * <p>
     * TODO should we just do .toString() if its not a string objects?
     *
     * @return DAO Audit object with data (if data is string).
     */
    public org.hisp.dhis.audit.Audit toAudit()
    {
        org.hisp.dhis.audit.Audit.AuditBuilder auditBuilder = org.hisp.dhis.audit.Audit.builder()
            .auditType( auditType )
            .auditScope( auditScope )
            .createdAt( createdAt )
            .createdBy( createdBy )
            .klass( klass )
            .uid( uid )
            .code( this.code );

        if ( data instanceof String )
        {
            auditBuilder.data( (String) data );
        }

        return auditBuilder.build();
    }
}
