/*
 * Copyright (C) 2007-2008  Camptocamp|
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
 * @requires core/GeoStat/Facility.js
 * @requires core/Color.js
 */

Ext.define('mapfish.widgets.geostat.Facility', {
	extend: 'Ext.panel.Panel',
	alias: 'widget.facility',

	// Ext panel
	cls: 'gis-form-widget el-border-0',
    border: false,

	// Mapfish
    layer: null,
    format: null,
    url: null,
    indicator: null,
    coreComp: null,
    classificationApplied: false,
    loadMask: false,
    labelGenerator: null,

    // Properties

    config: {
		extended: {}
	},

    tmpView: {},

    view: {},

    cmp: {},

    features: [],

    selectHandlers: {},

    store: {
		infrastructuralDataElementValues: Ext.create('Ext.data.Store', {
			fields: ['dataElementName', 'value'],
			proxy: {
				type: 'ajax',
				url: '../getInfrastructuralDataElementMapValues.action',
				reader: {
					type: 'json',
					root: 'mapValues'
				}
			},
			sortInfo: {field: 'dataElementName', direction: 'ASC'},
			autoLoad: false,
			isLoaded: false,
			listeners: {
				load: function() {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}
				}
			}
		}),

		features: Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			loadFeatures: function(features) {
				if (features && features.length) {
					var data = [];
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
		})
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

        doc = Ext.decode(doc);

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

    getColors: function(low, high) {
        var startColor = new mapfish.ColorRgb();
        startColor.setFromHex(low || this.cmp.colorLow.getValue());
        var endColor = new mapfish.ColorRgb();
        endColor.setFromHex(high || this.cmp.colorHigh.getValue());
        return [startColor, endColor];
    },

    initComponent: function() {
		this.createItems();

		this.addItems();

		this.createSelectHandlers();

		this.coreComp = new mapfish.GeoStat.Facility(this.map, {
            layer: this.layer,
            format: this.format,
            url: this.url,
            requestSuccess: Ext.bind(this.requestSuccess, this),
            requestFailure: Ext.bind(this.requestFailure, this),
            legendDiv: this.legendDiv,
            labelGenerator: this.labelGenerator,
            widget: this
        });

		mapfish.widgets.geostat.Facility.superclass.initComponent.apply(this);
    },

    createItems: function() {

		// Group set

        this.cmp.groupSet = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: GIS.app.i18n.groupset,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            mode: 'remote',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            currentValue: false,
            store: GIS.store.groupSets, //todo
            listeners: {
                select: {
					scope: this,
					fn: function(cb) {
						var store = GIS.store.groupsByGroupSet,
							value = cb.getValue();

						this.config.extended.updateLegend = true;
					}
                }
            }
        });

        // Organisation unit options

        this.cmp.level = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: GIS.app.i18n.level,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            mode: 'remote',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            style: 'margin-bottom: 4px',
            store: GIS.store.organisationUnitLevels,
			listeners: {
				added: function() {
					this.store.cmp.push(this);
				},
				select: {
					scope: this,
					fn: function() {
						this.config.extended.updateOrganisationUnit = true;
					}
				}
			}
        });

        this.cmp.parent = Ext.create('Ext.tree.Panel', {
            autoScroll: true,
            lines: false,
			rootVisible: false,
			multiSelect: false,
			width: GIS.conf.layout.widget.item_width,
			height: 210,
			reset: function() {
				this.collapseAll();
				this.expandPath(GIS.init.rootNodes[0].path);
				this.selectPath(GIS.init.rootNodes[0].path);
			},
			store: Ext.create('Ext.data.TreeStore', {
				proxy: {
					type: 'ajax',
					url: GIS.conf.url.base + GIS.conf.url.path_gis + 'getOrganisationUnitChildren.action'
				},
				root: {
					id: 'root',
					expanded: true,
					children: GIS.init.rootNodes
				},
				listeners: {
					load: function(s, node, r) {
						for (var i = 0; i < r.length; i++) {
							r[i].data.text = GIS.util.json.encodeString(r[i].data.text);
						}
					}
				}
			}),
			listeners: {
				select: {
					scope: this,
					fn: function() {
						this.config.extended.updateOrganisationUnit = true;
					}
				},
				afterrender: function() {
					this.getSelectionModel().select(0);
				}
			}
        });

        this.cmp.areaRadius = Ext.create('Ext.ux.panel.CheckTextNumber', {
			width: 262,
			text: 'Show circular area with radius (km):', //i18n
			listeners: {
				added: function() {
					var that = this;
					this.checkbox.on('change', function() {
						GIS.base.facility.widget.config.extended.updateLegend = true;
					});
					this.numberField.on('change', function() {
						GIS.base.facility.widget.config.extended.updateLegend = true;
					});
				}
			}
		});
    },

    addItems: function() {

        this.items = [
            {
                xtype: 'form',
				cls: 'el-border-0',
                items: [
					{
						html: 'Organisation unit group set', //i18n
						cls: 'gis-form-subtitle-first'
					},
					this.cmp.groupSet,
					{
						html: 'Organisation unit level / parent', //i18n
						cls: 'gis-form-subtitle',
						bodyStyle: 'padding-top: 4px'
					},
					this.cmp.level,
					this.cmp.parent,
					{
						html: 'Surrounding areas', //i18n
						cls: 'gis-form-subtitle',
						bodyStyle: 'padding-top: 4px'
					},
					this.cmp.areaRadius
				]
            }
        ];
    },

    createSelectHandlers: function() {
        var that = this,
			window,
			menu,
			infrastructuralPeriod,
			onHoverSelect,
			onHoverUnselect,
			onClickSelect;

        onHoverSelect = function fn(feature) {
			if (window) {
				window.destroy();
			}
			window = Ext.create('Ext.window.Window', {
				cls: 'gis-window-widget-feature',
				preventHeader: true,
				shadow: false,
				resizable: false,
				items: {
					html: feature.attributes.label
				}
			});

			window.show();

			var x = window.getPosition()[0];
			window.setPosition(x, 32);
        };

        onHoverUnselect = function fn(feature) {
			window.destroy();
        };

        onClickSelect = function fn(feature) {
			var showInfo,
				showRelocate,
				menu,
				isPoint = feature.geometry.CLASS_NAME === GIS.conf.finals.openLayers.point_classname;

			// Relocate
			showRelocate = function() {
				if (that.cmp.relocateWindow) {
					that.cmp.relocateWindow.destroy();
				}

				that.cmp.relocateWindow = Ext.create('Ext.window.Window', {
					title: 'Relocate facility',
					layout: 'fit',
					iconCls: 'gis-window-title-icon-relocate',
					cls: 'gis-container-default',
					setMinWidth: function(minWidth) {
						this.setWidth(this.getWidth() < minWidth ? minWidth : this.getWidth());
					},
					items: {
						html: feature.attributes.name,
						cls: 'gis-container-inner'
					},
					bbar: [
						'->',
						{
							xtype: 'button',
							hideLabel: true,
							text: GIS.app.i18n.cancel,
							handler: function() {
								GIS.map.relocate.active = false;
								that.cmp.relocateWindow.destroy();
								GIS.map.getViewport().style.cursor = 'auto';
							}
						}
					],
					listeners: {
						close: function() {
							GIS.map.relocate.active = false;
							GIS.map.getViewport().style.cursor = 'auto';
						}
					}
				});

				that.cmp.relocateWindow.show();
				that.cmp.relocateWindow.setMinWidth(220);

				GIS.util.gui.window.setPositionTopRight(that.cmp.relocateWindow);
			};

			// Infrastructural data
			showInfo = function() {
				Ext.Ajax.request({
					url: GIS.conf.url.base + GIS.conf.url.path_gis + 'getFacilityInfo.action',
					params: {
						id: feature.attributes.id
					},
					success: function(r) {
						var ou = Ext.decode(r.responseText);

						if (that.cmp.infrastructuralWindow) {
							that.cmp.infrastructuralWindow.destroy();
						}

						that.cmp.infrastructuralWindow = Ext.create('Ext.window.Window', {
							title: 'Facility information', //i18n
							layout: 'column',
							iconCls: 'gis-window-title-icon-information',
							cls: 'gis-container-default',
							width: 460,
							height: 400, //todo
							period: null,
							items: [
								{
									cls: 'gis-container-inner',
									columnWidth: 0.4,
									bodyStyle: 'padding-right:4px',
									items: [
										{
											html: GIS.app.i18n.name,
											cls: 'gis-panel-html-title'
										},
										{
											html: feature.attributes.name,
											cls: 'gis-panel-html'
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											html: GIS.app.i18n.type,
											cls: 'gis-panel-html-title'
										},
										{
											html: ou.ty,
											cls: 'gis-panel-html'
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											html: GIS.app.i18n.code,
											cls: 'gis-panel-html-title'
										},
										{
											html: ou.co,
											cls: 'gis-panel-html'
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											html: GIS.app.i18n.address,
											cls: 'gis-panel-html-title'
										},
										{
											html: ou.ad,
											cls: 'gis-panel-html'
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											html: GIS.app.i18n.contact_person,
											cls: 'gis-panel-html-title'
										},
										{
											html: ou.cp,
											cls: 'gis-panel-html'
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											html: GIS.app.i18n.email,
											cls: 'gis-panel-html-title'
										},
										{
											html: ou.em,
											cls: 'gis-panel-html'
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											html: GIS.app.i18n.phone_number,
											cls: 'gis-panel-html-title'
										},
										{
											html: ou.pn,
											cls: 'gis-panel-html'
										}
									]
								},
								{
									xtype: 'form',
									cls: 'gis-container-inner gis-form-widget',
									columnWidth: 0.6,
									bodyStyle: 'padding-left:4px',
									items: [
										{
											html: GIS.app.i18n.infrastructural_data,
											cls: 'gis-panel-html-title'
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											xtype: 'combo',
											fieldLabel: GIS.app.i18n.period,
											editable: false,
											valueField: 'id',
											displayField: 'name',
											forceSelection: true,
											width: 255, //todo
											labelWidth: 70,
											store: GIS.store.infrastructuralPeriodsByType,
											lockPosition: false,
											listeners: {
												select: function() {
													infrastructuralPeriod = this.getValue();

													that.store.infrastructuralDataElementValues.load({
														params: {
															periodId: infrastructuralPeriod,
															organisationUnitId: feature.attributes.internalId
														}
													});
												}
											}
										},
										{
											cls: 'gis-panel-html-separator'
										},
										{
											xtype: 'grid',
											cls: 'gis-grid',
											height: 300, //todo
											width: 255,
											scroll: 'vertical',
											columns: [
												{
													id: 'dataElementName',
													text: 'Data element',
													dataIndex: 'dataElementName',
													sortable: true,
													width: 195
												},
												{
													id: 'value',
													header: 'Value',
													dataIndex: 'value',
													sortable: true,
													width: 60
												}
											],
											disableSelection: true,
											store: that.store.infrastructuralDataElementValues
										}
									]
								}
							],
							listeners: {
								show: function() {
									if (infrastructuralPeriod) {
										this.down('combo').setValue(infrastructuralPeriod);
										that.store.infrastructuralDataElementValues.load({
											params: {
												periodId: infrastructuralPeriod,
												organisationUnitId: feature.attributes.internalId
											}
										});
									}
								}
							}
						});

						that.cmp.infrastructuralWindow.show();
						GIS.util.gui.window.setPositionTopRight(that.cmp.infrastructuralWindow);
					}
				});
			};

			// Menu
			var menuItems = [];

			if (isPoint) {
				menuItems.push( Ext.create('Ext.menu.Item', {
					text: GIS.app.i18n.relocate,
					iconCls: 'gis-menu-item-icon-relocate',
					disabled: !GIS.init.security.isAdmin,
					handler: function(item) {
						GIS.map.relocate.active = true;
						GIS.map.relocate.widget = that;
						GIS.map.relocate.feature = feature;
						GIS.map.getViewport().style.cursor = 'crosshair';
						showRelocate();
					}
				}));

				menuItems.push( Ext.create('Ext.menu.Item', {
					text: 'Show information', //i18n
					iconCls: 'gis-menu-item-icon-information',
					handler: function(item) {
						if (GIS.store.infrastructuralPeriodsByType.isLoaded) {
							showInfo();
						}
						else {
							GIS.store.infrastructuralPeriodsByType.load({
								params: {
									name: GIS.init.systemSettings.infrastructuralPeriodType
								},
								callback: function() {
									showInfo();
								}
							});
						}
					}
				}));
			}

			menuItems[menuItems.length - 1].addCls('gis-menu-item-last');

			menu = new Ext.menu.Menu({
				shadow: false,
				showSeparator: false,
				defaults: {
					bodyStyle: 'padding-right:6px'
				},
				items: menuItems,
				listeners: {
					afterrender: function() {
						this.getEl().addCls('gis-toolbar-btn-menu');
					}
				}
			});

            menu.showAt([GIS.map.mouseMove.x, GIS.map.mouseMove.y]);
        };

        this.selectHandlers = new OpenLayers.Control.newSelectFeature(this.layer, {
			onHoverSelect: onHoverSelect,
			onHoverUnselect: onHoverUnselect,
			onClickSelect: onClickSelect
		});

        GIS.map.addControl(this.selectHandlers);
        this.selectHandlers.activate();
    },

	getLegendConfig: function() {
		return {
			where: this.tmpView.organisationUnitLevel.name + ' / ' + this.tmpView.parentOrganisationUnit.name
		};
	},

	reset: function() {

		// Components
		this.cmp.groupSet.clearValue();

		this.cmp.level.clearValue();
		this.cmp.parent.reset();

		this.cmp.areaRadius.reset();

		// Layer options
		if (this.cmp.searchWindow) {
			this.cmp.searchWindow.destroy();
		}
		if (this.cmp.filterWindow) {
			this.cmp.filterWindow.destroy();
		}
		if (this.cmp.labelWindow) {
			this.cmp.labelWindow.destroy();
		}

		// View
		this.config = {
			extended: {}
		};
		this.tmpView = {};
		this.view = {};

		// Layer
		this.layer.destroyFeatures();
		this.features = this.layer.features.slice(0);
		this.store.features.loadFeatures();
		this.layer.item.setValue(false);

		if (this.layer.circleLayer) {
			this.layer.circleLayer.deactivateControls();
			this.layer.circleLayer = null;
		}

		// Legend
		document.getElementById(this.legendDiv).innerHTML = '';
	},

	setGui: function() {
		var view = this.tmpView,
			that = this;

		// Group set
		GIS.store.groupSets.load({
			callback: function() {
				that.cmp.groupSet.setValue(view.organisationUnitGroupSet.id);
			}
		});

		// Level and parent
		GIS.store.organisationUnitLevels.loadFn( function() {
			that.cmp.level.setValue(view.organisationUnitLevel.id);
		});

		this.cmp.parent.selectPath('/root' + view.parentGraph);

		if (Ext.isDefined(view.areaRadius)) {
			this.cmp.areaRadius.setValue(true, view.areaRadius);
		}
		else {
			this.cmp.areaRadius.reset();
		}
	},

	getView: function() {
		var level = this.cmp.level,
			parent = this.cmp.parent.getSelectionModel().getSelection(),
			store = GIS.store.organisationUnitLevels,
			view;

		parent = parent.length ? parent : [{raw: GIS.init.rootNodes[0]}];

		view = {
			organisationUnitGroupSet: {
				id: this.cmp.groupSet.getValue(),
				name: this.cmp.groupSet.getRawValue()
			},
			organisationUnitLevel: {
				id: level.getValue(),
				name: level.getRawValue(),
				level: store.data.items.length && level.getValue() ? store.getById(level.getValue()).data.level : null
			},
			parentOrganisationUnit: {
				id: parent[0].raw.id,
				name: parent[0].raw.text
			},
			areaRadius: this.cmp.areaRadius.getValue() ? this.cmp.areaRadius.getNumber() : null,
			parentLevel: parent[0].raw.level,
			parentGraph: parent[0].raw.path,
			opacity: this.layer.item.getOpacity()
		};

		return view;
	},

	extendView: function(view) {
		var conf = this.config;
		view = view || {};

		view.organisationUnitGroupSet = conf.organisationUnitGroupSet || view.organisationUnitGroupSet;
		view.organisationUnitLevel = conf.organisationUnitLevel || view.organisationUnitLevel;
		view.parentOrganisationUnit = conf.parentOrganisationUnit || view.parentOrganisationUnit;
		view.parentLevel = conf.parentLevel || view.parentLevel;
		view.parentGraph = conf.parentGraph || view.parentGraph;
		view.opacity = conf.opacity || view.opacity;

		view.extended = {
			updateOrganisationUnit: Ext.isDefined(conf.extended.updateOrganisationUnit) ? conf.extended.updateOrganisationUnit : false,
			updateData: Ext.isDefined(conf.extended.updateData) ? conf.extended.updateData : false,
			updateLegend: Ext.isDefined(conf.extended.updateLegend) ? conf.extended.updateLegend : false,
			updateGui: Ext.isDefined(conf.extended.updateGui) ? conf.extended.updateGui : false
		};

		return view;
	},

	validateView: function(view) {
		if (!view.organisationUnitGroupSet.id || !Ext.isString(view.organisationUnitGroupSet.id)) {
			GIS.app.logg.push([view.organisationUnitGroupSet.id, this.xtype + '.organisationUnitGroupSet.id: string']);
				alert('No group set selected'); //todo //i18n
			return false;
		}

		if (!view.organisationUnitLevel.id || !Ext.isString(view.organisationUnitLevel.id)) {
			GIS.app.logg.push([view.organisationUnitLevel.id, this.xtype + '.organisationUnitLevel.id: string']);
				alert('No level selected'); //todo
			return false;
		}
		if (!view.organisationUnitLevel.name || !Ext.isString(view.organisationUnitLevel.name)) {
			GIS.app.logg.push([view.organisationUnitLevel.name, this.xtype + '.organisationUnitLevel.name: string']);
				//alert("validation failed"); //todo
			return false;
		}
		if (!view.organisationUnitLevel.level || !Ext.isNumber(view.organisationUnitLevel.level)) {
			GIS.app.logg.push([view.organisationUnitLevel.level, this.xtype + '.organisationUnitLevel.level: number']);
				//alert("validation failed"); //todo
			return false;
		}
		if (!view.parentOrganisationUnit.id || !Ext.isString(view.parentOrganisationUnit.id)) {
			GIS.app.logg.push([view.parentOrganisationUnit.id, this.xtype + '.parentOrganisationUnit.id: string']);
				alert('No parent organisation unit selected'); //todo
			return false;
		}
		if (!view.parentOrganisationUnit.name || !Ext.isString(view.parentOrganisationUnit.name)) {
			GIS.app.logg.push([view.parentOrganisationUnit.name, this.xtype + '.parentOrganisationUnit.name: string']);
				//alert("validation failed"); //todo
			return false;
		}
		if (!view.parentLevel || !Ext.isNumber(view.parentLevel)) {
			GIS.app.logg.push([view.parentLevel, this.xtype + '.parentLevel: number']);
				//alert("validation failed"); //todo
			return false;
		}
		if (!view.parentGraph || !Ext.isString(view.parentGraph)) {
			GIS.app.logg.push([view.parentGraph, this.xtype + '.parentGraph: string']);
				//alert("validation failed"); //todo
			return false;
		}

		if (view.parentOrganisationUnit.level > view.organisationUnitLevel.level) {
			GIS.app.logg.push([view.parentOrganisationUnit.level, view.organisationUnitLevel.level, this.xtype + '.parentOrganisationUnit.level: number <= ' + this.xtype + '.organisationUnitLevel.level']);
				alert('Orgunit level cannot be higher than parent level'); //todo
			return false;
		}

		if (!view.extended.updateOrganisationUnit && !view.extended.updateData && !view.extended.updateLegend) {
			GIS.app.logg.push([view.extended.updateOrganisationUnit, view.extended.updateData, view.extended.updateLegend, this.xtype + '.extended.update ou/data/legend: true||true||true']);
			return false;
		}

		return true;
	},

    loadOrganisationUnits: function() {
		Ext.Ajax.request({
			url: GIS.conf.url.base + GIS.conf.url.path_gis + 'getGeoJsonFacilities.action',
			params: {
				parentId: this.tmpView.parentOrganisationUnit.id,
				level: this.tmpView.organisationUnitLevel.id
			},
			scope: this,
			disableCaching: false,
			success: function(r) {
				var geojson = this.decode(r.responseText),
					format = new OpenLayers.Format.GeoJSON(),
					features = format.read(geojson);

				if (!features.length) {
					alert('No valid coordinates found'); //todo //i18n
					GIS.mask.hide();

					this.config = {
						extended: {}
					};
					return;
				}

				this.loadData(features);
			}
		});
    },

    loadData: function(features) {
		features = features || this.layer.features;

		for (var i = 0; i < features.length; i++) {
			var feature = features[i];
			feature.attributes.label = feature.attributes.name;
		}

		this.layer.removeFeatures(this.layer.features);
		this.layer.addFeatures(features);

		if (this.tmpView.extended.updateOrganisationUnit) {
			this.layer.features = GIS.util.vector.getTransformedFeatureArray(this.layer.features);
		}

		this.features = this.layer.features.slice(0);

		this.loadLegend();
	},

	loadLegend: function() {
		var store = GIS.store.groupsByGroupSet,
			options;

		store.proxy.url = GIS.conf.url.base + GIS.conf.url.path_gis + 'getOrganisationUnitGroupsByGroupSet.action?id=' + this.tmpView.organisationUnitGroupSet.id;
		store.load({
			scope: this,
			callback: function() {
				options = {
					indicator: this.tmpView.organisationUnitGroupSet.name
				};
				this.coreComp.applyClassification(options);
				this.classificationApplied = true;

				this.addCircles();

				this.afterLoad();
			}
		});
	},

	addCircles: function() {
		var radius = this.tmpView.areaRadius;

		if (this.layer.circleLayer) {
			this.layer.circleLayer.deactivateControls();
			this.layer.circleLayer = null;
		}
		if (Ext.isDefined(radius) && radius) {
			this.layer.circleLayer = new GIS.obj.CircleLayer(this.layer.features, radius);
		}
	},

    execute: function(view) {
		if (view) {
			this.config.extended.updateOrganisationUnit = true;
			this.config.extended.updateGui = true;
		}
		else {
			view = this.getView();
		}

		this.tmpView = this.extendView(view);

		if (!this.validateView(this.tmpView)) {
			return;
		}

		GIS.mask.msg = GIS.app.i18n.loading;
		GIS.mask.show();

		if (this.tmpView.extended.updateOrganisationUnit) {
			this.loadOrganisationUnits();
		}
		else {
			this.loadLegend();
		}
	},

	afterLoad: function() {
		if (this.tmpView.extended.updateGui) {
			this.setGui();
		}

		this.view = this.tmpView;
		this.config = {
			extended: {}
		};

		// Layer item
		this.layer.item.setValue(true);

		// Layer menu
		this.menu.enableItems();

		// Update search window
		this.store.features.loadFeatures(this.layer.features);

		// Update filter window
		if (this.cmp.filterWindow && this.cmp.filterWindow.isVisible()) {
			this.cmp.filterWindow.filter();
		}

		// Legend
		GIS.cmp.region.east.doLayout();
		this.layer.legend.expand();

        // Zoom to visible extent if not set by a favorite
        if (GIS.map.mapViewLoader) {
			GIS.map.mapViewLoader.callBack(this);
		}
		else {
			GIS.util.map.zoomToVisibleExtent();
		}

        GIS.mask.hide();
	},

    onRender: function(ct, position) {
        mapfish.widgets.geostat.Facility.superclass.onRender.apply(this, arguments);
    }
});
