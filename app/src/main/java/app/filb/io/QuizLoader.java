package app.filb.io;

import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import app.filb.core.QuizQuestion;

public class QuizLoader {
    private static final Gson GSON = new Gson();

    private AssetManager assetManager;

    public QuizLoader(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    // JSON-Fragendatei aus den Android-Assets laden und als veränderbare Fragenliste zurückgeben.
    // IOException: Datei/Pfad nicht gefunden oder Lesefehler.
    // JsonSyntaxException: JSON ist syntaktisch fehlerhaft oder passt nicht zur erwarteten Struktur.
    public List<QuizQuestion> load(String filename) throws IOException, JsonSyntaxException {

        try (InputStream dataStream = assetManager.open(filename);
             InputStreamReader reader = new InputStreamReader(dataStream, StandardCharsets.UTF_8)) {

            // Gson befüllt die QuizQuestion-Objekte anhand der JSON-Feldnamen.
            QuizQuestion[] quizArray = GSON.fromJson(reader, QuizQuestion[].class);

            if (quizArray == null) {
                return new ArrayList<>();
            }

            // ArrayList wird genutzt, weil Fragenpools später gemischt und verändert werden.
            return new ArrayList<>(Arrays.asList(quizArray));
        }
    }
}
