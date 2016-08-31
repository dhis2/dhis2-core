Ext.onReady( function() {
	var N = PT;

    // UiManager
    (function() {
        var UiManager = function() {
            var t = this;

            // uninitialized
            t.viewport;
            t.menuRegion;

            // support
            t.getScrollbarSize = function(force) {
                var size,
                    body = document.body,
                    div = document.createElement('div');

                div.style.width = div.style.height = '100px';
                div.style.overflow = 'scroll';
                div.style.position = 'absolute';

                body.appendChild(div);

                size = {
                    width: div.offsetWidth - div.clientWidth,
                    height: div.offsetHeight - div.clientHeight
                };

                body.removeChild(div);

                return size;
            };
        };

        UiManager.addViewport = function(viewport) {
            this.viewport = viewport;
        };

        UiManager.addMenuRegion = function(viewport) {
            this.viewport = viewport;
        };

        N.UiManager = new UiManager();
    })();

    // UiViewport
    (function() {

        // UiMenuRegion

        var UiMenuAccordion = function(cmp, config) {
            var t = this;

            config = N.isObject(config) ? config : {};

            // constants
            var tabHeight = config.tabHeight || 28;

            // constructor
            $.extend(this, cmp);
        };

        //UiMenuAccordion.prototype.thisSetHeight = function(mx) {
            //var panelHeight = this.panels.length * 28,
                //height;

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

        // UiMenuRegion

        var UiMenuRegion = function(config) {
            var t = this;

            config = N.isObject(config) ? config : {};

            // constants
            var width = config.width || 424;

            // constructor
            $.extend(this, Ext.create('Ext.panel.Panel', {
                region: 'west',
                preventHeader: true,
                collapsible: true,
                collapseMode: 'mini',
                border: false,
                width: width + N.UiManager.getScrollbarSize().width,
                items: new N.UiMenuAccordion()
            }));
        };

        // UiViewport

        var UiViewport = function(config) {
            var t = this;

            config = N.isObject(config) ? config : {};

            // local
            var menuRegion = new UiMenuRegion();

            // constructor
            $.extend(this, Ext.create('Ext.container.Viewport', {
                layout: 'border',
                //period: period,
                //treePanel: treePanel,
                //setGui: setGui,
                //westRegion: westRegion,
                //centerRegion: centerRegion,
                items: [
                    menuRegion,
                    Ext.create('Ext.panel.Panel', {
                        region: 'center'
                    })
                ],
                listeners: {
                    //render: function() {
                        //ns.app.viewport = this;

                        //ns.app.layoutWindow = LayoutWindow();
                        //ns.app.layoutWindow.hide();

                        //ns.app.optionsWindow = OptionsWindow();
                        //ns.app.optionsWindow.hide();
                    //},
                    afterrender: function() {

                        // resize event handler
                        westRegion.on('resize', function() {
                            var panel = accordion.getExpandedPanel();

                            if (panel) {
                                panel.onExpand();
                            }
                        });

                        // left gui
                        var viewportHeight = westRegion.getHeight(),
                            numberOfTabs = ns.core.init.dimensions.length + 3,
                            tabHeight = 28,
                            minPeriodHeight = 380;

                        if (viewportHeight > numberOfTabs * tabHeight + minPeriodHeight) {
                            if (!Ext.isIE) {
                                accordion.setAutoScroll(false);
                                westRegion.setWidth(ns.core.conf.layout.west_width);
                                accordion.doLayout();
                            }
                        }
                        else {
                            westRegion.hasScrollbar = true;
                        }

                        // expand first panel
                        accordion.getFirstPanel().expand();

                        // look for url params
                        var id = ns.core.web.url.getParam('id'),
                            session = ns.core.web.url.getParam('s'),
                            layout;

                        if (id) {
                            ns.core.web.pivot.loadTable(id);
                        }
                        else if (Ext.isString(session) && NS.isSessionStorage && Ext.isObject(JSON.parse(sessionStorage.getItem('dhis2'))) && session in JSON.parse(sessionStorage.getItem('dhis2'))) {
                            layout = ns.core.api.layout.Layout(JSON.parse(sessionStorage.getItem('dhis2'))[session]);

                            if (layout) {
                                ns.core.web.pivot.getData(layout, true);
                            }
                        }

                        // remove params from url
                        if (id || session) {
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
                    }
                }
            });

            N.UiManager.setMenuRegion(menuRegion);

            N.UiViewport = new UiViewport();
        };
    })();

    // initialize
    (function() {
        var appManager = N.AppManager,
            calendarManager = N.CalendarManager,
            requestManager = new N.Api.RequestManager(),
            manifestReq = $.getJSON('manifest.webapp'),
            systemInfoReq = $.getJSON('/api/system/info.json'),
            systemSettingsReq = $.getJSON('/api/systemSettings.json?key=keyCalendar&key=keyDateFormat&key=keyAnalysisRelativePeriod&key=keyHideUnapprovedDataInAnalytics'),
            userAccountReq = $.getJSON('/api/me/user-account.json');

        manifestReq.done(function(manifest) {
            appManager.manifest = manifest;

        systemInfoReq.done(function(systemInfo) {
            appManager.systemInfo = systemInfo;
            appManager.path = systemInfo.contextPath;

        systemSettingsReq.done(function(systemSettings) {
            appManager.systemSettings = systemSettings;

        userAccountReq.done(function(userAccount) {
            appManager.userAccount = userAccount;

            calendarManager.setBaseUrl(appManager.getPath());
            calendarManager.setDateFormat(appManager.getDateFormat());
            calendarManager.generate(appManager.systemSettings.keyCalendar);

        // requests
        (function() {
            var uiLocale = appManager.getUiLocale(),
                displayProperty = appManager.getDisplayProperty(),
                path = appManager.getPath();

            // i18n
            requestManager.add(new N.Api.Request({
                baseUrl: 'i18n/i18n_app.properties',
                type: 'ajax',
                success: function(r) {
                    var t = this;

                    N.I18nManager.add(dhis2.util.parseJavaProperties(r));

                    if (appManager.isUiLocaleDefault()) {
                        requestManager.ok(t);
                    }
                    else {
                        $.ajax({
                            url: 'i18n/i18n_app_' + uiLocale + '.properties',
                            success: function(r) {
                                N.I18nManager.add(dhis2.util.parseJavaProperties(r));
                            },
                            error: function() {
                                console.log('(i18n) No translations found for system locale (' + uiLocale + ')');
                            },
                            complete: function() {
                                requestManager.ok(t);
                            }
                        });
                    }
                },
                error: function() {
                    $.ajax({
                        url: 'i18n/i18n_app_' + uiLocale + '.properties',
                        success: function(r) {
                            N.I18nManager.add(dhis2.util.parseJavaProperties(r));
                        },
                        error: function() {
                            alert('(i18n) No translations found for system locale (' + uiLocale + ') or default locale (' + appManager.defaultUiLocale + ')');
                        },
                        complete: function() {
                            requestManager.ok(this);
                        }
                    });
                }
            }));

            // authorization
            requestManager.add(new N.Api.Request({
                baseUrl: path + '/api/me/authorization/F_VIEW_UNAPPROVED_DATA',
                success: function(r) {
                    appManager.viewUnapprovedData = r;
                    requestManager.ok(this);
                }
            }));

            // root nodes
            requestManager.add(new N.Api.Request({
                baseUrl: path + '/api/organisationUnits.json',
                params: [
                    'userDataViewFallback=true',
                    'fields=id,' + displayProperty + ',children[id,' + displayProperty + ']',
                    'paging=false'
                ],
                success: function(r) {
                    appManager.addRootNodes(r.organisationUnits);
                    requestManager.ok(this);
                }
            }));

            // organisation unit levels
            requestManager.add(new N.Api.Request({
                baseUrl: path + '/api/organisationUnitLevels.json',
                params: [
                    'fields=id,' + displayProperty + 'level',
                    'paging=false'
                ],
                success: function(r) {
                    appManager.addOrganisationUnitLevels(r.organisationUnitLevels);

                    if (!r.organisationUnitLevels.length) {
                        alert('No organisation unit levels found');
                    }

                    requestManager.ok(this);
                }
            }));

            // legend sets
            requestManager.add(new N.Api.Request({
                baseUrl: path + '/api/legendSets.json',
                params: [
                    'fields=id,' + displayProperty + ',legends[id,' + displayProperty + ',startValue,endValue,color]',
                    'paging=false'
                ],
                success: function(r) {
                    appManager.addLegendSets(r.legendSets);
                    requestManager.ok(this);
                }
            }));

            // dimensions
            requestManager.add(new N.Api.Request({
                baseUrl: path + '/api/dimensions.json',
                params: [
                    'fields=id,' + displayProperty,
                    'paging=false'
                ],
                success: function(r) {
                    appManager.addDimensions(r.dimensions);
                    requestManager.ok(this);
                }
            }));

            // approval levels
            requestManager.add(new N.Api.Request({
                baseUrl: path + '/api/dataApprovalLevels.json',
                params: [
                    'order=level:asc',
                    'fields=id,' + displayProperty + ',level',
                    'paging=false'
                ],
                success: function(r) {
                    appManager.addDataApprovalLevels(r.dataApprovalLevels);
                    requestManager.ok(this);
                }
            }));
        })();

        //requestManager.set(function() {

        requestManager.run();

        });
        });
        });
        });
    })();
});
