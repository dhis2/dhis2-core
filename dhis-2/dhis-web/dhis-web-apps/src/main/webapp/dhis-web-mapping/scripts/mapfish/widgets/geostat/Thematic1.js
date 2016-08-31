/*
 * Copyright (C) 2007-2008  Camptocamp|
 *
 * This file is part of MapFish Clien
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
 * @requires core/GeoStat/Thematic1.js
 * @requires core/Color.js
 */

Ext.define('mapfish.widgets.geostat.Thematic1', {
	extend: 'Ext.panel.Panel',
	alias: 'widget.thematic1',

	// Ext panel
	cls: 'gis-form-widget el-border-0',
    border: false,

	// Mapfish
    layer: null,
    format: null,
    url: null,
    indicator: null,
    core: null,
    classificationApplied: false,
    loadMask: false,
    labelGenerator: null,

    cmp: {},

    toggler: {},

    features: [],

    selectHandlers: {},

    store: {
		indicatorsByGroup: Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'legendSet'],
			proxy: {
				type: 'ajax',
				url: '',
				reader: {
					type: 'json',
					root: 'indicators'
				}
			},
			isLoaded: false,
			loadFn: function(fn) {
				if (this.isLoaded) {
					fn.call();
				}
				else {
					this.load(fn);
				}
			},
			listeners: {
				load: function() {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}
					this.sort('name', 'ASC');
				}
			}
		}),

		dataElementsByGroup: Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: '',
				reader: {
					type: 'json',
					root: 'dataElements'
				}
			},
			isLoaded: false,
			loadFn: function(fn) {
				if (this.isLoaded) {
					fn.call();
				}
				else {
					this.load(fn);
				}
			},
			listeners: {
				load: function() {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}
					this.sort('name', 'ASC');
				}
			}
		}),

		periodsByType: Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'index'],
			data: [],
			setIndex: function(periods) {
				for (var i = 0; i < periods.length; i++) {
					periods[i].index = i;
				}
			},
			sortStore: function() {
				this.sort('index', 'ASC');
			}
		}),

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
		}),

		legendsByLegendSet: Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'startValue', 'endValue', 'color'],
			proxy: {
				type: 'ajax',
				url: '',
				reader: {
					type: 'json',
					root: 'mapLegends'
				}
			},
			isLoaded: false,
			loadFn: function(fn) {
				if (this.isLoaded) {
					fn.call();
				}
				else {
					this.load(fn);
				}
			},
			listeners: {
				load: function() {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}
					this.sort('name', 'ASC');
				}
			}
		})
	},

    initComponent: function() {
		this.createUtils();

		this.createItems();

		this.addItems();

		this.createSelectHandlers();

		this.core = this.layer.base.core;
		this.core.widget = this;

		mapfish.widgets.geostat.Thematic1.superclass.initComponent.apply(this);
    },

    createUtils: function() {
		var that = this;

		this.toggler.valueType = function(valueType) {
			if (valueType === 'indicator') {
				that.cmp.indicatorGroup.show();
				that.cmp.indicator.show();
				that.cmp.dataElementGroup.hide();
				that.cmp.dataElement.hide();
			}
			else if (valueType === 'dataElement') {
				that.cmp.indicatorGroup.hide();
				that.cmp.indicator.hide();
				that.cmp.dataElementGroup.show();
				that.cmp.dataElement.show();
			}
		};

		this.toggler.legendType = function(legendType) {
			if (legendType === 'automatic') {
				that.cmp.methodPanel.show();
				that.cmp.lowPanel.show();
				that.cmp.highPanel.show();
				that.cmp.legendSet.hide();
			}
			else if (legendType === 'predefined') {
				that.cmp.methodPanel.hide();
				that.cmp.lowPanel.hide();
				that.cmp.highPanel.hide();
				that.cmp.legendSet.show();
			}
		};
	},

    createItems: function() {

		// Data options

        this.cmp.valueType = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: 'Value type', //i18n
            editable: false,
            valueField: 'id',
            displayField: 'name',
            queryMode: 'local',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            value: GIS.conf.finals.dimension.indicator.id,
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['id', 'name'],
                data: [
                    [GIS.conf.finals.dimension.indicator.id, 'Indicator'], //i18n
                    [GIS.conf.finals.dimension.dataElement.id, 'Data element'] //i18n
                ]
            }),
            listeners: {
                select: {
                    scope: this,
                    fn: function(cb) {
						this.toggler.valueType(cb.getValue());
                    }
                }
            }
        });

        this.cmp.indicatorGroup = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: GIS.app.i18n.indicator_group,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            store: GIS.store.indicatorGroups,
            listeners: {
				added: function() {
					this.store.cmp.push(this);
				},
                select: {
                    scope: this,
                    fn: function(cb) {
                        this.cmp.indicator.clearValue();

                        var store = this.cmp.indicator.store;
                        store.proxy.url = GIS.conf.url.base + GIS.conf.url.path_api +  'indicatorGroups/' + cb.getValue() + '.json?links=false&paging=false';
                        store.load();
                    }
                }
            }
        });

        this.cmp.indicator = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: GIS.app.i18n.indicator,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            queryMode: 'local',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            listConfig: {loadMask: false},
            scope: this,
            store: this.store.indicatorsByGroup,
            listeners: {
                select: function() {
					var that = this.scope;

					Ext.Ajax.request({
						url: GIS.conf.url.base + GIS.conf.url.path_api + 'indicators/' + this.getValue() + '.json?links=false',
						scope: this,
						success: function(r) {
							r = Ext.decode(r.responseText);
							if (Ext.isDefined(r.legendSet) && r.legendSet && r.legendSet.id) {
								that.cmp.legendType.setValue(GIS.conf.finals.widget.legendtype_predefined);
								that.toggler.legendType(GIS.conf.finals.widget.legendtype_predefined);
								if (GIS.store.legendSets.isLoaded) {
									that.cmp.legendSet.setValue(r.legendSet.id);
								}
								else {
									GIS.store.legendSets.loadFn( function() {
										that.cmp.legendSet.setValue(r.legendSet.id);
									});
								}
							}
							else {
								that.cmp.legendType.setValue(GIS.conf.finals.widget.legendtype_automatic);
								that.toggler.legendType(GIS.conf.finals.widget.legendtype_automatic);
							}
						}
					});
                }
            }
        });

        this.cmp.dataElementGroup = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: GIS.app.i18n.dataelement_group,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            hidden: true,
            store: GIS.store.dataElementGroups,
            listeners: {
				added: function() {
					this.store.cmp.push(this);
				},
                select: {
                    scope: this,
                    fn: function(cb) {
                        this.cmp.dataElement.clearValue();

                        var store = this.cmp.dataElement.store;
                        store.proxy.url = GIS.conf.url.base + GIS.conf.url.path_api +  'dataElementGroups/' + cb.getValue() + '.json?links=false&paging=false';
                        store.load();
                    }
                }
            }
        });

        this.cmp.dataElement = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: GIS.app.i18n.dataelement,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            queryMode: 'local',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            listConfig: {loadMask: false},
            hidden: true,
            scope: this,
            store: this.store.dataElementsByGroup,
            listeners: {
                select: function() {
					var that = this.scope;

					Ext.Ajax.request({
						url: GIS.conf.url.base + GIS.conf.url.path_api + 'dataElements/' + this.getValue() + '.json?links=false',
						scope: this,
						success: function(r) {
							r = Ext.decode(r.responseText);
							if (Ext.isDefined(r.legendSet) && r.legendSet && r.legendSet.id) {
								that.cmp.legendType.setValue(GIS.conf.finals.widget.legendtype_predefined);
								that.toggler.legendType(GIS.conf.finals.widget.legendtype_predefined);
								if (GIS.store.legendSets.isLoaded) {
									that.cmp.legendSet.setValue(r.legendSet.id);
								}
								else {
									GIS.store.legendSets.loadFn( function() {
										that.cmp.legendSet.setValue(r.legendSet.id);
									});
								}
							}
							else {
								that.cmp.legendType.setValue(GIS.conf.finals.widget.legendtype_automatic);
								that.toggler.legendType(GIS.conf.finals.widget.legendtype_automatic);
							}
						}
					});
                }
			}
        });

        this.cmp.periodType = Ext.create('Ext.form.field.ComboBox', {
            editable: false,
            valueField: 'id',
            displayField: 'name',
            forceSelection: true,
            queryMode: 'local',
            width: 116,
            store: GIS.store.periodTypes,
			periodOffset: 0,
            listeners: {
                select: {
                    scope: this,
                    fn: function() {
						var pt = new PeriodType(),
							periodType = this.cmp.periodType.getValue();

						var periods = pt.get(periodType).generatePeriods({
							offset: this.cmp.periodType.periodOffset,
							filterFuturePeriods: true,
							reversePeriods: true
						});

						this.store.periodsByType.setIndex(periods);
						this.store.periodsByType.loadData(periods);
                    }
                }
            }
        });

        this.cmp.period = Ext.create('Ext.form.field.ComboBox', {
			fieldLabel: GIS.app.i18n.period,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            queryMode: 'local',
            forceSelection: true,
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            store: this.store.periodsByType
        });

        this.cmp.periodPrev = Ext.create('Ext.button.Button', {
			xtype: 'button',
			text: '<',
			width: 20,
			style: 'margin-left: 3px',
			scope: this,
			handler: function() {
				if (this.cmp.periodType.getValue()) {
					this.cmp.periodType.periodOffset--;
					this.cmp.periodType.fireEvent('select');
				}
			}
		});

        this.cmp.periodNext = Ext.create('Ext.button.Button', {
			xtype: 'button',
			text: '>',
			width: 20,
			style: 'margin-left: 3px',
			scope: this,
			handler: function() {
				if (this.cmp.periodType.getValue() && this.cmp.periodType.periodOffset < 0) {
					this.cmp.periodType.periodOffset++;
					this.cmp.periodType.fireEvent('select');
				}
			}
		});

		// Legend options

        this.cmp.legendType = Ext.create('Ext.form.field.ComboBox', {
            editable: false,
            valueField: 'id',
            displayField: 'name',
            fieldLabel: GIS.app.i18n.legend_type,
            value: GIS.conf.finals.widget.legendtype_automatic,
            queryMode: 'local',
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['id', 'name'],
                data: [
                    [GIS.conf.finals.widget.legendtype_automatic, GIS.app.i18n.automatic],
                    [GIS.conf.finals.widget.legendtype_predefined, GIS.app.i18n.predefined]
                ]
            }),
            listeners: {
                select: {
                    scope: this,
                    fn: function(cb) {
						this.toggler.legendType(cb.getValue());
                    }
                }
            }
        });

        this.cmp.legendSet = Ext.create('Ext.form.field.ComboBox', {
            fieldLabel: GIS.app.i18n.legendset,
            editable: false,
            valueField: 'id',
            displayField: 'name',
            width: GIS.conf.layout.widget.item_width,
            labelWidth: GIS.conf.layout.widget.itemlabel_width,
            hidden: true,
            store: GIS.store.legendSets
        });

        this.cmp.classes = Ext.create('Ext.form.field.Number', {
            editable: false,
            valueField: 'id',
            displayField: 'id',
            queryMode: 'local',
            value: 5,
            minValue: 1,
            maxValue: 7,
            width: 50,
            style: 'margin-right: 3px',
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['id'],
                data: [[1], [2], [3], [4], [5], [6], [7]]
            })
        });

        this.cmp.method = Ext.create('Ext.form.field.ComboBox', {
            editable: false,
            valueField: 'id',
            displayField: 'name',
            queryMode: 'local',
            value: 2,
            width: 109,
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['id', 'name'],
                data: [
                    [2, 'By class range'],
                    [3, 'By class count'] //i18n
                ]
            })
        });

		this.cmp.colorLow = Ext.create('Ext.ux.button.ColorButton', {
			style: 'margin-right: 3px',
			value: 'ff0000',
			scope: this
		});

        this.cmp.colorHigh = Ext.create('Ext.ux.button.ColorButton', {
			style: 'margin-right: 3px',
			value: '00ff00',
			scope: this
		});

        this.cmp.radiusLow = Ext.create('Ext.form.field.Number', {
            width: 50,
            allowDecimals: false,
            minValue: 1,
            value: 5
        });

        this.cmp.radiusHigh = Ext.create('Ext.form.field.Number', {
            width: 50,
            allowDecimals: false,
            minValue: 1,
            value: 15
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
				afterrender: function() {
					this.getSelectionModel().select(0);
				}
			}
        });

        // Custom panels

        this.cmp.periodTypePanel = Ext.create('Ext.panel.Panel', {
			layout: 'hbox',
			items: [
				{
					html: 'Period type:', //i18n
					width: 100,
					bodyStyle: 'color: #444',
					style: 'padding: 3px 0 0 4px'
				},
				this.cmp.periodType,
				this.cmp.periodPrev,
				this.cmp.periodNext
			]
		});

        this.cmp.methodPanel = Ext.create('Ext.panel.Panel', {
			layout: 'hbox',
			items: [
				{
					html: 'Classes / method:', //i18n
					width: 100,
					bodyStyle: 'color: #444',
					style: 'padding: 3px 0 0 4px'
				},
				this.cmp.classes,
				this.cmp.method
			]
		});

        this.cmp.lowPanel = Ext.create('Ext.panel.Panel', {
			layout: 'hbox',
			items: [
				{
					html: 'Low color / size:', //i18n
					width: 100,
					bodyStyle: 'color: #444',
					style: 'padding: 3px 0 0 4px'
				},
				this.cmp.colorLow,
				this.cmp.radiusLow
			]
		});

        this.cmp.highPanel = Ext.create('Ext.panel.Panel', {
			layout: 'hbox',
			items: [
				{
					html: 'High color / size:', //i18n
					width: 100,
					bodyStyle: 'color: #444',
					style: 'padding: 3px 0 0 4px'
				},
				this.cmp.colorHigh,
				this.cmp.radiusHigh
			]
		});
    },

    addItems: function() {

        this.items = [
            {
                xtype: 'form',
				cls: 'el-border-0',
                width: 270,
                items: [
					{
						html: GIS.app.i18n.data_options,
						cls: 'gis-form-subtitle-first'
					},
					this.cmp.valueType,
					this.cmp.indicatorGroup,
					this.cmp.indicator,
					this.cmp.dataElementGroup,
					this.cmp.dataElement,
					this.cmp.periodTypePanel,
					this.cmp.period,
					{
						html: GIS.app.i18n.legend_options,
						cls: 'gis-form-subtitle'
					},
					this.cmp.legendType,
					this.cmp.legendSet,
					this.cmp.methodPanel,
					this.cmp.lowPanel,
					this.cmp.highPanel,
					{
						html: 'Organisation unit level / parent', //i18n
						cls: 'gis-form-subtitle'
					},
					this.cmp.level,
					this.cmp.parent
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
			window.setPosition(window.getPosition()[0], 32);
        };

        onHoverUnselect = function fn(feature) {
			window.destroy();
        };

        onClickSelect = function fn(feature) {
			var showInfo,
				showRelocate,
				drill,
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

			// Drill or float
			drill = function(direction) {
				var store = GIS.store.organisationUnitLevels;

				store.loadFn( function() {
					var store = GIS.store.organisationUnitLevels,
						config;

					if (direction === 'up') {
						var rootNode = GIS.init.rootNodes[0],
							level = store.getAt(store.find('level', that.view.organisationUnitLevel.level - 1));

						config = {
							organisationUnitLevel: {
								id: level.data.id,
								name: level.data.name,
								level: level.data.level
							},
							parentOrganisationUnit: {
								id: rootNode.id,
								name: rootNode.text,
								level: rootNode.level
							},
							parentGraph: '/' + GIS.init.rootNodes[0].id
						};

						that.execute(config);
					}
					else if (direction === 'down') {
						var level = store.getAt(store.find('level', that.view.organisationUnitLevel.level + 1));

						config = {
							organisationUnitLevel: {
								id: level.data.id,
								name: level.data.name,
								level: level.data.level
							},
							parentOrganisationUnit: {
								id: feature.attributes.id,
								name: feature.attributes.name,
								level: that.view.organisationUnitLevel.level
							},
							parentGraph: feature.attributes.path
						};

					//that.config.extended.updateOrganisationUnit = true;
					//that.config.extended.updateGui = true;

						that.execute(config);
					}
				});
			};

			// Menu
			var menuItems = [
				Ext.create('Ext.menu.Item', {
					text: 'Float up',
					iconCls: 'gis-menu-item-icon-float',
					disabled: !that.view.extended.hasCoordinatesUp,
					scope: this,
					handler: function() {
						drill('up');
					}
				}),
				Ext.create('Ext.menu.Item', {
					text: 'Drill down',
					iconCls: 'gis-menu-item-icon-drill',
					cls: 'gis-menu-item-first',
					disabled: !feature.attributes.hcwc,
					scope: this,
					handler: function() {
						drill('down');
					}
				})
			];

			if (isPoint) {
				menuItems.push({
					xtype: 'menuseparator'
				});

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

	reset: function() {

		// Components
		this.cmp.valueType.reset();
		this.toggler.valueType(GIS.conf.finals.dimension.indicator.id);

		this.cmp.indicatorGroup.clearValue();
		this.cmp.indicator.clearValue();
		this.cmp.dataElementGroup.clearValue();
		this.cmp.dataElement.clearValue();

		this.cmp.periodType.clearValue();
		this.cmp.period.clearValue();

		this.cmp.legendType.reset();
		this.toggler.legendType(GIS.conf.finals.widget.legendtype_automatic);
		this.cmp.legendSet.clearValue();
		this.cmp.classes.reset();
		this.cmp.method.reset();
		this.cmp.colorLow.reset();
		this.cmp.colorHigh.reset();
		this.cmp.radiusLow.reset();
		this.cmp.radiusHigh.reset();
		this.cmp.level.clearValue();
		this.cmp.parent.reset();

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

		// Layer and features
		this.features = this.layer.features.slice(0);
		this.store.features.loadFeatures();
		this.layer.item.setValue(false);
	},

	setGui: function(view) {
		var that = this;

		// Value type
		this.cmp.valueType.setValue(view.valueType);

		// Indicator and data element
		this.toggler.valueType(view.valueType);

		var	indeGroupView = view.valueType === GIS.conf.finals.dimension.indicator.id ? this.cmp.indicatorGroup : this.cmp.dataElementGroup,
			indeGroupStore = indeGroupView.store,
			indeGroupValue = view.valueType === GIS.conf.finals.dimension.indicator.id ? view.indicatorGroup.id : view.dataElementGroup.id,

			indeStore = view.valueType === GIS.conf.finals.dimension.indicator.id ? this.store.indicatorsByGroup : this.store.dataElementsByGroup,
			indeView = view.valueType === GIS.conf.finals.dimension.indicator.id ? this.cmp.indicator : this.cmp.dataElement,
			indeValue = view.valueType === GIS.conf.finals.dimension.indicator.id ? view.indicator.id : view.dataElement.id;

		indeGroupStore.loadFn( function() {
			indeGroupView.setValue(indeGroupValue);
		});

		indeStore.proxy.url = GIS.conf.url.base + GIS.conf.url.path_api + view.valueType + 'Groups/' + indeGroupValue + '.json?links=false&paging=false';
		indeStore.loadFn( function() {
			indeView.setValue(indeValue);
		});

		// Period
		this.cmp.periodType.setValue(view.periodType);
		this.cmp.periodType.fireEvent('select');
		this.cmp.period.setValue(view.period.id);

		// Legend
		this.cmp.legendType.setValue(view.legendType);
		this.toggler.legendType(view.legendType);

		if (view.legendType === GIS.conf.finals.widget.legendtype_automatic) {
			this.cmp.classes.setValue(view.classes);
			this.cmp.method.setValue(view.method);
			this.cmp.colorLow.setValue(view.colorLow);
			this.cmp.colorHigh.setValue(view.colorHigh);
			this.cmp.radiusLow.setValue(view.radiusLow);
			this.cmp.radiusHigh.setValue(view.radiusHigh);
		}
		else if (view.legendType === GIS.conf.finals.widget.legendtype_predefined) {
			GIS.store.legendSets.loadFn( function() {
				that.cmp.legendSet.setValue(view.legendSet.id);
			});
		}

		// Level and parent
		GIS.store.organisationUnitLevels.loadFn( function() {
			that.cmp.level.setValue(view.organisationUnitLevel.id);
		});

		this.cmp.parent.selectPath('/root' + view.parentGraph);

		// Layer item
		this.layer.item.setValue(true, view.opacity);

		// Layer menu
		this.menu.enableItems();

		// Update search window
		this.store.features.loadFeatures(this.layer.features);

		// Update filter window
		if (this.cmp.filterWindow && this.cmp.filterWindow.isVisible()) {
			this.cmp.filterWindow.filter();
		}
	},

	getView: function(config) {
		var level = this.cmp.level,
			parent = this.cmp.parent.getSelectionModel().getSelection(),
			store = GIS.store.organisationUnitLevels,
			view;

		parent = parent.length ? parent : [{raw: GIS.init.rootNodes[0]}];

		view = {
			valueType: this.cmp.valueType.getValue(),
			indicatorGroup: {
				id: this.cmp.indicatorGroup.getValue(),
				name: this.cmp.indicatorGroup.getRawValue()
			},
			indicator: {
				id: this.cmp.indicator.getValue(),
				name: this.cmp.indicator.getRawValue()
			},
			dataElementGroup: {
				id: this.cmp.dataElementGroup.getValue(),
				name: this.cmp.dataElementGroup.getRawValue()
			},
			dataElement: {
				id: this.cmp.dataElement.getValue(),
				name: this.cmp.dataElement.getRawValue()
			},
			periodType: this.cmp.periodType.getValue(),
			period: {
				id: this.cmp.period.getValue()
			},
			legendType: this.cmp.legendType.getValue(),
			legendSet: {
				id: this.cmp.legendSet.getValue(),
				name: this.cmp.legendSet.getRawValue()
			},
			classes: parseInt(this.cmp.classes.getValue()),
			method: parseInt(this.cmp.method.getValue()),
			colorLow: this.cmp.colorLow.getValue(),
			colorHigh: this.cmp.colorHigh.getValue(),
			radiusLow: parseInt(this.cmp.radiusLow.getValue()),
			radiusHigh: parseInt(this.cmp.radiusHigh.getValue()),
			organisationUnitLevel: {
				id: level.getValue(),
				name: level.getRawValue(),
				level: store.data.items.length && level.getValue() ? store.getById(level.getValue()).data.level : null
			},
			parentOrganisationUnit: {
				id: parent[0].raw.id,
				name: parent[0].raw.text
			},
			parentLevel: parent[0].raw.level,
			parentGraph: parent[0].raw.path,
			opacity: this.layer.item.getOpacity()
		};

		if (config && Ext.isObject(config)) {
			view = this.extendView(view, config);
		}

		return this.validateView(view);
	},

	extendView: function(view, config) {
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

	validateView: function(view) {
		if (view.valueType === GIS.conf.finals.dimension.indicator.id) {
			if (!view.indicatorGroup.id || !Ext.isString(view.indicatorGroup.id)) {
				GIS.app.logg.push([view.indicatorGroup.id, this.xtype + '.indicatorGroup.id: string']);
				//alert("validation failed"); //todo
				return false;
			}
			if (!view.indicator.id || !Ext.isString(view.indicator.id)) {
				GIS.app.logg.push([view.indicator.id, this.xtype + '.indicator.id: string']);
				alert('No indicator selected'); //todo //i18n
				return false;
			}
		}
		else if (view.valueType === GIS.conf.finals.dimension.dataElement.id) {
			if (!view.dataElementGroup.id || !Ext.isString(view.dataElementGroup.id)) {
				GIS.app.logg.push([view.dataElementGroup.id, this.xtype + '.dataElementGroup.id: string']);
				//alert("validation failed"); //todo
				return false;
			}
			if (!view.dataElement.id || !Ext.isString(view.dataElement.id)) {
				GIS.app.logg.push([view.dataElement.id, this.xtype + '.dataElement.id: string']);
				alert('No data element selected'); //todo //i18n
				return false;
			}
		}

		if (!view.periodType || !Ext.isString(view.periodType)) {
			GIS.app.logg.push([view.periodType, this.xtype + '.periodType: string']);
				//alert("validation failed"); //todo
			return false;
		}
		if (!view.period.id || !Ext.isString(view.period.id)) {
			GIS.app.logg.push([view.period.id, this.xtype + '.period.id: string']);
				alert('No period selected'); //todo //i18n
			return false;
		}

		if (view.legendType === GIS.conf.finals.widget.legendtype_automatic) {
			if (!view.classes || !Ext.isNumber(view.classes)) {
				GIS.app.logg.push([view.classes, this.xtype + '.classes: number']);
				//alert("validation failed"); //todo
				return false;
			}
			if (!view.method || !Ext.isNumber(view.method)) {
				GIS.app.logg.push([view.method, this.xtype + '.method: number']);
				//alert("validation failed"); //todo
				return false;
			}
			if (!view.colorLow || !Ext.isString(view.colorLow)) {
				GIS.app.logg.push([view.colorLow, this.xtype + '.colorLow: string']);
				//alert("validation failed"); //todo
				return false;
			}
			if (!view.radiusLow || !Ext.isNumber(view.radiusLow)) {
				GIS.app.logg.push([view.radiusLow, this.xtype + '.radiusLow: number']);
				//alert("validation failed"); //todo
				return false;
			}
			if (!view.colorHigh || !Ext.isString(view.colorHigh)) {
				GIS.app.logg.push([view.colorHigh, this.xtype + '.colorHigh: string']);
				//alert("validation failed"); //todo
				return false;
			}
			if (!view.radiusHigh || !Ext.isNumber(view.radiusHigh)) {
				GIS.app.logg.push([view.radiusHigh, this.xtype + '.radiusHigh: number']);
				//alert("validation failed"); //todo
				return false;
			}
		}
		else if (view.legendType === GIS.conf.finals.widget.legendtype_predefined) {
			if (!view.legendSet.id || !Ext.isString(view.legendSet.id)) {
				GIS.app.logg.push([view.legendSet.id, this.xtype + '.legendSet.id: string']);
				alert('No legend set selected'); //todo //i18n
				return false;
			}
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

		return view;
	},

    execute: function(config) {
		var view = this.getView(config);

		if (!this.validateView(view)) {
			return;
		}

		this.core.execute(view);
	},

    onRender: function(ct, position) {
        mapfish.widgets.geostat.Thematic1.superclass.onRender.apply(this, arguments);
    }
});
