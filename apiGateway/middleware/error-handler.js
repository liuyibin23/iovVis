const logger = require('../util/logger');

function errorHandler(options) {
    return function (err, req, res, next) {
        if (res.headersSent) {
            return next(err);
        }
        res.status(500);
        res.end('error', { error: err });
        // logger.log('error');
    }
}
module.exports = errorHandler;