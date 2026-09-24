package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.crawler.CrawlService;
import de.mik.kabuproxy.crypto.CredentialCipher;
import de.mik.kabuproxy.digikabu.DigikabuException;
import de.mik.kabuproxy.digikabu.parser.ParsedHeader;
import de.mik.kabuproxy.persistence.entities.UserStatus;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
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
    @Inject private transient CredentialCipher cipher;
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
        return cipher.isConfigured();
    }

    public void select(AdminUserView user)
    {
        selectedUserId = user.userId();
        selectedUserLabel = user.username();
        digikabuUsername = user.account() == null ? null : user.account().digikabuUsername();
        digikabuPassword = null;
    }

    /**
     * Verifies the credentials with one real login first, so typos never end up in the crawler.
     */
    public void saveCredentials()
    {
        logger.info("saving digikabu credentials for user {}", selectedUserId);
        if (selectedUserId == null || isBlank(digikabuUsername) || isBlank(digikabuPassword))
        {
            Messages.error("Benutzername und Passwort angeben.");
            return;
        }
        String username = digikabuUsername.trim();
        ParsedHeader header;
        try
        {
            header = crawlService.testLogin(username, digikabuPassword);
        }
        catch (DigikabuException.AuthFailed e)
        {
            Messages.error("digikabu hat die Zugangsdaten abgelehnt – nichts gespeichert.");
            return;
        }
        catch (DigikabuException e)
        {
            logger.warn("test login failed: {}", e.getMessage());
            Messages.error("Test-Login fehlgeschlagen: " + e.getMessage());
            return;
        }

        long accountId = accountService.saveCredentials(selectedUserId, username, digikabuPassword, header);
        digikabuPassword = null;
        crawlService.submit(accountId);
        Messages.info("Gespeichert: " + header.displayName() + " (" + header.className() + "). Erster Abruf läuft.");
        reload();
    }

    public void activate(AdminUserView user)
    {
        accountService.setUserStatus(user.userId(), UserStatus.ACTIVE);
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

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }
}
