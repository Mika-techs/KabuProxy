package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.config.KabuConfig;
import de.mik.kabuproxy.crawler.CrawlService;
import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
import de.mik.kabuproxy.web.model.AccountView;
import de.mik.kabuproxy.web.model.Formats;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Header/footer state shared by all pages: who is logged in, how fresh the data is, problems with the account.
 */
@Named("status")
@RequestScoped
public class StatusController
{
    @Inject private KabuConfig config;
    @Inject private UserSession userSession;
    @Inject private AccountService accountService;
    @Inject private CrawlService crawlService;

    private AccountView account;

    @PostConstruct
    void init()
    {
        if (userSession.isLoggedIn())
        {
            account = accountService.findByUser(userSession.getUserId()).orElse(null);
        }
    }

    public AccountView getAccount()
    {
        return account;
    }

    public boolean isHasAccount()
    {
        return account != null;
    }

    public boolean isHasClass()
    {
        return account != null && account.classId() != null;
    }

    public String getClassName()
    {
        return account == null ? null : account.className();
    }

    public String getDisplayName()
    {
        if (account != null && account.displayName() != null)
        {
            return account.displayName();
        }
        return userSession.getUsername();
    }

    public boolean isAdmin()
    {
        return userSession.isAdmin();
    }

    public boolean isDevAuth()
    {
        return config.isDevAuth();
    }

    public String getFreshness()
    {
        if (account == null)
        {
            return null;
        }
        return "Stand " + Formats.relative(account.timetableUpdatedAt() != null ? account.timetableUpdatedAt() : account.lastSuccessAt());
    }

    /**
     * Warning shown on every page while the account has a problem; null when everything is fine.
     */
    public String getProblem()
    {
        if (account == null || account.crawlStatus() == CrawlStatus.OK)
        {
            return null;
        }
        return switch (account.crawlStatus())
        {
            case NEVER -> "Daten werden gerade zum ersten Mal von digikabu geholt …";
            case AUTH_FAILED -> "digikabu hat dein Passwort abgelehnt. Der Abruf ist pausiert, bis du es unter Einstellungen aktualisierst.";
            case UNAVAILABLE -> "digikabu ist gerade nicht erreichbar – angezeigt werden die zuletzt geladenen Daten.";
            case PARSE_ERROR -> "digikabu hat etwas Unerwartetes geliefert – angezeigt werden die zuletzt geladenen Daten.";
            case OK -> null;
        };
    }

    public String refresh()
    {
        switch (crawlService.requestRefresh(userSession.getUserId()))
        {
            case STARTED -> Messages.info("Aktualisierung gestartet – lade die Seite in ein paar Sekunden neu.");
            case COOLDOWN -> Messages.warn("Gerade erst aktualisiert. Nächster Versuch in ein paar Minuten möglich.");
            case BUSY -> Messages.info("Es läuft bereits ein Abruf – gleich sind neue Daten da.");
            case NO_ACCOUNT -> Messages.warn("Kein digikabu-Konto verknüpft.");
            default -> Messages.warn("Unbekannter Zustand.");
        }
        return null;
    }
}
