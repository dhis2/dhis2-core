/*
 * Copyright (C) 2007-2008  Camptocamp
 *
 * This file is part of MapFish Client
 *
 * MapFish Client is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Client is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Client.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Namespace: mapfish.Util
 * Utility functions
 */
mapfish.Util = {};

/**
 * APIFunction: sum
 * Return the sum of the elements of an array.
 */
mapfish.Util.sum = function(array) {
    for (var i=0, sum=0; i < array.length; sum += array[i++]);
    return sum;
};

/**
 * APIFunction: max
 * Return the max of the elements of an array.
 */
mapfish.Util.max = function(array) {
    return Math.max.apply({}, array);
};

/**
 * APIFunction: min
 * Return the min of the elements of an array.
 */
mapfish.Util.min = function(array) {
    return Math.min.apply({}, array);
};

/**
 * Function: getIconUrl
 * Builds the URL for a layer icon, based on a WMS GetLegendGraphic request.
 *
 * Parameters:
 * wmsUrl - {String} The URL of a WMS server.
 * options - {Object} The options to set in the request:
 *                    'layer' - the name of the layer for which the icon is requested (required)
 *                    'rule' - the name of a class for this layer (this is set to the layer name if not specified)
 *                    'format' - "image/png" by default
 *                    ...
 *
 * Returns:
 * {String} The URL at which the icon can be found.
 */
mapfish.Util.getIconUrl = function(wmsUrl, options) {
    if (!options.layer) {
        OpenLayers.Console.warn(
            'Missing required layer option in mapfish.Util.getIconUrl');
        return '';
    }
    if (!options.rule) {
        options.rule = options.layer;
    }
    if (wmsUrl.indexOf("?") < 0) {
        //add a ? to the end of the url if it doesn't
        //already contain one
        wmsUrl += "?";
    } else if (wmsUrl.lastIndexOf('&') != (wmsUrl.length - 1)) {
        //if there was already a ? , assure that the parameters
        //are ended with an &, except if the ? was at the last char
        if (wmsUrl.indexOf("?") != (wmsUrl.length - 1)) {
            wmsUrl += "&";
        }
    }
    var options = OpenLayers.Util.extend({
        layer: "",
        rule: "",
        service: "WMS",
        version: "1.1.1",
        request: "GetLegendGraphic",
        format: "image/png",
        width: 16,
        height: 16
    }, options);
    options = OpenLayers.Util.upperCaseObject(options);
    return wmsUrl + OpenLayers.Util.getParameterString(options);
};


/**
 * APIFunction: arrayEqual
 * Compare two arrays containing primitive types.
 *
 * Parameters:
 * a - {Array} 1st to be compared.
 * b - {Array} 2nd to be compared.
 *
 * Returns:
 * {Boolean} True if both given arrays contents are the same (elements value and type).
 */
mapfish.Util.arrayEqual = function(a, b) {
    if(a == null || b == null)
        return false;
    if(typeof(a) != 'object' || typeof(b) != 'object')
        return false;
    if (a.length != b.length)
        return false;
    for (var i = 0; i < a.length; i++) {
        if (typeof(a[i]) != typeof(b[i]))
            return false;
        if (a[i] != b[i])
            return false;
    }
    return true;
};

/**
 * Function: isIE7
 *
 * Returns:
 * {Boolean} True if the browser is Internet Explorer V7
 */
mapfish.Util.isIE7 = function () {
    var ua = navigator.userAgent.toLowerCase();
    return ua.indexOf("msie 7") > -1;
};

/**
 * APIFunction: relativeToAbsoluteURL
 *
 * Parameters:
 * source - {String} the source URL
 *
 * Returns:
 * {String} An absolute URL
 */
mapfish.Util.relativeToAbsoluteURL = function(source) {
    if (/^\w+:/.test(source) || !source) {
        return source;
    }

    var h = location.protocol + "//" + location.host;
    if (source.indexOf("/") == 0) {
        return h + source;
    }

    var p = location.pathname.replace(/\/[^\/]*$/, '');
    return h + p + "/" + source;
};

/**
 * Function: fixArray
 *
 * In some fields, OpenLayers allows to use a coma separated string instead
 * of an array. This method make sure we end up with an array.
 *
 * Parameters:
 * subs - {String/Array}
 *
 * Returns:
 * {Array}
 */
mapfish.Util.fixArray = function(subs) {
    if (subs == '' || subs == null) {
        return [];
    } else if (subs instanceof Array) {
        return subs;
    } else {
        return subs.split(',');
    }
};

