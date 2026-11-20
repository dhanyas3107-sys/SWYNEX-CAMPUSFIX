package com.campusfix.service;

import com.campusfix.exception.UnauthorizedOperationException;
import com.campusfix.model.Permission;
import com.campusfix.model.User;

/**
 * Role-based access control. Every service method that changes or reveals data calls
 * {@link #require(User, Permission)} first, so permissions are enforced in ONE layer
 * regardless of which menu (or future UI) calls the service.
 */
public final class AuthorizationService {

    private AuthorizationService() { }

    /** @throws UnauthorizedOperationException if nobody is logged in or the role lacks the permission */
    public static void require(User actor, Permission permission) {
        if (actor == null) {
            throw new UnauthorizedOperationException("You must be logged in to do this.");
        }
        if (!actor.hasPermission(permission)) {
            throw new UnauthorizedOperationException("Your account (" + actor.getRole().getLabel()
                    + ") is not allowed to perform this operation.");
        }
    }

    public static boolean isAllowed(User actor, Permission permission) {
        return actor != null && actor.hasPermission(permission);
    }
}
