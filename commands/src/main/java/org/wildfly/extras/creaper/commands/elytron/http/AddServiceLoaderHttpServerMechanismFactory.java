package org.wildfly.extras.creaper.commands.elytron.http;

import org.wildfly.extras.creaper.core.ServerVersion;
import org.wildfly.extras.creaper.core.online.OnlineCommand;
import org.wildfly.extras.creaper.core.online.OnlineCommandContext;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

public final class AddServiceLoaderHttpServerMechanismFactory implements OnlineCommand {

    private final String name;
    private final String module;
    private final boolean replaceExisting;

    private AddServiceLoaderHttpServerMechanismFactory(Builder builder) {
        this.name = builder.name;
        this.module = builder.module;
        this.replaceExisting = builder.replaceExisting;
    }

    @Override
    public void apply(OnlineCommandContext ctx) throws Exception {
        if (ctx.version.lessThan(ServerVersion.VERSION_5_0_0)) {
            throw new AssertionError("Elytron is available since WildFly 11.");
        }

        Operations ops = new Operations(ctx.client);
        Address factoryAddress = Address.subsystem("elytron")
                .and("service-loader-http-server-mechanism-factory", name);
        if (replaceExisting) {
            ops.removeIfExists(factoryAddress);
            new Administration(ctx.client).reloadIfRequired();
        }

        ops.add(factoryAddress, Values.empty()
                .andOptional("module", module));
    }

    public static final class Builder {

        private final String name;
        private String module;
        private boolean replaceExisting;

        public Builder(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name of the service-loader-http-server-mechanism-factory must be specified as non null value");
            }
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name of the service-loader-http-server-mechanism-factory must not be empty value");
            }
            this.name = name;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder replaceExisting() {
            this.replaceExisting = true;
            return this;
        }

        public AddServiceLoaderHttpServerMechanismFactory build() {
            return new AddServiceLoaderHttpServerMechanismFactory(this);
        }

    }

}
