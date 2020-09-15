package org.hisp.dhis.dxf2.events.importer.insert.postprocess;

import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.dxf2.events.importer.audit.AbstractEventAuditPostProcessor;

import lombok.Getter;

public class EventInsertAuditPostProcessor extends AbstractEventAuditPostProcessor
{

    @Getter
    private final AuditType auditType = AuditType.CREATE;

}
