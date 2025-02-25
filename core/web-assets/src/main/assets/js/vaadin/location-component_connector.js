/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
/* eslint no-console: 0 */

const L = require('vendor/leaflet-js');

// Do not remove unused as they are not included in the assets directory otherwise
import MarkerIcon from '../../static/legacy/leaflet/dist/images/marker-icon.png';
import MarkerRetinaIcon from '../../static/legacy/leaflet/dist/images/marker-icon-2x.png';
import MarkerShadowIcon from '../../static/legacy/leaflet/dist/images/marker-shadow.png';
import NotMarkedIcon from './not-marked-icon.png';
import NotMarkedRetinaIcon from './not-marked-icon-2x.png';

if (!window.org_opennms_features_topology_app_internal_ui_geographical_LocationComponent) {
    var __onms_getImagePath = function getImagePath() {
        var el = L.DomUtil.create('div',  'leaflet-default-icon-path', document.body);
        var path = L.DomUtil.getStyle(el, 'background-image') ||
                   L.DomUtil.getStyle(el, 'backgroundImage');   // IE8

        document.body.removeChild(el);

        console.log("__onms_getImagePath", el, path);

        return path.indexOf('url') === 0 ?
            path.replace(/^url\(["']?/, '').replace(/marker-icon\.png\?v=.+["']?\)$/, '') : '';
    };

    window.org_opennms_features_topology_app_internal_ui_geographical_LocationComponent = function LocationComponent() {
        var state = this.getState();

        // The id is configurable, as we may have multiple or to prevent id conflicts
        var mapId = state.mapId;

        // Add the map container
        this.getElement().innerHTML='<div style="width: 100%; height: 100%" id="' + mapId + '"></div>';

        // Create the Map
        var theMap = L.map(mapId);
        L.tileLayer(state.tileLayer, state.layerOptions).addTo(theMap);

        var imagePath = __onms_getImagePath();

        var notMarkedIcon = L.icon({
            /*
            iconUrl: L.Icon.Default.imagePath + '/not-marked-icon.png',
            iconRetinaUrl: L.Icon.Default.imagePath + '/not-marked-icon-2x.png',
            */
            iconUrl: imagePath + 'not-marked-icon.png',
            iconRetinaUrl: imagePath + 'not-marked-icon-2x.png',
            iconSize:    [25, 41],
            iconAnchor:  [12, 41],
            popupAnchor: [1, -34],
            tooltipAnchor: [16, -28],
            /*
            shadowUrl: L.Icon.Default.imagePath + '/marker-shadow.png',
            shadowRetinaUrl: L.Icon.Default.imagePath + '/marker-shadow.png',
            */
            shadowUrl: imagePath + 'marker-shadow.png',
            shadowRetinaUrl: imagePath + 'marker-shadow.png',
            shadowSize:  [41, 41]
        });

        var markerIcon = L.icon({
            iconUrl: imagePath + 'marker-icon.png',
            iconRetinaUrl: imagePath + 'marker-icon-2x.png',
            iconSize:    [25, 41],
            iconAnchor:  [12, 41],
            popupAnchor: [1, -34],
            tooltipAnchor: [16, -28],
            shadowUrl: imagePath + 'marker-shadow.png',
            shadowRetinaUrl: imagePath + 'marker-shadow.png',
            shadowSize:  [41, 41]
        });

        var markers = state.markers;
        var coordinates = [];
        var markerArray = [];
        for (var i = 0; i < markers.length; i++) {
            var latitude = markers[i].coordinates.latitude;
            var longitude = markers[i].coordinates.longitude;
            var marker = L.marker(L.latLng(latitude, longitude));

            if (markers[i].tooltip !== undefined) {
                marker.bindPopup(markers[i].tooltip);
            }
            if (!markers[i].marked) {
                marker.setIcon(notMarkedIcon);
            } else {
                marker.setIcon(markerIcon);
            }
            marker.addTo(theMap);
            coordinates.push([latitude, longitude]);
            markerArray.push(marker);
        }

        // show all markers
        var markerGroup = new L.featureGroup(markerArray);
        theMap.fitBounds(markerGroup.getBounds().pad(0.2));

        // If we have only one vertex, center it
        if (markerArray.length === 1) {
            // Center the view
            var center = coordinates.reduce(function getCenter(x,y) {
                return [x[0] + y[0]/coordinates.length, x[1] + y[1]/coordinates.length];
            }, [0,0]);

            // Collect coordinates
            theMap.setView([center[0], center[1]], state.initialZoom);
        }
    };

    console.log('init: location-component');
}

module.exports = window.org_opennms_features_topology_app_internal_ui_geographical_LocationComponent;