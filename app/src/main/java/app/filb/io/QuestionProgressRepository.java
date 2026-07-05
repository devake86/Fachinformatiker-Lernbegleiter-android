package app.filb.io;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestionProgressRepository {
    private static final String DB_FILE_NAME = "filb-progress.db";
    private static final int DB_VERSION = 1;

    private final QuestionProgressDbHelper dbHelper;

    public QuestionProgressRepository(Context context) {
        dbHelper = new QuestionProgressDbHelper(context.getApplicationContext());
    }

    public void markCorrect(String questionId) {
        saveQuestionStatus(questionId, true);
    }

    public void markWrong(String questionId) {
        saveQuestionStatus(questionId, false);
    }

    private void saveQuestionStatus(String questionId, boolean correctAnswer) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return;
        }

        int correctAnswerValue = correctAnswer ? 1 : 0;
        int wrongAnswerValue = correctAnswer ? 0 : 1;

        String insertSql =
                "INSERT OR IGNORE INTO question_progress (question_id, correct_answer, wrong_answer) " +
                        "VALUES (?, ?, ?);";

        String updateSql =
                "UPDATE question_progress " +
                        "SET correct_answer = ?, wrong_answer = ? " +
                        "WHERE question_id = ?;";

        SQLiteDatabase database = dbHelper.getWritableDatabase();

        database.beginTransaction();

        try {
            database.execSQL(insertSql, new Object[]{
                            questionId,
                            correctAnswerValue,
                            wrongAnswerValue
                    }
            );

            database.execSQL(updateSql, new Object[]{
                            correctAnswerValue,
                            wrongAnswerValue,
                            questionId
                    }
            );

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public Set<String> getCorrectQuestionIds() {
        Set<String> questionIds = new HashSet<>();

        String sql =
                "SELECT question_id " +
                        "FROM question_progress " +
                        "WHERE correct_answer = 1;";

        SQLiteDatabase database = dbHelper.getReadableDatabase();

        try (Cursor cursor = database.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                questionIds.add(cursor.getString(0));
            }
        }

        return questionIds;
    }

    public Set<String> getWrongQuestionIds() {
        Set<String> questionIds = new HashSet<>();

        String sql =
                "SELECT question_id " +
                        "FROM question_progress " +
                        "WHERE wrong_answer = 1;";

        SQLiteDatabase database = dbHelper.getReadableDatabase();

        try (Cursor cursor = database.rawQuery(sql, null)) {
            while (cursor.moveToNext()) {
                questionIds.add(cursor.getString(0));
            }
        }

        return questionIds;
    }

    public void resetCorrectAnswersForQuestionIds(Collection<String> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        String placeholders = placeholderForSqlInQuery(questionIds.size());

        String sql =
                "UPDATE question_progress " +
                        "SET correct_answer = 0 " +
                        "WHERE correct_answer = 1 " +
                        "AND question_id IN (" + placeholders + ");";

        Object[] bindArgs = new Object[questionIds.size()];

        int index = 0;

        for (String questionId : questionIds) {
            bindArgs[index] = questionId;
            index++;
        }

        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.execSQL(sql, bindArgs);
    }

    private String placeholderForSqlInQuery(int count) {
        List<String> placeholders = new ArrayList<>();

        for (int index = 0; index < count; index++) {
            placeholders.add("?");
        }

        return String.join(", ", placeholders);
    }

    private static class QuestionProgressDbHelper extends SQLiteOpenHelper {
        private QuestionProgressDbHelper(Context context) {
            super(context, DB_FILE_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            String sql =
                    "CREATE TABLE IF NOT EXISTS question_progress (" +
                            "question_id TEXT PRIMARY KEY, " +
                            "correct_answer INTEGER NOT NULL DEFAULT 0, " +
                            "wrong_answer INTEGER NOT NULL DEFAULT 0" +
                            ");";

            database.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        }
    }

}