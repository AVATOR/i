'use strict';

var express = require('express');
var passport = require('passport');
var authService = require('../auth.service');

var router = express.Router();

router.get('/', function (req, res, next) {
    passport.authenticate('oauth2', {
        callbackURL: '/auth/eds/callback?link=' + req.query.link,
        eds: true
    })(req, res, next);
});

router.get('/callback', function (req, res, next) {
    passport.authenticate('oauth2', {
        session: false,
        code: req.query.code,
        callbackURL: '/auth/eds/callback?link=' + req.query.link
    }, function (err, user, info) {
        var error;

        if (err) {
            error = {error: JSON.stringify(err)};
        }
        if (!info.accessToken) {
            error = {error: 'Cant find acess token. Something went wrong, please try again.'};
        }
        if (info.accessToken.oauthError) {
            error = {error: info.accessToken.message + ' ' + info.accessToken.oauthError.message};
        }
        if (!info.refreshToken) {
            error = {error: 'Cant find refresh token. Something went wrong, please try again.'};
        }
        if (!user) {
            error = {error: 'Cant sync user'};
        }

        if (error) {
            res.redirect(req.query.link + '?error=' + JSON.stringify(error));
        } else {
            req.session.subject = user.subject;
            req.session.access = info;
            res.redirect(req.query.link);
        }
    })(req, res, next)
});

module.exports = router;