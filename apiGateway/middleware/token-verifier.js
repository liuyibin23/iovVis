const axios = require('axios');
const logger = require('../util/logger');

/*
 * 登录-》获取token
 */
async function getToken() {
    let loginRes;
    try {
        loginRes = await axios.post('http://cf.beidouapp.com:8080/api/auth/login',
            { "username": "lvyu@beidouapp.com", "password": "12345" });
        return loginRes.data;
    } catch (err) {
        throw new Error("login failed!");
    }
}

function tokenVerify() {
    return async function (req, res, next) {
        // let tk = await getToken();
        // logger.log('info', 'token:%s', tk.token);
        let tok = req.headers['x-authorization'];
        logger.log('info', 'headers x-authorization: %s', tok);
        if (tok) {
            // res.status(200).send('ok');
            next();
        } else
            res.status(401).send('unauthorized token').end();
        }
}
module.exports = tokenVerify;