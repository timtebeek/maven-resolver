/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.aether.internal.impl;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.RemoteRepositoryFilterManager;
import org.eclipse.aether.impl.RepositoryConnectorProvider;
import org.eclipse.aether.internal.impl.filter.FilteringRepositoryConnector;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilter;
import org.eclipse.aether.spi.locator.Service;
import org.eclipse.aether.spi.locator.ServiceLocator;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 */
@Singleton
@Named
public class DefaultRepositoryConnectorProvider implements RepositoryConnectorProvider, Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRepositoryConnectorProvider.class);

    private Collection<RepositoryConnectorFactory> connectorFactories = new ArrayList<>();

    private RemoteRepositoryFilterManager remoteRepositoryFilterManager;

    @Deprecated
    public DefaultRepositoryConnectorProvider() {
        // enables default constructor
    }

    @Inject
    public DefaultRepositoryConnectorProvider(
            Set<RepositoryConnectorFactory> connectorFactories,
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        setRepositoryConnectorFactories(connectorFactories);
        setRemoteRepositoryFilterManager(remoteRepositoryFilterManager);
    }

    public void initService(ServiceLocator locator) {
        setRepositoryConnectorFactories(locator.getServices(RepositoryConnectorFactory.class));
        setRemoteRepositoryFilterManager(locator.getService(RemoteRepositoryFilterManager.class));
    }

    public DefaultRepositoryConnectorProvider addRepositoryConnectorFactory(RepositoryConnectorFactory factory) {
        connectorFactories.add(requireNonNull(factory, "repository connector factory cannot be null"));
        return this;
    }

    public DefaultRepositoryConnectorProvider setRepositoryConnectorFactories(
            Collection<RepositoryConnectorFactory> factories) {
        if (factories == null) {
            this.connectorFactories = new ArrayList<>();
        } else {
            this.connectorFactories = factories;
        }
        return this;
    }

    public DefaultRepositoryConnectorProvider setRemoteRepositoryFilterManager(
            RemoteRepositoryFilterManager remoteRepositoryFilterManager) {
        this.remoteRepositoryFilterManager = requireNonNull(remoteRepositoryFilterManager);
        return this;
    }

    public RepositoryConnector newRepositoryConnector(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        requireNonNull(repository, "remote repository cannot be null");

        if (repository.isBlocked()) {
            if (repository.getMirroredRepositories().isEmpty()) {
                throw new NoRepositoryConnectorException(repository, "Blocked repository: " + repository);
            } else {
                throw new NoRepositoryConnectorException(
                        repository, "Blocked mirror for repositories: " + repository.getMirroredRepositories());
            }
        }

        RemoteRepositoryFilter filter = remoteRepositoryFilterManager.getRemoteRepositoryFilter(session);

        PrioritizedComponents<RepositoryConnectorFactory> factories = new PrioritizedComponents<>(session);
        for (RepositoryConnectorFactory factory : this.connectorFactories) {
            factories.add(factory, factory.getPriority());
        }

        List<NoRepositoryConnectorException> errors = new ArrayList<>();
        for (PrioritizedComponent<RepositoryConnectorFactory> factory : factories.getEnabled()) {
            try {
                RepositoryConnector connector = factory.getComponent().newInstance(session, repository);

                if (LOGGER.isDebugEnabled()) {
                    StringBuilder buffer = new StringBuilder(256);
                    buffer.append("Using connector ")
                            .append(connector.getClass().getSimpleName());
                    Utils.appendClassLoader(buffer, connector);
                    buffer.append(" with priority ").append(factory.getPriority());
                    buffer.append(" for ").append(repository.getUrl());

                    Authentication auth = repository.getAuthentication();
                    if (auth != null) {
                        buffer.append(" with ").append(auth);
                    }

                    Proxy proxy = repository.getProxy();
                    if (proxy != null) {
                        buffer.append(" via ")
                                .append(proxy.getHost())
                                .append(':')
                                .append(proxy.getPort());

                        auth = proxy.getAuthentication();
                        if (auth != null) {
                            buffer.append(" with ").append(auth);
                        }
                    }

                    LOGGER.debug(buffer.toString());
                }

                if (filter != null) {
                    return new FilteringRepositoryConnector(repository, connector, filter);
                } else {
                    return connector;
                }
            } catch (NoRepositoryConnectorException e) {
                // continue and try next factory
                errors.add(e);
            }
        }
        if (LOGGER.isDebugEnabled() && errors.size() > 1) {
            for (Exception e : errors) {
                LOGGER.debug("Could not obtain connector factory for {}", repository, e);
            }
        }

        StringBuilder buffer = new StringBuilder(256);
        if (factories.isEmpty()) {
            buffer.append("No connector factories available");
        } else {
            buffer.append("Cannot access ").append(repository.getUrl());
            buffer.append(" with type ").append(repository.getContentType());
            buffer.append(" using the available connector factories: ");
            factories.list(buffer);
        }

        throw new NoRepositoryConnectorException(
                repository, buffer.toString(), errors.size() == 1 ? errors.get(0) : null);
    }
}
