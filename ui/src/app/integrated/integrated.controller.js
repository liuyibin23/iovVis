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
import "cesium/Widgets/lighter.css";

/*@ngInject*/
export default function IntegratedController($scope, $filter, $mdMedia, $q, menu, $state) {

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
    
    $scope.$watch(function() { return $mdMedia('lg'); }, function() {
        updateColumnCount();
    });

    $scope.$watch(function() { return $mdMedia('gt-lg'); }, function() {
        updateColumnCount();
    });

    updateColumnCount();

    vm.model = menu.getHomeSections();    

    var integratedCtx = { 
        subscriptions: {},
        subscriptionApi: {
            createSubscription: function(options, subscribe) {
                return createSubscription(options, subscribe);
            },
            removeSubscription: function(id) {
                var subscription = integratedCtx.subscriptions[id];
                if (subscription) {
                    subscription.destroy();
                    delete integratedCtx.subscriptions[id];
                }
            }
        }
    };

    var subscriptionContext = {
        $scope: $scope,
        $q: $q,
        $filter: $filter,
        // $timeout: $timeout,
        // tbRaf: tbRaf,
        // timeService: timeService,
        // deviceService: deviceService,
        // datasourceService: datasourceService,
        // alarmService: alarmService,
        // utils: utils,
        // widgetUtils: widgetContext.utils,
        // dashboardTimewindowApi: dashboardTimewindowApi,
        // types: types,
        // getStDiff: dashboardService.getServerTimeDiff,
        // aliasController: aliasController
    };

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

    // dashboard id---> d96c1d40-de3e-11e8-90b2-6708b10d2f5b 战略情报
    // dashboard id---> af5dadb0-e5ba-11e8-be95-f3713e6700c3 桥梁数据展示
    function openDashboard(dashboardID) {
        $state.go('home.integrated.dashboard', {dashboardId: dashboardID});
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
