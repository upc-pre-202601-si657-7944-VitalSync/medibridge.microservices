function fn() {
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);

  return {
    iamBaseUrl: karate.properties['iam.baseUrl'] || 'http://localhost:8081/api/v1',
    profilesBaseUrl: karate.properties['profiles.baseUrl'] || 'http://localhost:8082/api/v1',
    gatewayUrl: karate.properties['gateway.url'] || 'http://localhost:8080',
    gatewayBaseUrl: karate.properties['gateway.baseUrl'] || 'http://localhost:8080/api/v1',
    internalToken: karate.properties['internal.token'] || 'local-internal-token'
  };
}
