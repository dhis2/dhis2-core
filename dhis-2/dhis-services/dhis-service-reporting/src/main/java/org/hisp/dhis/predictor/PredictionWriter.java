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
package org.hisp.dhis.predictor;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.jdbc.batchhandler.DataValueBatchHandler;
import org.hisp.dhis.period.Period;
import org.hisp.quick.BatchHandler;
import org.hisp.quick.BatchHandlerFactory;

/**
 * Writes predictions to the database.
 * <p>
 * For performance, a BatchHandler is used where possible.
 *
 * @author Jim Grace
 */
public class PredictionWriter
{
    private final DataValueService dataValueService;

    private final BatchHandlerFactory batchHandlerFactory;

    private BatchHandler<DataValue> dataValueBatchHandler;

    private Set<Period> existingOutputPeriods;

    private PredictionSummary summary;

    public PredictionWriter( DataValueService dataValueService, BatchHandlerFactory batchHandlerFactory )
    {
        checkNotNull( dataValueService );
        checkNotNull( batchHandlerFactory );

        this.dataValueService = dataValueService;
        this.batchHandlerFactory = batchHandlerFactory;
    }

    /**
     * Initializes the PredictionWriter.
     *
     * @param existingOutputPeriods existing output periods before transation.
     * @param summary prediction summary into which to write statistics.
     */
    public void init( Set<Period> existingOutputPeriods, PredictionSummary summary )
    {
        this.existingOutputPeriods = existingOutputPeriods;
        this.summary = summary;

        dataValueBatchHandler = batchHandlerFactory.createBatchHandler( DataValueBatchHandler.class ).init();
    }

    /**
     * Writes a List of predicted data values.
     * <p>
     * A list of old predicted values is used to compare the predictions with.
     * If the predicted value exists already, it is unchanged. If it exists but
     * is different, it is updated. If it doesn't exist, it is inserted.
     * <p>
     * Finally, if there are any old predictions that no longer apply, they are
     * deleted (soft deleted).
     *
     * @param predictions new predicted data values.
     * @param oldPredictions existing predicted data values.
     */
    public void write( List<DataValue> predictions, List<DataValue> oldPredictions )
    {
        Map<String, DataValue> oldPredictionMap = oldPredictions.stream()
            .collect( Collectors.toMap( this::mapKey, dv -> dv ) );

        for ( DataValue prediction : predictions )
        {
            writePrediction( prediction, oldPredictionMap );
        }

        deleteObsoletePredictions( oldPredictionMap );
    }

    public void flush()
    {
        dataValueBatchHandler.flush();
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Writes a predicted data value to the database.
     * <p>
     * Note that the entry from oldPredictionMap may be removed as a consequence
     * of this method. If it is not removed from the map, then it may be an
     * obsoleted prediction that needs to be deleted from the database.
     *
     * @param prediction new predicted data value.
     * @param oldPredictionMap existing predicted data values.
     */
    private void writePrediction( DataValue prediction, Map<String, DataValue> oldPredictionMap )
    {
        boolean predictionIsZeroAndInsignificant = dataValueIsZeroAndInsignificant(
            prediction.getValue(), prediction.getDataElement() );

        DataValue oldPrediction = oldPredictionMap.get( mapKey( prediction ) );

        if ( oldPrediction == null )
        {
            if ( !predictionIsZeroAndInsignificant )
            {
                insertPrediction( prediction );
            }
        }
        else
        {
            if ( predictionIsZeroAndInsignificant )
            {
                if ( !oldPrediction.isDeleted() )
                {
                    return; // Keep old prediction in the list to delete it.
                }
            }
            else if ( !prediction.getValue().equals( oldPrediction.getValue() )
                || oldPrediction.isDeleted() )
            {
                updatePrediction( prediction );
            }
            else
            {
                summary.incrementUnchanged();
            }

            oldPredictionMap.remove( mapKey( prediction ) );
        }
    }

    /**
     * Adds a predicted data value to the database.
     * <p>
     * Note: BatchHandler can be used for inserting only when the period
     * previously existed. To insert values into new periods (just added to the
     * database within this transaction), the dataValueService must be used.
     *
     * @param prediction the predicted data value.
     */
    private void insertPrediction( DataValue prediction )
    {
        summary.incrementInserted();

        if ( existingOutputPeriods.contains( prediction.getPeriod() ) )
        {
            dataValueBatchHandler.addObject( prediction );
        }
        else
        {
            dataValueService.addDataValue( prediction );
        }
    }

    /**
     * Updates a predicted data value in the database.
     *
     * @param prediction the predicted data value.
     */
    private void updatePrediction( DataValue prediction )
    {
        summary.incrementUpdated();

        dataValueBatchHandler.updateObject( prediction );
    }

    /**
     * (Soft) deletes any remaining old predictions from the database.
     *
     * @param oldPredictionMap
     */
    private void deleteObsoletePredictions( Map<String, DataValue> oldPredictionMap )
    {
        for ( DataValue remainingOldPrediction : oldPredictionMap.values() )
        {
            if ( !remainingOldPrediction.isDeleted() )
            {
                summary.incrementDeleted();

                remainingOldPrediction.setDeleted( true );

                dataValueBatchHandler.updateObject( remainingOldPrediction );
            }
        }
    }

    /**
     * Gets a key to be used in the old prediction map.
     *
     * @param dv data value to be accessed.
     * @return a key identifying this data value's dimensions.
     */
    private String mapKey( DataValue dv )
    {
        return dv.getPeriod().getCode() + dv.getSource().getUid() + dv.getDataElement().getUid()
            + dv.getCategoryOptionCombo().getUid() + dv.getAttributeOptionCombo().getUid();
    }
}
