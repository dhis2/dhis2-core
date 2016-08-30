"use strict";
/*
 * Copyright (c) 2004-2014, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Created by Mark Polak on 28/01/14.
 */
(function (dhis2, undefined) {
    var MAX_FAVORITES = 9,
        du = {
            isFunction: function(obj) {
                return Object.prototype.toString.call(obj) == '[object Function]';
            },
            isString: function (value) {
                if (typeof value === 'string' || value instanceof String) {
                    return true;
                }
                return false;
            },
            clone: function (sourceObj) {
                var x;
                var cloneObj = {};

                for (x in sourceObj) {
                    if (sourceObj.hasOwnProperty(x)) {
                        cloneObj[x] = sourceObj[x];
                    }
                }
                return cloneObj;
            }
        },
        getBaseUrl = (function () {
            var href;

            //Add window.location.origin for IE8
            if (!window.location.origin) {
                window.location.origin = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
            }

            href = window.location.origin;

            return function () {
                var urlParts = href.split("/"),
                    baseUrl;

                if (dhis2.settings.baseUrl === undefined) {
                    return "..";
                }

                if (typeof dhis2.settings.baseUrl !== "string") {
                    throw new TypeError("Dhis2 settings: baseUrl should be a string");
                }

                if (urlParts[urlParts.length - 1] !== "") {
                    baseUrl = href + '/' + dhis2.settings.baseUrl;
                } else {
                    urlParts.pop();
                    urlParts.push(dhis2.settings.baseUrl);
                    baseUrl = urlParts.join('/');
                }
                return baseUrl;
            }
        })(),
        /**
         * Adjusts the url to include the baseUrl
         *
         * @param iconUrl
         * @returns {String}
         */
         fixUrlIfNeeded = function (iconUrl) {
            if (iconUrl.substring(0, 2) === "..") {
                return getBaseUrl() + iconUrl.substring(2, iconUrl.length);
            }
            return iconUrl;
        },
        /**
         * Object that represents the list of menu items
         * and managers the order of the items to be saved.
         */
         menuItemsList = function () {
            var menuOrder = [];
            var menuItems = {};
            var filterFunction = function (item, key) {
                return true;
            };

            return {
                getItem: function (key) {
                    return menuItems[key];
                },
                setItem: function (key, item) {
                    menuOrder.push(key);
                    menuItems[key] = item;
                },
                list: function () {
                    var result = [];
                    var filtered = [];

                    menuOrder.forEach(function (element, index, array) {
                        if (filterFunction(menuItems[element], menuItems[element].id)) {
                            result.push(menuItems[element]);
                        }
                    });

                    return result;
                },
                setOrder: function (order) {
                    menuOrder = order;
                },
                getOrder: function () {
                    return menuOrder;
                },
                addFilter: function (filter) {
                    if (du.isFunction(filter)) {
                        filterFunction = function (item, key, items) {
                            if (filter(du.clone(item), key)) {
                                return true;
                            }
                            return false;
                        };
                    }
                }
            }
        };
    var menus = {};
    dhis2.menu = {};

    dhis2.menu = function (nameKey, preLoadedData) {
        var that = {},
            menuReady = false,
            menuItems = menuItemsList(),
            callBacks = [], //Array of callbacks to call when serviced is updated
            onceCallBacks = [];

        /***********************************************************************
         * Private methods
         **********************************************************************/

        function processTranslations(translations) {
            var items = that.getApps();

            that.name = translations[nameKey];

            items.forEach(function (element, index, items) {
                if (element.id && translations[element.id]) {
                    items[index].name = translations.get(element.id);
                }
                if (element.description === '' && translations.get('intro_' + element.id) !== 'intro_' + element.id){
                    element.description = translations['intro_' + element.id];
                }
            });

            setReady();
        }

        function setReady() {
            menuReady = true;
            executeCallBacks();
        }

        function isReady() {
            return menuReady;
        }

        /**
         * Execute any callbacks that are set onto the callbacks array
         */
        function executeCallBacks() {
            var onceCallBack;

            //If not ready or no menu items
            if ( ! isReady() || menuItems === {})
                return false;

            //Execute the single time callbacks
            while (onceCallBacks.length !== 0) {
                onceCallBack = onceCallBacks.pop();
                onceCallBack.apply(that, [that]);
            }
            callBacks.forEach(function (callback, index, callBacks) {
                callback.apply(that, [that]);
            });
        }

        //TODO: Function seems complicated and can be improved perhaps
        /**
         * Sort apps (objects with a name property) by name
         *
         * @param apps
         * @param inverse {boolean} Return the elements in an inverted order (DESC sort)
         * @returns {Array}
         */
        function sortAppsByName (apps, inverse) {
            var smaller = [],
                bigger = [],
                center = Math.floor(apps.length / 2),
                comparisonResult,
                result;

            //If nothing left to sort return the app list
            if (apps.length <= 1)
                return apps;

            center = apps[center];
            apps.forEach(function (app, index, apps) {
                comparisonResult = center.name.localeCompare(app.name);
                if (comparisonResult <= -1) {
                    bigger.push(app);
                }
                if (comparisonResult >= 1) {
                    smaller.push(app);
                }
            });

            smaller = sortAppsByName(smaller);
            bigger = sortAppsByName(bigger);

            result = smaller.concat([center]).concat(bigger);

            return inverse ? result.reverse() : result;
        }

        /***********************************************************************
         * Public methods
         **********************************************************************/

        that.id = nameKey;
        that.name = nameKey;
        that.displayOrder = 'custom';

        that.getMenuItems = function () {
            return menuItems;
        };

        /**
         * Get the max number of favorites
         */
        that.getMaxFavorites = function () {
            return MAX_FAVORITES;
        };

        /**
         * Order the menuItems by a given list
         *
         * @param orderedIdList
         * @returns {{}}
         */
        that.orderMenuItemsByList = function (orderedIdList) {
            menuItems.setOrder(orderedIdList);

            executeCallBacks();

            return that;
        };

        that.updateFavoritesFromList = function (orderedIdList) {
            var newFavsIds = orderedIdList.slice(0, MAX_FAVORITES),
                oldFavsIds = menuItems.getOrder().slice(0, MAX_FAVORITES),
                currentOrder = menuItems.getOrder(),
                newOrder;

            //Take the new favorites as the new order
            newOrder = newFavsIds;

            //Find the favorites that were pushed out and add  them to the list on the top of the order
            oldFavsIds.forEach(function (id, index, ids) {
                if (-1 === newFavsIds.indexOf(id)) {
                    newOrder.push(id);
                }
            });

            //Loop through the remaining current order to add the remaining apps to the new order
            currentOrder.forEach(function (id, index, ids) {
                //Add id to the order when it's not already in there
                if (-1 === newOrder.indexOf(id)) {
                    newOrder.push(id);
                }
            });

            menuItems.setOrder(newOrder);

            executeCallBacks();

            return that;
        };

        /**
         * Adds the menu items given to the menu
         */
        that.addMenuItems = function (items) {
            var keysToTranslate = [];

            //Add the name of the menu to the translationList
            keysToTranslate.push(nameKey);

            items.forEach(function (item, index, items) {
                item.id = item.name;
                keysToTranslate.push(item.name);
                if(item.description === "") {
                    keysToTranslate.push("intro_" + item.name);
                }

                item.defaultAction = fixUrlIfNeeded(item.defaultAction);
                item.icon = fixUrlIfNeeded(item.icon);

                menuItems.setItem(item.id, item);
            });

            dhis2.translate.get(keysToTranslate, processTranslations);
        };

        /**
         * Subscribe to the service
         *
         * @param callback {function} Function that should be run when service gets updated
         * @param onlyOnce {boolean} Callback should only be run once on the next update
         * @returns boolean Returns false when callback is not a function
         */
        that.subscribe = function (callback, onlyOnce) {
            var once = onlyOnce ? true : false;

            if ( ! du.isFunction(callback)) {
                setTimeout(executeCallBacks, 300);
                return false;
            }

            if (isReady() && (menuItems !== undefined)) {
                callback(that);
            }

            if (true === once) {
                onceCallBacks.push(callback);
            } else {
                callBacks.push(callback);
            }
            return true;
        };

        that.notify = function () {
            executeCallBacks();
        };

        /**
         * Get the favorite apps
         *
         * @returns {Array}
         */
        that.getFavorites = function () {
            return menuItems.list().slice(0, MAX_FAVORITES);
        };

        /**
         * Get the current menuItems
         */
        that.getApps = function () {
            return menuItems.list();
        };

        /**
         * Get non favorite apps
         */
        that.getNonFavoriteApps = function () {
            return menuItems.list().slice(MAX_FAVORITES);
        };

        that.sortNonFavAppsByName = function (inverse) {
            return sortAppsByName(that.getNonFavoriteApps(), inverse);
        };

        /**
         * Gets the applist based on the current display order
         *
         * @returns {Array} Array of app objects
         */
        that.getOrderedAppList = function () {
            var favApps = that.getFavorites(),
                nonFavApps = that.getNonFavoriteApps();
            switch (that.displayOrder) {
                case 'name-asc':
                    nonFavApps = that.sortNonFavAppsByName();
                    break;
                case 'name-desc':
                    nonFavApps = that.sortNonFavAppsByName(true);
                    break;
            }
            return favApps.concat(nonFavApps);;
        };

        that.updateOrder = function (reorderedApps) {
            switch (that.displayOrder) {
                case 'name-asc':
                case 'name-desc':
                    that.updateFavoritesFromList(reorderedApps);
                    break;

                default:
                    //Update the menu object with the changed order
                    that.orderMenuItemsByList(reorderedApps);
                    break;
            }
        };

        that.save = function (saveMethod) {
            if ( ! du.isFunction(saveMethod)) {
                return false;
            }

            return saveMethod(that.getMenuItems().getOrder());
        };

        that.search = function (searchFor) {
            //Get all the apps
            var menuItems = that.getApps(),
                searchMatches = [];

            //Find the matches
            menuItems.forEach(function (menuItem) {
                var menuItemName = menuItem.name.toLowerCase(),
                    searchScore = menuItemName.indexOf(searchFor);

                if (searchScore !== -1) {
                    menuItem.searchScore = searchScore;
                    searchMatches.push(menuItem);
                }
            });

            //Order the search matches on occurrence
            searchMatches.sort(function (a, b) {
                if (a.searchScore < b.searchScore)
                    return -1;
                if (a.searchScore > b.searchScore)
                    return 1;
                return 0;
            });

            return searchMatches;
        };

        if (typeof preLoadedData === 'object') {
            that.addMenuItems(preLoadedData);
        }

        menus[nameKey] = that;

        return that;
    };

    //The following are dhis2.menu "Api" functions that let you interact with the menu.
    /**
     * Add a filter to the menu that has the id nameKey.
     *
     * @param {String} nameKey Id string of the menu
     * @param {Function} filterFunction Function that is called for every menu element, should return true for items
     *                                  that should be in the menu, otherwise false. The filterFunction receives a copy
     *                                  of the menuItem as the first parameter and the menuItemId as the second.
     *
     *
     * @returns {boolean} Returns true if the filter function was added to the specified menu. Returns false when either
     *                    nameKey was not a string or filterFunction not a function.
     */
    dhis2.menu.filter = function (nameKey, filterFunction) {
        if (du.isString(nameKey) && du.isFunction(filterFunction)) {
            menus[nameKey].getMenuItems().addFilter(filterFunction);
            menus[nameKey].notify();
            return true;
        }
        return false;
    };

    /**
     * Returns the name keys for the current menus.
     *
     * @returns {Object} Returns an object with a property that contains the nameKey for each menu and has an array of
     *                   menuItem ids.
     */
    dhis2.menu.getNameKeysForMenus = function () {
        var nameKeys = {};
        var menuId;
        var tempMenu;
        for (menuId in menus) {
            if (menus.hasOwnProperty(menuId)) {
                nameKeys[menuId] = menus[menuId].getMenuItems().getOrder();
            }
        }
        return nameKeys;
    };

    //Expose the fixUrl method so we can use externally
    dhis2.menu.fixUrlIfNeeded = fixUrlIfNeeded;
})(dhis2 = dhis2 || {});
