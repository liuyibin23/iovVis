const axios = require('axios');
const logger = require('../util/logger');

var token = '';

/*
 * 登录-》获取token
 */
async function getToken() {
    let loginRes;
    try {
        loginRes = await axios.post('http://cf.beidouapp.com:8080/api/auth/login',
            { "username": "gongjian@beidouapp.com", "password": "12345" });
        return loginRes.data;
    } catch (err) {
        throw new Error("login failed!");
    }
}

/*
* 返回token
*/
function getTokenStr(){
    return token;
}

function tokenVerify() {
    return async function (req, res, next) {
         let tk = await getToken();
         logger.log('info', 'token:%s', tk.token);
       // let tok = req.headers['x-authorization'];
        //logger.log('info', 'headers x-authorization: %s', tok);
        if (tk) {
            token = tk.token;
            // res.status(200).send('ok');
            next();
        } else
            res.status(401).send('ERROR: 401 unauthorized token').end();
        }
}
// module.exports = getTokenStr;
// module.exports = tokenVerify;

exports.getTokenStr = getTokenStr;
exports.tokenVerify = tokenVerify;