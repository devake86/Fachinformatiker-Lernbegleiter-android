package app.filb.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import app.filb.R;
import app.filb.core.QuizQuestion;
import app.filb.io.QuestionProgressRepository;
import app.filb.io.QuizLoader;

// Einstiegspunkt der Android-App.
// Erbt von AppCompatActivity, um Activity-Lifecycle, Layouts, Navigation und AppCompat-Themes zu nutzen.
public class MainActivity extends AppCompatActivity {
    private static final String SETTINGS_NAME = "settings";

    private static final String THEME_SETTING_KEY = "themeSetting";
    private static final int LIGHT_THEME = 1;
    private static final int DARK_THEME = 2;

    private static final String LEFT_HANDED_KEY = "leftHanded";

    private static final String INTENT_EXTRA_QUIZ_MODE = "quizMode";
    private static final String INTENT_EXTRA_JSON_LIST = "jsonList";

    private static final String HAS_LAST_QUIZ_DATA_KEY = "hasLastQuizData";
    private static final String LAST_QUIZ_MODE_KEY = "lastQuizMode";
    private static final String LAST_JSON_LIST_KEY = "lastJsonList";

    private static final String INTENT_EXTRA_WRONG_QUESTIONS_JSON = "wrongQuestionsJson";

    private static final String QUIZ_MODE_AP1 = "AP1";

    private static final int LF20_QUESTIONS_SIZE = 20;

    private static final int QUESTION_MULTIPLIER_AP1 = 3;
    private static final int TAKE_QUESTIONS_LF01 = 1 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF02 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF03 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF04 = 1 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF05 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF06 = 1 * QUESTION_MULTIPLIER_AP1;

    private static final String ERROR_JSON_PATH = "Keine JSON vorhanden oder falscher Pfad.";
    private static final String ERROR_JSON_SYNTAX = "Fehler in der JSON-Struktur.";

    private static final String QUIZ_MODE_ERROR_TRAINING = "Fehlertraining";
    private static final String LAST_SELECTION_ERROR_TEXT =
            "Letzte Auswahl konnte nicht geladen werden. Wähle bitte Modus und Fragenpaket neu.";

    private static final float LOWERED_BUTTON_ALPHA = 0.65f;
    private static final float ENABLED_BUTTON_ALPHA = 1.0f;

    // XML: Texte
    private TextView versionIdText;
    private TextView titleText;
    private TextView flavorText;

    // XML: Hauptnavigation
    private Button quizChoiceButton;
    private Button startWithLastQuizDataButton;
    private Button errorTrainingButton;

    // XML: Platzhalter / Action
    private Button optionsButton;
    private Button actionButton;

    // XML: UI Settings
    private Button leftHandedSelectButton;
    private Button rightHandedSelectButton;
    private Button lightModeSelectButton;
    private Button darkModeSelectButton;

    // Einstellungen und Navigation
    private SharedPreferences sharedPreferences;
    private Intent quizContentData;

    // Quiz Vorbereitung
    private QuizLoader quizLoader;
    private QuestionProgressRepository questionProgressRepository;

    private final List<List<QuizQuestion>> lf20QuestionsSessionPools = new ArrayList<>();

    // AP1-Fragenpools je Lernfeld
    private final List<QuizQuestion> lf01QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf02QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf03QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf04QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf05QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf06QuizSessionPool = new ArrayList<>();

    //  Easter Egg
    private int clickCount = 0;
    private long lastClickTime = 0;
    private CharSequence previousText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUISettings();

        setContentView(R.layout.main_activity);

        quizLoader = new QuizLoader(getAssets());
        questionProgressRepository = new QuestionProgressRepository(this);

        initViews();
        initUI();
        initButtonBehavior();

        creatorEasterEgg();
    }

    // „Letzte Auswahl“-Button und „Fehlertraining (x)"-Button aktualisieren, wenn MainActivity wieder sichtbar wird.
    @Override
    protected void onResume() {
        super.onResume();

        if (!sharedPreferences.getBoolean(HAS_LAST_QUIZ_DATA_KEY, false)) {
            startWithLastQuizDataButton.setVisibility(View.INVISIBLE);
        } else {
            startWithLastQuizDataButton.setVisibility(View.VISIBLE);
        }

        int wrongQuestionCount = questionProgressRepository.getWrongQuestionIds().size();

        if (wrongQuestionCount > 0) {
            errorTrainingButton.setVisibility(View.VISIBLE);
            errorTrainingButton.setText("Fehlertraining (" + wrongQuestionCount + ")");
        } else {
            errorTrainingButton.setVisibility(View.INVISIBLE);
        }
    }

    // Gespeicherte UI-Einstellungen laden und Standardwerte setzen.
    private void initUISettings() {
        sharedPreferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);

        if (!sharedPreferences.contains(THEME_SETTING_KEY)) {
            sharedPreferences.edit().putInt(THEME_SETTING_KEY, DARK_THEME).apply();
        }

        if (!sharedPreferences.contains(LEFT_HANDED_KEY)) {
            sharedPreferences.edit().putBoolean(LEFT_HANDED_KEY, false).apply();
        }

        if (sharedPreferences.getInt(THEME_SETTING_KEY, DARK_THEME) == LIGHT_THEME) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    // XML-Views holen und Feldern zuweisen.
    private void initViews() {
        versionIdText = findViewById(R.id.versionIdText);

        titleText = findViewById(R.id.titleText);
        flavorText = findViewById(R.id.flavorText);

        quizChoiceButton = findViewById(R.id.quizChoiceButton);
        startWithLastQuizDataButton = findViewById(R.id.startWithLastQuizDataButton);
        errorTrainingButton = findViewById(R.id.errorTrainingButton);

        optionsButton = findViewById(R.id.optionsButton);

        leftHandedSelectButton = findViewById(R.id.leftHandedSelectButton);
        rightHandedSelectButton = findViewById(R.id.rightHandedSelectButton);
        lightModeSelectButton = findViewById(R.id.lightModeSelectButton);
        darkModeSelectButton = findViewById(R.id.darkModeSelectButton);

        actionButton = findViewById(R.id.actionButton);
    }

    // Hauptmenü anzeigen und gespeicherte UI-Einstellungen darstellen.
    private void initUI() {
        versionIdText.setVisibility(View.VISIBLE);

        titleText.setVisibility(View.VISIBLE);
        flavorText.setVisibility(View.VISIBLE);

        quizChoiceButton.setVisibility(View.VISIBLE);

        if (!sharedPreferences.getBoolean(HAS_LAST_QUIZ_DATA_KEY, false)) {
            startWithLastQuizDataButton.setVisibility(View.INVISIBLE);
        } else {
            startWithLastQuizDataButton.setVisibility(View.VISIBLE);
        }

        // Fehlertraining Button mit Anzahl
        int wrongQuestionCount = questionProgressRepository.getWrongQuestionIds().size();

        if (wrongQuestionCount > 0) {
            errorTrainingButton.setVisibility(View.VISIBLE);
            errorTrainingButton.setText("Fehlertraining (" + wrongQuestionCount + ")");
        } else {
            errorTrainingButton.setVisibility(View.INVISIBLE);
        }

        optionsButton.setVisibility(View.GONE);

        leftHandedSelectButton.setVisibility(View.VISIBLE);
        rightHandedSelectButton.setVisibility(View.VISIBLE);

        // Aktiven Bedienmodus hervorheben.
        if (!sharedPreferences.getBoolean(LEFT_HANDED_KEY, false)) {
            leftHandedSelectButton.setAlpha(LOWERED_BUTTON_ALPHA);
            rightHandedSelectButton.setAlpha(ENABLED_BUTTON_ALPHA);
        } else {
            leftHandedSelectButton.setAlpha(ENABLED_BUTTON_ALPHA);
            rightHandedSelectButton.setAlpha(LOWERED_BUTTON_ALPHA);
        }

        lightModeSelectButton.setVisibility(View.VISIBLE);
        darkModeSelectButton.setVisibility(View.VISIBLE);

        // Aktives Theme hervorheben.
        if (sharedPreferences.getInt(THEME_SETTING_KEY, DARK_THEME) == DARK_THEME) {
            lightModeSelectButton.setAlpha(LOWERED_BUTTON_ALPHA);
            darkModeSelectButton.setAlpha(ENABLED_BUTTON_ALPHA);
        } else {
            lightModeSelectButton.setAlpha(ENABLED_BUTTON_ALPHA);
            darkModeSelectButton.setAlpha(LOWERED_BUTTON_ALPHA);
        }

        actionButton.setVisibility(View.VISIBLE);
    }

    private void initButtonBehavior() {
        quizChoiceButton.setOnClickListener(onClick -> loadQuizMenu());

        startWithLastQuizDataButton.setOnClickListener(onClick -> startWithLastQuizData());

        errorTrainingButton.setOnClickListener(onClick -> startErrorTraining());

        // Linkshändige/rechtshändige Bedienung beeinflusst Exit-Button und Antwortmarker im Quiz.
        leftHandedSelectButton.setOnClickListener(v -> {
            sharedPreferences.edit().putBoolean(LEFT_HANDED_KEY, true).apply();

            leftHandedSelectButton.setAlpha(ENABLED_BUTTON_ALPHA);
            rightHandedSelectButton.setAlpha(LOWERED_BUTTON_ALPHA);
        });

        rightHandedSelectButton.setOnClickListener(v -> {
            sharedPreferences.edit().putBoolean(LEFT_HANDED_KEY, false).apply();

            leftHandedSelectButton.setAlpha(LOWERED_BUTTON_ALPHA);
            rightHandedSelectButton.setAlpha(ENABLED_BUTTON_ALPHA);
        });

        lightModeSelectButton.setOnClickListener(v -> {
            sharedPreferences.edit().putInt(THEME_SETTING_KEY, LIGHT_THEME).apply();
            applyTheme(sharedPreferences);
        });

        darkModeSelectButton.setOnClickListener(v -> {
            sharedPreferences.edit().putInt(THEME_SETTING_KEY, DARK_THEME).apply();
            applyTheme(sharedPreferences);
        });

        actionButton.setOnClickListener(onClick -> {
            showDialogConfirmationMenu("Lernbegleiter wirklich verlassen?");
        });

        // Android Zurück-Taste blockieren, damit die App nur bewusst über den Dialog verlassen wird.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    // Quiz-Auswahlmenü starten.
    private void loadQuizMenu() {
        quizContentData = new Intent(this, QuizMenuActivity.class);

        startActivity(quizContentData);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
    }

    // Letzte geprüfte Quiz-Auswahl direkt starten und QuizMenu/ContentMenu/ContentSubmenu überspringen.
    private void startWithLastQuizData() {
        if (!sharedPreferences.getBoolean(HAS_LAST_QUIZ_DATA_KEY, false)) {
            sharedPreferences.edit().putBoolean(HAS_LAST_QUIZ_DATA_KEY, false).apply();

            startWithLastQuizDataButton.setVisibility(View.INVISIBLE);
            flavorText.setText(LAST_SELECTION_ERROR_TEXT);

            return;
        }

        String lastQuizMode = sharedPreferences.getString(LAST_QUIZ_MODE_KEY, null);
        String jsonListAsString = sharedPreferences.getString(LAST_JSON_LIST_KEY, null);

        if (lastQuizMode == null || jsonListAsString == null) {
            sharedPreferences.edit().putBoolean(HAS_LAST_QUIZ_DATA_KEY, false).apply();

            startWithLastQuizDataButton.setVisibility(View.INVISIBLE);
            flavorText.setText(LAST_SELECTION_ERROR_TEXT);

            return;
        }

        // JSON-String zurück in String[] umwandeln, da SharedPreferences keine String-Arrays direkt speichert.
        String[] lastJsonList = new Gson().fromJson(jsonListAsString, String[].class);

        if (lastJsonList == null) {
            sharedPreferences.edit().putBoolean(HAS_LAST_QUIZ_DATA_KEY, false).apply();

            startWithLastQuizDataButton.setVisibility(View.INVISIBLE);
            flavorText.setText(LAST_SELECTION_ERROR_TEXT);

            return;
        }

        prepareLastQuizData(lastQuizMode, lastJsonList);
    }

    // Schlüsselmeister: JSONs und interne Datenbank correct_answer prüfen, Quiz nur bei ausreichend Fragen starten.
    // REFACTOR PRIO 4: Ähnliche Prüfungen in mehreren Screens -> Schlüsselmeister-/Torwächterlogik auslagern.
    private void prepareLastQuizData(String quizMode, String[] jsonList) {
        List<String> selectedQuestionIds = new ArrayList<>();

        try {
            if (quizMode.contains(QUIZ_MODE_AP1)) {
                lf01QuizSessionPool.clear();
                lf02QuizSessionPool.clear();
                lf03QuizSessionPool.clear();
                lf04QuizSessionPool.clear();
                lf05QuizSessionPool.clear();
                lf06QuizSessionPool.clear();

                for (String json : jsonList) {
                    List<QuizQuestion> quizQuestions = quizLoader.load(json);

                    addQuestionIdsToSelectedQuestionIds(quizQuestions, selectedQuestionIds);

                    List<QuizQuestion> filteredQuizQuestions = filterCorrectQuestions(quizQuestions);

                    if (json.contains("lf-01")) {
                        lf01QuizSessionPool.addAll(filteredQuizQuestions);
                    } else if (json.contains("lf-02")) {
                        lf02QuizSessionPool.addAll(filteredQuizQuestions);
                    } else if (json.contains("lf-03")) {
                        lf03QuizSessionPool.addAll(filteredQuizQuestions);
                    } else if (json.contains("lf-04")) {
                        lf04QuizSessionPool.addAll(filteredQuizQuestions);
                    } else if (json.contains("lf-05")) {
                        lf05QuizSessionPool.addAll(filteredQuizQuestions);
                    } else if (json.contains("lf-06")) {
                        lf06QuizSessionPool.addAll(filteredQuizQuestions);
                    }
                }

                if (lf01QuizSessionPool.size() < TAKE_QUESTIONS_LF01
                        || lf02QuizSessionPool.size() < TAKE_QUESTIONS_LF02
                        || lf03QuizSessionPool.size() < TAKE_QUESTIONS_LF03
                        || lf04QuizSessionPool.size() < TAKE_QUESTIONS_LF04
                        || lf05QuizSessionPool.size() < TAKE_QUESTIONS_LF05
                        || lf06QuizSessionPool.size() < TAKE_QUESTIONS_LF06) {
                    showDialogConfirmationResetCorrectQuestions(quizMode, jsonList, selectedQuestionIds);
                    return;
                }
            } else {
                lf20QuestionsSessionPools.clear();

                for (String json : jsonList) {
                    List<QuizQuestion> quizQuestions = quizLoader.load(json);

                    addQuestionIdsToSelectedQuestionIds(quizQuestions, selectedQuestionIds);

                    List<QuizQuestion> filteredQuizQuestions = filterCorrectQuestions(quizQuestions);

                    if (!filteredQuizQuestions.isEmpty()) {
                        Collections.shuffle(filteredQuizQuestions);
                        lf20QuestionsSessionPools.add(new ArrayList<>(filteredQuizQuestions));
                    }
                }

                int remainingQuestions = 0;

                for (List<QuizQuestion> lf20QuestionSessionPool : lf20QuestionsSessionPools) {
                    remainingQuestions += lf20QuestionSessionPool.size();
                }

                if (remainingQuestions < LF20_QUESTIONS_SIZE) {
                    showDialogConfirmationResetCorrectQuestions(quizMode, jsonList, selectedQuestionIds);
                    return;
                }
            }

            startLastQuiz(quizMode, jsonList);
        } catch (IOException ioException) {
            flavorText.setText(ERROR_JSON_PATH);
            flavorText.setVisibility(View.VISIBLE);
        } catch (JsonSyntaxException jsonSyntaxException) {
            flavorText.setText(ERROR_JSON_SYNTAX);
            flavorText.setVisibility(View.VISIBLE);
        }
    }

    private void startLastQuiz(String quizMode, String[] jsonList) {
        quizContentData = new Intent(this, QuizActivity.class);
        quizContentData.putExtra(INTENT_EXTRA_QUIZ_MODE, quizMode);
        quizContentData.putExtra(INTENT_EXTRA_JSON_LIST, jsonList);

        startActivity(quizContentData);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
    }

    // Fehlertraining-Pool direkt als QuizActivity starten.
    private void startErrorTraining() {
        Set<String> wrongQuestionIds = questionProgressRepository.getWrongQuestionIds();

        if (wrongQuestionIds.isEmpty()) {
            return;
        }

        List<QuizQuestion> wrongQuestions = new ArrayList<>();

        try {
            for (String json : getAllKnownJsonPaths()) {
                List<QuizQuestion> quizQuestions = quizLoader.load(json);

                for (QuizQuestion quizQuestion : quizQuestions) {
                    if (quizQuestion == null || quizQuestion.getId() == null || quizQuestion.getId().trim().isEmpty()) {
                        continue;
                    }

                    if (wrongQuestionIds.contains(quizQuestion.getId())) {
                        wrongQuestions.add(quizQuestion);
                    }
                }
            }

            if (wrongQuestions.isEmpty()) {
                flavorText.setText("Fehlertraining konnte nicht geladen werden.");
                return;
            }

            String wrongQuestionsAsJson = new Gson().toJson(wrongQuestions);

            Intent wrongQuestionsIntent = new Intent(this, QuizActivity.class);
            wrongQuestionsIntent.putExtra(INTENT_EXTRA_QUIZ_MODE, QUIZ_MODE_ERROR_TRAINING);
            wrongQuestionsIntent.putExtra(INTENT_EXTRA_WRONG_QUESTIONS_JSON, wrongQuestionsAsJson);

            startActivity(wrongQuestionsIntent);
            overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
        } catch (IOException ioException) {
            flavorText.setText(ERROR_JSON_PATH);
        } catch (JsonSyntaxException jsonSyntaxException) {
            flavorText.setText(ERROR_JSON_SYNTAX);
        }
    }

    // REFACTOR PRIO 3: JSON Pfade kommen in mindestens 2 Activities vor -> Auslagern
    private String[] getAllKnownJsonPaths() {
        return new String[]{
                // Sachfragen
                // LF-01
                "questions/lf-01/lf-01-sach-arbm.json",
                "questions/lf-01/lf-01-sach-bwl.json",
                "questions/lf-01/lf-01-sach-markt.json",
                "questions/lf-01/lf-01-sach-orga.json",
                "questions/lf-01/lf-01-sach-sozi.json",
                "questions/lf-01/lf-01-sach-ziel.json",

                // LF-02
                "questions/lf-02/lf-02-sach-besch.json",
                "questions/lf-02/lf-02-sach-hard.json",
                "questions/lf-02/lf-02-sach-kommun.json",
                "questions/lf-02/lf-02-sach-proj.json",
                "questions/lf-02/lf-02-sach-recht.json",
                "questions/lf-02/lf-02-sach-sich.json",

                // LF-03
                "questions/lf-03/lf-03-sach-adrs.json",
                "questions/lf-03/lf-03-sach-cloud.json",
                "questions/lf-03/lf-03-sach-integ.json",
                "questions/lf-03/lf-03-sach-komp.json",
                "questions/lf-03/lf-03-sach-modl.json",
                "questions/lf-03/lf-03-sach-secu.json",
                "questions/lf-03/lf-03-sach-strv.json",

                // LF-04
                "questions/lf-04/lf-04-sach-auth.json",
                "questions/lf-04/lf-04-sach-datg.json",
                "questions/lf-04/lf-04-sach-grds.json",
                "questions/lf-04/lf-04-sach-kryp.json",
                "questions/lf-04/lf-04-sach-lizn.json",
                "questions/lf-04/lf-04-sach-malw.json",
                "questions/lf-04/lf-04-sach-risi.json",
                "questions/lf-04/lf-04-sach-sba.json",
                "questions/lf-04/lf-04-sach-seng.json",
                "questions/lf-04/lf-04-sach-tom.json",

                // LF-05
                "questions/lf-05/lf-05-sach-data.json",
                "questions/lf-05/lf-05-sach-db.json",
                "questions/lf-05/lf-05-sach-dev.json",
                "questions/lf-05/lf-05-sach-modl.json",
                "questions/lf-05/lf-05-sach-prog.json",

                // LF-06
                "questions/lf-06/lf-06-sach-anal.json",
                "questions/lf-06/lf-06-sach-komm.json",
                "questions/lf-06/lf-06-sach-moni.json",
                "questions/lf-06/lf-06-sach-serv.json",
                "questions/lf-06/lf-06-sach-tick.json",
                "questions/lf-06/lf-06-sach-vert.json",
                "questions/lf-06/lf-06-sach-wart.json",

                // Fachbegriffe
                // LF-01
                "questions/lf-01/lf-01-fach-arbm.json",
                "questions/lf-01/lf-01-fach-bwl.json",
                "questions/lf-01/lf-01-fach-markt.json",
                "questions/lf-01/lf-01-fach-orga.json",
                "questions/lf-01/lf-01-fach-sozi.json",
                "questions/lf-01/lf-01-fach-ziel.json",

                // LF-02
                "questions/lf-02/lf-02-fach-besch.json",
                "questions/lf-02/lf-02-fach-hard.json",
                "questions/lf-02/lf-02-fach-kalk.json",
                "questions/lf-02/lf-02-fach-kommun.json",
                "questions/lf-02/lf-02-fach-proj.json",
                "questions/lf-02/lf-02-fach-recht.json",

                // LF-03
                "questions/lf-03/lf-03-fach-adrs.json",
                "questions/lf-03/lf-03-fach-cloud.json",
                "questions/lf-03/lf-03-fach-komp.json",
                "questions/lf-03/lf-03-fach-modl.json",
                "questions/lf-03/lf-03-fach-secu.json",
                "questions/lf-03/lf-03-fach-strv.json",

                // LF-04
                "questions/lf-04/lf-04-fach-auth.json",
                "questions/lf-04/lf-04-fach-datg.json",
                "questions/lf-04/lf-04-fach-grds.json",
                "questions/lf-04/lf-04-fach-kryp.json",
                "questions/lf-04/lf-04-fach-lizn.json",
                "questions/lf-04/lf-04-fach-malw.json",
                "questions/lf-04/lf-04-fach-risi.json",
                "questions/lf-04/lf-04-fach-sba.json",
                "questions/lf-04/lf-04-fach-seng.json",
                "questions/lf-04/lf-04-fach-tom.json",

                // LF-05
                "questions/lf-05/lf-05-fach-data.json",
                "questions/lf-05/lf-05-fach-db.json",
                "questions/lf-05/lf-05-fach-dev.json",
                "questions/lf-05/lf-05-fach-modl.json",
                "questions/lf-05/lf-05-fach-prog.json",

                // LF-06
                "questions/lf-06/lf-06-fach-anal.json",
                "questions/lf-06/lf-06-fach-komm.json",
                "questions/lf-06/lf-06-fach-moni.json",
                "questions/lf-06/lf-06-fach-serv.json",
                "questions/lf-06/lf-06-fach-tick.json",
                "questions/lf-06/lf-06-fach-vert.json",
                "questions/lf-06/lf-06-fach-wart.json",

                // Abkürzungen
                // LF-01
                "questions/lf-01/lf-01-abkz.json",

                // LF-02
                "questions/lf-02/lf-02-abkz.json",

                // LF-03
                "questions/lf-03/lf-03-abkz.json",

                // LF-04
                "questions/lf-04/lf-04-abkz.json",

                // LF-05
                "questions/lf-05/lf-05-abkz.json",

                // LF-06
                "questions/lf-06/lf-06-abkz.json",

                // Berechnungen
                // LF-02
                "questions/lf-02/lf-02-rech-finanz.json",
                "questions/lf-02/lf-02-rech-kalk.json",
                "questions/lf-02/lf-02-rech-wirt.json",

                // LF-03
                "questions/lf-03/lf-03-rech-netz.json",

                // LF-05
                "questions/lf-05/lf-05-rech-zahl.json",

                // Subnetting
                // LF-03
                "questions/lf-03/lf-03-subn-ipv4.json",

                // Codeausschnitte
                // LF-05
                "questions/lf-05/lf-05-code-py.json",
                "questions/lf-05/lf-05-code-sql.json",
        };
    }

    // REFACTOR PRIO 1: Dialog-Erzeugung ist ähnlich wie in anderen Screens und könnte später ausgelagert werden.
    private void showDialogConfirmationMenu(String popUpMessage) {
        View dialogConfirmationXML = getLayoutInflater().inflate(R.layout.dialog_confirmation, null);

        TextView dialogText = dialogConfirmationXML.findViewById(R.id.dialogText);
        Button leftConfirmationButton = dialogConfirmationXML.findViewById(R.id.leftConfirmationButton);
        Button rightConfirmationButton = dialogConfirmationXML.findViewById(R.id.rightConfirmationButton);

        dialogText.setText(popUpMessage);

        AlertDialog dialogConfirmationWindow = new AlertDialog.Builder(this).setView(dialogConfirmationXML).create();

        // Dialog darf nur über die Dialogbuttons geschlossen werden.
        dialogConfirmationWindow.setCanceledOnTouchOutside(false);
        dialogConfirmationWindow.setCancelable(false);

        if (!sharedPreferences.getBoolean(LEFT_HANDED_KEY, false)) {
            leftConfirmationButton.setText("Beenden");
            rightConfirmationButton.setText("Weiterlernen");

            leftConfirmationButton.setOnClickListener(onClick -> {
                finishAndRemoveTask();
                dialogConfirmationWindow.dismiss();
            });

            rightConfirmationButton.setOnClickListener(onClick -> dialogConfirmationWindow.dismiss());
        } else {
            leftConfirmationButton.setText("Weiterlernen");
            rightConfirmationButton.setText("Beenden");

            leftConfirmationButton.setOnClickListener(onClick -> dialogConfirmationWindow.dismiss());

            rightConfirmationButton.setOnClickListener(onClick -> {
                finishAndRemoveTask();
                dialogConfirmationWindow.dismiss();
            });
        }

        dialogConfirmationWindow.show();
    }

    // Richtige Fragen zurücksetzen Dialog.
    // REFACTOR PRIO 1: Ähnlichkeiten mit den anderen Dialogmenüs.
    private void showDialogConfirmationResetCorrectQuestions(String quizMode, String[] jsonList, List<String> selectedQuestionIds) {
        View dialogConfirmationXML = getLayoutInflater().inflate(R.layout.dialog_confirmation, null);

        TextView dialogText = dialogConfirmationXML.findViewById(R.id.dialogText);
        Button leftConfirmationButton = dialogConfirmationXML.findViewById(R.id.leftConfirmationButton);
        Button rightConfirmationButton = dialogConfirmationXML.findViewById(R.id.rightConfirmationButton);

        dialogText.setText("Auswahl bereits abgeschlossen.\n\nZurücksetzen und Quiz starten?");

        AlertDialog dialogConfirmationWindow = new AlertDialog.Builder(this).setView(dialogConfirmationXML).create();

        dialogConfirmationWindow.setCanceledOnTouchOutside(false);
        dialogConfirmationWindow.setCancelable(false);

        if (!sharedPreferences.getBoolean(LEFT_HANDED_KEY, false)) {
            leftConfirmationButton.setText("Abbrechen");
            rightConfirmationButton.setText("Quiz starten");

            leftConfirmationButton.setOnClickListener(onClick -> dialogConfirmationWindow.dismiss());

            rightConfirmationButton.setOnClickListener(onClick -> {
                questionProgressRepository.resetCorrectAnswersForQuestionIds(selectedQuestionIds);

                dialogConfirmationWindow.dismiss();

                prepareLastQuizData(quizMode, jsonList);
            });
        } else {
            leftConfirmationButton.setText("Quiz starten");
            rightConfirmationButton.setText("Abbrechen");

            leftConfirmationButton.setOnClickListener(onClick -> {
                questionProgressRepository.resetCorrectAnswersForQuestionIds(selectedQuestionIds);

                dialogConfirmationWindow.dismiss();

                prepareLastQuizData(quizMode, jsonList);
            });

            rightConfirmationButton.setOnClickListener(onClick -> dialogConfirmationWindow.dismiss());
        }

        dialogConfirmationWindow.show();
    }

    // Gespeichertes Theme anwenden und Activity neu aufbauen.
    private void applyTheme(SharedPreferences sharedPreferences) {
        if (sharedPreferences.getInt(THEME_SETTING_KEY, DARK_THEME) == LIGHT_THEME) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        recreate();
    }

    // Hilfsmethode um alle gültigen IDs aus einer geladenen JSON in selectedQuestionIds zu sammeln.
    private void addQuestionIdsToSelectedQuestionIds(List<QuizQuestion> quizQuestions, List<String> selectedQuestionIds) {
        for (QuizQuestion quizQuestion : quizQuestions) {
            if (quizQuestion == null || quizQuestion.getId() == null || quizQuestion.getId().trim().isEmpty()) {
                continue;
            }

            selectedQuestionIds.add(quizQuestion.getId());
        }
    }

    // Hilfsmethode um bereits richtig beantwortete Fragen beim Quiz-Start zu filtern.
    private List<QuizQuestion> filterCorrectQuestions(List<QuizQuestion> quizQuestions) {
        List<QuizQuestion> filteredQuizQuestions = new ArrayList<>();

        Set<String> correctQuestionIds = questionProgressRepository.getCorrectQuestionIds();

        for (QuizQuestion quizQuestion : quizQuestions) {
            if (quizQuestion == null || quizQuestion.getId() == null || quizQuestion.getId().trim().isEmpty()) {
                continue;
            }

            if (!correctQuestionIds.contains(quizQuestion.getId())) {
                filteredQuizQuestions.add(quizQuestion);
            }
        }

        return filteredQuizQuestions;
    }

    // Easter Egg: 3 Klicks innerhalb von 1 Sekunde auf die Versionsanzeige.
    private void creatorEasterEgg() {
        versionIdText = findViewById(R.id.versionIdText);
        previousText = versionIdText.getText();

        versionIdText.setOnClickListener(onClick -> {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastClickTime > 1000) {
                clickCount = 0;
            }

            clickCount++;
            lastClickTime = currentTime;

            if (clickCount >= 3) {
                versionIdText.setText("created by devake86");

                // Text nach 3 Sekunden wieder zurücksetzen.
                versionIdText.postDelayed(() -> versionIdText.setText(previousText), 3000);

                clickCount = 0;
            }
        });
    }

}
