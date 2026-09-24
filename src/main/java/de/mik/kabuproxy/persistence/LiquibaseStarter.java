package de.mik.kabuproxy.persistence;

import javax.sql.DataSource;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.apache.logging.log4j.Logger;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class LiquibaseStarter
{
    @Inject private Logger logger;

    @Resource(name = "KabuDataSource")
    private DataSource dataSource;

    public void migrate() throws LiquibaseException, SQLException
    {
        try (Connection connection = dataSource.getConnection())
        {
            logger.info("migrating {}@{} ({})", connection.getMetaData().getUserName(), connection.getCatalog(), connection.getMetaData().getURL());
            Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase("liquibase/changelog.xml", new ClassLoaderResourceAccessor(), database))
            {
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }
}
