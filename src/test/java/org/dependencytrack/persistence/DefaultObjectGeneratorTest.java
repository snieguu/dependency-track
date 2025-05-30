/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright (c) OWASP Foundation. All Rights Reserved.
 */
package org.dependencytrack.persistence;

import org.dependencytrack.PersistenceCapableTest;
import org.dependencytrack.auth.Permissions;
import org.dependencytrack.model.ConfigPropertyConstants;
import org.dependencytrack.model.License;
import org.dependencytrack.notification.publisher.DefaultNotificationPublishers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultObjectGeneratorTest extends PersistenceCapableTest {

    @Test
    void testContextInitialized() throws Exception {
        testLoadDefaultPermissions();
        testLoadDefaultPersonas();
        testLoadDefaultLicenses();
        testLoadDefaultRepositories();
        testLoadDefaultConfigProperties();
        testLoadDefaultNotificationPublishers();
    }

    @Test
    void testLoadDefaultLicenses() throws Exception {
        DefaultObjectGenerator generator = new DefaultObjectGenerator();
        Method method = generator.getClass().getDeclaredMethod("loadDefaultLicenses");
        method.setAccessible(true);
        method.invoke(generator);
        Assertions.assertEquals(757, qm.getAllLicensesConcise().size());
    }

    @Test
    void testLoadDefaultLicensesUpdatesExistingLicenses() throws Exception {
        final var license = new License();
        license.setLicenseId("LGPL-2.1+");
        license.setName("name");
        license.setComment("comment");
        license.setHeader("header");
        license.setSeeAlso("seeAlso");
        license.setTemplate("template");
        license.setText("text");
        qm.persist(license);

        final var generator = new DefaultObjectGenerator();
        final Method method = generator.getClass().getDeclaredMethod("loadDefaultLicenses");
        method.setAccessible(true);
        method.invoke(generator);

        qm.getPersistenceManager().evictAll();
        assertThat(license.getLicenseId()).isEqualTo("LGPL-2.1+");
        assertThat(license.getName()).isEqualTo("GNU Lesser General Public License v2.1 or later");
        assertThat(license.getComment()).isNotEqualTo("comment");
        assertThat(license.getHeader()).isNotEqualTo("header");
        assertThat(license.getSeeAlso()).isNotEqualTo(new String[]{"seeAlso"});
        assertThat(license.getTemplate()).isNotEqualTo("template");
        assertThat(license.getText()).isNotEqualTo("text");
    }

    @Test
    void testLoadDefaultPermissions() throws Exception {
        DefaultObjectGenerator generator = new DefaultObjectGenerator();
        Method method = generator.getClass().getDeclaredMethod("loadDefaultPermissions");
        method.setAccessible(true);
        method.invoke(generator);
        Assertions.assertEquals(Permissions.values().length, qm.getPermissions().size());
    }

    @Test
    void testLoadDefaultPersonas() throws Exception {
        DefaultObjectGenerator generator = new DefaultObjectGenerator();
        Method method = generator.getClass().getDeclaredMethod("loadDefaultPersonas");
        method.setAccessible(true);
        method.invoke(generator);
        Assertions.assertEquals(4, qm.getTeams().size());
    }

    @Test
    void testLoadDefaultRepositories() throws Exception {
        DefaultObjectGenerator generator = new DefaultObjectGenerator();
        Method method = generator.getClass().getDeclaredMethod("loadDefaultRepositories");
        method.setAccessible(true);
        method.invoke(generator);
        Assertions.assertEquals(17, qm.getAllRepositories().size());
    }

    @Test
    void testLoadDefaultConfigProperties() throws Exception {
        DefaultObjectGenerator generator = new DefaultObjectGenerator();
        Method method = generator.getClass().getDeclaredMethod("loadDefaultConfigProperties");
        method.setAccessible(true);
        method.invoke(generator);
        Assertions.assertEquals(ConfigPropertyConstants.values().length, qm.getConfigProperties().size());
    }

    @Test
    void testLoadDefaultNotificationPublishers() throws Exception {
        DefaultObjectGenerator generator = new DefaultObjectGenerator();
        Method method = generator.getClass().getDeclaredMethod("loadDefaultNotificationPublishers");
        method.setAccessible(true);
        method.invoke(generator);
        Assertions.assertEquals(DefaultNotificationPublishers.values().length, qm.getAllNotificationPublishers().size());
    }
}
