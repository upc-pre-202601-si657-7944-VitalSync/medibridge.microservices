@ignore
Feature: Create an authenticated family care context

  Scenario: Create premium family user, patient profile and family patient link
    * def auth = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * def uuid = java.util.UUID.randomUUID().toString()
    * def suffix = uuid.substring(0, 8)

    * url paymentsBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }
    * def subscriptionRequest =
      """
      {
        "userId": #(auth.userId),
        "commercialLine": "FAMILY",
        "planType": "FAMILY_PREMIUM",
        "billingCycle": "MONTHLY",
        "returnUrl": "http://localhost:8080"
      }
      """
    Given path 'subscriptions', 'mock', 'approve'
      And header Authorization = auth.authorization
      And request subscriptionRequest
    When method post
    Then status 200
      And match response.userId == auth.userId
      And match response.status == 'ACTIVE'

    * url profilesBaseUrl
    * def patientName = 'Karate Patient ' + suffix
    Given path 'profiles', 'patients'
      And header Authorization = auth.authorization
      And request { fullName: '#(patientName)' }
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.fullName == patientName
    * def patientId = response.id

    * def familyName = 'Karate Family ' + suffix
    Given path 'profiles', 'family-members'
      And header Authorization = auth.authorization
      And request { fullName: '#(familyName)' }
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.userId == auth.userId
      And match response.fullName == familyName
    * def familyMemberProfileId = response.id

    Given path 'profiles', 'patients', patientId, 'family-members', familyMemberProfileId
      And header Authorization = auth.authorization
    When method post
    Then status 201
      And match response.patientId == patientId
      And match response.familyMemberProfileId == familyMemberProfileId
      And match response.active == true

    * def userId = auth.userId
    * def username = auth.username
    * def token = auth.token
    * def authorization = auth.authorization
