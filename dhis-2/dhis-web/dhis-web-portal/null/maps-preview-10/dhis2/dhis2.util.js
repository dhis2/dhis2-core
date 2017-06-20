"use strict";

/*
 * Copyright (c) 2004-2013, University of Oslo
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

var dhis2 = dhis2 || {};
dhis2['util'] = dhis2['util'] || {};

/**
 * Creates namespace object based on path
 *
 * @param path {String} The path of the namespace, i.e. 'a.b.c'
 *
 * @returns {object} Namespace object
 */
dhis2.util.namespace = function( path ) {
  var parts = path.split('.');
  var parent = window;
  var currentPart = '';

  for( var i = 0, length = parts.length; i < length; i++ ) {
    currentPart = parts[i];
    parent[currentPart] = parent[currentPart] || {};
    parent = parent[currentPart];
  }

  return parent;
};

/**
 * Escape function for regular expressions.
 */
dhis2.util.escape = function( text ) {
  return text.replace(/[-[\]{}()*+?.,\/\\^$|#\s]/g, "\\$&");
};

/**
 * Convert a Java properties file into a javascript object.
 */
dhis2.util.parseJavaProperties = function( javaProperties ) {
    var obj = {}, lines;
    
    if (typeof javaProperties !== 'string') {
        return obj;
    }
    
    lines = javaProperties.split(/\n/);

    for (var i = 0, a; i < lines.length; i++) {
        if (!!(typeof lines[i] === 'string' && lines[i].length && lines[i].indexOf('=') !== -1)) {
            a = lines[i].split('=');
            obj[a[0].trim()] = eval('"' + a[1].trim().replace(/"/g, '\'') + '"');
        }
    }

    return obj;
};

/**
 * jQuery cannot correctly filter strings with () in them, so here is a fix
 * until jQuery gets updated.
 */
dhis2.util.jqTextFilterCaseSensitive = function( key, not ) {
  key = dhis2.util.escape(key);
  not = not || false;

  if( not ) {
    return function( i, el ) {
      return !!!$(el).text().match("" + key);
    };
  }
  else {
    return function( i, el ) {
      return !!$(el).text().match("" + key);
    };
  }
};

dhis2.util.jqTextFilter = function( key, not ) {
  key = dhis2.util.escape(key).toLowerCase();
  not = not || false;

  if( not ) {
    return function( i, el ) {
      return !!!$(el).text().toLowerCase().match("" + key);
    };
  }
  else {
    return function( i, el ) {
      return !!$(el).text().toLowerCase().match("" + key);
    };
  }
};

/**
 * Generates a valid UUID.
 */
dhis2.util.uuid = function() {
  var S4 = function() {
    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
  };

  return (S4() + S4() + "-" + S4() + "-" + S4() + "-" + S4() + "-" + S4() + S4() + S4());
};

/**
 * Generates a valid UID.
 */
dhis2.util.uid = function() {
  var letters = "abcdefghijklmnopqrstuvwxyz" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  var allowedChars = "0123456789" + letters;
  var NUMBER_OF_CODEPOINTS = allowedChars.length;
  var CODESIZE = 11;
  var uid;

  //the uid should start with a char 
  uid = letters.charAt( Math.random() * (letters.length) );

  for ( var i = 1; i < CODESIZE; ++i ){
    uid += allowedChars.charAt( Math.random() * (NUMBER_OF_CODEPOINTS) );
  }
  
  return uid;
};

/**
 * Normalizes an argument object returned from a jQuery promise. If the argument
 * is undefined, not an array or an empty array, undefined is returned. If the
 * argument is a single promise object, the object is wrapped in an array. If the
 * argument is an array of promise objects, the array is returned unmodified.
 */
dhis2.util.normalizeArguments = function( args ) {
  if( !args || !args.length || !args[0] ) {
    return undefined;
  }

  if( $.isArray(args[0]) ) {
    return args;
  }
  else {
    var arr = [];
    arr[0] = args;
    return arr;
  }
};

/**
 * Convenience method to be used from inside custom forms. When a function is
 * registered inside a form it will be loaded every time the form is loaded,
 * hence the need to unregister and the register the function.
 */
dhis2.util.on = function( event, fn ) {
    $( document ).off( event ).on( event, fn );
};

/**
 * Returns a query parameter string where _ is the parameter and the time stamp
 * in milliseconds is the value, intended to force fresh non-cached responses
 * from server.
 */
dhis2.util.cacheBust = function() {
	return "_=" + new Date().getTime();
}

/**
 * Sorts the two given objects on the name property.
 */
dhis2.util.nameSort = function( a, b ) {
    return a.name > b.name ? 1 : a.name < b.name ? -1 : 0;
}

/**
 * adds ':containsNC' to filtering.
 * $(sel).find(':containsNC(key)').doSomething();
 */
$.expr[":"].containsNC = function( a, i, m, r ) {
  var search = dhis2.util.escape(m[3]);
  return jQuery(a).text().toUpperCase().indexOf(m[search].toUpperCase()) >= 0;
};

/**
 * adds ':regex' to filtering, use to filter by regular expression
 */
$.expr[":"].regex = function( a, i, m, r ) {
  var re = new RegExp(m[3], 'i');
  return re.test(jQuery(a).text());
};

/**
 * adds ':regex' to filtering, use to filter by regular expression
 * (this is the case sensitive version)
 */
$.expr[":"].regexCS = function( a, i, m, r ) {
  var re = new RegExp(m[3]);
  return re.test(jQuery(a).text());
};

/**
 * Returns an array of the keys in a given object. Will use ES5 Object.keys() if
 * available, if not it will provide a pure javascript implementation.
 * @returns array of keys
 */
if( !Object.keys ) {
  Object.keys = function( obj ) {
    var keys = [];
    for( var k in obj )
      if( obj.hasOwnProperty(k) )
        keys.push(k);
    return keys;
  };
}

// http://stackoverflow.com/questions/3326650/console-is-undefined-error-for-internet-explorer
(function() {
  var method;
  var noop = function() {
  };

  var methods = [
    'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
    'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
    'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
    'timeStamp', 'trace', 'warn'
  ];

  var length = methods.length;
  var console = (window.console = window.console || {});

  while( length-- ) {
    method = methods[length];

    // Only stub undefined methods.
    if( !console[method] ) {
      console[method] = noop;
    }
  }

  // this shouldn't really be used anymore, but leaving it in for legacy reasons
  window.log = function( msg ) {
    console.log(msg);
  }
}());
