function fn() {
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);

  return {
    iamBaseUrl: karate.properties['iam.baseUrl'] || 'http://localhost:8081/api/v1',
    profilesBaseUrl: karate.properties['profiles.baseUrl'] || 'http://localhost:8082/api/v1',
    paymentsBaseUrl: karate.properties['payments.baseUrl'] || 'http://localhost:8083/api/v1',
    appointmentsBaseUrl: karate.properties['appointments.baseUrl'] || 'http://localhost:8084/api/v1',
    healthMonitoringBaseUrl: karate.properties['healthmonitoring.baseUrl'] || 'http://localhost:8085/api/v1',
    medicationBaseUrl: karate.properties['medication.baseUrl'] || 'http://localhost:8086/api/v1',
    reportsBaseUrl: karate.properties['reports.baseUrl'] || 'http://localhost:8087/api/v1',
    communicationBaseUrl: karate.properties['communication.baseUrl'] || 'http://localhost:8088/api/v1',
    gatewayUrl: karate.properties['gateway.url'] || 'http://localhost:8080',
    gatewayBaseUrl: karate.properties['gateway.baseUrl'] || 'http://localhost:8080/api/v1',
    internalToken: karate.properties['internal.token'] || 'local-internal-token'
  };
}
