const logger = require('../util/logger');

function tokenVerify() {
    return async function (req, res, next) {
        // let tk = await getToken();
        // logger.log('info', 'token:%s', tk.token);
        let tok = req.headers['x-authorization'];
        try {
            let remoteIp = req.connection.remoteAddress.split(':');
            logger.log('info', `[${Date.now()}] from ip ${remoteIp[remoteIp.length - 1]}  ->`);
        } catch (err) {
            logger.log('error', `[${Date.now()}] ${err.message}`);
        };
        if (tok) {
            next();
        } else
            res.status(401).send('ERROR: 401 unauthorized token').end();
    }
}
module.exports = tokenVerify;