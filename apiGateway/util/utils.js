// const url   = require("url");
const axios = require('axios');
var path = require('path'); //系统路径模块
var fs = require('fs'); //文件模块
const logger = require('./logger');

var gCfg = null;

function loadCfg(filePath){
    let data = [];
    var file = path.join(filePath); 

    try {
        //读取json文件
        var cfg = fs.readFileSync(file, 'utf-8');
        var cfgInfo = JSON.parse(cfg);

        if (cfgInfo.serverCfg) {
            gCfg = cfgInfo.serverCfg;
            return true;
        }
    } catch(err){
        console.log(err.message);
        logger.log('error', err.message);
        return false;
    }
}

function getAPI() {
    return gCfg.API;
}
function getFSVR() {
    return gCfg.fileSVR;
}

function getBackendBUS(){
    return gCfg.mqttBackendBUS;
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
        else if (code == 403) {
            resMsg.message = err.message;
        }
        else if (code == 404) {
            resMsg.message = '访问资源不存在。';
        }
        else {
            resMsg.code = 500;
            if (err.response.data.message) {
                resMsg.message = `服务器内部错误。 ${err.response.data.message}`;
            } else {
                resMsg.message = '服务器内部错误。';
            }            
        }
    } else {
        resMsg.message = err.message;
    }
    res.status(resMsg.code).json({message: resMsg.message});
}

function responData(code, data, res) {
    // let resMsg = {
    //     // "code": `${code}`,
    //     "message": data
    // };

    res.status(code).json(data);
}

exports.CFG = Object.freeze({
    ALARM_NODE_NAME_1: '一级告警规则配置',
    ALARM_NODE_NAME_2: '二级告警规则配置',
    WARN_NODE_ALARM_DEV: '获取所有规则需要的属性',
    WARN_NODE_RULE: '预警规则设置'
});

exports.CST = Object.freeze({
    OK200: 200, MSG200: {message: '成功。'},
    ERR400: 400, MSG400: {message: '请求方法参数错误或方法不被允许。'},
    ERR401: 401, MSG401: {message: '无效的api token。'},
    ERR404: 404, MSG404: {message: '访问资源不存在。'},
    ERR510: 510, MSG510: {message: '规则不存在。'},
    ERR511: 511, MSG511: {message: 'CONFIG_ALARM_RULE的META值异常。'},
    ERR512: 512, MSG512: {message: '设备号对应信息获取失败。'},
});


function dateFormat(date, fmt) {
    if (null == date || undefined == date) return '';
    var o = {
        "M+": date.getMonth() + 1, //月份
        "d+": date.getDate(), //日
        "h+": date.getHours(), //小时
        "m+": date.getMinutes(), //分
        "s+": date.getSeconds(), //秒
        "S": date.getMilliseconds() //毫秒
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

exports.getSync = getSync;
exports.postSync = postSync;
exports.getBackendBUS = getBackendBUS;
exports.getAPI = getAPI;
exports.getFSVR = getFSVR;
exports.responErrorMsg = responErrorMsg;
exports.responData = responData;
exports.loadCfg = loadCfg;
exports.dateFormat = dateFormat;
