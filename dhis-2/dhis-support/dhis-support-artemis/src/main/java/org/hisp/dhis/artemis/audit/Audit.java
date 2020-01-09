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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.artemis.Message;
import org.hisp.dhis.artemis.MessageType;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IdentifiableObject;

import java.time.LocalDateTime;

/**
 * Class for Audit messages, mostly compatible with {@link org.hisp.dhis.audit.Audit}
 * but has some additions relevant only to Artemis messages.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
public class Audit implements Message
{
    @JsonProperty
    private final AuditType auditType;

    @JsonProperty
    private AuditScope auditScope;

    @JsonProperty
    private LocalDateTime createdAt;

    @JsonProperty
    private String createdBy;

    @JsonProperty
    private String klass;

    @JsonProperty
    private String uid;

    @JsonProperty
    private String code;

    @JsonProperty
    private AuditAttributes attributes;

    @JsonProperty // TODO remove this from builder
    private Object data;

    @JsonIgnore
    private AuditableEntity auditableEntity;

    /**
     * This constructor is used to create an instance of Audit using the 'Builder'
     * pattern This should be used by a service that needs to create a new Audit
     * entry
     */
    @Builder( builderClassName = "AuditBuilder" )
    public Audit( AuditType auditType, AuditScope auditScope, LocalDateTime createdAt, String createdBy, String klass,
        String uid, String code, AuditAttributes attributes, AuditableEntity auditableEntity )
    {
        this.auditType = auditType;
        this.auditScope = auditScope;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.klass = klass;
        this.uid = uid;
        this.code = code;
        this.attributes = attributes;
        this.auditableEntity = auditableEntity;
    }

    /**
     * This constructor should only be used by Jackson to deserialize an Audit instance from a Json String
     */
    @JsonCreator
    public Audit( @JsonProperty( "auditType" ) AuditType auditType, @JsonProperty( "auditScope" ) AuditScope auditScope,
        @JsonProperty( "createdAt" ) LocalDateTime createdAt, @JsonProperty( "createdBy" ) String createdBy,
        @JsonProperty( "klass" ) String klass, @JsonProperty( "uid" ) String uid, @JsonProperty( "code" ) String code,
        @JsonProperty( "attributes" ) AuditAttributes attributes, @JsonProperty( "data" ) Object data )
    {
        this.auditType = auditType;
        this.auditScope = auditScope;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.klass = klass;
        this.uid = uid;
        this.code = code;
        this.attributes = attributes;
        this.data = data;
    }

    @Override
    public MessageType getMessageType()
    {
        return MessageType.AUDIT;
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
            .code( code )
            .attributes( attributes );

        if ( data instanceof String )
        {
            auditBuilder.data( (String) data );
        }

        return auditBuilder.build();
    }

    public static final class AuditBuilder
    {
        private String klass;
        private String uid;
        private String code;

        public AuditBuilder object( Object o )
        {
            if ( o == null )
            {
                return this;
            }

            klass = o.getClass().getName();

            if ( o instanceof IdentifiableObject )
            {
                uid = ((IdentifiableObject) o).getUid();
                code = ((IdentifiableObject) o).getCode();
            }

            return this;
        }
    }

    String toLog() {
        return "Audit{" +
                "auditType=" + auditType +
                ", auditScope=" + auditScope +
                ", createdAt=" + createdAt +
                ", createdBy='" + createdBy + '\'' +
                ", klass='" + klass + '\'' +
                ", uid='" + uid + '\'' +
                ", code='" + code + '\'' +
                '}';
    }
}
