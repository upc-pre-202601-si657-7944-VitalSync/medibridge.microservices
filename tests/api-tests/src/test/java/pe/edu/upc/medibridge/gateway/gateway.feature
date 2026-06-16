Feature: API Gateway routing

  Background:
    * url gatewayBaseUrl
    * def uuid = java.util.UUID.randomUUID().toString()
    * def username = 'gateway_' + uuid
    * def password = 'Test123456!'

  Scenario: Route IAM authentication and protected users endpoints
    * def signUpRequest =
      """
      {
        "username": "#(username)",
        "password": "#(password)",
        "roles": ["ROLE_USER"]
      }
      """
    Given path 'authentication', 'sign-up'
      And request signUpRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.username == username
    * def userId = response.id

    * def signInRequest =
      """
      {
        "username": "#(username)",
        "password": "#(password)"
      }
      """
    Given path 'authentication', 'sign-in'
      And request signInRequest
    When method post
    Then status 200
      And match response.token == '#string'
    * def authorization = 'Bearer ' + response.token

    Given path 'users', userId
      And header Authorization = authorization
    When method get
    Then status 200
      And match response.id == userId
      And match response.username == username

  Scenario: Route Profiles endpoints with a gateway-issued token
    * def signUpRequest = { username: '#(username)', password: '#(password)', roles: ['ROLE_USER'] }
    Given path 'authentication', 'sign-up'
      And request signUpRequest
    When method post
    Then status 201
    * def userId = response.id

    * def signInRequest = { username: '#(username)', password: '#(password)' }
    Given path 'authentication', 'sign-in'
      And request signInRequest
    When method post
    Then status 200
    * def authorization = 'Bearer ' + response.token

    * def patientName = 'Gateway Patient ' + uuid.substring(0, 8)
    * def patientRequest = { fullName: '#(patientName)' }
    Given path 'profiles', 'patients'
      And header Authorization = authorization
      And request patientRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.fullName == patientName
    * def patientId = response.id

    Given path 'profiles', 'patients', patientId
      And header Authorization = authorization
    When method get
    Then status 200
      And match response.id == patientId
      And match response.fullName == patientName

    * def doctorName = 'Gateway Doctor ' + uuid.substring(0, 8)
    * def doctorRequest = { userId: '#(userId)', fullName: '#(doctorName)' }
    Given path 'profiles', 'doctors'
      And header Authorization = authorization
      And request doctorRequest
    When method post
    Then status 201
      And match response.userId == userId
      And match response.fullName == doctorName

  Scenario: Block internal endpoints at the gateway
    Given path 'internal', 'users', 1, 'exists'
    When method get
    Then status 403

    Given path 'internal', 'profiles', 'patients', 1, 'exists'
    When method get
    Then status 403

  Scenario: Expose proxied OpenAPI docs for downstream services
    * url gatewayUrl
    Given path 'docs', 'iam', 'v3', 'api-docs'
    When method get
    Then status 200
      And match response.openapi == '#string'

    Given path 'docs', 'profiles', 'v3', 'api-docs'
    When method get
    Then status 200
      And match response.openapi == '#string'
