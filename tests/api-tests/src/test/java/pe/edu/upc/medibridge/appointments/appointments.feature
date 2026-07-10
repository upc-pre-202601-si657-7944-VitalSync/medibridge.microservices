Feature: Appointments Service API

  Background:
    * def care = call read('classpath:pe/edu/upc/medibridge/common/create-care-context.feature')
    * url appointmentsBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }
    * def nextBusinessSlot =
      """
      function() {
        var LocalDateTime = Java.type('java.time.LocalDateTime');
        var DayOfWeek = Java.type('java.time.DayOfWeek');
        var slot = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
        while (slot.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
          slot = slot.plusDays(1);
        }
        return slot.toString();
      }
      """

  Scenario: Schedule and query a family visit
    * def startsAt = nextBusinessSlot()
    * def appointmentRequest =
      """
      {
        "patientId": #(care.patientId),
        "familyMemberProfileId": #(care.familyMemberProfileId),
        "startsAt": "#(startsAt)",
        "durationInMinutes": 60,
        "reason": "Visita familiar Karate"
      }
      """
    Given path 'appointments', 'family-visits'
      And header Authorization = care.authorization
      And request appointmentRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.patientId == care.patientId
      And match response.familyMemberProfileId == care.familyMemberProfileId
      And match response.appointmentType == 'FAMILY_VISIT'
      And match response.status == 'SCHEDULED'
    * def appointmentId = response.id

    Given path 'appointments', appointmentId
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response.id == appointmentId
      And match response.patientId == care.patientId

    Given path 'appointments', 'patient', care.patientId
      And header Authorization = care.authorization
    When method get
    Then status 200
      And match response == '#[]'
      And match response[*].id contains appointmentId
