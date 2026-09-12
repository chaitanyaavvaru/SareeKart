#!/usr/bin/env node
import { performance } from 'node:perf_hooks';

const BASE_URL = process.env.TARGET_URL || 'http://localhost:8081';
const CONCURRENCY = parseInt(process.env.CONCURRENCY || '100', 10);
const DURATION_SEC = parseInt(process.env.DURATION || '30', 10);

const ENDPOINTS = [
  { path: '/api/products?page=0&size=12', weight: 35, name: 'Catalog Listing' },
  { path: '/api/products/search?q=silk', weight: 25, name: 'Catalog Search' },
  { path: '/api/categories', weight: 20, name: 'Category Navigation' },
  { path: '/api/products/11', weight: 15, name: 'Product Detail' },
  { path: '/api/admin/dashboard', weight: 5, name: 'Admin Telemetry' },
];

function pickEndpoint() {
  const rand = Math.random() * 100;
  let sum = 0;
  for (const ep of ENDPOINTS) {
    sum += ep.weight;
    if (rand <= sum) return ep;
  }
  return ENDPOINTS[0];
}

console.log('===============================================================');
console.log('   SareeKart High-Concurrency Load & Stress Simulation Harness ');
console.log('===============================================================');
console.log(` Target URL     : ${BASE_URL}`);
console.log(` Concurrency    : ${CONCURRENCY} concurrent virtual shoppers`);
console.log(` Duration       : ${DURATION_SEC} seconds`);
console.log(' Endpoints:');
ENDPOINTS.forEach(ep => console.log(`   - [${ep.weight}%] ${ep.name.padEnd(20)}: ${ep.path}`));
console.log('===============================================================');

// Quick health check first
try {
  const probe = await fetch(`${BASE_URL}/api/products?page=0&size=1`, { signal: AbortSignal.timeout(4000) });
  if (!probe.ok) {
    console.error(`✖ Target health check failed with status: ${probe.status}`);
    process.exit(1);
  }
  console.log('✔ Pre-test health check passed.');
} catch (err) {
  console.error(`✖ Cannot connect to target ${BASE_URL}. Ensure SareeKart is running.`);
  console.error(err.message);
  process.exit(1);
}

// Acquire Admin Bearer Token for Admin Telemetry endpoint
let adminToken = null;
try {
  const loginRes = await fetch(`${BASE_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@sareekart.com', password: 'admin123' }),
    signal: AbortSignal.timeout(4000)
  });
  if (loginRes.ok) {
    const data = await loginRes.json();
    adminToken = data.data?.token || data.token;
    if (adminToken) console.log('✔ Admin token acquired for authenticated telemetry endpoints.');
  }
} catch {
  console.log('ℹ Proceeding with unauthenticated load profile.');
}

console.log('✔ Launching simulation...');

const latencies = [];
const statusCounts = {};
let totalRequests = 0;
let totalErrors = 0;
let isRunning = true;

const startTime = performance.now();
const endTime = startTime + (DURATION_SEC * 1000);

async function worker() {
  while (isRunning && performance.now() < endTime) {
    const ep = pickEndpoint();
    const reqUrl = `${BASE_URL}${ep.path}`;
    const headers = {};
    if (ep.name === 'Admin Telemetry' && adminToken) {
      headers['Authorization'] = `Bearer ${adminToken}`;
    }

    const t0 = performance.now();
    totalRequests++;

    try {
      const res = await fetch(reqUrl, { headers, signal: AbortSignal.timeout(10000) });
      const t1 = performance.now();
      const durationMs = t1 - t0;
      latencies.push(durationMs);

      statusCounts[res.status] = (statusCounts[res.status] || 0) + 1;
      if (!res.ok) {
        totalErrors++;
      }
      // Consume body
      await res.arrayBuffer();
    } catch (err) {
      totalErrors++;
      const key = err.name === 'TimeoutError' ? 'TIMEOUT' : 'CONN_ERR';
      statusCounts[key] = (statusCounts[key] || 0) + 1;
    }
  }
}

// Launch all concurrent workers
const workerPromises = [];
for (let i = 0; i < CONCURRENCY; i++) {
  workerPromises.push(worker());
}

// Progress reporter interval
const progressInterval = setInterval(() => {
  const elapsed = (performance.now() - startTime) / 1000;
  const currentRps = (totalRequests / elapsed).toFixed(1);
  process.stdout.write(`\r[Running] Elapsed: ${elapsed.toFixed(0)}s / ${DURATION_SEC}s | Requests: ${totalRequests} | Current RPS: ${currentRps} | Errors: ${totalErrors}   `);
}, 1000);

await Promise.all(workerPromises);
isRunning = false;
clearInterval(progressInterval);

const actualDuration = (performance.now() - startTime) / 1000;
latencies.sort((a, b) => a - b);

function percentile(arr, p) {
  if (arr.length === 0) return 0;
  const index = Math.ceil((p / 100) * arr.length) - 1;
  return arr[Math.max(0, Math.min(index, arr.length - 1))];
}

const min = latencies.length ? latencies[0].toFixed(1) : 0;
const max = latencies.length ? latencies[latencies.length - 1].toFixed(1) : 0;
const avg = latencies.length ? (latencies.reduce((a, b) => a + b, 0) / latencies.length).toFixed(1) : 0;
const p50 = percentile(latencies, 50).toFixed(1);
const p90 = percentile(latencies, 90).toFixed(1);
const p95 = percentile(latencies, 95).toFixed(1);
const p99 = percentile(latencies, 99).toFixed(1);
const rps = (totalRequests / actualDuration).toFixed(1);
const errorRate = totalRequests > 0 ? ((totalErrors / totalRequests) * 100).toFixed(2) : '0.00';

console.log('\n\n===============================================================');
console.log('                 LOAD TEST PERFORMANCE SCORECARD               ');
console.log('===============================================================');
console.log(` Total Completed Requests : ${totalRequests}`);
console.log(` Test Duration            : ${actualDuration.toFixed(2)} seconds`);
console.log(` Average Throughput       : ${rps} requests/sec`);
console.log(` Total Errors             : ${totalErrors} (${errorRate}%)`);
console.log('---------------------------------------------------------------');
console.log(' Response Latency Distribution (milliseconds):');
console.log(`   - Min Latency          : ${min} ms`);
console.log(`   - Average (Mean)       : ${avg} ms`);
console.log(`   - Median (p50)         : ${p50} ms`);
console.log(`   - 90th Percentile (p90): ${p90} ms`);
console.log(`   - 95th Percentile (p95): ${p95} ms`);
console.log(`   - 99th Percentile (p99): ${p99} ms`);
console.log(`   - Max Latency          : ${max} ms`);
console.log('---------------------------------------------------------------');
console.log(' HTTP Status Code Breakdown:');
for (const [code, count] of Object.entries(statusCounts)) {
  const pct = ((count / totalRequests) * 100).toFixed(1);
  console.log(`   - HTTP ${code.padEnd(8)}: ${count} requests (${pct}%)`);
}
console.log('===============================================================');

// Evaluate Pass / Fail against SLAs
const passedSla = parseFloat(p95) <= 500 && parseFloat(errorRate) <= 1.0;
if (passedSla) {
  console.log('✔ PASS: All High-Concurrency SLAs Met (< 500ms p95, < 1.0% error rate)');
  process.exit(0);
} else {
  console.error(`✖ FAIL: SLAs not met (p95: ${p95}ms, error rate: ${errorRate}%)`);
  process.exit(1);
}
