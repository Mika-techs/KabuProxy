package de.mik.kabuproxy.web.controller;

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

    private static void add(FacesMessage.Severity severity, String text)
    {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, text, null));
    }
}
