import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<500'],
  },
  scenarios: {
    steady: {
      executor: 'constant-arrival-rate',
      rate: 10,
      timeUnit: '1s',
      duration: '45s',
      preAllocatedVUs: 5,
      maxVUs: 20,
    },
    spike: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 10,
      maxVUs: 50,
      stages: [
        { target: 50, duration: '15s' },
        { target: 5, duration: '15s' }
      ]
    }
  }
};

const BASE = __ENV.GW_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE}/q/metrics`);
  check(res, {
    'status 200': (r) => r.status === 200,
  });
  sleep(1);
}
