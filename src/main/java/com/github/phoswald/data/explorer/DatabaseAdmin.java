package com.github.phoswald.data.explorer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import styx.data.Store;
import styx.data.db.Database;
import styx.data.db.DatabaseStore;
import styx.data.db.DatabaseTransaction;
import styx.data.db.Row;

public class DatabaseAdmin {

    public static void main(String[] args) throws IOException {
        if(args.length == 3 && args[0].equals("-export")) {
            exportDatabase(args[1], Paths.get(args[2]));
        } else if(args.length == 3 && args[0].equals("-import")) {
            importDatabase(args[1], Paths.get(args[2]));
        } else {
            printUsage();
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  $ java " + DatabaseAdmin.class.getName() + " -export <database-url> <destination-file>");
        System.out.println("  $ java " + DatabaseAdmin.class.getName() + " -import <database-url> <source-file>");
    }

    private static void exportDatabase(String databaseUrl, Path destinationFile) throws IOException {
        try(Store store = Store.open(databaseUrl)) {
            AtomicLong counter = new AtomicLong();
            try(DatabaseTransaction txn = getDatabase(store).openReadTransaction()) {
                Files.write(destinationFile, toIterable(txn.selectAll().
                        peek(r -> counter.incrementAndGet()).
                        map(Row::encode)));
            }
            System.out.println("Exported " + counter.get() + " rows from " + databaseUrl + " into " + destinationFile);
        }
    }

    private static void importDatabase(String databaseUrl, Path sourceFile) throws IOException {
        try(Store store = Store.open(databaseUrl)) {
            AtomicLong counter = new AtomicLong();
            try(DatabaseTransaction txn = getDatabase(store).openWriteTransaction()) {
                txn.deleteAll();
                Files.lines(sourceFile).
                        map(Row::decode).
                        peek(r -> counter.incrementAndGet()).
                        forEach(txn::insert);
            }
            System.out.println("Imported " + counter.get() + " rows from " + sourceFile + " into " + databaseUrl);
        }
    }

    private static Database getDatabase(Store store) {
        if(store instanceof DatabaseStore) {
            return ((DatabaseStore) store).getDatabase();
        } else {
            throw new IllegalArgumentException("Not a database URL.");
        }
    }

    private static <T> Iterable<T> toIterable(Stream<T> stream) {
        return stream::iterator;
    }
}
