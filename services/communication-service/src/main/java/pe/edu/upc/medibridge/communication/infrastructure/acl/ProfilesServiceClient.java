package pe.edu.upc.medibridge.communication.infrastructure.acl;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Objects;

@FeignClient(name = "profiles-service", url = "${medibridge.services.profiles.base-url}", path = "/api/v1/internal/profiles")
public interface ProfilesServiceClient {

    @GetMapping("/patients/{patientId}/care-team-members")
    CareTeamMembersResource getCareTeamMembers(@PathVariable Long patientId);

    default List<Long> getCareTeamUserIds(Long patientId) {
        var resource = getCareTeamMembers(patientId);
        if (resource == null || resource.careTeamUserIds() == null) {
            return List.of();
        }
        return resource.careTeamUserIds()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}

