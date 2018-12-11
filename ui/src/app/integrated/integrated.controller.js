/*
 * Copyright © 2016-2018 The BeiDouApp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* eslint-disable */
import './integrated.scss';
import Subscription from '../api/subscription';

import Cesium from 'cesium/Cesium';
import "cesium/Widgets/widgets.css";
// import "cesium/Widgets/lighter.css";

/*@ngInject*/
export default function IntegratedController(
    $scope, $filter, $mdMedia, $q, menu, $state, $timeout, 
    $log, tbRaf, types, utils, timeService,
    datasourceService, alarmService,  dashboardService, deviceService
    ) {
       
    var vm = this;

    vm.sectionColspan = sectionColspan;
    vm.openDashboard = openDashboard;
    // $rootScope.forceFullscreen = !$rootScope.forceFullscreen;
    // $scope.searchConfig.searchEnabled = true;
    vm.isLibraryOpen = true;

    var viewer = new Cesium.Viewer('cesiumContainer');
    // var tileset = viewer.scene.primitives.add(new Cesium.Cesium3DTileset({
    // 	url : '../tilesets/TilesetWithDiscreteLOD/tileset.json'
    // }));

    $scope.$watch(function () { return $mdMedia('lg'); }, function () {
        updateColumnCount();
    });

    $scope.$watch(function () { return $mdMedia('gt-lg'); }, function () {
        updateColumnCount();
    });

    updateColumnCount();

    vm.model = menu.getHomeSections();

    /*订阅开始*/
    var integratedCtx = {
        subscriptions: {},
        subscriptionApi: {
            createSubscription: function (options, subscribe) {
                return createSubscription(options, subscribe);
            },
            removeSubscription: function (id) {
                var subscription = integratedCtx.subscriptions[id];
                if (subscription) {
                    subscription.destroy();
                    delete integratedCtx.subscriptions[id];
                }
            }
        }
    };
    var deferred = $q.defer();

    var subscriptionContext = {
        $scope: $scope,
        $q: $q,
        $filter: $filter,
        $timeout: $timeout,
        tbRaf: tbRaf,
        timeService: timeService,
        deviceService: deviceService,
        datasourceService: datasourceService,
        alarmService: alarmService,
        utils: utils,
        // widgetUtils: widgetContext.utils,
        // dashboardTimewindowApi: dashboardTimewindowApi,
        types: types,
        getStDiff: dashboardService.getServerTimeDiff,
        // aliasController: aliasController
    };
    var subscriptionOptions = {
        datasources: [],
        callbacks: {
            onDataUpdated: onDataUpdated,
            onDataUpdateError: onDataUpdateError
        }
    };

    createSubscription(subscriptionOptions, true).then(
        function success(subscription) {
            integratedCtx.defaultSubscription = subscription;
            $log.info('AWEN-->subscription is created in integratedCtx.defaultSubscription');
            deferred.resolve();
        },
        function fail() {
            deferred.reject();
        }
    );

    function createSubscription(options, subscribe) {
        var deferred = $q.defer();
        // options.dashboardTimewindow = vm.dashboardTimewindow;
        new Subscription(subscriptionContext, options).then(
            function success(subscription) {
                integratedCtx.subscriptions[subscription.id] = subscription;
                if (subscribe) {
                    subscription.subscribe();
                }
                deferred.resolve(subscription);
            },
            function fail() {
                deferred.reject();
            }
        );
        return deferred.promise;
    }

    function onDataUpdated(subscription, apply) {
        // var value = false;
        var data = subscription.data;
        if (data.length) {
            $log.log('AWEN-->DATA');
            // var keyData = data[0];
            // if (keyData && keyData.data && keyData.data[0]) {
            //     var attrValue = keyData.data[0][1];
            //     if (attrValue) {
            //         var parsed = null;
            //         try {
            //             parsed = vm.parseValueFunction(angular.fromJson(attrValue));
            //         } catch (e){/**/}
            //         value = parsed ? true : false;
            //     }
            // }
        }
        /*
        setValue(value);
        if (apply) {
            $scope.$digest();
        }
        */
    }

    function onDataUpdateError(subscription, e) {
        $log.error('AWEN-->ERR');
        // var exceptionData = utils.parseException(e);
        // var errorText = exceptionData.name;
        // if (exceptionData.message) {
        //     errorText += ': ' + exceptionData.message;
        // }
        // onError(errorText);
    }

    /*订阅结束*/

    // dashboard id---> d96c1d40-de3e-11e8-90b2-6708b10d2f5b 战略情报
    // dashboard id---> af5dadb0-e5ba-11e8-be95-f3713e6700c3 桥梁数据展示
    function openDashboard(dashboardID) {
        $state.go('home.integrated.dashboard', { dashboardId: dashboardID });
    }

    function updateColumnCount() {
        vm.cols = 2;
        if ($mdMedia('lg')) {
            vm.cols = 3;
        }
        if ($mdMedia('gt-lg')) {
            vm.cols = 4;
        }
    }

    function sectionColspan(section) {
        var colspan = vm.cols;
        if (section && section.places && section.places.length <= colspan) {
            colspan = section.places.length;
        }
        return colspan;
    }
}
