/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge.dataelement.handler;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupStore;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperandStore;
import org.hisp.dhis.dataset.DataSetElement;
import org.hisp.dhis.dataset.DataSetStore;
import org.hisp.dhis.dataset.Section;
import org.hisp.dhis.dataset.SectionStore;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.minmax.MinMaxDataElement;
import org.hisp.dhis.minmax.MinMaxDataElementStore;
import org.hisp.dhis.predictor.Predictor;
import org.hisp.dhis.predictor.PredictorStore;
import org.hisp.dhis.sms.command.code.SMSCode;
import org.hisp.dhis.sms.command.hibernate.SMSCommandStore;
import org.springframework.stereotype.Component;

/**
 * Merge handler for metadata entities.
 *
 * @author david mackessy
 */
@Component
@RequiredArgsConstructor
public class MetadataDataElementMergeHandler {

  private final MinMaxDataElementStore minMaxDataElementStore;
  private final SMSCommandStore smsCommandStore;
  private final PredictorStore predictorStore;
  private final DataElementOperandStore dataElementOperandStore;
  private final DataSetStore dataSetStore;
  private final SectionStore sectionStore;
  private final DataElementGroupStore dataElementGroupStore;

  /**
   * Method retrieving {@link MinMaxDataElement}s by source {@link DataElement} references. All
   * retrieved {@link MinMaxDataElement}s will have their {@link DataElement} replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link MinMaxDataElement}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     MinMaxDataElement}
   */
  public void handleMinMaxDataElement(List<DataElement> sources, DataElement target) {
    List<MinMaxDataElement> minMaxDataElements = minMaxDataElementStore.getByDataElement(sources);
    minMaxDataElements.forEach(mmde -> mmde.setDataElement(target));
  }

  /**
   * Method retrieving {@link SMSCode}s by source {@link DataElement} references. All retrieved
   * {@link SMSCode}s will have their {@link DataElement} replaced with the target {@link
   * DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link SMSCode}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     SMSCode}
   */
  public void handleSmsCode(List<DataElement> sources, DataElement target) {
    List<SMSCode> smsCodes = smsCommandStore.getCodesByDataElement(sources);
    smsCodes.forEach(c -> c.setDataElement(target));
  }

  /**
   * Method retrieving {@link org.hisp.dhis.predictor.Predictor}s by source {@link DataElement}
   * references. All retrieved {@link org.hisp.dhis.predictor.Predictor}s will have their {@link
   * DataElement} replaced with the target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link
   *     org.hisp.dhis.predictor.Predictor}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     org.hisp.dhis.predictor.Predictor}
   */
  public void handlePredictor(List<DataElement> sources, DataElement target) {
    List<Predictor> predictors = predictorStore.getAllByDataElement(sources);
    predictors.forEach(p -> p.setOutput(target));
  }

  /**
   * Method retrieving {@link Predictor}s which have a source {@link DataElement} reference in its
   * generator {@link Expression}. All retrieved {@link Predictor}s will have their generator {@link
   * Expression} {@link DataElement} {@link UID} replaced with the target {@link DataElement} {@link
   * UID}
   *
   * @param sources source {@link DataElement}s used to retrieve {@link Predictor}s
   * @param target {@link DataElement} which will replace the {@link DataElement} {@link UID} in a
   *     {@link Predictor} {@link Expression}
   */
  public void handlePredictorGeneratorExpression(List<DataElement> sources, DataElement target) {
    List<Predictor> predictors =
        predictorStore.getAllWithGeneratorContainingDataElement(
            IdentifiableObjectUtils.getUidsNonNull(sources));
    for (DataElement source : sources) {
      predictors.forEach(
          p -> {
            Expression generator = p.getGenerator();
            generator.setExpression(
                generator.getExpression().replace(source.getUid(), target.getUid()));
          });
    }
  }

  /**
   * Method retrieving {@link Predictor}s which have a source {@link DataElement} reference in its
   * sampleSkipTest {@link Expression}. All retrieved {@link Predictor}s will have their
   * sampleSkipTest {@link Expression} {@link DataElement} {@link UID} replaced with the target
   * {@link DataElement} {@link UID}
   *
   * @param sources source {@link DataElement}s used to retrieve {@link Predictor}s
   * @param target {@link DataElement} which will replace the {@link DataElement} {@link UID} in a
   *     {@link Predictor} {@link Expression}
   */
  public void handlePredictorSampleSkipTestExpression(
      List<DataElement> sources, DataElement target) {
    List<Predictor> predictors =
        predictorStore.getAllWithSampleSkipTestContainingDataElement(
            IdentifiableObjectUtils.getUidsNonNull(sources));
    for (DataElement source : sources) {
      predictors.forEach(
          p -> {
            Expression sampleSkipTest = p.getSampleSkipTest();
            sampleSkipTest.setExpression(
                sampleSkipTest.getExpression().replace(source.getUid(), target.getUid()));
          });
    }
  }

  /**
   * Method retrieving {@link DataElementOperand}s by source {@link DataElement} references. All
   * retrieved {@link DataElementOperand}s will have their {@link DataElement} replaced with the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataElementOperand}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     DataElementOperand}
   */
  public void handleDataElementOperand(List<DataElement> sources, DataElement target) {
    List<DataElementOperand> dataElementOperands =
        dataElementOperandStore.getByDataElement(sources);

    dataElementOperands.forEach(pra -> pra.setDataElement(target));
  }

  /**
   * Method retrieving {@link DataSetElement}s by source {@link DataElement} references. Sources
   * will have all found {@link DataSetElement}s removed. All found {@link DataSetElement}s will
   * have their {@link DataElement} set as the target {@link DataElement}. All found {@link
   * DataSetElement}s will be added to the target.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataSetElement}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     DataSetElement}
   */
  public void handleDataSetElement(List<DataElement> sources, DataElement target) {
    List<DataSetElement> sourceDataSetElements =
        dataSetStore.getDataSetElementsByDataElement(sources);
    List<String> targetDataSets = dataSetStore.getDataSetUidsByDataElement(List.of(target));
    sourceDataSetElements.forEach(
        dse -> {
          if (!targetDataSets.contains(dse.getDataSet().getUid())) {
            sources.forEach(de -> de.removeDataSetElement(dse));
            dse.setDataElement(target);
            target.addDataSetElement(dse);
          }
        });
  }

  /**
   * Method retrieving {@link Section}s by source {@link DataElement} references. All retrieved
   * {@link Section}s will have their {@link DataElement} replaced with the target {@link
   * DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link Section}s
   * @param target {@link DataElement} which will be set as the {@link DataElement} for an {@link
   *     Section}
   */
  public void handleSection(List<DataElement> sources, DataElement target) {
    List<Section> sections = sectionStore.getSectionsByDataElement(sources);
    sources.forEach(
        source -> {
          sections.forEach(s -> s.getDataElements().remove(source));
          sections.forEach(s -> s.getDataElements().add(target));
        });
  }

  /**
   * Method retrieving {@link DataElementGroup}s by source {@link DataElement} references. All
   * retrieved {@link DataElementGroup}s will remove their source {@link DataElement}s and add the
   * target {@link DataElement}.
   *
   * @param sources source {@link DataElement}s used to retrieve {@link DataElementGroup}s and then
   *     removed from the {@link DataElementGroup}
   * @param target {@link DataElement} which will be added to the {@link DataElementGroup}
   */
  public void handleDataElementGroup(List<DataElement> sources, DataElement target) {
    List<DataElementGroup> dataElementGroups = dataElementGroupStore.getByDataElement(sources);
    sources.forEach(
        source -> {
          dataElementGroups.forEach(deg -> deg.removeDataElement(source));
          dataElementGroups.forEach(deg -> deg.addDataElement(target));
        });
  }
}
