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

public abstract class AbstractEventAuditPostProcessor implements Processor
{

    @Override
    public void process( final Event event, final WorkContext ctx )
    {
        final AuditManager auditManager = ctx.getServiceDelegator().getAuditManager();
        final EventImporterUserService eventImporterUserService = ctx.getServiceDelegator()
            .getEventImporterUserService();
        final ProgramStageInstanceMapper programStageInstanceMapper = new ProgramStageInstanceMapper( ctx );
        final ProgramStageInstance programStageInstance = programStageInstanceMapper.map( event );

        auditManager.send( Audit.builder()
            .auditType( getAuditType() )
            .auditScope( AuditScope.TRACKER )
            .createdAt( LocalDateTime.now() )
            .createdBy( eventImporterUserService.getAuditUsername() )
            .object( programStageInstance )
            .auditableEntity( new AuditableEntity( programStageInstance ) )
            .build() );
    }

    protected abstract AuditType getAuditType();

}
