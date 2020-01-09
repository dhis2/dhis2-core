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

package org.hisp.dhis.artemis.audit.configuration;

import java.util.HashMap;
import java.util.Map;

import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;

/**
 * @author Luciano Fiandesio
 */
@Component
public class AuditMatrix
{
    private static final Map<AuditType, Boolean> ALL_ENABLED = ImmutableMap.<AuditType, Boolean> builder()
        .put( AuditType.CREATE, true ).put( AuditType.UPDATE, true ).put( AuditType.READ, true )
        .put( AuditType.SEARCH, true ).build();

    private Map<AuditScope, Map<AuditType, Boolean>> matrix;

    public AuditMatrix()
    {
        // TODO initialize this matrix with real configuration data
        matrix = new HashMap<>();

        matrix.put( AuditScope.METADATA, ALL_ENABLED );
    }

    public boolean isEnabled( Audit audit )
    {
        return matrix.get( audit.getAuditScope() ).getOrDefault( audit.getAuditType(), true );
    }

    public boolean isEnabled( AuditScope auditScope, AuditType auditType )
    {
        return matrix.get( auditScope ).getOrDefault( auditType, true );
    }

}
