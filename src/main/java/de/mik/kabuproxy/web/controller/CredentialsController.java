package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
import de.mik.kabuproxy.service.CredentialService;
import de.mik.kabuproxy.service.UserService;
import de.mik.kabuproxy.web.model.AccountView;
import lombok.Getter;
import lombok.Setter;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

/**
 * The user's own digikabu login on the settings page.
 */
@Named
@ViewScoped
public class CredentialsController implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Inject private transient AccountService accountService;
    @Inject private transient CredentialService credentialService;
    @Inject private transient UserService userService;
    @Inject private UserSession userSession;

    @Getter private AccountView account;
    @Getter @Setter private String digikabuUsername;
    @Getter @Setter private String digikabuPassword;

    @PostConstruct
    void init()
    {
        reload();
    }

    private void reload()
    {
        account = accountService.findByUser(userSession.getUserId()).orElse(null);
        digikabuUsername = account == null ? null : account.digikabuUsername();
        digikabuPassword = null;
    }

    public boolean isHasAccount()
    {
        return account != null;
    }

    public boolean isReady()
    {
        return credentialService.isReady();
    }

    public void save()
    {
        boolean active = userService.isActive(userSession.getUserId());
        String hint = active ? "creds.hintActive" : "creds.hintPending";
        if (Messages.linkResult(credentialService.linkOwn(userSession.getUserId(), digikabuUsername, digikabuPassword), hint))
        {
            reload();
        }
        else
        {
            digikabuPassword = null;
        }
    }
}
