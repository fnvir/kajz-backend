package dev.fnvir.kajz.storageservice.enums;

/**
 * Enum representing different levels of file access.
 */
public enum FileAccessLevel {
    /** Anyone can access. */
    PUBLIC,
    
    /** Requires authentication + ownership. */
    PRIVATE,
    
    /** Requires authentication (Only logged-in user can view). */
    PROTECTED
}
