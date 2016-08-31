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

	decode: function(doc) {
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

        for (var i = 0; i < doc.geojson.length; i++) {
			attr = doc.geojson[i];

			feature = {
                geometry: {
                    type: parseInt(attr.ty) === 1 ? 'MultiPolygon' : 'Point',
                    coordinates: attr.co
                },
                properties: {
                    id: attr.uid,
                    internalId: attr.iid,
                    name: attr.na
                }
            };
            feature.properties = Ext.Object.merge(feature.properties, attr.groupSets);

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

	getLegendConfig: function() {
		var orgUnit = this.view.parentOrganisationUnit,
			orgUnitLevel = this.view.organisationUnitLevel,
			parent = orgUnit ? orgUnit.name : '',
			level = orgUnitLevel ? orgUnitLevel.name : '',
			where = parent + ' / ' + level;

		return {
			where: where
		};
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
			items = this.gis.store.groupsByGroupSet.data.items,
			config = this.getLegendConfig();

        child.style.height = "14px";
		child.style.overflow = "hidden";
		child.title = config.where;
        child.innerHTML = config.where;
        element.appendChild(child);

        child = document.createElement("div");
        child.style.clear = "left";
        element.appendChild(child);

        child = document.createElement("div");
        child.style.width = "1px";
        child.style.height = "5px";
        element.appendChild(child);

        for (var i = 0; i < items.length; i++) {
            child = document.createElement("div");
            child.style.backgroundImage = 'url(../images/orgunitgroup/' + items[i].data.symbol + ')';
            child.style.backgroundRepeat = 'no-repeat';
            child.style.width = "25px";
            child.style.height = "18px";
            child.style.cssFloat = "left";
            child.style.marginLeft = "3px";
            element.appendChild(child);

            child = document.createElement("div");
            child.innerHTML = items[i].data.name;
            element.appendChild(child);

            child = document.createElement("div");
            child.style.clear = "left";
            element.appendChild(child);
        }

        this.layer.legendPanel.update(element.outerHTML);
    },

    CLASS_NAME: "mapfish.GeoStat.Facility"
});
