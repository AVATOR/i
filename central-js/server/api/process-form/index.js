var express = require('express');
var router = express.Router();
var form = require('./form.controller');
var auth = require('../../auth/auth.service.js');

router.get('/', auth.isAuthenticated(), form.index);
router.post('/', auth.isAuthenticated(), form.submit);
router.post('/scanUpload', auth.isAuthenticated(), form.scanUpload);

module.exports = router;
