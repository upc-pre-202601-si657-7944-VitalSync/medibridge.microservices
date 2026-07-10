Feature: Payments Service API

  Background:
    * def auth = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * url paymentsBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }

  Scenario: Approve mock family premium subscription and query active subscription
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
      And match response.id == '#number'
      And match response.userId == auth.userId
      And match response.status == 'ACTIVE'
      And match response.plan.commercialLine == 'FAMILY'
      And match response.plan.planType == 'FAMILY_PREMIUM'

    Given path 'subscriptions', 'users', auth.userId, 'active'
      And header Authorization = auth.authorization
    When method get
    Then status 200
      And match response.userId == auth.userId
      And match response.status == 'ACTIVE'

  Scenario: Return not found when active subscription does not exist
    Given path 'subscriptions', 'users', 999999999, 'active'
      And header Authorization = auth.authorization
    When method get
    Then status 404
