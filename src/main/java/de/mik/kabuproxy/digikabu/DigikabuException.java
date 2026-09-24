package de.mik.kabuproxy.digikabu;

import lombok.Getter;

/**
 * Base of all crawl failures. The subclass decides how the crawler reacts (pause, back off, store debug html).
 */
@Getter
public class DigikabuException extends Exception
{
    private static final long serialVersionUID = 1L;

    private final String url;
    private final String html;

    public DigikabuException(String message, String url, String html, Throwable cause)
    {
        super(message, cause);
        this.url = url;
        this.html = html;
    }

    /**
     * digikabu rejected username/password. Retrying would risk an account lock.
     */
    public static class AuthFailed extends DigikabuException
    {
        private static final long serialVersionUID = 1L;

        public AuthFailed(String message)
        {
            super(message, null, null, null);
        }
    }

    /**
     * Network error, 5xx, or the digikabu session was dropped mid-crawl. Retried with back-off.
     */
    public static class Unavailable extends DigikabuException
    {
        private static final long serialVersionUID = 1L;

        public Unavailable(String message, String url, Throwable cause)
        {
            super(message, url, null, cause);
        }
    }

    /**
     * digikabu answered, but not in the expected shape (layout change). Raw html is kept for debugging.
     */
    public static class ParseFailed extends DigikabuException
    {
        private static final long serialVersionUID = 1L;

        public ParseFailed(String message, String url, String html)
        {
            super(message, url, html, null);
        }
    }
}
