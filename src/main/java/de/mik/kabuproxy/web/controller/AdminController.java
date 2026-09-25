package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.crawler.CrawlService;
import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.persistence.entities.UserStatus;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
import de.mik.kabuproxy.service.CredentialService;
import de.mik.kabuproxy.web.model.AdminUserView;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * User management. The admin sees crawl status but never a user's absences or password.
 */
@Named
@ViewScoped
public class AdminController implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Inject private transient Logger logger;
    @Inject private transient AccountService accountService;
    @Inject private transient CrawlService crawlService;
    @Inject private transient CredentialService credentialService;
    @Inject private UserSession userSession;

    @Getter private List<AdminUserView> users;

    @Getter @Setter private Long selectedUserId;
    @Getter @Setter private String selectedUserLabel;
    @Getter @Setter private String digikabuUsername;
    @Getter @Setter private String digikabuPassword;

    @PostConstruct
    void init()
    {
        reload();
    }

    public boolean isCipherReady()
    {
        return credentialService.isReady();
    }

    public void select(AdminUserView user)
    {
        selectedUserId = user.userId();
        selectedUserLabel = user.username();
        digikabuUsername = user.account() == null ? null : user.account().digikabuUsername();
        digikabuPassword = null;
    }

    public void saveCredentials()
    {
        if (selectedUserId == null)
        {
            Messages.error("Kein Benutzer ausgewählt.");
            return;
        }
        logger.info("admin links digikabu credentials for user {}", selectedUserId);
        if (Messages.linkResult(credentialService.linkByAdmin(selectedUserId, digikabuUsername, digikabuPassword), "Erster Abruf läuft."))
        {
            digikabuPassword = null;
            reload();
        }
    }

    public void activate(AdminUserView user)
    {
        accountService.setUserStatus(user.userId(), UserStatus.ACTIVE);
        if (user.account() != null && user.account().crawlStatus() == CrawlStatus.NEVER)
        {
            // the user linked their own account while pending, nothing was crawled yet
            crawlService.submit(user.account().accountId());
        }
        reload();
    }

    public void disable(AdminUserView user)
    {
        if (user.userId() == userSession.getUserId())
        {
            Messages.warn("Du kannst dich nicht selbst sperren.");
            return;
        }
        accountService.setUserStatus(user.userId(), UserStatus.DISABLED);
        reload();
    }

    public void delete(AdminUserView user)
    {
        if (user.userId() == userSession.getUserId())
        {
            Messages.warn("Du kannst dich nicht selbst löschen.");
            return;
        }
        accountService.deleteUser(user.userId());
        Messages.info("Benutzer " + user.username() + " und alle Daten gelöscht.");
        reload();
    }

    public void crawlNow(AdminUserView user)
    {
        if (user.account() != null)
        {
            crawlService.submit(user.account().accountId());
            Messages.info("Abruf für " + user.username() + " gestartet.");
        }
    }

    public void reload()
    {
        users = accountService.listUsers();
    }
}
