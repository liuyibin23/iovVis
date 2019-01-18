const express = require('express');
const bodyParser = require('body-parser');
const swaggerUi = require('swagger-ui-express');

const swaggerDocument = require('./swagger.json');
const tokVerifier = require('./middleware/token-verifier');
const errHandler = require('./middleware/error-handler');
const templatesRouter = require('./routers/templates-router');
const reportsRouter = require('./routers/reports-router');
const alarmsRouter = require('./routers/alarms-router');
const defaultRouter = require('./routers/default-router');
const logger = require('./util/logger');

let app = express();
let port = 4001;
let options = {
    customCss: '.swagger-ui .topbar { display: none }',
    explorer: false
  };
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocument,options));

app.use(tokVerifier());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use('/api/v1/templates', templatesRouter);
app.use('/api/v1/reports', reportsRouter);
app.use('/api/v1/alarms', alarmsRouter);
app.use(defaultRouter);
app.use(errHandler);
logger.log('info', 'Simple API Gateway run on localhost:%d', port);

app.listen(port);