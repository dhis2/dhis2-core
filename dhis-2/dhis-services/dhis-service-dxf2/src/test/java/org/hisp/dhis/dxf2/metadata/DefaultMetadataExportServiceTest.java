/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.dxf2.metadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.fieldfilter.FieldFilterParams;
import org.hisp.dhis.fieldfilter.FieldFilterService;
import org.hisp.dhis.node.types.CollectionNode;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.ProgramRuleVariableService;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.user.CurrentUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

/**
 * Unit tests for {@link DefaultMetadataExportService}.
 *
 * @author Volker Schmidt
 */
@ExtendWith( MockitoExtension.class )
class DefaultMetadataExportServiceTest
{
    @Mock
    private SchemaService schemaService;

    @Mock
    private QueryService queryService;

    @Mock
    private FieldFilterService fieldFilterService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ProgramRuleService programRuleService;

    @Mock
    private ProgramRuleVariableService programRuleVariableService;

    @Mock
    private SystemService systemService;

    @InjectMocks
    private DefaultMetadataExportService service;

    @Test
    void getMetadataWithDependenciesAsNodeSharing()
    {
        Attribute attribute = new Attribute();
        SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata = new SetMap<>();
        metadata.put( Attribute.class, new HashSet<>() );

        service = Mockito.spy( service );
        Mockito.when( service.getMetadataWithDependencies( Mockito.eq( attribute ) ) ).thenReturn( metadata );

        Mockito.when( fieldFilterService.toCollectionNode( Mockito.eq( Attribute.class ), Mockito.any() ) )
            .then( (Answer<CollectionNode>) invocation -> {
                FieldFilterParams fieldFilterParams = invocation.getArgument( 1 );
                Assertions.assertFalse( fieldFilterParams.getSkipSharing() );
                return new CollectionNode( "test" );
            } );

        MetadataExportParams params = new MetadataExportParams();
        service.getMetadataWithDependenciesAsNode( attribute, params );

        Mockito.verify( fieldFilterService, Mockito.only() ).toCollectionNode( Mockito.eq( Attribute.class ),
            Mockito.any() );
    }

    @Test
    void getMetadataWithDependenciesAsNodeSkipSharing()
    {
        Attribute attribute = new Attribute();
        SetMap<Class<? extends IdentifiableObject>, IdentifiableObject> metadata = new SetMap<>();
        metadata.put( Attribute.class, new HashSet<>() );

        service = Mockito.spy( service );
        Mockito.when( service.getMetadataWithDependencies( Mockito.eq( attribute ) ) ).thenReturn( metadata );

        Mockito.when( fieldFilterService.toCollectionNode( Mockito.eq( Attribute.class ), Mockito.any() ) )
            .then( (Answer<CollectionNode>) invocation -> {
                FieldFilterParams fieldFilterParams = invocation.getArgument( 1 );
                Assertions.assertTrue( fieldFilterParams.getSkipSharing() );
                return new CollectionNode( "test" );
            } );

        MetadataExportParams params = new MetadataExportParams();
        params.setSkipSharing( true );
        service.getMetadataWithDependenciesAsNode( attribute, params );

        Mockito.verify( fieldFilterService, Mockito.only() ).toCollectionNode( Mockito.eq( Attribute.class ),
            Mockito.any() );
    }

    @Test
    void getParamsFromMapIncludedSecondary()
    {
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "jobConfigurations" ) ) )
            .thenReturn( new Schema( JobConfiguration.class, "jobConfiguration", "jobConfigurations" ) );
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "options" ) ) )
            .thenReturn( new Schema( Option.class, "option", "options" ) );

        final Map<String, List<String>> params = new HashMap<>();
        params.put( "jobConfigurations", Collections.singletonList( "true" ) );
        params.put( "options", Collections.singletonList( "true" ) );

        MetadataExportParams exportParams = service.getParamsFromMap( params );
        Assertions.assertTrue( exportParams.getClasses().contains( JobConfiguration.class ) );
        Assertions.assertTrue( exportParams.getClasses().contains( Option.class ) );
    }

    @Test
    void getParamsFromMapNotIncludedSecondary()
    {
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "jobConfigurations" ) ) )
            .thenReturn( new Schema( JobConfiguration.class, "jobConfiguration", "jobConfigurations" ) );
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "options" ) ) )
            .thenReturn( new Schema( Option.class, "option", "options" ) );

        final Map<String, List<String>> params = new HashMap<>();
        params.put( "jobConfigurations", Arrays.asList( "true", "false" ) );
        params.put( "options", Collections.singletonList( "true" ) );

        MetadataExportParams exportParams = service.getParamsFromMap( params );
        Assertions.assertFalse( exportParams.getClasses().contains( JobConfiguration.class ) );
        Assertions.assertTrue( exportParams.getClasses().contains( Option.class ) );
    }

    @Test
    void getParamsFromMapNoSecondary()
    {
        Mockito.when( schemaService.getSchemaByPluralName( Mockito.eq( "options" ) ) )
            .thenReturn( new Schema( Option.class, "option", "options" ) );

        final Map<String, List<String>> params = new HashMap<>();
        params.put( "options", Collections.singletonList( "true" ) );

        MetadataExportParams exportParams = service.getParamsFromMap( params );
        Assertions.assertFalse( exportParams.getClasses().contains( JobConfiguration.class ) );
        Assertions.assertTrue( exportParams.getClasses().contains( Option.class ) );
    }
}