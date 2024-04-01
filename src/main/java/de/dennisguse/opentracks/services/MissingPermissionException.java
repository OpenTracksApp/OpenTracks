package de.dennisguse.opentracks.services;

import de.dennisguse.opentracks.util.PermissionRequester;

public class MissingPermissionException extends RuntimeException {

    private final PermissionRequester permissionRequester;

    public MissingPermissionException(PermissionRequester permissionRequester) {
        this.permissionRequester = permissionRequester;
    }
}
