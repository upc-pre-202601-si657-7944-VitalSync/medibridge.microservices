Feature: Reports Analytics Service API

  Background:
    * def care = call read('classpath:pe/edu/upc/medibridge/common/create-care-context.feature')
    * url reportsBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }
    * def startDate = java.time.LocalDate.now().minusDays(7).toString()
    * def endDate = java.time.LocalDate.now().toString()

  Scenario: Generate, query and download a clinical report PDF
    * def reportRequest =
      """
      {
        "patientId": #(care.patientId),
        "reportType": "FULL_CLINICAL",
        "startDate": "#(startDate)",
        "endDate": "#(endDate)"
      }
      """
    Given path 'clinical-reports'
      And header Authorization = care.authorization
      And request reportRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.patientId == care.patientId
      And match response.reportType == 'FULL_CLINICAL'
      And match response.summary == '#string'
      And match response.sections == '#[]'
    * def reportId = response.id

    Given path 'clinical-reports', reportId
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response.id == reportId
      And match response.patientId == care.patientId

    Given path 'clinical-reports', reportId, 'pdf'
      And header Authorization = care.authorization
    When method post
    Then status 200
      And match responseHeaders['Content-Type'][0] contains 'application/pdf'
      And match responseHeaders['Content-Disposition'][0] contains 'attachment'
