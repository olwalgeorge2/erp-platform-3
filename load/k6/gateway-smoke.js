import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  vus: 5,
  duration: '30s',
};

const BASE = __ENV.GW_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE}/q/metrics`);
  check(res, {
    'status 200': (r) => r.status === 200,
  });
  sleep(1);
}

