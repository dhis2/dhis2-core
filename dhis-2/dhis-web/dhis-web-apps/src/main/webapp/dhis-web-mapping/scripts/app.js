Ext.onReady( function() {
	var createViewport,
		initialize,
		gis;

	// set app config
	(function() {

		// ext configuration
		Ext.QuickTips.init();

		Ext.override(Ext.LoadMask, {
			onHide: function() {
				this.callParent();
			}
		});

        Ext.override(Ext.grid.Scroller, {
            afterRender: function() {
                var me = this;
                me.callParent();
                me.mon(me.scrollEl, 'scroll', me.onElScroll, me);
                Ext.cache[me.el.id].skipGarbageCollection = true;
                // add another scroll event listener to check, if main listeners is active
                Ext.EventManager.addListener(me.scrollEl, 'scroll', me.onElScrollCheck, me);
                // ensure this listener doesn't get removed
                Ext.cache[me.scrollEl.id].skipGarbageCollection = true;
            },

            // flag to check, if main listeners is active
            wasScrolled: false,

            // synchronize the scroller with the bound gridviews
            onElScroll: function(event, target) {
                this.wasScrolled = true; // change flag -> show that listener is alive
                this.fireEvent('bodyscroll', event, target);
            },

            // executes just after main scroll event listener and check flag state
            onElScrollCheck: function(event, target, options) {
                var me = this;

                if (!me.wasScrolled) {
                    // Achtung! Event listener was disappeared, so we'll add it again
                    me.mon(me.scrollEl, 'scroll', me.onElScroll, me);
                }
                me.wasScrolled = false; // change flag to initial value
            }

        });

        Ext.override(Ext.data.TreeStore, {
            load: function(options) {
                options = options || {};
                options.params = options.params || {};

                var me = this,
                    node = options.node || me.tree.getRootNode(),
                    root;

                // If there is not a node it means the user hasnt defined a rootnode yet. In this case lets just
                // create one for them.
                if (!node) {
                    node = me.setRootNode({
                        expanded: true
                    });
                }

                if (me.clearOnLoad) {
                    node.removeAll(true);
                }

                options.records = [node];

                Ext.applyIf(options, {
                    node: node
                });
                //options.params[me.nodeParam] = node ? node.getId() : 'root';

                if (node) {
                    node.set('loading', true);
                }

                return me.callParent([options]);
            }
        });

		// right click handler
		document.body.oncontextmenu = function() {
			return false;
		};
	}());

	GIS.app = {};

	GIS.app.extendInstance = function(gis) {
		var conf = gis.conf,
			util = gis.util,
			init = gis.init,
			store = gis.store,
			layer;

		// util
		(function() {
			util.map.getFeaturesByLayers = function(layers) {
				var a = [];
				for (var i = 0; i < layers.length; i++) {
					a = a.concat(layers[i].features);
				}
				return a;
			};

			util.map.hasVisibleFeatures = function() {
				var layers = util.map.getVisibleVectorLayers(),
					layer;

				if (layers.length) {
					for (var i = 0; i < layers.length; i++) {
						layer = layers[i];
						if (layer.features.length) {
							return true;
						}
					}
				}

				return false;
			};

			util.map.getLayersByType = function(layerType) {
				var layers = [];
				for (var i = 0; i < gis.olmap.layers.length; i++) {
					var layer = gis.olmap.layers[i];
					if (layer.layerType === layerType) {
						layers.push(layer);
					}
				}
				return layers;
			};

			util.map.addMapControl = function(name, fn) {
				var panel = GIS.app.MapControlPanel(name, fn);
				gis.olmap.addControl(panel);
				panel.div.className += ' ' + name;
				panel.div.childNodes[0].className += ' ' + name + 'Button';
			};

			util.map.getTransformedPointByXY = function(x, y) {
				var p = new OpenLayers.Geometry.Point(parseFloat(x), parseFloat(y));
				return p.transform(new OpenLayers.Projection('EPSG:4326'), new OpenLayers.Projection('EPSG:900913'));
			};

			util.map.getLonLatByXY = function(x, y) {
				var point = util.map.getTransformedPointByXY(x, y);
				return new OpenLayers.LonLat(point.x, point.y);
			};

			util.map.map2plugin = function(map) {
				map.url = init.contextPath;

				if (map.id) {
					return {id: map.id};
				}

				delete map.access;
				delete map.created;
				delete lastUpdated;
				delete name;

				for (var i = 0, dimensions, layout; i < map.mapViews.length; i++) {
					layout = map.mapViews[i];

					dimensions = Ext.Array.clean([].concat(layout.columns || [], layout.rows || [], layout.filters || []));dimension = dimensions[i];

					for (var j = 0, dimension; j < dimensions.length; j++) {
						dimension = dimensions[j];

						delete dimension.id;
						delete dimension.ids;
						delete dimension.type;
						delete dimension.dimensionName;
						delete dimension.objectName;

						for (var k = 0, item; k < dimension.items.length; k++) {
							item = dimension.items[k];

							delete item.name;
							delete item.code;
							delete item.created;
							delete item.lastUpdated;
						}
					}

					if (layout.legendSet) {
						delete layout.method;
						delete layout.classes;
						delete layout.colorLow;
						delete layout.colorHigh;
						delete layout.radiusLow;
						delete layout.radiusHigh;
					}
					else {
						if (layout.method === 2) {
							delete layout.method;
						}

						if (layout.classes === 5) {
							delete layout.classes;
						}

						if (layout.colorLow === "ff0000") {
							delete layout.colorLow;
						}

						if (layout.colorHigh === "00ff00") {
							delete layout.colorHigh;
						}

						if (layout.radiusLow === 5) {
							delete layout.radiusLow;
						}

						if (layout.radiusHigh === 15) {
							delete layout.radiusHigh;
						}
					}

					if (layout.opacity === gis.conf.layout.layer.opacity) {
						delete layout.opacity;
					}

					if (!layout.userOrganisationUnit) {
						delete layout.userOrganisationUnit;
					}

					if (!layout.userOrganisationUnitChildren) {
						delete layout.userOrganisationUnitChildren;
					}

					if (!layout.userOrganisationUnitGrandChildren) {
						delete layout.userOrganisationUnitGrandChildren;
					}

					if (!layout.organisationUnitGroupSet) {
						delete layout.organisationUnitGroupSet;
					}

					delete layout.parentGraphMap;
				}

				return map;
			};

			util.url = util.url || {};

			util.url.getUrlParam = function(s) {
				var output = '';
				var href = window.location.href;
				if (href.indexOf('?') > -1 ) {
					var query = href.substr(href.indexOf('?') + 1);
					var query = query.split('&');
					for (var i = 0; i < query.length; i++) {
						if (query[i].indexOf('=') > -1) {
							var a = query[i].split('=');
							if (a[0].toLowerCase() === s) {
								output = a[1];
								break;
							}
						}
					}
				}
				return unescape(output);
			};

			util.svg = util.svg || {};

			util.svg.merge = function(str, strArray) {
				if (strArray.length) {
					str = str || '<svg></svg>';
					for (var i = 0; i < strArray.length; i++) {
						str = str.replace('</svg>', '');
						strArray[i] = strArray[i].substring(strArray[i].indexOf('>') + 1);
						str += strArray[i];
					}
				}
				return str;
			};

			util.svg.getString = function(title, layers) {
				var svgArray = [],
					svg = '',
					namespace,
					title = Ext.htmlEncode(title),
					titleSVG,
					legendSVG = '',
					scalelineSVG,
					x = 20,
					y = 35,
					center = gis.viewport.centerRegion,
					scalelineEl = Ext.get(Ext.query('.olControlScaleLineTop')[0]);

				if (!layers.length) {
					return false;
				}

				layers = layers.reverse();

				namespace = 'xmlns="http://www.w3.org/2000/svg"';

				svg = '<svg ' + namespace + ' width="' + center.getWidth() + '" height="' + center.getHeight() + '"></svg>';

				titleSVG = '<g id="title" style="display: block; visibility: visible;">' +
						   '<text id="title" x="' + x + '" y="' + y + '" font-size="18" font-weight="bold">' +
						   '<tspan>' + title + '</tspan></text></g>';

				y += 35;

				for (var i = layers.length - 1; i > 0; i--) {
					if (layers[i].id === gis.layer.facility.id) {
						layers.splice(i, 1);
						console.log(GIS.i18n.facility_layer_export_currently_not_supported);
					}
				}

                if (!layers.length) {
					return false;
				}

				for (var i = 0; i < layers.length; i++) {
					var layer = layers[i],
						id = layer.id,
						imageLegendConfig = layer.core.getImageLegendConfig(),
						what,
						when,
						where,
						legend;

					// SVG
					svgArray.push(layer.div.innerHTML);

					// Legend
					if (id !== gis.layer.boundary.id && id !== gis.layer.facility.id && id !== gis.layer.event.id) {
						what = '<g id="indicator" style="display: block; visibility: visible;">' +
							   '<text id="indicator" x="' + x + '" y="' + y + '" font-size="12">' +
							   '<tspan>' + Ext.htmlEncode(layer.core.view.columns[0].items[0].name) + '</tspan></text></g>';

						y += 15;

						when = '<g id="period" style="display: block; visibility: visible;">' +
							   '<text id="period" x="' + x + '" y="' + y + '" font-size="12">' +
							   '<tspan>' + Ext.htmlEncode(layer.core.view.filters[0].items[0].name) + '</tspan></text></g>';

						y += 8;

						legend = '<g>';

						for (var j = 0; j < imageLegendConfig.length; j++) {
							if (j !== 0) {
								y += 15;
							}

							legend += '<rect x="' + x + '" y="' + y + '" height="15" width="30" ' +
									  'fill="' + Ext.htmlEncode(imageLegendConfig[j].color) + '" stroke="#000000" stroke-width="1"/>';

							legend += '<text id="label" x="' + (x + 40) + '" y="' + (y + 12) + '" font-size="12">' +
									  '<tspan>' + Ext.htmlEncode(imageLegendConfig[j].label) + '</tspan></text>';
						}

						legend += '</g>';

						legendSVG += (what + when + where + legend);

						y += 50;
					}
				}

				// Scale line
				scalelineSVG = '<text x="' + (x + 3) + '" y="' + y + '" fill="#000">' + scalelineEl.dom.innerHTML + '</text>';

				y += 3;
				scalelineSVG += '<line x1="' + x + '" y1="' + y + '" x2="' + x + '" y2="' + (y + 3) + '" style="stroke:#000;stroke-width:1" />';
				scalelineSVG += '<line x1="' + (x + scalelineEl.getWidth()) + '" y1="' + y + '" x2="' + (x + scalelineEl.getWidth()) + '" y2="' + (y + 3) + '" style="stroke:#000;stroke-width:1" />';

				y += 3;
				scalelineSVG += '<line x1="' + x + '" y1="' + y + '" x2="' + (x + scalelineEl.getWidth()) + '" y2="' + y + '" style="stroke:#000;stroke-width:1" />';

				// Map
				if (svgArray.length) {
					svg = util.svg.merge(svg, svgArray);
				}

				svg = svg.replace('</svg>', (titleSVG + legendSVG + scalelineSVG) + '</svg>');

				return svg;
			};

			util.json = util.json || {};

			util.json.encodeString = function(str) {
				return Ext.isString(str) ? str.replace(/[^a-zA-Z 0-9(){}<>_!+;:?*&%#-]+/g,'') : str;
			};

			util.json.decodeAggregatedValues = function(responseText) {
				responseText = Ext.decode(responseText);
				var values = [];

				for (var i = 0; i < responseText.length; i++) {
					values.push({
						oi: responseText[i][0],
						v: responseText[i][1]
					});
				}
				return values;
			};

			util.gui = util.gui || {};
			util.gui.window = util.gui.window || {};

			util.gui.window.setPositionTopRight = function(window) {
				window.setPosition(gis.viewport.centerRegion.getWidth() - (window.getWidth() + 3), gis.viewport.centerRegion.y + 64);
			};

			util.gui.window.setPositionTopLeft = function(window) {
				window.setPosition(2,33);
			};

			util.gui.window.addHideOnBlurHandler = function(w) {
				var maskElements = Ext.query('.x-mask'),
                    el = Ext.get(maskElements[maskElements.length - 1]);

				el.on('click', function() {
					if (w.hideOnBlur) {
						w.hide();
					}
				});

				w.hasHideOnBlurHandler = true;
			};

			util.gui.window.addDestroyOnBlurHandler = function(w) {
				var maskElements = Ext.query('.x-mask'),
                    el = Ext.get(maskElements[maskElements.length - 1]);

				el.on('click', function() {
					if (w.destroyOnBlur) {
						w.destroy();
					}
				});

				w.hasDestroyOnBlurHandler = true;
			};

			util.gui.window.setAnchorPosition = function(w, target) {
				var vpw = gis.viewport.getWidth(),
					targetx = target ? target.getPosition()[0] : 4,
					winw = w.getWidth(),
					y = target ? target.getPosition()[1] + target.getHeight() + 4 : 33;

				if ((targetx + winw) > vpw) {
					w.setPosition((vpw - winw - 2), y);
				}
				else {
					w.setPosition(targetx, y);
				}
			};

			util.layout = {};

			util.layout.getAnalytical = function(map) {
				var layout,
					layer;

				if (Ext.isObject(map) && Ext.isArray(map.mapViews) && map.mapViews.length) {
					for (var i = 0, view, id; i < map.mapViews.length; i++) {
						view = map.mapViews[i];
						id = view.layer;

						if (gis.layer.hasOwnProperty(id) && gis.layer[id].layerCategory === gis.conf.finals.layer.category_thematic) {
							layout = gis.api.layout.Layout(view);

							if (layout) {
								return layout;
							}
						}
					}
				}
				else {
					for (var key in gis.layer) {
						if (gis.layer.hasOwnProperty(key) && gis.layer[key].layerCategory === gis.conf.finals.layer.category_thematic && gis.layer[key].core.view) {
							layer = gis.layer[key];
							layout = gis.api.layout.Layout(layer.core.view);

							if (layout) {
								if (!layout.parentGraphMap && layer.widget) {
									layout.parentGraphMap = layer.widget.getParentGraphMap();
								}

								return layout;
							}
						}
					}
				}

				return;
			};

			util.layout.getPluginConfig = function() {
				var layers = gis.util.map.getVisibleVectorLayers(),
					map = {};

				if (gis.map) {
					return gis.map;
				}

				map.mapViews = [];

				for (var i = 0, layer; i < layers.length; i++) {
					layer = layers[i];

					if (layer.core.view) {
						layer.core.view.layer = layer.id;

						map.mapViews.push(layer.core.view);
					}
				}

				return map;
			};

			util.layout.setSessionStorage = function(session, obj, url) {
				if (GIS.isSessionStorage) {
					var dhis2 = JSON.parse(sessionStorage.getItem('dhis2')) || {};
					dhis2[session] = obj;
					sessionStorage.setItem('dhis2', JSON.stringify(dhis2));

					if (Ext.isString(url)) {
						window.location.href = url;
					}
				}
			};
		}());

		// init
		(function() {

			// root nodes
			for (var i = 0; i < init.rootNodes.length; i++) {
				init.rootNodes[i].path = '/root/' + init.rootNodes[i].id;
			}

			// sort organisation unit levels
			util.array.sort(init.organisationUnitLevels, 'ASC', 'level');

			// sort indicator groups
			util.array.sort(init.indicatorGroups);

			// sort data element groups
			util.array.sort(init.dataElementGroups);
		}());

		// store
		(function() {
			store.periodTypes = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				data: gis.conf.period.periodTypes
			});

			store.groupSets = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				proxy: {
					type: 'ajax',
					url: gis.init.contextPath + '/api/organisationUnitGroupSets.json?fields=id,' + gis.init.namePropertyUrl + '&paging=false',
					reader: {
						type: 'json',
						root: 'organisationUnitGroupSets'
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
			});

			store.groupsByGroupSet = Ext.create('Ext.data.Store', {
				fields: ['id', 'name', 'symbol'],
			});

			store.organisationUnitGroup = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				proxy: {
					type: 'ajax',
					url: init.contextPath + '/api/organisationUnitGroups.json?fields=id,' + gis.init.namePropertyUrl + '&paging=false',
					reader: {
						type: 'json',
						root: 'organisationUnitGroups'
					}
				}
			});

			store.legendSets = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				proxy: {
					type: 'ajax',
					url: gis.init.contextPath + '/api/legendSets.json?fields=id,displayName|rename(name)&paging=false',
					reader: {
						type: 'json',
						root: 'legendSets'
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
			});

			store.maps = Ext.create('Ext.data.Store', {
				fields: ['id', 'name', 'access'],
				proxy: {
					type: 'ajax',
					reader: {
						type: 'json',
						root: 'maps'
					}
				},
				isLoaded: false,
				pageSize: 10,
				page: 1,
				defaultUrl: gis.init.contextPath + '/api/maps.json?fields=id,displayName|rename(name),access',
				loadStore: function(url) {
					this.proxy.url = url || this.defaultUrl;

					this.load({
						params: {
							pageSize: this.pageSize,
							page: this.page
						}
					});
				},
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
			});
		}());

		// layer
		(function() {
			layer = gis.layer.event;
			layer.menu = GIS.app.LayerMenu(layer, 'gis-toolbar-btn-menu-first');
			layer.widget = GIS.app.LayerWidgetEvent(layer);
			layer.window = GIS.app.WidgetWindow(layer, gis.conf.layout.widget.window_width + 150, 1);
			layer.window.widget = layer.widget;
			GIS.core.createSelectHandlers(gis, layer);

			layer = gis.layer.facility;
			layer.menu = GIS.app.LayerMenu(layer);
			layer.widget = GIS.app.LayerWidgetFacility(layer);
			layer.window = GIS.app.WidgetWindow(layer);
			layer.window.widget = layer.widget;
			GIS.core.createSelectHandlers(gis, layer);

			layer = gis.layer.thematic1;
			layer.menu = GIS.app.LayerMenu(layer);
			layer.widget = GIS.app.LayerWidgetThematic(layer);
			layer.window = GIS.app.WidgetWindow(layer);
			layer.window.widget = layer.widget;
			GIS.core.createSelectHandlers(gis, layer);

			layer = gis.layer.thematic2;
			layer.menu = GIS.app.LayerMenu(layer);
			layer.widget = GIS.app.LayerWidgetThematic(layer);
			layer.window = GIS.app.WidgetWindow(layer);
			layer.window.widget = layer.widget;
			GIS.core.createSelectHandlers(gis, layer);

			layer = gis.layer.thematic3;
			layer.menu = GIS.app.LayerMenu(layer);
			layer.widget = GIS.app.LayerWidgetThematic(layer);
			layer.window = GIS.app.WidgetWindow(layer);
			GIS.core.createSelectHandlers(gis, layer);

			layer = gis.layer.thematic4;
			layer.menu = GIS.app.LayerMenu(layer);
			layer.widget = GIS.app.LayerWidgetThematic(layer);
			layer.window = GIS.app.WidgetWindow(layer);
			layer.window.widget = layer.widget;
			GIS.core.createSelectHandlers(gis, layer);

			layer = gis.layer.boundary;
			layer.menu = GIS.app.LayerMenu(layer);
			layer.widget = GIS.app.LayerWidgetBoundary(layer);
			layer.window = GIS.app.WidgetWindow(layer);
			layer.window.widget = layer.widget;
			GIS.core.createSelectHandlers(gis, layer);
		}());
	};

	GIS.app.createExtensions = function() {

		Ext.define('Ext.ux.panel.LayerItemPanel', {
			extend: 'Ext.panel.Panel',
			alias: 'widget.layeritempanel',
			layout: 'column',
			layer: null,
			checkbox: null,
			numberField: null,
			imageUrl: null,
			text: null,
			height: 22,
			value: false,
			opacity: gis.conf.layout.layer.opacity,
			getValue: function() {
				return this.checkbox.getValue();
			},
			setValue: function(value, opacity) {
				this.checkbox.setValue(value);
				this.numberField.setDisabled(!value);
				this.layer.setVisibility(value);

				if (value) {
					opacity = Ext.isNumber(parseFloat(opacity)) ? parseFloat(opacity) : this.opacity;

					if (opacity === 0) {
						this.numberField.setValue(0);
						this.setOpacity(0.01);
					}
					else {
						this.numberField.setValue(opacity * 100);
						this.setOpacity(opacity);
					}
				}
			},
			getOpacity: function() {
				return this.opacity;
			},
			setOpacity: function(opacity) {
				this.opacity = opacity === 0 ? 0.01 : opacity;
				this.layer.setLayerOpacity(this.opacity);

				if (this.layer.circleLayer) {
					this.layer.circleLayer.setOpacity(this.opacity);
				}
			},
			disableItem: function() {
				this.checkbox.setValue(false);
				this.numberField.disable();
				this.layer.setVisibility(false);
			},
			enableItem: function() {
				this.checkbox.setValue(true);
				this.numberField.enable();
				this.layer.setVisibility(true);
			},
			updateItem: function(value) {
				this.numberField.setDisabled(!value);
				this.layer.setVisibility(value);

				if (value && this.layer.layerType === gis.conf.finals.layer.type_base) {
					gis.olmap.setBaseLayer(this.layer);
				}

				if (this.layer.circleLayer) {
					this.layer.circleLayer.setVisibility(value);
				}
			},
			initComponent: function() {
				var that = this,
					image;

				this.checkbox = Ext.create('Ext.form.field.Checkbox', {
					width: 14,
					checked: this.value,
					listeners: {
						change: function(chb, value) {
							if (value && that.layer.layerType === gis.conf.finals.layer.type_base) {
								var layers = gis.util.map.getLayersByType(gis.conf.finals.layer.type_base);

								for (var i = 0; i < layers.length; i++) {
									if (layers[i] !== that.layer) {
										layers[i].item.checkbox.suppressChange = true;
										layers[i].item.disableItem();
									}
								}
							}
							that.updateItem(value);

							if (gis.viewport) {
								gis.viewport.downloadButton.xable();
							}
						}
					}
				});

				image = Ext.create('Ext.Img', {
					width: 14,
					height: 14,
					src: this.imageUrl
				});

				this.numberField = Ext.create('Ext.form.field.Number', {
					width: 47,
					height: 18,
					minValue: 0,
					maxValue: 100,
					value: this.opacity * 100,
					allowBlank: false,
					disabled: this.numberFieldDisabled,
					listeners: {
						change: function() {
							var value = this.getValue(),
								opacity = value === 0 ? 0.01 : value/100;

							that.setOpacity(opacity);
						}
					}
				});

				this.items = [
					{
						width: this.checkbox.width + 4,
						items: this.checkbox
					},
					{
						width: image.width + 5,
						items: image,
						bodyStyle: 'padding-top: 4px'
					},
					{
						width: 106,
						html: this.text,
						bodyStyle: 'padding-top: 4px'
					},
					{
						width: this.numberField.width,
						items: this.numberField
					}
				];

				this.layer.setOpacity(this.opacity);

				this.callParent();
			}
		});

		Ext.define('Ext.ux.panel.CheckTextNumber', {
			extend: 'Ext.panel.Panel',
			alias: 'widget.checktextnumber',
			layout: 'column',
            bodyStyle: 'border: 0 none',
			layer: null,
			checkbox: null,
			checkboxBoxLabel: null,
			numberField: null,
            numberFieldWidth: 70,
			height: 24,
			number: 5000,
			value: false,
            components: [],
			getValue: function() {
				return this.checkbox.getValue();
			},
			getNumber: function() {
				return this.numberField.getValue();
			},
			setValue: function(value, number) {
				if (value) {
					this.checkbox.setValue(value);
				}
				if (number) {
					this.numberField.setValue(number);
				}
			},
			enable: function() {
				for (var i = 0; i < this.components.length; i++) {
                    this.components[i].enable();
                }
			},
			disable: function() {
				for (var i = 0; i < this.components.length; i++) {
                    this.components[i].disable();
                }
			},
			reset: function() {
				this.numberField.setValue(this.number);

				this.checkbox.setValue(false);
                this.disable();
			},
			initComponent: function() {
				var ct = this,
                    padding = 2,
                    onAdded = function(cmp) {
                        ct.components.push(cmp);
                    };

                ct.items = [];

				ct.numberField = Ext.create('Ext.form.field.Number', {
                    cls: 'gis-numberfield',
					width: ct.numberFieldWidth,
					height: 21,
					minValue: 0,
					maxValue: 9999999,
					allowBlank: false,
					disabled: true,
					value: ct.number,
                    listeners: {
                        added: onAdded
                    }
				});

				ct.checkbox = Ext.create('Ext.form.field.Checkbox', {
                    cls: 'gis-checkbox',
					width: ct.width - ct.numberField.width,
					boxLabel: ct.checkboxBoxLabel,
					boxLabelCls: 'x-form-cb-label-alt1',
					checked: ct.value,
					disabled: ct.disabled,
                    style: 'padding-left: 3px',
					listeners: {
						change: function(chb, value) {
							if (value) {
								ct.enable();
							}
							else {
								ct.disable();
							}
						}
					}
				});

                ct.items.push(ct.checkbox);
                ct.items.push(ct.numberField);

				this.callParent();
			}
		});

		Ext.define('Ext.ux.panel.LabelPanel', {
			extend: 'Ext.panel.Panel',
			alias: 'widget.labelpanel',
			layout: 'column',
            bodyStyle: 'border: 0 none',
            skipBoldButton: false,
            skipColorButton: false,
            checkboxWidth: 100,
			chechboxBoxLabel: 'Show labels',
            numberFieldValue: 11,
            numberFieldWidth: 50,
            colorButtonWidth: 87,
            colorButtonColor: '000000',
			width: 290,
			height: 24,
            value: false,
            components: [],
			getConfig: function() {
                var config = {
                    labels: this.checkbox.getValue(),
                    labelFontSize: this.numberField.getValue() + 'px',
                    labelFontStyle: this.italicButton.pressed ? 'italic' : 'normal'
                };

                if (!this.skipBoldButton) {
                    config.labelFontWeight = this.boldButton.pressed ? 'bold' : 'normal';
                }

                if (!this.skipColorButton) {
                    config.labelFontColor = '#' + this.colorButton.getValue();
                }

                return config;
			},
			setConfig: function(config) {
                this.numberField.setValue(parseInt(config.labelFontSize));
                this.italicButton.toggle(Ext.Array.contains(['italic', 'oblique'], config.labelFontStyle));

                if (!this.skipBoldButton) {
                    this.boldButton.toggle(Ext.Array.contains(['bold', 'bolder'], config.labelFontWeight) || (Ext.isNumber(parseInt(config.labelFontWeight)) && parseInt(config.labelFontWeight) >= 700));
                }

                if (!this.skipColorButton) {
                    this.colorButton.setValue(config.labelFontColor);
                }

                this.checkbox.setValue(config.labels);
			},
			enable: function() {
				for (var i = 0; i < this.components.length; i++) {
                    this.components[i].enable();
                }
			},
			disable: function() {
				for (var i = 0; i < this.components.length; i++) {
                    this.components[i].disable();
                }
			},
			reset: function() {
                this.numberField.setValue(this.numberFieldValue);
                this.boldButton.toggle(false);
                this.italicButton.toggle(false);
                this.colorButton.setValue(this.colorButtonColor);

                this.checkbox.setValue(false);
                this.disable();
			},
			initComponent: function() {
				var ct = this,
                    onAdded = function(cmp) {
                        ct.components.push(cmp);
                    };

                ct.items = [];

				ct.checkbox = Ext.create('Ext.form.field.Checkbox', {
                    cls: 'gis-checkbox',
					width: ct.checkboxWidth,
					boxLabel: ct.chechboxBoxLabel,
					checked: ct.value,
					disabled: ct.disabled,
					boxLabelCls: 'x-form-cb-label-alt1',
                    style: 'padding-left: 3px',
					listeners: {
						change: function(chb, value) {
							if (value) {
								ct.enable();
							}
							else {
								ct.disable();
							}
						}
					}
				});

                ct.items.push(ct.checkbox);

				ct.numberField = Ext.create('Ext.form.field.Number', {
                    cls: 'gis-numberfield',
					width: ct.numberFieldWidth,
					height: 21,
					minValue: 0,
					maxValue: 9999999,
					allowBlank: false,
					disabled: true,
					value: ct.numberFieldValue,
                    listeners: {
                        added: onAdded
                    }
				});

                ct.items.push(ct.numberField);

                if (!ct.skipBoldButton) {
                    ct.boldButton = Ext.create('Ext.button.Button', {
                        width: 24,
                        height: 24,
                        icon: 'images/text_bold.png',
                        style: 'margin-left: 1px',
                        disabled: true,
                        enableToggle: true,
                        listeners: {
                            added: onAdded
                        }
                    });

                    ct.items.push(ct.boldButton);
                }

                ct.italicButton = Ext.create('Ext.button.Button', {
                    width: 24,
                    height: 24,
                    icon: 'images/text_italic.png',
                    style: 'margin-left: 1px',
					disabled: true,
                    enableToggle: true,
                    listeners: {
                        added: onAdded
                    }
                });

                ct.items.push(ct.italicButton);

                if (!ct.skipColorButton) {
                    ct.colorButton = Ext.create('Ext.ux.button.ColorButton', {
                        width: ct.colorButtonWidth,
                        height: 24,
                        style: 'margin-left: 1px',
                        value: ct.colorButtonColor,
                        listeners: {
                            added: onAdded
                        }
                    });

                    ct.items.push(ct.colorButton);
                }

				this.callParent();
			}
		});

        var operatorCmpWidth = 70,
            valueCmpWidth = 306,
            buttonCmpWidth = 20,
            nameCmpWidth = 400,
            namePadding = '2px 3px',
            margin = '3px 0 1px';

        Ext.define('Ext.ux.panel.DataElementIntegerContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementintegerpanel',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var record = {};

                record.dimension = this.dataElement.id;
                record.name = this.dataElement.name;

                if (this.valueCmp.getValue()) {
					record.filter = this.operatorCmp.getValue() + ':' + this.valueCmp.getValue();
				}

				return record;
            },
            setRecord: function(record) {
				if (record.filter) {
					var a = record.filter.split(':');

					this.operatorCmp.setValue(a[0]);
					this.valueCmp.setValue(a[1]);
				}
			},
            initComponent: function() {
                var container = this;

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    width: nameCmpWidth,
                    style: 'padding:' + namePadding
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    width: operatorCmpWidth,
					style: 'margin-bottom:0',
                    value: 'EQ',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'EQ', name: '='},
                            {id: 'GT', name: '>'},
                            {id: 'GE', name: '>='},
                            {id: 'LT', name: '<'},
                            {id: 'LE', name: '<='},
                            {id: 'NE', name: '!='}
                        ]
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.Number', {
                    width: valueCmpWidth,
					style: 'margin-bottom:0'
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    text: '+',
                    width: buttonCmpWidth,
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    text: 'x',
                    width: buttonCmpWidth,
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.items = [
                    this.nameCmp,
                    this.operatorCmp,
                    this.valueCmp,
                    this.addCmp,
                    this.removeCmp
                ];

                this.callParent();
            }
        });

        Ext.define('Ext.ux.panel.DataElementStringContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementstringpanel',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var record = {};

                record.dimension = this.dataElement.id;
                record.name = this.dataElement.name;

                if (this.valueCmp.getValue()) {
					record.filter = this.operatorCmp.getValue() + ':' + this.valueCmp.getValue();
				}

				return record;
            },
            setRecord: function(record) {
                this.operatorCmp.setValue(record.operator);
                this.valueCmp.setValue(record.filter);
            },
            initComponent: function() {
                var container = this;

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    width: nameCmpWidth,
                    style: 'padding:' + namePadding
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    width: operatorCmpWidth,
					style: 'margin-bottom:0',
                    value: 'LIKE',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'LIKE', name: 'Contains'},
                            {id: 'EQ', name: 'Is exact'}
                        ]
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.Text', {
                    width: valueCmpWidth,
					style: 'margin-bottom:0'
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    text: '+',
                    width: buttonCmpWidth,
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    text: 'x',
                    width: buttonCmpWidth,
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.items = [
                    this.nameCmp,
                    this.operatorCmp,
                    this.valueCmp,
                    this.addCmp,
                    this.removeCmp
                ];

                this.callParent();
            }
        });

        Ext.define('Ext.ux.panel.DataElementDateContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementdatepanel',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var record = {};

                record.dimension = this.dataElement.id;
                record.name = this.dataElement.name;

                if (this.valueCmp.getValue()) {
					record.filter = this.operatorCmp.getValue() + ':' + this.valueCmp.getSubmitValue();
				}

				return record;
            },
            setRecord: function(record) {
				if (record.filter && Ext.isString(record.filter)) {
					var a = record.filter.split(':');

					this.operatorCmp.setValue(a[0]);
					this.valueCmp.setValue(a[1]);
				}
            },
            initComponent: function() {
                var container = this;

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    width: nameCmpWidth,
                    style: 'padding:' + namePadding
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    width: operatorCmpWidth,
                    style: 'margin-bottom:0',
                    value: 'EQ',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'EQ', name: '='},
                            {id: 'GT', name: '>'},
                            {id: 'GE', name: '>='},
                            {id: 'LT', name: '<'},
                            {id: 'LE', name: '<='},
                            {id: 'NE', name: '!='}
                        ]
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.Date', {
					width: valueCmpWidth,
					style: 'margin-bottom:0',
					format: 'Y-m-d'
				});

                this.addCmp = Ext.create('Ext.button.Button', {
                    text: '+',
                    width: buttonCmpWidth,
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    text: 'x',
                    width: buttonCmpWidth,
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.items = [
                    this.nameCmp,
                    this.operatorCmp,
                    this.valueCmp,
                    this.addCmp,
                    this.removeCmp
                ];

                this.callParent();
            }
        });

        Ext.define('Ext.ux.panel.DataElementBooleanContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.dataelementbooleanpanel',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            getRecord: function() {
                var record = {};

                record.dimension = this.dataElement.id;
                record.name = this.dataElement.name;

                if (this.valueCmp.getValue()) {
					record.filter = 'EQ:' + this.valueCmp.getValue();
				}

				return record;
            },
            setRecord: function(record) {
                this.valueCmp.setValue(record.filter);
            },
            initComponent: function() {
                var container = this;

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    width: nameCmpWidth,
                    style: 'padding:' + namePadding
                });

                this.valueCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    width: operatorCmpWidth + valueCmpWidth,
                    style: 'margin-bottom:0',
                    value: 'true',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'true', name: GIS.i18n.yes},
                            {id: 'false', name: GIS.i18n.no}
                        ]
                    }
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    text: '+',
                    width: buttonCmpWidth,
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    text: 'x',
                    width: buttonCmpWidth,
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.items = [
                    this.nameCmp,
                    this.valueCmp,
                    this.addCmp,
                    this.removeCmp
                ];

                this.callParent();
            }
        });

		Ext.define('Ext.ux.panel.OrganisationUnitGroupSetContainer', {
			extend: 'Ext.container.Container',
			alias: 'widget.organisationunitgroupsetpanel',
			layout: 'column',
            bodyStyle: 'border:0 none',
            style: 'margin: ' + margin,
            addCss: function() {
                var css = '.optionselector .x-boundlist-selected { background-color: #fff; border-color: #fff } \n';
                css += '.optionselector .x-boundlist-selected.x-boundlist-item-over { background-color: #ddd; border-color: #ddd } \n';

                Ext.util.CSS.createStyleSheet(css);
            },
            getRecord: function() {
                var items = this.valueCmp.getValue(),
					record = {
                        dimension: this.dataElement.id,
                        name: this.dataElement.name
                    };

                // array or object
                for (var i = 0; i < items.length; i++) {
                    if (Ext.isObject(items[i])) {
                        items[i] = items[i].code;
                    }
                }

                if (items.length) {
                    record.filter = 'IN:' + items.join(';');
                }

                return record;
            },
            setRecord: function(record) {
				if (Ext.isString(record.filter) && record.filter.length) {
					var a = record.filter.split(':');
					this.valueCmp.setOptionValues(a[1].split(';'));
				}
            },
            getRecordsByCode: function(options, codeArray) {
                var records = [];

                for (var i = 0; i < options.length; i++) {
                    for (var j = 0; j < codeArray.length; j++) {
                        if (options[i].code === codeArray[j]) {
                            records.push(options[i]);
                        }
                    }
                }

                return records;
            },
            initComponent: function() {
                var container = this,
                    idProperty = 'code',
                    nameProperty = 'name';

                this.addCss();

                this.nameCmp = Ext.create('Ext.form.Label', {
                    text: this.dataElement.name,
                    width: nameCmpWidth,
                    style: 'padding:' + namePadding
                });

                this.operatorCmp = Ext.create('Ext.form.field.ComboBox', {
                    valueField: 'id',
                    displayField: 'name',
                    queryMode: 'local',
                    editable: false,
                    style: 'margin-bottom:0',
                    width: operatorCmpWidth,
                    value: 'IN',
                    store: {
                        fields: ['id', 'name'],
                        data: [
                            {id: 'IN', name: 'One of'}
                        ]
                    }
                });

                this.searchStore = Ext.create('Ext.data.Store', {
					fields: [idProperty, nameProperty],
					data: [],
					loadOptionSet: function(optionSetId, key, pageSize) {
						var store = this;

                        optionSetId = optionSetId || container.dataElement.optionSet.id;
                        pageSize = pageSize || 100;

                        dhis2.gis.store.get('optionSets', optionSetId).done( function(obj) {
                            if (Ext.isObject(obj) && Ext.isArray(obj.options) && obj.options.length) {
                                store.removeAll();
                                store.loadData(obj.options.slice(0, pageSize));
                            }
                        });
					},
                    listeners: {
						datachanged: function(s) {
							if (container.searchCmp && s.getRange().length) {
								container.searchCmp.expand();
							}
						}
					}
				});

                // function
                this.filterSearchStore = function() {
                    var selected = container.valueCmp.getValue();

                    container.searchStore.clearFilter();

                    container.searchStore.filterBy(function(record) {
                        return !Ext.Array.contains(selected, record.data[idProperty]);
                    });
                };

                this.searchCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    width: 62,
                    style: 'margin-bottom:0',
                    emptyText: 'Search..',
                    valueField: idProperty,
                    displayField: nameProperty,
                    hideTrigger: true,
                    delimiter: '; ',
                    enableKeyEvents: true,
                    queryMode: 'local',
                    listConfig: {
                        minWidth: 304
                    },
                    store: this.searchStore,
                    listeners: {
						keyup: {
							fn: function() {
								var value = this.getValue(),
									optionSetId = container.dataElement.optionSet.id;

								// search
								container.searchStore.loadOptionSet(optionSetId, value);

                                // trigger
                                if (!value || (Ext.isString(value) && value.length === 1)) {
									container.triggerCmp.setDisabled(!!value);
								}
							}
						},
						select: function() {
                            var id = Ext.Array.from(this.getValue())[0];

                            // value
                            if (container.valueStore.findExact(idProperty, id) === -1) {
                                container.valueStore.add(container.searchStore.getAt(container.searchStore.findExact(idProperty, id)).data);
                            }

                            // search
                            this.select([]);

                            // filter
                            container.filterSearchStore();

                            // trigger
                            container.triggerCmp.enable();
						},
                        expand: function() {
                            container.filterSearchStore();
                        }
					}
                });

                this.triggerCmp = Ext.create('Ext.button.Button', {
                    cls: 'gis-button-combotrigger',
                    disabledCls: 'gis-button-combotrigger-disabled',
                    width: 18,
                    height: 22,
                    handler: function(b) {
                        container.searchStore.loadOptionSet();
                    }
                });

                this.valueStore = Ext.create('Ext.data.Store', {
					fields: [idProperty, nameProperty],
                    listeners: {
                        add: function() {
                            container.valueCmp.select(this.getRange());
                        },
                        remove: function() {
                            container.valueCmp.select(this.getRange());
                        }
                    }
                });

                this.valueCmp = Ext.create('Ext.form.field.ComboBox', {
                    multiSelect: true,
                    style: 'margin-bottom:0',
					width: 226,
                    valueField: idProperty,
                    displayField: nameProperty,
                    emptyText: 'No selected items',
                    editable: false,
                    hideTrigger: true,
                    store: container.valueStore,
                    queryMode: 'local',
                    listConfig: {
                        cls: 'optionselector'
                    },
                    setOptionValues: function(codeArray) {
                        var me = this,
                            records = [];

                        dhis2.gis.store.get('optionSets', container.dataElement.optionSet.id).done( function(obj) {
                            if (Ext.isObject(obj) && Ext.isArray(obj.options) && obj.options.length) {
                                records = container.getRecordsByCode(obj.options, codeArray);

                                container.valueStore.removeAll();
                                container.valueStore.loadData(records);

                                me.setValue(records);
                            }
                        });
                    },
					listeners: {
                        change: function(cmp, newVal, oldVal) {
                            newVal = Ext.Array.from(newVal);
                            oldVal = Ext.Array.from(oldVal);

                            if (newVal.length < oldVal.length) {
                                var id = Ext.Array.difference(oldVal, newVal)[0];
                                container.valueStore.removeAt(container.valueStore.findExact(idProperty, id));
                            }
                        }
                    }
                });

                this.addCmp = Ext.create('Ext.button.Button', {
                    text: '+',
                    width: buttonCmpWidth,
                    style: 'font-weight:bold',
                    handler: function() {
						container.duplicateDataElement();
					}
                });

                this.removeCmp = Ext.create('Ext.button.Button', {
                    text: 'x',
                    width: buttonCmpWidth,
                    handler: function() {
                        container.removeDataElement();
                    }
                });

                this.items = [
                    this.nameCmp,
                    this.operatorCmp,
                    this.searchCmp,
                    this.triggerCmp,
                    this.valueCmp,
                    this.addCmp,
                    this.removeCmp
                ];

                this.callParent();
            }
        });

    };

    // Objects

    GIS.app.LayerMenu = function(layer, cls) {
		var items = [],
			item;

		item = {
			text: GIS.i18n.edit_layer,
			iconCls: 'gis-menu-item-icon-edit',
			cls: 'gis-menu-item-first',
			alwaysEnabled: true,
			handler: function() {
				layer.window.show();
			}
		};
		items.push(item);

		items.push({
			xtype: 'menuseparator',
			alwaysEnabled: true
		});

		if (!(layer.id === gis.layer.boundary.id || layer.id === gis.layer.facility.id || layer.id === gis.layer.event.id)) {
			item = {
				text: GIS.i18n.filter + '..',
				iconCls: 'gis-menu-item-icon-filter',
				handler: function() {
					if (layer.filterWindow) {
						if (layer.filterWindow.isVisible()) {
							return;
						}
						else {
							layer.filterWindow.destroy();
						}
					}

					layer.filterWindow = layer.id === gis.layer.facility.id ?
						GIS.app.FilterWindowFacility(layer) : GIS.app.FilterWindow(layer);
					layer.filterWindow.show();
				}
			};
			items.push(item);
		}

        if (!(layer.id === gis.layer.event.id)) {
            item = {
                text: GIS.i18n.search,
                iconCls: 'gis-menu-item-icon-search',
                handler: function() {
                    if (layer.searchWindow) {
                        if (layer.searchWindow.isVisible()) {
                            return;
                        }
                        else {
                            layer.searchWindow.destroy();
                        }
                    }

                    layer.searchWindow = GIS.app.SearchWindow(layer);
                    layer.searchWindow.show();
                }
            };
            items.push(item);
        }

        if (items[items.length - 1].xtype !== 'menuseparator') {
            items.push({
                xtype: 'menuseparator',
                alwaysEnabled: true
            });
        }

		item = {
			text: GIS.i18n.clear,
			iconCls: 'gis-menu-item-icon-clear',
			handler: function() {
				layer.core.reset();
			}
		};
		items.push(item);

		return Ext.create('Ext.menu.Menu', {
			shadow: false,
			showSeparator: false,
			enableItems: function() {
				Ext.each(this.items.items, function(item) {
					item.enable();
				});
			},
			disableItems: function() {
				Ext.Array.each(this.items.items, function(item) {
					if (!item.alwaysEnabled) {
						item.disable();
					}
				});
			},
			items: items,
			listeners: {
				afterrender: function() {
					this.getEl().addCls('gis-toolbar-btn-menu');
					if (cls) {
						this.getEl().addCls(cls);
					}
				},
				show: function() {
					if (layer.features.length) {
						this.enableItems();
					}
					else {
						this.disableItems();
					}

					this.doLayout(); // show menu bug workaround
				}
			}
		});
	};

	GIS.app.LayersPanel = function() {
		var layers = gis.layer,
			layer,
			items = [],
			item,
			panel,
			visibleLayer = function() {
                return window.google ? layers.googleStreets : layers.openStreetMap;
            }(),
			orderedLayers = gis.olmap.layers.reverse(),
            layerIsVisibleLayer;

        // gm first
        for (var i = 0; i < 2; i++) {
            if (Ext.Array.contains(['googleStreets', 'googleHybrid'], orderedLayers[0].id)) {
                orderedLayers.push(orderedLayers.shift());
            }
        }

		for (var i = 0, layerIsVisibleLayer; i < orderedLayers.length; i++) {
			layer = orderedLayers[i];
            layerIsVisibleLayer = Ext.isObject(visibleLayer) && layer.id === visibleLayer.id;

			item = Ext.create('Ext.ux.panel.LayerItemPanel', {
				cls: 'gis-container-inner',
				height: 23,
				layer: layer,
				text: layer.name,
				imageUrl: 'images/' + layer.id + '_14.png',
				value: layerIsVisibleLayer && window.google ? true : false,
				opacity: layer.layerOpacity,
				defaultOpacity: layer.layerOpacity,
				numberFieldDisabled: !layerIsVisibleLayer
			});

			layer.item = item;
			items.push(layer.item);
		}

        if (visibleLayer) {
            visibleLayer.item.setValue(!!window.google);
        }

        panel = Ext.create('Ext.panel.Panel', {
			renderTo: 'layerItems',
			layout: 'fit',
			cls: 'gis-container-inner',
			layerItems: items,
			items: {
				cls: 'gis-container-inner',
				items: items
			}
		});

		return panel;
	};

	GIS.app.WidgetWindow = function(layer, width, padding) {
		width = width || gis.conf.layout.widget.window_width;
		padding = padding || 0;

		return Ext.create('Ext.window.Window', {
			//autoShow: true,
			title: layer.name,
			layout: 'fit',
			iconCls: 'gis-window-title-icon-' + layer.id,
            bodyStyle: 'padding:' + padding + 'px',
			cls: 'gis-container-default',
			closeAction: 'hide',
			width: width,
			resizable: false,
			isRendered: false,
			items: layer.widget,
			bbar: [
				'->',
				{
					text: GIS.i18n.update,
					handler: function() {
						var view = layer.widget.getView();

						if (view) {
							var loader = layer.core.getLoader();
							loader.compare = (layer.id !== gis.layer.facility.id),
							loader.zoomToVisibleExtent = true;
							loader.hideMask = true;
							loader.load(view);
						}
					}
				}
			],
			listeners: {
				show: function(w) {
					if (!this.isRendered) {
						this.isRendered = true;

						if (layer.core.view) {
							this.widget.setGui(layer.core.view);
						}
					}

					gis.util.gui.window.setPositionTopLeft(this);
				}
			}
		});
	};

	GIS.app.SearchWindow = function(layer) {
		var data = [],
			store = layer.core.featureStore,
			button,
			window;

		for (var i = 0; i < layer.features.length; i++) {
			data.push([layer.features[i].data.id, layer.features[i].data.name]);
		}

		if (!data.length) {
			GIS.logg.push([data, layer.id + '.search.data: feature ids/names']);
			alert(GIS.i18n.layer + ' ' + GIS.i18n.has_no_orgunits);
			return;
		}

		button = Ext.create('Ext.ux.button.ColorButton', {
			width: gis.conf.layout.tool.item_width - gis.conf.layout.tool.itemlabel_width,
            height: 24,
			value: '0000ff'
		});

		window = Ext.create('Ext.window.Window', {
			title: GIS.i18n.organisationunit_search,
			iconCls: 'gis-window-title-icon-search',
            bodyStyle: 'background-color: #fff; padding: 1px',
			resizable: false,
			height: 380,
			items: [
                {
                    layout: 'column',
                    cls: 'gis-container-inner',
                    items: [
                        {
                            cls: 'gis-panel-html-label',
                            html: GIS.i18n.highlight_color + ':',
                            width: gis.conf.layout.tool.itemlabel_width
                        },
                        button
                    ]
                },
                {
                    xtype: 'container',
                    height: 1
                },
                {
                    layout: 'column',
                    cls: 'gis-container-inner',
                    items: [
                        {
                            cls: 'gis-panel-html-label',
                            html: GIS.i18n.text_filter + ':',
                            width: gis.conf.layout.tool.itemlabel_width
                        },
                        {
                            xtype: 'textfield',
                            cls: 'gis-textfield',
                            width: gis.conf.layout.tool.item_width - gis.conf.layout.tool.itemlabel_width,
                            enableKeyEvents: true,
                            listeners: {
                                keyup: function() {
                                    store.clearFilter();
                                    if (this.getValue()) {
                                        store.filter('name', this.getValue());
                                    }
                                    store.sortStore();
                                }
                            }
                        }
                    ]
                },
                {
                    xtype: 'grid',
                    cls: 'gis-grid',
                    bodyStyle: 'border: 0 none',
                    height: 290,
                    width: gis.conf.layout.tool.item_width,
                    scroll: 'vertical',
                    hideHeaders: true,
                    columns: [{
                        id: 'name',
                        text: 'Organisation units',
                        dataIndex: 'name',
                        sortable: false,
                        width: gis.conf.layout.tool.item_width
                    }],
                    store: layer.core.featureStore,
                    listeners: {
                        select: function(grid, record) {
                            var feature = layer.getFeaturesByAttribute('id', record.data.id)[0],
                                color = button.getValue(),
                                symbolizer;

                            layer.redraw();

                            if (feature.geometry.CLASS_NAME === gis.conf.finals.openLayers.point_classname) {
                                symbolizer = new OpenLayers.Symbolizer.Point({
                                    pointRadius: 6,
                                    fillColor: '#' + color,
                                    strokeWidth: 1
                                });
                            }
                            else {
                                symbolizer = new OpenLayers.Symbolizer.Polygon({
                                    strokeColor: '#' + color,
                                    fillColor: '#' + color
                                });
                            }

                            layer.drawFeature(feature, symbolizer);
                        }
                    }
                }
			],
			listeners: {
				render: function() {
					gis.util.gui.window.setPositionTopLeft(this);
					store.sortStore();
				},
				destroy: function() {
					layer.redraw();
				}
			}
		});

		return window;
	};

	GIS.app.FilterWindow = function(layer) {
		var lowerNumberField,
			greaterNumberField,
			lt,
			gt,
			filter,
			window;

		greaterNumberField = Ext.create('Ext.form.field.Number', {
            cls: 'gis-numberfield',
            fieldLabel: 'Greater than',
            width: 200,
			value: parseInt(layer.core.minVal),
			listeners: {
				change: function() {
					gt = this.getValue();
				}
			}
		});

		lowerNumberField = Ext.create('Ext.form.field.Number', {
            cls: 'gis-numberfield',
            fieldLabel: 'And/or lower than',
            style: 'margin-bottom: 0',
            width: 200,
			value: parseInt(layer.core.maxVal) + 1,
			listeners: {
				change: function() {
					lt = this.getValue();
				}
			}
		});

        filter = function() {
			var cache = layer.core.featureStore.features.slice(0),
				features = [];

            if (!gt && !lt) {
                features = cache;
            }
            else if (gt && lt) {
                for (var i = 0; i < cache.length; i++) {
                    if (gt < lt && (cache[i].attributes.value > gt && cache[i].attributes.value < lt)) {
                        features.push(cache[i]);
                    }
                    else if (gt > lt && (cache[i].attributes.value > gt || cache[i].attributes.value < lt)) {
                        features.push(cache[i]);
                    }
                    else if (gt === lt && cache[i].attributes.value === gt) {
                        features.push(cache[i]);
                    }
                }
            }
            else if (gt && !lt) {
                for (var i = 0; i < cache.length; i++) {
                    if (cache[i].attributes.value > gt) {
                        features.push(cache[i]);
                    }
                }
            }
            else if (!gt && lt) {
                for (var i = 0; i < cache.length; i++) {
                    if (cache[i].attributes.value < lt) {
                        features.push(cache[i]);
                    }
                }
            }

            layer.removeAllFeatures();
            layer.addFeatures(features);
        };

		window = Ext.create('Ext.window.Window', {
			title: 'Filter by value',
			iconCls: 'gis-window-title-icon-filter',
            bodyStyle: 'background-color: #fff; padding: 1px',
			resizable: false,
			filter: filter,
			items: [
                {
                    xtype: 'container',
                    style: 'padding: 4px; border: 0 none',
                    html: '<b>Show</b> organisation units with values..'
                },
                {
                    xtype: 'container',
                    height: 7
                },
                greaterNumberField,
                lowerNumberField
                //{
                    //layout: 'column',
                    //height: 22,
                    //cls: 'gis-container-inner',
                    //items: [
                        //{
                            //cls: 'gis-panel-html-label',
                            //html: 'Greater than:',
                            //width: gis.conf.layout.tool.item_width - gis.conf.layout.tool.itemlabel_width
                        //},
                        //greaterNumberField
                    //]
                //},
					//{
						//cls: 'gis-panel-html-separator'
					//},
					//{
						//layout: 'column',
						//height: 22,
						//cls: 'gis-container-inner',
						//items: [
							//{
								//cls: 'gis-panel-html-label',
								//html: 'And/or lower than:',
								//width: gis.conf.layout.tool.item_width - gis.conf.layout.tool.itemlabel_width
							//},
							//lowerNumberField
						//]
					//}
				//]
			],
			bbar: [
				'->',
				{
					xtype: 'button',
					text: GIS.i18n.update,
					handler: function() {
						filter();
					}
				}
			],
			listeners: {
				render: function() {
					gis.util.gui.window.setPositionTopLeft(this);
				},
				destroy: function() {
					layer.removeAllFeatures();
					layer.addFeatures(layer.core.featureStore.features);
				}
			}
		});

		return window;
	};

	GIS.app.FilterWindowFacility = function(layer) {
		var window,
			multiSelect,
			button,
			filter,
			selection,
			features = [],
			coreFeatures = layer.core.featureStore.features.slice(0),
			groupSetName = layer.core.view.organisationUnitGroupSet.name,
			store = gis.store.groupsByGroupSet;

		filter = function() {
			features = [];

			if (!selection.length || !selection[0]) {
				features = coreFeatures;
			}
			else {
				for (var i = 0; i < coreFeatures.length; i++) {
					for (var j = 0; j < selection.length; j++) {
						if (coreFeatures[i].attributes[groupSetName] === selection[j]) {
							features.push(coreFeatures[i]);
						}
					}
				}
			}

			layer.removeAllFeatures();
			layer.addFeatures(features);
		};

		multiSelect = Ext.create('Ext.ux.form.MultiSelect', {
			hideLabel: true,
			dataFields: ['id', 'name'],
			valueField: 'name',
			displayField: 'name',
			width: 200,
			height: 300,
			store: store
		});

		button = Ext.create('Ext.button.Button', {
			text: 'Filter',
			handler: function() {
				selection = multiSelect.getValue();
				filter();
			}
		});

		window = Ext.create('Ext.window.Window', {
			title: 'Filter by value',
			iconCls: 'gis-window-title-icon-filter',
			cls: 'gis-container-default',
			resizable: false,
			filter: filter,
			items: multiSelect,
			bbar: [
				'->',
				button
			],
			listeners: {
				render: function() {
					gis.util.gui.window.setPositionTopLeft(this);
				},
				destroy: function() {
					layer.removeAllFeatures();
					layer.addFeatures(coreFeatures);
				}
			}
		});

		return window;
	};

	GIS.app.SharingWindow = function(sharing) {

		// Objects
		var UserGroupRow,

		// Functions
			getBody,

		// Components
			userGroupStore,
			userGroupField,
			userGroupButton,
			userGroupRowContainer,
			externalAccess,
			publicGroup,
			window;

		UserGroupRow = function(obj, isPublicAccess, disallowPublicAccess) {
			var getData,
				store,
				getItems,
				combo,
				getAccess,
				panel;

			getData = function() {
				var data = [
					{id: 'r-------', name: GIS.i18n.can_view},
					{id: 'rw------', name: GIS.i18n.can_edit_and_view}
				];

				if (isPublicAccess) {
					data.unshift({id: '--------', name: GIS.i18n.none});
				}

				return data;
			}

			store = Ext.create('Ext.data.Store', {
				fields: ['id', 'name'],
				data: getData()
			});

			getItems = function() {
				var items = [];

				combo = Ext.create('Ext.form.field.ComboBox', {
                    style: 'margin-bottom:2px',
					fieldLabel: isPublicAccess ? GIS.i18n.public_access : obj.name,
					labelStyle: 'color:#333',
					cls: 'gis-combo',
					width: 380,
					labelWidth: 250,
					queryMode: 'local',
					valueField: 'id',
					displayField: 'name',
					labelSeparator: null,
					editable: false,
					disabled: !!disallowPublicAccess,
					value: obj.access || 'rw------',
					store: store
				});

				items.push(combo);

				if (!isPublicAccess) {
					items.push(Ext.create('Ext.Img', {
						src: 'images/grid-delete_16.png',
						style: 'margin-top:2px; margin-left:7px',
						overCls: 'pointer',
						width: 16,
						height: 16,
						listeners: {
							render: function(i) {
								i.getEl().on('click', function(e) {
									i.up('panel').destroy();
									window.doLayout();
								});
							}
						}
					}));
				}

				return items;
			};

			getAccess = function() {
				return {
					id: obj.id,
					name: obj.name,
					access: combo.getValue()
				};
			};

			panel = Ext.create('Ext.panel.Panel', {
				layout: 'column',
				bodyStyle: 'border:0 none',
				getAccess: getAccess,
				items: getItems()
			});

			return panel;
		};

		getBody = function() {
			var body = {
				object: {
					id: sharing.object.id,
					name: sharing.object.name,
					publicAccess: publicGroup.down('combobox').getValue(),
					externalAccess: externalAccess ? externalAccess.getValue() : false,
					user: {
						id: gis.init.user.id,
						name: gis.init.user.name
					}
				}
			};

			if (userGroupRowContainer.items.items.length > 1) {
				body.object.userGroupAccesses = [];
				for (var i = 1, item; i < userGroupRowContainer.items.items.length; i++) {
					item = userGroupRowContainer.items.items[i];
					body.object.userGroupAccesses.push(item.getAccess());
				}
			}

			return body;
		};

		// Initialize
		userGroupStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: gis.init.contextPath + '/api/sharing/search',
                extraParams: {
                    pageSize: 50
                },
                startParam: false,
				limitParam: false,
				reader: {
					type: 'json',
					root: 'userGroups'
				}
			}
		});

		userGroupField = Ext.create('Ext.form.field.ComboBox', {
			valueField: 'id',
			displayField: 'name',
			emptyText: GIS.i18n.search_for_user_groups,
			queryParam: 'key',
			queryDelay: 200,
			minChars: 1,
			hideTrigger: true,
			fieldStyle: 'height:26px; padding-left:6px; border-radius:1px; font-size:11px',
			style: 'margin-bottom:5px',
			width: 380,
			store: userGroupStore,
			listeners: {
				beforeselect: function(cb) { // beforeselect instead of select, fires regardless of currently selected item
					userGroupButton.enable();
				},
				afterrender: function(cb) {
					cb.inputEl.on('keyup', function() {
						userGroupButton.disable();
					});
				}
			}
		});

		userGroupButton = Ext.create('Ext.button.Button', {
			text: '+',
			style: 'margin-left:2px; padding-right:4px; padding-left:4px; border-radius:1px',
			disabled: true,
			height: 26,
			handler: function(b) {
				userGroupRowContainer.add(UserGroupRow({
					id: userGroupField.getValue(),
					name: userGroupField.getRawValue(),
					access: 'r-------'
				}));

				userGroupField.clearValue();
				b.disable();
			}
		});

		userGroupRowContainer = Ext.create('Ext.container.Container', {
			bodyStyle: 'border:0 none'
		});

		if (sharing.meta.allowExternalAccess) {
			externalAccess = userGroupRowContainer.add({
				xtype: 'checkbox',
				fieldLabel: GIS.i18n.allow_external_access,
				labelSeparator: '',
				labelWidth: 250,
				checked: !!sharing.object.externalAccess
			});
		}

		publicGroup = userGroupRowContainer.add(UserGroupRow({
			id: sharing.object.id,
			name: sharing.object.name,
			access: sharing.object.publicAccess
		}, true, !sharing.meta.allowPublicAccess));

		if (Ext.isArray(sharing.object.userGroupAccesses)) {
			for (var i = 0, userGroupRow; i < sharing.object.userGroupAccesses.length; i++) {
				userGroupRow = UserGroupRow(sharing.object.userGroupAccesses[i]);
				userGroupRowContainer.add(userGroupRow);
			}
		}

		window = Ext.create('Ext.window.Window', {
			title: 'Sharing settings',
			bodyStyle: 'padding:5px 5px 3px; background-color:#fff',
			resizable: false,
			modal: true,
			destroyOnBlur: true,
			items: [
				{
					html: sharing.object.name,
					bodyStyle: 'border:0 none; font-weight:bold; color:#333',
					style: 'margin-bottom:7px'
				},
				{
					xtype: 'container',
					layout: 'column',
					bodyStyle: 'border:0 none',
					items: [
						userGroupField,
						userGroupButton
					]
				},
				{
					html: GIS.i18n.created_by + ' ' + sharing.object.user.name,
					bodyStyle: 'border:0 none; color:#777',
					style: 'margin-top:2px;margin-bottom:7px'
				},
				userGroupRowContainer
			],
			bbar: [
				'->',
				{
					text: 'Save',
					handler: function() {
						Ext.Ajax.request({
							url: gis.init.contextPath + '/api/sharing?type=map&id=' + sharing.object.id,
							method: 'POST',
							headers: {
								'Content-Type': 'application/json'
							},
							params: Ext.encode(getBody())
						});

						window.destroy();
					}
				}
			],
			listeners: {
				show: function(w) {
					var pos = gis.viewport.favoriteWindow.getPosition();
					w.setPosition(pos[0] + 5, pos[1] + 5);

					if (!w.hasDestroyOnBlurHandler) {
						gis.util.gui.window.addDestroyOnBlurHandler(w);
					}

                    gis.viewport.favoriteWindow.destroyOnBlur = false;
				},
                destroy: function() {
                    gis.viewport.favoriteWindow.destroyOnBlur = true;
                }
			}
		});

		return window;
	};

    GIS.app.MapControlPanel = function(name, fn) {
		var button,
			panel;

		button = new OpenLayers.Control.Button({
			displayClass: 'olControlButton',
			trigger: function() {
				fn.call(gis.olmap);
			}
		});

		panel = new OpenLayers.Control.Panel({
			defaultControl: button
		});

		panel.addControls([button]);

		return panel;
	};

	GIS.app.FavoriteWindow = function() {

		// Objects
		var NameWindow,

		// Instances
			nameWindow,

		// Components
			addButton,
			searchTextfield,
			grid,
			prevButton,
			nextButton,
			tbar,
			bbar,
			info,
			nameTextfield,
			createButton,
			updateButton,
			cancelButton,
			favoriteWindow,

		// Vars
			windowWidth = 500,
			windowCmpWidth = windowWidth - 14,

			dimConf = gis.conf.finals.dimension;

		gis.store.maps.on('load', function(store, records) {
			var pager = store.proxy.reader.jsonData.pager;

            if (!pager) {
                return;
            }

			info.setText('Page ' + pager.page + ' of ' + pager.pageCount);

			prevButton.enable();
			nextButton.enable();

			if (pager.page === 1) {
				prevButton.disable();
			}

			if (pager.page === pager.pageCount) {
				nextButton.disable();
			}
		});

		NameWindow = function(id) {
			var window,
				record = gis.store.maps.getById(id);

			nameTextfield = Ext.create('Ext.form.field.Text', {
				height: 26,
				width: 371,
				fieldStyle: 'padding-left: 5px; border-radius: 1px; border-color: #bbb; font-size:11px',
				style: 'margin-bottom:0',
				emptyText: 'Favorite name',
				value: id ? record.data.name : '',
				listeners: {
					afterrender: function() {
						this.focus();
					}
				}
			});

			createButton = Ext.create('Ext.button.Button', {
				text: GIS.i18n.create,
				handler: function() {
					var name = nameTextfield.getValue(),
						layers = gis.util.map.getRenderedVectorLayers(),
                        centerPoint = function() {
                            var lonlat = gis.olmap.getCenter();
                            return new OpenLayers.Geometry.Point(lonlat.lon, lonlat.lat).transform('EPSG:900913', 'EPSG:4326');
                        }(),
						layer,
						views = [],
						view,
						map;

					if (!layers.length) {
						alert('Please create a map first');
						return;
					}

					if (!name) {
						alert('Please enter a name');
						return;
					}

					for (var i = 0; i < layers.length; i++) {
						layer = layers[i];

						view = Ext.clone(layer.core.view);

                        view.hidden = !layer.visibility;

						// add
						view.layer = layer.id;

                        // remove
                        delete view.dataDimensionItems;

						views.push(view);
					}

					map = {
						name: name,
						longitude: centerPoint.x,
						latitude: centerPoint.y,
						zoom: gis.olmap.getZoom(),
						mapViews: views,
						user: {
							id: 'currentUser'
						}
					};

					Ext.Ajax.request({
						url: gis.init.contextPath + '/api/maps/',
						method: 'POST',
						headers: {'Content-Type': 'application/json'},
						params: Ext.encode(map),
						success: function(r) {
							var id = r.getAllResponseHeaders().location.split('/').pop();

                            gis.map = {
                                id: id,
                                name: name
                            };

							gis.store.maps.loadStore();

							window.destroy();
						}
					});
				}
			});

			updateButton = Ext.create('Ext.button.Button', {
				text: GIS.i18n.update,
				handler: function() {
					var name = nameTextfield.getValue(),
                        map;

                    Ext.Ajax.request({
                        url: gis.init.contextPath + '/api/maps/' + id + '.json?fields=' + gis.conf.url.mapFields.join(','),
                        success: function(r) {
                            map = Ext.decode(r.responseText);

                            map.name = name;

                            Ext.Ajax.request({
                                url: gis.init.contextPath + '/api/maps/' + id + '?mergeStrategy=REPLACE',
                                method: 'PUT',
                                headers: {'Content-Type': 'application/json'},
                                params: Ext.encode(map),
                                success: function() {
                                    gis.store.maps.loadStore();

                                    window.destroy();
                                }
                            });
                        }
                    });
				}
			});

			cancelButton = Ext.create('Ext.button.Button', {
				text: GIS.i18n.cancel,
				handler: function() {
					window.destroy();
				}
			});

			window = Ext.create('Ext.window.Window', {
				title: id ? 'Rename favorite' : 'Create new favorite',
				iconCls: 'gis-window-title-icon-favorite',
				bodyStyle: 'padding:1px; background:#fff',
				resizable: false,
				modal: true,
				items: nameTextfield,
				destroyOnBlur: true,
				bbar: [
					cancelButton,
					'->',
					id ? updateButton : createButton
				],
				listeners: {
					show: function(w) {
						this.setPosition(favoriteWindow.x + 14, favoriteWindow.y + 67);

                        if (!w.hasDestroyOnBlurHandler) {
                            gis.util.gui.window.addDestroyOnBlurHandler(w);
                        }

                        gis.viewport.favoriteWindow.destroyOnBlur = true;

						nameTextfield.focus(false, 500);
					}
				}
			});

			return window;
		};

		addButton = Ext.create('Ext.button.Button', {
			text: GIS.i18n.add_new,
			width: 67,
			height: 26,
			style: 'border-radius: 1px;',
			menu: {},
			handler: function() {
				nameWindow = new NameWindow(null, 'create');
				nameWindow.show();
			}
		});

		searchTextfield = Ext.create('Ext.form.field.Text', {
			width: windowCmpWidth - addButton.width - 3,
			height: 26,
			fieldStyle: 'padding-right: 0; padding-left: 4px; border-radius: 1px; border-color: #bbb; font-size:11px',
			emptyText: GIS.i18n.search_for_favorites,
			enableKeyEvents: true,
			currentValue: '',
			listeners: {
				keyup: {
					fn: function() {
						if (this.getValue() !== this.currentValue) {
							this.currentValue = this.getValue();

							var value = this.getValue(),
								url = value ? gis.init.contextPath + '/api/maps.json?fields=id,displayName|rename(name),access' + (value ? '&filter=displayName:ilike:' + value : '') : null;
								store = gis.store.maps;

							store.page = 1;
							store.loadStore(url);
						}
					},
					buffer: 100
				}
			}
		});

		prevButton = Ext.create('Ext.button.Button', {
			text: GIS.i18n.prev,
			handler: function() {
				var value = searchTextfield.getValue(),
					url = value ? gis.init.contextPath + '/api/maps.json?fields=id,displayName|rename(name),access' + (value ? '&filter=displayName:ilike:' + value : '') : null;
					store = gis.store.maps;

				store.page = store.page <= 1 ? 1 : store.page - 1;
				store.loadStore(url);
			}
		});

		nextButton = Ext.create('Ext.button.Button', {
			text: GIS.i18n.next,
			handler: function() {
				var value = searchTextfield.getValue(),
					url = value ? gis.init.contextPath + '/api/maps.json?fields=id,displayName|rename(name),access' + (value ? '&filter=displayName:ilike:' + value : '') : null;
					store = gis.store.maps;

				store.page = store.page + 1;
				store.loadStore(url);
			}
		});

		info = Ext.create('Ext.form.Label', {
			cls: 'gis-label-info',
			width: 300,
			height: 22
		});

		grid = Ext.create('Ext.grid.Panel', {
			cls: 'gis-grid',
			scroll: false,
			hideHeaders: true,
			columns: [
				{
					dataIndex: 'name',
					sortable: false,
					width: windowCmpWidth - 88,
					renderer: function(value, metaData, record) {
						var fn = function() {
							var element = Ext.get(record.data.id);

							if (element) {
								element = element.parent('td');
								element.addClsOnOver('link');
								element.load = function() {
									favoriteWindow.hide();
									gis.map = {id: record.data.id};
									GIS.core.MapLoader(gis).load();
								};
								element.dom.setAttribute('onclick', 'Ext.get(this).load();');
							}
						};

						Ext.defer(fn, 100);

						return '<div id="' + record.data.id + '" class="el-fontsize-10">' + value + '</div>';
					}
				},
				{
					xtype: 'actioncolumn',
					sortable: false,
					width: 80,
					items: [
						{
							iconCls: 'gis-grid-row-icon-edit',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-edit' + (!record.data.access.update ? ' disabled' : '');
							},
							handler: function(grid, rowIndex, colIndex, col, event) {
								var record = this.up('grid').store.getAt(rowIndex);

								if (record.data.access.update) {
									nameWindow = new NameWindow(record.data.id);
									nameWindow.show();
								}
							}
						},
						{
							iconCls: 'gis-grid-row-icon-overwrite',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-overwrite' + (!record.data.access.update ? ' disabled' : '');
							},
							handler: function(grid, rowIndex, colIndex, col, event) {
								var record = this.up('grid').store.getAt(rowIndex),
									layers,
									layer,
									lonlat,
									views,
									view,
									map,
									message;

								if (record.data.access.update) {
									layers = gis.util.map.getRenderedVectorLayers();
									message = 'Overwrite favorite?\n\n' + record.data.name;

									if (layers.length) {
										if (confirm(message)) {
											lonlat = gis.olmap.getCenter();
											views = [];

											for (var i = 0; i < layers.length; i++) {
												layer = layers[i];
												view = layer.core.view;

												// add
												view.layer = layer.id;
                                                view.hidden = !layer.visibility;

												// remove
												delete view.periodType;
                                                delete view.dataDimensionItems;

												views.push(view);
											}

											map = {
                                                name: record.data.name,
												longitude: lonlat.lon,
												latitude: lonlat.lat,
												zoom: gis.olmap.getZoom(),
												mapViews: views
											};

											Ext.Ajax.request({
												url: gis.init.contextPath + '/api/maps/' + record.data.id + '?mergeStrategy=REPLACE',
												method: 'PUT',
												headers: {'Content-Type': 'application/json'},
												params: Ext.encode(map),
												success: function() {
													gis.map = map;
													gis.store.maps.loadStore();
												}
											});
										}
									}
									else {
										alert(GIS.i18n.no_map_to_save);
									}
								}
							}
						},
						{
							iconCls: 'gis-grid-row-icon-sharing',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-sharing' + (!record.data.access.manage ? ' disabled' : '');
							},
							handler: function(grid, rowIndex) {
								var record = this.up('grid').store.getAt(rowIndex);

								if (record.data.access.manage) {
									Ext.Ajax.request({
										url: gis.init.contextPath + '/api/sharing?type=map&id=' + record.data.id,
										method: 'GET',
										failure: function(r) {
                                            gis.olmap.mask.hide();
                                            gis.alert(r);
										},
										success: function(r) {
											var sharing = Ext.decode(r.responseText),
												window = GIS.app.SharingWindow(sharing);
											window.show();
										}
									});
								}
							}
						},
						{
							iconCls: 'gis-grid-row-icon-delete',
							getClass: function(value, metaData, record) {
								return 'tooltip-favorite-delete' + (!record.data.access['delete'] ? ' disabled' : '');
							},
							handler: function(grid, rowIndex, colIndex, col, event) {
								var record = this.up('grid').store.getAt(rowIndex),
									message;

								if (record.data.access['delete']) {
									message = 'Delete favorite?\n\n' + record.data.name;

									if (confirm(message)) {
										Ext.Ajax.request({
											url: gis.init.contextPath + '/api/maps/' + record.data.id,
											method: 'DELETE',
											success: function() {
												gis.store.maps.loadStore();
											}
										});
									}
								}
							}
						}
					]
				},
				{
					sortable: false,
					width: 6
				}
			],
			store: gis.store.maps,
			bbar: [
				info,
				'->',
				prevButton,
				nextButton
			],
			listeners: {
				added: function() {
					gis.viewport.mapGrid = this;
				},
				render: function() {
					var size = Math.floor((gis.viewport.centerRegion.getHeight() - 155) / gis.conf.layout.grid.row_height);
					this.store.pageSize = size;
					this.store.page = 1;
					this.store.loadStore();

					gis.store.maps.on('load', function() {
						if (this.isVisible()) {
							this.fireEvent('afterrender');
						}
					}, this);
				},
				afterrender: function() {
					var fn = function() {
						var editArray = Ext.query('.tooltip-favorite-edit'),
							overwriteArray = Ext.query('.tooltip-favorite-overwrite'),
							sharingArray = Ext.query('.tooltip-favorite-sharing'),
							dashboardArray = Ext.query('.tooltip-favorite-dashboard'),
							deleteArray = Ext.query('.tooltip-favorite-delete'),
							el;

						for (var i = 0; i < editArray.length; i++) {
							var el = editArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: GIS.i18n.rename,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}

						for (var i = 0; i < overwriteArray.length; i++) {
							el = overwriteArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: GIS.i18n.overwrite,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}

						for (var i = 0; i < sharingArray.length; i++) {
							el = sharingArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: GIS.i18n.share_with_other_people,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}

						for (var i = 0; i < dashboardArray.length; i++) {
							el = dashboardArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: GIS.i18n.add_to_dashboard,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}

						for (var i = 0; i < deleteArray.length; i++) {
							el = deleteArray[i];
							Ext.create('Ext.tip.ToolTip', {
								target: el,
								html: GIS.i18n.delete_,
								'anchor': 'bottom',
								anchorOffset: -14,
								showDelay: 1000
							});
						}
					};

					Ext.defer(fn, 100);
				},
				itemmouseenter: function(grid, record, item) {
					this.currentItem = Ext.get(item);
					this.currentItem.removeCls('x-grid-row-over');
				},
				select: function() {
					this.currentItem.removeCls('x-grid-row-selected');
				},
				selectionchange: function() {
					this.currentItem.removeCls('x-grid-row-focused');
				}
			}
		});

		favoriteWindow = Ext.create('Ext.window.Window', {
			title: GIS.i18n.manage_favorites + (gis.map ? '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + gis.map.name + '</span>' : ''),
			iconCls: 'gis-window-title-icon-favorite',
			cls: 'gis-container-default',
			bodyStyle: 'padding:1px',
			resizable: false,
			modal: true,
			width: windowWidth,
			destroyOnBlur: true,
			items: [
				{
					xtype: 'panel',
					layout: 'hbox',
					cls: 'gis-container-inner',
					height: 27,
					items: [
						addButton,
						{
							height: 26,
							width: 1,
							style: 'width:1px; margin-left:1px; margin-right:1px',
							bodyStyle: 'border-left: 1px solid #aaa'
						},
						searchTextfield
					]
				},
				grid
			],
			listeners: {
				show: function(w) {
					this.setPosition(199, 33);

					if (!w.hasDestroyOnBlurHandler) {
						gis.util.gui.window.addDestroyOnBlurHandler(w);
					}

					searchTextfield.focus(false, 500);
				}
			}
		});

		return favoriteWindow;
	};

	GIS.app.LegendSetWindow = function() {

		// Stores
		var legendSetStore,
			legendStore,
			tmpLegendStore,

		// Objects
			LegendSetPanel,
			LegendPanel,

		// Instances
			legendSetPanel,
			legendPanel,

		// Components
			window,
			legendSetName,
			legendName,
			startValue,
			endValue,
			color,
			legendGrid,
			create,
			update,
			cancel,
			info,

		// Functions
			showUpdateLegendSet,
			deleteLegendSet,
			deleteLegend,
			getRequestBody,
			reset,
			validateLegends,

            windowWidth = 450,
            windowBorder = 12,
            bodyPadding = 2,

            legendBodyBorder = 1,
            legendBodyPadding = 1,
            fieldLabelWidth = 105,
            gridPadding = 1;

		legendSetStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: gis.init.contextPath + '/api/legendSets.json?fields=id,displayName|rename(name)&paging=false',
				reader: {
					type: 'json',
					root: 'legendSets'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			},
			listeners: {
				load: function(store, records) {
					this.sort('name', 'ASC');

					info.setText(records.length + ' legend set' + (records.length !== 1 ? 's' : '') + ' available');
				}
			}
		});

		legendStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'startValue', 'endValue', 'color'],
			proxy: {
				type: 'ajax',
				url: '',
				reader: {
					type: 'json',
					root: 'legends'
				}
			},
			deleteLegend: deleteLegend,
			listeners: {
				load: function(store, records) {
					var data = [],
						record;

					for (var i = 0; i < records.length; i++) {
						data.push(records[i].data);
					}

					Ext.Array.sort(data, function (a, b) {
						return a.startValue - b.startValue;
					});

					tmpLegendStore.add(data);

					info.setText(records.length + ' legend sets available');
				}
			}
		});

		LegendSetPanel = function() {
			var items,
				addButton;

			addButton = Ext.create('Ext.button.Button', {
				text: GIS.i18n.add_new,
				height: 26,
				style: 'border-radius: 1px',
				menu: {},
				handler: function() {
					showUpdateLegendSet();
				}
			});

			legendSetGrid = Ext.create('Ext.grid.Panel', {
				cls: 'gis-grid',
				scroll: 'vertical',
				height: true,
				hideHeaders: true,
				currentItem: null,
				columns: [
					{
						dataIndex: 'name',
						sortable: false,
						width: 369
					},
					{
						xtype: 'actioncolumn',
						sortable: false,
						width: 40,
						items: [
							{
								iconCls: 'gis-grid-row-icon-edit',
								getClass: function() {
									return 'tooltip-legendset-edit';
								},
								handler: function(grid, rowIndex, colIndex, col, event) {
									var id = this.up('grid').store.getAt(rowIndex).data.id;
									showUpdateLegendSet(id);
								}
							},
							{
								iconCls: 'gis-grid-row-icon-delete',
								getClass: function() {
									return 'tooltip-legendset-delete';
								},
								handler: function(grid, rowIndex, colIndex, col, event) {
									var record = this.up('grid').store.getAt(rowIndex),
										id = record.data.id,
										name = record.data.name,
										message = 'Delete legend set?\n\n' + name;

									if (confirm(message)) {
										deleteLegendSet(id);
									}
								}
							}
						]
					},
					{
						sortable: false,
						width: 17
					}
				],
				store: legendSetStore,
				listeners: {
					render: function() {
						var that = this,
							maxHeight = gis.viewport.centerRegion.getHeight() - 155,
							height;

						this.store.on('load', function() {
							if (Ext.isDefined(that.setHeight)) {
								height = 1 + that.store.getCount() * gis.conf.layout.grid.row_height;
								that.setHeight(height > maxHeight ? maxHeight : height);
								window.doLayout();
							}
						});

						this.store.load();
					},
					afterrender: function() {
						var fn = function() {
							var editArray = document.getElementsByClassName('tooltip-legendset-edit'),
								deleteArray = document.getElementsByClassName('tooltip-legendset-delete'),
								len = editArray.length,
								el;

							for (var i = 0; i < len; i++) {
								el = editArray[i];
								Ext.create('Ext.tip.ToolTip', {
									target: el,
									html: 'Edit',
									'anchor': 'bottom',
									anchorOffset: -14,
									showDelay: 1000
								});

								el = deleteArray[i];
								Ext.create('Ext.tip.ToolTip', {
									target: el,
									html: 'Delete',
									'anchor': 'bottom',
									anchorOffset: -14,
									showDelay: 1000
								});
							}
						};

						Ext.defer(fn, 100);
					},
					itemmouseenter: function(grid, record, item) {
						this.currentItem = Ext.get(item);
						this.currentItem.removeCls('x-grid-row-over');
					},
					select: function() {
						this.currentItem.removeCls('x-grid-row-selected');
					},
					selectionchange: function() {
						this.currentItem.removeCls('x-grid-row-focused');
					}
				}
			});

			items = [
				{
					xtype: 'panel',
					layout: 'hbox',
					cls: 'gis-container-inner',
                    bodyStyle: 'padding: 0',
					style: 'margin-bottom: 1px',
					items: [
						addButton
					]
				},
				legendSetGrid
			];

			return items;
		};

		LegendPanel = function(id) {
			var panel,
				addLegend,
				reset,
				data = [],

                LegendEditWindow;

            // edit legend panel
            LegendEditWindow = function(record) {
                var editLegendName,
                    editStartValue,
                    editEndValue,
                    editColor,
                    editCancel,
                    editUpdate,
                    showUpdateLegend,
                    validateForm,
                    editWindow;

                editLegendName = Ext.create('Ext.form.field.Text', {
                    cls: 'gis-textfield',
                    width: windowWidth - windowBorder - bodyPadding - (2 * legendBodyBorder) - (2 * legendBodyPadding) + 4,
                    height: 23,
                    fieldStyle: 'padding-left: 3px; border-color: #bbb',
                    labelStyle: 'padding-top: 5px; padding-left: 3px',
                    fieldLabel: GIS.i18n.legend_name,
                    value: record.data.name
                });

                editStartValue = Ext.create('Ext.form.field.Number', {
                    width: 163 + 2,
                    height: 23,
                    allowDecimals: true,
                    style: 'margin-bottom: 0px',
                    value: record.data.startValue
                });

                editEndValue = Ext.create('Ext.form.field.Number', {
                    width: 163 + 2,
                    height: 23,
                    allowDecimals: true,
                    style: 'margin-bottom: 0px; margin-left: 1px',
                    value: record.data.endValue
                });

                editColor = Ext.create('Ext.ux.button.ColorButton', {
                    width: windowWidth - windowBorder - bodyPadding - (2 * legendBodyBorder) - (2 * legendBodyPadding) - fieldLabelWidth + 4,
                    height: 23,
                    style: 'border-radius: 1px',
                    value: record.data.color.replace('#', '')
                });

                validateEditLegendForm = function() {
                    if (!(editLegendName.getValue() && Ext.isNumber(editStartValue.getValue()) && Ext.isNumber(editEndValue.getValue()) && editColor.getValue())) {
                        return;
                    }

                    if (editStartValue.getValue() >= editEndValue.getValue()) {
                        return;
                    }

                    return true;
                };

                editCancel = Ext.create('Ext.button.Button', {
                    text: 'Cancel',
                    handler: function() {
                        editWindow.destroy();
                    }
                });

                editUpdate = Ext.create('Ext.button.Button', {
                    text: 'Update',
                    handler: function() {
                        if (!validateEditLegendForm()) {
                            return;
                        }

                        record.set('name', editLegendName.getValue());
                        record.set('startValue', editStartValue.getValue());
                        record.set('endValue', editEndValue.getValue());
                        record.set('color', '#' + editColor.getValue());

                        editWindow.destroy();
                        window.isDirty = true;
                        tmpLegendStore.sort('startValue', 'ASC');
                    }
                });

                editWindow = Ext.create('Ext.window.Window', {
                    title: 'Edit legend (' + record.data.name + ')',
                    width: windowWidth,
                    modal: true,
                    shadow: true,
                    resizable: false,
                    bodyStyle: 'background: #fff; padding: 1px',
                    bbar: [
                        editCancel,
                        '->',
                        editUpdate
                    ],
                    items: [
                        editLegendName,
                        {
                            layout: 'hbox',
                            cls: 'gis-container-inner',
                            bodyStyle: 'background: transparent',
                            items: [
                                {
                                    html: GIS.i18n.start_end_value + ':',
                                    width: fieldLabelWidth,
                                    bodyStyle: 'background:transparent; padding-top:3px; padding-left:3px'
                                },
                                editStartValue,
                                editEndValue
                            ]
                        },
                        {
                            layout: 'column',
                            cls: 'gis-container-inner',
                            bodyStyle: 'background: transparent',
                            items: [
                                {
                                    html: GIS.i18n.legend_symbolizer + ':',
                                    width: fieldLabelWidth,
                                    bodyStyle: 'background:transparent; padding-top:3px; padding-left:3px'
                                },
                                editColor
                            ]
                        }
                    ]
                });

                return editWindow;
            };

            showUpdateLegend = function(record) {
                LegendEditWindow(record).showAt(window.getPosition()[0], window.getPosition()[1] + 55);
            };

            // legend panel
			tmpLegendStore = Ext.create('Ext.data.ArrayStore', {
				fields: ['id', 'name', 'startValue', 'endValue', 'color']
			});

			legendSetName = Ext.create('Ext.form.field.Text', {
				cls: 'gis-textfield',
				width: windowWidth - windowBorder - bodyPadding,
				height: 25,
				fieldStyle: 'padding-left: 5px; border-color: #bbb',
                labelStyle: 'padding-top: 5px; padding-left: 3px',
				fieldLabel: GIS.i18n.legend_set_name,
                style: 'margin-bottom: 6px'
			});

			legendName = Ext.create('Ext.form.field.Text', {
				cls: 'gis-textfield',
				width: windowWidth - windowBorder - bodyPadding - (2 * legendBodyBorder) - (2 * legendBodyPadding),
				height: 23,
				fieldStyle: 'padding-left: 3px; border-color: #bbb',
                labelStyle: 'padding-top: 5px; padding-left: 3px',
				fieldLabel: GIS.i18n.legend_name
			});

			startValue = Ext.create('Ext.form.field.Number', {
				width: 163,
				height: 23,
				allowDecimals: true,
                style: 'margin-bottom: 0px',
				value: 0
			});

			endValue = Ext.create('Ext.form.field.Number', {
				width: 163,
				height: 23,
				allowDecimals: true,
                style: 'margin-bottom: 0px; margin-left: 1px',
				value: 0
			});

			color = Ext.create('Ext.ux.button.ColorButton', {
				width: windowWidth - windowBorder - bodyPadding - (2 * legendBodyBorder) - (2 * legendBodyPadding) - fieldLabelWidth,
				height: 23,
				style: 'border-radius: 1px',
				value: 'e1e1e1'
			});

			addLegend = Ext.create('Ext.button.Button', {
				text: GIS.i18n.add_legend,
				height: 26,
				style: 'border-radius: 1px',
				handler: function() {
					var date = new Date(),
						id = date.toISOString(),
						ln = legendName.getValue(),
						sv = startValue.getValue(),
						ev = endValue.getValue(),
						co = color.getValue().toUpperCase(),
						items = tmpLegendStore.data.items,
						data = [];

					if (ln && (ev > sv)) {
						for (var i = 0; i < items.length; i++) {
							data.push(items[i].data);
						}

						data.push({
							id: id,
							name: ln,
							startValue: sv,
							endValue: ev,
							color: '#' + co
						});

						Ext.Array.sort(data, function (a, b) {
							return a.startValue - b.startValue;
						});

						tmpLegendStore.removeAll();
						tmpLegendStore.add(data);

						legendName.reset();
						startValue.reset();
						endValue.reset();
						color.reset();

                        window.isDirty = true;
					}
				}
			});

			legendGrid = Ext.create('Ext.grid.Panel', {
				cls: 'gis-grid',
				bodyStyle: 'border-top: 0 none',
				width: windowWidth - windowBorder - bodyPadding - (2 * gridPadding),
				height: 235,
				scroll: 'vertical',
				hideHeaders: true,
				currentItem: null,
				columns: [
					{
						dataIndex: 'name',
						sortable: false,
						width: 236
					},
					{
						sortable: false,
						width: 45,
						renderer: function(value, metaData, record) {
							return '<span style="color:' + record.data.color + '">Color</span>';
						}
					},
					{
						dataIndex: 'startValue',
						sortable: false,
						width: 45
					},
					{
						dataIndex: 'endValue',
						sortable: false,
						width: 45
					},
					{
						xtype: 'actioncolumn',
						sortable: false,
						width: 40,
						items: [
							{
								iconCls: 'gis-grid-row-icon-edit',
								getClass: function() {
									return 'tooltip-legendset-edit';
								},
								handler: function(grid, rowIndex, colIndex, col, event) {
									var record = this.up('grid').store.getAt(rowIndex);
									showUpdateLegend(record);
								}
							},
							{
								iconCls: 'gis-grid-row-icon-delete',
								getClass: function() {
									return 'tooltip-legend-delete';
								},
								handler: function(grid, rowIndex, colIndex, col, event) {
									var id = this.up('grid').store.getAt(rowIndex).data.id;
									deleteLegend(id);
								}
							}
						]
					},
					{
						sortable: false,
						width: 17
					}
				],
				store: tmpLegendStore,
				listeners: {
					itemmouseenter: function(grid, record, item) {
						this.currentItem = Ext.get(item);
						this.currentItem.removeCls('x-grid-row-over');
					},
					select: function() {
						this.currentItem.removeCls('x-grid-row-selected');
					},
					selectionchange: function() {
						this.currentItem.removeCls('x-grid-row-focused');
					},
					afterrender: function() {
						var fn = function() {
							var deleteArray = document.getElementsByClassName('tooltip-legend-delete'),
								len = deleteArray.length,
								el;

							for (var i = 0; i < len; i++) {
								el = deleteArray[i];
								Ext.create('Ext.tip.ToolTip', {
									target: el,
									html: 'Delete',
									'anchor': 'bottom',
									anchorOffset: -14,
									showDelay: 1000
								});
							}
						};

						Ext.defer(fn, 100);
					}
				}
			});

			panel = Ext.create('Ext.panel.Panel', {
				cls: 'gis-container-inner',
				bodyStyle: 'padding:0px',
				legendSetId: id,
				items: [
					legendSetName,
					{
                        xtype: 'container',
						html: GIS.i18n.add_legend,
						cls: 'gis-panel-html-title',
                        style: 'padding-left: 3px; margin-bottom: 3px'
                    },
					{
						bodyStyle: 'background-color:#f1f1f1; border:1px solid #ccc; border-radius:1px; padding:' + legendBodyPadding + 'px',
                        style: 'margin-bottom: 1px',
						items: [
							legendName,
							{
								layout: 'hbox',
								bodyStyle: 'background: transparent',
								items: [
									{
										html: GIS.i18n.start_end_value + ':',
										width: fieldLabelWidth,
										bodyStyle: 'background:transparent; padding-top:3px; padding-left:3px'
									},
									startValue,
									endValue
								]
							},
							{
								layout: 'column',
								cls: 'gis-container-inner',
								bodyStyle: 'background: transparent',
								items: [
									{
										html: GIS.i18n.legend_symbolizer + ':',
										width: fieldLabelWidth,
										bodyStyle: 'background:transparent; padding-top:3px; padding-left:3px'
									},
									color
								]
							}
						]
					},
					{
						cls: 'gis-container-inner',
						bodyStyle: 'text-align: right',
						width: windowWidth - windowBorder - bodyPadding,
						items: addLegend
					},
					{
                        xtype: 'container',
						html: GIS.i18n.current_legends,
						cls: 'gis-panel-html-title',
                        style: 'padding-left: 3px; margin-bottom: 3px'
                    },
                    {
                        xtype: 'container',
                        cls: 'gis-container-inner',
                        style: 'padding:' + gridPadding + 'px',
                        items: legendGrid
                    }
				]
			});

			if (id) {
				legendStore.proxy.url = gis.init.contextPath + '/api/legendSets/' + id + '.json?fields=legends[id,displayName|rename(name),startValue,endValue,color]';
				legendStore.load();

				legendSetName.setValue(legendSetStore.getById(id).data.name);
			}

			return panel;
		};

		showUpdateLegendSet = function(id) {
			legendPanel = new LegendPanel(id);
			window.removeAll();
			window.add(legendPanel);
			info.hide();
			cancel.show();

			if (id) {
				update.show();
			}
			else {
				create.show();
			}
		};

		deleteLegendSet = function(id) {
			if (id) {
				Ext.Ajax.request({
					url: gis.init.contextPath + '/api/legendSets/' + id,
					method: 'DELETE',
					success: function() {
						legendSetStore.load();
						gis.store.legendSets.load();
					}
				});
			}
		};

		deleteLegend = function(id) {
			tmpLegendStore.remove(tmpLegendStore.getById(id));
		};

		getRequestBody = function() {
			var items = tmpLegendStore.data.items,
				body;

			body = {
				name: legendSetName.getValue(),
				symbolizer: gis.conf.finals.widget.symbolizer_color,
				legends: []
			};

			for (var i = 0; i < items.length; i++) {
				var item = items[i];
				body.legends.push({
					name: item.data.name,
					startValue: item.data.startValue,
					endValue: item.data.endValue,
					color: item.data.color
				});
			}

			return body;
		};

		reset = function() {
			legendPanel.destroy();
			legendSetPanel = new LegendSetPanel();
			window.removeAll();
			window.add(legendSetPanel);
            window.isDirty = false;

			info.show();
			cancel.hide();
			create.hide();
			update.hide();
		};

		validateLegends = function() {
			var items = tmpLegendStore.data.items,
				item,
				prevItem;

			if (items.length === 0) {
				alert('At least one legend is required');
				return false;
			}

			for (var i = 1; i < items.length; i++) {
				item = items[i].data;
				prevItem = items[i - 1].data;

				if (item.startValue < prevItem.endValue) {
					var msg = 'Overlapping legends not allowed!\n\n' +
							  prevItem.name + ' (' + prevItem.startValue + ' - ' + prevItem.endValue + ')\n' +
							  item.name + ' (' + item.startValue + ' - ' + item.endValue + ')';
					alert(msg);
					return false;
				}

				if (prevItem.endValue < item.startValue) {
					var msg = 'Legend gaps detected!\n\n' +
							  prevItem.name + ' (' + prevItem.startValue + ' - ' + prevItem.endValue + ')\n' +
							  item.name + ' (' + item.startValue + ' - ' + item.endValue + ')\n\n' +
							  'Proceed anyway?';

					if (!confirm(msg)) {
						return false;
					}
				}
			}

			return true;
		};

		create = Ext.create('Ext.button.Button', {
			text: GIS.i18n.create,
			hidden: true,
			handler: function() {
				if (legendSetName.getValue() && validateLegends()) {
					if (legendSetStore.findExact('name', legendSetName.getValue()) !== -1) {
						alert('Name already in use');
						return;
					}

					var body = Ext.encode(getRequestBody());

					Ext.Ajax.request({
						url: gis.init.contextPath + '/api/legendSets/',
						method: 'POST',
						headers: {'Content-Type': 'application/json'},
						params: body,
						success: function() {
							gis.store.legendSets.load();
							reset();
						}
					});
				}
			}
		});

		update = Ext.create('Ext.button.Button', {
			text: GIS.i18n.update,
			hidden: true,
			handler: function() {
				if (legendSetName.getValue() && validateLegends()) {
					var body = getRequestBody(),
						id = legendPanel.legendSetId;
					body.id = id;
					body = Ext.encode(getRequestBody());

					Ext.Ajax.request({
						url: gis.init.contextPath + '/api/legendSets/' + id + '?mergeStrategy=REPLACE',
						method: 'PUT',
						headers: {'Content-Type': 'application/json'},
						params: body,
						success: function() {
							reset();
						}
					});
				}
			}
		});

		cancel = Ext.create('Ext.button.Button', {
			text: GIS.i18n.cancel,
			hidden: true,
			handler: function() {
				reset();
			}
		});

		info = Ext.create('Ext.form.Label', {
			cls: 'gis-label-info',
			width: 400,
			height: 22
		});

		window = Ext.create('Ext.window.Window', {
			title: GIS.i18n.legendsets,
			iconCls: 'gis-window-title-icon-legendset', //todo
            bodyStyle: 'padding:1px; background-color:#fff',
			resizable: false,
			width: windowWidth,
			modal: true,
			items: new LegendSetPanel(),
            destroyOnBlur: true,
			bbar: {
				height: 27,
				items: [
					info,
					cancel,
					'->',
					create,
					update
				]
			},
			listeners: {
				show: function(w) {
					this.setPosition(269, 33);

					if (!w.hasDestroyOnBlurHandler) {
						gis.util.gui.window.addDestroyOnBlurHandler(w);
					}
				},
                beforeclose: function() {
                    if (window.isDirty) {
                        return confirm('The legend set has unsaved modifications. Close anyway?');
                    }
                }
			}
		});

		return window;
	};

	GIS.app.DownloadWindow = function() {
		var window,
			format,
			name,
			button;

		format = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			width: 60,
			style: 'margin-bottom:0; margin-left:1px',
			valueField: 'id',
			displayField: 'text',
			editable: false,
			queryMode: 'local',
			forceSelection: true,
			value: 'png',
			store: Ext.create('Ext.data.ArrayStore', {
				fields: ['id', 'text'],
				data: [
					['png', 'PNG'],
					['pdf', 'PDF']
				]
			})
		});

		name = Ext.create('Ext.form.field.Text', {
			cls: 'gis-textfield',
			//height: 23,
			width: 230,
			fieldStyle: 'padding-left:4px',
			style: 'margin-bottom:0',
			emptyText: GIS.i18n.please_enter_map_title
		});

		button = Ext.create('Ext.button.Button', {
			text: GIS.i18n.download,
			handler: function() {
				var type = format.getValue(),
					title = name.getValue(),
					svg = gis.util.svg.getString(title, gis.util.map.getVisibleVectorLayers()),
					exportForm = document.getElementById('exportForm');

				if (!svg) {
					alert(GIS.i18n.please_create_map_first);
					return;
				}

				document.getElementById('filenameField').value = title;
				document.getElementById('svgField').value = svg;
				exportForm.action = gis.init.contextPath + '/api/svg.' + type;
				exportForm.submit();

				window.destroy();
			}
		});

		window = Ext.create('Ext.window.Window', {
			title: GIS.i18n.download_map_as_png,
			layout: 'column',
			iconCls: 'gis-window-title-icon-download',
			cls: 'gis-container-default',
            bodyStyle: 'padding:1px',
			resizable: true,
			modal: true,
            destroyOnBlur: true,
			items: [
				name,
				format
			],
			bbar: [
				'->',
				button
			],
			listeners: {
				show: function(w) {
					this.setPosition(253, 33);

					if (!w.hasDestroyOnBlurHandler) {
						gis.util.gui.window.addDestroyOnBlurHandler(w);
					}
				}
			}
		});

		return window;
	};

	GIS.app.InterpretationWindow = function() {
		var textArea,
			shareButton,
			window;

		if (Ext.isString(gis.map.id)) {
            textArea = Ext.create('Ext.form.field.TextArea', {
                cls: 'gis-textarea',
                height: 130,
                fieldStyle: 'padding-left: 3px; padding-top: 3px',
                emptyText: GIS.i18n.write_your_interpretation + '..',
				enableKeyEvents: true,
				listeners: {
					keyup: function() {
						shareButton.xable();
					}
				}
            });

			shareButton = Ext.create('Ext.button.Button', {
				text: GIS.i18n.share,
				disabled: true,
				xable: function() {
					this.setDisabled(!textArea.getValue());
				},
				handler: function() {
					if (textArea.getValue()) {
						Ext.Ajax.request({
                            url: gis.init.contextPath + '/api/interpretations/map/' + gis.map.id,
							method: 'POST',
							params: textArea.getValue(),
							headers: {'Content-Type': 'text/html'},
							success: function() {
								textArea.reset();
								window.hide();
							}
						});
					}
				}
			});

            window = Ext.create('Ext.window.Window', {
				title: 'Write interpretation' + '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + gis.map.name + '</span>',
                layout: 'fit',
                iconCls: 'gis-window-title-icon-interpretation',
                cls: 'gis-container-default',
                bodyStyle: 'padding: 1px',
                width: 500,
                resizable: true,
                modal: true,
                destroyOnBlur: true,
                items: [
                    textArea
                ],
				bbar: {
					//cls: 'gis-toolbar-bbar',
					defaults: {
						height: 24
					},
					items: [
						'->',
						shareButton
					]
				},
                listeners: {
                    show: function(w) {
                        this.setPosition(325, 33);

                        if (!w.hasDestroyOnBlurHandler) {
                            gis.util.gui.window.addDestroyOnBlurHandler(w);
                        }

						document.body.oncontextmenu = true;
                    },
					hide: function() {
						document.body.oncontextmenu = function(){return false;};
					},
					destroy: function() {
						gis.viewport.interpretationWindow = null;
					}
                }
            });

			return window;
		}

		return;
    };

	GIS.app.AboutWindow = function() {
		return Ext.create('Ext.window.Window', {
			title: GIS.i18n.about,
			bodyStyle: 'background:#fff; padding:6px',
			modal: true,
            resizable: false,
			destroyOnBlur: true,
			listeners: {
				show: function(w) {
					Ext.Ajax.request({
						url: gis.init.contextPath + '/api/system/info.json',
						success: function(r) {
							var info = Ext.decode(r.responseText),
								divStyle = 'padding:3px',
								html = '<div class="user-select">';

							if (Ext.isObject(info)) {
								html += '<div style="' + divStyle + '"><b>' + GIS.i18n.time_since_last_data_update + ': </b>' + info.intervalSinceLastAnalyticsTableSuccess + '</div>';
								html += '<div style="' + divStyle + '"><b>' + GIS.i18n.version + ': </b>' + info.version + '</div>';
								html += '<div style="' + divStyle + '"><b>' + GIS.i18n.revision + ': </b>' + info.revision + '</div>';
                                html += '<div style="' + divStyle + '"><b>' + GIS.i18n.username + ': </b>' + gis.init.userAccount.username + '</div>';
                                html += '</div>';
							}
							else {
								html += 'No system info found';
							}

							w.update(html);
						},
						failure: function(r) {
							w.update(r.status + '\n' + r.statusText + '\n' + r.responseText);
						},
                        callback: function() {
                            document.body.oncontextmenu = true;

                            gis.util.gui.window.setAnchorPosition(w, gis.viewport.aboutButton);

                            //if (!w.hasHideOnBlurHandler) {
                                //ns.core.web.window.addHideOnBlurHandler(w);
                            //}
                        }
					});

					if (!w.hasDestroyOnBlurHandler) {
						gis.util.gui.window.addDestroyOnBlurHandler(w);
					}
				},
                hide: function() {
                    document.body.oncontextmenu = function() {
                        return false;
                    };
                },
                destroy: function() {
                    document.body.oncontextmenu = function() {
                        return false;
                    };
                }
			}
		});
	};

	GIS.app.LayerWidgetEvent = function(layer) {

		// stores
		var programStore,
			stagesByProgramStore,
            //dataElementsByStageStore,

        // cache
            stageStorage = {},
            attributeStorage = {},
            dataElementStorage = {},

		// components
			program,
            onProgramSelect,
			stage,
            onStageSelect,
            loadDataElements,
            dataElementAvailable,
            dataElementSelected,
            addUxFromDataElement,
            selectDataElements,
            dataElement,

            periodMode,
            onPeriodModeSelect,
            getDateLink,
			startDate,
			endDate,
            startEndDate,

            onPeriodChange,
            onCheckboxAdd,
            intervalListeners,
            relativePeriodCmpMap = {},
            weeks,
            months,
            biMonths,
            quarters,
            sixMonths,
            financialYears,
            years,
            relativePeriod,
            checkboxes = [],

            fixedPeriodAvailable,
            fixedPeriodSelected,
            onPeriodTypeSelect,
            periodType,
            prevYear,
            nextYear,
            fixedPeriodSettings,
            fixedPeriodAvailableSelected,
            periods,
			period,

			treePanel,
			userOrganisationUnit,
			userOrganisationUnitChildren,
			userOrganisationUnitGrandChildren,
			organisationUnitLevel,
			organisationUnitGroup,
            organisationUnitPanel,
			toolMenu,
			tool,
			toolPanel,
            organisationUnit,

			panel,

		// functions
			reset,
			setGui,
			getView,
			validateView,

        // constants
            baseWidth = 444,
            toolWidth = 36,

            accBaseWidth = baseWidth - 2,

            namePropertyUrl = gis.init.namePropertyUrl,
            nameProperty = gis.init.userAccount.settings.keyAnalysisDisplayProperty;

		// stores

		programStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: gis.init.contextPath + '/api/programs.json?fields=id,displayName|rename(name)&paging=false',
				reader: {
					type: 'json',
					root: 'programs'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			},
			sortInfo: {field: 'name', direction: 'ASC'},
			isLoaded: false,
			listeners: {
				load: function() {
					if (!this.isLoaded) {
						this.isLoaded = true;
					}
				}
			}
		});

		stagesByProgramStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: '',
				reader: {
					type: 'json',
					root: 'programStages'
				}
			},
			isLoaded: false,
			loadFn: function(fn) {
				if (Ext.isFunction(fn)) {
					if (this.isLoaded) {
						fn.call();
					}
					else {
						this.load({
							callback: fn
						});
					}
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
		});

		dataElementsByStageStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'isAttribute'],
			data: [],
			sorters: [
                {
                    property: 'isAttribute',
                    direction: 'DESC'
                },
                {
                    property: 'name',
                    direction: 'ASC'
                }
            ]
		});

		// components

            // data element
		program = Ext.create('Ext.form.field.ComboBox', {
			fieldLabel: GIS.i18n.programs,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			fieldLabel: 'Program',
			labelAlign: 'top',
			labelCls: 'gis-form-item-label-top',
			labelSeparator: '',
			emptyText: 'Select program',
			forceSelection: true,
			queryMode: 'remote',
			columnWidth: 0.5,
			style: 'margin:1px 1px 1px 0',
			storage: {},
			store: programStore,
            getRecord: function() {
                return this.getValue ? {
                    id: this.getValue(),
                    name: this.getRawValue()
                } : null;
            },
			listeners: {
				select: function(cb) {
					onProgramSelect(cb.getValue());
				}
			}
		});

		onProgramSelect = function(programId, layout) {
            var load;

            programId = layout ? layout.program.id : programId;
			stage.clearValue();

			dataElementsByStageStore.removeAll();
			dataElementSelected.removeAll();

            load = function(stages) {
                stage.enable();
                stage.clearValue();

                stagesByProgramStore.removeAll();
                stagesByProgramStore.loadData(stages);

                //ns.app.aggregateLayoutWindow.resetData();
				//ns.app.queryLayoutWindow.resetData();

                stageId = (layout ? layout.programStage.id : null) || (stages.length === 1 ? stages[0].id : null);

                if (stageId) {
                    stage.setValue(stageId);
                    onStageSelect(stageId, layout);
                }
            };

            if (stageStorage.hasOwnProperty(programId)) {
                load(stageStorage[programId]);
            }
            else {
                Ext.Ajax.request({
                    url: gis.init.contextPath + '/api/programs.json?filter=id:eq:' + programId + '&fields=programStages[id,displayName|rename(name)],programTrackedEntityAttributes[trackedEntityAttribute[id,displayName|rename(name),valueType,optionSet[id,displayName|rename(name)]]]&paging=false',
                    success: function(r) {
                        var program = Ext.decode(r.responseText).programs[0],
                            stages,
                            attributes,
                            stageId;

                        if (!program) {
                            return;
                        }

                        stages = program.programStages;
                        attributes = Ext.Array.pluck(program.programTrackedEntityAttributes, 'trackedEntityAttribute');

                        // mark as attribute
                        for (var i = 0; i < attributes.length; i++) {
                            attributes[i].isAttribute = true;
                        }

                        // attributes cache
                        if (Ext.isArray(attributes) && attributes.length) {
                            attributeStorage[programId] = attributes;
                        }

                        if (Ext.isArray(stages) && stages.length) {

                            // stages cache
                            stageStorage[programId] = stages;

                            load(stages);
                        }
                    }
                });
            }
		};

		stage = Ext.create('Ext.form.field.ComboBox', {
			fieldLabel: GIS.i18n.indicator,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			fieldLabel: 'Stage',
			labelAlign: 'top',
			labelCls: 'gis-form-item-label-top',
			labelSeparator: '',
			emptyText: 'Select stage',
			queryMode: 'local',
			forceSelection: true,
			columnWidth: 0.5,
			style: 'margin:1px 0 1px 0',
			disabled: true,
			listConfig: {loadMask: false},
			store: stagesByProgramStore,
            getRecord: function() {
                return this.getValue() ? {
                    id: this.getValue(),
                    name: this.getRawValue()
                } : null;
            },
			listeners: {
				select: function(cb) {
					onStageSelect(cb.getValue());
				}
			}
		});

		onStageSelect = function(stageId, layout) {
            if (!layout) {
				dataElementSelected.removeAll();
			}

			loadDataElements(stageId, layout);
		};

		loadDataElements = function(stageId, layout) {
			var programId = layout ? layout.program.id : (program.getValue() || null),
                load;

            stageId = stageId || layout.programStage.id;

			load = function(dataElements) {
                var attributes = attributeStorage[programId],
                    data = Ext.Array.clean([].concat(attributes || [], dataElements || []));

				dataElementsByStageStore.loadData(data);

                if (layout) {
                    var dataDimensions = gis.util.layout.getDataDimensionsFromLayout(layout),
                        records = [];

                    for (var i = 0, dim, row; i < dataDimensions.length; i++) {
                        dim = dataDimensions[i];
                        row = dataElementsByStageStore.getById(dim.dimension);

                        if (row) {
                            records.push(Ext.applyIf(dim, row.data));
                        }
                    }

                    selectDataElements(records, layout);
                }
			};

            // data elements
            if (dataElementStorage.hasOwnProperty(stageId)) {
                load(dataElementStorage[stageId]);
            }
            else {
                Ext.Ajax.request({
                    url: gis.init.contextPath + '/api/programStages.json?filter=id:eq:' + stageId + '&fields=programStageDataElements[dataElement[id,' + gis.init.namePropertyUrl + ',type,optionSet[id,displayName|rename(name)]]]',
                    success: function(r) {
                        var objects = Ext.decode(r.responseText).programStages,
                            dataElements;

                        if (!objects.length) {
                            load();
                            return;
                        }

                        dataElements = Ext.Array.pluck(objects[0].programStageDataElements, 'dataElement');

                        // data elements cache
                        dataElementStorage[stageId] = dataElements;

                        load(dataElements);
                    }
                });
            }
		};

		dataElementAvailable = Ext.create('Ext.ux.form.MultiSelect', {
			cls: 'ns-toolbar-multiselect-left',
			width: accBaseWidth - 4,
            height: 118,
			valueField: 'id',
			displayField: 'name',
            style: 'margin-bottom:1px',
			store: dataElementsByStageStore,
			tbar: [
				{
					xtype: 'label',
                    text: 'Available data items',
					cls: 'ns-toolbar-multiselect-left-label'
				},
				'->',
				{
					xtype: 'button',
					icon: 'images/arrowdown.png',
					width: 22,
					height: 22,
					handler: function() {
                        if (dataElementAvailable.getValue().length) {
                            selectDataElements(dataElementAvailable.getValue());
                        }
					}
				},
				{
					xtype: 'button',
					icon: 'images/arrowdowndouble.png',
					width: 22,
					height: 22,
					handler: function() {
                        if (dataElementsByStageStore.getRange().length) {
                            selectDataElements(dataElementsByStageStore.getRange());
                        }
					}
				}
			],
			listeners: {
				afterrender: function(ms) {
					this.boundList.on('itemdblclick', function() {
                        if (ms.getValue().length) {
                            selectDataElements(ms.getValue());
                        }
					});
				}
			}
		});

        dataElementSelected = Ext.create('Ext.panel.Panel', {
			width: accBaseWidth - 4,
            height: 204,
            bodyStyle: 'padding:2px 0 1px 3px; overflow-y: scroll',
            tbar: {
                height: 27,
                items: [
					{
						xtype: 'label',
						text: 'Selected data items',
						style: 'padding-left:6px; color:#222',
						cls: 'ns-toolbar-multiselect-left-label'
					},
					'->',
					{
						xtype: 'button',
						icon: 'images/arrowupdouble.png',
						width: 22,
						height: 22,
						handler: function() {
							dataElementSelected.removeAllDataElements();
						}
					}
				]
            },
            getChildIndex: function(child) {
				var items = this.items.items;

				for (var i = 0; i < items.length; i++) {
					if (items[i].id === child.id) {
						return i;
					}
				}

				return items.length;
			},
			hasDataElement: function(dataElementId) {
				var hasDataElement = false;

				this.items.each(function(item) {
					if (item.dataElement.id === dataElementId) {
						hasDataElement = true;
					}
				});

				return hasDataElement;
			},
			removeAllDataElements: function() {
				var items = this.items.items,
					len = items.length;

				for (var i = 0; i < len; i++) {
					items[0].removeDataElement();
				}
			}
        });

        addUxFromDataElement = function(element, index) {
			var getUxType,
				ux;

            element.type = element.type || element.valueType;

			index = index || dataElementSelected.items.items.length;

			getUxType = function(element) {
				if (Ext.isObject(element.optionSet) && Ext.isString(element.optionSet.id)) {
					return 'Ext.ux.panel.OrganisationUnitGroupSetContainer';
				}

				if (element.type === 'int' || element.type === 'number') {
					return 'Ext.ux.panel.DataElementIntegerContainer';
				}

				if (element.type === 'string') {
					return 'Ext.ux.panel.DataElementStringContainer';
				}

				if (element.type === 'date') {
					return 'Ext.ux.panel.DataElementDateContainer';
				}

				if (element.type === 'bool' || element.type === 'trueOnly') {
					return 'Ext.ux.panel.DataElementBooleanContainer';
				}

				return 'Ext.ux.panel.DataElementIntegerContainer';
			};

			// add
			ux = dataElementSelected.insert(index, Ext.create(getUxType(element), {
				dataElement: element
			}));

			ux.removeDataElement = function() {
				dataElementSelected.remove(ux);

				if (!dataElementSelected.hasDataElement(element.id)) {
					dataElementsByStageStore.add(element);
					dataElementsByStageStore.sort();

                    //ns.app.aggregateLayoutWindow.removeDimension(element.id);
                    //ns.app.queryLayoutWindow.removeDimension(element.id);
				}
			};

			ux.duplicateDataElement = function() {
				var index = dataElementSelected.getChildIndex(ux) + 1;
				addUxFromDataElement(element, index);
			};

			dataElementsByStageStore.removeAt(dataElementsByStageStore.findExact('id', element.id));

            return ux;
		};

        selectDataElements = function(items) {
            var dataElements = [];

			// data element objects
            for (var i = 0, item; i < items.length; i++) {
				item = items[i];

                if (Ext.isString(item)) {
                    dataElements.push(dataElementsByStageStore.getAt(dataElementsByStageStore.findExact('id', item)).data);
                }
                else if (Ext.isObject(item)) {
                    if (item.data) {
                        dataElements.push(item.data);
                    }
                    else {
                        dataElements.push(item);
                    }
                }
            }

			// panel, store
            for (var i = 0, element, ux; i < dataElements.length; i++) {
				element = dataElements[i];

				addUxFromDataElement(element);
			}
        };

        dataElement = Ext.create('Ext.panel.Panel', {
            title: '<div class="gis-panel-title-data">Data</div>',
            bodyStyle: 'padding:1px',
            hideCollapseTool: true,
            items: [
                {
					layout: 'column',
                    bodyStyle: 'border:0 none',
					style: 'margin-top:2px',
					items: [
						program,
						stage
					]
				},
                dataElementAvailable,
                dataElementSelected
            ]
        });

            // date

        onDateFieldRender = function(c) {
            $('#' + c.inputEl.id).calendarsPicker({
                calendar: gis.init.calendar,
                dateFormat: gis.init.systemInfo.dateFormat
            });

            Ext.get(c.id).setStyle('z-index', 100000);
        };

        startDate = Ext.create('Ext.form.field.Text', {
			fieldLabel: 'Start date',
			labelAlign: 'top',
			labelCls: 'gis-form-item-label-top',
			labelSeparator: '',
            columnWidth: 0.5,
            height: 41,
            value: gis.init.calendar.formatDate(gis.init.systemInfo.dateFormat, gis.init.calendar.today().add(-3, 'm')),
            listeners: {
                render: function(c) {
                    onDateFieldRender(c);
                }
            }
        });

        endDate = Ext.create('Ext.form.field.Text', {
			fieldLabel: 'End date',
			labelAlign: 'top',
			labelCls: 'gis-form-item-label-top',
			labelSeparator: '',
            columnWidth: 0.5,
            height: 41,
            style: 'margin-left: 1px',
            value: gis.init.calendar.formatDate(gis.init.systemInfo.dateFormat, gis.init.calendar.today()),
            listeners: {
                render: function(c) {
                    onDateFieldRender(c);
                }
            }
        });

        period = Ext.create('Ext.panel.Panel', {
            title: '<div class="gis-panel-title-period">Periods</div>',
            bodyStyle: 'padding:4px 1px 2px',
            hideCollapseTool: true,
            layout: 'column',
            width: accBaseWidth,
            items: [
                startDate,
                endDate
            ]
        });

            // organisation unit
		treePanel = Ext.create('Ext.tree.Panel', {
			cls: 'gis-tree',
			width: accBaseWidth - 4,
			height: 333,
            bodyStyle: 'border:0 none',
			style: 'border-top: 1px solid #ddd; padding-top: 1px',
			displayField: 'name',
			rootVisible: false,
			autoScroll: true,
			multiSelect: true,
			rendered: false,
			reset: function() {
				var rootNode = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
				this.collapseAll();
				this.expandPath(rootNode.getPath());
				this.getSelectionModel().select(rootNode);
			},
			selectRootIf: function() {
				if (this.getSelectionModel().getSelection().length < 1) {
					var node = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
					if (this.rendered) {
						this.getSelectionModel().select(node);
					}
					return node;
				}
			},
			isPending: false,
			recordsToSelect: [],
			recordsToRestore: [],
			multipleSelectIf: function(map, doUpdate) {
                this.recordsToSelect = Ext.Array.clean(this.recordsToSelect);

				if (this.recordsToSelect.length === gis.util.object.getLength(map)) {
					this.getSelectionModel().select(this.recordsToSelect);
					this.recordsToSelect = [];
					this.isPending = false;

					if (doUpdate) {
						update();
					}
				}
			},
			multipleExpand: function(id, map, doUpdate) {
				var that = this,
					rootId = gis.conf.finals.root.id,
					path = map[id];

				if (path.substr(0, rootId.length + 1) !== ('/' + rootId)) {
					path = '/' + rootId + path;
				}

				that.expandPath(path, 'id', '/', function() {
					record = Ext.clone(that.getRootNode().findChild('id', id, true));
					that.recordsToSelect.push(record);
					that.multipleSelectIf(map, doUpdate);
				});
			},
            select: function(url, params) {
                if (!params) {
                    params = {};
                }
                Ext.Ajax.request({
                    url: url,
                    method: 'GET',
                    params: params,
                    scope: this,
                    success: function(r) {
                        var a = Ext.decode(r.responseText).organisationUnits;
                        this.numberOfRecords = a.length;
                        for (var i = 0; i < a.length; i++) {
                            this.multipleExpand(a[i].id, a[i].path);
                        }
                    }
                });
            },
			getParentGraphMap: function() {
				var selection = this.getSelectionModel().getSelection(),
					map = {};

				if (Ext.isArray(selection) && selection.length) {
					for (var i = 0, pathArray, key; i < selection.length; i++) {
						pathArray = selection[i].getPath().split('/');
						map[pathArray.pop()] = pathArray.join('/');
					}
				}

				return map;
			},
			selectGraphMap: function(map, update) {
				if (!gis.util.object.getLength(map)) {
					return;
				}

				this.isPending = true;

				for (var key in map) {
					if (map.hasOwnProperty(key)) {
						treePanel.multipleExpand(key, map, update);
					}
				}
			},
            store: Ext.create('Ext.data.TreeStore', {
				fields: ['id', 'name', 'hasChildren'],
				proxy: {
					type: 'rest',
					format: 'json',
					noCache: false,
					extraParams: {
						fields: 'children[id,' + gis.init.namePropertyUrl + ',children::isNotEmpty|rename(hasChildren)&paging=false'
					},
					url: gis.init.contextPath + '/api/organisationUnits',
					reader: {
						type: 'json',
						root: 'children'
					},
					sortParam: false
				},
				sorters: [{
					property: 'name',
					direction: 'ASC'
				}],
				root: {
					id: gis.conf.finals.root.id,
					expanded: true,
					children: gis.init.rootNodes
				},
				listeners: {
					load: function(store, node, records) {
						Ext.Array.each(records, function(record) {
                            if (Ext.isBoolean(record.data.hasChildren)) {
                                record.set('leaf', !record.data.hasChildren);
                            }
                        });
					}
				}
			}),
			xable: function(values) {
				for (var i = 0; i < values.length; i++) {
					if (!!values[i]) {
						this.disable();
						return;
					}
				}

				this.enable();
			},
			getDimension: function() {
				var r = treePanel.getSelectionModel().getSelection(),
					config = {
						dimension: gis.conf.finals.dimension.organisationUnit.objectName,
						items: []
					};

				if (toolMenu.menuValue === 'orgunit') {
					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue() || userOrganisationUnitGrandChildren.getValue()) {
						if (userOrganisationUnit.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT',
								name: ''
							});
						}
						if (userOrganisationUnitChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_CHILDREN',
								name: ''
							});
						}
						if (userOrganisationUnitGrandChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_GRANDCHILDREN',
								name: ''
							});
						}
					}
					else {
						for (var i = 0; i < r.length; i++) {
							config.items.push({id: r[i].data.id});
						}
					}
				}
				else if (toolMenu.menuValue === 'level') {
					var levels = organisationUnitLevel.getValue();

					for (var i = 0; i < levels.length; i++) {
						config.items.push({
							id: 'LEVEL-' + levels[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}
				else if (toolMenu.menuValue === 'group') {
					var groupIds = organisationUnitGroup.getValue();

					for (var i = 0; i < groupIds.length; i++) {
						config.items.push({
							id: 'OU_GROUP-' + groupIds[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}

				return config.items.length ? config : null;
			},
			listeners: {
				beforeitemexpand: function() {
					var rts = treePanel.recordsToSelect;

					if (!treePanel.isPending) {
						treePanel.recordsToRestore = treePanel.getSelectionModel().getSelection();
					}
				},
				itemexpand: function() {
					if (!treePanel.isPending && treePanel.recordsToRestore.length) {
						treePanel.getSelectionModel().select(treePanel.recordsToRestore);
						treePanel.recordsToRestore = [];
					}
				},
				render: function() {
					this.rendered = true;
				},
				afterrender: function() {
					this.getSelectionModel().select(0);
				},
				itemcontextmenu: function(v, r, h, i, e) {
					v.getSelectionModel().select(r, false);

					if (v.menu) {
						v.menu.destroy();
					}
					v.menu = Ext.create('Ext.menu.Menu', {
						showSeparator: false,
						shadow: false
					});
					if (!r.data.leaf) {
						v.menu.add({
							text: GIS.i18n.select_sub_units,
							icon: 'images/node-select-child.png',
							handler: function() {
								r.expand(false, function() {
									v.getSelectionModel().select(r.childNodes, true);
									v.getSelectionModel().deselect(r);
								});
							}
						});
					}
					else {
						return;
					}

					v.menu.showAt(e.xy);
				}
			}
		});

		userOrganisationUnit = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.25,
			style: 'padding-top: 2px; padding-left: 5px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: GIS.i18n.user_organisation_unit,
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnitChildren.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.26,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: GIS.i18n.user_sub_units,
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitGrandChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.4,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: GIS.i18n.user_sub_x2_units,
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitChildren.getValue()]);
			}
		});

		organisationUnitLevel = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: accBaseWidth - toolWidth - 1 - 4,
			valueField: 'level',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_levels,
			editable: false,
			store: {
				fields: ['id', 'name', 'level'],
				data: gis.init.organisationUnitLevels
			}
		});

		organisationUnitGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: accBaseWidth - toolWidth - 1 - 4,
			valueField: 'id',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_groups,
			editable: false,
			store: gis.store.organisationUnitGroup
		});

        organisationUnitPanel = Ext.create('Ext.panel.Panel', {
			width: accBaseWidth - toolWidth - 2,
            layout: 'column',
            bodyStyle: 'border:0 none',
            items: [
                userOrganisationUnit,
                userOrganisationUnitChildren,
                userOrganisationUnitGrandChildren,
                organisationUnitLevel,
                organisationUnitGroup
            ]
        });

		toolMenu = Ext.create('Ext.menu.Menu', {
			shadow: false,
			showSeparator: false,
			menuValue: 'level',
			clickHandler: function(param) {
				param = param || this.menuValue;

				var items = this.items.items;
				this.menuValue = param;

				// Menu item icon cls
				for (var i = 0; i < items.length; i++) {
					if (items[i].setIconCls) {
						if (items[i].param === param) {
							items[i].setIconCls('gis-menu-item-selected');
						}
						else {
							items[i].setIconCls('gis-menu-item-unselected');
						}
					}
				}

				// Gui
				if (param === 'orgunit') {
					userOrganisationUnit.show();
					userOrganisationUnitChildren.show();
					userOrganisationUnitGrandChildren.show();
					organisationUnitLevel.hide();
					organisationUnitGroup.hide();

					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue()) {
						treePanel.disable();
					}
				}
				else if (param === 'level') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.show();
					organisationUnitGroup.hide();
					treePanel.enable();
				}
				else if (param === 'group') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.hide();
					organisationUnitGroup.show();
					treePanel.enable();
				}
			},
			items: [
				{
					xtype: 'label',
					text: 'Selection mode',
					style: 'padding:7px 5px 5px 7px; font-weight:bold; border:0 none'
				},
				{
					text: GIS.i18n.select_organisation_units + '&nbsp;&nbsp;',
					param: 'orgunit'
				},
				{
					text: 'Select levels' + '&nbsp;&nbsp;',
					param: 'level'
				},
				{
					text: 'Select groups' + '&nbsp;&nbsp;',
					param: 'group'
				}
			],
			listeners: {
				afterrender: function() {
					this.getEl().addCls('gis-btn-menu');
				},
				click: function(menu, item) {
					this.clickHandler(item.param);
				}
			}
		});

		tool = Ext.create('Ext.button.Button', {
			cls: 'gis-button-organisationunitselection',
			iconCls: 'gis-button-icon-gear',
			width: toolWidth,
			height: 24,
			menu: toolMenu
		});

		toolPanel = Ext.create('Ext.panel.Panel', {
			width: toolWidth,
			bodyStyle: 'border:0 none; text-align:right',
			style: 'margin-right:1px',
			items: tool
		});

        organisationUnit = Ext.create('Ext.panel.Panel', {
            title: '<div class="gis-panel-title-organisationunit">' + GIS.i18n.organisation_units + '</div>',
            cls: 'gis-accordion-last',
            bodyStyle: 'padding:1px',
            hideCollapseTool: true,
            items: [
                {
                    layout: 'column',
                    width: accBaseWidth,
                    bodyStyle: 'border:0 none',
                    style: 'padding-bottom:1px',
                    items: [
                        toolPanel,
                        organisationUnitPanel
                    ]
                },
                treePanel
            ],
			listeners: {
				render: function() {
                    toolMenu.clickHandler();
                }
            }
        });

            // accordion
        accordionBody = Ext.create('Ext.panel.Panel', {
			layout: 'accordion',
			activeOnTop: true,
			cls: 'gis-accordion',
			bodyStyle: 'border:0 none',
			height: 450,
			items: [
                dataElement,
                period,
                organisationUnit
            ],
            listeners: {
                afterrender: function() { // nasty workaround
                    organisationUnit.expand();
                    period.expand();
                    dataElement.expand();
                }
            }
		});

		// functions

		reset = function(skipTree) {

			// Item
			layer.item.setValue(false);

			if (!layer.window.isRendered) {
				layer.core.view = null;
				return;
			}

			// Components
            program.clearValue();
            stage.clearValue();

            dataElementsByStageStore.removeAll();
            dataElementSelected.removeAll();

            startDate.reset();
            endDate.reset();

			toolMenu.clickHandler(toolMenu.menuValue);

			if (!skipTree) {
				treePanel.reset();
			}

			userOrganisationUnit.setValue(false);
			userOrganisationUnitChildren.setValue(false);
			userOrganisationUnitGrandChildren.setValue(false);

			organisationUnitLevel.clearValue();
			organisationUnitGroup.clearValue();
		};

		setGui = function(view) { //todo
			var ouDim = view.rows[0],
				isOu = false,
				isOuc = false,
				isOugc = false,
				levels = [],
				groups = [],
				setWidgetGui,
				setLayerGui;

			setWidgetGui = function() {

				// Components
				if (!layer.window.isRendered) {
					return;
				}

				reset(true);

				// Organisation units
				for (var i = 0, item; i < ouDim.items.length; i++) {
					item = ouDim.items[i];

					if (item.id === 'USER_ORGUNIT') {
						isOu = true;
					}
					else if (item.id === 'USER_ORGUNIT_CHILDREN') {
						isOuc = true;
					}
					else if (item.id === 'USER_ORGUNIT_GRANDCHILDREN') {
						isOugc = true;
					}
					else if (item.id.substr(0,5) === 'LEVEL') {
						levels.push(parseInt(item.id.split('-')[1]));
					}
					else if (item.id.substr(0,8) === 'OU_GROUP') {
						groups.push(parseInt(item.id.split('-')[1]));
					}
				}

				if (levels.length) {
					toolMenu.clickHandler('level');
					organisationUnitLevel.setValue(levels);
				}
				else if (groups.length) {
					toolMenu.clickHandler('group');
					organisationUnitGroup.setValue(groups);
				}
				else {
					toolMenu.clickHandler('orgunit');
					userOrganisationUnit.setValue(isOu);
					userOrganisationUnitChildren.setValue(isOuc);
					userOrganisationUnitGrandChildren.setValue(isOugc);
				}

				treePanel.selectGraphMap(view.parentGraphMap);
			}();

			setLayerGui = function() {

				// Layer item
				layer.item.setValue(!view.hidden, view.opacity);

				// Layer menu
				layer.menu.enableItems();
			}();
		};

		getView = function(config) {
			var view = {};

            view.program = program.getRecord();
            view.stage = stage.getRecord();

            //view.startDate = startDate.getSubmitValue();
            //view.endDate = endDate.getSubmitValue();
            view.startDate = startDate.getValue();
            view.endDate = endDate.getValue();

            view.dataElements = [];

            for (var i = 0, panel; i < dataElementSelected.items.items.length; i++) {
                panel = dataElementSelected.items.items[i];

                view.dataElements.push(panel.getRecord());
            }

            view.organisationUnits = treePanel.getDimension().items;

			return view;
		};

		validateView = function(view) {
			if (!(Ext.isArray(view.rows) && view.rows.length && Ext.isString(view.rows[0].dimension) && Ext.isArray(view.rows[0].items) && view.rows[0].items.length)) {
				GIS.logg.push([view.rows, layer.id + '.rows: dimension array']);
				alert('No organisation units selected');
				return false;
			}

			return view;
		};

		panel = Ext.create('Ext.panel.Panel', {
			map: layer.map,
			layer: layer,
			menu: layer.menu,

			reset: reset,
			setGui: setGui,
			getView: getView,
			getParentGraphMap: function() {
				return treePanel.getParentGraphMap();
			},

			cls: 'gis-form-widget',
			border: false,
			items: [
                accordionBody
			]
		});

		return panel;
	};

	GIS.app.LayerWidgetFacility = function(layer) {

		var infrastructuralDataElementValuesStore,

			groupSet,
            icons,

			treePanel,
			userOrganisationUnit,
			userOrganisationUnitChildren,
			userOrganisationUnitGrandChildren,
			organisationUnitLevel,
			organisationUnitGroup,
			toolMenu,
			tool,
			toolPanel,
            organisationUnit,

            labelPanel,
			areaRadius,
            options,

			reset,
			setGui,
			getView,
			validateView,

			accordionBody,
            accordion,

            accordionPanels = [];

		// Stores

		infrastructuralDataElementValuesStore = Ext.create('Ext.data.Store', {
			fields: ['name', 'value'],
            sorters: [{
                property: 'name',
                direction: 'ASC'
            }]
		});

		// Components

		groupSet = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: 'Group set',
            editable: false,
            valueField: 'id',
            displayField: 'name',
            emptyText: 'Organisation unit group set',
            mode: 'remote',
            forceSelection: true,
            width: gis.conf.layout.widget.item_width,
            labelWidth: gis.conf.layout.widget.itemlabel_width,
            currentValue: false,
            store: gis.store.groupSets
        });

        icons = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">' + 'Organisation unit group icons' + '</div>',
			hideCollapseTool: true,
            items: [
                groupSet
            ],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });


		treePanel = Ext.create('Ext.tree.Panel', {
			cls: 'gis-tree',
			height: 307,
			style: 'border-top: 1px solid #ddd; padding-top: 1px',
			displayField: 'name',
			width: gis.conf.layout.widget.item_width,
			rootVisible: false,
			autoScroll: true,
			multiSelect: true,
			rendered: false,
			reset: function() {
				var rootNode = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
				this.collapseAll();
				this.expandPath(rootNode.getPath());
				this.getSelectionModel().select(rootNode);
			},
			selectRootIf: function() {
				if (this.getSelectionModel().getSelection().length < 1) {
					var node = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
					if (this.rendered) {
						this.getSelectionModel().select(node);
					}
					return node;
				}
			},
			isPending: false,
			recordsToSelect: [],
			recordsToRestore: [],
			multipleSelectIf: function(map, doUpdate) {
                this.recordsToSelect = Ext.Array.clean(this.recordsToSelect);
                
				if (this.recordsToSelect.length === gis.util.object.getLength(map)) {
					this.getSelectionModel().select(this.recordsToSelect);
					this.recordsToSelect = [];
					this.isPending = false;

					if (doUpdate) {
						update();
					}
				}
			},
			multipleExpand: function(id, map, doUpdate) {
				var that = this,
					rootId = gis.conf.finals.root.id,
					path = map[id];

				if (path.substr(0, rootId.length + 1) !== ('/' + rootId)) {
					path = '/' + rootId + path;
				}

				that.expandPath(path, 'id', '/', function() {
					record = Ext.clone(that.getRootNode().findChild('id', id, true));
					that.recordsToSelect.push(record);
					that.multipleSelectIf(map, doUpdate);
				});
			},
            select: function(url, params) {
                if (!params) {
                    params = {};
                }
                Ext.Ajax.request({
                    url: url,
                    method: 'GET',
                    params: params,
                    scope: this,
                    success: function(r) {
                        var a = Ext.decode(r.responseText).organisationUnits;
                        this.numberOfRecords = a.length;
                        for (var i = 0; i < a.length; i++) {
                            this.multipleExpand(a[i].id, a[i].path);
                        }
                    }
                });
            },
			getParentGraphMap: function() {
				var selection = this.getSelectionModel().getSelection(),
					map = {};

				if (Ext.isArray(selection) && selection.length) {
					for (var i = 0, pathArray, key; i < selection.length; i++) {
						pathArray = selection[i].getPath().split('/');
						map[pathArray.pop()] = pathArray.join('/');
					}
				}

				return map;
			},
			selectGraphMap: function(map, update) {
				if (!gis.util.object.getLength(map)) {
					return;
				}

				this.isPending = true;

				for (var key in map) {
					if (map.hasOwnProperty(key)) {
						treePanel.multipleExpand(key, map, update);
					}
				}
			},
            store: Ext.create('Ext.data.TreeStore', {
				fields: ['id', 'name', 'hasChildren'],
				proxy: {
					type: 'rest',
					format: 'json',
					noCache: false,
					extraParams: {
						fields: 'children[id,' + gis.init.namePropertyUrl + ',children::isNotEmpty|rename(hasChildren)&paging=false'
					},
					url: gis.init.contextPath + '/api/organisationUnits',
					reader: {
						type: 'json',
						root: 'children'
					},
					sortParam: false
				},
				sorters: [{
					property: 'name',
					direction: 'ASC'
				}],
				root: {
					id: gis.conf.finals.root.id,
					expanded: true,
					children: gis.init.rootNodes
				},
				listeners: {
					load: function(store, node, records) {
						Ext.Array.each(records, function(record) {
                            if (Ext.isBoolean(record.data.hasChildren)) {
                                record.set('leaf', !record.data.hasChildren);
                            }
                        });
					}
				}
			}),
			xable: function(values) {
				for (var i = 0; i < values.length; i++) {
					if (!!values[i]) {
						this.disable();
						return;
					}
				}

				this.enable();
			},
			getDimension: function() {
				var r = treePanel.getSelectionModel().getSelection(),
					config = {
						dimension: gis.conf.finals.dimension.organisationUnit.objectName,
						items: []
					};

				if (toolMenu.menuValue === 'orgunit') {
					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue() || userOrganisationUnitGrandChildren.getValue()) {
						if (userOrganisationUnit.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT',
								name: ''
							});
						}
						if (userOrganisationUnitChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_CHILDREN',
								name: ''
							});
						}
						if (userOrganisationUnitGrandChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_GRANDCHILDREN',
								name: ''
							});
						}
					}
					else {
						for (var i = 0; i < r.length; i++) {
							config.items.push({id: r[i].data.id});
						}
					}
				}
				else if (toolMenu.menuValue === 'level') {
					var levels = organisationUnitLevel.getValue();

					for (var i = 0; i < levels.length; i++) {
						config.items.push({
							id: 'LEVEL-' + levels[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}
				else if (toolMenu.menuValue === 'group') {
					var groupIds = organisationUnitGroup.getValue();

					for (var i = 0; i < groupIds.length; i++) {
						config.items.push({
							id: 'OU_GROUP-' + groupIds[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}

				return config.items.length ? config : null;
			},
			listeners: {
				beforeitemexpand: function() {
					var rts = treePanel.recordsToSelect;

					if (!treePanel.isPending) {
						treePanel.recordsToRestore = treePanel.getSelectionModel().getSelection();
					}
				},
				itemexpand: function() {
					if (!treePanel.isPending && treePanel.recordsToRestore.length) {
						treePanel.getSelectionModel().select(treePanel.recordsToRestore);
						treePanel.recordsToRestore = [];
					}
				},
				render: function() {
					this.rendered = true;
				},
				afterrender: function() {
					this.getSelectionModel().select(0);
				},
				itemcontextmenu: function(v, r, h, i, e) {
					v.getSelectionModel().select(r, false);

					if (v.menu) {
						v.menu.destroy();
					}
					v.menu = Ext.create('Ext.menu.Menu', {
						showSeparator: false,
						shadow: false
					});
					if (!r.data.leaf) {
						v.menu.add({
							text: GIS.i18n.select_sub_units,
							icon: 'images/node-select-child.png',
							handler: function() {
								r.expand(false, function() {
									v.getSelectionModel().select(r.childNodes, true);
									v.getSelectionModel().deselect(r);
								});
							}
						});
					}
					else {
						return;
					}

					v.menu.showAt(e.xy);
				}
			}
		});

		userOrganisationUnit = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.3,
			style: 'padding-top: 2px; padding-left: 3px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'User OU',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnitChildren.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.33,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'Sub-units',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitGrandChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.34,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'Sub-x2-units',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitChildren.getValue()]);
			}
		});

		organisationUnitLevel = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: gis.conf.layout.widget.item_width - 37,
			valueField: 'level',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_levels,
			editable: false,
			store: {
				fields: ['id', 'name', 'level'],
				data: gis.init.organisationUnitLevels
			}
		});

		organisationUnitGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: gis.conf.layout.widget.item_width - 37,
			valueField: 'id',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_groups,
			editable: false,
			store: gis.store.organisationUnitGroup
		});

		toolMenu = Ext.create('Ext.menu.Menu', {
			shadow: false,
			showSeparator: false,
			menuValue: 'level',
			clickHandler: function(param) {
				if (!param) {
					return;
				}

				var items = this.items.items;
				this.menuValue = param;

				// Menu item icon cls
				for (var i = 0; i < items.length; i++) {
					if (items[i].setIconCls) {
						if (items[i].param === param) {
							items[i].setIconCls('gis-menu-item-selected');
						}
						else {
							items[i].setIconCls('gis-menu-item-unselected');
						}
					}
				}

				// Gui
				if (param === 'orgunit') {
					userOrganisationUnit.show();
					userOrganisationUnitChildren.show();
					userOrganisationUnitGrandChildren.show();
					organisationUnitLevel.hide();
					organisationUnitGroup.hide();

					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue()) {
						treePanel.disable();
					}
				}
				else if (param === 'level') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.show();
					organisationUnitGroup.hide();
					treePanel.enable();
				}
				else if (param === 'group') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.hide();
					organisationUnitGroup.show();
					treePanel.enable();
				}
			},
			items: [
				{
					xtype: 'label',
					text: 'Selection mode',
					style: 'padding:7px 5px 5px 7px; font-weight:bold; border:0 none'
				},
				{
					text: GIS.i18n.select_organisation_units + '&nbsp;&nbsp;',
					param: 'orgunit',
					iconCls: 'gis-menu-item-selected'
				},
				{
					text: 'Select levels' + '&nbsp;&nbsp;',
					param: 'level',
					iconCls: 'gis-menu-item-unselected'
				},
				{
					text: 'Select groups' + '&nbsp;&nbsp;',
					param: 'group',
					iconCls: 'gis-menu-item-unselected'
				}
			],
			listeners: {
				afterrender: function() {
					this.getEl().addCls('gis-btn-menu');
				},
				click: function(menu, item) {
					this.clickHandler(item.param);
				}
			}
		});

		tool = Ext.create('Ext.button.Button', {
			cls: 'gis-button-organisationunitselection',
			iconCls: 'gis-button-icon-gear',
			width: 36,
			height: 24,
			menu: toolMenu
		});

		toolPanel = Ext.create('Ext.panel.Panel', {
			width: 36,
			bodyStyle: 'border:0 none; text-align:right',
			style: 'margin-right:1px',
			items: tool
		});

        organisationUnit = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">' + GIS.i18n.organisation_units + '</div>',
			hideCollapseTool: true,
            items: [
                {
                    layout: 'column',
                    bodyStyle: 'border:0 none',
                    style: 'padding-bottom:1px',
                    items: [
                        toolPanel,
                        {
                            layout: 'column',
                            bodyStyle: 'border:0 none',
                            items: [
                                userOrganisationUnit,
                                userOrganisationUnitChildren,
                                userOrganisationUnitGrandChildren,
                                organisationUnitLevel,
                                organisationUnitGroup
                            ]
                        }
                    ]
                },
                treePanel
            ],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });


        labelPanel = Ext.create('Ext.ux.panel.LabelPanel');

		areaRadius = Ext.create('Ext.ux.panel.CheckTextNumber', {
			width: gis.conf.layout.widget.item_width,
			checkboxBoxLabel: GIS.i18n.show_circular_area + ':'
		});

        options = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">' + 'Options' + '</div>',
			hideCollapseTool: true,
            items: [
                labelPanel,
                {
                    xtype: 'container',
                    height: 1
                },
                areaRadius
            ],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });

		// Functions

		reset = function(skipTree) {

			// Item
			layer.item.setValue(false, layer.item.defaultOpacity);

			// Layer
			if (layer.searchWindow) {
				layer.searchWindow.destroy();
				layer.searchWindow = null;
			}
			if (layer.filterWindow) {
				layer.filterWindow.destroy();
				layer.filterWindow = null;
			}

			if (layer.circleLayer & !skipTree) {
				layer.circleLayer.deactivateControls();
				layer.circleLayer = null;
			}

			// Components
			if (!layer.window.isRendered) {
				layer.core.view = null;
				return;
			}

			groupSet.clearValue();

			toolMenu.clickHandler(toolMenu.menuValue);

			if (!skipTree) {
				treePanel.reset();
			}

			userOrganisationUnit.setValue(false);
			userOrganisationUnitChildren.setValue(false);
			userOrganisationUnitGrandChildren.setValue(false);

			organisationUnitLevel.clearValue();
			organisationUnitGroup.clearValue();

			areaRadius.reset();
		};

		setGui = function(view) {
			var ouDim = view.rows[0],
				isOu = false,
				isOuc = false,
				isOugc = false,
				levels = [],
				groups = [],
				setWidgetGui,
				setLayerGui;

			setWidgetGui = function() {

				// Components
				if (!layer.window.isRendered) {
					return;
				}

				reset(true);

				// Group set
				groupSet.store.removeAll();
				groupSet.store.add(view.organisationUnitGroupSet);
				groupSet.setValue(view.organisationUnitGroupSet.id);

				// Organisation units
				for (var i = 0, item; i < ouDim.items.length; i++) {
					item = ouDim.items[i];

					if (item.id === 'USER_ORGUNIT') {
						isOu = true;
					}
					else if (item.id === 'USER_ORGUNIT_CHILDREN') {
						isOuc = true;
					}
					else if (item.id === 'USER_ORGUNIT_GRANDCHILDREN') {
						isOugc = true;
					}
					else if (item.id.substr(0,5) === 'LEVEL') {
						levels.push(parseInt(item.id.split('-')[1]));
					}
					else if (item.id.substr(0,8) === 'OU_GROUP') {
						groups.push(parseInt(item.id.split('-')[1]));
					}
				}

				if (levels.length) {
					toolMenu.clickHandler('level');
					organisationUnitLevel.setValue(levels);
				}
				else if (groups.length) {
					toolMenu.clickHandler('group');
					organisationUnitGroup.setValue(groups);
				}
				else {
					toolMenu.clickHandler('orgunit');
					userOrganisationUnit.setValue(isOu);
					userOrganisationUnitChildren.setValue(isOuc);
					userOrganisationUnitGrandChildren.setValue(isOugc);
				}

				treePanel.selectGraphMap(view.parentGraphMap);

                // labels
                labelPanel.setConfig(view);

				// area radius
				areaRadius.setValue(!!view.areaRadius, !!view.areaRadius ? view.areaRadius : null);
			}();

			setLayerGui = function() {

				// Layer item
				layer.item.setValue(!view.hidden, view.opacity);

				// Layer menu
				layer.menu.enableItems();

				// Update filter window
				if (layer.filterWindow && layer.filterWindow.isVisible()) {
					layer.filterWindow.filter();
				}
			}();
		};

		getView = function(config) {
			var view = {};

			view.layer = layer.id;

			view.rows = [treePanel.getDimension()];

			view.organisationUnitGroupSet = {
				id: groupSet.getValue()
			};

            Ext.apply(view, labelPanel.getConfig());

			view.areaRadius = areaRadius.getValue() ? areaRadius.getNumber() : null;

			view.opacity = layer.item.getOpacity();

			return validateView(view);
		};

		validateView = function(view) {
			if (!(Ext.isObject(view.organisationUnitGroupSet) && Ext.isString(view.organisationUnitGroupSet.id))) {
				GIS.logg.push([view.organisationUnitGroupSet.id, layer.id + '.organisationUnitGroupSet.id: string']);
				alert(GIS.i18n.no_groupset_selected);
				return false;
			}

			if (!(Ext.isArray(view.rows) && view.rows.length && Ext.isString(view.rows[0].dimension) && Ext.isArray(view.rows[0].items) && view.rows[0].items.length)) {
				GIS.logg.push([view.rows, layer.id + '.rows: dimension array']);
				alert('No organisation units selected');
				return false;
			}

			return view;
		};

        accordionBody = Ext.create('Ext.panel.Panel', {
			layout: 'accordion',
			activeOnTop: true,
			cls: 'ns-accordion',
			bodyStyle: 'border:0 none; margin-bottom:1px',
			height: 414,
			items: function() {
				var panels = [
					icons,
					organisationUnit,
					options
				];

				last = panels[panels.length - 1];
				last.cls = 'ns-accordion-last';

				return panels;
			}(),
            listeners: {
                afterrender: function() { // nasty workaround
                    for (var i = accordionPanels.length - 1; i >= 0; i--) {
                        accordionPanels[i].expand();
                    }
                }
            }
		});

		accordion = Ext.create('Ext.panel.Panel', {
			bodyStyle: 'border-style:none; padding:1px; padding-bottom:0',
			items: accordionBody,
			panels: accordionPanels,

			map: layer.map,
			layer: layer,
			menu: layer.menu,

			reset: reset,
			setGui: setGui,
			getView: getView,
			getParentGraphMap: function() {
				return treePanel.getParentGraphMap();
			},

			infrastructuralDataElementValuesStore: infrastructuralDataElementValuesStore,
			getExpandedPanel: function() {
				for (var i = 0, panel; i < this.panels.length; i++) {
					if (!this.panels[i].collapsed) {
						return this.panels[i];
					}
				}

				return null;
			},
			getFirstPanel: function() {
				return this.panels[0];
			},
			listeners: {
				added: function() {
					layer.accordion = this;
				},
				render: function() {
					toolMenu.clickHandler('level');
				}
			}
		});

		return accordion;
	};

	GIS.app.LayerWidgetBoundary = function(layer) {

		var infrastructuralDataElementValuesStore,

			treePanel,
			userOrganisationUnit,
			userOrganisationUnitChildren,
			userOrganisationUnitGrandChildren,
			organisationUnitLevel,
			organisationUnitGroup,
			toolMenu,
			tool,
			toolPanel,
            organisationUnit,

            labelPanel,
            label,

			reset,
			setGui,
			getView,
			validateView,

            accordionBody,
            accordion,

			accordionPanels = [];

		// Stores

		infrastructuralDataElementValuesStore = Ext.create('Ext.data.Store', {
			fields: ['name', 'value'],
            sorters: [{
                property: 'name',
                direction: 'ASC'
            }]
		});

		// Components

		treePanel = Ext.create('Ext.tree.Panel', {
			cls: 'gis-tree',
			height: 327,
			style: 'border-top: 1px solid #ddd; padding-top: 1px',
			displayField: 'name',
			width: gis.conf.layout.widget.item_width,
			rootVisible: false,
			autoScroll: true,
			multiSelect: true,
			rendered: false,
			reset: function() {
				var rootNode = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
				this.collapseAll();
				this.expandPath(rootNode.getPath());
				this.getSelectionModel().select(rootNode);
			},
			selectRootIf: function() {
				if (this.getSelectionModel().getSelection().length < 1) {
					var node = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
					if (this.rendered) {
						this.getSelectionModel().select(node);
					}
					return node;
				}
			},
			isPending: false,
			recordsToSelect: [],
			recordsToRestore: [],
			multipleSelectIf: function(map, doUpdate) {
                this.recordsToSelect = Ext.Array.clean(this.recordsToSelect);
                
				if (this.recordsToSelect.length === gis.util.object.getLength(map)) {
					this.getSelectionModel().select(this.recordsToSelect);
					this.recordsToSelect = [];
					this.isPending = false;

					if (doUpdate) {
						update();
					}
				}
			},
			multipleExpand: function(id, map, doUpdate) {
				var that = this,
					rootId = gis.conf.finals.root.id,
					path = map[id];

				if (path.substr(0, rootId.length + 1) !== ('/' + rootId)) {
					path = '/' + rootId + path;
				}

				that.expandPath(path, 'id', '/', function() {
					record = Ext.clone(that.getRootNode().findChild('id', id, true));
					that.recordsToSelect.push(record);
					that.multipleSelectIf(map, doUpdate);
				});
			},
            select: function(url, params) {
                if (!params) {
                    params = {};
                }
                Ext.Ajax.request({
                    url: url,
                    method: 'GET',
                    params: params,
                    scope: this,
                    success: function(r) {
                        var a = Ext.decode(r.responseText).organisationUnits;
                        this.numberOfRecords = a.length;
                        for (var i = 0; i < a.length; i++) {
                            this.multipleExpand(a[i].id, a[i].path);
                        }
                    }
                });
            },
			getParentGraphMap: function() {
				var selection = this.getSelectionModel().getSelection(),
					map = {};

				if (Ext.isArray(selection) && selection.length) {
					for (var i = 0, pathArray, key; i < selection.length; i++) {
						pathArray = selection[i].getPath().split('/');
						map[pathArray.pop()] = pathArray.join('/');
					}
				}

				return map;
			},
			selectGraphMap: function(map, update) {
				if (!gis.util.object.getLength(map)) {
					return;
				}

				this.isPending = true;

				for (var key in map) {
					if (map.hasOwnProperty(key)) {
						treePanel.multipleExpand(key, map, update);
					}
				}
			},
            store: Ext.create('Ext.data.TreeStore', {
				fields: ['id', 'name', 'hasChildren'],
				proxy: {
					type: 'rest',
					format: 'json',
					noCache: false,
					extraParams: {
						fields: 'children[id,' + gis.init.namePropertyUrl + ',children::isNotEmpty|rename(hasChildren)&paging=false'
					},
					url: gis.init.contextPath + '/api/organisationUnits',
					reader: {
						type: 'json',
						root: 'children'
					},
					sortParam: false
				},
				sorters: [{
					property: 'name',
					direction: 'ASC'
				}],
				root: {
					id: gis.conf.finals.root.id,
					expanded: true,
					children: gis.init.rootNodes
				},
				listeners: {
					load: function(store, node, records) {
						Ext.Array.each(records, function(record) {
                            if (Ext.isBoolean(record.data.hasChildren)) {
                                record.set('leaf', !record.data.hasChildren);
                            }
                        });
					}
				}
			}),
			xable: function(values) {
				for (var i = 0; i < values.length; i++) {
					if (!!values[i]) {
						this.disable();
						return;
					}
				}

				this.enable();
			},
			getDimension: function() {
				var r = treePanel.getSelectionModel().getSelection(),
					config = {
						dimension: gis.conf.finals.dimension.organisationUnit.objectName,
						items: []
					};

				if (toolMenu.menuValue === 'orgunit') {
					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue() || userOrganisationUnitGrandChildren.getValue()) {
						if (userOrganisationUnit.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT',
								name: ''
							});
						}
						if (userOrganisationUnitChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_CHILDREN',
								name: ''
							});
						}
						if (userOrganisationUnitGrandChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_GRANDCHILDREN',
								name: ''
							});
						}
					}
					else {
						for (var i = 0; i < r.length; i++) {
							config.items.push({id: r[i].data.id});
						}
					}
				}
				else if (toolMenu.menuValue === 'level') {
					var levels = organisationUnitLevel.getValue();

					for (var i = 0; i < levels.length; i++) {
						config.items.push({
							id: 'LEVEL-' + levels[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}
				else if (toolMenu.menuValue === 'group') {
					var groupIds = organisationUnitGroup.getValue();

					for (var i = 0; i < groupIds.length; i++) {
						config.items.push({
							id: 'OU_GROUP-' + groupIds[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}

				return config.items.length ? config : null;
			},
			listeners: {
				beforeitemexpand: function() {
					var rts = treePanel.recordsToSelect;

					if (!treePanel.isPending) {
						treePanel.recordsToRestore = treePanel.getSelectionModel().getSelection();
					}
				},
				itemexpand: function() {
					if (!treePanel.isPending && treePanel.recordsToRestore.length) {
						treePanel.getSelectionModel().select(treePanel.recordsToRestore);
						treePanel.recordsToRestore = [];
					}
				},
				render: function() {
					this.rendered = true;
				},
				afterrender: function() {
					this.getSelectionModel().select(0);
				},
				itemcontextmenu: function(v, r, h, i, e) {
					v.getSelectionModel().select(r, false);

					if (v.menu) {
						v.menu.destroy();
					}
					v.menu = Ext.create('Ext.menu.Menu', {
						showSeparator: false,
						shadow: false
					});
					if (!r.data.leaf) {
						v.menu.add({
							text: GIS.i18n.select_sub_units,
							icon: 'images/node-select-child.png',
							handler: function() {
								r.expand(false, function() {
									v.getSelectionModel().select(r.childNodes, true);
									v.getSelectionModel().deselect(r);
								});
							}
						});
					}
					else {
						return;
					}

					v.menu.showAt(e.xy);
				}
			}
		});

		userOrganisationUnit = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.3,
			style: 'padding-top: 2px; padding-left: 3px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'User OU',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnitChildren.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.33,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'Sub-units',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitGrandChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.34,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'Sub-x2-units',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitChildren.getValue()]);
			}
		});

		organisationUnitLevel = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: gis.conf.layout.widget.item_width - 37,
			valueField: 'level',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_levels,
			editable: false,
			hidden: true,
			store: {
				fields: ['id', 'name', 'level'],
				data: gis.init.organisationUnitLevels
			}
		});

		organisationUnitGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: gis.conf.layout.widget.item_width - 37,
			valueField: 'id',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_groups,
			editable: false,
			hidden: true,
			store: gis.store.organisationUnitGroup
		});

		toolMenu = Ext.create('Ext.menu.Menu', {
			shadow: false,
			showSeparator: false,
			menuValue: 'level',
			clickHandler: function(param) {
				if (!param) {
					return;
				}

				var items = this.items.items;
				this.menuValue = param;

				// Menu item icon cls
				for (var i = 0; i < items.length; i++) {
					if (items[i].setIconCls) {
						if (items[i].param === param) {
							items[i].setIconCls('gis-menu-item-selected');
						}
						else {
							items[i].setIconCls('gis-menu-item-unselected');
						}
					}
				}

				// Gui
				if (param === 'orgunit') {
					userOrganisationUnit.show();
					userOrganisationUnitChildren.show();
					userOrganisationUnitGrandChildren.show();
					organisationUnitLevel.hide();
					organisationUnitGroup.hide();

					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue()) {
						treePanel.disable();
					}
				}
				else if (param === 'level') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.show();
					organisationUnitGroup.hide();
					treePanel.enable();
				}
				else if (param === 'group') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.hide();
					organisationUnitGroup.show();
					treePanel.enable();
				}
			},
			items: [
				{
					xtype: 'label',
					text: 'Selection mode',
					style: 'padding:7px 5px 5px 7px; font-weight:bold; border:0 none'
				},
				{
					text: GIS.i18n.select_organisation_units + '&nbsp;&nbsp;',
					param: 'orgunit',
					iconCls: 'gis-menu-item-selected'
				},
				{
					text: 'Select levels' + '&nbsp;&nbsp;',
					param: 'level',
					iconCls: 'gis-menu-item-unselected'
				},
				{
					text: 'Select groups' + '&nbsp;&nbsp;',
					param: 'group',
					iconCls: 'gis-menu-item-unselected'
				}
			],
			listeners: {
				afterrender: function() {
					this.getEl().addCls('gis-btn-menu');
				},
				click: function(menu, item) {
					this.clickHandler(item.param);
				}
			}
		});

		tool = Ext.create('Ext.button.Button', {
			cls: 'gis-button-organisationunitselection',
			iconCls: 'gis-button-icon-gear',
			width: 36,
			height: 24,
			menu: toolMenu
		});

		toolPanel = Ext.create('Ext.panel.Panel', {
			width: 36,
			bodyStyle: 'border:0 none; text-align:right',
			style: 'margin-right:1px',
			items: tool
		});

        organisationUnit = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">' + GIS.i18n.organisation_units + '</div>',
			hideCollapseTool: true,
            items: [
                {
                    layout: 'column',
                    bodyStyle: 'border:0 none',
                    style: 'padding-bottom:1px',
                    items: [
                        toolPanel,
                        {
                            layout: 'column',
                            bodyStyle: 'border:0 none',
                            items: [
                                userOrganisationUnit,
                                userOrganisationUnitChildren,
                                userOrganisationUnitGrandChildren,
                                organisationUnitLevel,
                                organisationUnitGroup
                            ]
                        }
                    ]
                },
                treePanel
            ],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });


        labelPanel = Ext.create('Ext.ux.panel.LabelPanel', {
            skipBoldButton: true,
            skipColorButton: true
        });

        label = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">Options</div>',
			hideCollapseTool: true,
            items: labelPanel,
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });
		// Functions

		reset = function(skipTree) {

			// Item
			layer.item.setValue(false);

			if (!layer.window.isRendered) {
				layer.core.view = null;
				return;
			}

			// Components
			toolMenu.clickHandler(toolMenu.menuValue);

			if (!skipTree) {
				treePanel.reset();
			}

			userOrganisationUnit.setValue(false);
			userOrganisationUnitChildren.setValue(false);
			userOrganisationUnitGrandChildren.setValue(false);

			organisationUnitLevel.clearValue();
			organisationUnitGroup.clearValue();

			// Layer options
			if (layer.searchWindow) {
				layer.searchWindow.destroy();
				layer.searchWindow = null;
			}
		};

		setGui = function(view) {
			var ouDim = view.rows[0],
				isOu = false,
				isOuc = false,
				isOugc = false,
				levels = [],
				groups = [],
				setWidgetGui,
				setLayerGui;

			setWidgetGui = function() {

				// Components
				if (!layer.window.isRendered) {
					return;
				}

				reset(true);

				// Organisation units
				for (var i = 0, item; i < ouDim.items.length; i++) {
					item = ouDim.items[i];

					if (item.id === 'USER_ORGUNIT') {
						isOu = true;
					}
					else if (item.id === 'USER_ORGUNIT_CHILDREN') {
						isOuc = true;
					}
					else if (item.id === 'USER_ORGUNIT_GRANDCHILDREN') {
						isOugc = true;
					}
					else if (item.id.substr(0,5) === 'LEVEL') {
						levels.push(parseInt(item.id.split('-')[1]));
					}
					else if (item.id.substr(0,8) === 'OU_GROUP') {
						groups.push(parseInt(item.id.split('-')[1]));
					}
				}

				if (levels.length) {
					toolMenu.clickHandler('level');
					organisationUnitLevel.setValue(levels);
				}
				else if (groups.length) {
					toolMenu.clickHandler('group');
					organisationUnitGroup.setValue(groups);
				}
				else {
					toolMenu.clickHandler('orgunit');
					userOrganisationUnit.setValue(isOu);
					userOrganisationUnitChildren.setValue(isOuc);
					userOrganisationUnitGrandChildren.setValue(isOugc);
				}

				treePanel.selectGraphMap(view.parentGraphMap);
			}();

			setLayerGui = function() {

				// Layer item
				layer.item.setValue(!view.hidden, view.opacity);

				// Layer menu
				layer.menu.enableItems();
			}();
		};

		getView = function(config) {
			var view = {};

			view.rows = [treePanel.getDimension()];

            Ext.apply(view, labelPanel.getConfig());

			return validateView(view);
		};

		validateView = function(view) {
			if (!(Ext.isArray(view.rows) && view.rows.length && Ext.isString(view.rows[0].dimension) && Ext.isArray(view.rows[0].items) && view.rows[0].items.length)) {
				GIS.logg.push([view.rows, layer.id + '.rows: dimension array']);
				alert('No organisation units selected');
				return false;
			}

			return view;
		};

        accordionBody = Ext.create('Ext.panel.Panel', {
			layout: 'accordion',
			activeOnTop: true,
			cls: 'ns-accordion',
			bodyStyle: 'border:0 none; margin-bottom:1px',
			height: 408,
			items: function() {
				var panels = [
					organisationUnit,
                    label
				];

				last = panels[panels.length - 1];
				last.cls = 'ns-accordion-last';

				return panels;
			}(),
            listeners: {
                afterrender: function() { // nasty workaround
                    for (var i = accordionPanels.length - 1; i >= 0; i--) {
                        accordionPanels[i].expand();
                    }
                }
            }
		});

		accordion = Ext.create('Ext.panel.Panel', {
			bodyStyle: 'border-style:none; padding:1px; padding-bottom:0',
			items: accordionBody,
			panels: accordionPanels,

			map: layer.map,
			layer: layer,
			menu: layer.menu,

			reset: reset,
			setGui: setGui,
			getView: getView,
			getParentGraphMap: function() {
				return treePanel.getParentGraphMap();
			},

			infrastructuralDataElementValuesStore: infrastructuralDataElementValuesStore,
			getExpandedPanel: function() {
				for (var i = 0, panel; i < this.panels.length; i++) {
					if (!this.panels[i].collapsed) {
						return this.panels[i];
					}
				}

				return null;
			},
			getFirstPanel: function() {
				return this.panels[0];
			},
			listeners: {
				added: function() {
					layer.accordion = this;
				},
				render: function() {
					toolMenu.clickHandler('level');
				}
			}
		});

		//createSelectHandlers();

		return accordion;
	};

	GIS.app.LayerWidgetThematic = function(layer) {

		var indicatorsByGroupStore,
			dataElementsByGroupStore,
            dataSetStore,
            programStore,
            eventDataItemAvailableStore,
            programIndicatorAvailableStore,
			periodsByTypeStore,
			infrastructuralDataElementValuesStore,
			legendsByLegendSetStore,

			valueTypeToggler,
			legendTypeToggler,

			valueType,
			indicatorGroup,
			indicator,
			dataElementGroup,
			dataElement,
			dataElementDetailLevel,
			dataElementPanel,
			dataSet,
            onEventDataItemProgramSelect,
            eventDataItemProgram,
            eventDataItem,
            onProgramIndicatorProgramSelect,
            programIndicatorProgram,
            programIndicator,
            onPeriodTypeSelect,
			periodType,
			period,
			periodPrev,
			periodNext,
            periodTypePanel,
            data,

			treePanel,
			userOrganisationUnit,
			userOrganisationUnitChildren,
			userOrganisationUnitGrandChildren,
			organisationUnitLevel,
			organisationUnitGroup,
			toolMenu,
			tool,
			toolPanel,
            organisationUnit,

			legendType,
			legendSet,
			classes,
			method,
			colorLow,
			colorHigh,
			radiusLow,
			radiusHigh,
            methodPanel,
            lowPanel,
            highPanel,
            legend,

            labelPanel,
            label,

			reset,
			setGui,
			getView,

            accordionBody,
            accordion,

            accordionPanels = [],
			dimConf = gis.conf.finals.dimension;

		// Stores

		indicatorsByGroupStore = Ext.create('Ext.data.Store', {
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
				if (Ext.isFunction(fn)) {
					if (this.isLoaded) {
						fn.call();
					}
					else {
						this.load({
							callback: fn
						});
					}
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
		});

		dataElementsByGroupStore = Ext.create('Ext.data.Store', {
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
			sortStore: function() {
				this.sort('name', 'ASC');
			},
			setTotalsProxy: function(uid, preventLoad, callbackFn) {
                var types = gis.conf.valueType.aAggregateTypes.join(','),
                    path = '/dataElements.json?fields=dimensionItem|rename(id),' + gis.init.namePropertyUrl + '&filter=valueType:in:[' + types + ']&domainType=aggregate&paging=false';

				if (Ext.isString(uid)) {
                    path += '&filter=dataElementGroups.id:eq:' + uid;
				}

				if (!path) {
					alert('Invalid parameter');
					return;
				}

				this.setProxy({
					type: 'ajax',
					url: gis.init.contextPath + '/api' + path,
					reader: {
						type: 'json',
						root: 'dataElements'
					}
				});

				if (!preventLoad) {
					this.load({
						scope: this,
						callback: function() {
							this.sortStore();

							if (Ext.isFunction(callbackFn)) {
								callbackFn();
							}
						}
					});
				}
			},
			setDetailsProxy: function(uid, preventLoad, callbackFn) {
				if (Ext.isString(uid)) {
                    var types = gis.conf.valueType.aAggregateTypes.join(',');
                        
					this.setProxy({
						type: 'ajax',
						url: gis.init.contextPath + '/api/dataElementOperands.json?fields=dimensionItem|rename(id),' + gis.init.namePropertyUrl + '&filter=valueType:in:[' + types + ']&filter=dataElement.dataElementGroups.id:eq:' + uid + '&paging=false',
						reader: {
							type: 'json',
							root: 'dataElementOperands'
						}
					});

					if (!preventLoad) {
						this.load({
							scope: this,
							callback: function() {
								this.sortStore();

								if (Ext.isFunction(callbackFn)) {
									callbackFn();
								}
							}
						});
					}
				}
				else {
					alert('Invalid parameter');
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
		});

        dataSetStore = Ext.create('Ext.data.Store', {
            fields: ['id', 'name'],
            proxy: {
                type: 'ajax',
                url: gis.init.contextPath + '/api/dataSets.json?fields=id,' + gis.init.namePropertyUrl + '&paging=false',
                reader: {
                    type: 'json',
                    root: 'dataSets'
                }
            },
            sortStore: function() {
                this.sort('name', 'ASC');
            },
            isLoaded: false,
            listeners: {
                load: function(s) {
                    this.isLoaded = true;
                }
            }
        });
        
        programStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			proxy: {
				type: 'ajax',
				url: gis.init.contextPath + '/api/programs.json?fields=id,displayName|rename(name)&paging=false',
				reader: {
					type: 'json',
					root: 'programs'
				},
				pageParam: false,
				startParam: false,
				limitParam: false
			}
		});

        eventDataItemAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: [],
			sortStore: function() {
				this.sort('name', 'ASC');
			},
            loadDataAndUpdate: function(data, append) {
                this.clearFilter(); // work around
                this.loadData(data, append);
                this.updateFilter();
            },
            getRecordsByIds: function(ids) {
                var records = [];

                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        records.push(this.getAt(index));
                    }
                }

                return records;
            },
            updateFilter: function() {
                var selectedStoreIds = dataSelectedStore.getIds();

                this.clearFilter();

                this.filterBy(function(record) {
                    return !Ext.Array.contains(selectedStoreIds, record.data.id);
                });
            }
		});

        programIndicatorAvailableStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name'],
			data: [],
			sortStore: function() {
				this.sort('name', 'ASC');
			},
            loadDataAndUpdate: function(data, append) {
                this.clearFilter(); // work around
                this.loadData(data, append);
                this.updateFilter();
            },
            getRecordsByIds: function(ids) {
                var records = [];

                ids = Ext.Array.from(ids);

                for (var i = 0, index; i < ids.length; i++) {
                    index = this.findExact('id', ids[i]);

                    if (index !== -1) {
                        records.push(this.getAt(index));
                    }
                }

                return records;
            },
            updateFilter: function() {
                var selectedStoreIds = dataSelectedStore.getIds();

                this.clearFilter();

                this.filterBy(function(record) {
                    return !Ext.Array.contains(selectedStoreIds, record.data.id);
                });
            }
		});
        
		periodsByTypeStore = Ext.create('Ext.data.Store', {
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
		});

		infrastructuralDataElementValuesStore = Ext.create('Ext.data.Store', {
			fields: ['name', 'value'],
            sorters: [{
                property: 'name',
                direction: 'ASC'
            }]
		});

		legendsByLegendSetStore = Ext.create('Ext.data.Store', {
			fields: ['id', 'name', 'startValue', 'endValue', 'color'],
			proxy: {
				type: 'ajax',
				url: '',
				reader: {
					type: 'json',
					root: 'legends'
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
		});

		// Togglers

		valueTypeToggler = function(valueType) {
			if (valueType === dimConf.indicator.objectName) {
				indicatorGroup.show();
				indicator.show();
				dataElementGroup.hide();
				dataElementPanel.hide();
				dataSet.hide();
                eventDataItemProgram.hide();
                eventDataItem.hide();
                programIndicatorProgram.hide();
                programIndicator.hide();
			}
			else if (valueType === dimConf.dataElement.objectName || valueType === dimConf.operand.objectName) {
				indicatorGroup.hide();
				indicator.hide();
				dataElementGroup.show();
				dataElementPanel.show();
				dataSet.hide();
                eventDataItemProgram.hide();
                eventDataItem.hide();
                programIndicatorProgram.hide();
                programIndicator.hide();
			}
			else if (valueType === dimConf.dataSet.objectName) {
				indicatorGroup.hide();
				indicator.hide();
				dataElementGroup.hide();
				dataElementPanel.hide();
				dataSet.show();
                eventDataItemProgram.hide();
                eventDataItem.hide();
                programIndicatorProgram.hide();
                programIndicator.hide();
			}
			else if (valueType === dimConf.eventDataItem.objectName) {
				indicatorGroup.hide();
				indicator.hide();
				dataElementGroup.hide();
				dataElementPanel.hide();
				dataSet.hide();
                eventDataItemProgram.show();
                eventDataItem.show();
                programIndicatorProgram.hide();
                programIndicator.hide();
			}
			else if (valueType === dimConf.programIndicator.objectName) {
				indicatorGroup.hide();
				indicator.hide();
				dataElementGroup.hide();
				dataElementPanel.hide();
				dataSet.hide();
                eventDataItemProgram.hide();
                eventDataItem.hide();
                programIndicatorProgram.show();
                programIndicator.show();
			}
		};

		legendTypeToggler = function(legendType) {
			if (legendType === 'automatic') {
				methodPanel.show();
				lowPanel.show();
				highPanel.show();
				legendSet.hide();
			}
			else if (legendType === 'predefined') {
				methodPanel.hide();
				lowPanel.hide();
				highPanel.hide();
				legendSet.show();
			}
		};

		// Components

		valueType = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.value_type,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			forceSelection: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			value: dimConf.indicator.objectName,
			store: Ext.create('Ext.data.ArrayStore', {
				fields: ['id', 'name'],
				data: [
					[dimConf.indicator.objectName, GIS.i18n.indicator],
					[dimConf.dataElement.objectName, GIS.i18n.dataelement],
					[dimConf.dataSet.objectName, GIS.i18n.reporting_rates],
					[dimConf.eventDataItem.objectName, GIS.i18n.event_data_items],
					[dimConf.programIndicator.objectName, GIS.i18n.program_indicators]
				]
			}),
			listeners: {
				select: function() {
					valueTypeToggler(this.getValue());
				}
			}
		});

		indicatorGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.indicator_group,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			forceSelection: true,
			queryMode: 'local',
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			store: {
				fields: ['id', 'name'],
				data: gis.init.indicatorGroups
			},
			listeners: {
				select: function() {
					indicator.clearValue();

					indicator.store.proxy.url = gis.init.contextPath + '/api/indicators.json?fields=id,' + gis.init.namePropertyUrl + '&paging=false&filter=indicatorGroups.id:eq:' + this.getValue();
					indicator.store.load();
				}
			}
		});

		indicator = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.indicator,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			forceSelection: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			listConfig: {loadMask: false},
			store: indicatorsByGroupStore,
			listeners: {
				select: function(cb) {
					Ext.Ajax.request({
						url: gis.init.contextPath + '/api/indicators.json?fields=legendSet[id]&paging=false&filter=id:eq:' + this.getValue(),
						success: function(r) {
							var set = Ext.decode(r.responseText).indicators[0].legendSet;

							if (Ext.isObject(set) && set.id) {
								legendType.setValue(gis.conf.finals.widget.legendtype_predefined);
								legendTypeToggler(gis.conf.finals.widget.legendtype_predefined);

								if (gis.store.legendSets.isLoaded) {
									legendSet.setValue(set.id);
								}
								else {
									gis.store.legendSets.loadFn( function() {
										legendSet.setValue(set.id);
									});
								}
							}
							else {
								legendType.setValue(gis.conf.finals.widget.legendtype_automatic);
								legendTypeToggler(gis.conf.finals.widget.legendtype_automatic);
							}
						}
					});
				}
			}
		});

		dataElementGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.dataelement_group,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			forceSelection: true,
			hidden: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			store: {
				fields: ['id', 'name'],
				data: gis.init.dataElementGroups
			},
			loadAvailable: function(preventLoad) {
				var store = dataElementsByGroupStore,
					detailLevel = dataElementDetailLevel.getValue(),
					value = this.getValue();

				if (value) {
					if (detailLevel === gis.conf.finals.dimension.dataElement.objectName) {
						store.setTotalsProxy(value, preventLoad);
					}
					else {
						store.setDetailsProxy(value, preventLoad);
					}
				}
			},
			listeners: {
				select: function(cb) {
					dataElement.clearValue();
					cb.loadAvailable();
				}
			}
		});

		dataElement = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.dataelement,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			forceSelection: true,
			width: gis.conf.layout.widget.item_width - 65,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			listConfig: {
				loadMask: false,
				minWidth: 188
			},
			store: dataElementsByGroupStore,
			listeners: {
				select: function() {
					var id = this.getValue(),
						index = id.indexOf('.');

					if (index !== -1) {
						id = id.substr(0, index);
					}

					Ext.Ajax.request({
						url: gis.init.contextPath + '/api/dataElements.json?fields=legendSet[id]&paging=false&filter=id:eq:' + id,
						success: function(r) {
							var set = Ext.decode(r.responseText).dataElements[0].legendSet;

							if (Ext.isObject(set) && set.id) {
								legendType.setValue(gis.conf.finals.widget.legendtype_predefined);
								legendTypeToggler(gis.conf.finals.widget.legendtype_predefined);

								if (gis.store.legendSets.isLoaded) {
									legendSet.setValue(set.id);
								}
								else {
									gis.store.legendSets.loadFn( function() {
										legendSet.setValue(set.id);
									});
								}
							}
							else {
								legendType.setValue(gis.conf.finals.widget.legendtype_automatic);
								legendTypeToggler(gis.conf.finals.widget.legendtype_automatic);
							}
						}
					});
				}
			}
		});

		dataElementDetailLevel = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			style: 'margin-left:1px',
			queryMode: 'local',
			editable: false,
			valueField: 'id',
			displayField: 'text',
			width: 65 - 1,
			value: dimConf.dataElement.objectName,
			onSelect: function() {
				dataElementGroup.loadAvailable();
				dataElement.clearValue();
			},
			store: {
				fields: ['id', 'text'],
				data: [
					{id: dimConf.dataElement.objectName, text: GIS.i18n.totals},
					{id: dimConf.operand.objectName, text: GIS.i18n.details}
				]
			},
			listeners: {
				select: function(cb) {
					cb.onSelect();
				}
			}
		});

		dataElementPanel = Ext.create('Ext.container.Container', {
			layout: 'column',
			bodyStyle: 'border:0 none',
			hidden: true,
			items: [
				dataElement,
				dataElementDetailLevel
			]
		});

		dataSet = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.dataset,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			forceSelection: true,
			hidden: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			listConfig: {loadMask: false},
			store: dataSetStore,
			listeners: {
				select: function(cb) {
					Ext.Ajax.request({
						url: gis.init.contextPath + '/api/dataSets.json?fields=legendSet[id]&paging=false&filter=id:eq:' + this.getValue(),
						success: function(r) {
							var set = Ext.decode(r.responseText).dataSets[0].legendSet;

							if (Ext.isObject(set) && set.id) {
								legendType.setValue(gis.conf.finals.widget.legendtype_predefined);
								legendTypeToggler(gis.conf.finals.widget.legendtype_predefined);

								if (gis.store.legendSets.isLoaded) {
									legendSet.setValue(set.id);
								}
								else {
									gis.store.legendSets.loadFn( function() {
										legendSet.setValue(set.id);
									});
								}
							}
							else {
								legendType.setValue(gis.conf.finals.widget.legendtype_automatic);
								legendTypeToggler(gis.conf.finals.widget.legendtype_automatic);
							}
						}
					});
				}
			}
		});

        onEventDataItemProgramSelect = function(programId) {
            eventDataItem.clearValue();

            var types = gis.conf.valueType.tAggregateTypes.join(','),
                namePropertyUrl = gis.init.namePropertyUrl;
            
            Ext.Ajax.request({
                url: gis.init.contextPath + '/api/programDataElements.json?program=' + programId + '&filter=valueType:in:[' + types + ']&fields=dimensionItem|rename(id),name,valueType&paging=false',
                disableCaching: false,
                success: function(r) {
                    var elements = Ext.decode(r.responseText).programDataElements,
                        isA = Ext.isArray,
                        isO = Ext.isObject;

                    Ext.Ajax.request({
                        url: gis.init.contextPath + '/api/programs.json?filter=id:eq:' + programId + '&filter=programTrackedEntityAttributes.trackedEntityAttribute.confidential:eq:false&filter=programTrackedEntityAttributes.valueType:in:[' + types + ']&fields=programTrackedEntityAttributes[dimensionItem|rename(id),' + namePropertyUrl + '|rename(name),valueType]&paging=false',
                        disableCaching: false,
                        success: function(r) {
                            var attributes = (Ext.decode(r.responseText).programs[0] || {}).programTrackedEntityAttributes || [],
                                data = gis.util.array.sort(Ext.Array.clean([].concat(elements, attributes))) || [];

                            if (data) {
                                eventDataItemAvailableStore.loadData(data);
                            }
                        }
                    });
                }
            });
        };

		eventDataItemProgram = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.program,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			forceSelection: true,
			hidden: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			store: programStore,
			listeners: {
				select: function(cb) {
					onEventDataItemProgramSelect(cb.getValue());
				}
			}
		});

		eventDataItem = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.event_data_item,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			forceSelection: true,
			hidden: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			listConfig: {loadMask: false},
			store: eventDataItemAvailableStore
		});

        onProgramIndicatorProgramSelect = function(programId) {
            programIndicator.clearValue();

            Ext.Ajax.request({
                url: gis.init.contextPath + '/api/programs.json?paging=false&fields=programIndicators[id,displayName|rename(name)]&filter=id:eq:' + programId,
                success: function(r) {
                    r = Ext.decode(r.responseText);

                    var isA = Ext.isArray,
                        isO = Ext.isObject,
                        program = isA(r.programs) && r.programs.length ? r.programs[0] : null,
                        programIndicators = isO(program) && isA(program.programIndicators) && program.programIndicators.length ? program.programIndicators : [],
                        data = gis.util.array.sort(Ext.Array.clean(programIndicators)) || [];

                    programIndicatorAvailableStore.loadData(data);
                }
            });

        };

        programIndicatorProgram = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.program,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			forceSelection: true,
			hidden: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			store: programStore,
			listeners: {
				select: function(cb) {
					onProgramIndicatorProgramSelect(cb.getValue());
				}
			}
		});

		programIndicator = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.event_data_item,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			forceSelection: true,
			hidden: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			listConfig: {loadMask: false},
			store: programIndicatorAvailableStore
		});

        onPeriodTypeSelect = function() {
            var type = periodType.getValue(),
                periodOffset = periodType.periodOffset,
                generator = gis.init.periodGenerator,
                periods;

            if (type === 'relativePeriods') {
                periodsByTypeStore.loadData(gis.conf.period.relativePeriods);

                periodPrev.disable();
                periodNext.disable();
            }
            else {
                periods = generator.generateReversedPeriods(type, type === 'Yearly' ? periodOffset - 5 : periodOffset);

                for (var i = 0; i < periods.length; i++) {
                    periods[i].id = periods[i].iso;
                }

                periodsByTypeStore.setIndex(periods);
                periodsByTypeStore.loadData(periods);

                periodPrev.enable();
                periodNext.enable();
            }

            period.selectFirst();
        };

		periodType = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			editable: false,
			valueField: 'id',
			displayField: 'name',
			forceSelection: true,
			queryMode: 'local',
			width: 142,
			store: gis.store.periodTypes,
			periodOffset: 0,
			listeners: {
				select: function() {
                    periodType.periodOffset = 0;
                    onPeriodTypeSelect();
				}
			}
		});

		period = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.period,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			forceSelection: true,
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			store: periodsByTypeStore,
			selectFirst: function() {
				this.setValue(this.store.getAt(0).data.id);
			}
		});

		periodPrev = Ext.create('Ext.button.Button', {
			xtype: 'button',
			text: '<',
			width: 22,
            height: 24,
			style: 'margin-left: 1px',
			handler: function() {
                if (periodType.getValue()) {
                    periodType.periodOffset--;
                    onPeriodTypeSelect();
                }
			}
		});

		periodNext = Ext.create('Ext.button.Button', {
			xtype: 'button',
			text: '>',
			width: 22,
            height: 24,
			style: 'margin-left: 1px',
			scope: this,
			handler: function() {
                if (periodType.getValue()) {
                    periodType.periodOffset++;
                    onPeriodTypeSelect();
                }
            }
		});

		periodTypePanel = Ext.create('Ext.panel.Panel', {
			layout: 'hbox',
            bodyStyle: 'border:0 none',
			items: [
				{
					html: GIS.i18n.period_type + ':',
					width: 100,
					bodyStyle: 'border:0 none',
					style: 'padding: 3px 0 0 4px'
				},
				periodType,
				periodPrev,
				periodNext
			]
		});

        data = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">' + 'Data and periods' + '</div>',
			hideCollapseTool: true,
            items: [
                valueType,
                indicatorGroup,
                indicator,
                dataElementGroup,
                dataElementPanel,
                dataSet,
                eventDataItemProgram,
                eventDataItem,
                programIndicatorProgram,
                programIndicator,
                periodTypePanel,
                period,
            ],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });


		treePanel = Ext.create('Ext.tree.Panel', {
			cls: 'gis-tree',
			height: 277,
			style: 'border-top: 1px solid #ddd; padding-top: 1px',
			displayField: 'name',
			width: gis.conf.layout.widget.item_width,
			rootVisible: false,
			autoScroll: true,
			multiSelect: true,
			rendered: false,
			reset: function() {
				var rootNode = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
				this.collapseAll();
				this.expandPath(rootNode.getPath());
				this.getSelectionModel().select(rootNode);
			},
			selectRootIf: function() {
				if (this.getSelectionModel().getSelection().length < 1) {
					var node = this.getRootNode().findChild('id', gis.init.rootNodes[0].id);
					if (this.rendered) {
						this.getSelectionModel().select(node);
					}
					return node;
				}
			},
			isPending: false,
			recordsToSelect: [],
			recordsToRestore: [],
			multipleSelectIf: function(map, doUpdate) {
                this.recordsToSelect = Ext.Array.clean(this.recordsToSelect);
                
				if (this.recordsToSelect.length === gis.util.object.getLength(map)) {
					this.getSelectionModel().select(this.recordsToSelect);
					this.recordsToSelect = [];
					this.isPending = false;

					if (doUpdate) {
						update();
					}
				}
			},
			multipleExpand: function(id, map, doUpdate) {
				var that = this,
					rootId = gis.conf.finals.root.id,
					path = map[id];

				if (path.substr(0, rootId.length + 1) !== ('/' + rootId)) {
					path = '/' + rootId + path;
				}

				that.expandPath(path, 'id', '/', function() {
					record = Ext.clone(that.getRootNode().findChild('id', id, true));
					that.recordsToSelect.push(record);
					that.multipleSelectIf(map, doUpdate);
				});
			},
            select: function(url, params) {
                if (!params) {
                    params = {};
                }
                Ext.Ajax.request({
                    url: url,
                    method: 'GET',
                    params: params,
                    scope: this,
                    success: function(r) {
                        var a = Ext.decode(r.responseText).organisationUnits;
                        this.numberOfRecords = a.length;
                        for (var i = 0; i < a.length; i++) {
                            this.multipleExpand(a[i].id, a[i].path);
                        }
                    }
                });
            },
			getParentGraphMap: function() {
				var selection = this.getSelectionModel().getSelection(),
					map = {};

				if (Ext.isArray(selection) && selection.length) {
					for (var i = 0, pathArray, key; i < selection.length; i++) {
						pathArray = selection[i].getPath().split('/');
						map[pathArray.pop()] = pathArray.join('/');
					}
				}

				return map;
			},
			selectGraphMap: function(map, update) {
				if (!gis.util.object.getLength(map)) {
					return;
				}

				this.isPending = true;

				for (var key in map) {
					if (map.hasOwnProperty(key)) {
						treePanel.multipleExpand(key, map, update);
					}
				}
			},
            store: Ext.create('Ext.data.TreeStore', {
				fields: ['id', 'name', 'hasChildren'],
				proxy: {
					type: 'rest',
					format: 'json',
					noCache: false,
					extraParams: {
						fields: 'children[id,' + gis.init.namePropertyUrl + ',children::isNotEmpty|rename(hasChildren)&paging=false'
					},
					url: gis.init.contextPath + '/api/organisationUnits',
					reader: {
						type: 'json',
						root: 'children'
					},
					sortParam: false
				},
				sorters: [{
					property: 'name',
					direction: 'ASC'
				}],
				root: {
					id: gis.conf.finals.root.id,
					expanded: true,
					children: gis.init.rootNodes
				},
				listeners: {
					load: function(store, node, records) {
						Ext.Array.each(records, function(record) {
                            if (Ext.isBoolean(record.data.hasChildren)) {
                                record.set('leaf', !record.data.hasChildren);
                            }
                        });
					}
				}
			}),
			xable: function(values) {
				for (var i = 0; i < values.length; i++) {
					if (!!values[i]) {
						this.disable();
						return;
					}
				}

				this.enable();
			},
			getDimension: function() {
				var r = treePanel.getSelectionModel().getSelection(),
					config = {
						dimension: gis.conf.finals.dimension.organisationUnit.objectName,
						items: []
					};

				if (toolMenu.menuValue === 'orgunit') {
					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue() || userOrganisationUnitGrandChildren.getValue()) {
						if (userOrganisationUnit.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT',
								name: ''
							});
						}
						if (userOrganisationUnitChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_CHILDREN',
								name: ''
							});
						}
						if (userOrganisationUnitGrandChildren.getValue()) {
							config.items.push({
								id: 'USER_ORGUNIT_GRANDCHILDREN',
								name: ''
							});
						}
					}
					else {
						for (var i = 0; i < r.length; i++) {
							config.items.push({id: r[i].data.id});
						}
					}
				}
				else if (toolMenu.menuValue === 'level') {
					var levels = organisationUnitLevel.getValue();

					for (var i = 0; i < levels.length; i++) {
						config.items.push({
							id: 'LEVEL-' + levels[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}
				else if (toolMenu.menuValue === 'group') {
					var groupIds = organisationUnitGroup.getValue();

					for (var i = 0; i < groupIds.length; i++) {
						config.items.push({
							id: 'OU_GROUP-' + groupIds[i],
							name: ''
						});
					}

					for (var i = 0; i < r.length; i++) {
						config.items.push({
							id: r[i].data.id,
							name: ''
						});
					}
				}

				return config.items.length ? config : null;
			},
			listeners: {
				beforeitemexpand: function() {
					var rts = treePanel.recordsToSelect;

					if (!treePanel.isPending) {
						treePanel.recordsToRestore = treePanel.getSelectionModel().getSelection();
					}
				},
				itemexpand: function() {
					if (!treePanel.isPending && treePanel.recordsToRestore.length) {
						treePanel.getSelectionModel().select(treePanel.recordsToRestore);
						treePanel.recordsToRestore = [];
					}
				},
				render: function() {
					this.rendered = true;
				},
				afterrender: function() {
					this.getSelectionModel().select(0);
				},
				itemcontextmenu: function(v, r, h, i, e) {
					v.getSelectionModel().select(r, false);

					if (v.menu) {
						v.menu.destroy();
					}
					v.menu = Ext.create('Ext.menu.Menu', {
						showSeparator: false,
						shadow: false
					});
					if (!r.data.leaf) {
						v.menu.add({
							text: GIS.i18n.select_sub_units,
							icon: 'images/node-select-child.png',
							handler: function() {
								r.expand(false, function() {
									v.getSelectionModel().select(r.childNodes, true);
									v.getSelectionModel().deselect(r);
								});
							}
						});
					}
					else {
						return;
					}

					v.menu.showAt(e.xy);
				}
			}
		});

		userOrganisationUnit = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.3,
			style: 'padding-top: 2px; padding-left: 3px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'User OU',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnitChildren.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.33,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'Sub-units',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitGrandChildren.getValue()]);
			}
		});

		userOrganisationUnitGrandChildren = Ext.create('Ext.form.field.Checkbox', {
			columnWidth: 0.34,
			style: 'padding-top: 2px; margin-bottom: 0',
			boxLabelCls: 'x-form-cb-label-alt1',
			boxLabel: 'Sub-x2-units',
			labelWidth: gis.conf.layout.form_label_width,
			handler: function(chb, checked) {
				treePanel.xable([checked, userOrganisationUnit.getValue(), userOrganisationUnitChildren.getValue()]);
			}
		});

		organisationUnitLevel = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: gis.conf.layout.widget.item_width - 37,
			valueField: 'level',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_levels,
			editable: false,
			store: {
				fields: ['id', 'name', 'level'],
				data: gis.init.organisationUnitLevels
			}
		});

		organisationUnitGroup = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			multiSelect: true,
			style: 'margin-bottom:0',
			width: gis.conf.layout.widget.item_width - 37,
			valueField: 'id',
			displayField: 'name',
			emptyText: GIS.i18n.select_organisation_unit_groups,
			editable: false,
			store: gis.store.organisationUnitGroup
		});

		toolMenu = Ext.create('Ext.menu.Menu', {
			shadow: false,
			showSeparator: false,
			menuValue: 'level',
			clickHandler: function(param) {
				if (!param) {
					return;
				}

				var items = this.items.items;
				this.menuValue = param;

				// Menu item icon cls
				for (var i = 0; i < items.length; i++) {
					if (items[i].setIconCls) {
						if (items[i].param === param) {
							items[i].setIconCls('gis-menu-item-selected');
						}
						else {
							items[i].setIconCls('gis-menu-item-unselected');
						}
					}
				}

				// Gui
				if (param === 'orgunit') {
					userOrganisationUnit.show();
					userOrganisationUnitChildren.show();
					userOrganisationUnitGrandChildren.show();
					organisationUnitLevel.hide();
					organisationUnitGroup.hide();

					if (userOrganisationUnit.getValue() || userOrganisationUnitChildren.getValue()) {
						treePanel.disable();
					}
				}
				else if (param === 'level') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.show();
					organisationUnitGroup.hide();
					treePanel.enable();
				}
				else if (param === 'group') {
					userOrganisationUnit.hide();
					userOrganisationUnitChildren.hide();
					userOrganisationUnitGrandChildren.hide();
					organisationUnitLevel.hide();
					organisationUnitGroup.show();
					treePanel.enable();
				}
			},
			items: [
				{
					xtype: 'label',
					text: 'Selection mode',
					style: 'padding:7px 5px 5px 7px; font-weight:bold; border:0 none'
				},
				{
					text: GIS.i18n.select_organisation_units + '&nbsp;&nbsp;',
					param: 'orgunit',
					iconCls: 'gis-menu-item-selected'
				},
				{
					text: 'Select levels' + '&nbsp;&nbsp;',
					param: 'level',
					iconCls: 'gis-menu-item-unselected'
				},
				{
					text: 'Select groups' + '&nbsp;&nbsp;',
					param: 'group',
					iconCls: 'gis-menu-item-unselected'
				}
			],
			listeners: {
				afterrender: function() {
					this.getEl().addCls('gis-btn-menu');
				},
				click: function(menu, item) {
					this.clickHandler(item.param);
				}
			}
		});

		tool = Ext.create('Ext.button.Button', {
			cls: 'gis-button-organisationunitselection',
			iconCls: 'gis-button-icon-gear',
			width: 36,
			height: 24,
			menu: toolMenu
		});

		toolPanel = Ext.create('Ext.panel.Panel', {
			width: 36,
			bodyStyle: 'border:0 none; text-align:right',
			style: 'margin-right:1px',
			items: tool
		});

        organisationUnit = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">' + GIS.i18n.organisation_units + '</div>',
			hideCollapseTool: true,
            items: [
                {
                    layout: 'column',
                    bodyStyle: 'border:0 none',
                    style: 'padding-bottom:1px',
                    items: [
                        toolPanel,
                        {
                            layout: 'column',
                            bodyStyle: 'border:0 none',
                            items: [
                                userOrganisationUnit,
                                userOrganisationUnitChildren,
                                userOrganisationUnitGrandChildren,
                                organisationUnitLevel,
                                organisationUnitGroup
                            ]
                        }
                    ]
                },
                treePanel
            ],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });


		legendType = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.legend_type,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			value: gis.conf.finals.widget.legendtype_automatic,
			width: gis.conf.layout.widget.item_width,
			store: Ext.create('Ext.data.ArrayStore', {
				fields: ['id', 'name'],
				data: [
					[gis.conf.finals.widget.legendtype_automatic, GIS.i18n.automatic],
					[gis.conf.finals.widget.legendtype_predefined, GIS.i18n.predefined]
				]
			}),
			listeners: {
				select: function() {
					legendTypeToggler(this.getValue());
				}
			}
		});

		legendSet = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			fieldLabel: GIS.i18n.legendset,
			editable: false,
			valueField: 'id',
			displayField: 'name',
			width: gis.conf.layout.widget.item_width,
			labelWidth: gis.conf.layout.widget.itemlabel_width,
			hidden: true,
			store: gis.store.legendSets
		});

		classes = Ext.create('Ext.form.field.Number', {
            cls: 'gis-numberfield',
			editable: false,
			valueField: 'id',
			displayField: 'id',
			queryMode: 'local',
			value: 5,
			minValue: 1,
			maxValue: 7,
			width: 50,
            fieldStyle: 'height: 24px',
			style: 'margin-right: 1px',
			store: Ext.create('Ext.data.ArrayStore', {
				fields: ['id'],
				data: [[1], [2], [3], [4], [5], [6], [7]]
			})
		});

		method = Ext.create('Ext.form.field.ComboBox', {
			cls: 'gis-combo',
			editable: false,
			valueField: 'id',
			displayField: 'name',
			queryMode: 'local',
			value: 3,
			width: 137,
			store: Ext.create('Ext.data.ArrayStore', {
				fields: ['id', 'name'],
				data: [
					[2, GIS.i18n.equal_intervals],
					[3, GIS.i18n.equal_counts]
				]
			})
		});

		colorLow = Ext.create('Ext.ux.button.ColorButton', {
			style: 'margin-right: 1px',
			width: 137,
            height: 24,
			value: 'ff0000',
			scope: this
		});

		colorHigh = Ext.create('Ext.ux.button.ColorButton', {
			style: 'margin-right: 1px',
			width: 137,
            height: 24,
			value: '00ff00',
			scope: this
		});

		radiusLow = Ext.create('Ext.form.field.Number', {
            cls: 'gis-numberfield',
			width: 50,
			allowDecimals: false,
			minValue: 1,
			value: 5
		});

		radiusHigh = Ext.create('Ext.form.field.Number', {
            cls: 'gis-numberfield',
			width: 50,
			allowDecimals: false,
			minValue: 1,
			value: 15
		});

		methodPanel = Ext.create('Ext.container.Container', {
			layout: 'hbox',
            height: 25,
            bodyStyle: 'border: 0 none; margin-bottom:1px',
			items: [
				{
					html: GIS.i18n.classes_method + ':',
					width: 100,
					style: 'padding: 4px 0 0 4px',
                    bodyStyle: 'border: 0 none'
				},
				classes,
				method
			]
		});

		lowPanel = Ext.create('Ext.container.Container', {
			layout: 'hbox',
            height: 25,
            bodyStyle: 'border: 0 none',
			items: [
				{
					html: GIS.i18n.low_color_size + ':',
					width: 100,
					style: 'padding: 4px 0 0 4px',
                    bodyStyle: 'border: 0 none'
				},
				colorLow,
				radiusLow
			]
		});

		highPanel = Ext.create('Ext.panel.Panel', {
			layout: 'hbox',
            height: 25,
            bodyStyle: 'border: 0 none',
			items: [
				{
					html: GIS.i18n.high_color_size + ':',
					width: 100,
					style: 'padding: 4px 0 0 4px',
                    bodyStyle: 'border: 0 none'
				},
				colorHigh,
				radiusHigh
			]
		});

        legend = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">' + GIS.i18n.legend + '</div>',
			hideCollapseTool: true,
            items: [
                legendType,
                legendSet,
                methodPanel,
                lowPanel,
                highPanel,
            ],
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });


        labelPanel = Ext.create('Ext.ux.panel.LabelPanel');

        label = Ext.create('Ext.panel.Panel', {
			title: '<div class="ns-panel-title-data">Options</div>',
			hideCollapseTool: true,
            items: labelPanel,
			listeners: {
				added: function() {
					accordionPanels.push(this);
				}
			}
        });

		// Functions

		reset = function(skipTree) {

			// Item
			layer.item.setValue(false);

			// Layer options
			if (layer.searchWindow) {
				layer.searchWindow.destroy();
				layer.searchWindow = null;
			}
			if (layer.filterWindow) {
				layer.filterWindow.destroy();
				layer.filterWindow = null;
			}

			// Components
			if (!layer.window.isRendered) {
				layer.core.view = null;
				return;
			}

			valueType.reset();
			valueTypeToggler(dimConf.indicator.objectName);

			indicatorGroup.clearValue();
			indicator.clearValue();
			indicator.store.removeAll();

			dataElementGroup.clearValue();
			dataElement.clearValue();
			dataElement.store.removeAll();

			dataSet.clearValue();
			dataSet.store.removeAll();

			periodType.clearValue();
			period.clearValue();
			period.store.removeAll();

			legendType.reset();
			legendTypeToggler(gis.conf.finals.widget.legendtype_automatic);
			legendSet.clearValue();
			legendSet.store.removeAll();

			classes.reset();
			method.reset();
			colorLow.reset();
			colorHigh.reset();
			radiusLow.reset();
			radiusHigh.reset();

			toolMenu.clickHandler(toolMenu.menuValue);

			if (!skipTree) {
				treePanel.reset();
			}

			userOrganisationUnit.setValue(false);
			userOrganisationUnitChildren.setValue(false);
			userOrganisationUnitGrandChildren.setValue(false);

			organisationUnitLevel.clearValue();
			organisationUnitGroup.clearValue();
		};

		setGui = function(view) {
			var dxDim = view.columns[0],
				peDim = view.filters[0],
				ouDim = view.rows[0],
				vType = dxDim.dimension === dimConf.operand.objectName ? dimConf.dataElement.objectName : dxDim.dimension,
				lType = Ext.isObject(view.legendSet) && Ext.isString(view.legendSet.id) ? gis.conf.finals.widget.legendtype_predefined : gis.conf.finals.widget.legendtype_automatic,
				itemTypeCmpMap = {},
                objectNameProgramCmpMap = {},
				isOu = false,
				isOuc = false,
				isOugc = false,
				levels = [],
				groups = [],
				setLayerGui,
				setWidgetGui,
                dxItemType,
                dxObjectName;

			itemTypeCmpMap[dimConf.indicator.itemType] = indicator;
			itemTypeCmpMap[dimConf.dataElement.itemType] = dataElement;
			itemTypeCmpMap[dimConf.operand.itemType] = dataElement;
			itemTypeCmpMap[dimConf.dataSet.itemType] = dataSet;
			itemTypeCmpMap[dimConf.programDataElement.itemType] = eventDataItem;
			itemTypeCmpMap[dimConf.programAttribute.itemType] = eventDataItem;
			itemTypeCmpMap[dimConf.programIndicator.itemType] = programIndicator;

            objectNameProgramCmpMap[dimConf.eventDataItem.objectName] = eventDataItemProgram;
            objectNameProgramCmpMap[dimConf.programIndicator.objectName] = programIndicatorProgram;

			setWidgetGui = function() {

				// Components
				if (!layer.window.isRendered) {
					return;
				}

				// Reset
				reset(true);

                // dx type
                dxItemType = gis.util.dhis.getDataDimensionItemTypes(view.dataDimensionItems)[0];
                dxObjectName = dimConf.itemTypeMap[dxItemType].objectName;

				// Value type
				valueType.setValue(dxObjectName);
				valueTypeToggler(dxObjectName);

                if (dxObjectName === dimConf.dataElement.objectName) {
                    dataElementDetailLevel.setValue(dxObjectName);
                }

				// Data
				itemTypeCmpMap[dxItemType].store.add(dxDim.items[0]);
				itemTypeCmpMap[dxItemType].setValue(dxDim.items[0].id);

                // program
                if (dxObjectName === dimConf.eventDataItem.objectName && view.program) {
                    objectNameProgramCmpMap[dimConf.eventDataItem.objectName].store.add(view.program);
                    objectNameProgramCmpMap[dimConf.eventDataItem.objectName].setValue(view.program.id);
                }
                else if (dxObjectName === dimConf.programIndicator.objectName && view.program) {
                    objectNameProgramCmpMap[dimConf.programIndicator.objectName].store.add(view.program);
                    objectNameProgramCmpMap[dimConf.programIndicator.objectName].setValue(view.program.id);
                }

				// Period
				period.store.add(gis.conf.period.relativePeriodRecordsMap[peDim.items[0].id] ? gis.conf.period.relativePeriodRecordsMap[peDim.items[0].id] : peDim.items[0]);
				period.setValue(peDim.items[0].id);

				// Legend
				legendType.setValue(lType);
				legendTypeToggler(lType);

				if (lType === gis.conf.finals.widget.legendtype_automatic) {
					classes.setValue(view.classes);
					method.setValue(view.method);
					colorLow.setValue(view.colorLow);
					colorHigh.setValue(view.colorHigh);
					radiusLow.setValue(view.radiusLow);
					radiusHigh.setValue(view.radiusHigh);
				}
				else if (lType === gis.conf.finals.widget.legendtype_predefined) {
					legendSet.store.add(view.legendSet);
					legendSet.setValue(view.legendSet.id);
				}

				// Organisation units
				for (var i = 0, item; i < ouDim.items.length; i++) {
					item = ouDim.items[i];

					if (item.id === 'USER_ORGUNIT') {
						isOu = true;
					}
					else if (item.id === 'USER_ORGUNIT_CHILDREN') {
						isOuc = true;
					}
					else if (item.id === 'USER_ORGUNIT_GRANDCHILDREN') {
						isOugc = true;
					}
					else if (item.id.substr(0,5) === 'LEVEL') {
						levels.push(parseInt(item.id.split('-')[1]));
					}
					else if (item.id.substr(0,8) === 'OU_GROUP') {
						groups.push(parseInt(item.id.split('-')[1]));
					}
				}

				if (levels.length) {
					toolMenu.clickHandler('level');
					organisationUnitLevel.setValue(levels);
				}
				else if (groups.length) {
					toolMenu.clickHandler('group');
					organisationUnitGroup.setValue(groups);
				}
				else {
					toolMenu.clickHandler('orgunit');
					userOrganisationUnit.setValue(isOu);
					userOrganisationUnitChildren.setValue(isOuc);
					userOrganisationUnitGrandChildren.setValue(isOugc);
				}

				treePanel.selectGraphMap(view.parentGraphMap);

                // labels
                labelPanel.setConfig(view);
			}();

			setLayerGui = function() {

				// Layer item
				layer.item.setValue(!view.hidden, view.opacity);

				// Layer menu
				layer.menu.enableItems();

				// Filter
				if (layer.filterWindow && layer.filterWindow.isVisible()) {
					layer.filterWindow.filter();
				}
			}();
		};

		getView = function(config) {
			var in_ = dimConf.indicator.objectName,
                de = dimConf.dataElement.objectName,
                dc = dimConf.operand.objectName,
                ds = dimConf.dataSet.objectName,
                di = dimConf.eventDataItem.objectName;
                pi = dimConf.programIndicator.objectName,
                vType = valueType.getValue() === de ? dataElementDetailLevel.getValue() : valueType.getValue(),
				objectNameCmpMap = {},
				view = {};

			objectNameCmpMap[in_] = indicator;
			objectNameCmpMap[de] = dataElement;
			objectNameCmpMap[dc] = dataElement;
			objectNameCmpMap[ds] = dataSet;
			objectNameCmpMap[di] = eventDataItem;
			objectNameCmpMap[pi] = programIndicator;

            // id
            view.layer = layer.id;

            // dx
            if (objectNameCmpMap[vType].getValue()) {
                view.columns = [{
                    dimension: 'dx',
                    objectName: vType,
                    items: [{
                        id: objectNameCmpMap[vType].getValue()
                    }]
                }];
            }

            // program
            if (vType === di && eventDataItemProgram.getValue()) {
                view.program = {id: eventDataItemProgram.getValue()};
            }
            else if (vType === pi && programIndicatorProgram.getValue()) {
                view.program = {id: programIndicatorProgram.getValue()};
            }

            // ou
            if (treePanel.getDimension()) {
                view.rows = [treePanel.getDimension()];
            }

            // pe
            if (period.getValue()) {
                view.filters = [{
                    dimension: dimConf.period.objectName,
                    items: [{
                        id: period.getValue()
                    }]
                }];
            }

            // options
			view.classes = parseInt(classes.getValue());
			view.method = parseInt(method.getValue());
			view.colorLow = colorLow.getValue();
			view.colorHigh = colorHigh.getValue();
			view.radiusLow = parseInt(radiusLow.getValue());
			view.radiusHigh = parseInt(radiusHigh.getValue());
			view.opacity = layer.item.getOpacity();

            Ext.apply(view, labelPanel.getConfig());

			if (legendType.getValue() === gis.conf.finals.widget.legendtype_predefined && legendSet.getValue()) {
				view.legendSet = {
					id: legendSet.getValue()
				};
			}

            return gis.api.layout.Layout(view);
		};

        accordionBody = Ext.create('Ext.panel.Panel', {
			layout: 'accordion',
			activeOnTop: true,
			cls: 'ns-accordion',
			bodyStyle: 'border:0 none; margin-bottom:1px',
			height: 410,
			items: function() {
				var panels = [
					data,
					organisationUnit,
					legend,
                    label
				];

				last = panels[panels.length - 1];
				last.cls = 'ns-accordion-last';

				return panels;
			}(),
            listeners: {
                afterrender: function() { // nasty workaround
                    for (var i = accordionPanels.length - 1; i >= 0; i--) {
                        accordionPanels[i].expand();
                    }
                }
            }
		});

		accordion = Ext.create('Ext.panel.Panel', {
			bodyStyle: 'border-style:none; padding:1px; padding-bottom:0',
			items: accordionBody,
			panels: accordionPanels,

			map: layer.map,
			layer: layer,
			menu: layer.menu,

			reset: reset,
			setGui: setGui,
			getView: getView,
			getParentGraphMap: function() {
				return treePanel.getParentGraphMap();
			},

			infrastructuralDataElementValuesStore: infrastructuralDataElementValuesStore,
			setThisHeight: function(mx) {
                return 450;
				//var panelHeight = this.panels.length * 28,
					//height;

                //mx = mx || 0;

				//if (westRegion.hasScrollbar) {
					//height = panelHeight + mx;
					//this.setHeight(viewport.getHeight() - 2);
					//accordionBody.setHeight(height - 2);
				//}
				//else {
					//height = westRegion.getHeight() - ns.core.conf.layout.west_fill;
					//mx += panelHeight;
					//accordion.setHeight((height > mx ? mx : height) - 2);
					//accordionBody.setHeight((height > mx ? mx : height) - 2);
				//}
			},
			getExpandedPanel: function() {
				for (var i = 0, panel; i < this.panels.length; i++) {
					if (!this.panels[i].collapsed) {
						return this.panels[i];
					}
				}

				return null;
			},
			getFirstPanel: function() {
				return this.panels[0];
			},
			listeners: {
				added: function() {
					layer.accordion = this;
				},
				render: function() {
					toolMenu.clickHandler('level');
				}
			}
		});

		//createSelectHandlers();

		return accordion;
	};

	createViewport = function() {
		var centerRegion,
			eastRegion,
			downloadButton,
			shareButton,
            aboutButton,
			defaultButton,
			layersPanel,
			resizeButton,
			viewport,
			onRender,
			afterRender;

		resizeButton = Ext.create('Ext.button.Button', {
			text: '>>>',
			handler: function() {
				eastRegion.toggleCollapse();
			}
		});

		defaultButton = Ext.create('Ext.button.Button', {
			text: GIS.i18n.map,
			iconCls: 'gis-button-icon-map',
			toggleGroup: 'module',
			pressed: true,
            menu: {},
            handler: function(b) {
                b.menu = Ext.create('Ext.menu.Menu', {
                    closeAction: 'destroy',
                    shadow: false,
                    showSeparator: false,
                    items: [
                        {
                            text: GIS.i18n.clear_map + '&nbsp;&nbsp;', //i18n
                            cls: 'gis-menu-item-noicon',
                            handler: function() {
                                window.location.href = gis.init.contextPath + '/dhis-web-mapping';
                            }
                        }
                    ],
                    listeners: {
                        show: function() {
                            gis.util.gui.window.setAnchorPosition(b.menu, b);
                        },
                        hide: function() {
                            b.menu.destroy();
                            defaultButton.toggle();
                        },
                        destroy: function(m) {
                            b.menu = null;
                        }
                    }
                });

                b.menu.show();
            }
		});

		interpretationItem = Ext.create('Ext.menu.Item', {
			text: 'Write interpretation' + '&nbsp;&nbsp;',
			iconCls: 'gis-menu-item-tablelayout',
			disabled: true,
			xable: function() {
				if (gis.map) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
			handler: function() {
				if (viewport.interpretationWindow) {
					viewport.interpretationWindow.destroy();
					viewport.interpretationWindow = null;
				}

				viewport.interpretationWindow = GIS.app.InterpretationWindow();
				viewport.interpretationWindow.show();
			}
		});

		pluginItem = Ext.create('Ext.menu.Item', {
			text: 'Embed in web page' + '&nbsp;&nbsp;',
			iconCls: 'gis-menu-item-datasource',
			disabled: true,
			xable: function() {
				if (gis.util.map.hasVisibleFeatures()) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
			handler: function() {
				var textArea,
					window,
					text = '',
                    el = 'table1',
                    layout = gis.util.map.map2plugin(gis.util.layout.getPluginConfig()),
                    version = 'v' + parseFloat(gis.init.systemInfo.version.split('.').join(''));

                layout.el = el;

                if (layout.mapViews) {
                    for (var i = 0, view; i < layout.mapViews.length; i++) {
                        view = layout.mapViews[i];

                        if (view.legendSet) {
                            delete view.legendSet.bounds;
                            delete view.legendSet.colors;
                            delete view.legendSet.names;
                        }

                        if (!view.labels) {
                            delete view.labels;
                            delete view.labelFontSize;
                            delete view.labelFontWeight;
                            delete view.labelFontStyle;
                            delete view.labelFontColor;
                        }
                    }
                }

				text += '<html>\n<head>\n';
				text += '<link rel="stylesheet" href="http://dhis2-cdn.org/' + version + '/ext/resources/css/ext-plugin-gray.css" />\n';
				text += '<script src="http://dhis2-cdn.org/' + version + '/ext/ext-all.js"></script>\n';
				text += '<script src="http://dhis2-cdn.org/' + version + '/plugin/table.js"></script>\n';
				text += '</head>\n\n<body>\n';
				text += '<div id="' + el + '"></div>\n\n';
				text += '<script>\n\n';
				text += 'Ext.onReady(function() {\n\n';
				text += 'DHIS.getMap(' + JSON.stringify(layout, null, 2) + ');\n\n';
				text += '});\n\n';
				text += '</script>\n\n';
				text += '</body>\n</html>';

				textArea = Ext.create('Ext.form.field.TextArea', {
					width: 700,
					height: 400,
					readOnly: true,
					cls: 'ns-textarea monospaced',
					value: text
				});

				window = Ext.create('Ext.window.Window', {
                    title: 'Embed in web page' + (gis.map && gis.map.name ? '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + gis.map.name + '</span>' : ''),
					layout: 'fit',
					modal: true,
					resizable: false,
					items: textArea,
					destroyOnBlur: true,
					bbar: [
						'->',
						{
							text: 'Select',
							handler: function() {
								textArea.selectText();
							}
						}
					],
					listeners: {
						show: function(w) {
							this.setPosition(215, 33);

                            if (!w.hasDestroyOnBlurHandler) {
                                gis.util.gui.window.addDestroyOnBlurHandler(w);
                            }
						}
					}
				});

				window.show();
			}
		});

        favoriteUrlItem = Ext.create('Ext.menu.Item', {
			text: 'Favorite link' + '&nbsp;&nbsp;',
			iconCls: 'gis-menu-item-datasource',
			disabled: true,
			xable: function() {
				if (gis.map && gis.map.id) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
            handler: function() {
                var url = gis.init.contextPath + '/dhis-web-mapping/index.html?id=' + gis.map.id,
                    textField,
                    window;

                textField = Ext.create('Ext.form.field.Text', {
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>'
                });

				window = Ext.create('Ext.window.Window', {
                    title: 'Favorite link' + '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + gis.map.name + '</span>',
					layout: 'fit',
					modal: true,
					resizable: false,
					destroyOnBlur: true,
                    bodyStyle: 'padding: 12px 18px; background-color: #fff; font-size: 11px',
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>',
					listeners: {
						show: function(w) {
                            this.setPosition(325, 33);

                            if (!w.hasDestroyOnBlurHandler) {
                                gis.util.gui.window.addDestroyOnBlurHandler(w);
                            }

							document.body.oncontextmenu = true;
						},
						hide: function() {
							document.body.oncontextmenu = function(){return false;};
						}
					}
				});

				window.show();
            }
        });

        apiUrlItem = Ext.create('Ext.menu.Item', {
			text: 'API link' + '&nbsp;&nbsp;',
			iconCls: 'gis-menu-item-datasource',
			disabled: true,
			xable: function() {
				if (gis.map && gis.map.id) {
					this.enable();
				}
				else {
					this.disable();
				}
			},
            handler: function() {
                var url = gis.init.contextPath + '/api/maps/' + gis.map.id + '/data',
                    textField,
                    window;

                textField = Ext.create('Ext.form.field.Text', {
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>'
                });

				window = Ext.create('Ext.window.Window', {
                    title: 'API link' + '<span style="font-weight:normal">&nbsp;|&nbsp;&nbsp;' + gis.map.name + '</span>',
					layout: 'fit',
					modal: true,
					resizable: false,
					destroyOnBlur: true,
                    bodyStyle: 'padding: 12px 18px; background-color: #fff; font-size: 11px',
                    html: '<a class="user-select td-nobreak" target="_blank" href="' + url + '">' + url + '</a>',
					listeners: {
						show: function(w) {
                            this.setPosition(325, 33);

                            if (!w.hasDestroyOnBlurHandler) {
                                gis.util.gui.window.addDestroyOnBlurHandler(w);
                            }

							document.body.oncontextmenu = true;
						},
						hide: function() {
							document.body.oncontextmenu = function(){return false;};
						}
					}
				});

				window.show();
            }
        });

		shareButton = Ext.create('Ext.button.Button', {
			text: GIS.i18n.share,
            disabled: true,
			xableItems: function() {
				interpretationItem.xable();
				pluginItem.xable();
				favoriteUrlItem.xable();
				apiUrlItem.xable();
			},
			menu: {
				cls: 'gis-menu',
				shadow: false,
				showSeparator: false,
				items: [
					interpretationItem,
					pluginItem,
                    favoriteUrlItem,
                    apiUrlItem
				],
				listeners: {
					afterrender: function() {
						this.getEl().addCls('gis-toolbar-btn-menu');
					},
					show: function() {
						shareButton.xableItems();
					}
				}
			}
		});

		aboutButton = Ext.create('Ext.button.Button', {
			text: GIS.i18n.about,
            menu: {},
			handler: function() {
                if (viewport.aboutWindow && viewport.aboutWindow.destroy) {
					viewport.aboutWindow.destroy();
					viewport.aboutWindow = null;
				}

				viewport.aboutWindow = GIS.app.AboutWindow();
				viewport.aboutWindow.show();
			}
		});

		centerRegion = new GeoExt.panel.Map({
			region: 'center',
			map: gis.olmap,
			fullSize: true,
			cmp: [defaultButton],
            trash: [],
			toggleCmp: function(show) {
				for (var i = 0; i < this.cmp.length; i++) {
					if (show) {
						this.cmp[i].show();
					}
					else {
						this.cmp[i].hide();
					}
				}
			},
			tbar: {
				defaults: {
					height: 26
				},
				items: function() {
					var a = [];
					a.push({
						iconCls: 'gis-btn-icon-' + gis.layer.event.id,
						menu: gis.layer.event.menu,
						tooltip: GIS.i18n.event_layer,
						width: 26
					});
					a.push({
						iconCls: 'gis-btn-icon-' + gis.layer.facility.id,
						menu: gis.layer.facility.menu,
						tooltip: GIS.i18n.symbol_layer,
						width: 26
					});
					a.push({
						iconCls: 'gis-btn-icon-' + gis.layer.thematic1.id,
						menu: gis.layer.thematic1.menu,
						tooltip: GIS.i18n.thematic_layer + ' 1',
						width: 26
					});
					a.push({
						iconCls: 'gis-btn-icon-' + gis.layer.thematic2.id,
						menu: gis.layer.thematic2.menu,
						tooltip: GIS.i18n.thematic_layer + ' 2',
						width: 26
					});
					a.push({
						iconCls: 'gis-btn-icon-' + gis.layer.thematic3.id,
						menu: gis.layer.thematic3.menu,
						tooltip: GIS.i18n.thematic_layer + ' 3',
						width: 26
					});
					a.push({
						iconCls: 'gis-btn-icon-' + gis.layer.thematic4.id,
						menu: gis.layer.thematic4.menu,
						tooltip: GIS.i18n.thematic_layer + ' 4',
						width: 26
					});
					a.push({
						iconCls: 'gis-btn-icon-' + gis.layer.boundary.id,
						menu: gis.layer.boundary.menu,
						tooltip: GIS.i18n.boundary_layer,
						width: 26
					});
					a.push({
						text: GIS.i18n.favorites,
						menu: {},
						handler: function() {
							if (viewport.favoriteWindow && viewport.favoriteWindow.destroy) {
								viewport.favoriteWindow.destroy();
							}

							viewport.favoriteWindow = GIS.app.FavoriteWindow();
							viewport.favoriteWindow.show();
						}
					});

					if (gis.init.user.isAdmin) {
						a.push({
							text: GIS.i18n.legends,
							menu: {},
							handler: function() {
								if (viewport.legendSetWindow && viewport.legendSetWindow.destroy) {
									viewport.legendSetWindow.destroy();
								}

								viewport.legendSetWindow = GIS.app.LegendSetWindow();
								viewport.legendSetWindow.show();
							}
						});
					}
					a.push({
						xtype: 'tbseparator',
						height: 18,
						style: 'border-color: transparent #d1d1d1 transparent transparent; margin-right: 4px',
					});
					a.push({
						text: GIS.i18n.download,
						menu: {},
						disabled: true,
						handler: function() {
							if (viewport.downloadWindow && viewport.downloadWindow.destroy) {
								viewport.downloadWindow.destroy();
							}

							viewport.downloadWindow = GIS.app.DownloadWindow();
							viewport.downloadWindow.show();
						},
						xable: function() {
							if (gis.util.map.hasVisibleFeatures()) {
								this.enable();
							}
							else {
								this.disable();
							}
						},
						listeners: {
							added: function() {
								downloadButton = this;
							}
						}
					});
					a.push(shareButton);
					a.push('->');

					a.push({
						text: GIS.i18n.table,
						iconCls: 'gis-button-icon-table',
						toggleGroup: 'module',
						menu: {},
						handler: function(b) {
							b.menu = Ext.create('Ext.menu.Menu', {
								closeAction: 'destroy',
								shadow: false,
								showSeparator: false,
								items: [
									{
										text: GIS.i18n.go_to_pivot_tables + '&nbsp;&nbsp;',
										cls: 'gis-menu-item-noicon',
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = gis.init.contextPath + '/dhis-web-pivot';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(gis.init.contextPath + '/dhis-web-pivot', '_blank');
														}
													}
												});
											}
										}
									},
									'-',
									{
										text: GIS.i18n.open_this_map_as_table + '&nbsp;&nbsp;',
										cls: 'gis-menu-item-noicon',
										disabled: !(GIS.isSessionStorage && gis.util.layout.getAnalytical()),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled && GIS.isSessionStorage) {
                                                        gis.util.layout.setSessionStorage('analytical', gis.util.layout.getAnalytical());

														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = gis.init.contextPath + '/dhis-web-pivot/index.html?s=analytical';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(gis.init.contextPath + '/dhis-web-pivot/index.html?s=analytical', '_blank');
														}
													}
												});
											}
										}
									},
									{
										text: GIS.i18n.open_last_table + '&nbsp;&nbsp;',
										cls: 'gis-menu-item-noicon',
										disabled: !(GIS.isSessionStorage && JSON.parse(sessionStorage.getItem('dhis2')) && JSON.parse(sessionStorage.getItem('dhis2'))['table']),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
                                                            window.location.href = gis.init.contextPath + '/dhis-web-pivot/index.html?s=table';
                                                        }
                                                        else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
                                                            window.open(gis.init.contextPath + '/dhis-web-pivot/index.html?s=table', '_blank');
                                                        }
                                                    }
												});
											}
										}
									}
								],
								listeners: {
									show: function() {
										gis.util.gui.window.setAnchorPosition(b.menu, b);
									},
									hide: function() {
										b.menu.destroy();
										defaultButton.toggle();
									},
									destroy: function(m) {
										b.menu = null;
									}
								}
							});

							b.menu.show();
						},
						listeners: {
							render: function() {
								centerRegion.cmp.push(this);
							}
						}
					});

					a.push({
						text: GIS.i18n.chart,
						iconCls: 'gis-button-icon-chart',
						toggleGroup: 'module',
						menu: {},
						handler: function(b) {
							b.menu = Ext.create('Ext.menu.Menu', {
								closeAction: 'destroy',
								shadow: false,
								showSeparator: false,
								items: [
									{
										text: GIS.i18n.go_to_charts + '&nbsp;&nbsp;',
										cls: 'gis-menu-item-noicon',
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
                                                            window.location.href = gis.init.contextPath + '/dhis-web-visualizer';
                                                        }
                                                        else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
                                                            window.open(gis.init.contextPath + '/dhis-web-visualizer', '_blank');
                                                        }
                                                    }
												});
											}
										}
									},
									'-',
									{
										text: GIS.i18n.open_this_map_as_chart + '&nbsp;&nbsp;',
										cls: 'gis-menu-item-noicon',
										disabled: !GIS.isSessionStorage || !gis.util.layout.getAnalytical(),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
                                                    if (!b.disabled && GIS.isSessionStorage) {
                                                        gis.util.layout.setSessionStorage('analytical', gis.util.layout.getAnalytical());

														if (e.button === 0 && !e.ctrlKey) {
															window.location.href = gis.init.contextPath + '/dhis-web-visualizer/index.html?s=analytical';
														}
														else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
															window.open(gis.init.contextPath + '/dhis-web-visualizer/index.html?s=analytical', '_blank');
														}
													}
												});
											}
										}
									},
									{
										text: GIS.i18n.open_last_chart + '&nbsp;&nbsp;',
										cls: 'gis-menu-item-noicon',
										disabled: !(GIS.isSessionStorage && JSON.parse(sessionStorage.getItem('dhis2')) && JSON.parse(sessionStorage.getItem('dhis2'))['chart']),
										listeners: {
											render: function(b) {
												this.getEl().dom.addEventListener('click', function(e) {
													if (!b.disabled) {
														if (e.button === 0 && !e.ctrlKey) {
                                                            window.location.href = gis.init.contextPath + '/dhis-web-visualizer/index.html?s=chart';
                                                        }
                                                        else if ((e.ctrlKey && Ext.Array.contains([0,1], e.button)) || (!e.ctrlKey && e.button === 1)) {
                                                            window.open(gis.init.contextPath + '/dhis-web-visualizer/index.html?s=chart', '_blank');
                                                        }
                                                    }
												});
											}
										}
									}
								],
								listeners: {
									show: function() {
										gis.util.gui.window.setAnchorPosition(b.menu, b);
									},
									hide: function() {
										b.menu.destroy();
										defaultButton.toggle();
									},
									destroy: function(m) {
										b.menu = null;
									}
								}
							});

							b.menu.show();
						},
						listeners: {
							render: function() {
								centerRegion.cmp.push(this);
							}
						}
					});

					a.push(defaultButton);

					a.push({
						xtype: 'tbseparator',
						height: 18,
						style: 'border-color: transparent #d1d1d1 transparent transparent; margin-right: 6px; margin-left: 3px',
						listeners: {
							render: function() {
								centerRegion.cmp.push(this);
							}
						}
					});

                    a.push(aboutButton);

					a.push({
						xtype: 'button',
						text: GIS.i18n.home,
						handler: function() {
							window.location.href = '../dhis-web-commons-about/redirect.action';
						}
					});

					a.push(resizeButton);

					return a;
				}()
			},
            listeners: {
                render: function() {
                    var me = this;

                    me.getEl().on('mouseleave', function() {
                        for (var i = 0, cmp; i < me.trash.length; i++) {
                            cmp = me.trash[i];

                            if (cmp && cmp.destroy) {
                                cmp.destroy();
                            }
                        }
                    });
                },
                resize: function() {
                    var width = this.getWidth();

                    if (width < 800 && this.fullSize) {
                        this.toggleCmp(false);
                        this.fullSize = false;
                    }
                    else if (width >= 800 && !this.fullSize) {
                        this.toggleCmp(true);
                        this.fullSize = true;
                    }
                }
            }
		});

		eastRegion = Ext.create('Ext.panel.Panel', {
			region: 'east',
			layout: 'anchor',
			width: 200,
			preventHeader: true,
			collapsible: true,
			collapseMode: 'mini',
			items: function() {
				var a = [];

				layersPanel = GIS.app.LayersPanel();

				a.push({
					title: GIS.i18n.layer_stack_transparency,
					bodyStyle: 'padding: 3px 2px 2px 5px; border:0 none; border-bottom: 1px solid #d0d0d0; border-top: 1px solid #d0d0d0',
					style: 'border:0 none',
					items: layersPanel,
					collapsible: true,
					animCollapse: false
				});

				a.push({
					title: GIS.i18n.facility_layer_legend,
					bodyStyle: 'padding: 5px 6px 3px; border: 0 none; border-bottom: 1px solid #d0d0d0; border-top: 1px solid #d0d0d0',
					collapsible: true,
					collapsed: true,
					animCollapse: false,
					listeners: {
						added: function() {
							gis.layer.facility.legendPanel = this;
						}
					}
				});

				a.push({
					title: GIS.i18n.thematic_layer_1_legend,
					bodyStyle: 'padding: 4px 6px 6px; border: 0 none; border-bottom: 1px solid #d0d0d0; border-top: 1px solid #d0d0d0',
					collapsible: true,
					collapsed: true,
					animCollapse: false,
					listeners: {
						added: function() {
							gis.layer.thematic1.legendPanel = this;
						}
					}
				});

				a.push({
					title: GIS.i18n.thematic_layer_2_legend,
					bodyStyle: 'padding: 4px 6px 6px; border: 0 none; border-bottom: 1px solid #d0d0d0; border-top: 1px solid #d0d0d0',
					collapsible: true,
					collapsed: true,
					animCollapse: false,
					listeners: {
						added: function() {
							gis.layer.thematic2.legendPanel = this;
						}
					}
				});

				a.push({
					title: GIS.i18n.thematic_layer_3_legend,
					bodyStyle: 'padding: 4px 6px 6px; border: 0 none; border-bottom: 1px solid #d0d0d0',
					collapsible: true,
					collapsed: true,
					animCollapse: false,
					listeners: {
						added: function() {
							gis.layer.thematic3.legendPanel = this;
						}
					}
				});

				a.push({
					title: GIS.i18n.thematic_layer_4_legend,
					bodyStyle: 'padding: 4px 6px 6px; border: 0 none',
					collapsible: true,
					collapsed: true,
					animCollapse: false,
					listeners: {
						added: function() {
							gis.layer.thematic4.legendPanel = this;
						}
					}
				});

				return a;
			}(),
			listeners: {
				collapse: function() {
					resizeButton.setText('<<<');
				},
				expand: function() {
					resizeButton.setText('>>>');
				}
			}
		});

		onRender = function(vp) {
			gis.olmap.mask = Ext.create('Ext.LoadMask', centerRegion, {
				msg: 'Loading'
			});
		};

		afterRender = function() {

			// Map tools
			Ext.query('.zoomInButton')[0].innerHTML = '<img src="images/zoomin_24.png" />';
			Ext.query('.zoomOutButton')[0].innerHTML = '<img src="images/zoomout_24.png" />';
			Ext.query('.zoomVisibleButton')[0].innerHTML = '<img src="images/zoomvisible_24.png" />';
			Ext.query('.measureButton')[0].innerHTML = '<img src="images/measure_24.png" />';

			gis.olmap.events.register('click', null, function(e) {
				if (gis.olmap.relocate.active) {
					var el = Ext.query('#mouseposition')[0],
                        id = gis.olmap.relocate.feature.attributes.id,
						coordinates = '[' + el.childNodes[1].data + ',' + el.childNodes[3].data + ']',
						center = gis.viewport.centerRegion;

                    Ext.Ajax.request({
                        url: gis.init.contextPath + '/api/organisationUnits/' + id + '.json?links=false',
                        success: function(r) {
                            var orgUnit = Ext.decode(r.responseText);

                            orgUnit.coordinates = coordinates;

                            Ext.Ajax.request({
                                url: gis.init.contextPath + '/api/metaData?preheatCache=false',
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                params: Ext.encode({organisationUnits: [orgUnit]}),
                                success: function(r) {
                                    gis.olmap.relocate.active = false;
                                    gis.olmap.relocate.window.destroy();
                                    gis.olmap.relocate.feature.move({x: parseFloat(e.clientX - center.x), y: parseFloat(e.clientY - 28)});
                                    gis.olmap.getViewport().style.cursor = 'auto';

                                    console.log(gis.olmap.relocate.feature.attributes.name + ' relocated to ' + coordinates);
                                }
                            });
                        }
                    });
				}
			});

			// Favorite
			var id = gis.util.url.getUrlParam('id'),
				session = gis.util.url.getUrlParam('s'),
				base = gis.util.url.getUrlParam('base'),
				layout;

			if (id) {
				gis.map = {
					id: id
				};
				GIS.core.MapLoader(gis).load();
			}
			else if (Ext.isString(session) && GIS.isSessionStorage && Ext.isObject(JSON.parse(sessionStorage.getItem('dhis2'))) && session in JSON.parse(sessionStorage.getItem('dhis2'))) {
				layout = gis.api.layout.Layout(JSON.parse(sessionStorage.getItem('dhis2'))[session]);

				if (layout) {
					GIS.core.MapLoader(gis, true).load([layout]);
				}
			}

			if (base.length) {

				// hide base layer
				if (Ext.Array.contains(['false', 'none', 'no', 'off'], base)) {
					for (var i = 0, item; i < layersPanel.layerItems.length; i++) {
						item = layersPanel.layerItems[i];

						if (item.layer.layerType === gis.conf.finals.layer.type_base && item.layer.visibility) {
							item.disableItem();
						}
					}
				}
				else {
					var isEnabled = false;

					for (var i = 0, item; i < layersPanel.layerItems.length; i++) {
						item = layersPanel.layerItems[i];

						if (item.layer.layerType === gis.conf.finals.layer.type_base) {
							if (base === item.layer.id) {
								item.enableItem();
								isEnabled = true;
							}
							else {
								item.disableItem();
							}
						}
					}

					if (!isEnabled) {
						layersPanel.layerItems[layersPanel.layerItems.length - 1].enableItem();
					}
				}
			}

            // remove params from url
            if (id || session || base) {
                history.pushState(null, null, '.')
            }

            var initEl = document.getElementById('init');
            initEl.parentNode.removeChild(initEl);

            Ext.getBody().setStyle('background', '#fff');
            Ext.getBody().setStyle('opacity', 0);

            // fade in
            Ext.defer( function() {
                Ext.getBody().fadeIn({
                    duration: 600
                });
            }, 300 );
		};

		viewport = Ext.create('Ext.container.Viewport', {
			id: 'viewport',
			layout: 'border',
			eastRegion: eastRegion,
			centerRegion: centerRegion,
			downloadButton: downloadButton,
			shareButton: shareButton,
            aboutButton: aboutButton,
			layersPanel: layersPanel,
			items: [
				centerRegion,
				eastRegion
			],
			listeners: {
				render: function() {
					onRender(this);
				},
				afterrender: function() {
					afterRender();
				}
			}
		});

		return viewport;
	};

	initialize = function() {
		var requests = [],
			callbacks = 0,
			init = {
				user: {},
				systemSettings: {},
				extensions: {}
			},
            fn;

		fn = function() {
			if (++callbacks === requests.length) {

                // instance
				gis = GIS.core.getInstance(init);

                // ux
				GIS.app.createExtensions();

                // extend instance
				GIS.app.extendInstance(gis);

                // google maps
                var gm_fn = function() {
                    var googleStreets = new OpenLayers.Layer.Google('Google Streets', {
                        numZoomLevels: 20,
                        animationEnabled: true,
                        layerType: gis.conf.finals.layer.type_base,
                        layerOpacity: 1,
                        setLayerOpacity: function(number) {
                            if (number) {
                                this.layerOpacity = parseFloat(number);
                            }
                            this.setOpacity(this.layerOpacity);
                        }
                    });
                    googleStreets.id = 'googleStreets';
                    gis.layer.googleStreets = googleStreets;

                    var googleHybrid = new OpenLayers.Layer.Google('Google Hybrid', {
                        type: google.maps.MapTypeId.HYBRID,
                        useTiltImages: false,
                        numZoomLevels: 20,
                        animationEnabled: true,
                        layerType: gis.conf.finals.layer.type_base,
                        layerOpacity: 1,
                        setLayerOpacity: function(number) {
                            if (number) {
                                this.layerOpacity = parseFloat(number);
                            }
                            this.setOpacity(this.layerOpacity);
                        }
                    });
                    googleHybrid.id = 'googleHybrid';
                    gis.layer.googleHybrid = googleHybrid;

                    gis.olmap.addLayers([googleStreets, googleHybrid]);
                    gis.olmap.setBaseLayer(googleStreets);
                };

                if (GIS_GM.ready) {
                    console.log('GM is ready -> skip queue, add layers, set as baselayer');
                    gm_fn();
                }
                else {
                    if (GIS_GM.offline) {
                        console.log('Deactivate base layer');
                        gis.olmap.baseLayer.setVisibility(false);
                    }
                    else {
                        console.log('GM is not ready -> add to queue');
                        GIS_GM.array.push({
                            scope: this,
                            fn: gm_fn
                        });
                    }
                }

                // viewport
				gis.viewport = createViewport();
			}
		};

        // dhis2
        dhis2.util.namespace('dhis2.gis');

        dhis2.gis.store = dhis2.gis.store || new dhis2.storage.Store({
            name: 'dhis2',
            adapters: [dhis2.storage.IndexedDBAdapter, dhis2.storage.DomSessionStorageAdapter, dhis2.storage.InMemoryAdapter],
            objectStores: ['optionSets']
        });

        // inject google maps
        GIS_GM = {
            ready: false,
            array: [],
            offline: false,
            key: 'AIzaSyBjlDmwuON9lJbPMDlh_LI3zGpGtpK9erc'
        };

        GIS_GM_fn = function() {
            console.log("GM called back, queue length: " + GIS_GM.array.length);
            GIS_GM.ready = true;

            for (var i = 0, obj; i < GIS_GM.array.length; i++) {
                obj = GIS_GM.array[i];

                if (obj) {
                    console.log("GM running queue obj " + (i + 1));
                    obj.fn.call(obj.scope);
                }
            }
        };

        Ext.Loader.injectScriptElement('//maps.googleapis.com/maps/api/js?v=3.24&callback=GIS_GM_fn&key=' + GIS_GM.key,
            function() {
                console.log("GM available (online)");
            },
            function() {
                console.log("GM not available (offline)");
                GIS_GM.offline = true;
            }
        );

		// requests
		Ext.Ajax.request({
			url: 'manifest.webapp',
			success: function(r) {
				init.contextPath = Ext.decode(r.responseText).activities.dhis.href;

                // system info
                Ext.Ajax.request({
                    url: init.contextPath + '/api/system/info.json',
                    success: function(r) {
                        init.systemInfo = Ext.decode(r.responseText);
                        init.contextPath = init.systemInfo.contextPath || init.contextPath;

                        // date, calendar
                        Ext.Ajax.request({
                            url: init.contextPath + '/api/systemSettings.json?key=keyCalendar&key=keyDateFormat',
                            success: function(r) {
                                var systemSettings = Ext.decode(r.responseText);
                                init.systemInfo.dateFormat = Ext.isString(systemSettings.keyDateFormat) ? systemSettings.keyDateFormat.toLowerCase() : 'yyyy-mm-dd';
                                init.systemInfo.calendar = systemSettings.keyCalendar;

                                // user-account
                                Ext.Ajax.request({
                                    url: init.contextPath + '/api/me/user-account.json',
                                    success: function(r) {
                                        init.userAccount = Ext.decode(r.responseText);

                                        // init
                                        var defaultKeyUiLocale = 'en',
                                            defaultKeyAnalysisDisplayProperty = 'displayName',
                                            displayPropertyMap = {
                                                'name': 'displayName',
                                                'displayName': 'displayName',
                                                'shortName': 'displayShortName',
                                                'displayShortName': 'displayShortName'
                                            },
                                            namePropertyUrl,
                                            contextPath,
                                            keyUiLocale,
                                            dateFormat;

                                        init.userAccount.settings.keyUiLocale = init.userAccount.settings.keyUiLocale || defaultKeyUiLocale;
                                        init.userAccount.settings.keyAnalysisDisplayProperty = displayPropertyMap[init.userAccount.settings.keyAnalysisDisplayProperty] || defaultKeyAnalysisDisplayProperty;

                                        // local vars
                                        contextPath = init.contextPath;
                                        keyUiLocale = init.userAccount.settings.keyUiLocale;
                                        keyAnalysisDisplayProperty = init.userAccount.settings.keyAnalysisDisplayProperty;
                                        namePropertyUrl = keyAnalysisDisplayProperty + '|rename(name)';
                                        dateFormat = init.systemInfo.dateFormat;

                                        init.namePropertyUrl = namePropertyUrl;

                                        // calendar
                                        (function() {
                                            var dhis2PeriodUrl = '../dhis-web-commons/javascripts/dhis2/dhis2.period.js',
                                                defaultCalendarId = 'gregorian',
                                                calendarIdMap = {'iso8601': defaultCalendarId},
                                                calendarId = calendarIdMap[init.systemInfo.calendar] || init.systemInfo.calendar || defaultCalendarId,
                                                calendarIds = ['coptic', 'ethiopian', 'islamic', 'julian', 'nepali', 'thai'],
                                                calendarScriptUrl,
                                                createGenerator;

                                            // calendar
                                            createGenerator = function() {
                                                init.calendar = $.calendars.instance(calendarId);
                                                init.periodGenerator = new dhis2.period.PeriodGenerator(init.calendar, init.systemInfo.dateFormat);
                                            };

                                            if (Ext.Array.contains(calendarIds, calendarId)) {
                                                calendarScriptUrl = '../dhis-web-commons/javascripts/jQuery/calendars/jquery.calendars.' + calendarId + '.min.js';

                                                Ext.Loader.injectScriptElement(calendarScriptUrl, function() {
                                                    Ext.Loader.injectScriptElement(dhis2PeriodUrl, createGenerator);
                                                });
                                            }
                                            else {
                                                Ext.Loader.injectScriptElement(dhis2PeriodUrl, createGenerator);
                                            }
                                        }());

                                        // i18n
                                        requests.push({
                                            url: 'i18n/i18n_app.properties',
                                            success: function(r) {
                                                GIS.i18n = dhis2.util.parseJavaProperties(r.responseText);

                                                if (keyUiLocale === defaultKeyUiLocale) {
                                                    fn();
                                                }
                                                else {
                                                    Ext.Ajax.request({
                                                        url: 'i18n/i18n_app_' + keyUiLocale + '.properties',
                                                        success: function(r) {
                                                            Ext.apply(GIS.i18n, dhis2.util.parseJavaProperties(r.responseText));
                                                        },
                                                        failure: function() {
                                                            console.log('No translations found for system locale (' + keyUiLocale + ')');
                                                        },
                                                        callback: function() {
                                                            fn();
                                                        }
                                                    });
                                                }
                                            },
                                            failure: function() {
                                                Ext.Ajax.request({
                                                    url: 'i18n/i18n_app_' + keyUiLocale + '.properties',
                                                    success: function(r) {
                                                        GIS.i18n = dhis2.util.parseJavaProperties(r.responseText);
                                                    },
                                                    failure: function() {
                                                        alert('No translations found for system locale (' + keyUiLocale + ') or default locale (' + defaultKeyUiLocale + ').');
                                                    },
                                                    callback: fn
                                                });
                                            }
                                        });

                                        // root nodes
                                        requests.push({
                                            url: contextPath + '/api/organisationUnits.json?userDataViewFallback=true&paging=false&fields=id,' + namePropertyUrl + ',children[id,' + namePropertyUrl + ']',
                                            success: function(r) {
                                                init.rootNodes = Ext.decode(r.responseText).organisationUnits || [];
                                                fn();
                                            }
                                        });

                                        // organisation unit levels
                                        requests.push({
                                            url: contextPath + '/api/organisationUnitLevels.json?fields=id,displayName|rename(name),level&paging=false',
                                            success: function(r) {
                                                init.organisationUnitLevels = Ext.decode(r.responseText).organisationUnitLevels || [];

                                                if (!init.organisationUnitLevels.length) {
                                                    console.log('Info: No organisation unit levels defined');
                                                }

                                                fn();
                                            }
                                        });

                                        // user orgunits and children
                                        requests.push({
                                            url: contextPath + '/api/organisationUnits.json?userOnly=true&fields=id,' + namePropertyUrl + ',children[id,' + namePropertyUrl + ']&paging=false',
                                            success: function(r) {
                                                var organisationUnits = Ext.decode(r.responseText).organisationUnits || [],
                                                    ou = [],
                                                    ouc = [];

                                                if (organisationUnits.length) {
                                                    for (var i = 0, org; i < organisationUnits.length; i++) {
                                                        org = organisationUnits[i];

                                                        ou.push(org.id);

                                                        if (org.children) {
                                                            ouc = Ext.Array.clean(ouc.concat(Ext.Array.pluck(org.children, 'id') || []));
                                                        }
                                                    }

                                                    init.user = init.user || {};
                                                    init.user.ou = ou;
                                                    init.user.ouc = ouc;
                                                }
                                                else {
                                                    alert('User is not assigned to any organisation units');
                                                }

                                                fn();
                                            }
                                        });

                                        // admin
                                        requests.push({
                                            url: init.contextPath + '/api/me/authorization/F_GIS_ADMIN',
                                            success: function(r) {
                                                init.user.isAdmin = (r.responseText === 'true');
                                                fn();
                                            }
                                        });

                                        // indicator groups
                                        requests.push({
                                            url: init.contextPath + '/api/indicatorGroups.json?fields=id,displayName|rename(name)&paging=false',
                                            success: function(r) {
                                                init.indicatorGroups = Ext.decode(r.responseText).indicatorGroups || [];
                                                fn();
                                            }
                                        });

                                        // data element groups
                                        requests.push({
                                            url: init.contextPath + '/api/dataElementGroups.json?fields=id,' + namePropertyUrl + '&paging=false',
                                            success: function(r) {
                                                init.dataElementGroups = Ext.decode(r.responseText).dataElementGroups || [];
                                                fn();
                                            }
                                        });

                                        // infrastructural indicator group
                                        requests.push({
                                            url: init.contextPath + '/api/configuration/infrastructuralIndicators.json',
                                            success: function(r) {
                                                var obj = Ext.decode(r.responseText);
                                                init.systemSettings.infrastructuralIndicatorGroup = Ext.isObject(obj) ? obj : null;

                                                if (!Ext.isObject(obj)) {
                                                    Ext.Ajax.request({
                                                        url: init.contextPath + '/api/indicatorGroups.json?fields=id,displayName|rename(name),indicators[id,' + namePropertyUrl + ']&pageSize=1',
                                                        success: function(r) {
                                                            r = Ext.decode(r.responseText);
                                                            init.systemSettings.infrastructuralIndicatorGroup = r.indicatorGroups ? r.indicatorGroups[0] : null;
                                                        },
                                                        callback: fn
                                                    });
                                                }
                                                else {
                                                    fn();
                                                }
                                            }
                                        });

                                        // infrastructural data element group
                                        requests.push({
                                            url: init.contextPath + '/api/configuration/infrastructuralDataElements.json',
                                            success: function(r) {
                                                var obj = Ext.decode(r.responseText);
                                                init.systemSettings.infrastructuralDataElementGroup = Ext.isObject(obj) ? obj : null;

                                                if (!Ext.isObject(obj)) {
                                                    Ext.Ajax.request({
                                                        url: init.contextPath + '/api/dataElementGroups.json?fields=id,' + namePropertyUrl + ',dataElements[id,' + namePropertyUrl + ']&pageSize=1',
                                                        success: function(r) {
                                                            r = Ext.decode(r.responseText);
                                                            init.systemSettings.infrastructuralDataElementGroup = r.dataElementGroups ? r.dataElementGroups[0] : null;
                                                        },
                                                        callback: fn
                                                    });
                                                }
                                                else {
                                                    fn();
                                                }
                                            }
                                        });

                                        // infrastructural period type
                                        requests.push({
                                            url: init.contextPath + '/api/configuration/infrastructuralPeriodType.json',
                                            success: function(r) {
                                                var obj = Ext.decode(r.responseText);

                                                init.systemSettings.infrastructuralPeriodType = Ext.isObject(obj) ? obj : {id: 'Yearly', code: 'Yearly', name: 'Yearly'};
                                                fn();
                                            }
                                        });

                                        // option sets
                                        requests.push({
                                            url: '.',
                                            disableCaching: false,
                                            success: function() {
                                                var store = dhis2.gis.store;

                                                store.open().done( function() {

                                                    // check if idb has any option sets
                                                    store.getKeys('optionSets').done( function(keys) {
                                                        if (keys.length === 0) {
                                                            Ext.Ajax.request({
                                                                url: contextPath + '/api/optionSets.json?fields=id,displayName|rename(name),version,options[code,displayName|rename(name)]&paging=false',
                                                                success: function(r) {
                                                                    var sets = Ext.decode(r.responseText).optionSets;

                                                                    if (sets.length) {
                                                                        store.setAll('optionSets', sets).done(fn);
                                                                    }
                                                                    else {
                                                                        fn();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else {
                                                            Ext.Ajax.request({
                                                                url: contextPath + '/api/optionSets.json?fields=id,version&paging=false',
                                                                success: function(r) {
                                                                    var optionSets = Ext.decode(r.responseText).optionSets || [],
                                                                        ids = [],
                                                                        url = '',
                                                                        callbacks = 0,
                                                                        checkOptionSet,
                                                                        updateStore;

                                                                    updateStore = function() {
                                                                        if (++callbacks === optionSets.length) {
                                                                            if (!ids.length) {
                                                                                fn();
                                                                                return;
                                                                            }

                                                                            for (var i = 0; i < ids.length; i++) {
                                                                                url += '&filter=id:eq:' + ids[i];
                                                                            }

                                                                            Ext.Ajax.request({
                                                                                url: contextPath + '/api/optionSets.json?fields=id,displayName|rename(name),version,options[code,displayName|rename(name)]&paging=false' + url,
                                                                                success: function(r) {
                                                                                    var sets = Ext.decode(r.responseText).optionSets;

                                                                                    store.setAll('optionSets', sets).done(fn);
                                                                                }
                                                                            });
                                                                        }
                                                                    };

                                                                    registerOptionSet = function(optionSet) {
                                                                        store.get('optionSets', optionSet.id).done( function(obj) {
                                                                            if (!Ext.isObject(obj) || obj.version !== optionSet.version) {
                                                                                ids.push(optionSet.id);
                                                                            }

                                                                            updateStore();
                                                                        });
                                                                    };

                                                                    if (optionSets.length) {
                                                                        for (var i = 0; i < optionSets.length; i++) {
                                                                            registerOptionSet(optionSets[i]);
                                                                        }
                                                                    }
                                                                    else {
                                                                        fn();
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    });
                                                });
                                            }
                                        });

                                        for (var i = 0; i < requests.length; i++) {
                                            Ext.Ajax.request(requests[i]);
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
	}();
});
