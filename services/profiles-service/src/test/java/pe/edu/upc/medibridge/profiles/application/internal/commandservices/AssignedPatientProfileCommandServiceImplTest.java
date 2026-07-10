package pe.edu.upc.medibridge.profiles.application.internal.commandservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.medibridge.profiles.application.internal.outboundservices.acl.ExternalSubscriptionService;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.DoctorPatientAssignment;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.DoctorProfile;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.PatientProfile;
import pe.edu.upc.medibridge.profiles.domain.model.commands.AssignDoctorToPatientCommand;
import pe.edu.upc.medibridge.profiles.domain.model.commands.CreateAssignedPatientProfileCommand;
import pe.edu.upc.medibridge.profiles.domain.model.commands.CreatePatientProfileCommand;
import pe.edu.upc.medibridge.profiles.domain.model.commands.LinkFamilyMemberToPatientCommand;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.ActiveSubscriptionRequiredException;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.PatientLimitExceededException;
import pe.edu.upc.medibridge.profiles.domain.services.CareRelationshipCommandService;
import pe.edu.upc.medibridge.profiles.domain.services.PatientProfileCommandService;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorPatientAssignmentRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorProfileRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.FamilyMemberProfileRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.FamilyPatientLinkRepository;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignedPatientProfileCommandServiceImplTest {
    private static final Long USER_ID = 7L;
    private static final Long DOCTOR_PROFILE_ID = 20L;
    private static final Long PATIENT_ID = 13L;

    @Mock
    private PatientProfileCommandService patientProfileCommandService;
    @Mock
    private CareRelationshipCommandService careRelationshipCommandService;
    @Mock
    private DoctorProfileRepository doctorProfileRepository;
    @Mock
    private FamilyMemberProfileRepository familyMemberProfileRepository;
    @Mock
    private DoctorPatientAssignmentRepository doctorPatientAssignmentRepository;
    @Mock
    private FamilyPatientLinkRepository familyPatientLinkRepository;
    @Mock
    private ExternalSubscriptionService externalSubscriptionService;
    @Mock
    private DoctorProfile doctorProfile;
    @Mock
    private PatientProfile patientProfile;

    private AssignedPatientProfileCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        var eligibilityPolicy = new CareRelationshipEligibilityPolicy(
                doctorProfileRepository,
                familyMemberProfileRepository,
                doctorPatientAssignmentRepository,
                familyPatientLinkRepository,
                externalSubscriptionService);
        service = new AssignedPatientProfileCommandServiceImpl(
                patientProfileCommandService,
                careRelationshipCommandService,
                eligibilityPolicy);
    }

    @Test
    void createsPatientOnlyAfterDoctorEligibilityAndAssignsItInSameCommand() throws NoSuchMethodException {
        givenAuthenticatedDoctor();
        when(doctorProfile.getId()).thenReturn(DOCTOR_PROFILE_ID);
        when(externalSubscriptionService.findActiveSubscriptionByUserId(USER_ID))
                .thenReturn(Optional.of(institutionalSubscription(50)));
        when(doctorPatientAssignmentRepository.countActivePatientsByDoctorProfileId(DOCTOR_PROFILE_ID))
                .thenReturn(3L);
        when(patientProfileCommandService.handle(argThat(this::isMariaPatientCommand)))
                .thenReturn(Optional.of(patientProfile));
        when(patientProfile.getId()).thenReturn(PATIENT_ID);
        when(careRelationshipCommandService.handle(argThat(this::isExpectedAssignmentCommand)))
                .thenReturn(Optional.of(mock(DoctorPatientAssignment.class)));

        var result = service.handle(new CreateAssignedPatientProfileCommand("Maria Perez", USER_ID));

        assertThat(result).containsSame(patientProfile);
        InOrder eligibilityBeforePersistence = inOrder(
                doctorProfileRepository,
                externalSubscriptionService,
                doctorPatientAssignmentRepository,
                patientProfileCommandService,
                careRelationshipCommandService);
        eligibilityBeforePersistence.verify(doctorProfileRepository).findByUserId(USER_ID);
        eligibilityBeforePersistence.verify(externalSubscriptionService).findActiveSubscriptionByUserId(USER_ID);
        eligibilityBeforePersistence.verify(doctorPatientAssignmentRepository)
                .countActivePatientsByDoctorProfileId(DOCTOR_PROFILE_ID);
        eligibilityBeforePersistence.verify(patientProfileCommandService)
                .handle(argThat(this::isMariaPatientCommand));
        eligibilityBeforePersistence.verify(careRelationshipCommandService)
                .handle(argThat(this::isExpectedAssignmentCommand));

        Method handle = AssignedPatientProfileCommandServiceImpl.class
                .getMethod("handle", CreateAssignedPatientProfileCommand.class);
        assertThat(handle.isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void doesNotCreatePatientWithoutActiveInstitutionalSubscription() {
        givenAuthenticatedDoctor();
        when(externalSubscriptionService.findActiveSubscriptionByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new CreateAssignedPatientProfileCommand("Maria Perez", USER_ID)))
                .isInstanceOf(ActiveSubscriptionRequiredException.class)
                .hasMessageContaining("institutional subscription");

        verify(doctorPatientAssignmentRepository, never()).countActivePatientsByDoctorProfileId(DOCTOR_PROFILE_ID);
        verifyNoInteractions(patientProfileCommandService, careRelationshipCommandService);
    }

    @Test
    void doesNotCreatePatientWhenInstitutionalPatientLimitWasReached() {
        givenAuthenticatedDoctor();
        when(doctorProfile.getId()).thenReturn(DOCTOR_PROFILE_ID);
        when(externalSubscriptionService.findActiveSubscriptionByUserId(USER_ID))
                .thenReturn(Optional.of(institutionalSubscription(3)));
        when(doctorPatientAssignmentRepository.countActivePatientsByDoctorProfileId(DOCTOR_PROFILE_ID))
                .thenReturn(3L);

        assertThatThrownBy(() -> service.handle(new CreateAssignedPatientProfileCommand("Maria Perez", USER_ID)))
                .isInstanceOf(PatientLimitExceededException.class)
                .hasMessageContaining("limit reached");

        verifyNoInteractions(patientProfileCommandService, careRelationshipCommandService);
    }

    @Test
    void existingCareRelationshipWritesAreTransactional() throws NoSuchMethodException {
        Method doctorHandle = CareRelationshipCommandServiceImpl.class
                .getMethod("handle", AssignDoctorToPatientCommand.class);
        Method familyHandle = CareRelationshipCommandServiceImpl.class
                .getMethod("handle", LinkFamilyMemberToPatientCommand.class);
        assertThat(doctorHandle.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(familyHandle.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private void givenAuthenticatedDoctor() {
        when(doctorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(doctorProfile));
        when(doctorProfile.getUserId()).thenReturn(USER_ID);
    }

    private ExternalSubscriptionService.ActiveSubscription institutionalSubscription(int maxPatients) {
        return new ExternalSubscriptionService.ActiveSubscription(
                USER_ID,
                "INSTITUTION",
                "INSTITUTION_BASIC",
                maxPatients);
    }

    private boolean isMariaPatientCommand(CreatePatientProfileCommand command) {
        return command != null && "Maria Perez".equals(command.fullName());
    }

    private boolean isExpectedAssignmentCommand(AssignDoctorToPatientCommand command) {
        return command != null
                && DOCTOR_PROFILE_ID.equals(command.doctorProfileId())
                && PATIENT_ID.equals(command.patientId())
                && USER_ID.equals(command.requestedByUserId());
    }
}
