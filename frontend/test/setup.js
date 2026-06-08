// Global test setup. The actual mock wiring happens in test/helpers.js
// (see installMock) because we need to attach the mock adapter to BOTH the
// global axios and the per-app `request` instance created via axios.create.
