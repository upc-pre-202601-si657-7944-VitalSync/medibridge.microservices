package pe.edu.upc.medibridge.profiles.interfaces.rest.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.profiles.application.internal.outboundservices.acl.ExternalIamContextService;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.DoctorProfile;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.FamilyMemberProfile;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.PatientProfile;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.InvalidProfileRequestException;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorPatientAssignmentRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorProfileRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.FamilyMemberProfileRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.FamilyPatientLinkRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.PatientProfileRepository;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.ChatContactResource;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/profiles/chat-contacts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profile Chat Contacts", description = "Care Team Chat Contact Endpoints")
public class ProfileChatContactsController {
    private static final String CONTACT_TYPE_DOCTOR = "DOCTOR";
    private static final String CONTACT_TYPE_FAMILY_MEMBER = "FAMILY_MEMBER";

    private final DoctorPatientAssignmentRepository doctorPatientAssignmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final ExternalIamContextService externalIamContextService;
    private final FamilyMemberProfileRepository familyMemberProfileRepository;
    private final FamilyPatientLinkRepository familyPatientLinkRepository;
    private final PatientProfileRepository patientProfileRepository;

    public ProfileChatContactsController(
            DoctorPatientAssignmentRepository doctorPatientAssignmentRepository,
            DoctorProfileRepository doctorProfileRepository,
            ExternalIamContextService externalIamContextService,
            FamilyMemberProfileRepository familyMemberProfileRepository,
            FamilyPatientLinkRepository familyPatientLinkRepository,
            PatientProfileRepository patientProfileRepository) {
        this.doctorPatientAssignmentRepository = doctorPatientAssignmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.externalIamContextService = externalIamContextService;
        this.familyMemberProfileRepository = familyMemberProfileRepository;
        this.familyPatientLinkRepository = familyPatientLinkRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    @GetMapping
    public ResponseEntity<List<ChatContactResource>> getAuthenticatedUserChatContacts(@AuthenticationPrincipal Jwt jwt) {
        var userId = resolveAuthenticatedUserId(jwt);
        var contacts = new LinkedHashMap<String, ChatContactResource>();

        doctorProfileRepository.findByUserId(userId)
                .ifPresent(doctorProfile -> doctorContacts(doctorProfile, userId)
                        .forEach(contact -> contacts.put(contactKey(contact), contact)));

        familyMemberProfileRepository.findByUserId(userId)
                .ifPresent(familyMemberProfile -> familyContacts(familyMemberProfile, userId)
                        .forEach(contact -> contacts.put(contactKey(contact), contact)));

        return ResponseEntity.ok(contacts.values().stream()
                .sorted(Comparator
                        .comparing(ChatContactResource::patientFullName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(ChatContactResource::fullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList());
    }

    private List<ChatContactResource> doctorContacts(DoctorProfile doctorProfile, Long currentUserId) {
        var patientIds = doctorPatientAssignmentRepository
                .findAllByDoctorProfileIdAndActiveTrue(doctorProfile.getId())
                .stream()
                .map(assignment -> assignment.getPatientId())
                .distinct()
                .toList();
        var patientById = patientProfileRepository.findAllById(patientIds)
                .stream()
                .collect(Collectors.toMap(PatientProfile::getId, Function.identity()));
        var familyLinks = patientIds.stream()
                .flatMap(patientId -> familyPatientLinkRepository.findAllByPatientIdAndActiveTrue(patientId).stream())
                .toList();
        var familyMemberIds = familyLinks.stream()
                .map(link -> link.getFamilyMemberProfileId())
                .distinct()
                .toList();
        var familyMemberById = familyMemberProfileRepository.findAllById(familyMemberIds)
                .stream()
                .collect(Collectors.toMap(FamilyMemberProfile::getId, Function.identity()));

        return familyLinks.stream()
                .map(link -> {
                    var familyMember = familyMemberById.get(link.getFamilyMemberProfileId());
                    var patient = patientById.get(link.getPatientId());
                    if (familyMember == null || patient == null || currentUserId.equals(familyMember.getUserId())) {
                        return Optional.<ChatContactResource>empty();
                    }
                    return Optional.of(new ChatContactResource(
                            familyMember.getUserId(),
                            familyMember.getFullName(),
                            CONTACT_TYPE_FAMILY_MEMBER,
                            familyMember.getId(),
                            patient.getId(),
                            patient.getFullName()));
                })
                .flatMap(Optional::stream)
                .toList();
    }

    private List<ChatContactResource> familyContacts(FamilyMemberProfile familyMemberProfile, Long currentUserId) {
        var familyLinks = familyPatientLinkRepository
                .findAllByFamilyMemberProfileIdAndActiveTrue(familyMemberProfile.getId());
        var patientIds = familyLinks.stream()
                .map(link -> link.getPatientId())
                .distinct()
                .toList();
        var patientById = patientProfileRepository.findAllById(patientIds)
                .stream()
                .collect(Collectors.toMap(PatientProfile::getId, Function.identity()));
        var doctorAssignments = patientIds.stream()
                .flatMap(patientId -> doctorPatientAssignmentRepository.findAllByPatientIdAndActiveTrue(patientId).stream())
                .toList();
        var doctorIds = doctorAssignments.stream()
                .map(assignment -> assignment.getDoctorProfileId())
                .distinct()
                .toList();
        var doctorById = doctorProfileRepository.findAllById(doctorIds)
                .stream()
                .collect(Collectors.toMap(DoctorProfile::getId, Function.identity()));

        return doctorAssignments.stream()
                .map(assignment -> {
                    var doctor = doctorById.get(assignment.getDoctorProfileId());
                    var patient = patientById.get(assignment.getPatientId());
                    if (doctor == null || patient == null || currentUserId.equals(doctor.getUserId())) {
                        return Optional.<ChatContactResource>empty();
                    }
                    return Optional.of(new ChatContactResource(
                            doctor.getUserId(),
                            doctor.getFullName(),
                            CONTACT_TYPE_DOCTOR,
                            doctor.getId(),
                            patient.getId(),
                            patient.getFullName()));
                })
                .flatMap(Optional::stream)
                .toList();
    }

    private String contactKey(ChatContactResource contact) {
        return contact.userId() + "-" + contact.patientId();
    }

    private Long resolveAuthenticatedUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new InvalidProfileRequestException("Authenticated user is required");
        }
        return externalIamContextService.findUserIdByUsername(jwt.getSubject())
                .orElseThrow(() -> new InvalidProfileRequestException("Authenticated user was not found"));
    }
}
