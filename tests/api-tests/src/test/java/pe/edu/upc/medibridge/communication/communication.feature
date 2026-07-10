Feature: Communication Service API

  Background:
    * def sender = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * def recipient = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * url communicationBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }

  Scenario: Connect users, send message and query chat history
    Given path 'chat', 'users', 'connect'
      And header Authorization = sender.authorization
      And request { userId: '#(sender.userId)', username: '#(sender.username)', fullName: 'Karate Sender' }
    When method post
    Then status 200
      And match response.userId == sender.userId
      And match response.status == 'ONLINE'

    Given path 'chat', 'users', 'connect'
      And header Authorization = recipient.authorization
      And request { userId: '#(recipient.userId)', username: '#(recipient.username)', fullName: 'Karate Recipient' }
    When method post
    Then status 200
      And match response.userId == recipient.userId
      And match response.status == 'ONLINE'

    * def sentAt = java.time.Instant.now().toString()
    Given path 'chat', 'messages'
      And header Authorization = sender.authorization
      And request { recipientUserId: '#(recipient.userId)', content: 'Hola desde Karate', sentAt: '#(sentAt)' }
    When method post
    Then status 201
      And match response.id == '#string'
      And match response.senderUserId == sender.userId
      And match response.recipientUserId == recipient.userId
      And match response.content == 'Hola desde Karate'

    Given path 'chat', 'messages', sender.userId, recipient.userId
      And header Authorization = sender.authorization
    When method get
    Then status 200
      And match response == '#[]'
      And match response[*].content contains 'Hola desde Karate'
