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

mapfish.GeoStat.Boundary = OpenLayers.Class(mapfish.GeoStat, {

    colors: [new mapfish.ColorRgb(120, 120, 0), new mapfish.ColorRgb(255, 0, 0)],
    method: mapfish.GeoStat.Distribution.CLASSIFY_BY_QUANTILS,
    numClasses: 5,
	minSize: 3,
	maxSize: 20,
	minVal: null,
	maxVal: null,
    defaultSymbolizer: {'fillOpacity': 1},
    classification: null,
    colorInterpolation: null,

    gis: null,
    view: null,
    featureStore: Ext.create('Ext.data.Store', {
		fields: ['id', 'name'],
		features: [],
		loadFeatures: function(features) {
			if (features && features.length) {
				var data = [];
				for (var i = 0; i < features.length; i++) {
					data.push([features[i].attributes.id, features[i].attributes.name]);
				}
				this.loadData(data);
				this.sortStore();

				this.features = features;
			}
			else {
				this.removeAll();
			}
		},
		sortStore: function() {
			this.sort('name', 'ASC');
		}
	}),

    initialize: function(map, options) {
        mapfish.GeoStat.prototype.initialize.apply(this, arguments);
    },

    getLoader: function() {
		return GIS.core.LayerLoaderBoundary(this.gis, this.layer);
	},

	reset: function() {
		this.layer.destroyFeatures();

		if (this.layer.widget) {
			this.layer.widget.reset();
		}
	},

	extendView: function(view, config) {
		view = view || this.view;

		view.organisationUnitLevel = config.organisationUnitLevel || view.organisationUnitLevel;
		view.parentOrganisationUnit = config.parentOrganisationUnit || view.parentOrganisationUnit;
		view.parentLevel = config.parentLevel || view.parentLevel;
		view.parentGraph = config.parentGraph || view.parentGraph;
		view.opacity = config.opacity || view.opacity;

		return view;
	},

	getLegendConfig: function() {
		return;
	},

	getImageLegendConfig: function() {
		return;
	},

    updateOptions: function(newOptions) {
        var oldOptions = OpenLayers.Util.extend({}, this.options);
        this.addOptions(newOptions);
        if (newOptions) {
            this.setClassification();
        }
    },

    createColorInterpolation: function() {
        var numColors = this.classification.bins.length;

		this.colorInterpolation = mapfish.ColorRgb.getColorsArrayByRgbInterpolation(this.colors[0], this.colors[1], numColors);
    },

    setClassification: function() {
        var values = [];
        for (var i = 0; i < this.layer.features.length; i++) {
            values.push(this.layer.features[i].attributes[this.indicator]);
        }

        var distOptions = {
            labelGenerator: this.options.labelGenerator
        };
        var dist = new mapfish.GeoStat.Distribution(values, distOptions);

		this.minVal = dist.minVal;
        this.maxVal = dist.maxVal;

        this.classification = dist.classify(
            this.method,
            this.numClasses,
            null
        );

        this.createColorInterpolation();
    },

    applyClassification: function(options) {
        this.updateOptions(options);

		var calculateRadius = OpenLayers.Function.bind(
			function(feature) {
				var value = feature.attributes[this.indicator];
                var size = (value - this.minVal) / (this.maxVal - this.minVal) *
					(this.maxSize - this.minSize) + this.minSize;
                return size || this.minSize;
            },	this
		);
		this.extendStyle(null, {'pointRadius': '${calculateRadius}'}, {'calculateRadius': calculateRadius});

        var boundsArray = this.classification.getBoundsArray();
        var rules = new Array(boundsArray.length-1);
        for (var i = 0; i < boundsArray.length-1; i++) {
            var rule = new OpenLayers.Rule({
                symbolizer: {fillColor: this.colorInterpolation[i].toHexString()},
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

    },

    CLASS_NAME: "mapfish.GeoStat.Boundary"
});
