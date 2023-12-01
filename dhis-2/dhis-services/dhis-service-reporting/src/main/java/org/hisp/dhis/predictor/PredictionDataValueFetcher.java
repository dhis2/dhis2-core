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
package org.hisp.dhis.predictor;

import static java.util.Collections.emptyList;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.COMPOSITE_DIM_OBJECT_PLAIN_SEP;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.commons.collection.CollectionUtils.isEmpty;
import static org.hisp.dhis.datavalue.DataValueStore.DDV_QUEUE_TIMEOUT_UNIT;
import static org.hisp.dhis.datavalue.DataValueStore.DDV_QUEUE_TIMEOUT_VALUE;
import static org.hisp.dhis.datavalue.DataValueStore.END_OF_DDV_DATA;
import static org.hisp.dhis.system.util.MathUtils.addDoubleObjects;
import static org.hisp.dhis.system.util.ValidationUtils.getObjectValue;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;

/**
 * Fetches DataValues for a prediction.
 *
 * <p>This class fetches values from the datavalue table and returns them for each organisation
 * unit. It assumes that they will be requested in order of organisation unit path and that the
 * values for each organisation unit will be requested only once. For reliability, these assumptions
 * are checked, and exceptions thrown if they are not met.
 *
 * <p>Each time the instance is initialized, it will fetch data for only one organisation unit
 * level.
 *
 * <p>This class maintains performance while limiting memory usage by using a single data request
 * (which means a single SQL query) on a separate thread that provides results to this class through
 * a blocking queue. This class then collects data values until it has all the data for an
 * organisation unit, and then it returns them to the caller.
 *
 * <p>The returned data includes deleted values for the predictor output because predictor
 * processing needs to know where the former predicted values are present, even when they are
 * deleted, because they might be replaced with new, undeleted values.
 *
 * <p>For reliability, if there is an exception on the second thread, the thread is terminated and
 * the exception is re-thrown in the main thread when data is next requested from the caller. This
 * makes sure that the exception is properly reported by the main thread.
 *
 * @author Jim Grace
 */
public class PredictionDataValueFetcher implements Runnable {
  private final DataValueService dataValueService;

  /** Organisation units assigned to the current user. */
  private final Set<OrganisationUnit> currentUserOrgUnits;

  /*
   * Organisation unit level at which we are fetching data.
   */
  private int orgUnitLevel;

  /*
   * Periods at which we are fetching input data.
   */
  private Set<Period> queryPeriods;

  /** Periods at which we are fetching previous predictions (deleted or not). */
  private Set<Period> outputPeriods;

  /** Data elements to fetch. */
  private Set<DataElement> dataElements;

  /** Data element operands to fetch. */
  private Set<DataElementOperand> dataElementOperands;

  /**
   * Output data element (and optionally category option combo) for the predictor output (to fetch
   * previous values, deleted or not).
   */
  private DataElementOperand outputDataElementOperand;

  /** Whether to include organisation unit descendants. */
  private boolean includeDescendants = false;

  /** Find organisation unit (or ancestor orgUnit) for a data value. */
  private Map<String, OrganisationUnit> orgUnitLookup;

  /**
   * Find data element for a data value. For this purpose it does not matter whether the DataElement
   * object has query modifiers.
   */
  private Map<Long, DataElement> dataElementLookup;

  /** Find period for a data value. */
  private Map<Long, Period> periodLookup;

  /** Find catOptionCombo & attributeOptionCombo for a data value. */
  private final Map<Long, CategoryOptionCombo> cocLookup;

  /** Requested data elements, by data element UID. */
  private Map<String, List<DataElement>> dataElementRequests;

  /** Requested data element operands, by data element operand UID. */
  private Map<String, List<DataElementOperand>> dataElementOperandRequests;

  /**
   * Next (lookahead) deflated data value. Processed immediately if it is for the current
   * organisation unit, otherwise saved for the next orgUnit.
   */
  private DeflatedDataValue nextDeflatedDataValue;

  /** Organisation unit for the next deflated data value. */
  private OrganisationUnit nextOrgUnit;

  /** Exception (if any) on the producer side, waiting to be reported. */
  private RuntimeException producerException;

  /** Queue to receive deflated data values from asynchronous thread. */
  private BlockingQueue<DeflatedDataValue> blockingQueue;

  /**
   * The blocking queue size was chosen after performance testing. A value of 1 performed slightly
   * better than 2 or larger values. Since most of the values are just buffered after they are
   * pulled from the queue, there is no benefit of incurring the slight overhead of a larger queue
   * size.
   */
  private static final int DDV_BLOCKING_QUEUE_SIZE = 1;

  public PredictionDataValueFetcher(
      DataValueService dataValueService,
      CategoryService categoryService,
      Set<OrganisationUnit> currentUserOrgUnits) {
    this.dataValueService = dataValueService;
    this.currentUserOrgUnits = currentUserOrgUnits;
    this.cocLookup =
        categoryService.getAllCategoryOptionCombos().stream()
            .collect(toMap(CategoryOptionCombo::getId, identity()));
  }

  /**
   * Initializes for datavalue retrieval.
   *
   * @param orgUnitLevel level of organisation units to fetch.
   * @param orgUnits organisation units to fetch.
   * @param queryPeriods periods to fetch.
   * @param outputPeriods predictor output periods.
   * @param dataElements data elements to fetch.
   * @param dataElementOperands data element operands to fetch.
   */
  public void init(
      int orgUnitLevel,
      List<OrganisationUnit> orgUnits,
      Set<Period> queryPeriods,
      Set<Period> outputPeriods,
      Set<DataElement> dataElements,
      Set<DataElementOperand> dataElementOperands,
      DataElementOperand outputDataElementOperand) {
    this.orgUnitLevel = orgUnitLevel;
    this.queryPeriods = queryPeriods;
    this.outputPeriods = outputPeriods;
    this.dataElements = dataElements;
    this.dataElementOperands = dataElementOperands;
    this.outputDataElementOperand = outputDataElementOperand;

    orgUnitLookup =
        orgUnits.stream().collect(Collectors.toMap(OrganisationUnit::getPath, Function.identity()));
    dataElementLookup =
        dataElements.stream()
            .collect(toMap(DataElement::getId, Function.identity(), (de1, de2) -> de1));
    dataElementLookup.putAll(
        dataElementOperands.stream()
            .map(DataElementOperand::getDataElement)
            .distinct()
            .collect(toMap(DataElement::getId, Function.identity(), (deo1, deo2) -> deo1)));
    periodLookup =
        queryPeriods.stream().collect(Collectors.toMap(Period::getId, Function.identity()));

    dataElementRequests = dataElements.stream().collect(groupingBy(DataElement::getUid));
    dataElementOperandRequests =
        dataElementOperands.stream().collect(groupingBy(DataElementOperand::getUid));

    producerException = null;

    blockingQueue = new ArrayBlockingQueue<>(DDV_BLOCKING_QUEUE_SIZE);

    if (isEmpty(dataElements) && isEmpty(dataElementOperands)) {
      nextOrgUnit = null; // There will be no data

      return;
    }

    ExecutorService executor = Executors.newSingleThreadExecutor();
    executor.execute(this); // Invoke run() on another thread
    executor.shutdown();

    getNextDeflatedDataValue(); // Prime the algorithm with the first value.
  }

  /** In a separate thread, fetches all the requested data values. */
  @Override
  public void run() {
    DataExportParams params = new DataExportParams();
    params.setDataElements(dataElements);
    params.setDataElementOperands(dataElementOperands);
    params.setPeriods(queryPeriods);
    params.setOrganisationUnits(currentUserOrgUnits);
    params.setOuMode(DESCENDANTS);
    params.setOrgUnitLevel(orgUnitLevel);
    params.setBlockingQueue(blockingQueue);
    params.setOrderByOrgUnitPath(true);
    params.setIncludeDescendants(includeDescendants);
    params.setIncludeDeleted(true);

    try {
      dataValueService.getDeflatedDataValues(params);
    } catch (RuntimeException ex) {
      producerException = ex; // Tell the main thread

      queueEndOfDataMarker(); // Wake up main thread if needed

      throw ex; // Log the exception
    }
  }

  /**
   * In the main thread, gets prediction data for the next organisation unit.
   *
   * <p>Note that "inflating" the data values from their deflated form must be done in the main
   * thread so as not to upset the DataValue's Hibernate properties.
   *
   * @return the prediction data
   */
  public PredictionData getData() {
    if (nextOrgUnit == null) {
      return null;
    }

    List<DeflatedDataValue> deflatedDataValues = new ArrayList<>();

    OrganisationUnit startingOrgUnit = nextOrgUnit;

    do {
      deflatedDataValues.add(nextDeflatedDataValue);

      getNextDeflatedDataValue();
    } while (startingOrgUnit.equals(nextOrgUnit));

    return getPredictionData(startingOrgUnit, deflatedDataValues);
  }

  // -------------------------------------------------------------------------
  // Set methods
  // -------------------------------------------------------------------------

  /**
   * Sets whether the data should return aggregated to the parent org unit.
   *
   * @param includeDescendants whether the data includes descendant orgUnits.
   * @return this object (for method chaining).
   */
  public PredictionDataValueFetcher setIncludeDescendants(boolean includeDescendants) {
    this.includeDescendants = includeDescendants;
    return this;
  }

  // -------------------------------------------------------------------------
  // Supportive Methods
  // -------------------------------------------------------------------------

  /** Gets the next deflated data value. Remembers it and its path. */
  private void getNextDeflatedDataValue() {
    nextDeflatedDataValue = dequeueDeflatedDataValue();

    checkForProducerException(); // Check for exception during dequeue

    if (nextDeflatedDataValue == END_OF_DDV_DATA) {
      nextOrgUnit = null; // No more data

      return;
    }

    nextOrgUnit = orgUnitLookup.get(truncatePathToLevel(nextDeflatedDataValue.getSourcePath()));
  }

  /** Dequeues the next {@see DeflatedDataValue} from the database feed */
  private DeflatedDataValue dequeueDeflatedDataValue() {
    try {
      return blockingQueue.poll(DDV_QUEUE_TIMEOUT_VALUE, DDV_QUEUE_TIMEOUT_UNIT);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      throw new IllegalStateException("could not fetch next DeflatedDataValue");
    }
  }

  /**
   * Truncates a path from a deflated data value to the level of the organisation units we are
   * looking for now.
   */
  private String truncatePathToLevel(String path) {
    return path.substring(0, orgUnitLevel * 12);
  }

  /** Gets prediction data for an orgUnit from a list of deflated data values. */
  private PredictionData getPredictionData(
      OrganisationUnit orgUnit, List<DeflatedDataValue> deflatedDataValues) {
    MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map = new MapMapMap<>();

    List<DataValue> oldPredictions = new ArrayList<>();

    for (DeflatedDataValue ddv : deflatedDataValues) {
      DataValue dv = inflateDataValue(ddv);

      if (!dv.isDeleted()) {
        addValueToMap(dv, map);
      }

      if (ddv.getSourcePath().equals(dv.getSource().getPath())
          && ddv.getDataElementId() == outputDataElementOperand.getDataElement().getId()
          && (outputDataElementOperand.getCategoryOptionCombo() == null
              || ddv.getCategoryOptionComboId()
                  == outputDataElementOperand.getCategoryOptionCombo().getId())
          && outputPeriods.contains(dv.getPeriod())) {
        oldPredictions.add(dv);
      }
    }

    return new PredictionData(orgUnit, mapToValues(orgUnit, map), oldPredictions);
  }

  /** "Inflates" a deflated data value, using our caches. */
  private DataValue inflateDataValue(DeflatedDataValue ddv) {
    DataElement dataElement = dataElementLookup.get(ddv.getDataElementId());

    Period period = periodLookup.get(ddv.getPeriodId());

    OrganisationUnit orgUnit = orgUnitLookup.get(truncatePathToLevel(ddv.getSourcePath()));

    CategoryOptionCombo categoryOptionCombo = cocLookup.get(ddv.getCategoryOptionComboId());

    CategoryOptionCombo attributeOptionCombo = cocLookup.get(ddv.getAttributeOptionComboId());

    return new DataValue(
        dataElement,
        period,
        orgUnit,
        categoryOptionCombo,
        attributeOptionCombo,
        ddv.getValue(),
        ddv.getStoredBy(),
        ddv.getLastUpdated(),
        ddv.getComment(),
        ddv.isFollowup(),
        ddv.isDeleted());
  }

  /**
   * Adds a non-deleted value to the value map.
   *
   * <p>There are three types of dimensional item objects that any data value may be returned for:
   *
   * <ol>
   *   <li>DataElement: sum values across all catOptionCombos
   *   <li>DataElementOperand: specified catOptionCombo
   *   <li>Wildcard DataElement (represented as a DataElementOperand with a null catOptionCombo):
   *       return a DataElementOperand for each different catOptionCombo (for use in disaggregated
   *       predictions)
   * </ol>
   *
   * Note that for any of these three types there may be multiple dimension item objects for any
   * data value if they have different query modifiers.
   */
  private void addValueToMap(
      DataValue dv, MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map) {
    Object value = getObjectValue(dv.getValue(), dv.getDataElement().getValueType());

    if (value != null) {
      // Add value to any requested data elements
      for (DataElement de : getDataElementRequestList(dv.getDataElement().getUid())) {
        addToMap(de, dv, value, map);
      }

      // Add value to any requested data element operands
      for (DataElementOperand deo :
          getDataElementOperandRequestList(
              dv.getDataElement().getUid()
                  + COMPOSITE_DIM_OBJECT_PLAIN_SEP
                  + dv.getCategoryOptionCombo().getUid())) {
        addToMap(deo, dv, value, map);
      }

      // Record value for any requested wildcard data element operands
      for (DataElementOperand deo :
          getDataElementOperandRequestList(dv.getDataElement().getUid())) {
        DataElementOperand newDeo =
            new DataElementOperand(deo.getDataElement(), dv.getCategoryOptionCombo());
        newDeo.setQueryMods(deo.getQueryMods());
        addToMap(newDeo, dv, value, map);
      }
    }
  }

  /** Returns matching data element requests or empty list if none. */
  private List<DataElement> getDataElementRequestList(String uid) {
    return firstNonNull(dataElementRequests.get(uid), emptyList());
  }

  /** Returns matching data element operands requests or empty list if none. */
  private List<DataElementOperand> getDataElementOperandRequestList(String uid) {
    return firstNonNull(dataElementOperandRequests.get(uid), emptyList());
  }

  /**
   * Adds the DataElementOperand or the DataElement value to existing data.
   *
   * <p>This is needed because we may get multiple data values that need to be aggregated to the
   * same item value. In the case of a DataElementOperand, this may be multiple values from children
   * organisation units. In the case of a DataElement, this may be multiple value from children
   * organisation units and/or it may be multiple disaggregated values that need to be summed for
   * the data element.
   *
   * <p>Note that a single data value may contribute to a DataElementOperand value, a DataElement
   * value, or both.
   */
  private void addToMap(
      DimensionalItemObject item,
      DataValue dv,
      Object value,
      MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map) {
    Object valueSoFar = map.getValue(dv.getAttributeOptionCombo(), dv.getPeriod(), item);

    Object valueToStore = (valueSoFar == null) ? value : addDoubleObjects(value, valueSoFar);

    map.putEntry(dv.getAttributeOptionCombo(), dv.getPeriod(), item, valueToStore);
  }

  /** Convert the value map to a list of found values. */
  private List<FoundDimensionItemValue> mapToValues(
      OrganisationUnit orgUnit,
      MapMapMap<CategoryOptionCombo, Period, DimensionalItemObject, Object> map) {
    List<FoundDimensionItemValue> values = new ArrayList<>();

    for (Map.Entry<CategoryOptionCombo, MapMap<Period, DimensionalItemObject, Object>> e1 :
        map.entrySet()) {
      CategoryOptionCombo aoc = e1.getKey();

      for (Map.Entry<Period, Map<DimensionalItemObject, Object>> e2 : e1.getValue().entrySet()) {
        Period period = e2.getKey();

        for (Map.Entry<DimensionalItemObject, Object> e3 : e2.getValue().entrySet()) {
          DimensionalItemObject obj = e3.getKey();
          Object value = e3.getValue();

          values.add(new FoundDimensionItemValue(orgUnit, period, aoc, obj, value));
        }
      }
    }

    return values;
  }

  /**
   * Checks for an unexpected exception in the producer thread, and if found, throws it on the main
   * thread.
   */
  private void checkForProducerException() {
    if (producerException != null) {
      throw producerException;
    }
  }

  /**
   * Adds the end of data marker to the queue. This is used in case there is an unexpected runtime
   * exception in the producer thread, and the consumer (main) thread is waiting for a data value.
   * This allows the main thread to wake up and handle (rethrow) the exception.
   */
  private void queueEndOfDataMarker() {
    try {
      blockingQueue.offer(END_OF_DDV_DATA, DDV_QUEUE_TIMEOUT_VALUE, DDV_QUEUE_TIMEOUT_UNIT);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();

      throw new IllegalStateException("could not add end of deflated data values marker");
    }
  }
}
