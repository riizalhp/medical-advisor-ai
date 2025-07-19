package com.contsol.ayra.data.source.local.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AppSQLiteHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_USER_TABLE)
        db.execSQL(CREATE_CHAT_LOG_TABLE)
        db.execSQL(CREATE_HEALTH_METRIC_TABLE)
        db.execSQL(CREATE_PHOTO_LOG_TABLE)
        // db.execSQL(CREATE_MENSTRUAL_CYCLE_TABLE)
        db.execSQL(CREATE_AI_SUGGESTION_TABLE)
        db.execSQL(CREATE_KNOWLEDGE_BASE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle schema changes
        db.execSQL("DROP TABLE IF EXISTS User")
        db.execSQL("DROP TABLE IF EXISTS ChatLog")
        db.execSQL("DROP TABLE IF EXISTS HealthMetric")
        db.execSQL("DROP TABLE IF EXISTS PhotoLog")
        // db.execSQL("DROP TABLE IF EXISTS MenstrualCycle")
        db.execSQL("DROP TABLE IF EXISTS AISuggestion")
        db.execSQL("DROP TABLE IF EXISTS KnowledgeBase")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "healthai.db"
        const val DATABASE_VERSION = 1

        private const val CREATE_USER_TABLE = """
            CREATE TABLE User (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                gender TEXT NOT NULL,
                created_at INTEGER NOT NULL
            );
        """

        private const val CREATE_CHAT_LOG_TABLE = """
            CREATE TABLE ChatLog (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                messageContent TEXT NOT NULL,
                isUserMessage INTEGER NOT NULL,
                imageUrl TEXT,
                timestamp INTEGER NOT NULL
            );
        """

        private const val CREATE_HEALTH_METRIC_TABLE = """
            CREATE TABLE HealthMetric (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                metric_type TEXT NOT NULL,
                value TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                notes TEXT
            );
        """

        private const val CREATE_PHOTO_LOG_TABLE = """
            CREATE TABLE PhotoLog (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_path TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                symptom_detected TEXT
            );
        """

        /* private const val CREATE_MENSTRUAL_CYCLE_TABLE = """
            CREATE TABLE MenstrualCycle (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                start_date INTEGER NOT NULL,
                end_date INTEGER NOT NULL,
                cycle_length INTEGER NOT NULL,
                period_length INTEGER NOT NULL,
            );
        """ */

        private const val CREATE_AI_SUGGESTION_TABLE = """
            CREATE TABLE AISuggestion (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date INTEGER NOT NULL,
                category TEXT NOT NULL,
                suggestion_text TEXT NOT NULL,
                expired_time INTEGER NOT NULL
            );
        """

        private const val CREATE_KNOWLEDGE_BASE_TABLE = """
            CREATE TABLE KnowledgeBase (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                last_updated INTEGER NOT NULL
            );
        """
    }
}
