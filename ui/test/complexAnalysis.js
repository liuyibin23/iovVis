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
 * > npm install axios ws
 * > node computation.js
 */
const mqtt = require('mqtt');
const axios = require('axios');
var devABC_ids = ["280f1810-1104-11e9-bae8-7562662cc4ee","b566b7e0-1104-11e9-bae8-7562662cc4ee"];
var client = mqtt.connect('mqtt://cf.beidouapp.com:9008');
client.on('connect', function () {
    console.log('connected');
    for(var iii=0; iii < devABC_ids.length; iii++) {
        client.subscribe('v1/devices/me/telemetry/' + devABC_ids[iii]);
    }
});
var devid_map = new Map();
client.on('message', function (topic, message) {
    console.log('request.topic: ' + topic);
    console.log('request.body: ' + message.toString());
    var mkey = topic.substr('v1/devices/me/telemetry/'.length);
    var vobjs = devid_map.get(mkey);
    if(!vobjs)
        vobjs = [];
    vobjs.push(JSON.parse(message));
    devid_map.set(mkey, vobjs);
    computeRule(devid_map);
});
function computeRule(devMap) {
    // analysis function to evaluate a new output by devMap
    let keys = [...devMap.keys()];
    let vectors = [...devMap.values()];
    if(vectors[0].length < 3) return;
    let resOut = {VirtualDevOutput: 100};
    // push to virtual dev end
    axios.post('http://cf.beidouapp.com:8080/api/v1/GC9IZChRRGpdQC42PcmY/telemetry', //Virtual dev
        resOut).then(res => {
            console.log(res.status);
        }).catch(e => {
            console.log(e);
        })
    // create alarm according with point
}