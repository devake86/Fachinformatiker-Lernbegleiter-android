package app.filb.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import app.filb.R;
import app.filb.core.QuizAnswer;
import app.filb.core.QuizEngine;
import app.filb.core.QuizQuestion;
import app.filb.io.QuestionProgressRepository;
import app.filb.io.QuizLoader;

public class QuizActivity extends AppCompatActivity {
    private static final String SETTINGS_NAME = "settings";
    private static final String LEFT_HANDED_KEY = "leftHanded";

    private static final String INTENT_EXTRA_QUIZ_MODE = "quizMode";
    private static final String INTENT_EXTRA_JSON_LIST = "jsonList";

    private static final String INTENT_EXTRA_WRONG_QUESTIONS_JSON = "wrongQuestionsJson";

    private static final String QUIZ_MODE_AP1 = "AP1";
    private static final String QUIZ_MODE_ERROR_TRAINING = "Fehlertraining";
    private static final String FALLBACK_QUIZ_MODE = "AP1 - Sachfragen";

    private static final String RESULT_TEXT_PREFIX = "Ergebnis: ";
    private static final String QUESTION_ID_PREFIX = "⧉ ID: ";
    private static final String QUESTION_ID_COPIED_TEXT = "Fragen-ID kopiert";
    private static final String ALL_QUESTIONS_SOLVED_TEXT = "Alle vorhandenen Fragen für dieses Fragenpaket richtig beantwortet.";
    private static final String ERROR_NOT_ENOUGH_QUESTIONS = "Zu wenig Fragen für diese Auswahl vorhanden.";
    private static final String ERROR_JSON_PATH = "Keine JSON vorhanden oder falscher Pfad.";
    private static final String ERROR_JSON_SYNTAX = "Fehler in der JSON-Struktur.";

    private static final String MARKER_CORRECT = "✓";
    private static final String MARKER_WRONG = "✕";
    private static final String EXIT_SYMBOL = "✕";

    private static final float LOWERED_BUTTON_ALPHA = 0.65f;
    private static final float ENABLED_BUTTON_ALPHA = 1.0f;

    // 20-Fragen-Modus: Poolgröße
    private static final int LF20_QUESTIONS_SIZE = 20;

    private static final int MAX_ANSWER_COUNT = 4;
    private static final int FALLBACK_CORRECT_INDEX = -1;
    private static final int COPY_FEEDBACK_MILLISECONDS = 1000;

    // AP1 Gewichtung LF01-LF06: 1:3:3:1:3:1, Multiplikator 3 ergibt 36 Fragen.
    private static final int QUESTION_MULTIPLIER_AP1 = 3;
    private static final int TAKE_QUESTIONS_LF01 = 1 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF02 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF03 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF04 = 1 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF05 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF06 = 1 * QUESTION_MULTIPLIER_AP1;

    // XML: Statusbar
    private TextView questionCounterText;
    private TextView quizModeText;
    private TextView questionIdText;

    // XML: Frage, Erklärung und Ergebnis
    private TextView questionText;
    private TextView resultText;
    private LinearLayout explanationBox;
    private TextView explanationText;
    private LinearLayout answersBox;

    // XML: Antwortreihen, Antwortbuttons und Marker
    private LinearLayout answerRow1;
    private LinearLayout answerRow2;
    private LinearLayout answerRow3;
    private LinearLayout answerRow4;

    private Button answerButton1;
    private Button answerButton2;
    private Button answerButton3;
    private Button answerButton4;

    private TextView answerLeftMarker1;
    private TextView answerLeftMarker2;
    private TextView answerLeftMarker3;
    private TextView answerLeftMarker4;

    private TextView answerRightMarker1;
    private TextView answerRightMarker2;
    private TextView answerRightMarker3;
    private TextView answerRightMarker4;

    // XML: Action Bar und Resultscreen Buttons
    private Button rightHandedExitButton;
    private Button leftHandedExitButton;
    private Button actionButton;
    private Button newRoundButton;
    private Button repeatSessionWrongQuestionsButton;

    // UI-Hilfslisten für einfachere Verarbeitung der festen XML-Elemente
    private List<LinearLayout> answerRows = new ArrayList<>();
    private List<Button> answerButtons = new ArrayList<>();
    private List<TextView> leftAnswerMarkers = new ArrayList<>();
    private List<TextView> rightAnswerMarkers = new ArrayList<>();

    // UI-Einstellungen
    private SharedPreferences sharedPreferences;

    // Quiz-Zustand
    private QuizEngine quizEngine;
    private QuizLoader quizLoader;
    private QuestionProgressRepository questionProgressRepository;

    private String quizMode;
    private String[] jsonList;

    private boolean confirmed = false;          // Antwort wurde bestätigt?
    private boolean isResultScreen = false;     // Action Button verhält sich im Resultscreen

    private Button selectedAnswerButton;
    private Button correctAnswerButton;

    private final List<List<QuizQuestion>> lf20QuestionsSessionPools = new ArrayList<>();

    // AP1-Fragenpools je Lernfeld
    private final List<QuizQuestion> lf01QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf02QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf03QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf04QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf05QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf06QuizSessionPool = new ArrayList<>();

    private final List<QuizQuestion> ap1QuizRound = new ArrayList<>();

    // Falsch beantwortete Fragen bleiben innerhalb der aktuellen Quizsession für optionale Wiederholung erhalten.
    private final List<QuizQuestion> wrongQuestionsSessionPool = new ArrayList<>();

    // Resultscreen Buttonzustände
    private boolean newRoundQuestionsAvailable = false;
    private boolean wrongQuestionsAvailable = false;

    // Aktuelle Fragen-ID für Kopierfunktion
    private String currentQuestionId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUISettings();

        setContentView(R.layout.quiz_activity);

        // Quizdaten aus Intent übernehmen; Fallback "AP1 - Sachfragen" dient als Sicherheit für direkten Activity-Start.
        quizMode = getIntent().getStringExtra(INTENT_EXTRA_QUIZ_MODE);

        if (quizMode == null) {
            quizMode = FALLBACK_QUIZ_MODE;
        }

        jsonList = getIntent().getStringArrayExtra(INTENT_EXTRA_JSON_LIST);

        if (jsonList == null) {
            jsonList = new String[]{
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
            };
        }

        initViews();
        initAnswerButtonsLists();
        initUI();
        initButtonBehavior();

        quizLoader = new QuizLoader(getAssets());
        questionProgressRepository = new QuestionProgressRepository(this);

        initQuizMode();
    }

    private void initUISettings() {
        sharedPreferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
    }

    // XML-Views holen und Feldern zuweisen.
    private void initViews() {
        questionCounterText = findViewById(R.id.questionCounterText);
        quizModeText = findViewById(R.id.quizModeText);
        questionIdText = findViewById(R.id.questionIdText);

        questionText = findViewById(R.id.questionText);
        resultText = findViewById(R.id.resultText);
        explanationBox = findViewById(R.id.explanationBox);
        explanationText = findViewById(R.id.explanationText);

        answersBox = findViewById(R.id.answersBox);

        answerRow1 = findViewById(R.id.answerRow1);
        answerRow2 = findViewById(R.id.answerRow2);
        answerRow3 = findViewById(R.id.answerRow3);
        answerRow4 = findViewById(R.id.answerRow4);

        answerButton1 = findViewById(R.id.answerButton1);
        answerButton2 = findViewById(R.id.answerButton2);
        answerButton3 = findViewById(R.id.answerButton3);
        answerButton4 = findViewById(R.id.answerButton4);

        answerLeftMarker1 = findViewById(R.id.answerLeftMarker1);
        answerLeftMarker2 = findViewById(R.id.answerLeftMarker2);
        answerLeftMarker3 = findViewById(R.id.answerLeftMarker3);
        answerLeftMarker4 = findViewById(R.id.answerLeftMarker4);

        answerRightMarker1 = findViewById(R.id.answerRightMarker1);
        answerRightMarker2 = findViewById(R.id.answerRightMarker2);
        answerRightMarker3 = findViewById(R.id.answerRightMarker3);
        answerRightMarker4 = findViewById(R.id.answerRightMarker4);

        newRoundButton = findViewById(R.id.newRoundButton);
        repeatSessionWrongQuestionsButton = findViewById(R.id.repeatSessionWrongQuestionsButton);

        rightHandedExitButton = findViewById(R.id.rightHandedExitButton);
        actionButton = findViewById(R.id.actionButton);
        leftHandedExitButton = findViewById(R.id.leftHandedExitButton);
    }

    private void initAnswerButtonsLists() {
        answerRows = new ArrayList<>(List.of(
                answerRow1,
                answerRow2,
                answerRow3,
                answerRow4
        ));

        answerButtons = new ArrayList<>(List.of(
                answerButton1,
                answerButton2,
                answerButton3,
                answerButton4
        ));

        leftAnswerMarkers = new ArrayList<>(List.of(
                answerLeftMarker1,
                answerLeftMarker2,
                answerLeftMarker3,
                answerLeftMarker4
        ));

        rightAnswerMarkers = new ArrayList<>(List.of(
                answerRightMarker1,
                answerRightMarker2,
                answerRightMarker3,
                answerRightMarker4
        ));
    }

    // Startzustand: Quizdetails sind erst sichtbar, sobald eine Frage geladen wurde.
    private void initUI() {
        questionCounterText.setVisibility(View.GONE);
        quizModeText.setVisibility(View.GONE);
        questionIdText.setVisibility(View.GONE);

        questionText.setVisibility(View.GONE);
        resultText.setVisibility(View.GONE);
        explanationBox.setVisibility(View.INVISIBLE);

        newRoundButton.setVisibility(View.GONE);
        repeatSessionWrongQuestionsButton.setVisibility(View.GONE);

        updateExitButtonVisibility();

        actionButton.setVisibility(View.INVISIBLE);
    }

    // Exit Button je nach Bedienmodus links oder rechts anzeigen.
    private void updateExitButtonVisibility() {
        rightHandedExitButton.setText(EXIT_SYMBOL);
        leftHandedExitButton.setText(EXIT_SYMBOL);

        if (!sharedPreferences.getBoolean(LEFT_HANDED_KEY, false)) {
            rightHandedExitButton.setVisibility(View.VISIBLE);
            leftHandedExitButton.setVisibility(View.INVISIBLE);
        } else {
            rightHandedExitButton.setVisibility(View.INVISIBLE);
            leftHandedExitButton.setVisibility(View.VISIBLE);
        }
    }

    private void initButtonBehavior() {
        actionButton.setOnClickListener(onClick -> actionButtonBehavior());
        newRoundButton.setOnClickListener(onClick -> newRoundButtonBehavior());
        repeatSessionWrongQuestionsButton.setOnClickListener(onClick -> repeatSessionWrongQuestionsButtonBehavior());

        rightHandedExitButton.setOnClickListener(onClick -> showDialogConfirmationQuiz("Quiz wirklich verlassen?"));
        leftHandedExitButton.setOnClickListener(onClick -> showDialogConfirmationQuiz("Quiz wirklich verlassen?"));

        questionIdText.setOnClickListener(onClick -> copyCurrentQuestionId());

        // Android Zurück-Taste blockieren, damit das Quiz nur über den Dialog verlassen wird.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        answerButtonBehavior();
    }

    private void answerButtonBehavior() {
        for (Button answerButton : answerButtons) {
            answerButton.setOnClickListener(onClick -> {
                selectedAnswerButton(answerButton);
                actionButton.setVisibility(View.VISIBLE);
            });
        }
    }

    // Quizmodus anhand der übergebenen Intent-Daten starten.
    private void initQuizMode() {
        if (QUIZ_MODE_ERROR_TRAINING.equals(quizMode)) {
            String wrongQuestionsJson = getIntent().getStringExtra(INTENT_EXTRA_WRONG_QUESTIONS_JSON);

            if (wrongQuestionsJson == null || wrongQuestionsJson.trim().isEmpty()) {
                explanationText.setText(ERROR_NOT_ENOUGH_QUESTIONS);
                explanationText.setVisibility(View.VISIBLE);
                explanationBox.setVisibility(View.VISIBLE);
                return;
            }

            QuizQuestion[] wrongQuestionsArray = new Gson().fromJson(wrongQuestionsJson, QuizQuestion[].class);
            List<QuizQuestion> wrongQuestions = new ArrayList<>(Arrays.asList(wrongQuestionsArray));

            setupErrorTraining(wrongQuestions);
        } else if (quizMode.contains(QUIZ_MODE_AP1)) {
            setupAP1();
        } else {
            setupLf20Questions();
        }
    }

    // AP1-Modus vorbereiten: JSONs LF01-LF06 laden, je Lernfeld poolen und mischen.
    public void setupAP1() {
        // Torwächter: Wenn ein AP1-Lernfeldpool nicht die Mindestanzahl Fragen hat, wird kein Quiz gestartet.
        // REFACTOR PRIO 4: Ähnliche Prüfungen in mehreren Controllern -> Schlüsselmeister-/Torwächterlogik auslagern
        try {
            lf01QuizSessionPool.clear();
            lf02QuizSessionPool.clear();
            lf03QuizSessionPool.clear();
            lf04QuizSessionPool.clear();
            lf05QuizSessionPool.clear();
            lf06QuizSessionPool.clear();

            for (String json : jsonList) {
                List<QuizQuestion> quizQuestions = quizLoader.load(json);

                List<QuizQuestion> filteredQuizQuestions = filterCorrectQuestions(quizQuestions);

                // Fragen anhand des JSON-Pfads dem passenden Lernfeldpool zuordnen.
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

            // AP1 benötigt pro Lernfeld genug Fragen für die gewichtete Runde.
            if (lf01QuizSessionPool.size() < TAKE_QUESTIONS_LF01
                    || lf02QuizSessionPool.size() < TAKE_QUESTIONS_LF02
                    || lf03QuizSessionPool.size() < TAKE_QUESTIONS_LF03
                    || lf04QuizSessionPool.size() < TAKE_QUESTIONS_LF04
                    || lf05QuizSessionPool.size() < TAKE_QUESTIONS_LF05
                    || lf06QuizSessionPool.size() < TAKE_QUESTIONS_LF06) {
                explanationText.setText(ERROR_NOT_ENOUGH_QUESTIONS);
                explanationText.setVisibility(View.VISIBLE);
                explanationBox.setVisibility(View.VISIBLE);

                for (LinearLayout answerRow : answerRows) {
                    answerRow.setVisibility(View.INVISIBLE);
                }

                return;
            }
        } catch (IOException ioException) {
            explanationText.setText(ERROR_JSON_PATH);
            explanationText.setVisibility(View.VISIBLE);
            explanationBox.setVisibility(View.VISIBLE);

            for (LinearLayout answerRow : answerRows) {
                answerRow.setVisibility(View.INVISIBLE);
            }

            return;
        } catch (JsonSyntaxException jsonSyntaxException) {
            explanationText.setText(ERROR_JSON_SYNTAX);
            explanationText.setVisibility(View.VISIBLE);
            explanationBox.setVisibility(View.VISIBLE);

            for (LinearLayout answerRow : answerRows) {
                answerRow.setVisibility(View.INVISIBLE);
            }

            return;
        }

        Collections.shuffle(lf01QuizSessionPool);
        Collections.shuffle(lf02QuizSessionPool);
        Collections.shuffle(lf03QuizSessionPool);
        Collections.shuffle(lf04QuizSessionPool);
        Collections.shuffle(lf05QuizSessionPool);
        Collections.shuffle(lf06QuizSessionPool);

        startAP1Round();
    }

    // AP1-Runde erstellen: LF01-LF06 im Verhältnis 1:3:3:1:3:1 ziehen und anschließend mischen.
    private void startAP1Round() {
        isResultScreen = false;
        ap1QuizRound.clear();

        if (lf01QuizSessionPool.size() >= TAKE_QUESTIONS_LF01) {
            ap1QuizRound.addAll(lf01QuizSessionPool.subList(0, TAKE_QUESTIONS_LF01));
            lf01QuizSessionPool.subList(0, TAKE_QUESTIONS_LF01).clear();
        }

        if (lf02QuizSessionPool.size() >= TAKE_QUESTIONS_LF02) {
            ap1QuizRound.addAll(lf02QuizSessionPool.subList(0, TAKE_QUESTIONS_LF02));
            lf02QuizSessionPool.subList(0, TAKE_QUESTIONS_LF02).clear();
        }

        if (lf03QuizSessionPool.size() >= TAKE_QUESTIONS_LF03) {
            ap1QuizRound.addAll(lf03QuizSessionPool.subList(0, TAKE_QUESTIONS_LF03));
            lf03QuizSessionPool.subList(0, TAKE_QUESTIONS_LF03).clear();
        }

        if (lf04QuizSessionPool.size() >= TAKE_QUESTIONS_LF04) {
            ap1QuizRound.addAll(lf04QuizSessionPool.subList(0, TAKE_QUESTIONS_LF04));
            lf04QuizSessionPool.subList(0, TAKE_QUESTIONS_LF04).clear();
        }

        if (lf05QuizSessionPool.size() >= TAKE_QUESTIONS_LF05) {
            ap1QuizRound.addAll(lf05QuizSessionPool.subList(0, TAKE_QUESTIONS_LF05));
            lf05QuizSessionPool.subList(0, TAKE_QUESTIONS_LF05).clear();
        }

        if (lf06QuizSessionPool.size() >= TAKE_QUESTIONS_LF06) {
            ap1QuizRound.addAll(lf06QuizSessionPool.subList(0, TAKE_QUESTIONS_LF06));
            lf06QuizSessionPool.subList(0, TAKE_QUESTIONS_LF06).clear();
        }

        Collections.shuffle(ap1QuizRound);

        quizEngine = new QuizEngine(ap1QuizRound);
        showCurrentQuestion();
    }

    // LF-20-Fragen-Modus vorbereiten: JSONs laden, je Unterthema mischen und als Poolliste speichern.
    public void setupLf20Questions() {
        // Torwächter: Wenn kein Fragenpool geladen werden konnte, wird kein Quiz gestartet.
        // REFACTOR PRIO 4: Ähnliche Prüfungen in mehreren Controllern -> Schlüsselmeister-/Torwächterlogik auslagern
        try {
            lf20QuestionsSessionPools.clear();

            for (String json : jsonList) {
                List<QuizQuestion> quizQuestions = quizLoader.load(json);

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
                explanationText.setText(ERROR_NOT_ENOUGH_QUESTIONS);
                explanationText.setVisibility(View.VISIBLE);
                explanationBox.setVisibility(View.VISIBLE);

                for (LinearLayout answerRow : answerRows) {
                    answerRow.setVisibility(View.INVISIBLE);
                }

                return;
            }
        } catch (IOException ioException) {
            explanationText.setText(ERROR_JSON_PATH);
            explanationText.setVisibility(View.VISIBLE);
            explanationBox.setVisibility(View.VISIBLE);

            for (LinearLayout answerRow : answerRows) {
                answerRow.setVisibility(View.INVISIBLE);
            }

            return;
        } catch (JsonSyntaxException jsonSyntaxException) {
            explanationText.setText(ERROR_JSON_SYNTAX);
            explanationText.setVisibility(View.VISIBLE);
            explanationBox.setVisibility(View.VISIBLE);

            for (LinearLayout answerRow : answerRows) {
                answerRow.setVisibility(View.INVISIBLE);
            }

            return;
        }

        // Poolliste zusätzlich mischen, damit nicht immer dasselbe Unterthema zuerst startet.
        Collections.shuffle(lf20QuestionsSessionPools);

        startLf20QuestionsRound();
    }

    // 20-Fragen-Runde per Round-Robin aus allen verfügbaren Unterthemen bilden.
    private void startLf20QuestionsRound() {
        isResultScreen = false;

        List<QuizQuestion> lf20QuestionsQuizRound = new ArrayList<>();

        while (lf20QuestionsQuizRound.size() < LF20_QUESTIONS_SIZE) {
            boolean stillHasQuestions = false;

            for (List<QuizQuestion> lf20QuestionsSessionPool : lf20QuestionsSessionPools) {
                if (lf20QuestionsQuizRound.size() >= LF20_QUESTIONS_SIZE) {
                    break;
                }

                if (!lf20QuestionsSessionPool.isEmpty()) {
                    lf20QuestionsQuizRound.add(lf20QuestionsSessionPool.remove(0));
                    stillHasQuestions = true;
                }
            }

            // Abbruch, wenn kein Unterthemenpool mehr Fragen liefern konnte.
            if (!stillHasQuestions) {
                break;
            }
        }

        Collections.shuffle(lf20QuestionsQuizRound);

        quizEngine = new QuizEngine(lf20QuestionsQuizRound);
        showCurrentQuestion();
    }

    // Fehlertraining Fragenpool direkt als eigene Wiederholungsrunde starten.
    private void setupErrorTraining(List<QuizQuestion> wrongQuestions) {
        if (wrongQuestions == null || wrongQuestions.isEmpty()) {
            explanationText.setText(ERROR_NOT_ENOUGH_QUESTIONS);
            explanationText.setVisibility(View.VISIBLE);
            explanationBox.setVisibility(View.VISIBLE);
            return;
        }

        Collections.shuffle(wrongQuestions);

        quizEngine = new QuizEngine(wrongQuestions);
        showCurrentQuestion();
    }

    // Aktuelle Frage anzeigen und Quiz-UI für die Antwortphase zurücksetzen.
    private void showCurrentQuestion() {
        isResultScreen = false;

        // UI-Zustand für aktive Frage
        updateExitButtonVisibility();

        answersBox.setVisibility(View.VISIBLE);

        questionCounterText.setVisibility(View.VISIBLE);
        quizModeText.setVisibility(View.VISIBLE);
        questionIdText.setVisibility(View.VISIBLE);

        resultText.setVisibility(View.GONE);

        newRoundButton.setVisibility(View.GONE);
        repeatSessionWrongQuestionsButton.setVisibility(View.GONE);

        questionText.setVisibility(View.VISIBLE);

        explanationBox.setVisibility(View.INVISIBLE);

        selectedAnswerButton = null;
        correctAnswerButton = null;
        confirmed = false;

        actionButton.setVisibility(View.INVISIBLE);
        actionButton.setText("Bestätigen");
        setButtonPrimary(actionButton);

        // Statusbar aktualisieren
        int currentIndex = quizEngine.getCurrentIndex() + 1;
        int totalQuestions = quizEngine.getQuestionCount();

        questionCounterText.setText("Frage " + twoDigit(currentIndex) + "/" + twoDigit(totalQuestions));
        quizModeText.setText(quizMode);

        currentQuestionId = quizEngine.getCurrentQuestion().getId();
        questionIdText.setText(QUESTION_ID_PREFIX + currentQuestionId);

        // Frage und Erklärung setzen
        QuizQuestion question = quizEngine.getCurrentQuestion();

        questionText.setText(question.getQuestion());
        explanationText.setText(question.getExplanation());

        List<QuizAnswer> answers = question.getAnswers();

        // Wahr/Falsch-Fragen sind vorbereitet; aktuell werden hauptsächlich 1-aus-4-Fragen genutzt.
        if (question.getAnswers().size() > 2) {
            Collections.shuffle(question.getAnswers());
        }

        // Feste vier Antwortreihen zurücksetzen und mit vorhandenen Antworten befüllen.
        for (int answerIndex = 0; answerIndex < MAX_ANSWER_COUNT; answerIndex++) {
            LinearLayout answerRow = answerRows.get(answerIndex);
            Button answerButton = answerButtons.get(answerIndex);

            TextView leftMarker = leftAnswerMarkers.get(answerIndex);
            TextView rightMarker = rightAnswerMarkers.get(answerIndex);

            resetAnswerMarker(leftMarker);
            resetAnswerMarker(rightMarker);

            setAnswerButtonNormal(answerButton);

            if (answerIndex < answers.size()) {
                QuizAnswer answer = answers.get(answerIndex);

                answerRow.setVisibility(View.VISIBLE);
                answerButton.setText(answer.getText());
            } else {
                answerRow.setVisibility(View.GONE);
            }
        }
    }

    // Gewählte Antwort optisch markieren und als aktuelle Auswahl speichern.
    private void selectedAnswerButton(Button selectedButton) {
        // Alle Antwortbuttons zuerst in den Normalzustand zurücksetzen.
        for (Button button : answerButtons) {
            setAnswerButtonNormal(button);
        }

        setAnswerButtonSelected(selectedButton);

        selectedAnswerButton = selectedButton;
    }

    // Bestätigen-/Weiter-Button: Antwort prüfen, nächste Frage laden oder Resultscreen öffnen.
    private void actionButtonBehavior() {
        if (quizEngine == null) {
            return;
        }

        if (isResultScreen) {
            showDialogConfirmationQuiz("Quiz wirklich verlassen?");
            return;
        }

        // Nach bestätigter Antwort führt der Button zur nächsten Frage oder zur Auswertung.
        if (confirmed) {
            if (quizEngine.isLastQuestion()) {
                showResult();
            } else {
                quizEngine.nextQuestion();
                showCurrentQuestion();
            }

            return;
        }

        if (selectedAnswerButton == null) {
            return;
        }

        int selectedIndex = answerButtons.indexOf(selectedAnswerButton);
        boolean correct = quizEngine.checkAnswer(selectedIndex);

        quizEngine.answerScore(correct);

        QuizQuestion currentQuestion = quizEngine.getCurrentQuestion();

        if (correct) {
            // Markieren in der internen DB zum Filtern der richtigen Fragen vor Quiz-Runden.
            // Wird somit auch gegebenenfalls aus dem "Fehlertraining"-Modus entfernt.
            questionProgressRepository.markCorrect(currentQuestion.getId());
        } else {
            // Markieren in der internen DB für den "Fehlertraining"-Modus im Hauptmenü.
            questionProgressRepository.markWrong(currentQuestion.getId());

            // Lokaler Fehlerpool für "Falsche wiederholen" am Ende der aktuellen Runde.
            wrongQuestionsSessionPool.add(currentQuestion);
        }

        // Antwortphase beenden: alle Buttons abdunkeln und für weitere Klicks blockieren.
        for (Button answerButton : answerButtons) {
            answerButton.setAlpha(LOWERED_BUTTON_ALPHA);
            answerButton.setClickable(false);
        }

        // Richtige Antwort suchen; -1 dient als Fallback bei fehlerhaft gepflegten Antworten.
        int correctIndex = FALLBACK_CORRECT_INDEX;
        List<QuizAnswer> answers = quizEngine.getCurrentQuestion().getAnswers();
        for (int answerIndex = 0; answerIndex < answers.size(); answerIndex++) {
            if (answers.get(answerIndex).isCorrect()) {
                correctIndex = answerIndex;
                break;
            }
        }

        if (correctIndex != FALLBACK_CORRECT_INDEX) {
            correctAnswerButton = answerButtons.get(correctIndex);

            setAnswerButtonCorrect(correctAnswerButton);
            showAnswerMarker(correctIndex, MARKER_CORRECT, true);
        }

        if (!correct) {
            setAnswerButtonWrong(selectedAnswerButton);
            showAnswerMarker(selectedIndex, MARKER_WRONG, false);
        }

        explanationText.setText(quizEngine.getCurrentQuestion().getExplanation());
        explanationBox.setVisibility(View.VISIBLE);

        if (quizEngine.isLastQuestion()) {
            actionButton.setText("Quiz auswerten");
        } else {
            actionButton.setText("Nächste Frage");
        }

        setButtonPrimary(actionButton);

        confirmed = true;
    }

    // Antwortmarker links oder rechts anzeigen und passend als richtig/falsch markieren.
    private void showAnswerMarker(int markerIndex, String markerSymbol, boolean correctMarker) {
        TextView marker;

        // Markerposition hängt vom Bedienmodus ab.
        if (!sharedPreferences.getBoolean(LEFT_HANDED_KEY, false)) {
            marker = leftAnswerMarkers.get(markerIndex);
        } else {
            marker = rightAnswerMarkers.get(markerIndex);
        }

        marker.setText(markerSymbol);
        marker.setTextColor(getColor(R.color.button_signal_text));

        if (correctMarker) {
            marker.setBackgroundResource(R.drawable.answer_marker_correct_bg);
        } else {
            marker.setBackgroundResource(R.drawable.answer_marker_wrong_bg);
        }

        marker.setVisibility(View.VISIBLE);
    }

    private void resetAnswerMarker(TextView marker) {
        marker.setText("");
        marker.setVisibility(View.INVISIBLE);
    }

    // Gesamtergebnis anzeigen und Resultscreen-Buttons vorbereiten.
    private void showResult() {
        isResultScreen = true;

        actionButton.setText("Hauptmenü");
        setButtonPrimary(actionButton);

        rightHandedExitButton.setVisibility(View.INVISIBLE);
        leftHandedExitButton.setVisibility(View.INVISIBLE);

        questionCounterText.setVisibility(View.GONE);
        quizModeText.setVisibility(View.VISIBLE);
        quizModeText.setText(quizMode);
        questionIdText.setVisibility(View.GONE);

        questionText.setVisibility(View.GONE);
        explanationBox.setVisibility(View.INVISIBLE);

        resultText.setVisibility(View.VISIBLE);
        resultText.setText(RESULT_TEXT_PREFIX + twoDigit(quizEngine.getScore()) + "/" + twoDigit(quizEngine.getQuestionCount()));

        answersBox.setVisibility(View.GONE);

        for (LinearLayout answerRow : answerRows) {
            answerRow.setVisibility(View.GONE);
        }

        showResultScreenButtons();
    }

    // Resultscreen-Buttons abhängig von verfügbaren Fragen anzeigen.
    private void showResultScreenButtons() {
        showNewRoundButton();
        showRepeatSessionWrongQuestionsButton();

        if (!newRoundQuestionsAvailable && !wrongQuestionsAvailable) {
            explanationText.setText(ALL_QUESTIONS_SOLVED_TEXT);
            explanationBox.setVisibility(View.VISIBLE);
        }
    }

    // Neue-Runde-Button nur anzeigen, wenn noch genug Fragen für den aktuellen Modus vorhanden sind.
    private void showNewRoundButton() {
        newRoundButton.setVisibility(View.GONE);
        newRoundQuestionsAvailable = false;

        if (quizMode.contains(QUIZ_MODE_AP1)) {
            boolean hasEnoughQuestions = true;

            if (lf01QuizSessionPool.size() < TAKE_QUESTIONS_LF01) {
                hasEnoughQuestions = false;
            } else if (lf02QuizSessionPool.size() < TAKE_QUESTIONS_LF02) {
                hasEnoughQuestions = false;
            } else if (lf03QuizSessionPool.size() < TAKE_QUESTIONS_LF03) {
                hasEnoughQuestions = false;
            } else if (lf04QuizSessionPool.size() < TAKE_QUESTIONS_LF04) {
                hasEnoughQuestions = false;
            } else if (lf05QuizSessionPool.size() < TAKE_QUESTIONS_LF05) {
                hasEnoughQuestions = false;
            } else if (lf06QuizSessionPool.size() < TAKE_QUESTIONS_LF06) {
                hasEnoughQuestions = false;
            }

            if (hasEnoughQuestions) {
                newRoundButton.setVisibility(View.VISIBLE);
                newRoundQuestionsAvailable = true;
            }

            return;
        }

        int remainingQuestions = 0;

        for (List<QuizQuestion> lf20QuestionSessionPool : lf20QuestionsSessionPools) {
            remainingQuestions += lf20QuestionSessionPool.size();
        }

        if (remainingQuestions >= LF20_QUESTIONS_SIZE) {
            newRoundButton.setVisibility(View.VISIBLE);
            newRoundQuestionsAvailable = true;
        }
    }

    // Falsche-wiederholen-Button nur anzeigen, wenn falsche Fragen in der Session vorhanden sind.
    private void showRepeatSessionWrongQuestionsButton() {
        if (wrongQuestionsSessionPool.isEmpty()) {
            repeatSessionWrongQuestionsButton.setVisibility(View.GONE);
            wrongQuestionsAvailable = false;
            return;
        }

        repeatSessionWrongQuestionsButton.setText("Falsche wiederholen (" + wrongQuestionsSessionPool.size() + ")");
        repeatSessionWrongQuestionsButton.setVisibility(View.VISIBLE);
        wrongQuestionsAvailable = true;
    }

    // Neue Runde im aktuellen Quizmodus starten.
    private void newRoundButtonBehavior() {
        if (quizMode.contains(QUIZ_MODE_AP1)) {
            startAP1Round();
            return;
        }

        startLf20QuestionsRound();
    }

    // Falsch beantwortete Fragen der aktuellen Quizsession als eigene Wiederholungsrunde starten.
    private void repeatSessionWrongQuestionsButtonBehavior() {
        List<QuizQuestion> repeatWrongQuestionsSessionRound = new ArrayList<>(wrongQuestionsSessionPool);

        wrongQuestionsSessionPool.clear();

        Collections.shuffle(repeatWrongQuestionsSessionRound);

        quizEngine = new QuizEngine(repeatWrongQuestionsSessionRound);
        showCurrentQuestion();
    }

    // REFACTOR PRIO 1: Dialog-Erzeugung ist ähnlich wie in anderen Screens und könnte später ausgelagert werden.
    private void showDialogConfirmationQuiz(String popUpMessage) {
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
            leftConfirmationButton.setText("Hauptmenü");
            rightConfirmationButton.setText("Weiterlernen");

            leftConfirmationButton.setOnClickListener(onClick -> {
                if (isResultScreen) {
                    // Resultscreen-Zustand zurücksetzen, bevor ins Hauptmenü gewechselt wird.
                    isResultScreen = false;

                    quizEngine = null;
                    selectedAnswerButton = null;

                    resultText.setVisibility(View.GONE);

                    lf01QuizSessionPool.clear();
                    lf02QuizSessionPool.clear();
                    lf03QuizSessionPool.clear();
                    lf04QuizSessionPool.clear();
                    lf05QuizSessionPool.clear();
                    lf06QuizSessionPool.clear();

                    lf20QuestionsSessionPools.clear();
                }

                backToMainMenu();
                dialogConfirmationWindow.dismiss();
            });

            rightConfirmationButton.setOnClickListener(onClick -> dialogConfirmationWindow.dismiss());
        } else {
            leftConfirmationButton.setText("Weiterlernen");
            rightConfirmationButton.setText("Hauptmenü");

            leftConfirmationButton.setOnClickListener(onClick -> dialogConfirmationWindow.dismiss());

            rightConfirmationButton.setOnClickListener(onClick -> {
                if (isResultScreen) {
                    // Resultscreen-Zustand zurücksetzen, bevor ins Hauptmenü gewechselt wird.
                    isResultScreen = false;

                    quizEngine = null;
                    selectedAnswerButton = null;

                    resultText.setVisibility(View.GONE);

                    lf01QuizSessionPool.clear();
                    lf02QuizSessionPool.clear();
                    lf03QuizSessionPool.clear();
                    lf04QuizSessionPool.clear();
                    lf05QuizSessionPool.clear();
                    lf06QuizSessionPool.clear();
                    lf20QuestionsSessionPools.clear();
                }

                backToMainMenu();
                dialogConfirmationWindow.dismiss();
            });
        }

        dialogConfirmationWindow.show();
    }

    // Aus dem Quiz zurück ins Hauptmenü wechseln und vorhandene MainActivity wiederverwenden.
    private void backToMainMenu() {
        Intent mainMenu = new Intent(this, MainActivity.class);

        // CLEAR_TOP entfernt Activities über MainActivity, SINGLE_TOP nutzt eine vorhandene MainActivity wieder.
        mainMenu.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(mainMenu);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
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

    // Aktuelle Fragen-ID ins Clipboard kopieren und kurzzeitig UI-Feedback anzeigen.
    private void copyCurrentQuestionId() {
        if (currentQuestionId == null || currentQuestionId.isEmpty()) {
            return;
        }

        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("Fragen-ID", currentQuestionId);

        clipboardManager.setPrimaryClip(clipData);

        questionIdText.setText(QUESTION_ID_COPIED_TEXT);

        String copiedId = currentQuestionId;

        questionIdText.postDelayed(() -> {
            // Nur zurücksetzen, wenn währenddessen keine neue Frage geladen wurde.
            if (copiedId.equals(currentQuestionId)) {
                questionIdText.setText(QUESTION_ID_PREFIX + currentQuestionId);
            }
        }, COPY_FEEDBACK_MILLISECONDS);
    }

    // Primären Buttonstil setzen.
    private void setButtonPrimary(Button button) {
        button.setBackgroundResource(R.drawable.button_primary_bg);
        button.setTextColor(getColor(R.color.button_text));
    }

    // Antwortbutton in Normalzustand zurücksetzen.
    private void setAnswerButtonNormal(Button button) {
        button.setBackgroundResource(R.drawable.button_answer_bg);
        button.setTextColor(getColor(R.color.button_text));
        button.setAlpha(ENABLED_BUTTON_ALPHA);
        button.setEnabled(true);
        button.setClickable(true);
    }

    // Antwortbutton als ausgewählt markieren.
    private void setAnswerButtonSelected(Button button) {
        button.setBackgroundResource(R.drawable.button_answer_selected_bg);
        button.setTextColor(getColor(R.color.button_text));
        button.setAlpha(ENABLED_BUTTON_ALPHA);
    }

    // Antwortbutton als korrekt markieren.
    private void setAnswerButtonCorrect(Button button) {
        button.setBackgroundResource(R.drawable.button_answer_correct_bg);
        button.setTextColor(getColor(R.color.button_text));
        button.setAlpha(ENABLED_BUTTON_ALPHA);
    }

    // Antwortbutton als falsch markieren.
    private void setAnswerButtonWrong(Button button) {
        button.setBackgroundResource(R.drawable.button_answer_wrong_bg);
        button.setTextColor(getColor(R.color.button_text));
        button.setAlpha(ENABLED_BUTTON_ALPHA);
    }

    // Zahlen zweistellig formatieren, z. B. 1 -> 01.
    private String twoDigit(int value) {
        return String.format("%02d", value);
    }

}
