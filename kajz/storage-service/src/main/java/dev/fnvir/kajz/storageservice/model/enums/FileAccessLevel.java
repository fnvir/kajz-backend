package dev.fnvir.kajz.storageservice.model.enums;

public enum FileAccessLevel {
    /** Anyone can access. */
    PUBLIC,
    
    /** Requires authentication + ownership. */
    PRIVATE,
    
    /** Requires authentication (Only logged-in user can view). */
    PROTECTED
}
