const { createLogger, format, transports } = require('winston');

let logger = createLogger({
    format: format.combine(
        format.splat(),
        format.colorize(),
        format.simple()
    ),
    transports: [
        new transports.Console(),
        new transports.File({ filename: 'error.log', level:'error'}),
        new transports.File({ filename: 'combined.log' })
    ]
});

module.exports = logger;