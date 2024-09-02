package fr.yodamad.svn2git.web.rest.errors;

/**
 * Simple exception with a message, that returns an Internal Server Error code.
 */
public class InternalServerErrorException extends Throwable {

    private static final long serialVersionUID = 1L;

    public InternalServerErrorException(String message) {

    }
}
