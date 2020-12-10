package org.hisp.dhis.tracker.preprocess;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.tracker.bundle.TrackerBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Enrico Colasante
 */
@Service
public class DefaultTrackerPreprocessService
    implements TrackerPreprocessService
{
    private List<BundlePreProcessor> preProcessors = new ArrayList<>();

    @Autowired( required = false )
    public void setPreProcessors( List<BundlePreProcessor> preProcessors )
    {
        this.preProcessors = preProcessors;
    }

    @Override
    public TrackerBundle preprocess( TrackerBundle bundle )
    {
        for ( BundlePreProcessor preProcessor : preProcessors )
        {
            preProcessor.process( bundle );
        }

        return bundle;
    }
}
