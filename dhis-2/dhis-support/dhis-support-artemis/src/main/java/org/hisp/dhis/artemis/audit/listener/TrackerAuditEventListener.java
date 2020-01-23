package org.hisp.dhis.artemis.audit.listener;

import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Conditional( value = AuditEnabledCondition.class )
public class TrackerAuditEventListener
{
    private final AuditManager auditManager;

    public TrackerAuditEventListener( AuditManager auditManager )
    {
        this.auditManager = auditManager;
    }

    @TransactionalEventListener
    public void processAuditEvent( Audit audit )
    {
        System.out.println( "In TrackerAuditEventListener.processAuditEvent" );
        auditManager.send( audit );
    }
}
