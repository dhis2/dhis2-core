package org.hisp.dhis.audit;

/*
 *
 *  Copyright (c) 2004-2018, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

import com.google.common.collect.Lists;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.AuditType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.schema.audit.MetadataAudit;
import org.hisp.dhis.schema.audit.MetadataAuditQuery;
import org.hisp.dhis.schema.audit.MetadataAuditStore;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author Viet Nguyen <viet@dhis2.org>
 */
public class MetadataAuditStoreTest
    extends DhisSpringTest
{
    @Autowired
    private MetadataAuditStore metadataAuditStore;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private RenderService renderService;

    @Test
    public void testQueryMetadataAudit()
    {
        DataElement de = createDataElement( 'A' );
        dataElementService.addDataElement( de );

        MetadataAudit audit = new MetadataAudit();
        audit.setCreatedAt( new Date() );
        audit.setCreatedBy( "userA" );
        audit.setKlass( DataElement.class.getName() );
        audit.setUid( de.getUid() );
        audit.setCode( de.getCode() );
        audit.setType( AuditType.CREATE );
        audit.setValue( renderService.toJsonAsString( de ) );

        metadataAuditStore.save( audit );

        MetadataAuditQuery query = new MetadataAuditQuery();
        query.setKlass( Lists.newArrayList( DataElement.class.getName() ) ) ;
        query.setCreatedBy( "userA" );

        Assert.assertEquals( 1, metadataAuditStore.query( query ).size() );
    }
}
