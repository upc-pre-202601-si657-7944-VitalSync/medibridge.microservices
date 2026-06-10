function fn() {
  karate.configure('connectTimeout', 5000);
  karate.configure('readTimeout', 10000);

  return {
    iamBaseUrl: karate.properties['iam.baseUrl'] || 'http://localhost:8081/api/v1',
    profilesBaseUrl: karate.properties['profiles.baseUrl'] || 'http://localhost:8082/api/v1'
  };
}
