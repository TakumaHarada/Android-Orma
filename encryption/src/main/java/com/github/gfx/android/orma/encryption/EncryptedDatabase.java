/*
 * Copyright (c) 2015 FUJI Goro (gfx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.gfx.android.orma.encryption;

import com.github.gfx.android.orma.core.Database;
import com.github.gfx.android.orma.core.DatabaseProvider;
import com.github.gfx.android.orma.core.DatabaseStatement;

import net.sqlcipher.DatabaseUtils;
import net.sqlcipher.database.SQLiteDatabase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import java.io.File;

public class EncryptedDatabase implements Database {

    private final SQLiteDatabase database;

    private EncryptedDatabase(SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues initialValues, int conflictAlgorithm) {
        return database.insertWithOnConflict(table, nullColumnHack, initialValues, conflictAlgorithm);
    }

    @Override
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        return database.update(table, values, whereClause, whereArgs);
    }

    @Override
    public Cursor rawQuery(String sql, String[] selectionArgs) {
        return database.rawQuery(sql, selectionArgs);
    }

    @Override
    public DatabaseStatement compileStatement(String sql) {
        return new EncryptedDatabaseStatement(database.compileStatement(sql));
    }

    @Override
    public void beginTransaction() {
        database.beginTransaction();
    }

    @Override
    public void beginTransactionNonExclusive() { database.beginTransactionNonExclusive(); }

    @Override
    public void setTransactionSuccessful() {
        database.setTransactionSuccessful();
    }

    @Override
    public void endTransaction() {
        database.endTransaction();
    }

    @Override
    public boolean inTransaction() {
        return database.inTransaction();
    }

    @Override
    public void execSQL(String sql) {
        database.execSQL(sql);
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) {
        database.execSQL(sql, bindArgs);
    }

    @Override
    public void setForeignKeyConstraintsEnabled(boolean enable) {
        if (enable) {
            execSQL("PRAGMA foreign_keys = ON");
        } else {
            execSQL("PRAGMA foreign_keys = OFF");
        }
    }

    @Override
    public boolean enableWriteAheadLogging() {
        // SQLCipher does not support WAL
        return false;
    }

    @Override
    public boolean isWriteAheadLoggingEnabled() {
        // SQLCipher does not support WAL
        return false;
    }

    @Override
    public long longForQuery(String query, String[] selectionArgs) {
        return DatabaseUtils.longForQuery(database, query, selectionArgs);
    }

    @Override
    public int getVersion() {
        return database.getVersion();
    }

    @Override
    public void setVersion(int version) {
        database.setVersion(version);
    }

    @Override
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) {
        return database.insertOrThrow(table, nullColumnHack, values);
    }

    @Override
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs, String groupBy,
            String having, String orderBy, String limit) {
        return database.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
    }

    @Override
    public long queryNumEntries(String table, String selection, String[] selectionArgs) {
        // copied from android.database.DatabaseUtils
        String s = (!TextUtils.isEmpty(selection)) ? " where " + selection : "";
        return longForQuery("select count(*) from " + table + s, selectionArgs);
    }

    @Override
    public void close() {
        database.close();
    }

    public static class Provider implements DatabaseProvider {

        private final String password;

        public Provider(@NonNull String password) {
            this.password = password;
        }

        @NonNull
        @Override
        public Database provideOnDiskDatabase(@NonNull Context context, @NonNull String name, int mode) {
            SQLiteDatabase.loadLibs(context);
            File path = context.getDatabasePath(name);
            return new EncryptedDatabase(SQLiteDatabase.openOrCreateDatabase(path, password, null));
        }

        @NonNull
        @Override
        public Database provideOnMemoryDatabase(@NonNull Context context) {
            SQLiteDatabase.loadLibs(context);
            return new EncryptedDatabase(SQLiteDatabase.create(null, password));
        }
    }
}
