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
package org.hisp.dhis.tracker.config;

import static org.mockito.Mockito.mock;

import org.hibernate.SessionFactory;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.fileresource.FileResourceService;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.ProgramStore;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.programrule.engine.ProgramRuleEngine;
import org.hisp.dhis.query.QueryService;
import org.hisp.dhis.relationship.RelationshipService;
import org.hisp.dhis.relationship.RelationshipStore;
import org.hisp.dhis.render.DefaultRenderService;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.reservedvalue.DefaultReservedValueService;
import org.hisp.dhis.reservedvalue.ReservedValueService;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceStore;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerService;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwnerStore;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueAuditService;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValueService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityCommentStore;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueAuditService;
import org.hisp.dhis.tracker.job.TrackerMessageManager;
import org.hisp.dhis.tracker.sideeffect.NotificationSideEffectHandlerService;
import org.hisp.dhis.tracker.sideeffect.RuleEngineSideEffectHandlerService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration( "trackerTestConfig" )
@Profile( "test-h2" )
public class TrackerTestConfig
{

    @Bean
    public RenderService renderService()
    {
        return new DefaultRenderService( jsonMapper(), xmlMapper(), schemaService() );
    }

    @Bean
    @Qualifier( "jsonMapper" )
    public ObjectMapper jsonMapper()
    {
        return JacksonObjectMapperConfig.staticJsonMapper();
    }

    @Bean
    @Qualifier( "xmlMapper" )
    public ObjectMapper xmlMapper()
    {
        return JacksonObjectMapperConfig.staticXmlMapper();
    }

    @Bean
    public UserService userService()
    {
        return mock( UserService.class );
    }

    @Bean
    public IdentifiableObjectManager manager()
    {
        return mock( IdentifiableObjectManager.class );
    }

    @Bean
    public SessionFactory sessionFactory()
    {
        return mock( SessionFactory.class );
    }

    @Bean
    public ReservedValueService reservedValueService()
    {
        return new DefaultReservedValueService( null, null, null );
    }

    @Bean
    public TrackedEntityAttributeValueAuditService trackedEntityAttributeValueAuditService()
    {
        return mock( TrackedEntityAttributeValueAuditService.class );
    }

    @Bean
    public TrackedEntityCommentService trackedEntityCommentService()
    {
        return mock( TrackedEntityCommentService.class );
    }

    @Bean
    public TrackedEntityProgramOwnerService trackedEntityProgramOwnerService()
    {
        return mock( TrackedEntityProgramOwnerService.class );
    }

    @Bean
    public TrackedEntityDataValueAuditService trackedEntityDataValueAuditService()
    {
        return mock( TrackedEntityDataValueAuditService.class );
    }

    @Bean( "serviceTrackerRuleEngine" )
    public ProgramRuleEngine programRuleEngine()
    {
        return mock( ProgramRuleEngine.class );
    }

    @Bean
    public TrackedEntityInstanceService trackedEntityInstanceService()
    {
        return mock( TrackedEntityInstanceService.class );
    }

    @Bean
    public ProgramInstanceService programInstanceService()
    {
        return mock( ProgramInstanceService.class );
    }

    @Bean
    public ProgramStageInstanceService programStageInstanceService()
    {
        return mock( ProgramStageInstanceService.class );
    }

    @Bean
    public RelationshipService relationshipService()
    {
        return mock( RelationshipService.class );
    }

    @Bean
    public NotificationSideEffectHandlerService notificationSideEffectHandlerService()
    {
        return new NotificationSideEffectHandlerService( null );
    }

    @Bean
    public RuleEngineSideEffectHandlerService ruleEngineSideEffectHandlerService()
    {
        return new RuleEngineSideEffectHandlerService( null );
    }

    @Bean
    public FileResourceService fileResourceService()
    {
        return mock( FileResourceService.class );
    }

    @Bean
    public SystemSettingManager systemSettingManager()
    {
        return mock( SystemSettingManager.class );
    }

    @Bean
    public I18nManager i18nManager()
    {
        return mock( I18nManager.class );
    }

    @Bean
    public AclService aclService()
    {
        return mock( AclService.class );
    }

    @Bean
    public TrackerOwnershipManager trackerOwnershipManager()
    {
        return mock( TrackerOwnershipManager.class );
    }

    @Bean
    public OrganisationUnitService organisationUnitService()
    {
        return mock( OrganisationUnitService.class );
    }

    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider()
    {
        return mock( DhisConfigurationProvider.class );
    }

    @Bean
    public CurrentUserService currentUserService()
    {
        return mock( CurrentUserService.class );
    }

    @Bean
    public Notifier notifier()
    {
        return mock( Notifier.class );
    }

    @Bean
    public ProgramRuleService programRuleService()
    {
        return mock( ProgramRuleService.class );
    }

    @Bean
    public TrackerMessageManager trackerMessageManager()
    {
        return mock( TrackerMessageManager.class );
    }

    @Bean
    public CategoryService categoryService()
    {
        return mock( CategoryService.class );
    }

    @Bean
    public PeriodStore periodStore()
    {
        return mock( PeriodStore.class );
    }

    @Bean
    public ProgramInstanceStore programInstanceStore()
    {
        return mock( ProgramInstanceStore.class );
    }

    @Bean
    public ProgramStageInstanceStore programStageInstanceStore()
    {
        return mock( ProgramStageInstanceStore.class );
    }

    @Bean
    public TrackedEntityInstanceStore trackedEntityInstanceStore()
    {
        return mock( TrackedEntityInstanceStore.class );
    }

    @Bean
    public RelationshipStore relationshipStore()
    {
        return mock( RelationshipStore.class );
    }

    @Bean
    public ProgramStore programStore()
    {
        return mock( ProgramStore.class );
    }

    @Bean
    public JdbcTemplate jdbcTemplate()
    {
        return mock( JdbcTemplate.class );
    }

    @Bean
    public TrackedEntityProgramOwnerStore trackedEntityProgramOwnerStore()
    {
        return mock( TrackedEntityProgramOwnerStore.class );
    }

    @Bean
    public TrackedEntityAttributeService trackedEntityAttributeService()
    {
        return mock( TrackedEntityAttributeService.class );
    }

    @Bean
    public TrackedEntityAttributeValueService trackedEntityAttributeValueService()
    {
        return mock( TrackedEntityAttributeValueService.class );
    }

    @Bean
    public SchemaService schemaService()
    {
        return mock( SchemaService.class );
    }

    @Bean
    public QueryService queryService()
    {
        return mock( QueryService.class );
    }

    @Bean
    public TrackedEntityCommentStore trackedEntityCommentStore()
    {
        return mock( TrackedEntityCommentStore.class );
    }
}
