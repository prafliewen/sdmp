// Global test setup.
// axios-mock-adapter v2 replaces `axios.defaults.adapter` on construction, but
// `src/utils/request.js` uses `axios.create({...})` which produces an INDEPENDENT
// axios instance with its own `defaults.adapter`. The mock therefore only
// intercepts calls made through the global `axios`, not through the per-app
// `request` instance.
//
// To make tests reliable, we expose a helper that attaches a mock to BOTH the
// global axios and the imported `request` instance. Tests that hit HTTP should
// import this helper instead of constructing MockAdapter directly.
import MockAdapter from 'axios-mock-adapter'
import axios from 'axios'
import request from '../src/utils/request.js'

export function installMock() {
  const mock = new MockAdapter(axios)
  request.defaults.adapter = mock.adapter()
  return mock
}
