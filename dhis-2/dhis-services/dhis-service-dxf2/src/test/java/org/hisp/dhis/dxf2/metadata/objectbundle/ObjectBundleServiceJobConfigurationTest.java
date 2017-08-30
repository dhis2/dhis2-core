package org.hisp.dhis.dxf2.metadata.objectbundle;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.render.RenderService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * Created by henninghakonsen on 30/08/2017.
 * Project: dhis-2.
 */
public class ObjectBundleServiceJobConfigurationTest
    extends DhisSpringTest
{
    //@Autowired
   // private ObjectBundleService objectBundleService;

    @Autowired
    private IdentifiableObjectManager manager;

    @Autowired
    private RenderService _renderService;

    @Override
    protected void setUpTest() throws Exception
    {
        renderService = _renderService;
    }

    @Test
    public void testCreateJobs() throws IOException
    {
        /*Map<Class<? extends IdentifiableObject>, List<IdentifiableObject>> metadata = renderService.fromMetadata(
            new ClassPathResource( "dxf2/create_jobs.json" ).getInputStream(), RenderFormat.JSON );

        ObjectBundleParams params = new ObjectBundleParams();
        params.setObjectBundleMode( ObjectBundleMode.COMMIT );
        params.setImportStrategy( ImportStrategy.CREATE );
        params.setAtomicMode( AtomicMode.NONE );
        params.setObjects( metadata );

        //ObjectBundle bundle = objectBundleService.create( params );
        //objectBundleService.commit( bundle );

        List<JobConfiguration> jobConfigurations = manager.getAll( JobConfiguration.class );
        System.out.println("jobs: " + jobConfigurations );
        assertEquals( 3, jobConfigurations.size() );*/
    }
}
