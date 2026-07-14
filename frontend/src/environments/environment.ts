export const environment = {
  production: true,
  // In the container, nginx proxies /api to the backend. In `ng serve`,
  // proxy.conf.json does the same, so a relative base works everywhere.
  apiBaseUrl: '/api',
};
