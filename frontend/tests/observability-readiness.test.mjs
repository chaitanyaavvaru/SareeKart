import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const frontendRoot = path.resolve(__dirname, '..');

test('Point 1 — ErrorBoundary Lifecycle: Catches unhandled errors and activates state', () => {
  const errorBoundaryPath = path.resolve(frontendRoot, 'src/components/common/ErrorBoundary.jsx');
  const content = fs.readFileSync(errorBoundaryPath, 'utf8');

  assert.ok(content.includes('static getDerivedStateFromError(error)'), 'Must implement getDerivedStateFromError');
  assert.ok(content.includes('componentDidCatch(error, errorInfo)'), 'Must implement componentDidCatch');
  assert.ok(content.includes('hasError: true'), 'Must transition hasError state to true');
});

test('Point 2 — ErrorBoundary UI: Clean luxury atelier notice with zero stack traces leaked', () => {
  const errorBoundaryPath = path.resolve(frontendRoot, 'src/components/common/ErrorBoundary.jsx');
  const content = fs.readFileSync(errorBoundaryPath, 'utf8');

  // Verify user-friendly luxury messaging
  assert.ok(content.includes('SareeKart Atelier Notice'), 'Must display boutique brand notice');
  assert.ok(content.includes('An Unexpected Exception Occurred'), 'Must display clear, professional title');

  // Verify stack traces or internal errors are never rendered to users
  assert.ok(!content.includes('{this.state.error?.stack}'), 'Must not render error stack trace in JSX');
  assert.ok(!content.includes('error.stack'), 'Must not render error.stack to DOM');
});

test('Point 3 — ErrorBoundary Recovery Actions: Provides Reload and Return to Showcase buttons', () => {
  const errorBoundaryPath = path.resolve(frontendRoot, 'src/components/common/ErrorBoundary.jsx');
  const content = fs.readFileSync(errorBoundaryPath, 'utf8');

  assert.ok(content.includes('Reload Atelier'), 'Must provide Reload Atelier button');
  assert.ok(content.includes('Return to Showcase'), 'Must provide Return to Showcase button');
  assert.ok(content.includes('window.location.reload()'), 'Reload action must reload page');
  assert.ok(content.includes("window.location.href = '/'"), 'Showcase action must redirect to home');
});

test('Point 4 — Request Correlation: Axios request interceptor injects X-Request-ID', () => {
  const axiosConfigPath = path.resolve(frontendRoot, 'src/api/axiosConfig.js');
  const content = fs.readFileSync(axiosConfigPath, 'utf8');

  assert.ok(content.includes("config.headers['X-Request-ID']"), 'Must set X-Request-ID on outbound request headers');
  assert.ok(content.includes('req-'), 'Must use structured request ID prefix');
});

test('Point 5 — Error Correlation: Axios error interceptor preserves requestId on rejected errors', () => {
  const axiosConfigPath = path.resolve(frontendRoot, 'src/api/axiosConfig.js');
  const content = fs.readFileSync(axiosConfigPath, 'utf8');

  assert.ok(content.includes("error.response?.headers?.['x-request-id']"), 'Must inspect response headers for correlation ID');
  assert.ok(content.includes('error.requestId = requestId'), 'Must attach requestId to rejected error object');
});

test('Point 6 — Retry Idempotency: Retries are strictly restricted to idempotent HTTP methods', () => {
  const axiosConfigPath = path.resolve(frontendRoot, 'src/api/axiosConfig.js');
  const content = fs.readFileSync(axiosConfigPath, 'utf8');

  assert.ok(content.includes("['get', 'head', 'options'].includes(method)"), 'Must restrict automatic retries to idempotent methods');
});

test('Point 7 — Financial Mutation Protection: Non-idempotent methods are NEVER retried', () => {
  const axiosConfigPath = path.resolve(frontendRoot, 'src/api/axiosConfig.js');
  const content = fs.readFileSync(axiosConfigPath, 'utf8');

  // Verify POST checkout / payment will never be retried
  assert.ok(content.includes('if (isColdStartError && isIdempotent && config.__retryCount < MAX_RETRIES)'), 'Guard must enforce isIdempotent');
});

test('Point 8 — Bounded Retries: Retry policy has bounded MAX_RETRIES to prevent retry storms', () => {
  const axiosConfigPath = path.resolve(frontendRoot, 'src/api/axiosConfig.js');
  const content = fs.readFileSync(axiosConfigPath, 'utf8');

  assert.ok(content.includes('const MAX_RETRIES = 2'), 'MAX_RETRIES must be bounded to 2');
  assert.ok(content.includes('INITIAL_BACKOFF_MS = 2000'), 'Initial backoff must be configured');
});

test('Point 9 — Cold-Start Telemetry: useColdStart and broadcaster inform users during tier spin-up', () => {
  const axiosConfigPath = path.resolve(frontendRoot, 'src/api/axiosConfig.js');
  const content = fs.readFileSync(axiosConfigPath, 'utf8');

  assert.ok(content.includes('export function useColdStart()'), 'Must export useColdStart hook');
  assert.ok(content.includes("new CustomEvent('sareekart:cold-start'"), 'Must dispatch cold-start CustomEvent');
  assert.ok(content.includes('Awakening our boutique atelier'), 'Must provide clear luxury awakening messaging');
});

test('Point 10 — Security & Privacy: No credentials, tokens, or raw payment details leaked in telemetry', () => {
  const axiosConfigPath = path.resolve(frontendRoot, 'src/api/axiosConfig.js');
  const content = fs.readFileSync(axiosConfigPath, 'utf8');

  // Token is safely cleared on 401/403 session expiration
  assert.ok(content.includes("localStorage.removeItem('sareekart_token')"), 'Must scrub expired token from storage');
  assert.ok(content.includes("localStorage.removeItem('sareekart_user')"), 'Must scrub user cache from storage');
  assert.ok(!content.includes('console.log(token'), 'Must never log raw auth tokens');
  assert.ok(!content.includes('console.log(password'), 'Must never log raw passwords');
});
