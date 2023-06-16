package com.cmymesh.event.assistant.repository;

import com.cmymesh.event.assistant.ApplicationConstants;
import com.cmymesh.event.assistant.model.GuestTracking;
import com.cmymesh.event.assistant.model.MessageResponse;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.IndexNotAvailableException;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.evolve.Conversion;
import com.sleepycat.persist.evolve.Converter;
import com.sleepycat.persist.evolve.Mutations;
import com.sleepycat.persist.evolve.Renamer;
import com.sleepycat.persist.model.EntityModel;
import com.sleepycat.persist.raw.RawObject;
import com.sleepycat.persist.raw.RawStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventAssistantRepository implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(EventAssistantRepository.class);

    private static final String DATA_STORE_NAME = "GuestTrackingStore";

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
        /*
        Mutations are needed when there are changes to related classes "entities" unfortunately I did changes on a live
        production use ... and I ended up needing to implement this :)
         */
        mutations.addRenamer(new Renamer(ApplicationConstants.OLD_BASE_PACKAGE + ".model.MessageResponse", 0,
                MessageResponse.class.getName()));
        mutations.addRenamer(new Renamer(ApplicationConstants.OLD_BASE_PACKAGE + ".model.GuestTracking", 0,
                GuestTracking.class.getName()));
        mutations.addConverter(new Converter(MessageResponse.class.getName(), 2, "status", new MessageResponseStatusConversion()));

        /* Open a transactional entity store. */
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        storeConfig.setMutations(mutations);
        store = new EntityStore(env, DATA_STORE_NAME, storeConfig);

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
        var status = dao.personById.delete(guestId);
        if (status) {
            // We need traces for deleted elements
            LOG.info("Deleted {}", guestId);
        }
        return status;
    }

    @Override
    public void close() throws DatabaseException {
        store.close();
        env.close();
    }

    public void dump() throws DatabaseException {
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setReadOnly(true);
        storeConfig.setTransactional(true);
        RawStore rawStore = new RawStore(env, DATA_STORE_NAME, storeConfig);
        EntityModel model = rawStore.getModel();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("./dump.xml"))) {
            writer.write("<Objects>");
            writer.newLine();
            for (String clsName : model.getKnownClasses()) {
                if (model.getEntityMetadata(clsName) != null) {
                    final PrimaryIndex<Object, RawObject> index;
                    try {
                        index = rawStore.getPrimaryIndex(clsName);
                    } catch (IndexNotAvailableException e) {
                        LOG.error("Skipping primary index that is {} not yet available", clsName);
                        continue;
                    }
                    EntityCursor<RawObject> entities = null;
                    try {
                        entities = index.entities();
                        for (RawObject entity : entities) {
                            writer.write(entity.toString());
                        }
                    } finally {
                        if (entities != null) {
                            entities.close();
                        }
                    }


                }
            }
            rawStore.close();
            writer.newLine();
            writer.write("</Objects>");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


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

    private static class MessageResponseStatusConversion implements Conversion {

        @Override
        public void initialize(EntityModel model) {
            // Not doing anything
        }

        @Override
        public Object convert(Object o) {
            return o;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MessageResponseStatusConversion;
        }
    }
}