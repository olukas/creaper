package org.wildfly.extras.creaper.core.online;

import org.jboss.as.controller.client.Operation;
import org.jboss.dmr.ModelNode;
import org.wildfly.extras.creaper.core.CommandFailedException;
import org.wildfly.extras.creaper.core.ServerVersion;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

final class AutomaticErrorHandlingForCommands implements OnlineManagementClient {
    private final OnlineManagementClient delegate;
    private boolean throwExceptionsOnOperationFailures = true;

    static OnlineManagementClient wrap(OnlineManagementClient client) {
        if (client instanceof AutomaticErrorHandlingForCommands) {
            return client;
        }

        return new AutomaticErrorHandlingForCommands(client);
    }

    private AutomaticErrorHandlingForCommands(OnlineManagementClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public OnlineOptions options() {
        return delegate.options();
    }

    @Override
    public ServerVersion version() throws IOException {
        return delegate.version();
    }

    @Override
    public void apply(OnlineCommand... commands) throws CommandFailedException {
        try {
            delegate.apply(commands);
        } catch (RuntimeCommandFailedException e) {
            throw e.original;
        }
    }

    @Override
    public void apply(Iterable<OnlineCommand> commands) throws CommandFailedException {
        try {
            delegate.apply(commands);
        } catch (RuntimeCommandFailedException e) {
            throw e.original;
        }
    }

    @Override
    public ModelNodeResult execute(ModelNode operation) {
        try {
            ModelNodeResult result = delegate.execute(operation);
            if (result.isFailed() && throwExceptionsOnOperationFailures) {
                commandFailedWithMessage("Operation " + operation.asString() + " failed: " + result.asString());
            }
            return result;
        } catch (RuntimeCommandFailedException e) {
            throw e;
        } catch (Exception e) {
            commandFailedWithCause(e);
            return null; // dead code
        }
    }

    @Override
    public ModelNodeResult execute(Operation operation) {
        try {
            ModelNodeResult result = delegate.execute(operation);
            if (result.isFailed() && throwExceptionsOnOperationFailures) {
                commandFailedWithMessage("Operation " + operation.getOperation().asString() + " failed: "
                        + result.asString());
            }
            return result;
        } catch (RuntimeCommandFailedException e) {
            throw e;
        } catch (Exception e) {
            commandFailedWithCause(e);
            return null; // dead code
        }
    }

    @Override
    public ModelNodeResult execute(String operation) {
        try {
            ModelNodeResult result = delegate.execute(operation);
            if (result.isFailed() && throwExceptionsOnOperationFailures) {
                commandFailedWithMessage("Operation " + operation + " failed: " + result.asString());
            }
            return result;
        } catch (RuntimeCommandFailedException e) {
            throw e;
        } catch (Exception e) {
            commandFailedWithCause(e);
            return null; // dead code
        }
    }

    @Override
    public void executeCli(String cliOperation) {
        try {
            delegate.executeCli(cliOperation);
        } catch (Exception e) {
            commandFailedWithCause(e);
        }
    }

    @Override
    public void reconnect(int timeoutInSeconds) throws TimeoutException, InterruptedException {
        delegate.reconnect(timeoutInSeconds);
    }

    @Override
    public FailuresAllowedBlock allowFailures() throws IOException {
        final boolean old = this.throwExceptionsOnOperationFailures;

        this.throwExceptionsOnOperationFailures = false;

        return new FailuresAllowedBlock() {
            @Override
            public void close() throws IOException {
                AutomaticErrorHandlingForCommands.this.throwExceptionsOnOperationFailures = old;
            }
        };
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private static void commandFailedWithMessage(String message) {
        throw new RuntimeCommandFailedException(new CommandFailedException(message));
    }

    private static void commandFailedWithCause(Throwable cause) {
        throw new RuntimeCommandFailedException(new CommandFailedException(cause));
    }
}
