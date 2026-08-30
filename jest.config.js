module.exports = {
  testPathIgnorePatterns: [
    '/node_modules/',
    '/tests/e2e/',
    '/playwright/'
  ],
  collectCoverageFrom: [
    'tests/unit/**/*.js',
    'tests/integration/**/*.js'
  ]
};
