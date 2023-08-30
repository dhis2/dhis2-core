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
package org.hisp.dhis.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.time.LocalDateTime;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class AuditTest {

  @Test
  void testStaticConstructor() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    Audit audit =
        Audit.builder()
            .auditType(AuditType.CREATE)
            .auditScope(AuditScope.AGGREGATE)
            .createdAt(LocalDateTime.of(2019, 5, 5, 12, 30))
            .createdBy("test-user")
            .klass(DataElement.class.getName())
            .uid(uid)
            .code(code)
            .data("{}")
            .build();
    assertEquals(AuditType.CREATE, audit.getAuditType());
    assertEquals(AuditScope.AGGREGATE, audit.getAuditScope());
    assertEquals(LocalDateTime.of(2019, 5, 5, 12, 30), audit.getCreatedAt());
    assertEquals("test-user", audit.getCreatedBy());
    assertEquals(DataElement.class.getName(), audit.getKlass());
    assertEquals(uid, audit.getUid());
    assertEquals(code, audit.getCode());
    assertEquals("{}", audit.getData());
  }

  @Test
  void testAuditQueryBuilder() {
    String uid = CodeGenerator.generateUid();
    String code = CodeGenerator.generateUid();
    LocalDateTime dateFrom = LocalDateTime.of(2010, 4, 6, 12, 0, 0);
    LocalDateTime dateTo = dateFrom.plusYears(4);
    // TODO should we add bean validation in AuditQuery so we know the from
    // is before to
    assertTrue(dateFrom.isBefore(dateTo));
    AuditQuery query =
        AuditQuery.builder()
            .klass(Sets.newHashSet(DataElement.class.getName()))
            .uid(Sets.newHashSet(uid))
            .code(Sets.newHashSet(code))
            .range(AuditQuery.range(dateFrom, dateTo))
            .build();
    assertEquals(Sets.newHashSet(DataElement.class.getName()), query.getKlass());
    assertEquals(Sets.newHashSet(uid), query.getUid());
    assertEquals(Sets.newHashSet(code), query.getCode());
    assertEquals(dateFrom, query.getRange().getFrom());
    assertEquals(dateTo, query.getRange().getTo());
  }
}
