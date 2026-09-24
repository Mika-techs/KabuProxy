package de.mik.kabuproxy.web.controller;

import de.mik.kabuproxy.security.UserSession;
import de.mik.kabuproxy.service.AccountService;
import de.mik.kabuproxy.web.model.AbsenceView;
import lombok.Getter;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

/**
 * Only ever shows the logged-in user's own absences.
 */
@Named
@RequestScoped
public class AbsenceController
{
    @Inject private UserSession userSession;
    @Inject private AccountService accountService;

    @Getter private List<AbsenceView> absences = List.of();

    @PostConstruct
    void init()
    {
        if (userSession.isLoggedIn())
        {
            absences = accountService.absences(userSession.getUserId());
        }
    }
}
