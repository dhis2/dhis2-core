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
 * An abstract representation of color.
 */
mapfish.Color = OpenLayers.Class({
    getColorRgb: function() {}
});

/**
 * Class: mapfish.ColorRgb
 * Class for representing RGB colors.
 */
mapfish.ColorRgb = OpenLayers.Class(mapfish.Color, {
    redLevel: null,
    greenLevel: null,
    blueLevel: null,

    /**
     * Constructor: mapfish.ColorRgb
     *
     * Parameters:
     * red - {Integer}
     * green - {Integer}
     * blue - {Integer}
     */
    initialize: function(red, green, blue) {
        this.redLevel = red;
        this.greenLevel = green;
        this.blueLevel = blue;
    },

    /**
     * APIMethod: equals
     *      Returns true if the colors at the same.
     *
     * Parameters:
     * {<mapfish.ColorRgb>} color
     */
    equals: function(color) {
        return color.redLevel == this.redLevel &&
               color.greenLevel == this.greenLevel &&
               color.blueLevel == this.blueLevel;
    },
    
    getColorRgb: function() {
        return this;
    },
    
    getRgbArray: function() {
        return [
            this.redLevel,
            this.greenLevel,
            this.blueLevel
        ];
    },
    
    /**
     * Method: hex2rgbArray
     * Converts a Hex color string to an Rbg array 
     *
     * Parameters:
     * rgbHexString - {String} Hex color string (format: #rrggbb)
     */
    hex2rgbArray: function(rgbHexString) {
        if (rgbHexString.charAt(0) == '#') {
            rgbHexString = rgbHexString.substr(1);
        }
        var rgbArray = [
            parseInt(rgbHexString.substring(0,2),16),
            parseInt(rgbHexString.substring(2,4),16),
            parseInt(rgbHexString.substring(4,6),16)
        ];
        for (var i = 0; i < rgbArray.length; i++) {
            if (rgbArray[i] < 0 || rgbArray[i] > 255 ) {
                OpenLayers.Console.error("Invalid rgb hex color string: rgbHexString");
            }
        }        
        return rgbArray;
    },
    
    /**
     * APIMethod: setFromHex
     * Sets the color from a color hex string 
     *
     * Parameters:
     * rgbHexString - {String} Hex color string (format: #rrggbb)
     */
    setFromHex: function(rgbHexString) {
        var rgbArray = this.hex2rgbArray(rgbHexString);
        this.redLevel = rgbArray[0];
        this.greenLevel = rgbArray[1];
        this.blueLevel = rgbArray[2];
    },
    
    /**
     * APIMethod: setFromRgb
     * Sets the color from a color rgb string
     *
     */
    setFromRgb: function(rgbString) {
        var color = dojo.colorFromString(rgbString);
        this.redLevel = color.r;
        this.greenLevel = color.g;
        this.blueLevel = color.b;
    },
    
    /**
     * APIMethod: toHexString
     * Converts the rgb color to hex string
     *
     */
    toHexString: function() {
        var r = this.toHex(this.redLevel);
        var g = this.toHex(this.greenLevel);
        var b = this.toHex(this.blueLevel);
        return '#' + r + g + b;
    },
    
    /**
     * Method: toHex
     * Converts a color level to its hexadecimal value
     *
     * Parameters:
     * dec - {Integer} Decimal value to convert [0..255]
     */
    toHex: function(dec) {
        // create list of hex characters
        var hexCharacters = "0123456789ABCDEF";
        // if number is out of range return limit
        if (dec < 0 || dec > 255 ) {
            var msg = "Invalid decimal value for color level";
            OpenLayers.Console.error(msg);
        }
        // decimal equivalent of first hex character in converted number
        var i = Math.floor(dec / 16);
        // decimal equivalent of second hex character in converted number
        var j = dec % 16;
        // return hexadecimal equivalent
        return hexCharacters.charAt(i) + hexCharacters.charAt(j);
    },
    
    CLASS_NAME: "mapfish.ColorRgb"
});

/**
 * APIMethod: getColorsArrayByRgbInterpolation
 *      Get an array of colors based on RGB interpolation.
 *
 * Parameters:
 * firstColor - {<mapfish.Color>} The first color in the range.
 * lastColor - {<mapfish.Color>} The last color in the range.
 * nbColors - {Integer} The number of colors in the range.
 *
 * Returns
 * {Array({<mapfish.Color>})} The resulting array of colors.
 */
mapfish.ColorRgb.getColorsArrayByRgbInterpolation =
    function(firstColor, lastColor, nbColors) {

    var resultColors = [];
    var colorA = firstColor.getColorRgb();
    var colorB = lastColor.getColorRgb();
    var colorAVal = colorA.getRgbArray();
    var colorBVal = colorB.getRgbArray();
    if (nbColors == 1) {
        return [colorA];
    }
    for (var i = 0; i < nbColors; i++) {
        var rgbTriplet = [];
        rgbTriplet[0] = colorAVal[0] + 
            i * (colorBVal[0] - colorAVal[0]) / (nbColors - 1);
        rgbTriplet[1] = colorAVal[1] + 
            i * (colorBVal[1] - colorAVal[1]) / (nbColors - 1);
        rgbTriplet[2] = colorAVal[2] + 
            i * (colorBVal[2] - colorAVal[2]) / (nbColors - 1);
        resultColors[i] = new mapfish.ColorRgb(parseInt(rgbTriplet[0]), 
            parseInt(rgbTriplet[1]), parseInt(rgbTriplet[2]));
    }
    return resultColors;
};
