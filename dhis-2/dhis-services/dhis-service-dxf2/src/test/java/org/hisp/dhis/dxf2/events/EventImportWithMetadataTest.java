package org.hisp.dhis.dxf2.events;

import com.vividsolutions.jts.io.ParseException;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdSchemes;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.event.EventService;
import org.hisp.dhis.dxf2.events.event.Events;
import org.hisp.dhis.dxf2.events.event.csv.CsvEventService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.dxf2.metadata.MetadataImportParams;
import org.hisp.dhis.dxf2.metadata.MetadataImportService;
import org.hisp.dhis.dxf2.metadata.feedback.ImportReport;
import org.hisp.dhis.dxf2.metadata.objectbundle.ObjectBundleMode;
import org.hisp.dhis.feedback.Status;
import org.hisp.dhis.importexport.ImportStrategy;
import org.hisp.dhis.render.RenderFormat;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.user.UserService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EventImportWithMetadataTest extends DhisSpringTest
{

    @Autowired
    private RenderService _renderService;

    @Autowired
    private CsvEventService csvEventService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserService _userService;

    @Autowired
    private MetadataImportService importService;

    @Override
    protected void setUpTest()
        throws Exception
    {
        renderService = _renderService;
        userService = _userService;
    }

    @Test
    public void shouldImportCsv()
        throws IOException,
        ParseException
    {
        Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource("dxf2/import/create_program_stages.json").getInputStream(), RenderFormat.JSON );

        MetadataImportParams params = new MetadataImportParams();
        params.setImportMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setObjects( metadata );
        params.setSkipSharing( true );

        ImportReport report = importService.importMetadata( params );
        assertEquals( Status.OK, report.getStatus() );

        ImportOptions importOptions = new ImportOptions();
        IdSchemes idSchemes = new IdSchemes();
        idSchemes.setDataElementIdScheme( "UID" );
        idSchemes.setOrgUnitIdScheme( "CODE" );
        idSchemes.setProgramStageIdScheme( "UID" );
        idSchemes.setIdScheme( "CODE" );

        importOptions.setIdSchemes( idSchemes );

        Events events = csvEventService
            .readEvents( new ClassPathResource( "dxf2/import/events-5b.csv" ).getInputStream(), true );
        ImportSummaries importSummaries = eventService.addEvents( events.getEvents(), importOptions, null );

        assertEquals( ImportStatus.SUCCESS, importSummaries.getStatus() );
    }
}
