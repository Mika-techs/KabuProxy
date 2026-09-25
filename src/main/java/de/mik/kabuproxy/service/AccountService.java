package de.mik.kabuproxy.service;

import de.mik.kabuproxy.crypto.CredentialCipher;
import de.mik.kabuproxy.digikabu.parser.ParsedHeader;
import de.mik.kabuproxy.persistence.entities.AbsenceEntity;
import de.mik.kabuproxy.persistence.entities.AppUserEntity;
import de.mik.kabuproxy.persistence.entities.CrawlStatus;
import de.mik.kabuproxy.persistence.entities.DigikabuAccountEntity;
import de.mik.kabuproxy.persistence.entities.SchoolClassEntity;
import de.mik.kabuproxy.persistence.entities.UserStatus;
import de.mik.kabuproxy.persistence.repository.AbsenceRepository;
import de.mik.kabuproxy.persistence.repository.AccountRepository;
import de.mik.kabuproxy.persistence.repository.SchoolClassRepository;
import de.mik.kabuproxy.persistence.repository.UserRepository;
import de.mik.kabuproxy.web.model.AbsenceView;
import de.mik.kabuproxy.web.model.AccountView;
import de.mik.kabuproxy.web.model.AdminUserView;
import de.mik.kabuproxy.web.model.Formats;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class AccountService
{
    @Inject private Logger logger;
    @Inject private CredentialCipher cipher;
    @Inject private UserRepository userRepository;
    @Inject private AccountRepository accountRepository;
    @Inject private SchoolClassRepository schoolClassRepository;
    @Inject private AbsenceRepository absenceRepository;

    @Transactional
    public Optional<AccountView> findByUser(long userId)
    {
        return accountRepository.findByUserId(userId).map(AccountService::toView);
    }

    @Transactional
    public List<AdminUserView> listUsers()
    {
        Map<Long, DigikabuAccountEntity> accounts = accountRepository.findAllWithUser().stream()
            .collect(Collectors.toMap(a -> a.getUser().getId(), Function.identity()));
        return userRepository.findAll().stream()
            .map(u -> new AdminUserView(u.getId(), u.getUsername(), u.getEmail(), u.getStatus(), u.getLastLoginAt(),
                accounts.containsKey(u.getId()) ? toView(accounts.get(u.getId())) : null))
            .toList();
    }

    /**
     * Stores (verified) credentials for a user. Resets any previous failure state.
     *
     * @param activate also activate a pending user (admin linking)
     * @return account id
     */
    @Transactional
    public long saveCredentials(long userId, String digikabuUsername, String password, ParsedHeader header, boolean activate)
    {
        AppUserEntity user = userRepository.findById(userId).orElseThrow();
        DigikabuAccountEntity account = accountRepository.findByUserId(userId).orElse(null);
        if (account == null)
        {
            account = new DigikabuAccountEntity();
            account.setUser(user);
        }
        account.setDigikabuUsername(digikabuUsername);
        account.setPasswordEnc(cipher.encrypt(password, digikabuUsername));
        account.setCrawlStatus(CrawlStatus.NEVER);
        account.setFailCount(0);
        account.setLastError(null);
        account.setNextAttemptAt(null);
        if (header != null)
        {
            account.setDisplayName(header.displayName());
            account.setSchoolClass(schoolClassRepository.findOrCreate(header.className()));
        }
        if (account.getId() == null)
        {
            accountRepository.persist(account);
        }
        if (activate && user.getStatus() == UserStatus.PENDING)
        {
            user.setStatus(UserStatus.ACTIVE);
        }
        logger.info("digikabu credentials saved for user {} ({})", userId, header == null ? "?" : header.className());
        return account.getId();
    }

    @Transactional
    public void setUserStatus(long userId, UserStatus status)
    {
        userRepository.findById(userId).ifPresent(u -> u.setStatus(status));
    }

    /**
     * Deletes the user; the DB cascades to account, absences and debug rows.
     */
    @Transactional
    public void deleteUser(long userId)
    {
        userRepository.findById(userId).ifPresent(userRepository::delete);
        logger.info("user {} deleted", userId);
    }

    @Transactional
    public List<AbsenceView> absences(long userId)
    {
        return accountRepository.findByUserId(userId)
            .map(a -> absenceRepository.findByAccount(a.getId()).stream().map(AccountService::toView).toList())
            .orElse(List.of());
    }

    private static AbsenceView toView(AbsenceEntity absence)
    {
        return new AbsenceView(Formats.date(absence.getDate()), Formats.weekdayShort(absence.getDate()), absence.getFromText(), absence.getToText(),
            absence.getRemark(), absence.getKind(), absence.getExcused());
    }

    private static AccountView toView(DigikabuAccountEntity account)
    {
        SchoolClassEntity schoolClass = account.getSchoolClass();
        return new AccountView(
            account.getId(),
            schoolClass == null ? null : schoolClass.getId(),
            schoolClass == null ? null : schoolClass.getName(),
            account.getDisplayName(),
            account.getDigikabuUsername(),
            account.getCrawlStatus(),
            account.getLastError(),
            account.getLastAttemptAt(),
            account.getLastSuccessAt(),
            schoolClass == null ? null : schoolClass.getTimetableUpdatedAt(),
            schoolClass == null ? null : schoolClass.getCalendarUpdatedAt(),
            account.getAbsenceFullDays(),
            account.getAbsenceFullDaysUnexcused(),
            account.getAbsenceHours(),
            account.getAbsenceHoursUnexcused(),
            account.getAbsencesUpdatedAt());
    }
}
