Feature: Profiles Service API

  Background:
    * def auth = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * url profilesBaseUrl
    * configure headers = { X-Internal-Token: '#(internalToken)' }
    * def uuid = java.util.UUID.randomUUID().toString()
    * def fullNameSuffix = uuid.substring(0, 8)

  Scenario: Reject protected profiles endpoint without token
    * def patientRequest =
      """
      {
        "fullName": "Patient Unauthorized #(fullNameSuffix)"
      }
      """
    Given path 'profiles', 'patients'
      And request patientRequest
    When method post
    Then status 401

  Scenario: Create and get patient profile
    * def patientName = 'Patient ' + fullNameSuffix
    * def patientRequest =
      """
      {
        "fullName": "#(patientName)"
      }
      """
    Given path 'profiles', 'patients'
      And header Authorization = auth.authorization
      And request patientRequest
    When method post
    Then status 201
      And match response == { id: '#number', fullName: '#(patientName)' }
    * def patientId = response.id

    Given path 'profiles', 'patients', patientId
      And header Authorization = auth.authorization
    When method get
    Then status 200
      And match response.id == patientId
      And match response.fullName == patientName

    Given path 'internal', 'profiles', 'patients', patientId, 'exists'
    When method get
    Then status 200
      And match response == 'true'

  Scenario: Create and get doctor profile using an IAM user
    * def doctorName = 'Doctor ' + fullNameSuffix
    * def doctorRequest =
      """
      {
        "userId": #(auth.userId),
        "fullName": "#(doctorName)"
      }
      """
    Given path 'profiles', 'doctors'
      And header Authorization = auth.authorization
      And request doctorRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.userId == auth.userId
      And match response.fullName == doctorName
    * def doctorProfileId = response.id

    Given path 'profiles', 'doctors', doctorProfileId
      And header Authorization = auth.authorization
    When method get
    Then status 200
      And match response.id == doctorProfileId
      And match response.userId == auth.userId
      And match response.fullName == doctorName

  Scenario: Create and get family member profile using an IAM user
    * def familyName = 'Family ' + fullNameSuffix
    * def familyRequest =
      """
      {
        "userId": #(auth.userId),
        "fullName": "#(familyName)"
      }
      """
    Given path 'profiles', 'family-members'
      And header Authorization = auth.authorization
      And request familyRequest
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.userId == auth.userId
      And match response.fullName == familyName
    * def familyMemberProfileId = response.id

    Given path 'profiles', 'family-members', familyMemberProfileId
      And header Authorization = auth.authorization
    When method get
    Then status 200
      And match response.id == familyMemberProfileId
      And match response.userId == auth.userId
      And match response.fullName == familyName

  Scenario: Assign doctor and family member to a patient
    * def familyAuth = call read('classpath:pe/edu/upc/medibridge/common/create-user-token.feature')
    * url profilesBaseUrl
    * def carePatientName = 'Care Patient ' + fullNameSuffix
    * def patientRequest = { fullName: '#(carePatientName)' }
    Given path 'profiles', 'patients'
      And header Authorization = auth.authorization
      And request patientRequest
    When method post
    Then status 201
    * def patientId = response.id

    * def careDoctorName = 'Care Doctor ' + fullNameSuffix
    * def doctorRequest = { userId: '#(auth.userId)', fullName: '#(careDoctorName)' }
    Given path 'profiles', 'doctors'
      And header Authorization = auth.authorization
      And request doctorRequest
    When method post
    Then status 201
    * def doctorProfileId = response.id

    * def careFamilyName = 'Care Family ' + fullNameSuffix
    * def familyRequest = { userId: '#(familyAuth.userId)', fullName: '#(careFamilyName)' }
    Given path 'profiles', 'family-members'
      And header Authorization = auth.authorization
      And request familyRequest
    When method post
    Then status 201
    * def familyMemberProfileId = response.id

    Given path 'profiles', 'patients', patientId, 'doctors', doctorProfileId
      And header Authorization = auth.authorization
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.patientId == patientId
      And match response.doctorProfileId == doctorProfileId
      And match response.active == true

    Given path 'profiles', 'patients', patientId, 'family-members', familyMemberProfileId
      And header Authorization = auth.authorization
    When method post
    Then status 201
      And match response.id == '#number'
      And match response.patientId == patientId
      And match response.familyMemberProfileId == familyMemberProfileId
      And match response.active == true

    Given path 'internal', 'profiles', 'doctors', doctorProfileId, 'can-attend', patientId
    When method get
    Then status 200
      And match response == 'true'

    Given path 'internal', 'profiles', 'family-members', familyMemberProfileId, 'can-visit', patientId
    When method get
    Then status 200
      And match response == 'true'

  Scenario: Reject duplicate doctor profile for the same IAM user
    * def duplicateDoctorName = 'Duplicate Doctor ' + fullNameSuffix
    * def doctorRequest = { userId: '#(auth.userId)', fullName: '#(duplicateDoctorName)' }
    Given path 'profiles', 'doctors'
      And header Authorization = auth.authorization
      And request doctorRequest
    When method post
    Then status 201

    Given path 'profiles', 'doctors'
      And header Authorization = auth.authorization
      And request doctorRequest
    When method post
    Then status 409

  Scenario: Return not found for missing profile resources
    Given path 'profiles', 'patients', 999999999
      And header Authorization = auth.authorization
    When method get
    Then status 404

    Given path 'internal', 'profiles', 'patients', 999999999, 'exists'
    When method get
    Then status 200
      And match response == 'false'
