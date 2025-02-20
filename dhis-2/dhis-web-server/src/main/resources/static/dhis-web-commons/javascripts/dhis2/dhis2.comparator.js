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


dhis2.util.namespace('dhis2.comparator');

/**
 * Compares two objects and returns result.
 *
 * @param a {object} Object A
 * @param b {object} Object B
 *
 * @returns {Number} 1 if a>b, -1 if a>b, 0 if equal
 */
dhis2.comparator.defaultComparator = function( a, b ) {
  if( a === b ) {
    return 0;
  }

  return ( a > b ) ? 1 : -1;
};

/**
 * Case sensitive compare of two jQuery objects (based on their innerHTML).
 *
 * @param a {jQuery} Object A
 * @param b {jQuery} Object B
 *
 * @returns {Number} 1 if a>b, -1 if a>b, 0 if equal
 */
dhis2.comparator.htmlComparator = function( a, b ) {
  return dhis2.comparator.defaultComparator(a.html(), b.html());
};

/**
 * Case insensitive compare of two jQuery objects (based on their innerHTML).
 *
 * @param a {jQuery} Object A
 * @param b {jQuery} Object B
 *
 * @returns {Number} 1 if a>b, -1 if a>=b
 */
dhis2.comparator.htmlNoCaseComparator = function( a, b ) {
  a = !!a ? a.html() : a;
  b = !!b ? b.html() : b;

  a = !!a ? a.toLowerCase() : a;
  b = !!b ? b.toLowerCase() : b;

  return dhis2.comparator.defaultComparator(a, b);
};
