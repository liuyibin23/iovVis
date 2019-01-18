const express = require('express');
const bodyParser = require('body-parser');

const tokVerifier = require('./middleware/token-verifier');
const errHandler = require('./middleware/error-handler');
const filesRouter = require('./routers/files-router');
const reportsRouter = require('./routers/reports-router');
const alarmsRouter = require('./routers/alarms-router');
const defaultRouter = require('./routers/default-router');
const logger = require('./util/logger');

let app = express();
let port = 4001;

app.use(tokVerifier());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use('/api/v1/files', filesRouter);
app.use('/api/v1/reports', reportsRouter);
app.use('/api/v1/alarms', alarmsRouter);
app.use(defaultRouter);
app.use(errHandler);
logger.log('info', 'Simple API Gateway run on localhost:%d', port);

app.listen(port);