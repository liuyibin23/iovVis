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
import $ from 'jquery';

/* eslint-disable import/no-unresolved, import/default */
import logoSvg from '../../svg/logo_title_white.svg';
import Cesium from 'cesium/Cesium';
import "cesium/Widgets/widgets.css";
import "cesium/Widgets/lighter.css";
/* eslint-enable import/no-unresolved, import/default */

/* eslint-disable angular/angularelement */

/*@ngInject*/
export default function HomeController(types, loginService, userService, deviceService, Fullscreen, $scope, $element, $rootScope, $document, $state,
    $window, $log, $mdMedia, $animate, $timeout) {

    var siteSideNav = $('.tb-site-sidenav', $element);

    var vm = this;

    vm.Fullscreen = Fullscreen;
    vm.logoSvg = logoSvg;

    if (angular.isUndefined($rootScope.searchConfig)) {
        $rootScope.searchConfig = {
            searchEnabled: false,
            searchByEntitySubtype: false,
            searchEntityType: null,
            showSearch: false,
            searchText: "",
            searchEntitySubtype: ""
        };
    }

    vm.isShowSidenav = false;
    vm.isLockSidenav = false;
    vm.showCesium = true;
    vm.displaySearchMode = displaySearchMode;
    vm.displayEntitySubtypeSearch = displayEntitySubtypeSearch;
    vm.openSidenav = openSidenav;
    vm.goBack = goBack;
    vm.searchTextUpdated = searchTextUpdated;
    vm.sidenavClicked = sidenavClicked;
    vm.toggleFullscreen = toggleFullscreen;
    vm.openSearch = openSearch;
    vm.closeSearch = closeSearch;
    // vm.openDashboard = openDashboard;

    $scope.$on('$stateChangeSuccess', function (evt, to, toParams, from) {
        watchEntitySubtype(false);
        if (angular.isDefined(to.data.searchEnabled)) {
            $scope.searchConfig.searchEnabled = to.data.searchEnabled;
            $scope.searchConfig.searchByEntitySubtype = to.data.searchByEntitySubtype;
            $scope.searchConfig.searchEntityType = to.data.searchEntityType;
            if ($scope.searchConfig.searchEnabled === false || to.name !== from.name) {
                $scope.searchConfig.showSearch = false;
                $scope.searchConfig.searchText = "";
                $scope.searchConfig.searchEntitySubtype = "";
            }
        } else {
            $scope.searchConfig.searchEnabled = false;
            $scope.searchConfig.searchByEntitySubtype = false;
            $scope.searchConfig.searchEntityType = null;
            $scope.searchConfig.showSearch = false;
            $scope.searchConfig.searchText = "";
            $scope.searchConfig.searchEntitySubtype = "";
        }
        watchEntitySubtype($scope.searchConfig.searchByEntitySubtype);
    });

    vm.isGtSm = $mdMedia('gt-sm');
    if (vm.isGtSm) {
        vm.isLockSidenav = true;
        $animate.enabled(siteSideNav, false);
    }

    $scope.$watch(function () { return $mdMedia('gt-sm'); }, function (isGtSm) {
        vm.isGtSm = isGtSm;
        vm.isLockSidenav = isGtSm;
        vm.isShowSidenav = isGtSm;
        if (!isGtSm) {
            $timeout(function () {
                $animate.enabled(siteSideNav, true);
            }, 0, false);
        } else {
            $animate.enabled(siteSideNav, false);
        }
    });

    bimPortal();

    // function openDashboard(dashboardID) {
    //     // $rootScope.forceFullscreen = true;
    //     $state.go('home.dashboards.dashboard', {dashboardId: dashboardID});
    // }

    function bimPortal() {
        // Power Plant design model provided by Bentley Systems
        var viewer = new Cesium.Viewer("cesiumContainer", {
            animation: false,  //是否显示动画控件
            fullscreenButton:false,
            vrButton:false,
            homeButton:false,
            sceneModePicker: true, //是否显示投影方式控件
            baseLayerPicker: true, //是否显示图层选择控件
            navigationHelpButton: false, //是否显示帮助信息控件
            geocoder: true, //是否显示地名查找控件
            timeline: false, //是否显示时间线控件          
            infoBox: false,  //是否显示点击要素之后显示的信息           
            
            /* 天地图 
            imageryProvider : new Cesium.WebMapTileServiceImageryProvider({
                url: "http://t0.tianditu.com/vec_w/wmts?service=wmts&request=GetTile&version=1.0.0&LAYER=vec&tileMatrixSet=w&TileMatrix={TileMatrix}&TileRow={TileRow}&TileCol={TileCol}&style=default&format=tiles",
                layer: "tdtVecBasicLayer",
                style: "default",
                format: "image/jpeg",
                tileMatrixSetID: "GoogleMapsCompatible",
                show: false
            })
            */
            /* OSM 1
            imageryProvider: new Cesium.createOpenStreetMapImageryProvider({
                url : 'https://stamen-tiles.a.ssl.fastly.net/toner/'
             })
            */
        //    imageryProvider: new Cesium.createOpenStreetMapImageryProvider({
        //        url: 'https://a.tile.openstreetmap.org/'
        //    }) 
        });

        // var viewModel = {
        //     height: 0,
        //     RotateX:0,
        //     RotateY:0,
        //     RotateZ:0
        // };    

        var scene = viewer.scene;
        var tileset = scene.primitives.add(
            new Cesium.Cesium3DTileset({
                url: '/3dtiles/tilesets/TilesetWithDiscreteLOD/tileset.json'
            })
        );
        // var tileset2 = scene.primitives.add(
        //     new Cesium.Cesium3DTileset({
        //         url: '/3dtiles/tilesets/TilesetWithDiscreteLOD/tileset.json'
        //     })
        // );
        tileset.readyPromise.then(function (tileset) {
            viewer.zoomTo(tileset, new Cesium.HeadingPitchRange(0.5, -0.2, tileset.boundingSphere.radius * 4.0));
        }).otherwise(function (error) {
            $log.log(error);
        });

        // var entity = viewer.entities.add({
        //     label: {
        //         show: false,
        //         showBackground: true,
        //         font: '14px monospace',
        //         horizontalOrigin: Cesium.HorizontalOrigin.LEFT,
        //         verticalOrigin: Cesium.VerticalOrigin.TOP,
        //         pixelOffset: new Cesium.Cartesian2(15, 0)
        //     }
        // });

        // Mouse over the globe to see the cartographic position
        var handler = new Cesium.ScreenSpaceEventHandler(scene.canvas);
        handler.setInputAction(function (movement) {
            var feature = scene.pick(movement.endPosition);
            if (feature instanceof Cesium.Cesium3DTileFeature) {
                feature.color = Cesium.Color.YELLOW;
                $log.log(feature);
/*             } else {
                var cartesian = viewer.camera.pickEllipsoid(movement.endPosition, scene.globe.ellipsoid);
                if (cartesian) {
                    var cartographic = Cesium.Cartographic.fromCartesian(cartesian);
                    var longitudeString = Cesium.Math.toDegrees(cartographic.longitude).toFixed(4);
                    var latitudeString = Cesium.Math.toDegrees(cartographic.latitude).toFixed(4);

                    entity.position = cartesian;
                    entity.label.show = true;
                    entity.label.text =
                        'Lon: ' + ('   ' + longitudeString).slice(-7) + '\u00B0' +
                        '\nLat: ' + ('   ' + latitudeString).slice(-7) + '\u00B0';
                    // $log.log(entity.label.text);
                    // $scope.loc.loc = entity.label.text;
                } else {
                    entity.label.show = false;
                } */
            }
        }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);
    }

    function watchEntitySubtype(enableWatch) {
        if ($scope.entitySubtypeWatch) {
            $scope.entitySubtypeWatch();
        }
        if (enableWatch) {
            $scope.entitySubtypeWatch = $scope.$watch('searchConfig.searchEntitySubtype', function (newVal, prevVal) {
                if (!angular.equals(newVal, prevVal)) {
                    $scope.$broadcast('searchEntitySubtypeUpdated');
                }
            });
        }
    }

    function displaySearchMode() {
        return $scope.searchConfig.searchEnabled &&
            $scope.searchConfig.showSearch;
    }

    function displayEntitySubtypeSearch() {
        return $scope.searchConfig.searchByEntitySubtype && vm.isGtSm;
    }

    function toggleFullscreen() {
        if (Fullscreen.isEnabled()) {
            Fullscreen.cancel();
        } else {
            Fullscreen.all();
        }
    }

    function openSearch() {
        if ($scope.searchConfig.searchEnabled) {
            $scope.searchConfig.showSearch = true;
            $timeout(() => {
                angular.element('#tb-search-text-input', $element).focus();
            });
        }
    }

    function closeSearch() {
        if ($scope.searchConfig.searchEnabled) {
            $scope.searchConfig.showSearch = false;
            if ($scope.searchConfig.searchText.length) {
                $scope.searchConfig.searchText = '';
                searchTextUpdated();
            }
        }
    }

    function searchTextUpdated() {
        $scope.$broadcast('searchTextUpdated');
    }

    function openSidenav() {
        vm.isShowSidenav = true;
    }

    function goBack() {
        $window.history.back();
    }

    function closeSidenav() {
        vm.isShowSidenav = false;
    }

    function sidenavClicked() {
        if (!vm.isLockSidenav) {
            closeSidenav();
        }
    }

}

/* eslint-enable angular/angularelement */