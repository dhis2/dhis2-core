package org.hisp.dhis.trackedentityattributevalue;

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

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.audit.Audit;
import org.hisp.dhis.audit.AuditAttributes;
import org.hisp.dhis.audit.AuditQuery;
import org.hisp.dhis.audit.AuditService;
import org.hisp.dhis.audit.AuditUtils;
import org.hisp.dhis.audit.JdbcAuditRepository;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Slf4j
@Service( "org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService" )
public class DefaultTrackedEntityAttributeValueAuditService
    implements TrackedEntityAttributeValueAuditService
{
    private final JdbcAuditRepository auditRepository;

    private final RenderService renderService;

    public DefaultTrackedEntityAttributeValueAuditService( JdbcAuditRepository auditRepository, RenderService renderService )
    {
        this.auditRepository = auditRepository;
        this.renderService = renderService;
    }

    @Override
    public List<TrackedEntityAttributeValueAudit> getTrackedEntityAttributeValueAudits( List<TrackedEntityAttribute> trackedEntityAttributes,
        List<TrackedEntityInstance> trackedEntityInstances, AuditType auditType )
    {

        AuditAttributes auditAttributes = new AuditAttributes();
        trackedEntityAttributes.forEach( tea -> auditAttributes.put( "trackedEntityAttribute", tea.getUid() ) );
        trackedEntityInstances.forEach( tei -> auditAttributes.put( "trackedEntityInstance", tei.getUid() ) );

        return auditRepository.query( AuditQuery.builder()
        .klass( Sets.newHashSet(TrackedEntityAttributeValue.class.getName() ) )
            .auditAttributes( auditAttributes )
            .auditType( Sets.newHashSet( AuditUtils.mapAuditType( auditType ) ) )
            .build() )
            .stream().filter( audit -> audit != null  )
            .map( audit -> mapAudit( audit ) ).collect( Collectors.toList() );
    }

    @Override
    public List<TrackedEntityAttributeValueAudit> getTrackedEntityAttributeValueAudits( List<TrackedEntityAttribute> trackedEntityAttributes,
        List<TrackedEntityInstance> trackedEntityInstances, AuditType auditType, int first, int max )
    {
        AuditAttributes auditAttributes = new AuditAttributes();
        trackedEntityAttributes.forEach( tea -> auditAttributes.put( "trackedEntityAttribute", tea.getUid() ) );
        trackedEntityInstances.forEach( tei -> auditAttributes.put( "trackedEntityInstance", tei.getUid() ) );

        return auditRepository.query( AuditQuery.builder()
            .klass( Sets.newHashSet(TrackedEntityAttributeValue.class.getName() ) )
            .auditAttributes( auditAttributes )
            .auditType( Sets.newHashSet( AuditUtils.mapAuditType( auditType ) ) )
            .first( first )
            .max( max )
            .build() ).stream().map( audit -> mapAudit( audit ) ).collect( Collectors.toList() );
    }

    @Override
    public int countTrackedEntityAttributeValueAudits( List<TrackedEntityAttribute> trackedEntityAttributes,
        List<TrackedEntityInstance> trackedEntityInstances, AuditType auditType )
    {
        AuditAttributes auditAttributes = new AuditAttributes();
        trackedEntityAttributes.forEach( tea -> auditAttributes.put( "trackedEntityAttribute", tea.getUid() ) );
        trackedEntityInstances.forEach( tei -> auditAttributes.put( "trackedEntityInstance", tei.getUid() ) );

        return auditRepository.count( AuditQuery.builder()
            .klass( Sets.newHashSet(TrackedEntityAttributeValue.class.getName() ) )
            .auditAttributes( auditAttributes )
            .auditType( Sets.newHashSet( AuditUtils.mapAuditType( auditType ) ) )
            .build() );
    }
    
    @Override
    public void deleteTrackedEntityAttributeValueAudits( TrackedEntityInstance trackedEntityInstance )
    {
        AuditAttributes auditAttributes = new AuditAttributes();
        auditAttributes.put( "trackedEntityInstance", trackedEntityInstance.getUid() );
        auditRepository.delete( AuditQuery.builder().auditAttributes( auditAttributes ).build() );
    }

    private TrackedEntityAttributeValueAudit mapAudit( Audit source )
    {
        if ( source == null )
        {
            return null;
        }

        TrackedEntityAttributeValue attributeValue;

        try
        {
            attributeValue = renderService.fromJson( source.getData(), TrackedEntityAttributeValue.class );
        }
        catch ( IOException e )
        {
            log.error( DebugUtils.getStackTrace( e ) );
            throw new RuntimeException( "Failed to get TrackedEntityAttributeValueAudit: " + source.getUid() );
        }

        if ( attributeValue == null )
        {
            return null;
        }

        TrackedEntityAttributeValueAudit attributeValueAudit = new TrackedEntityAttributeValueAudit();

        attributeValueAudit.setAttribute( attributeValue.getAttribute() );
        attributeValueAudit.setAuditType( AuditUtils.mapAuditType( source.getAuditType() ) );
        attributeValueAudit.setCreated( source.getCreatedAt() ) ;
        attributeValueAudit.setEncryptedValue( attributeValue.getEncryptedValue() );
        attributeValueAudit.setValue( attributeValue.getValue() );
        attributeValueAudit.setModifiedBy( source.getCreatedBy() );

        return attributeValueAudit;
    }

}
