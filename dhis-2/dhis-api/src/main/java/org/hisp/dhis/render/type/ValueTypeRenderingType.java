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
package org.hisp.dhis.render.type;

import com.google.common.collect.ImmutableSet;

/**
 * This class represents all the different ways ValueTypes can be rendered. constrains is defined in
 * StaticRenderingConfiguration.java and is enforced in DataElementObjectBundleHook and
 * TrackedEntityAttributeObjectBundleHook
 */
public enum ValueTypeRenderingType {
  DEFAULT,
  DROPDOWN,
  VERTICAL_RADIOBUTTONS,
  HORIZONTAL_RADIOBUTTONS,
  VERTICAL_CHECKBOXES,
  HORIZONTAL_CHECKBOXES,
  SHARED_HEADER_RADIOBUTTONS,
  ICONS_AS_BUTTONS,
  SPINNER,
  ICON,
  TOGGLE,
  VALUE,
  SLIDER,
  LINEAR_SCALE,
  AUTOCOMPLETE,
  QR_CODE,
  BAR_CODE,
  GS1_DATAMATRIX;

  /** RenderingTypes supported by OptionSet ValueTypes */
  public static final ImmutableSet<ValueTypeRenderingType> OPTION_SET_TYPES =
      ImmutableSet.of(
          DEFAULT,
          DROPDOWN,
          VERTICAL_RADIOBUTTONS,
          HORIZONTAL_RADIOBUTTONS,
          VERTICAL_CHECKBOXES,
          HORIZONTAL_CHECKBOXES,
          SHARED_HEADER_RADIOBUTTONS,
          ICONS_AS_BUTTONS,
          SPINNER,
          ICON);

  /** RenderingTypes supported by boolean ValueTypes */
  public static final ImmutableSet<ValueTypeRenderingType> BOOLEAN_TYPES =
      ImmutableSet.of(
          DEFAULT,
          VERTICAL_RADIOBUTTONS,
          HORIZONTAL_RADIOBUTTONS,
          VERTICAL_CHECKBOXES,
          HORIZONTAL_CHECKBOXES,
          TOGGLE);

  /** RenderingTypes supported by numerical ValueTypes */
  public static final ImmutableSet<ValueTypeRenderingType> NUMERIC_TYPES =
      ImmutableSet.of(DEFAULT, VALUE, SLIDER, LINEAR_SCALE, SPINNER);

  /** RenderingTypes supported by textual valueTypes */
  public static final ImmutableSet<ValueTypeRenderingType> TEXT_TYPES =
      ImmutableSet.of(DEFAULT, VALUE, AUTOCOMPLETE, QR_CODE, BAR_CODE, GS1_DATAMATRIX);
}
