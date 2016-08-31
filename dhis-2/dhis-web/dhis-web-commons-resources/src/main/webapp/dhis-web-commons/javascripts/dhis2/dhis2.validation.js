"use strict";

/*
 * Copyright (c) 2004-2014, University of Oslo
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

dhis2.util.namespace('dhis2.validation');

dhis2.validation.INT_MAX_VALUE = parseInt('2147483647');

/**
 * Checks if the given value is valid zero.
 */
dhis2.validation.isValidZeroNumber = function(value) {
  var regex = /^0(\.0*)?$/;
  return regex.test(value);
};

/**
 * Allow only integers or a single zero and no thousands separators.
 */
dhis2.validation.isInt = function(value) {
  var regex = /^(0|-?[1-9]\d*)$/;
  var patternTest = regex.test(value);
  var rangeTest = value &&
    parseInt(value) < dhis2.validation.INT_MAX_VALUE &&
    parseInt(value) > ( dhis2.validation.INT_MAX_VALUE * -1 );
  return patternTest && rangeTest;
};

/**
 * Allow only positive integers, not zero, no thousands separators.
 */
dhis2.validation.isPositiveInt = function(value) {
  var regex = /^[1-9]\d*$/;
  return regex.test(value) && dhis2.validation.isInt(value);
};

/**
 * Allow only zero or positive integers, no thousands separators.
 */
dhis2.validation.isZeroOrPositiveInt = function(value) {
  var regex = /(^0$)|(^[1-9]\d*$)/;
  return regex.test(value) && dhis2.validation.isInt(value);
};

/**
 * Allow coordinate.
 */
dhis2.validation.isCoordinate = function(value) {
  try {
	var m = value.match(/^([+-]?\d+(?:\.\d+)?)\s*,\s*([+-]?\d+(?:\.\d+)?)$/);
   	var lng = parseFloat(m[1]);
    var lat = parseFloat(m[2]);
    return lng >= -180 && lng <= 180 && lat >= -180 && lat <= 90;
  } 
  catch (_) {
    return false;
  }
};

/**
 * Allow only negative integers, not zero and no thousands separators.
 */
dhis2.validation.isNegativeInt = function(value) {
  var regex = /^-[1-9]\d*$/;
  return regex.test(value) && dhis2.validation.isInt(value);
};

/**
 * Allow any real number,optionally with a sign, no thousands separators and a
 * single decimal point.
 */
dhis2.validation.isNumber = function(value) {
  var regex = /^(-?0|-?[1-9]\d*)(\.\d+)?(E\d+)?$/;
  return regex.test(value);
};

/**
 * Checks if the given value is a valid positive number.
 */
dhis2.validation.isPositiveNumber = function(value) {
  return dhis2.validation.isNumber(value) && parseFloat(value) > 0;
};

/**
 * Checks if the given value is a valid negative number.
 */
dhis2.validation.isNegativeNumber = function(value) {
  return dhis2.validation.isNumber(value) && parseFloat(value) < 0;
};

/**
 * Allow only integers inclusive between 0 and 100.
 */
dhis2.validation.isPercentage = function(value) {
  return dhis2.validation.isInt(value) && parseInt(value) >= 0 && parseInt(value) <= 100;
};

/**
 * Returns true if the provided string argument is to be considered a unit
 * interval, which implies that the value is numeric and inclusive between 0
 * and 1.
 */
dhis2.validation.isUnitInterval = function(value) {
  if( !dhis2.validation.isNumber(value) ) {
    return false;
  }

  var f = parseFloat(value);

  return f >= 0 && f <= 1;
};

/**
 * Validate value type. To have proper message displayed, requires the following vars to be available:
 * i18n_value_must_integer
 * i18n_value_must_number
 * i18n_value_must_positive_integer
 * i18n_value_must_zero_or_positive_integer
 * i18n_value_must_coordinate
 * i18n_value_must_negative_integer
 * i18n_value_must_unit_interval
 * i18n_value_must_percentage
 *
 * @param value Value to check against
 * @param valueType Value type (from data element, option set, etc)
 */
dhis2.validation.isValidValueType = function(value, valueType) {
  switch( valueType ) {
    case 'TEXT':
    case 'LONG_TEXT':
    case 'USERNAME':
    case 'DATE':
    case 'DATETIME':
    {
      break;
    }
    case 'INTEGER':
    {
      if( !dhis2.validation.isInt(value) ) {
        setHeaderDelayMessage(i18n_value_must_integer);
        return false;
      }
      break;
    }
    case 'INTEGER_POSITIVE':
    {
      if( !dhis2.validation.isPositiveInt(value) ) {
        setHeaderDelayMessage(i18n_value_must_positive_integer);
        return false;
      }
      break;
    }
    case 'INTEGER_NEGATIVE':
    {
      if( !dhis2.validation.isNegativeInt(value) ) {
        setHeaderDelayMessage(i18n_value_must_negative_integer);
        return false;
      }
      break;
    }
    case 'INTEGER_ZERO_OR_POSITIVE':
    {
      if( !dhis2.validation.isZeroOrPositiveInt(value) ) {
        setHeaderDelayMessage(i18n_value_must_zero_or_positive_integer);
        return false;
      }
      break;
    }
    case 'COORDINATE':
    {
      if( !dhis2.validation.isCoordinate(value) ) {
        setHeaderDelayMessage(i18n_value_must_coordinate);
        return false;
      }
      break;
    }
    case 'NUMBER':
    {
      if( !dhis2.validation.isNumber(value) ) {
        setHeaderDelayMessage(i18n_value_must_number);
        return false;
      }
      break;
    }
    case 'UNIT_INTERVAL':
    {
      if( !dhis2.validation.isUnitInterval(value) ) {
        setHeaderDelayMessage(i18n_value_must_unit_interval);
        return false;
      }
      break;
    }
    case 'PERCENTAGE':
    {
      if( !dhis2.validation.isPercentage(value) ) {
        setHeaderDelayMessage(i18n_value_must_percentage);
        return false;
      }
      break;
    }
  }

  return true;
};
