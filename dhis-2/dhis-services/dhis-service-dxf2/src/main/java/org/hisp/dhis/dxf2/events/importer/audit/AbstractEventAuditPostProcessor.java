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
package org.hisp.dhis.dxf2.events.importer.audit;

import java.time.LocalDateTime;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.importer.EventImporterUserService;
import org.hisp.dhis.dxf2.events.importer.Processor;
import org.hisp.dhis.dxf2.events.importer.context.WorkContext;
import org.hisp.dhis.dxf2.events.importer.mapper.ProgramStageInstanceMapper;
import org.hisp.dhis.program.ProgramStageInstance;

/**
 * This is the base implementation for AuditProcessor. Insert, Update and Delete
 * EventAuditProcessors (which are implementation of this class) will provide a value for AuditType
 * and will inherit shared process logic.
 *
 * @author Giuseppe Nespolino <g.nespolino@gmail.com>
 */
public abstract class AbstractEventAuditPostProcessor implements Processor {

  @Override
  public void process(final Event event, final WorkContext ctx) {
    final AuditManager auditManager = ctx.getServiceDelegator().getAuditManager();
    final EventImporterUserService eventImporterUserService =
        ctx.getServiceDelegator().getEventImporterUserService();
    final ProgramStageInstanceMapper programStageInstanceMapper =
        new ProgramStageInstanceMapper(ctx);
    final ProgramStageInstance programStageInstance = programStageInstanceMapper.map(event);

    auditManager.send(
        Audit.builder()
            .auditType(getAuditType())
            .auditScope(AuditScope.TRACKER)
            .createdAt(LocalDateTime.now())
            .createdBy(eventImporterUserService.getAuditUsername())
            .object(programStageInstance)
            .auditableEntity(new AuditableEntity(ProgramStageInstance.class, programStageInstance))
            .build());
  }

  protected abstract AuditType getAuditType();
}
