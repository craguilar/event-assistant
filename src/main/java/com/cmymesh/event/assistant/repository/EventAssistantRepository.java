package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.model.GuestTracking;
import com.cmymesh.event.assistant.model.MessageResponse;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.evolve.Renamer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EventAssistantRepository implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(EventAssistantRepository.class);

    private final Environment env;
    private final EntityStore store;

    private final GuestTrackingAccessor dao;

    public EventAssistantRepository(File envHome) {
        var homeExists = envHome.mkdir();
        if (homeExists) {
            LOG.warn("BDB home doesn't exist creating it");
        }
        /* Open a transactional Berkeley DB engine environment. */
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        env = new Environment(envHome, envConfig);

        Mutations mutations = new Mutations();
        // TODO: :) document this
        mutations.addRenamer(new Renamer("com.cmymesh.service.model.MessageResponse", 0,
                MessageResponse.class.getName()));
        mutations.addRenamer(new Renamer("com.cmymesh.service.model.GuestTracking", 0,
                GuestTracking.class.getName()));

        /* Open a transactional entity store. */
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        storeConfig.setMutations(mutations);
        store = new EntityStore(env, "GuestTrackingStore", storeConfig);

        dao = new GuestTrackingAccessor(store);

    }

    public void save(GuestTracking guestTracking) {
        dao.personById.put(guestTracking);
    }

    public List<GuestTracking> listAll() {
        List<GuestTracking> all = new ArrayList<>();
        for (var entry : dao.personById.map().entrySet()) {
            all.add(entry.getValue());
        }
        return all;
    }

    public GuestTracking get(String guestId) {
        return dao.personById.get(guestId);
    }

    public boolean delete(String guestId) {
        return dao.personById.delete(guestId);
    }

    @Override
    public void close() throws DatabaseException {
        store.close();
        env.close();
    }

    static class GuestTrackingAccessor {

        /* Person accessors */
        final PrimaryIndex<String, GuestTracking> personById;


        /* Opens all primary and secondary indices. */
        public GuestTrackingAccessor(EntityStore store)
                throws DatabaseException {

            personById = store.getPrimaryIndex(String.class, GuestTracking.class);

        }
    }
}
