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
 * @requires core/GeoStat/Thematic2.js
 * @requires core/Color.js
 */

Ext.define('mapfish.widgets.geostat.Thematic2', {
	extend: 'Ext.panel.Panel',
	alias: 'widget.thematic2',

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

    getColors: function(low, high) {
        var startColor = new mapfish.ColorRgb();
        startColor.setFromHex(low || this.cmp.colorLow.getValue());
        var endColor = new mapfish.ColorRgb();
        endColor.setFromHex(high || this.cmp.colorHigh.getValue());
        return [startColor, endColor];
    },

    initComponent: function() {
		this.createUtils();

		this.createItems();

		this.addItems();

		this.createSelectHandlers();

		this.coreComp = new mapfish.GeoStat.Thematic2(this.map, {
            layer: this.layer,
            format: this.format,
            url: this.url,
            requestSuccess: Ext.bind(this.requestSuccess, this),
            requestFailure: Ext.bind(this.requestFailure, this),
            legendDiv: this.legendDiv,
            labelGenerator: this.labelGenerator,
            widget: this
        });

		mapfish.widgets.geostat.Thematic2.superclass.initComponent.apply(this);
    },

    createUtils: function() {
		var that = this;

		this.toggler.valueType = function(valueType) {
			if (valueType === GIS.conf.finals.dimension.indicator.id) {
				that.cmp.indicatorGroup.show();
				that.cmp.indicator.show();
				that.cmp.dataElementGroup.hide();
				that.cmp.dataElement.hide();
			}
			else if (valueType === GIS.conf.finals.dimension.dataElement.id) {
				that.cmp.indicatorGroup.hide();
				that.cmp.indicator.hide();
				that.cmp.dataElementGroup.show();
				that.cmp.dataElement.show();
			}
		};

		this.toggler.legendType = function(legendType) {
			if (legendType === GIS.conf.finals.widget.legendtype_automatic) {
				that.cmp.methodPanel.show();
				that.cmp.lowPanel.show();
				that.cmp.highPanel.show();
				that.cmp.legendSet.hide();
			}
			else if (legendType === GIS.conf.finals.widget.legendtype_predefined) {
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
						this.config.extended.updateData = true;
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
						this.config.extended.updateData = true;
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
					that.config.extended.updateData = true;

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
					that.config.extended.updateData = true;

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
            store: this.store.periodsByType,
			listeners: {
				select: {
					scope: this,
					fn: function() {
						this.config.extended.updateData = true;
					}
				}
			}
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

						this.config.extended.updateLegend = true;
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
            store: GIS.store.legendSets,
            listeners: {
				select: {
					scope: this,
					fn: function() {
						this.config.extended.updateLegend = true;
					}
				}
			}
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
            }),
            listeners: {
				change: {
					scope: this,
					fn: function() {
						this.config.extended.updateLegend = true;
					}
				}
			}
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
            }),
            listeners: {
				select: {
					scope: this,
					fn: function() {
						this.config.extended.updateLegend = true;
					}
				}
			}
        });

		this.cmp.colorLow = Ext.create('Ext.ux.button.ColorButton', {
			style: 'margin-right: 3px',
			value: 'ff0000',
			scope: this,
			menuHandler: function() {
				this.scope.config.extended.updateLegend = true;
			}
		});

        this.cmp.colorHigh = Ext.create('Ext.ux.button.ColorButton', {
			style: 'margin-right: 3px',
			value: '00ff00',
			scope: this,
			menuHandler: function() {
				this.scope.config.extended.updateLegend = true;
			}
		});

        this.cmp.radiusLow = Ext.create('Ext.form.field.Number', {
            width: 50,
            allowDecimals: false,
            minValue: 1,
            value: 5,
            listeners: {
				change: {
					scope: this,
					fn: function() {
						this.config.extended.updateLegend = true;
					}
				}
			}
        });

        this.cmp.radiusHigh = Ext.create('Ext.form.field.Number', {
            width: 50,
            allowDecimals: false,
            minValue: 1,
            value: 15,
            listeners: {
				change: {
					scope: this,
					fn: function() {
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

			var x = window.getPosition()[0];
			window.setPosition(x, 32);
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
					var store = GIS.store.organisationUnitLevels;

					if (direction === 'up') {
						var rootNode = GIS.init.rootNodes[0],
							level = store.getAt(store.find('level', that.view.organisationUnitLevel.level - 1));

						that.config.organisationUnitLevel = {
							id: level.data.id,
							name: level.data.name,
							level: level.data.level
						};
						that.config.parentOrganisationUnit = {
							id: rootNode.id,
							name: rootNode.text,
							level: rootNode.level
						};
						that.config.parentGraph = '/' + GIS.init.rootNodes[0].id;
					}
					else if (direction === 'down') {
						var level = store.getAt(store.find('level', that.view.organisationUnitLevel.level + 1));

						that.config.organisationUnitLevel = {
							id: level.data.id,
							name: level.data.name,
							level: level.data.level
						};
						that.config.parentOrganisationUnit = {
							id: feature.attributes.id,
							name: feature.attributes.name,
							level: that.view.organisationUnitLevel.level
						};
						that.config.parentGraph = feature.attributes.path;
					}

					that.config.extended.updateOrganisationUnit = true;
					that.config.extended.updateGui = true;

					that.execute();
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

	setPredefinedLegend: function(fn) {
		var store = this.store.legendsByLegendSet,
			colors = [],
			bounds = [],
			names = [],
			legends;

		Ext.Ajax.request({
			url: GIS.conf.url.base + GIS.conf.url.path_api + 'mapLegendSets/' + this.tmpView.legendSet.id + '.json?links=false&paging=false',
			scope: this,
			success: function(r) {
				legends = Ext.decode(r.responseText).mapLegends;

				Ext.Array.sort(legends, function (a, b) {
					return a.startValue - b.startValue;
				});

				for (var i = 0; i < legends.length; i++) {
					if (bounds[bounds.length-1] !== legends[i].startValue) {
						if (bounds.length !== 0) {
							colors.push(new mapfish.ColorRgb(240,240,240));
                            names.push('');
						}
						bounds.push(legends[i].startValue);
					}
					colors.push(new mapfish.ColorRgb());
					colors[colors.length - 1].setFromHex(legends[i].color);
                    names.push(legends[i].name);
					bounds.push(legends[i].endValue);
				}

				this.tmpView.extended.colorInterpolation = colors;
				this.tmpView.extended.bounds = bounds;
                this.tmpView.extended.legendNames = names;

				if (fn) {
					fn.call(this);
				}
			}
		});
	},

	getLegendConfig: function() {
		return {
			what: this.tmpView.valueType === 'indicator' ? this.tmpView.indicator.name : this.tmpView.dataElement.name,
			when: this.tmpView.period.id, //todo name
			where: this.tmpView.organisationUnitLevel.name + ' / ' + this.tmpView.parentOrganisationUnit.name
		};
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

		// Legend
		document.getElementById(this.legendDiv).innerHTML = '';
		this.layer.legend.collapse();
	},

	setGui: function() {
		var view = this.tmpView,
			that = this;

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
	},

	getView: function() {
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

		return view;
	},

	extendView: function(view) {
		var conf = this.config;
		view = view || {};

		view.valueType = conf.valueType || view.valueType;
		view.indicatorGroup = conf.indicatorGroup || view.indicatorGroup;
		view.indicator = conf.indicator || view.indicator;
		view.dataElementGroup = conf.dataElementGroup || view.dataElementGroup;
		view.dataElement = conf.dataElement || view.dataElement;
		view.periodType = conf.periodType || view.periodType;
		view.period = conf.period || view.period;
		view.legendType = conf.legendType || view.legendType;
		view.legendSet = conf.legendSet || view.legendSet;
		view.classes = conf.classes || view.classes;
		view.method = conf.method || view.method;
		view.colorLow = conf.colorLow || view.colorLow;
		view.colorHigh = conf.colorHigh || view.colorHigh;
		view.radiusLow = conf.radiusLow || view.radiusLow;
		view.radiusHigh = conf.radiusHigh || view.radiusHigh;
		view.organisationUnitLevel = conf.organisationUnitLevel || view.organisationUnitLevel;
		view.parentOrganisationUnit = conf.parentOrganisationUnit || view.parentOrganisationUnit;
		view.parentLevel = conf.parentLevel || view.parentLevel;
		view.parentGraph = conf.parentGraph || view.parentGraph;
		view.opacity = conf.opacity || view.opacity;

		view.extended = {
			colors: this.getColors(view.colorLow, view.colorHigh),
			updateOrganisationUnit: Ext.isDefined(conf.extended.updateOrganisationUnit) ? conf.extended.updateOrganisationUnit : false,
			updateData: Ext.isDefined(conf.extended.updateData) ? conf.extended.updateData : false,
			updateLegend: Ext.isDefined(conf.extended.updateLegend) ? conf.extended.updateLegend : false,
			updateGui: Ext.isDefined(conf.extended.updateGui) ? conf.extended.updateGui : false
		};

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

		if (!view.extended.updateOrganisationUnit && !view.extended.updateData && !view.extended.updateLegend) {
			GIS.app.logg.push([view.extended.updateOrganisationUnit, view.extended.updateData, view.extended.updateLegend, this.xtype + '.extended.update ou/data/legend: true||true||true']);
			return false;
		}

		return true;
	},

    loadOrganisationUnits: function() {
		Ext.Ajax.request({
			url: GIS.conf.url.base + GIS.conf.url.path_gis + 'getGeoJson.action',
			params: {
				parentId: this.tmpView.parentOrganisationUnit.id,
				level: this.tmpView.organisationUnitLevel.id
			},
			scope: this,
			disableCaching: false,
			success: function(r) {
				var geojson = GIS.util.geojson.decode(r.responseText, this),
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
		var type = this.tmpView.valueType,
			dataUrl = 'mapValues/' + GIS.conf.finals.dimension[type].param + '.json',
			indicator = GIS.conf.finals.dimension.indicator,
			dataElement = GIS.conf.finals.dimension.dataElement,
			period = GIS.conf.finals.dimension.period,
			organisationUnit = GIS.conf.finals.dimension.organisationUnit,
			params = {};

		features = features || this.layer.features;

		params[type === indicator.id ? indicator.param : dataElement.param] = this.tmpView[type].id;
		params[period.param] = this.tmpView.period.id;
		params[organisationUnit.param] = this.tmpView.parentOrganisationUnit.id;
		params.le = this.tmpView.organisationUnitLevel.id;

		Ext.Ajax.request({
			url: GIS.conf.url.base + GIS.conf.url.path_api + dataUrl,
			params: params,
			disableCaching: false,
			scope: this,
			success: function(r) {
				var values = Ext.decode(r.responseText),
					featureMap = {},
					valueMap = {},
					newFeatures = [];

				if (values.length === 0) {
					alert('No aggregated data values found'); //todo //i18n
					GIS.mask.hide();

					this.config = {
						extended: {}
					};
					return;
				}

				for (var i = 0; i < features.length; i++) {
					var iid = features[i].attributes.internalId;
					featureMap[iid] = true;
				}
				for (var i = 0; i < values.length; i++) {
					var iid = values[i].organisationUnitId,
						value = values[i].value;
					valueMap[iid] = value;
				}

				for (var i = 0; i < features.length; i++) {
					var feature = features[i],
						iid = feature.attributes.internalId;
					if (featureMap.hasOwnProperty(iid) && valueMap.hasOwnProperty(iid)) {
						feature.attributes.value = valueMap[iid];
						feature.attributes.label = feature.attributes.name + ' (' + feature.attributes.value + ')';
						newFeatures.push(feature);
					}
				}

				this.layer.removeFeatures(this.layer.features);
				this.layer.addFeatures(newFeatures);

				if (this.tmpView.extended.updateOrganisationUnit) {
					this.layer.features = GIS.util.vector.getTransformedFeatureArray(this.layer.features);
				}

				this.features = this.layer.features.slice(0);

				this.loadLegend();
			}
		});
	},

	loadLegend: function() {
		var options,
			that = this,

			fn = function() {
				options = {
					indicator: GIS.conf.finals.widget.value,
					method: that.tmpView.method,
					numClasses: that.tmpView.classes,
					colors: that.tmpView.extended.colors,
					minSize: that.tmpView.radiusLow,
					maxSize: that.tmpView.radiusHigh
				};

				that.coreComp.applyClassification(options);
				that.classificationApplied = true;

				that.afterLoad();
			};

		this.tmpView.extended.legendConfig = {
			what: this.tmpView.valueType === 'indicator' ? this.tmpView.indicator.name : this.tmpView.dataElement.name,
			when: this.tmpView.period.id, //todo name
			where: this.tmpView.organisationUnitLevel.name + ' / ' + this.tmpView.parentOrganisationUnit.name
		};

		if (this.tmpView.legendType === GIS.conf.finals.widget.legendtype_predefined) {
			this.setPredefinedLegend(fn);
		}
		else {
			fn();
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
		else if (this.tmpView.extended.updateData) {
			this.loadData();
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
		this.layer.item.setValue(true, this.view.opacity);

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
        mapfish.widgets.geostat.Thematic2.superclass.onRender.apply(this, arguments);
    }
});
