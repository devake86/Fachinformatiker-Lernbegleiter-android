package app.projectfiqa.ui;

// Da R in app.projectfiqa liegt nicht in app.projectfiqa.ui muss hier importiert werden
import app.projectfiqa.R;

// Enthält Speicherzustand.
import android.os.Bundle;

// Basisklasse für Activities.
import androidx.appcompat.app.AppCompatActivity;

// Für Listen
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// View imports
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

// Button stuff
import android.widget.Button;

// IOException für Error Handling
import java.io.IOException;

// core imports
import app.projectfiqa.core.*;

// io imports
import app.projectfiqa.io.*;

// Antwort Button Tints
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.core.view.ViewCompat;


// Von AppCompatActivity erben damit die Klasse eine Activity wird.
public class QuizActivity extends AppCompatActivity {

    // Klasse als Feld zur späteren Verwendung in Methoden.
    private QuizEngine quizEngine;
    private QuizLoader quizLoader;

    // XML
    private TextView questionCounterText;
    private TextView quizModeText;
    private TextView questionIdText;

    private LinearLayout questionBox;
    private LinearLayout questionTextBox;
    private TextView questionText;
    private TextView resultText;

    private LinearLayout explanationBox;
    private TextView explanationText;

    private LinearLayout interactionBox;
    private LinearLayout answersBox;

    private LinearLayout actionBox;
    private Button actionButton;
    private Button newRoundButton;
    private Button menuButton;



    // Bestätigen gedrückt?
    private boolean confirmed = false;

    // Ausgewählter Antwort-Button
    private Button selectedAnswerButton;

    // Richtige Antwort Button
    private Button correctAnswerButton;

    // Alle Antwort Buttons der aktuellen Frage (zum gleichzeitigen abdunkeln bei Auswahl)
    private List<Button> answerButtons = new ArrayList<>();

    // Fragenpoolgröße für 20 Questions Modus
    private int lf20QuestionsSize = 20;

    // 20 Questions Liste von Session-Fragenpools
    private List<List<QuizQuestion>> lf20QuestionsSessionPools = new ArrayList<>();

    // Quiz Pools zum Zusammenführen von einzelnen Lernfeld JSONs für AP1
    List<QuizQuestion> lf01quizSessionPool = new ArrayList<>();
    List<QuizQuestion> lf02quizSessionPool = new ArrayList<>();
    List<QuizQuestion> lf03quizSessionPool = new ArrayList<>();
    List<QuizQuestion> lf04quizSessionPool = new ArrayList<>();
    List<QuizQuestion> lf05quizSessionPool = new ArrayList<>();
    List<QuizQuestion> lf06quizSessionPool = new ArrayList<>();

    // AP1 Quiz Runde
    List<QuizQuestion> ap1QuizRound = new ArrayList<>();

    // AP1 Gewichtung
    private int questionMultiplierAP1 = 3;

    private int takeQuestionsLF01 = 1 * questionMultiplierAP1;
    private int takeQuestionsLF02 = 3 * questionMultiplierAP1;
    private int takeQuestionsLF03 = 3 * questionMultiplierAP1;
    private int takeQuestionsLF04 = 1 * questionMultiplierAP1;
    private int takeQuestionsLF05 = 3 * questionMultiplierAP1;
    private int takeQuestionsLF06 = 1 * questionMultiplierAP1;

    // Quiz String + Setter
    private String[] jsonList = {"questions/lf-test.json"};

    public void setJsonList(String[] jsonList) {
        this.jsonList = jsonList;
    }

    // Quiz Mode + Setter
    private String quizMode = "20 Fragen";

    public void setQuizMode(String quizMode) {
        this.quizMode = quizMode;
    }

    // Hilfsmethode um auf 2 Zeichen zu erhöhen, sprich z.B Frage 01/20 nicht 1/20
    private String twoDigit(int value) {
        return String.format("%02d", value);
    }

    // Hilfsmethode JSON laden mit IOException
    private List<QuizQuestion> loadJsonWithIOException(String jsonPath) {
        try {
            return quizLoader.load(jsonPath);
        } catch (IOException error) {
            return new ArrayList<>();
        }
    }

    // Hilfsmethoden für Antwort Button Tints
    // Alte Farbe speichern
    private void saveOriginalButtonColor(Button button) {
        button.setTag(R.id.tag_original_tint, ViewCompat.getBackgroundTintList(button));
        button.setTag(R.id.tag_original_text_colors, button.getTextColors());

    }

    // Alte Farbe zurücksetzen
    private void resetOriginalButtonColor(Button button) {
        ColorStateList buttonTint = (ColorStateList) button.getTag(R.id.tag_original_tint);
        ViewCompat.setBackgroundTintList(button, buttonTint);

        ColorStateList buttonText = (ColorStateList) button.getTag(R.id.tag_original_text_colors);
        if (buttonText != null) button.setTextColor(buttonText);

    }

    // Button einfärben
    private void tintButton(Button button, String hexColorCode) {
        ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(Color.parseColor(hexColorCode)));
        button.setTextColor(Color.WHITE);

    }


    // Überschreibe Methode der Oberklasse
    @Override

    // onCreate wird automatisch von Android bei Start aufgerufen. Initialisiert interne Android Dinge.
    // Methodenaufrufe und deren Reihenfolge
    // "Orchestriert"
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout setzen
        setContentView(R.layout.quiz_activity);

        quizMode = getIntent().getStringExtra("quizMode");
        if (quizMode == null) quizMode = "20 Fragen - LF-Test";

        // Zurück Taste blockieren
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed(){
            }
        });

        // Views initialisieren
        initViews();

        // UI initialisieren
        initUI();

        // Button Verhalten initialisieren
        initButtonBehavior();

        // Quiz Loader initialisieren
        quizLoader = new QuizLoader(getAssets());

        // Quiz Mode initialisieren
        initQuizMode();

    }

    // Views initialisieren Methode
    // Aufbau "Welche Views gibt es und woher diese?".
    private void initViews () {

        // Views greifen (Referenzenen auf UI Elemente aus dem XML holen)
        // findViewById ist quasi Android equivalent zu new VBox / new Button etc.
        // Status Bar
        questionCounterText = findViewById(R.id.questionCounterText);
        quizModeText = findViewById(R.id.quizModeText);
        questionIdText = findViewById(R.id.questionIdText);

        // Fragenanzeige
        questionBox = findViewById(R.id.questionBox);
        questionTextBox = findViewById(R.id.questionTextBox);
        questionText = findViewById(R.id.questionText);
        resultText = findViewById(R.id.resultText);

        explanationBox = findViewById(R.id.explanationBox);
        explanationText = findViewById(R.id.explanationText);

        // Ineraktionsbereich
        interactionBox = findViewById(R.id.interactionBox);
        answersBox = findViewById(R.id.answersBox);

        newRoundButton = findViewById(R.id.newRoundButton);
        menuButton = findViewById(R.id.menuButton);

        // Action Button Bereich
        actionButton = findViewById(R.id.actionButton);

    }

    // UI initialisieren
    private void initUI() {

        // Statusbar
        questionCounterText.setVisibility(View.GONE);
        quizModeText.setVisibility(View.GONE);
        questionIdText.setVisibility(View.GONE);

        // Fragenanzeige
        questionText.setVisibility(View.GONE);
        resultText.setVisibility(View.GONE);
        explanationBox.setVisibility(View.INVISIBLE);

        // Ineraktionsbereich
        newRoundButton.setVisibility(View.GONE);
        menuButton.setVisibility(View.GONE);

        // Action Button Bereich
        actionButton.setVisibility(View.INVISIBLE);
        actionButton.setText("Bestätigen");

    }

    // Action Button Verhalten initialisieren
    private void initButtonBehavior(){
        actionButton.setOnClickListener(onClick -> actionButtonBehavior());
        newRoundButton.setOnClickListener(onClick -> newRoundButtonBehavior());
        menuButton.setOnClickListener(onClick -> menuButtonBehavior());

    }


    // Quiz Mode auswählen
    private void initQuizMode() {
        if ("AP1 PoC".equals(quizMode)) {
            setQuizMode("AP1 PoC");
            setJsonList(new String[]{
                    "questions/lf-01.json",
                    "questions/lf-02.json",
                    "questions/lf-03.json",
                    "questions/lf-04.json",
                    "questions/lf-05.json",
                    "questions/lf-06.json"
            });
            setupAP1();
        } else if ("20 Fragen - LF-VT1".equals(quizMode)) {
            setQuizMode("20 Fragen - LF-VT1");
            setJsonList(new String[]{
                    "questions/lf-vt1/lf-vt1-abkz.json",
                    "questions/lf-vt1/lf-vt1-apwb.json",
                    "questions/lf-vt1/lf-vt1-bumi.json",
                    "questions/lf-vt1/lf-vt1-edak.json",
                    "questions/lf-vt1/lf-vt1-fbeg.json",
                    "questions/lf-vt1/lf-vt1-ksdb.json",
                    "questions/lf-vt1/lf-vt1-navw.json",
                    "questions/lf-vt1/lf-vt1-netz.json",
                    "questions/lf-vt1/lf-vt1-spdv.json",
                    "questions/lf-vt1/lf-vt1-trup.json",
            });
            setupLf20Questions();



        // Extra Modi für beta2
        } else if ("20 Fragen - ABKZ".equals(quizMode)) {
            setQuizMode("20 Fragen - ABKZ");
            setJsonList(new String[]{
                    "questions/lf-vt1/lf-vt1-abkz.json",

            });
            setupLf20Questions();

        } else if ("20 Fragen - FBEG".equals(quizMode)) {
            setQuizMode("20 Fragen - FBEG");
            setJsonList(new String[]{
                    "questions/lf-vt1/lf-vt1-fbeg.json",

            });
            setupLf20Questions();







        }
    }

    // Action Button Verhalten (Falsche Antwort: Rot, Richtige Antwort: Grün und Textwechsel mit entsprechender Weiterleitung)
    private void actionButtonBehavior() {

        // Beim Drücken von Quiz auswerten oder Nächste Frage und confirmed true
        if (confirmed) {
            // Übergang zur nächsten Frage oder Gesamtauswertung
            if (quizEngine.isLastQuestion()) {
                showResult();

            } else {
                quizEngine.nextQuestion();
                showCurrentQuestion();

            }
            return;
        }

        // Wenn nichts ausgewählt
        if (selectedAnswerButton == null) {
            return;

        }

        // Antwort prüfen
        int selectedIndex = answerButtons.indexOf(selectedAnswerButton);
        boolean correct = quizEngine.checkAnswer(selectedIndex);

        // Wenn richtig erhöhe Punktzahl
        quizEngine.answerScore(correct);

        // Erstmal alle Button auf halbe Transparenz und ggf. Farben entfernen und blockieren
        for (Button answerButton : answerButtons) {
            answerButton.setAlpha(0.5f);
            answerButton.setEnabled(false);
            answerButton.setClickable(false);
        }

        // Ausgewählte Antwort hervorheben
        selectedAnswerButton.setAlpha(1.0f);



        // Richtige Antwort raussuchen
        // Bei -1 starten (OutOfBounds) nicht 0 damit index 0 beim Suchen der richtigen Antwort auch wirklich die richtige ist
        // Und Absicherung gegen Crashes falls Antworten falsch gepflegt
        int correctIndex = -1;

        List<QuizAnswer> answers = quizEngine.getCurrentQuestion().getAnswers();

        for (int answerIndex = 0; answerIndex < answers.size(); answerIndex++) {
            if (answers.get(answerIndex).isCorrect()) {
                correctIndex = answerIndex;
                break;
            }
        }

        // Wenn Index von richter Antwort gefunden färbe grün
        if (correctIndex != -1) {
            correctAnswerButton = answerButtons.get(correctIndex);

            tintButton(correctAnswerButton, "#388E3C");
            correctAnswerButton.setAlpha(1.0f);
        }

        // Gewählte Antwort falsch: Rot oder richtig: Grün
        if (correct) {
            // Richtige Antwort: grün
            tintButton(correctAnswerButton, "#388E3C");

        } else {
            // Falsche Antwort: rot
            tintButton(selectedAnswerButton, "#D32F2F");
        }

        // Ausgewählte Antwort keine Transparenz
        selectedAnswerButton.setAlpha(1.0f);

        // Erklärung anzeigen
        explanationText.setText(quizEngine.getCurrentQuestion().getExplanation());
        explanationBox.setVisibility(View.VISIBLE);

        // Button Quiz auswerten, wenn letzte Frage; ansonsten Nächste Frage
        if (quizEngine.isLastQuestion()) {
            actionButton.setText("Quiz auswerten");
        } else {
            actionButton.setText("Nächste Frage");
        }

        confirmed = true;

    }

    // Neue Runde Button Verhalten
    private void newRoundButtonBehavior() {

        if ("AP1 PoC".equals(quizMode)) {

            boolean hasEnoughQuestions = true;

            // Falls nicht genug Fragen für neue Runde
            if (lf01quizSessionPool.size() < takeQuestionsLF01) {
                // Nicht genug Fragen setzen
                hasEnoughQuestions = false;
            } else if (lf02quizSessionPool.size() < takeQuestionsLF02) {
                // Nicht genug Fragen setzen
                hasEnoughQuestions = false;
            } else if (lf03quizSessionPool.size() < takeQuestionsLF03) {
                // Nicht genug Fragen setzen
                hasEnoughQuestions = false;
            } else if (lf04quizSessionPool.size() < takeQuestionsLF04) {
                // Nicht genug Fragen setzen
                hasEnoughQuestions = false;
            } else if (lf05quizSessionPool.size() < takeQuestionsLF05) {
                // Nicht genug Fragen setzen
                hasEnoughQuestions = false;
            } else if (lf06quizSessionPool.size() < takeQuestionsLF06) {
                // Nicht genug Fragen setzen
                hasEnoughQuestions = false;
            }


            // Wenn nicht genug Fragen für Neue Runde
            if (!hasEnoughQuestions) {

                // Setze Text auf fertig und mache nichts
                newRoundButton.setText("v Fertig v");
                return;
            }

            // Ansonsten neue Runde AP1PoC starten
            startAP1Round();
            return;
        }

        int remainingQuestions = 0;

        for (List<QuizQuestion> lf20QuestionSessionPool : lf20QuestionsSessionPools) {
            remainingQuestions += lf20QuestionSessionPool.size();
        }

        // Falls Fragenpoolsgröße unter gewünschter Fragenpoolgröße
        if (remainingQuestions < lf20QuestionsSize) {

            // Setze Text auf fertig und mache nichts
            newRoundButton.setText("v Fertig v");
            return;
        }

        // Ansonsten neue Runde AP1PoC starten
        startLf20QuestionsRound();

    }

    // Hauptmenü Button Verhalten
    private void menuButtonBehavior() {

        // Zurück zum Hauptmenü
        finish();
        // Sanfter Übergang zur nächsten Activity
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

    }

    // Button Transparenz festlegen
    private void selectedAnswerButton(Button selectedButton) {

        // Alle Buttons abdunkeln
        for (Button button : answerButtons) {
            button.setAlpha(0.5f);

        }

        // Ausgewählten Button hervorheben
        selectedButton.setAlpha(1.0f);

        // Referenz merken
        selectedAnswerButton = selectedButton;

    }

    // AP1 Modus (JSONs Lernfeld 1-6)
    public void setupAP1(){

        // Fragenpools leeren
        lf01quizSessionPool.clear();
        lf02quizSessionPool.clear();
        lf03quizSessionPool.clear();
        lf04quizSessionPool.clear();
        lf05quizSessionPool.clear();
        lf06quizSessionPool.clear();

        // Für jede JSON in der JSON Liste
        for (String json : jsonList) {

            // Lade JSON und Liste Fragen
            List<QuizQuestion> quizQuestions = loadJsonWithIOException(json);

            // Unterthemen von Lernfeldern zusammenführen und in jeweiligen Lernfeldpool speichern.
            if (json.contains("lf-01")) {
                lf01quizSessionPool.addAll(quizQuestions);
            } else if (json.contains("lf-02")) {
                lf02quizSessionPool.addAll(quizQuestions);
            } else if (json.contains("lf-03")) {
                lf03quizSessionPool.addAll(quizQuestions);
            } else if (json.contains("lf-04")) {
                lf04quizSessionPool.addAll(quizQuestions);
            } else if (json.contains("lf-05")) {
                lf05quizSessionPool.addAll(quizQuestions);
            } else if (json.contains("lf-06")) {
                lf06quizSessionPool.addAll(quizQuestions);
            }

        }

        // Lernfeld Fragenpools mischen
        Collections.shuffle(lf01quizSessionPool);
        Collections.shuffle(lf02quizSessionPool);
        Collections.shuffle(lf03quizSessionPool);
        Collections.shuffle(lf04quizSessionPool);
        Collections.shuffle(lf05quizSessionPool);
        Collections.shuffle(lf06quizSessionPool);

        // Erste AP1 PoC Runde aus Session-Fragenpool starten
        startAP1Round();

    }

    // AP1 Gewichtung LF1-6 1:3:3:1:3:1 (Grundlage: Lernfeldwochen) *3 für 36 Fragen (vergleichbar mit großer LZK)
    private void startAP1Round() {

        ap1QuizRound.clear();

        // Check ob Fragenpool groß genug
        if (lf01quizSessionPool.size() >= takeQuestionsLF01) {
            // X Anzahl Fragen aus Quiz Pool der AP1 Quiz Runde zufügen
            ap1QuizRound.addAll(lf01quizSessionPool.subList(0, takeQuestionsLF01));
            // X Anzahl der zugefügten Fragen aus Pool entfernen
            lf01quizSessionPool.subList(0, takeQuestionsLF01).clear();
        }
        if (lf02quizSessionPool.size() >= takeQuestionsLF02) {
            ap1QuizRound.addAll(lf02quizSessionPool.subList(0, takeQuestionsLF02));
            lf02quizSessionPool.subList(0, takeQuestionsLF02).clear();
        }
        if (lf03quizSessionPool.size() >= takeQuestionsLF03) {
            ap1QuizRound.addAll(lf03quizSessionPool.subList(0, takeQuestionsLF03));
            lf03quizSessionPool.subList(0, takeQuestionsLF03).clear();
        }
        if (lf04quizSessionPool.size() >= takeQuestionsLF04) {
            ap1QuizRound.addAll(lf04quizSessionPool.subList(0, takeQuestionsLF04));
            lf04quizSessionPool.subList(0, takeQuestionsLF04).clear();
        }
        if (lf05quizSessionPool.size() >= takeQuestionsLF05) {
            ap1QuizRound.addAll(lf05quizSessionPool.subList(0, takeQuestionsLF05));
            lf05quizSessionPool.subList(0, takeQuestionsLF05).clear();
        }
        if (lf06quizSessionPool.size() >= takeQuestionsLF06) {
            ap1QuizRound.addAll(lf06quizSessionPool.subList(0, takeQuestionsLF06));
            lf06quizSessionPool.subList(0, takeQuestionsLF06).clear();
        }

        // Gezogene Rundenfragen mischen
        Collections.shuffle(ap1QuizRound);

        quizEngine = new QuizEngine(ap1QuizRound);


        showCurrentQuestion();

    }

    // LF 20 Questions Modus Fragenpool vorbereiten
    public void setupLf20Questions(){

        // Fragenpool leeren
        lf20QuestionsSessionPools.clear();

        // Für jede JSON in der JSON Liste
        for (String json : jsonList) {

            // Lade JSON und Liste Fragen
            List<QuizQuestion> quizQuestions = loadJsonWithIOException(json);

            // Solange es noch Fragen gibt
            if (!quizQuestions.isEmpty()) {

                // Mische die Liste
                Collections.shuffle(quizQuestions);

                // Packe die gemischte Liste in den Fragenpool
                lf20QuestionsSessionPools.add(new ArrayList<>(quizQuestions));

            }

        }

        // Fragenlistenpool nochmal mischen damit die erste nicht immer an erster Stelle
        Collections.shuffle(lf20QuestionsSessionPools);

        // Erste AP1 PoC Runde aus Session-Fragenpool starten
        startLf20QuestionsRound();

    }

    private void startLf20QuestionsRound() {

        List<QuizQuestion> lf20QuestionsQuizRound = new ArrayList<>();

        // Solang Größe der Fragenrunde kleiner gewünschter Fragenpoolgröße
        while (lf20QuestionsQuizRound.size() < lf20QuestionsSize) {


            boolean stillHasQuestions = false;

            // Quizrunde befüllen "Round Robin"-Style bis Fragepool voll
            for (List<QuizQuestion> lf20QuestionsSessionPool : lf20QuestionsSessionPools) {

                // Wenn die Runde voll ist, ziehe keine weiteren Fragen mehr
                if (lf20QuestionsQuizRound.size() >= lf20QuestionsSize) {
                    break;
                }

                // Wenn Fragenpool nicht leer
                if (!lf20QuestionsSessionPool.isEmpty()) {
                    // Ziehe Frage aus index 0 des Fragenpools, entferne diese aus dem Fragenpool und packe sie in Quizrunde
                    lf20QuestionsQuizRound.add(lf20QuestionsSessionPool.remove(0));
                    stillHasQuestions = true;
                }
            }

            // Wenn nicht genug Fragen für Runde dann abbrechen
            if (!stillHasQuestions) {
                break;
            }

        }

        // Gezogene Rundenfragen mischen
        Collections.shuffle(lf20QuestionsQuizRound);

        quizEngine = new QuizEngine(lf20QuestionsQuizRound);


        showCurrentQuestion();

    }



    private void showCurrentQuestion() {

        answersBox.setVisibility(View.VISIBLE);

        // Statusbartext anzeigen
        questionCounterText.setVisibility(View.VISIBLE);
        quizModeText.setVisibility(View.VISIBLE);
        questionIdText.setVisibility(View.VISIBLE);

        // Ergebnistext ausblenden
        resultText.setVisibility(View.GONE);

        // Ergebnis Screen Buttons ausschalten
        newRoundButton.setVisibility(View.GONE);
        menuButton.setVisibility(View.GONE);

        // Frage anzeigen
        questionText.setVisibility(View.VISIBLE);

        // Erklärung ausblenden aber Platz reservieren
        explanationBox.setVisibility(View.INVISIBLE);

        // Antwort Buttons zurücksetzen
        for (Button answerButton : answerButtons) {
            resetOriginalButtonColor(answerButton);
        }
        answersBox.removeAllViews();
        answerButtons.clear();

        // Rücksetzen auf Antwortphase.
        selectedAnswerButton = null;
        correctAnswerButton = null;
        confirmed = false;

        // Action Button verstecken und benennen
        actionButton.setVisibility(View.INVISIBLE);
        actionButton.setText("Bestätigen");

        // Aktueller Fragen Index und Gesamtzahl Fragenpool für Statusbar Anzeige
        int currentIndex = quizEngine.getCurrentIndex() + 1;
        int totalQuestions = quizEngine.getQuestionCount();

        // Index und Gesamtzahl zusammensetzen Format z.B. Frage 01/20
        questionCounterText.setText("Frage " + twoDigit(currentIndex) + "/" + twoDigit(totalQuestions));

        // Quiz Mode Anzeige
        quizModeText.setText(quizMode);

        // Fragen ID holen
        questionIdText.setText(quizEngine.getCurrentQuestion().getId());

        // Aktuelle Frage holen.
        QuizQuestion question = quizEngine.getCurrentQuestion();

        // Fragetext erzeugen.
        questionText.setText(question.getQuestion());

        // Erklärungstext erzeugen.
        explanationText.setText(question.getExplanation());

        if (question.getAnswers().size() > 2) {
            Collections.shuffle(question.getAnswers());
        }

        // Antwort-Buttons dynamisch (2-4) erzeugen
        // Für Antworten aus Antwortenpool
        for (QuizAnswer answer : question.getAnswers()) {

            // Erzeuge Button pro Antwort
            Button answerButton = new Button(new ContextThemeWrapper(this, R.style.AnswerButton));
            answerButton.setText(answer.getText());
            answerButton.setAlpha(1.0f);

            // Antwortbuttons Tag um später nur die Buttons zu löschen
            answerButton.setTag("answerButton");

            // Button Farben speichern
            saveOriginalButtonColor(answerButton);

            answerButtons.add(answerButton);

            answerButton.setOnClickListener(onClick -> {

                // Methode für Button Transparenz aufrufen
                selectedAnswerButton(answerButton);

                // Action Button anzeigen
                actionButton.setVisibility(View.VISIBLE);
            });

            answersBox.addView(answerButton);

        }



    }



    // Gesamtergebnis Bildschirm
    private void showResult() {

        //Statusbartext ausblenden
        questionCounterText.setVisibility(View.GONE);
        quizModeText.setVisibility(View.VISIBLE);
        quizModeText.setText(quizMode);
        questionIdText.setVisibility(View.GONE);

        // Action Button ausblenden
        actionButton.setVisibility(View.INVISIBLE);

        // Fragen ausblenden
        questionText.setVisibility(View.GONE);

        // Erklärung ausblenden
        explanationBox.setVisibility(View.INVISIBLE);

        // Gesamtergebnis Text anzeigen und festlegen
        resultText.setVisibility(View.VISIBLE);
        resultText.setText("Ergebnis: " + twoDigit(quizEngine.getScore()) + "/" + twoDigit(quizEngine.getQuestionCount()));

        // AntwortBox leeren und ausblenden
        answersBox.removeAllViews();
        answerButtons.clear();
        answersBox.setVisibility(View.GONE);

        // Neue Runde und Hauptmenü Button
        newRoundButton.setVisibility(View.VISIBLE);
        menuButton.setVisibility(View.VISIBLE);

    }






















}
