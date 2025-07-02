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
package org.hisp.dhis.eventvisualization;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.hisp.dhis.common.DimensionalObjectUtils.asActualDimension;
import static org.hisp.dhis.eventvisualization.Attribute.COLUMN;
import static org.hisp.dhis.eventvisualization.Attribute.FILTER;
import static org.hisp.dhis.eventvisualization.Attribute.ROW;
import static org.hisp.dhis.eventvisualization.SimpleDimension.Type.OU;
import static org.hisp.dhis.eventvisualization.SimpleDimension.Type.contains;
import static org.hisp.dhis.eventvisualization.SimpleDimension.Type.from;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;

/**
 * Responsible for handling and associating the simple event dimensions in the EventAnalyticalObject
 * instances.
 *
 * @author maikel arabori
 */
public class SimpleDimensionHandler {
  /**
   * We use the EventAnalyticalObject interface because this handler is also used by the deprecated
   * EventReport. So, in order to being able to reuse it, we make usage of the common interface.
   *
   * <p>Once the EventReport is completely removed we can start using the actual EventVisualization
   * class.
   */
  private final EventAnalyticalObject eventAnalyticalObject;

  public SimpleDimensionHandler(final EventAnalyticalObject eventAnalyticalObject) {
    this.eventAnalyticalObject = eventAnalyticalObject;
  }

  /**
   * Based on the given dimension and parent attribute, this method will return an instance
   * representing the associated DimensionalObject. It actually returns an instance of
   * BaseDimensionalObject.
   *
   * @param qualifiedDimension the dimension, ie: dx, pe, eventDate, program.eventDate,
   *     program.stage.dimension.
   * @param parent the parent attribute.
   * @return the respective dimensional object.
   * @throws IllegalArgumentException if the dimension does not exist in {@link
   *     SimpleDimension.Type}.
   */
  public DimensionalObject getDimensionalObject(String qualifiedDimension, Attribute parent) {
    String actualDim = asActualDimension(qualifiedDimension);

    return new BaseDimensionalObject(
        qualifiedDimension,
        from(actualDim).getParentType(),
        getSimpleDimensionalItems(qualifiedDimension, parent));
  }

  /**
   * Based on the given event analytical object provided in the constructor of this class, this
   * method will populate its respective list of simple event dimensions.
   *
   * <p>It, basically, iterates through columns, rows and filters in order to extract the correct
   * dimension and add it into the list of simple event dimensions. This is obviously done by
   * reference on top of the current event analytical object.
   */
  public void associateDimensions() {
    associateDimensionalObjects(eventAnalyticalObject.getColumns(), COLUMN);
    associateDimensionalObjects(eventAnalyticalObject.getRows(), ROW);
    associateDimensionalObjects(eventAnalyticalObject.getFilters(), FILTER);
  }

  /**
   * Makes the correct internal associations where applicable, for each one of the given {@link
   * DimensionalObject} in the list.
   *
   * @param dimensionalObjects the list of {@link DimensionalObject}.
   * @param parent the parent {@link Attribute} of the possible association.
   */
  private void associateDimensionalObjects(
      List<DimensionalObject> dimensionalObjects, Attribute parent) {
    if (isNotEmpty(dimensionalObjects)) {
      for (DimensionalObject object : dimensionalObjects) {
        if (object != null && contains(object.getUid())) {
          eventAnalyticalObject
              .getSimpleDimensions()
              .add(createSimpleEventDimensionFor(object, parent));
        }
      }
    }
  }

  /**
   * Returns a list of {@link BaseDimensionalItemObject} based on the internal list of {@link
   * SimpleDimension} objects, and also based on the given dimension. The items returned must have
   * the same qualified dimension and same parent.
   *
   * @param qualifiedDimension the qualified dimension.
   * @param parent the parent {@link Attribute} of the association.
   * @return the list of {@link BaseDimensionalItemObject}.
   */
  private List<DimensionalItemObject> getSimpleDimensionalItems(
      String qualifiedDimension, Attribute parent) {
    List<DimensionalItemObject> items = new ArrayList<>();

    for (SimpleDimension simpleDimension : eventAnalyticalObject.getSimpleDimensions()) {
      boolean hasSameDimension = simpleDimension.asQualifiedDimension().equals(qualifiedDimension);
      boolean isOrgUnit = from(simpleDimension.getDimension()) == OU;

      if (simpleDimension.belongsTo(parent) && hasSameDimension) {
        for (String dim : simpleDimension.getValues()) {
          if (isOrgUnit) {
            DimensionalItemObject itemObject = new BaseDimensionalItemObject(dim, null);
            items.add(itemObject);
          } else {
            items.add(new BaseDimensionalItemObject(dim));
          }
        }
      }
    }

    return items;
  }

  /**
   * Simply creates a new {@link SimpleDimension} object based on the given arguments.
   *
   * @param dimensionalObject the {@link DimensionalObject}.
   * @param parent the parent {@link Attribute} of the association.
   * @return an instance of {@link SimpleDimension}.
   */
  private SimpleDimension createSimpleEventDimensionFor(
      DimensionalObject dimensionalObject, Attribute parent) {
    String programUid =
        getProgramUid(dimensionalObject.getProgram(), dimensionalObject.getProgramStage());
    String programStageUid =
        dimensionalObject.getProgramStage() != null
            ? dimensionalObject.getProgramStage().getUid()
            : null;
    String actualDim = asActualDimension(dimensionalObject.getUid());

    SimpleDimension simpleDimension =
        new SimpleDimension(parent, actualDim, programUid, programStageUid);

    if (isNotEmpty(dimensionalObject.getItems())) {
      simpleDimension.setValues(
          dimensionalObject.getItems().stream().map(DimensionalItemObject::getUid).toList());
    }

    return simpleDimension;
  }

  /**
   * Retrieves the user uid of the program, based on the given objects. Tries to get the uid from
   * the program. If not found, it tries the program present in the program stage.
   *
   * @param program the {@link Program}.
   * @param programStage the {@link ProgramStage}.
   * @return the program uid or null.
   */
  private String getProgramUid(Program program, ProgramStage programStage) {
    if (program != null) {
      return program.getUid();
    }

    if (programStage != null && programStage.getProgram() != null) {
      return programStage.getProgram().getUid();
    }

    return null;
  }
}
