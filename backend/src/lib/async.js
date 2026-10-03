/**
 * Express 4 does not catch rejected promises from async handlers — an unhandled
 * rejection leaves the request hanging until the client times out, and never
 * reaches the error middleware in server.js.
 *
 * Every async route handler must be wrapped in this.
 */
function wrap(fn) {
  return function wrappedHandler(req, res, next) {
    Promise.resolve(fn(req, res, next)).catch(next);
  };
}

module.exports = { wrap };
