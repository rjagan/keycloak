package org.keycloak.testsuite.rule;

import org.jboss.logging.Logger;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoggingRule implements TestRule {

    private final Logger log;

    public LoggingRule(Object test) {
        log = Logger.getLogger(test.getClass());
    }

    public Statement apply(Statement base, Description description) {
        return this.statement(base, description);
    }

    private Statement statement(final Statement base, final Description description) {
        return new Statement() {
            public void evaluate() throws Throwable {
                log.debugf("Before %s", description.getMethodName());

                try {
                    base.evaluate();
                } finally {
                    log.debugf("After %s", description.getMethodName());
                }

            }
        };
    }
}
