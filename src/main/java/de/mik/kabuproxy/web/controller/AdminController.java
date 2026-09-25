package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.crawler.CrawlService;
import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.persistence.entities.UserStatus;
import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
import de.mik.kabuproxy.service.CredentialService;
import de.mik.kabuproxy.web.I18n;
import de.mik.kabuproxy.web.model.AdminUserView;
import de.mik.kabuproxy.web.model.Formats;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * User management. The admin sees crawl status but never a user's absences or password.
 */
@Named
@ViewScoped
public class AdminController implements Serializable
{
    private static final long serialVersionUID = 1L;
    /** written by the Dockerfile at image build time (UTC, ISO-8601); missing in dev runs */
    private static final String BUILD_TIME_RESOURCE = "/WEB-INF/build-time";

    @Inject private transient Logger logger;
    @Inject private transient AccountService accountService;
    @Inject private transient CrawlService crawlService;
    @Inject private transient CredentialService credentialService;
    @Inject private UserSession userSession;

    @Getter private List<AdminUserView> users;
    @Getter private String buildTimeLabel;

    @Getter @Setter private Long selectedUserId;
    @Getter @Setter private String selectedUserLabel;
    @Getter @Setter private String digikabuUsername;
    @Getter @Setter private String digikabuPassword;

    @PostConstruct
    void init()
    {
        buildTimeLabel = readBuildTime();
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
            Messages.error("admin.noSelection");
            return;
        }
        logger.info("admin links digikabu credentials for user {}", selectedUserId);
        if (Messages.linkResult(credentialService.linkByAdmin(selectedUserId, digikabuUsername, digikabuPassword), "admin.firstCrawl"))
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
            Messages.warn("admin.selfDisable");
            return;
        }
        accountService.setUserStatus(user.userId(), UserStatus.DISABLED);
        reload();
    }

    public void delete(AdminUserView user)
    {
        if (user.userId() == userSession.getUserId())
        {
            Messages.warn("admin.selfDelete");
            return;
        }
        accountService.deleteUser(user.userId());
        Messages.info("admin.deleted", user.username());
        reload();
    }

    public void crawlNow(AdminUserView user)
    {
        if (user.account() != null)
        {
            crawlService.submit(user.account().accountId());
            Messages.info("admin.crawlStarted", user.username());
        }
    }

    public void reload()
    {
        users = accountService.listUsers();
    }

    private String readBuildTime()
    {
        try (InputStream in = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(BUILD_TIME_RESOURCE))
        {
            if (in == null)
            {
                return I18n.text("admin.buildUnknown");
            }
            return Formats.relative(Instant.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8).trim()));
        }
        catch (IOException | DateTimeParseException e)
        {
            logger.warn("cannot read {}: {}", BUILD_TIME_RESOURCE, e.getMessage());
            return I18n.text("admin.buildUnknown");
        }
    }
}
