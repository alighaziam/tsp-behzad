package ir.tax.trustedtaxpayers.application.usecase;

import ir.tax.trustedtaxpayers.application.service.UserRoleService;

import java.util.UUID;

public class CreateUserUseCase {
    private final UserRoleService userRoleService;

    public CreateUserUseCase(UserRoleService userRoleService) {
        this.userRoleService = userRoleService;
    }

    public UUID execute(String username, String passwordHash, String email) {
        return userRoleService.createUser(username, passwordHash, email);
    }
}



