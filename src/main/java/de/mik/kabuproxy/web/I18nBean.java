package de.mik.kabuproxy.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * Parameterized texts for the views, e.g. {@code #{i18n.format('admin.deleteConfirm', u.username)}}; plain texts use {@code #{msg[...]}}.
 */
@Named("i18n")
@ApplicationScoped
public class I18nBean
{
    public String format(String key, Object arg)
    {
        return I18n.text(key, arg);
    }

    public String format(String key, Object arg1, Object arg2)
    {
        return I18n.text(key, arg1, arg2);
    }
}
