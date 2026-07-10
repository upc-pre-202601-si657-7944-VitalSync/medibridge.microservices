Feature: Medication Service API

  Background:
    * def care = call read('classpath:pe/edu/upc/medibridge/common/create-care-context.feature')
    * url medicationBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }
    * def expirationDate = java.time.LocalDate.now().plusMonths(6).toString()

  Scenario: Register medication, query by patient and low stock
    * def medicationRequest =
      """
      {
        "patientId": #(care.patientId),
        "name": "Paracetamol Karate",
        "dosageAmount": 500.00,
        "dosageUnit": "MG",
        "administrationRoute": "ORAL",
        "stockQuantity": 2,
        "lowStockThreshold": 5,
        "expirationDate": "#(expirationDate)"
      }
      """
    Given path 'medications'
      And header Authorization = care.authorization
      And request medicationRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.patientId == care.patientId
      And match response.name == 'Paracetamol Karate'
      And match response.stockQuantity == 2
      And match response.lowStockThreshold == 5
    * def medicationId = response.id

    Given path 'medications', medicationId
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response.id == medicationId
      And match response.patientId == care.patientId

    Given path 'medications', 'patients', care.patientId
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response == '#[]'
      And match response[*].id contains medicationId

    Given path 'medications', 'patients', care.patientId, 'low-stock'
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response == '#[]'
      And match response[*].medicationId contains medicationId
