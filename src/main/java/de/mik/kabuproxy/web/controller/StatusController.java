package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.config.KabuConfig;
import de.mik.kabuproxy.crawler.CrawlService;
import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
import de.mik.kabuproxy.web.I18n;
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
        return I18n.text("layout.freshness", Formats.relative(account.timetableUpdatedAt() != null ? account.timetableUpdatedAt() : account.lastSuccessAt()));
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
        return I18n.text("problem." + account.crawlStatus().name());
    }

    public String refresh()
    {
        switch (crawlService.requestRefresh(userSession.getUserId()))
        {
            case STARTED -> Messages.info("refresh.started");
            case COOLDOWN -> Messages.warn("refresh.cooldown");
            case BUSY -> Messages.info("refresh.busy");
            case NO_ACCOUNT -> Messages.warn("refresh.noAccount");
            default -> Messages.warn("unknownState");
        }
        return null;
    }
}
