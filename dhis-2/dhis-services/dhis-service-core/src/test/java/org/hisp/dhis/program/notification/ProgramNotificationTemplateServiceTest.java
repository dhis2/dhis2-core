package org.hisp.dhis.program.notification;

import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Zubair Asghar
 */
public class ProgramNotificationTemplateServiceTest extends IntegrationTestBase
{
    private Program program;

    private ProgramStage programStage;

    private ProgramNotificationTemplate pnt1;

    private ProgramNotificationTemplate pnt2;

    private ProgramNotificationTemplate pnt3;

    private OrganisationUnit organisationUnit;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramNotificationTemplateService programNotificationTemplateService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Override
    protected void setUpTest()
            throws Exception
    {
        organisationUnit = createOrganisationUnit( 'O' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        program = createProgram( 'P' );
        program.setAutoFields();
        program.setUid( "P_UID_1" );
        programService.addProgram( program );

        programStage = createProgramStage( 'S', program );
        programStage.setAutoFields();
        programStage.setUid( "PS_UID_1" );
        programStageService.saveProgramStage( programStage );

        pnt1 = createProgramNotificationTemplate( "test1", 1, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.USER_GROUP );
        pnt1.setAutoFields();
        pnt1.setUid( "PNT_UID_1" );
        pnt2 = createProgramNotificationTemplate( "test2", 1, NotificationTrigger.COMPLETION, ProgramNotificationRecipient.DATA_ELEMENT );
        pnt2.setAutoFields();
        pnt2.setUid( "PNT_UID_2" );
        pnt3 = createProgramNotificationTemplate( "test3", 1, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE );
        pnt3.setAutoFields();
        pnt3.setUid( "PNT_UID_3" );

        programNotificationTemplateService.save(pnt1);
        programNotificationTemplateService.save(pnt2);
        programNotificationTemplateService.save(pnt3);

        program.getNotificationTemplates().add(pnt1);
        program.getNotificationTemplates().add(pnt2);
    }

    @Test
    public void testGetProgramNotificationTemplates()
    {
        ProgramNotificationTemplateParam param = ProgramNotificationTemplateParam.builder().program( program ).build();

        List<ProgramNotificationTemplate> templates = programNotificationTemplateService.getProgramNotificationTemplates( param );

        assertFalse(  templates.isEmpty() );
        assertEquals( 2, templates.size() );
        assertTrue( templates.contains( pnt1 ) );
        assertTrue( templates.contains( pnt2 ) );
        assertFalse( templates.contains( pnt3 ) );
    }
}
