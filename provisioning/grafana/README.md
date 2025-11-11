Grafana provisioning (dev example)

This example auto-loads the API Gateway dashboard in a local Grafana:

- Datasource: Prometheus at http://localhost:9090
- Dashboard path: mount dashboards under /var/lib/grafana/dashboards

docker run -it --rm -p 3000:3000 \
  -v $PWD/provisioning/grafana/datasources:/etc/grafana/provisioning/datasources \
  -v $PWD/provisioning/grafana/dashboards:/etc/grafana/provisioning/dashboards \
  -v $PWD/dashboards/grafana:/var/lib/grafana/dashboards \
  grafana/grafana:10.4.2

Login at http://localhost:3000 (admin/admin) and you should see the dashboard under folder "API Gateway".

