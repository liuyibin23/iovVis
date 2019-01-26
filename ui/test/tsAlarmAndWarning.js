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
 * 1. 获取token 
 * 2. 得到rule chain
 * 3. 得到规则链meta
 * 对于节点5的操作
 * 4.1. 取已有配置的与或规则，检查是否存在assetId对应的配置
 * 4.2. 不存在，则添加一个新的配置
 * 4.3. 存在，记录旧规则，之后替换，
 * 对于节点2的操作
 * 4.4. 从新的配置项的andRule中获取涉及到的所有设备ID
 * 5. 写回后端规则引擎数据库
 */
//newBridge warning rules, user input(from front end)
var assetId = "265c7510-1df4-11e9-b372-db8be707c5f4";
var blueRules = [{
    andRule: [
        "devA"
    ]
}, {
    andRule: [
        "devD"
    ]
}]

var orangeRules = [{
    andRule: ["devD","devA"]
}]

//1
async function main() {
    let loginRes = await postSync('http://cf.beidouapp.com:8080/api/auth/login',
        { "username": "lvyu@beidouapp.com", "password": "12345" },
        "");
    if (!loginRes) return;
    let token = loginRes.token;
    //2
    let ruleChain = await getSync('http://cf.beidouapp.com:8080/api/ruleChains',
        {
            headers: {
                "X-Authorization": "Bearer " + token
            },
            params: {
                textSearch: "CONFIG_WARNING_RULE",
                limit: 1
            }
        }
    );
    //3
    let ruleID = {};
    if (ruleChain.data.length > 0) {
        ruleID = ruleChain.data[0].id;
        /*获取告警规则链的meta数据*/
        let ruleMeta = await getSync('http://cf.beidouapp.com:8080/api/ruleChain/' + ruleID.id + '/metadata',
            {
                headers: {
                    "X-Authorization": "Bearer " + token
                }
            }
        );
        //预警规则（节点2#，5#）
        let js5 = ruleMeta.nodes[5].configuration.jsScript;
        let js2 = ruleMeta.nodes[2].configuration.latestTsKeyNames;

        let index = js5.indexOf('/* warning rule tables */');
        eval(js5.substr(0, index));
        if (ruleTables) {
            //从规则配置中把涉及的所有属性拿到
            var oldBlueRules;
            var oldOrangeRules;
            if (ruleTables[assetId]) {
                oldBlueRules = ruleTables[assetId].blueRules;
                oldOrangeRules = ruleTables[assetId].orangeRules;
            } else
                ruleTables[assetId] = {};

            ruleTables[assetId].blueRules = blueRules;
            ruleTables[assetId].orangeRules = orangeRules;

            js5 = "var ruleTables = " + JSON.stringify(ruleTables) + ";\n" + js5.substr(index);

            //遍历新的ruleTables，把devId全部整理出来
            let allDevIds = new Set();
            let keys =  Object.keys(ruleTables);
            keys.forEach(it => {
                let rule = ruleTables[it];
                let bRules = rule['blueRules'];
                let oRules = rule['orangeRules'];
                bRules.forEach(element => {
                    let ids = element.andRule;
                    ids.forEach(el => {
                        allDevIds.add(el);
                    });
                });
                oRules.forEach(element => {
                    let ids = element.andRule;
                    ids.forEach(el => {
                        allDevIds.add(el);
                    });
                });
            });

            js2 = [...allDevIds];

        }
        // console.log(js5);
        // console.log(js2);
        ruleMeta.nodes[5].configuration.jsScript = js5;
        ruleMeta.nodes[2].configuration.latestTsKeyNames = js2;
        var resultMeta = await postSync('http://cf.beidouapp.com:8080/api/ruleChain/metadata',
            ruleMeta, token);
        if (!resultMeta) return;
        console.log('warning config rules is set', ruleMeta);
    }
}



