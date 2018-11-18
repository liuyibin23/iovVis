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
import Cesium from 'cesium/Cesium';
import Color from 'cesium/Core/Color';
import "cesium/Widgets/widgets.css";
import "cesium/Widgets/lighter.css";

/*@ngInject*/
export default function IntegratedController($scope, $rootScope, $mdMedia, $mdSidenav, menu) {

    var vm = this;

    vm.sectionColspan = sectionColspan;
    vm.toggleRight = buildToggler;
    $rootScope.forceFullscreen = !$rootScope.forceFullscreen;
    $scope.searchConfig.searchEnabled = true;

	
// Power Plant design model provided by Bentley Systems
var viewer = new Cesium.Viewer('cesiumContainer');
var scene = viewer.scene;

var tileset = scene.primitives.add(
    new Cesium.Cesium3DTileset({
        url: Cesium.IonResource.fromAssetId(8564)
    })
);

tileset.readyPromise.then(function(tileset) {
    viewer.zoomTo(tileset, new Cesium.HeadingPitchRange(0.5, -0.2, tileset.boundingSphere.radius * 4.0));
}).otherwise(function(error) {
    console.log(error);
});

tileset.colorBlendMode = Cesium.Cesium3DTileColorBlendMode.REPLACE;

var selectedFeature;
var picking = true;

// viewer.addToggleButton('Per-feature selection', false, function(checked) {
//    picking = checked;
//    if (!picking) {
//        unselectFeature(selectedFeature);
//    }
//});

function selectFeature(feature) {
    var element = feature.getProperty('element');
    setElementColor(element, Cesium.Color.YELLOW);
    selectedFeature = feature;
}

function unselectFeature(feature) {
    if (!Cesium.defined(feature)) {
        return;
    }
    var element = feature.getProperty('element');
    setElementColor(element, Cesium.Color.WHITE);
    if (feature === selectedFeature) {
        selectedFeature = undefined;
    }
}

var handler = new Cesium.ScreenSpaceEventHandler(scene.canvas);
handler.setInputAction(function(movement) {
    if (!picking) {
        return;
    }

    var feature = scene.pick(movement.endPosition);

    unselectFeature(selectedFeature);

    if (feature instanceof Cesium.Cesium3DTileFeature) {
        selectFeature(feature);
    }
}, Cesium.ScreenSpaceEventType.MOUSE_MOVE);

// In this tileset every feature has an "element" property which is a global ID.
// This property is used to associate features across different tiles and LODs.
// Workaround until 3D Tiles has the concept of global batch ids: https://github.com/AnalyticalGraphicsInc/3d-tiles/issues/265
var elementMap = {}; // Build a map of elements to features.
var hiddenElements = [112001, 113180, 131136, 113167, 71309, 109652, 111178, 113156, 113170, 124846, 114076, 131122, 113179, 114325, 131134, 113164, 113153, 113179, 109656, 114095, 114093, 39225, 39267, 113149, 113071, 112003, 39229, 113160, 39227, 39234, 113985, 39230, 112004, 39223];

function getElement(feature) {
    return parseInt(feature.getProperty('element'), 10);
}

function setElementColor(element, color) {
    var featuresToColor = elementMap[element];
    var length = featuresToColor.length;
    for (var i = 0; i < length; ++i) {
        var feature = featuresToColor[i];
        feature.color = Cesium.Color.clone(color, feature.color);
    }
}

function unloadFeature(feature) {
    unselectFeature(feature);
    var element = getElement(feature);
    var features = elementMap[element];
    var index = features.indexOf(feature);
    if (index > -1) {
        features.splice(index, 1);
    }
}

function loadFeature(feature) {
    var element = getElement(feature);
    var features = elementMap[element];
    if (!Cesium.defined(features)) {
        features = [];
        elementMap[element] = features;
    }
    features.push(feature);

    if (hiddenElements.indexOf(element) > -1) {
        feature.show = false;
    }
}

function processContentFeatures(content, callback) {
    var featuresLength = content.featuresLength;
    for (var i = 0; i < featuresLength; ++i) {
        var feature = content.getFeature(i);
        callback(feature);
    }
}

function processTileFeatures(tile, callback) {
    var content = tile.content;
    var innerContents = content.innerContents;
    if (Cesium.defined(innerContents)) {
        var length = innerContents.length;
        for (var i = 0; i < length; ++i) {
            processContentFeatures(innerContents[i], callback);
        }
    } else {
        processContentFeatures(content, callback);
    }
}

tileset.tileLoad.addEventListener(function(tile) {
    processTileFeatures(tile, loadFeature);
});

tileset.tileUnload.addEventListener(function(tile) {
    processTileFeatures(tile, unloadFeature);
});

	
	
	
	
	
	
	
	
    // Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJmZDgzOWVlYi00ZmMyLTRkZWItYmQ0OS04MmZmNWRmOTFmMmIiLCJpZCI6NDkxNiwic2NvcGVzIjpbImFzciIsImdjIl0sImlhdCI6MTU0MjAyNzQwM30.AWZo3H818wSZa8Mjk_ataDjcGMLvf2PvYdyNsnMYJmk';
    // // var viewer = new Cesium.Viewer('cesiumContainer', {
    // //     terrainProvider: Cesium.createWorldTerrain()
    // // });
    // var viewer = new Cesium.Viewer('cesiumContainer',{
    //     terrainProvider : new Cesium.CesiumTerrainProvider({
    //         url: Cesium.IonResource.fromAssetId(3956)
    //     })
    // });

/*     Cesium.Ion.defaultAccessToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiJmZDgzOWVlYi00ZmMyLTRkZWItYmQ0OS04MmZmNWRmOTFmMmIiLCJpZCI6NDkxNiwic2NvcGVzIjpbImFzciIsImdjIl0sImlhdCI6MTU0MjAyNzQwM30.AWZo3H818wSZa8Mjk_ataDjcGMLvf2PvYdyNsnMYJmk';
    var viewer = new Cesium.Viewer('cesiumContainer', {
        terrainProvider: Cesium.createWorldTerrain()
    });

    var tileset = viewer.scene.primitives.add(
        new Cesium.Cesium3DTileset({
            url: Cesium.IonResource.fromAssetId(10287)
        })
    );
    viewer.zoomTo(tileset); */

    function buildToggler(componentId) {
        $mdSidenav(componentId).toggle();
        
    }

    $scope.$watch(function() { return $mdMedia('lg'); }, function() {
        updateColumnCount();
    });

    $scope.$watch(function() { return $mdMedia('gt-lg'); }, function() {
        updateColumnCount();
    });

    updateColumnCount();

    vm.model = menu.getHomeSections();

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
