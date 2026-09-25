package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.service.CredentialService.LinkResult;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

final class Messages
{
    private Messages()
    {
    }

    static void info(String text)
    {
        add(FacesMessage.SEVERITY_INFO, text);
    }

    static void warn(String text)
    {
        add(FacesMessage.SEVERITY_WARN, text);
    }

    static void error(String text)
    {
        add(FacesMessage.SEVERITY_ERROR, text);
    }

    /**
     * Reports the outcome of linking digikabu credentials.
     *
     * @param savedHint appended to the success message
     * @return true when the credentials were saved
     */
    static boolean linkResult(LinkResult result, String savedHint)
    {
        switch (result.outcome())
        {
            case SAVED -> info("Gespeichert: " + result.header().displayName() + " (" + result.header().className() + "). " + savedHint);
            case INCOMPLETE -> error("Benutzername und Passwort angeben.");
            case NOT_CONFIGURED -> error("Zugangsdaten können gerade nicht gespeichert werden (KABU_CRED_KEY fehlt).");
            case RATE_LIMITED -> error("Zu viele Versuche – bitte in ein paar Minuten erneut probieren.");
            case REJECTED -> error("digikabu hat die Zugangsdaten abgelehnt – nichts gespeichert.");
            case FAILED -> error("Test-Login fehlgeschlagen: " + result.error());
            default -> error("Unbekannter Zustand.");
        }
        return result.outcome() == LinkResult.Outcome.SAVED;
    }

    private static void add(FacesMessage.Severity severity, String text)
    {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, text, null));
    }
}
