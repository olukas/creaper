package org.wildfly.extras.creaper.commands.elytron.sasl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.dmr.ModelNode;
import org.wildfly.extras.creaper.commands.elytron.Mechanism;
import org.wildfly.extras.creaper.core.ServerVersion;
import org.wildfly.extras.creaper.core.online.OnlineCommand;
import org.wildfly.extras.creaper.core.online.OnlineCommandContext;
import org.wildfly.extras.creaper.core.online.operations.Address;
import org.wildfly.extras.creaper.core.online.operations.Operations;
import org.wildfly.extras.creaper.core.online.operations.Values;
import org.wildfly.extras.creaper.core.online.operations.admin.Administration;

public final class AddSaslAuthenticationFactory implements OnlineCommand {

    private final String name;
    private final String securityDomain;
    private final String saslServerFactory;
    private final List<Mechanism> mechanismConfigurations;
    private final boolean replaceExisting;

    private AddSaslAuthenticationFactory(Builder builder) {
        this.name = builder.name;
        this.securityDomain = builder.securityDomain;
        this.saslServerFactory = builder.saslServerFactory;
        this.mechanismConfigurations = builder.mechanismConfigurations;
        this.replaceExisting = builder.replaceExisting;
    }

    @Override
    public void apply(OnlineCommandContext ctx) throws Exception {
        if (ctx.version.lessThan(ServerVersion.VERSION_5_0_0)) {
            throw new AssertionError("Elytron is available since WildFly 11.");
        }

        Operations ops = new Operations(ctx.client);
        Address factoryAddress = Address.subsystem("elytron")
                .and("sasl-authentication-factory", name);
        if (replaceExisting) {
            ops.removeIfExists(factoryAddress);
            new Administration(ctx.client).reloadIfRequired();
        }

        List<ModelNode> mechanismConfigurationsNodeList = null;
        if (mechanismConfigurations != null && !mechanismConfigurations.isEmpty()) {
            mechanismConfigurationsNodeList = new ArrayList<ModelNode>();
            for (Mechanism mechanismConfiguration : mechanismConfigurations) {
                ModelNode mechanismNode = new ModelNode();
                addOptionalToModelNode(mechanismNode, "mechanism-name", mechanismConfiguration.getMechanismName());
                addOptionalToModelNode(mechanismNode, "host-name", mechanismConfiguration.getHostName());
                addOptionalToModelNode(mechanismNode, "protocol", mechanismConfiguration.getProtocol());
                addOptionalToModelNode(mechanismNode, "pre-realm-principal-transformer",
                        mechanismConfiguration.getPreRealmPrincipalTransformer());
                addOptionalToModelNode(mechanismNode, "post-realm-principal-transformer",
                        mechanismConfiguration.getPostRealmPrincipalTransformer());
                addOptionalToModelNode(mechanismNode, "final-principal-transformer",
                        mechanismConfiguration.getFinalPrincipalTransformer());
                addOptionalToModelNode(mechanismNode, "realm-mapper", mechanismConfiguration.getRealmMapper());
                addOptionalToModelNode(mechanismNode, "credential-security-factory",
                        mechanismConfiguration.getCredentialSecurityFactory());

                List<ModelNode> mechanismRealmConfigurationsNodeList = null;
                if (mechanismConfiguration.getMechanismRealmConfigurations() != null
                        && !mechanismConfiguration.getMechanismRealmConfigurations().isEmpty()) {

                    mechanismRealmConfigurationsNodeList = new ArrayList<ModelNode>();
                    for (Mechanism.MechanismRealm mechanismRealm
                            : mechanismConfiguration.getMechanismRealmConfigurations()) {
                        ModelNode mechanismRealmNode = new ModelNode();
                        mechanismRealmNode.add("realm-name", mechanismRealm.getRealmName());
                        addOptionalToModelNode(mechanismRealmNode, "pre-realm-principal-transformer",
                                mechanismRealm.getPreRealmPrincipalTransformer());
                        addOptionalToModelNode(mechanismRealmNode, "post-realm-principal-transformer",
                                mechanismRealm.getPostRealmPrincipalTransformer());
                        addOptionalToModelNode(mechanismRealmNode, "final-principal-transformer",
                                mechanismRealm.getFinalPrincipalTransformer());
                        addOptionalToModelNode(mechanismRealmNode, "realm-mapper", mechanismRealm.getRealmMapper());
                        mechanismRealmNode = mechanismRealmNode.asObject();
                        mechanismRealmConfigurationsNodeList.add(mechanismRealmNode);
                    }
                    ModelNode mechanismRealmConfigurations = new ModelNode();
                    mechanismRealmConfigurations.set(mechanismRealmConfigurationsNodeList);
                    mechanismNode.add("mechanism-realm-configurations", mechanismRealmConfigurations);
                }

                mechanismNode = mechanismNode.asObject();
                mechanismConfigurationsNodeList.add(mechanismNode);
            }
        }

        ops.add(factoryAddress, Values.empty()
                .and("security-domain", securityDomain)
                .and("sasl-server-factory", saslServerFactory)
                .andListOptional(ModelNode.class, "mechanism-configurations", mechanismConfigurationsNodeList));

    }

    private void addOptionalToModelNode(ModelNode node, String name, String value) {
        if (value != null && !value.isEmpty()) {
            node.add(name, value);
        }
    }

    public static final class Builder {

        private final String name;
        private String securityDomain;
        private String saslServerFactory;
        private List<Mechanism> mechanismConfigurations = new ArrayList<Mechanism>();
        private boolean replaceExisting;

        public Builder(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Name of the sasl-authentication-factory must be specified as non null value");
            }
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name of the sasl-authentication-factory must not be empty value");
            }
            this.name = name;
        }

        public Builder securityDomain(String securityDomain) {
            this.securityDomain = securityDomain;
            return this;
        }

        public Builder saslServerFactory(String saslServerFactory) {
            this.saslServerFactory = saslServerFactory;
            return this;
        }

        public Builder addMechanismConfigurations(Mechanism... mechanismConfigurations) {
            if (mechanismConfigurations == null) {
                throw new IllegalArgumentException("Mechanism added to mechanism-configuration must not be null");
            }
            Collections.addAll(this.mechanismConfigurations, mechanismConfigurations);
            return this;
        }

        public Builder replaceExisting() {
            this.replaceExisting = true;
            return this;
        }

        public AddSaslAuthenticationFactory build() {
            if (securityDomain == null || securityDomain.isEmpty()) {
                throw new IllegalArgumentException("security-domain must not be null and must have a minimum length of 1 characters");
            }
            if (saslServerFactory == null || saslServerFactory.isEmpty()) {
                throw new IllegalArgumentException("sasl-server-factory must not be null and must have a minimum length of 1 characters");
            }
            return new AddSaslAuthenticationFactory(this);
        }
    }

}
