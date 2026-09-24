package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.SettingsService;
import de.mik.kabuproxy.web.model.UserSettings;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * The logged-in user's settings for the layout (theme attribute, accent colour); rendered server-side so there is no flash.
 */
@Named("theme")
@RequestScoped
public class ThemeController
{
    @Inject private UserSession userSession;
    @Inject private SettingsService settingsService;

    private UserSettings settings = UserSettings.DEFAULT;

    @PostConstruct
    void init()
    {
        if (userSession.isLoggedIn())
        {
            settings = settingsService.load(userSession.getUserId());
        }
    }

    public UserSettings getSettings()
    {
        return settings;
    }
}
