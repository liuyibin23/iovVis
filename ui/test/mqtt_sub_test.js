/*
 * Copyright © 2016-2018 The ET-iLink Authors
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
 * 
 * usage:
 * > npm install mqtt
 * > node mqtt_sub_test.js
 */
 
/* eslint-disable import/no-commonjs */
/* eslint-disable global-require */
/* eslint-disable import/no-nodejs-modules */

const mqtt = require('mqtt');
var options={
    username:"22HVYAZzRuzqj6fwqNag"
}

// var client = mqtt.connect("mqtt://sgcc.beidouapp.com:1883",options);

var client = mqtt.connect('mqtt://192.168.1.179');
client.on('connect', function () {
	console.log('connected');
	client.subscribe('paho');

	// client.subscribe('v1/devices/me/telemetry/+');
	// client.subscribe('v1/devices/me/telemetry/5074b200-e31a-11e8-be95-f3713e6700c3');
	// client.subscribe('v1/devices/me/telemetry/056a2f60-e31a-11e8-be95-f3713e6700c3');
});
client.on('error', function (err) {
	console.log('error',err);
	// client.subscribe('v1/devices/me/telemetry/+');
	// client.subscribe('v1/devices/me/telemetry/5074b200-e31a-11e8-be95-f3713e6700c3');
	// client.subscribe('v1/devices/me/telemetry/056a2f60-e31a-11e8-be95-f3713e6700c3');
});
var cnt_ = 0;
client.on('message', function (topic, message) {
	console.log('request.topic: ' + topic);
	console.log('request.body: ' + message);
});