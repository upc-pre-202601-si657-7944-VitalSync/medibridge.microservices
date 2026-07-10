Feature: Health Monitoring Service API

  Background:
    * def care = call read('classpath:pe/edu/upc/medibridge/common/create-care-context.feature')
    * url healthMonitoringBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }
    * def recordedAt = java.time.LocalDateTime.now().withNano(0).toString()

  Scenario: Record observation, list observations and get structured summary
    * def observationRequest =
      """
      {
        "recordedByDoctorProfileId": null,
        "systolicBloodPressure": 130,
        "diastolicBloodPressure": 85,
        "bodyTemperature": 38.1,
        "painLevel": 8,
        "emotionalState": "ANXIOUS",
        "emotionalNotes": "Paciente ansioso",
        "clinicalNotes": "Observacion registrada por Karate",
        "recordedAt": "#(recordedAt)"
      }
      """
    Given path 'health-monitoring', 'patients', care.patientId, 'observations'
      And header Authorization = care.authorization
      And request observationRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.patientId == care.patientId
      And match response.systolicBloodPressure == 130
      And match response.diastolicBloodPressure == 85
    * def observationId = response.id

    Given path 'health-monitoring', 'patients', care.patientId, 'observations'
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response == '#[]'
      And match response[*].id contains observationId

    Given path 'health-monitoring', 'patients', care.patientId, 'summary'
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response.patientId == care.patientId
      And match response.latestBloodPressure == '130/85'
      And match response.averageTemperature == '#number'
      And match response.activeAlerts == '#number'
      And match response.observationsCount == '#number'

    Given path 'health-monitoring', 'patients', care.patientId, 'alerts', 'active'
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response == '#[]'
