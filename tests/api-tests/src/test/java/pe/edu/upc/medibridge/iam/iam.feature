Feature: IAM Service API

  Background:
    * url iamBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }
    * def uuid = java.util.UUID.randomUUID().toString()
    * def username = 'iam_' + uuid
    * def password = 'Test123456!'

  Scenario: Sign up and sign in a user
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
      And match response.roles contains 'ROLE_USER'
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
      And match response.id == userId
      And match response.username == username
      And match response.token == '#string'

  Scenario: Get public JWKS
    Given path 'jwks', '.well-known', 'jwks.json'
    When method get
    Then status 200
      And match response.keys == '#[]'
      And match response.keys[0] contains { kty: 'RSA', alg: 'RS256', kid: '#string' }

  Scenario: Reject protected users endpoint without token
    Given path 'users'
    When method get
    Then status 401

  Scenario: Get users, roles and user by id with token
    * def auth = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * url iamBaseUrl
    Given path 'users'
      And header Authorization = auth.authorization
    When method get
    Then status 200
      And match response == '#[]'

    Given path 'users', auth.userId
      And header Authorization = auth.authorization
    When method get
    Then status 200
      And match response.id == auth.userId
      And match response.username == auth.username
      And match response.roles contains 'ROLE_USER'

    Given path 'roles'
      And header Authorization = auth.authorization
    When method get
    Then status 200
      And match response == '#[]'
      And match response[*].name contains 'ROLE_USER'

  Scenario: Check internal user existence endpoint
    * def auth = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * url iamBaseUrl
    Given path 'internal', 'users', auth.userId, 'exists'
    When method get
    Then status 200
      And match response == 'true'

    Given path 'internal', 'users', 999999999, 'exists'
    When method get
    Then status 200
      And match response == 'false'
