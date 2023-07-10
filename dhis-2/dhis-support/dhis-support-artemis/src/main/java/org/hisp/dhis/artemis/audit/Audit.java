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
package org.hisp.dhis.artemis.audit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import org.hisp.dhis.artemis.MessageType;
import org.hisp.dhis.artemis.SerializableMessage;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IdentifiableObject;

/**
 * Class for Audit messages, mostly compatible with {@link org.hisp.dhis.audit.Audit} but has some
 * additions relevant only to Artemis messages.
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Data
@Builder(builderClassName = "AuditBuilder")
@JsonDeserialize(builder = Audit.AuditBuilder.class)
public class Audit implements SerializableMessage {
  @JsonProperty private final AuditType auditType;

  @JsonProperty private AuditScope auditScope;

  @JsonProperty private LocalDateTime createdAt;

  @JsonProperty private String createdBy;

  @JsonProperty private String klass;

  @JsonProperty private String uid;

  @JsonProperty private String code;

  @JsonProperty @Builder.Default private AuditAttributes attributes = new AuditAttributes();

  /**
   * This property holds the serialized Audited entity: it should not be used during the
   * construction of an instance of this object
   */
  @JsonProperty private Object data;

  /**
   * This property should be used when constructing an Audit instance to send to the Audit
   * sub-system The AuditableEntity object allows the AuditManager to serialize the audited entity
   * only if needed
   */
  @JsonIgnore private AuditableEntity auditableEntity;

  @Override
  public MessageType getMessageType() {
    return MessageType.AUDIT;
  }

  /**
   * Converts the AMQP Audit object to a DAO Audit object. The data property will only be set if
   * data == string.
   *
   * <p>TODO should we just do .toString() if its not a string objects?
   *
   * @return DAO Audit object with data (if data is string).
   */
  public org.hisp.dhis.audit.Audit toAudit() {
    org.hisp.dhis.audit.Audit.AuditBuilder auditBuilder =
        org.hisp.dhis.audit.Audit.builder()
            .auditType(auditType)
            .auditScope(auditScope)
            .createdAt(createdAt)
            .createdBy(createdBy)
            .klass(klass)
            .uid(uid)
            .code(code)
            .attributes(attributes);

    if (data instanceof String) {
      auditBuilder.data((String) data);
    }

    return auditBuilder.build();
  }

  @JsonPOJOBuilder(withPrefix = "")
  public static final class AuditBuilder {
    private String klass;

    private String uid;

    private String code;

    public AuditBuilder object(Object o) {
      if (o == null) {
        return this;
      }

      klass = o.getClass().getName();

      if (o instanceof IdentifiableObject) {
        uid = ((IdentifiableObject) o).getUid();
        code = ((IdentifiableObject) o).getCode();
      }

      return this;
    }
  }

  String toLog() {
    return "Audit{"
        + "auditType="
        + auditType
        + ", auditScope="
        + auditScope
        + ", createdAt="
        + createdAt
        + ", createdBy='"
        + createdBy
        + '\''
        + ", klass='"
        + klass
        + '\''
        + ", uid='"
        + uid
        + '\''
        + ", code='"
        + code
        + '\''
        + '}';
  }
}
