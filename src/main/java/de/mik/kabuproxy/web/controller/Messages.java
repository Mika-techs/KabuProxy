package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.service.CredentialService.LinkResult;
import de.mik.kabuproxy.web.I18n;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

final class Messages
{
    private Messages()
    {
    }

    /**
     * @param key text in the {@link I18n} bundle
     */
    static void info(String key, Object... args)
    {
        add(FacesMessage.SEVERITY_INFO, I18n.text(key, args));
    }

    static void warn(String key, Object... args)
    {
        add(FacesMessage.SEVERITY_WARN, I18n.text(key, args));
    }

    static void error(String key, Object... args)
    {
        add(FacesMessage.SEVERITY_ERROR, I18n.text(key, args));
    }

    /**
     * Reports the outcome of linking digikabu credentials.
     *
     * @param savedHintKey bundle key of the text appended to the success message
     * @return true when the credentials were saved
     */
    static boolean linkResult(LinkResult result, String savedHintKey)
    {
        switch (result.outcome())
        {
            case SAVED -> info("creds.saved", result.header().displayName(), result.header().className(), I18n.text(savedHintKey));
            case INCOMPLETE -> error("creds.incomplete");
            case NOT_CONFIGURED -> error("creds.notConfigured");
            case RATE_LIMITED -> error("creds.rateLimited");
            case REJECTED -> error("creds.rejected");
            case FAILED -> error("creds.failed", result.error());
            default -> error("unknownState");
        }
        return result.outcome() == LinkResult.Outcome.SAVED;
    }

    private static void add(FacesMessage.Severity severity, String text)
    {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, text, null));
    }
}
