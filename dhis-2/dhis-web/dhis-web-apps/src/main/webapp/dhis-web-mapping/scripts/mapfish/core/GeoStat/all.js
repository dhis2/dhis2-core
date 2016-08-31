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

mapfish.GeoStat.Facility = OpenLayers.Class(mapfish.GeoStat, {

    classification: null,

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
		return GIS.core.LayerLoaderFacility(this.gis, this.layer);
	},

	decode: function(organisationUnits) {
		var feature,
			group,
			attr,
			geojson = {
				type: 'FeatureCollection',
				crs: {
					type: 'EPSG',
					properties: {
						code: '4326'
					}
				},
				features: []
			};

        for (var i = 0; i < organisationUnits.length; i++) {
			attr = organisationUnits[i];

			feature = {
                type: 'Feature',
                geometry: {
                    type: parseInt(attr.ty) === 1 ? 'Point' : 'MultiPolygon',
                    coordinates: JSON.parse(attr.co)
                },
                properties: {
                    id: attr.id,
                    name: attr.na
                }
            };
            feature.properties = Ext.Object.merge(feature.properties, attr.dimensions);

            geojson.features.push(feature);
        }

        return geojson;
    },

	reset: function() {
		this.layer.destroyFeatures();

		// Legend
		this.layer.legendPanel.update('');
		this.layer.legendPanel.collapse();

		if (this.layer.widget) {
			this.layer.widget.reset();
		}
	},

	extendView: function(view, config) {
		view = view || this.view;

		view.organisationUnitGroupSet = config.organisationUnitGroupSet || view.organisationUnitGroupSet;
		view.organisationUnitLevel = config.organisationUnitLevel || view.organisationUnitLevel;
		view.parentOrganisationUnit = config.parentOrganisationUnit || view.parentOrganisationUnit;
		view.parentLevel = config.parentLevel || view.parentLevel;
		view.parentGraph = config.parentGraph || view.parentGraph;
		view.opacity = config.opacity || view.opacity;

		return view;
	},

    updateOptions: function(newOptions) {
        this.addOptions(newOptions);
    },

    applyClassification: function(options) {
        this.updateOptions(options);

        var items = this.gis.store.groupsByGroupSet.data.items;

        var rules = new Array(items.length);
        for (var i = 0; i < items.length; i++) {
            var rule = new OpenLayers.Rule({
                symbolizer: {
                    'pointRadius': 8,
                    'externalGraphic': '../images/orgunitgroup/' + items[i].data.symbol
                },
                filter: new OpenLayers.Filter.Comparison({
                    type: OpenLayers.Filter.Comparison.EQUAL_TO,
                    property: this.indicator,
                    value: items[i].data.name
                })
            });
            rules[i] = rule;
        }

        this.extendStyle(rules);
        mapfish.GeoStat.prototype.applyClassification.apply(this, arguments);
    },

    updateLegend: function() {
		var	element = document.createElement("div"),
			child = document.createElement("div"),
			items = this.gis.store.groupsByGroupSet.data.items;

        for (var i = 0; i < items.length; i++) {
            child = document.createElement("div");
            child.style.backgroundImage = 'url(../images/orgunitgroup/' + items[i].data.symbol + ')';
            child.style.backgroundRepeat = 'no-repeat';
            child.style.width = "21px";
            child.style.height = "16px";
            child.style.marginBottom = "2px";
            child.style.cssFloat = "left";
            element.appendChild(child);

            child = document.createElement("div");
            child.innerHTML = items[i].data.name;
            child.style.height = "16px";
            child.style.lineHeight = "17px";
            element.appendChild(child);

            child = document.createElement("div");
            child.style.clear = "left";
            element.appendChild(child);
        }

        this.layer.legendPanel.update(element.outerHTML);
    },

    CLASS_NAME: "mapfish.GeoStat.Facility"
});

mapfish.GeoStat.Event = OpenLayers.Class(mapfish.GeoStat, {

    colors: [new mapfish.ColorRgb(120, 120, 0), new mapfish.ColorRgb(255, 0, 0)],
    method: mapfish.GeoStat.Distribution.CLASSIFY_BY_QUANTILS,
    numClasses: 5,
	minSize: 4,
	maxSize: 4,
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
		return GIS.core.LayerLoaderEvent(this.gis, this.layer);
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
        return {};
    },

    CLASS_NAME: "mapfish.GeoStat.Event"
});

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

    getDefaultFeatureStyle: function() {
        return {
            fillOpacity: 0,
            fillColor: '#000',
            strokeColor: '#000',
            strokeWidth: 1,
            pointRadius: 5,
            cursor: 'pointer'
        };
    },

    setFeatureStyle: function(style) {
        for (var i = 0; i < this.layer.features.length; i++) {
            this.layer.features[i].style = style;
        }

        this.layer.redraw();
    },

    setFeatureLabelStyle: function(isLabel, skipDraw, view) {
        for (var i = 0, feature, style, label; i < this.layer.features.length; i++) {
            feature = this.layer.features[i];
            style = feature.style;

            if (isLabel) {
                style.label = feature.attributes.label;
                style.fontColor = style.strokeColor;
                style.fontWeight = style.strokeWidth > 1 ? 'bold' : 'normal';
                style.labelAlign = 'cr';
                style.labelYOffset = 13;

                if (view.labelFontSize) {
                    style.fontSize = view.labelFontSize;
                }
                if (view.labelFontStyle) {
                    style.fontStyle = view.labelFontStyle;
                }
            }
            else {
                style.label = null;
            }
        }

        if (!skipDraw) {
            this.layer.redraw();
        }
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
        return {};
    },

    CLASS_NAME: "mapfish.GeoStat.Boundary"
});

mapfish.GeoStat.createThematic = function(name) {

	mapfish.GeoStat[name] = OpenLayers.Class(mapfish.GeoStat, {
		colors: [new mapfish.ColorRgb(120, 120, 0), new mapfish.ColorRgb(255, 0, 0)],
		method: mapfish.GeoStat.Distribution.CLASSIFY_BY_QUANTILS,
		numClasses: 5,
		bounds: null,
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

					this.features = features;

					for (var i = 0; i < features.length; i++) {
						data.push([features[i].attributes.id, features[i].attributes.name]);
					}
					this.loadData(data);
					this.sortStore();

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
			return GIS.core.LayerLoaderThematic(this.gis, this.layer);
		},

		reset: function() {
			this.layer.destroyFeatures();
			this.featureStore.loadFeatures(this.layer.features.slice(0));

			// Legend
			this.layer.legendPanel.update('');
			this.layer.legendPanel.collapse();

			// Widget
			if (this.layer.widget) {
				this.layer.widget.reset();
			}
		},

		extendView: function(view, config) {
			view = view || this.view;

			view.valueType = config.valueType || view.valueType;
			view.indicatorGroup = config.indicatorGroup || view.indicatorGroup;
			view.indicator = config.indicator || view.indicator;
			view.dataElementGroup = config.dataElementGroup || view.dataElementGroup;
			view.dataElement = config.dataElement || view.dataElement;
			view.periodType = config.periodType || view.periodType;
			view.period = config.period || view.period;
			view.legendType = config.legendType || view.legendType;
			view.legendSet = config.legendSet || view.legendSet;
			view.classes = config.classes || view.classes;
			view.method = config.method || view.method;
			view.colorLow = config.colorLow || view.colorLow;
			view.colorHigh = config.colorHigh || view.colorHigh;
			view.radiusLow = config.radiusLow || view.radiusLow;
			view.radiusHigh = config.radiusHigh || view.radiusHigh;
			view.organisationUnitLevel = config.organisationUnitLevel || view.organisationUnitLevel;
			view.parentOrganisationUnit = config.parentOrganisationUnit || view.parentOrganisationUnit;
			view.parentLevel = config.parentLevel || view.parentLevel;
			view.parentGraph = config.parentGraph || view.parentGraph;
			view.opacity = config.opacity || view.opacity;

			return view;
		},

		getImageLegendConfig: function() {
			var bins = this.classification.bins,
				rgb = this.colorInterpolation,
				config = [];

			for (var i = 0; i < bins.length; i++) {
				config.push({
					color: rgb[i].toHexString(),
					label: bins[i].lowerBound.toFixed(1) + ' - ' + bins[i].upperBound.toFixed(1) + ' (' + bins[i].nbVal + ')'
				});
			}

			return config;
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

			if (!this.view.legendSet) {
				this.colorInterpolation = mapfish.ColorRgb.getColorsArrayByRgbInterpolation(this.colors[0], this.colors[1], numColors);
			}
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

			if (this.view.legendType === this.gis.conf.finals.widget.legendtype_predefined) {
				if (this.bounds[0] > this.minVal) {
					this.bounds.unshift(this.minVal);
					//if (this.widget == centroid) { this.widget.symbolizerInterpolation.unshift('blank');
					this.colorInterpolation.unshift(new mapfish.ColorRgb(240,240,240));
				}

				if (this.bounds[this.bounds.length-1] < this.maxVal) {
					this.bounds.push(this.maxVal);
					//todo if (this.widget == centroid) { G.vars.activeWidget.symbolizerInterpolation.push('blank');
					this.colorInterpolation.push(new mapfish.ColorRgb(240,240,240));
				}
			}

			this.classification = dist.classify(
				this.method,
				this.numClasses,
				this.bounds
			);

			this.createColorInterpolation();
		},

		applyClassification: function(options, legend) {
			this.updateOptions(options, legend);

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
			var rules = new Array(boundsArray.length - 1);
			for (var i = 0; i < boundsArray.length - 1; i++) {
				var rule = new OpenLayers.Rule({
					symbolizer: {
						fillColor: this.colorInterpolation[i].toHexString()
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
			var	view = this.view,
                response = this.gis.response,
                isPlugin = this.gis.plugin,
                element = document.createElement("div"),
                style = {},
                legendNames = view.legendSet ? view.legendSet.names || {} : {},
				child,
                id,
                name;

                style.dataLineHeight = isPlugin ? '12px' : '14px';
                style.dataPaddingBottom = isPlugin ? '1px' : '3px';
                style.colorWidth = isPlugin ? '15px' : '30px';
                style.colorHeight = isPlugin ? '13px' : '15px';
                style.colorMarginRight = isPlugin ? '5px' : '8px';
                style.fontSize = isPlugin ? '10px' : '11px';

			// data
            id = view.columns[0].items[0].id;
            name = view.columns[0].items[0].name;
			child = document.createElement("div");
            child.style.lineHeight = style.dataLineHeight;
            child.style.paddingBottom = style.dataPaddingBottom;
            child.innerHTML += response.metaData.names[id] || name || id;
            child.innerHTML += "<br/>";

			// period
            id = view.filters[0].items[0].id;
            name = view.filters[0].items[0].name;
            child.innerHTML += response.metaData.names[id] || name || id;
			element.appendChild(child);

			child = document.createElement("div");
			child.style.clear = "left";
			element.appendChild(child);

			// legends
			if (view.legendSet) {
				for (var i = 0; i < this.classification.bins.length; i++) {
					child = document.createElement("div");
					child.style.backgroundColor = this.colorInterpolation[i].toHexString();
					child.style.width = style.colorWidth;
					child.style.height = legendNames[i] ? '25px' : style.colorHeight;
					child.style.cssFloat = 'left';
					child.style.marginRight = style.colorMarginRight;
					element.appendChild(child);

					child = document.createElement("div");
					child.style.lineHeight = legendNames[i] ? "12px" : "7px";
					child.innerHTML = '<b style="color:#222; font-size:10px !important">' + (legendNames[i] || '') + '</b><br/>' + this.classification.bins[i].label;
					element.appendChild(child);

					child = document.createElement("div");
					child.style.clear = "left";
					element.appendChild(child);
				}
			}
			else {
				for (var i = 0; i < this.classification.bins.length; i++) {
					child = document.createElement("div");
					child.style.backgroundColor = this.colorInterpolation[i].toHexString();
					child.style.width = style.colorWidth;
					child.style.height = style.colorHeight;
					child.style.cssFloat = 'left';
					child.style.marginRight = style.colorMarginRight;
					element.appendChild(child);

					child = document.createElement("div");
					child.innerHTML = this.classification.bins[i].label;
					element.appendChild(child);

					child = document.createElement("div");
					child.style.clear = "left";
					element.appendChild(child);
				}
			}

			this.layer.legendPanel.update(element.outerHTML);
		},

		CLASS_NAME: "mapfish.GeoStat." + name
	});
};

mapfish.GeoStat.createThematic('Thematic1');
mapfish.GeoStat.createThematic('Thematic2');
mapfish.GeoStat.createThematic('Thematic3');
mapfish.GeoStat.createThematic('Thematic4');
