/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.reservedvalue;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.textpattern.TextPattern;
import org.hisp.dhis.textpattern.TextPatternGenerationException;
import org.hisp.dhis.textpattern.TextPatternMethod;
import org.hisp.dhis.textpattern.TextPatternSegment;
import org.hisp.dhis.textpattern.TextPatternService;
import org.hisp.dhis.textpattern.TextPatternValidationUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Stian Sandvold
 */
@Slf4j
@Service("org.hisp.dhis.reservedvalue.ReservedValueService")
public class DefaultReservedValueService implements ReservedValueService {
  private static final int RESERVED_VALUE_GENERATION_ATTEMPT = 10;
  private static final long RESERVED_VALUE_GENERATION_TIMEOUT = (1000 * 30);

  private final TextPatternService textPatternService;

  private final ReservedValueStore reservedValueStore;

  private final ValueGeneratorService valueGeneratorService;

  private final TransactionTemplate transactionTemplate;

  public DefaultReservedValueService(
      TextPatternService textPatternService,
      ReservedValueStore reservedValueStore,
      ValueGeneratorService valueGeneratorService,
      PlatformTransactionManager transactionManager) {
    this.textPatternService = textPatternService;
    this.reservedValueStore = reservedValueStore;
    this.valueGeneratorService = valueGeneratorService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  @Transactional
  public List<ReservedValue> reserve(
      TrackedEntityAttribute trackedEntityAttribute,
      int numberOfReservations,
      Map<String, String> values,
      Date expires)
      throws ReserveValueException, TextPatternGenerationException {
    long startTime = System.currentTimeMillis();

    TextPattern textPattern = trackedEntityAttribute.getTextPattern();

    TextPatternSegment generatedSegment =
        textPattern.getSegments().stream()
            .filter(
                (tp) ->
                    tp.getMethod().isGenerated()
                        && Boolean.TRUE.equals(trackedEntityAttribute.isGenerated()))
            .findFirst()
            .orElse(null);

    String key = textPatternService.resolvePattern(textPattern, values);

    // Used for searching value tables
    String valueKey =
        Optional.ofNullable(generatedSegment)
            .map(gs -> key.replaceAll(Pattern.quote(gs.getRawSegment()), "%"))
            .orElse(key);

    ReservedValue reservedValue =
        ReservedValue.builder()
            .created(new Date())
            .ownerObject(textPattern.getOwnerObject().name())
            .ownerUid(textPattern.getOwnerUid())
            .key(key)
            .value(valueKey)
            .expiryDate(expires)
            .build();

    checkIfEnoughValues(numberOfReservations, generatedSegment, reservedValue);

    if (generatedSegment == null) {
      if (numberOfReservations == 1) {
        List<ReservedValue> reservedValues =
            Collections.singletonList(reservedValue.toBuilder().value(key).build());
        reservedValueStore.reserveValues(reservedValues);

        return reservedValues;
      }
    } else {
      return reserveMultipleValues(
          numberOfReservations,
          generatedSegment,
          reservedValue,
          trackedEntityAttribute,
          startTime,
          key,
          values);
    }

    return List.of();
  }

  private List<ReservedValue> reserveMultipleValues(
      int numberOfReservations,
      TextPatternSegment generatedSegment,
      ReservedValue reservedValue,
      TrackedEntityAttribute trackedEntityAttribute,
      long startTime,
      String key,
      Map<String, String> values)
      throws ReserveValueException, TextPatternGenerationException {
    int attemptsLeft = RESERVED_VALUE_GENERATION_ATTEMPT;
    boolean isPersistable = generatedSegment.getMethod().isPersistable();
    reservedValue.setTrackedEntityAttributeId(trackedEntityAttribute.getId());
    List<ReservedValue> reservedValues = new ArrayList<>();
    TextPattern textPattern = trackedEntityAttribute.getTextPattern();

    try {
      List<String> generatedValues = new ArrayList<>();

      while (attemptsLeft-- > 0 && numberOfReservations - reservedValues.size() > 0) {
        checkTimeout(startTime);
        generatedValues.addAll(
            valueGeneratorService.generateValues(
                generatedSegment, textPattern, key, numberOfReservations - reservedValues.size()));
        List<String> resolvedPatterns =
            getResolvedPatterns(values, textPattern, generatedSegment, generatedValues);

        saveGeneratedValues(
            numberOfReservations - reservedValues.size(),
            reservedValues,
            textPattern,
            reservedValue,
            isPersistable,
            resolvedPatterns);

        generatedValues = new ArrayList<>();
      }

    } catch (TimeoutException ex) {
      log.warn(
          "Generation and reservation of values for {} with uid {} timed out. {} values was reserved. You might be running low on available values",
          textPattern.getOwnerObject().name(),
          textPattern.getOwnerUid(),
          reservedValues.size());
    }

    return reservedValues;
  }

  private void checkTimeout(long startTime) throws TimeoutException {
    if (System.currentTimeMillis() - startTime >= RESERVED_VALUE_GENERATION_TIMEOUT) {
      throw new TimeoutException("Generation and reservation of values took too long");
    }
  }

  private List<String> getResolvedPatterns(
      Map<String, String> values,
      TextPattern textPattern,
      TextPatternSegment generatedSegment,
      List<String> generatedValues)
      throws TextPatternGenerationException {
    List<String> resolvedPatterns = new ArrayList<>();

    for (String generatedValue : generatedValues) {
      resolvedPatterns.add(
          textPatternService.resolvePattern(
              textPattern,
              ImmutableMap.<String, String>builder()
                  .putAll(values)
                  .put(generatedSegment.getMethod().name(), generatedValue)
                  .build()));
    }

    return resolvedPatterns;
  }

  private void checkIfEnoughValues(
      int numberOfReservations, TextPatternSegment generatedSegment, ReservedValue reservedValue)
      throws ReserveValueException {
    if ((generatedSegment == null
            || !TextPatternMethod.SEQUENTIAL.equals(generatedSegment.getMethod()))
        && !hasEnoughValuesLeft(
            reservedValue,
            TextPatternValidationUtils.getTotalValuesPotential(generatedSegment),
            numberOfReservations)) {
      throw new ReserveValueException(
          "Not enough values left to reserve " + numberOfReservations + " values.");
    }
  }

  private void saveGeneratedValues(
      int remainingValuesToReserve,
      List<ReservedValue> resultList,
      TextPattern textPattern,
      ReservedValue reservedValue,
      boolean isPersistable,
      List<String> resolvedPatterns) {
    if (isPersistable) {
      List<ReservedValue> availableValues =
          reservedValueStore.getAvailableValues(
              reservedValue,
              resolvedPatterns.stream().distinct().collect(Collectors.toList()),
              textPattern.getOwnerObject().name());

      List<ReservedValue> requiredValues =
          availableValues.subList(0, Math.min(availableValues.size(), remainingValuesToReserve));

      reservedValueStore.bulkInsertReservedValues(requiredValues);

      resultList.addAll(requiredValues);
    } else {
      resultList.addAll(
          resolvedPatterns.stream()
              .map(value -> reservedValue.toBuilder().value(value).build())
              .toList());
    }
  }

  private boolean hasEnoughValuesLeft(
      ReservedValue reservedValue, long totalValues, int valuesRequired) {
    int used = reservedValueStore.getNumberOfUsedValues(reservedValue);

    return totalValues >= valuesRequired + used;
  }

  @Override
  @Transactional
  public boolean useReservedValue(TextPattern textPattern, String value) {
    return reservedValueStore.useReservedValue(textPattern.getOwnerUid(), value);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isReserved(TextPattern textPattern, String value) {
    return reservedValueStore.isReserved(
        textPattern.getOwnerObject().name(), textPattern.getOwnerUid(), value);
  }

  @Override
  @Transactional
  public void deleteReservedValueByUid(String uid) {
    reservedValueStore.deleteReservedValueByUid(uid);
  }

  @Override
  public int removeUsedOrExpiredReservations() {
    int total = 0;
    int deleted;

    do {
      deleted =
          requireNonNullElse(
              transactionTemplate.execute(s -> reservedValueStore.removeExpiredValues()), 0);
      total += deleted;
    } while (deleted >= DELETE_BATCH_SIZE);

    do {
      deleted =
          requireNonNullElse(
              transactionTemplate.execute(s -> reservedValueStore.removeUsedValues()), 0);
      total += deleted;
    } while (deleted >= DELETE_BATCH_SIZE);

    return total;
  }
}
