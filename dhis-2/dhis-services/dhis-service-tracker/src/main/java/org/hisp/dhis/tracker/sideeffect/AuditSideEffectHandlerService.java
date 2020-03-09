package org.hisp.dhis.tracker.sideeffect;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.collect.ImmutableMap;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.artemis.audit.AuditableEntity;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.job.TrackerSideEffectDataBundle;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author Zubair Asghar
 */

@Service
public class AuditSideEffectHandlerService implements SideEffectHandlerService
{
    private static final ImmutableMap<TrackerImportStrategy, AuditType> TYPE_MAPPER =
        new ImmutableMap.Builder<TrackerImportStrategy, AuditType>()
        .put( TrackerImportStrategy.CREATE, AuditType.CREATE )
        .put( TrackerImportStrategy.UPDATE, AuditType.UPDATE )
        .put( TrackerImportStrategy.DELETE, AuditType.DELETE )
        .build();

    private final AuditManager auditManager;

    public AuditSideEffectHandlerService( AuditManager auditManager )
    {
        this.auditManager = auditManager;
    }

    @Override
    public void handleSideEffect( TrackerSideEffectDataBundle sideEffectDataBundle )
    {
        AuditType auditType = TYPE_MAPPER.getOrDefault( sideEffectDataBundle.getImportStrategy(), AuditType.READ );

        Audit audit = Audit.builder()
            .auditType( auditType )
            .auditScope( AuditScope.TRACKER )
            .createdAt( LocalDateTime.now() )
            .createdBy( sideEffectDataBundle.getAccessedBy() )
            .klass( sideEffectDataBundle.getKlass().getName() )
            .auditableEntity( new AuditableEntity( sideEffectDataBundle ) )
            .build();

        auditManager.send( audit );
    }
}
