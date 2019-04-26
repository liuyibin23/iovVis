const express = require('express');
const bodyParser = require('body-parser');
const swaggerUi = require('swagger-ui-express');
const cors = require('cors');
const path = require('path');

const swaggerDocument = require('./swagger.json');
const tokenVerify = require('./middleware/token-verifier');
const errHandler = require('./middleware/error-handler');
const templatesRouter = require('./routers/templates-router');
const reportsRouter = require('./routers/reports-router');
const echartsRouter = require('./routers/echarts-router');
const contentRouter =  require('./routers/content-router');
const generateReportsRouter = require('./routers/generate-reports-router');
const alarmsRouter = require('./routers/alarms-router');
const warningsRouter = require('./routers/warnings-router');
const virtualDeviceRouter = require('./routers/virtual-device-router');
const wallResultRouter = require('./routers/wall-result');
const statisticsRouter = require('./routers/statistics-router');
const defaultRouter = require('./routers/default-router');
const logger = require('./util/logger');
const util = require('./util/utils');
const chartCfg = require('./routers/echarts/chartConfig');
const VERSION = '1.0.0';

let app = express();
let port = 20050;
let options = {
    customCss: '.swagger-ui .topbar { display: none }',
    explorer: false
  };

const root = path.join(__dirname, '/public');
let ret = util.loadCfg('config.json');
if (ret) {
  ret = chartCfg.loadChartCfg('./routers/echarts/PlotConfig.json');

  if (ret) {
    app.use('/config', express.static(root));  
    app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerDocument,options));
    app.use(cors());
    app.use(tokenVerify());
    app.use(bodyParser.json());
    app.use(bodyParser.urlencoded({ extended: false }));


    app.use('/api/v1/templates', templatesRouter);
    app.use('/api/v1/reports', reportsRouter);
    app.use('/api/v1/rules/alarms', alarmsRouter);
    app.use('/api/v1/echarts', echartsRouter);
    app.use('/api/v1/tables', contentRouter);
    app.use('/api/v1/content', contentRouter);
    app.use('/api/v1/generate/reports', generateReportsRouter);
    app.use('/api/v1/warnings', warningsRouter);
    app.use('/api/v1/rules/warnings', warningsRouter);
    app.use('/api/v1/virtualDevice/create', virtualDeviceRouter);
    app.use('/api/v1/virtualDevice/config', virtualDeviceRouter);
    app.use('/api/v1/currentUser/wallResult', wallResultRouter);
    app.use('/api/v1/statistics', statisticsRouter);
    app.use(defaultRouter);
    app.use(errHandler);

    logger.log('info', 'API Gateway version: %s', VERSION);
    logger.log('info', 'Simple API Gateway run on localhost:%d', port);

    app.listen(port);
  } 
  else {
    logger.log('info', 'API Gateway version: %s', VERSION);
    logger.log('error', 'API Gateway load cfg file [PlotConfig.json] failed.');
  }  
} 
else {
  logger.log('info', 'API Gateway version: %s', VERSION);
  logger.log('error', 'API Gateway load cfg file [config.json] failed.');
}
