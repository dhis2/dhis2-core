package org.hisp.dhis.tracker.preheat.supplier;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.tracker.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.preheat.TrackerPreheatParams;
import org.junit.Test;

/**
 * @author Luciano Fiandesio
 */
public class PreheatClassScannerTest
{

    @Test
    public void verifyScanSupplierRespectsOrderAnnotation()
    {
        final List<String> suppliers = new PreheatClassScanner().scanSuppliers();

        assertTrue(
            suppliers.indexOf( PreheatClassScannerTest.class.getSimpleName() + "$" + ZSupplier.class.getSimpleName() ) <
            suppliers.indexOf( PreheatClassScannerTest.class.getSimpleName() + "$" + ASupplier.class.getSimpleName() ) );
    }

    public class ZSupplier extends AbstractPreheatSupplier
    {
        @Override
        public void preheatAdd( TrackerPreheatParams params, TrackerPreheat preheat )
        {
            // DUMMY
        }
    }

    @SupplierDependsOn( ZSupplier.class )
    public class ASupplier extends AbstractPreheatSupplier
    {
        @Override
        public void preheatAdd( TrackerPreheatParams params, TrackerPreheat preheat )
        {
            // DUMMY
        }
    }

}