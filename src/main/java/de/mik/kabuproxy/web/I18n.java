package de.mik.kabuproxy.web;

import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * UI texts from {@code i18n/messages*.properties} in the locale JSF picked from the browser's Accept-Language (see
 * faces-config.xml); German outside a Faces request. The views use the same bundle as {@code #{msg[...]}}.
 */
public final class I18n
{
    public static final String BUNDLE = "i18n.messages";

    private static final Locale FALLBACK = Locale.GERMAN;
    /**
     * Never fall back to the JVM's default locale – the root bundle (German) is the fallback.
     */
    private static final ResourceBundle.Control NO_FALLBACK = ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    private I18n()
    {
    }

    public static Locale locale()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null)
        {
            return FALLBACK;
        }
        UIViewRoot view = context.getViewRoot();
        if (view != null && view.getLocale() != null)
        {
            return view.getLocale();
        }
        return context.getApplication().getViewHandler().calculateLocale(context);
    }

    /**
     * The text for {@code key}; with arguments it is a {@link MessageFormat} pattern (so {@code '} must be doubled there).
     */
    public static String text(String key, Object... args)
    {
        Locale locale = locale();
        String pattern;
        try
        {
            pattern = ResourceBundle.getBundle(BUNDLE, locale, I18n.class.getClassLoader(), NO_FALLBACK).getString(key);
        }
        catch (MissingResourceException e)
        {
            return "???" + key + "???";
        }
        return args.length == 0 ? pattern : new MessageFormat(pattern, locale).format(args);
    }
}
