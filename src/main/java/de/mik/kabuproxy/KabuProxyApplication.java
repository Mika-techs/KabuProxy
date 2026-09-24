package de.mik.kabuproxy;

import de.mik.kabuproxy.config.KabuConfig;
import de.mik.kabuproxy.crawler.CrawlScheduler;
import de.mik.kabuproxy.crypto.CredentialCipher;
import de.mik.kabuproxy.persistence.LiquibaseStarter;
import liquibase.exception.LiquibaseException;
import org.apache.logging.log4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.sql.SQLException;

@ApplicationScoped
public class KabuProxyApplication
{
    @Inject private Logger logger;
    @Inject private KabuConfig config;
    @Inject private LiquibaseStarter liquibaseStarter;
    @Inject private CredentialCipher credentialCipher;
    @Inject private CrawlScheduler crawlScheduler;

    private void init(@Observes @Initialized(ApplicationScoped.class) Object event) throws SQLException, LiquibaseException
    {
        liquibaseStarter.migrate();

        if (config.isDevAuth())
        {
            logger.warn("KABU_DEV_AUTH=true - authentication is DISABLED, every visitor is logged in as admin. Never use this in production!");
        }
        else if (config.getOidcProviderUri().isBlank() || config.getOidcClientId().isBlank())
        {
            logger.error("KABU_OIDC_PROVIDER_URI / KABU_OIDC_CLIENT_ID not set - nobody can log in. Configure Authentik (docs/AUTHENTIK.md)"
                + " or set KABU_DEV_AUTH=true for local development");
        }
        if (!credentialCipher.isConfigured())
        {
            logger.error("KABU_CRED_KEY is missing or invalid (needs 32 bytes base64) - digikabu accounts cannot be stored or crawled");
        }

        crawlScheduler.start();
    }
}
