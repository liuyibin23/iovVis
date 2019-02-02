// const url   = require("url");
const axios = require('axios')

// var API = 'http://cf.beidouapp.com:8080/api/';
var API = 'http://192.168.1.76:8080/api/';


function getAPI() {
    return API;
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

function responErrorMsg(err, res) {
    let resMsg = {
        "code": ``,
        "message": ''
    };
    resMsg.code = 500;
    if (err.response && err.response.status) {
        let code = err.response.status;
    
        resMsg.code = code;
        if (code == 401) {
            resMsg.message = '无授权访问。';
        }
        else if (code == 404) {
            resMsg.message = '访问资源不存在。';
        }
        else{
            resMsg.code = 500;
            resMsg.message = '服务器内部错误。';
        }
    }
    else
    {
        resMsg.message = err.message;
    }

    res.status(resMsg.code).json(resMsg);
}

function responData(code, data, res) {
    let resMsg = {
        // "code": `${code}`,
        "message": data
    };

    res.status(code).json(resMsg);
}

exports.CFG = Object.freeze({
    ALARM_NODE_NAME_1: '一级告警规则配置',
    ALARM_NODE_NAME_2: '二级告警规则配置'
});

exports.CST = Object.freeze({
    OK200: 200, MSG200: '成功。',
    ERR404: 404, MSG404: '访问资源不存在。',
    ERR510: 510, MSG510: 'CONFIG_ALARM_RULE规则链不存在。',
    ERR511: 511, MSG511: 'CONFIG_ALARM_RULE的META值异常。',
});
exports.getSync = getSync;
exports.postSync = postSync;
exports.getAPI = getAPI;
exports.responErrorMsg = responErrorMsg;
exports.responData = responData;
