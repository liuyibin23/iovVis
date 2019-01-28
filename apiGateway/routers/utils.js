// const url   = require("url");
const axios = require('axios')

var API = 'http://cf.beidouapp.com:8080/api/';


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
    let code = 500;
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
            resMsg.message = '服务器内部错误。';
        }
    }
    else
    {
        resMsg.message = err.message;
    }

    res.status(code).json(resMsg);
}

exports.getSync = getSync;
exports.postSync = postSync;
exports.getAPI = getAPI;
exports.responErrorMsg = responErrorMsg;