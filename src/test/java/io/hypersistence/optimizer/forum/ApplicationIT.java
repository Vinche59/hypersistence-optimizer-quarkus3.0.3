/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.hypersistence.optimizer.forum;

import io.hypersistence.optimizer.HypersistenceOptimizer;
import io.hypersistence.optimizer.core.config.JpaConfig;
import io.hypersistence.optimizer.core.event.Event;
import io.hypersistence.optimizer.forum.domain.Post;
import io.hypersistence.optimizer.forum.domain.Tag;
import io.hypersistence.optimizer.forum.service.ForumService;
import io.hypersistence.optimizer.hibernate.event.configuration.query.QueryInClauseParameterPaddingEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.query.QueryPaginationCollectionFetchingEvent;
import io.hypersistence.optimizer.hibernate.event.configuration.schema.SchemaGenerationEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.ManyToManyListEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.OneToOneParentSideEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.OneToOneWithoutMapsIdEvent;
import io.hypersistence.optimizer.hibernate.event.mapping.association.fetching.EagerFetchingEvent;
import io.hypersistence.optimizer.hibernate.event.query.PaginationWithoutOrderByEvent;
import io.hypersistence.optimizer.hibernate.event.session.SessionTimeoutEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class ApplicationIT {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private HypersistenceOptimizer hypersistenceOptimizer;

    @PersistenceContext
    private EntityManager entityManager;

    @Inject
    private ForumService forumService;

    @BeforeEach
    @Transactional
    public void init() {
        hypersistenceOptimizer = new HypersistenceOptimizer(
            new JpaConfig(entityManager.getEntityManagerFactory())
                    .setBannerPrintingEnabled(false)
        );

        Tag hibernate = new Tag();
        hibernate.setName("hibernate");
        entityManager.persist(hibernate);

        Tag jpa = new Tag();
        jpa.setName("jpa");
        entityManager.persist(jpa);
    }

    @Test
    void test() {
        assertEventTriggered(2, EagerFetchingEvent.class);
        assertEventTriggered(1, ManyToManyListEvent.class);
        assertEventTriggered(1, OneToOneWithoutMapsIdEvent.class);
        assertEventTriggered(1, SchemaGenerationEvent.class);
        assertEventTriggered(1, QueryPaginationCollectionFetchingEvent.class);

        Post newPost = null;

        for (int i = 0; i < 10; i++) {
            newPost = forumService.newPost("High-Performance Java Persistence", Arrays.asList("hibernate", "jpa"));
            assertNotNull(newPost.getId());
        }

        List<Post> posts = forumService.findAllByTitle("High-Performance Java Persistence");
        assertEquals(10, posts.size());

        Post post = forumService.findById(newPost.getId());
        assertEquals("High-Performance Java Persistence", post.getTitle());

        hypersistenceOptimizer.getEvents().clear();
    }

    protected void assertEventTriggered(int expectedCount, Class<? extends Event> eventClass) {
        int count = 0;

        for (Event event : hypersistenceOptimizer.getEvents()) {
            if (event.getClass().equals(eventClass)) {
                count++;
            }
        }

        assertSame(expectedCount, count);
    }
}

