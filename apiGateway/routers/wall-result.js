const express = require('express');
const router = express.Router();
const axios = require('axios');
const util = require('../util/utils');
const logger = require('../util/logger');


var sys_admin_id = "13814000-1dd2-11b2-8080-808080808080";

// middleware that is specific to this router
// router.use(function timeLog(req, res, next) {
//   console.log('Time: ', Date.now());
//   next();
// })

// define the about route
router.get('/about', function (req, res) {
    res.send('About wall result');
})

function GetVideoInfo(groupType, groupID, token, res) {
    let api = util.getAPI() + `currentUser/videoInfo?groupType=${groupType}&groupId=${groupID}`;
    axios.get(api, {
        headers: {
            "X-Authorization": token
        }
    }).then(resp => {
        util.responData(util.CST.OK200, resp.data, res);
    }).catch(err => {
        util.responErrorMsg(err, res);
    });
}

//GET获取指定设备号上绑定的告警规则
router.get('/', async function (req, res) {
    let token = req.headers['x-authorization'];

    let spStr = token.split('.');
    if (spStr[1]) {
        var token_decode = new Buffer(spStr[1], 'base64');
        var js = JSON.parse(token_decode.toString());
        if (js.hasOwnProperty('scopes')) {
            let scopes = js.scopes[0];
            let id = null;
            if (scopes === "CUSTOMER_USER") {
                id = js.customerId;
            } else if (scopes === "TENANT_ADMIN"){
                id = js.tenantId;
            }
            else if (scopes === "SYS_ADMIN"){
                id = sys_admin_id;
            }
            
            if (id && scopes) { 
                GetVideoInfo(scopes, id, token, res);
            }
            else {
                util.responData(util.CST.ERR400, util.CST.MSG400, res);
            }
        } else {
            util.responData(util.CST.ERR400, util.CST.MSG400, res);
        }
    }
    else {
        util.responData(util.CST.ERR400, util.CST.MSG400, res);
    }
})

function PostVideoInfo(groupType, groupID, videoInfo , token, res) {
    let api = util.getAPI() + `currentUser/videoInfo?groupType=${groupType}&groupId=${groupID}`;
    axios.post(api, {
        videoInfo: videoInfo
    },
        {
            headers: {
                "X-Authorization": token
            },
        }).then(resp => {
            util.responData(util.CST.OK200, resp.data, res);
        }).catch(err => {
            // 由devID查询tenantId出现问题
            // util.responData(util.CST.ERR512, util.CST.MSG512, res);
            util.responErrorMsg(err, res);
        });
}

//POST更新设备的告警规则，并且更新设备表的addtionalInfo，记录该告警规则
router.post('/', async function (req, res) {
    let token = req.headers['x-authorization'];

    let spStr = token.split('.');
    if (spStr[1]) {
        var token_decode = new Buffer(spStr[1], 'base64');
        var js = JSON.parse(token_decode.toString());
        if (js.hasOwnProperty('scopes')) {
            let scopes = js.scopes[0];
            let id = null;
            if (scopes === "CUSTOMER_USER") {
                id = js.customerId;
            } else if (scopes === "TENANT_ADMIN"){
                id = js.tenantId;
            }
            else if (scopes === "SYS_ADMIN"){
                id = sys_admin_id;
            }
            
            if (scopes) { 
                PostVideoInfo(scopes, id, req.query.videoInfo, token, res);
            }
            else {
                util.responData(util.CST.ERR400, util.CST.MSG400, res);
            }
        } else {
            util.responData(util.CST.ERR400, util.CST.MSG400, res);
        }
    }
    else {
        util.responData(util.CST.ERR400, util.CST.MSG400, res);
    }
})

module.exports = router