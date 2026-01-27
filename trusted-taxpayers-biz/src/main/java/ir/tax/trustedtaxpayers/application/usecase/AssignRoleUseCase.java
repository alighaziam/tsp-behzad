package ir.tax.trustedtaxpayers.application.usecase;

import ir.tax.trustedtaxpayers.application.service.UserRoleService;

import java.util.UUID;

public class AssignRoleUseCase {
    private final UserRoleService userRoleService;

    public AssignRoleUseCase(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    public void execute(UUID userId, String roleName) {
        userRoleService.assignRole(userId, roleName);
    }
}



