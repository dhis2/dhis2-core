/*
 * Copyright (C) 2007  Camptocamp
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
 * @requires core/GeoStat.js
 */

mapfish.GeoStat.Symbol = OpenLayers.Class(mapfish.GeoStat, {

    colors: [
        new mapfish.ColorRgb(120, 120, 0),
        new mapfish.ColorRgb(255, 0, 0)
    ],

    method: mapfish.GeoStat.Distribution.CLASSIFY_BY_QUANTILS,

    numClasses: 5,
	
	minSize: 3,
	
	maxSize: 20,
	
	minVal: null,
	
	maxVal: null,

    defaultSymbolizer: {'fillOpacity': 1},

    classification: null,

    symbolizerInterpolation: null,
    
    widget: null,

    initialize: function(map, options) {
        mapfish.GeoStat.prototype.initialize.apply(this, arguments);
    },

    updateOptions: function(newOptions) {
        var oldOptions = OpenLayers.Util.extend({}, this.options);
        this.addOptions(newOptions);
        if (newOptions) {
            this.setClassification();
        }
    },
    
    createSymbolizerInterpolation: function() {
        this.widget.imageLegend = [];        
        this.symbolizerInterpolation = this.widget.symbolizerInterpolation;
            
        for (var i = 0; i < this.classification.bins.length; i++) {
            this.widget.imageLegend.push({
                label: this.classification.bins[i].label.replace('&nbsp;&nbsp;', ' '),
                color: this.symbolizerInterpolation[i]
            });
        }
    },

    setClassification: function() {
        var values = [];
        for (var i = 0; i < this.layer.features.length; i++) {
            values.push(this.layer.features[i].attributes[this.indicator]);
        }
        
        var distOptions = {
            'labelGenerator': this.options.labelGenerator
        };
        var dist = new mapfish.GeoStat.Distribution(values, distOptions);

		this.minVal = dist.minVal;
        this.maxVal = dist.maxVal;

        this.classification = dist.classify(
            this.method,
            this.numClasses,
            null
        );

        this.createSymbolizerInterpolation();
    },

    applyClassification: function(options, widget) {
        this.widget = widget;
        this.updateOptions(options);
        
        var boundsArray = this.classification.getBoundsArray();
        var rules = new Array(boundsArray.length-1);
        for (var i = 0; i < boundsArray.length-1; i++) {
            var rule = new OpenLayers.Rule({                
                symbolizer: {
                    'pointRadius': this.symbolizerInterpolation[i] == 'blank' ? 0 : 16,
                    'externalGraphic': '../resources/ext-ux/iconcombo/' + this.symbolizerInterpolation[i] + '-map.png'
                }, 
                filter: new OpenLayers.Filter.Comparison({
                    type: OpenLayers.Filter.Comparison.BETWEEN,
                    property: this.indicator,
                    lowerBoundary: boundsArray[i],
                    upperBoundary: boundsArray[i + 1]
                })
            });
            rules[i] = rule;
        }

        this.extendStyle(rules);
        mapfish.GeoStat.prototype.applyClassification.apply(this, arguments);
    },

    updateLegend: function() {
        if (!this.legendDiv) {
            return;
        }
        
        var info = this.widget.formValues.getLegendInfo.call(this.widget);
        var element;
        this.legendDiv.update("");
        
        for (var p in info) {
            element = document.createElement("div");
            element.style.height = "14px";
            element.innerHTML = info[p];
            this.legendDiv.appendChild(element);
            
            element = document.createElement("div");
            element.style.clear = "left";
            this.legendDiv.appendChild(element);
        }
        
        element = document.createElement("div");
        element.style.width = "1px";
        element.style.height = "5px";
        this.legendDiv.appendChild(element);
        
        for (var i = 0; i < this.classification.bins.length; i++) {
            var element = document.createElement("div");
            element.style.backgroundImage = 'url(../resources/ext-ux/iconcombo/' + this.symbolizerInterpolation[i] + '.png)';
            element.style.backgroundRepeat = 'no-repeat';
            element.style.width = "25px";
            element.style.height = this.widget.legendNames[i] ? "25px" : "20px";
            element.style.cssFloat = "left";
            element.style.marginRight = "5px";
            this.legendDiv.appendChild(element);

            element = document.createElement("div");
            element.style.lineHeight = this.widget.legendNames[i] ? "12px" : "7px";
            element.innerHTML = '<b style="color:#222">' + (this.widget.legendNames[i] || '') + '</b><br/>' + this.classification.bins[i].label;
            this.legendDiv.appendChild(element);

            element = document.createElement("div");
            element.style.clear = "left";
            this.legendDiv.appendChild(element);
        }
    },

    CLASS_NAME: "mapfish.GeoStat.Symbol"
});
