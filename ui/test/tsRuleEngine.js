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
 * > node tsRuleEngine.js
 */
/* eslint-disable import/no-commonjs */
/* eslint-disable global-require */
/* eslint-disable import/no-nodejs-modules */

const axios = require('axios');
main();

async function postSync(url, data, tok) {
    try {
        let res = await axios.post(url, (data), { headers: { "X-Authorization": "Bearer " + tok } });
        let res_data = res.data;
        return new Promise((resolve, reject) => {
            if (res.status === 200) {
                resolve(res_data);
            } else {
                reject(res);
            }
        })
    } catch (err) {
        console.log("error:", err);
    }
}

async function getSync(url, data, tok) {
    try {
        let res = await axios.get(url, data);
        let res_data = res.data;
        return new Promise((resolve, reject) => {
            if (res.status === 200) {
                resolve(res_data);
            } else {
                reject(res);
            }
        })
    } catch (err) {
        console.log("error:", err);
    }
}

/* begin rule engine create session                  
 * from sensor originators to d virtual device
 * INPUT: dev_id of a&b, dev_name of virtual device to created, alarm rule, alarm severity
 * OUTPUT: 
 * 1. create new virtual device D 
 * 2. create rule chain
 * 3. create rule nodes 1)from a to mqtt 2)create alarm
 * 4. generate js template
 */
var devABC_ids = ['280f1810-1104-11e9-bae8-7562662cc4ee','b566b7e0-1104-11e9-bae8-7562662cc4ee'];
var virtual_name = 'demoVirtualDev0';
var virtual_dev_id = '';
var virtual_dev_tok = '';



async function main() {
    var loginRes = await postSync('http://cf.beidouapp.com:8080/api/auth/login',
        { "username": "26896225@qq.com", "password": "12345" },
        "");
    if (!loginRes) return;
    var token = loginRes.token;

    var devRes = await postSync('http://cf.beidouapp.com:8080/api/device',
        {
            name: virtual_name,
            type: "虚拟设备",
            additionalInfo: { description: "虚拟设备，用于现场传感数据的复杂解算。" }
        },
        token
    );
    if (!devRes) return;
    virtual_dev_id = devRes.id.id;
    devRes = await getSync(`http://cf.beidouapp.com:8080/api/device/${virtual_dev_id}/credentials`,
        {
            headers: {
                "X-Authorization": "Bearer " + token
            }
        }
    );
    if (!devRes) return;
    virtual_dev_tok = devRes.credentialsId;
    console.log('1)virtual device is created:', virtual_dev_tok);

    var ruleChain = await getSync('http://cf.beidouapp.com:8080/api/ruleChains',
        {
            headers: {
                "X-Authorization": "Bearer " + token
            },
            params: {
                textSearch: "MQTT_ENGINE_BUS",
                limit: 1
            }
        }
    );

    var ruleID = {};
    if (ruleChain.data.length > 0) {
        ruleID = ruleChain.data[0].id;
    }
    else {
        ruleChain = await postSync('http://cf.beidouapp.com:8080/api/ruleChain',
            {
                name: "MQTT_ENGINE_BUS"
            },
            token
        );
        ruleID = ruleChain.id;
    }
    console.log('2)MQTT_ENGINE_BUS rulechain is ready:', ruleID);

    /*订阅devABC_ids数据源到mqtt bus总线*/
    var ruleMeta = await getSync('http://cf.beidouapp.com:8080/api/ruleChain/' + ruleID.id + '/metadata',
        {
            headers: {
                "X-Authorization": "Bearer " + token
            }
        }
    );
    var index = ruleMeta.nodes[2].configuration.jsScript.indexOf('/*device ids array*/');
    eval(ruleMeta.nodes[2].configuration.jsScript.substr(0, index));
    if(devids) {    //var devids = [];
        for(var iii=0; iii<devABC_ids.length; iii++) {
            var iItem = devids.filter(function(item){return item === devABC_ids[iii]});
            if(iItem.length > 0) {
                continue;
            }
            devids.push(devABC_ids[iii]);
        }
    }
    ruleMeta.nodes[2].configuration.jsScript = "var devids = " + JSON.stringify(devids) + ";\n"
        + ruleMeta.nodes[2].configuration.jsScript.substr(index);
    var resultMeta = await postSync('http://cf.beidouapp.com:8080/api/ruleChain/metadata', 
        ruleMeta, token);
    if(!resultMeta) return;
    console.log('3)required device subject data is copy to MQTT BUS:', ruleMeta);


    var js_template = `
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
var devABC_ids = ${JSON.stringify(devABC_ids)};
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
    axios.post('http://cf.beidouapp.com:8080/api/v1/${virtual_dev_tok}/telemetry', //Virtual dev
        resOut).then(res => {
            console.log(res.status);
        }).catch(e => {
            console.log(e);
        })
    // create alarm according with point
}
`;
    console.log(js_template);

}



