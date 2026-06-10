@ignore
Feature: Create an IAM user and issue a JWT

  Scenario: Create user and sign in
    * url iamBaseUrl
    * def uuid = java.util.UUID.randomUUID().toString()
    * def username = 'karate_' + uuid
    * def password = 'Test123456!'
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
    * def token = response.token
    * def authorization = 'Bearer ' + token
