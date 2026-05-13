package app.projectfiqaandroid.ui;

// Da R in app.projectfiqaandroid liegt nicht in app.projectfiqaandroid.ui muss hier importiert werden
import app.projectfiqaandroid.R;

// Enthält Speicherzustand.
import android.os.Bundle;

// Basisklasse für Activities.
import androidx.appcompat.app.AppCompatActivity;

// Für Listen
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// InputStream import
import java.io.InputStream;

// View imports
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.View;

// Bitmap imports
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

// Button stuff
import android.widget.Button;

// IOException für Error Handling
import java.io.IOException;

// core imports
import app.projectfiqaandroid.core.*;

// io imports
import app.projectfiqaandroid.io.*;



// Von AppCompatActivity erben damit die Klasse eine Activity wird.
public class QuizActivity extends AppCompatActivity {

    // Quiz Logik Feld festlegen
    private QuizEngine engine;

    // UI Zustände
    // Standard Quiz gewählte Antwort Zustand
    private QuizAnswer inputAnswer = null;

    // Auswertungsmodus ja / nein Zustand
    private boolean evaluationMode = false;

    // UI Referenzen
    private TextView questionCounterText;
    private TextView questionIdText;
    private LinearLayout questionBox;
    private LinearLayout questionTextBox;
    private TextView questionText;
    private FrameLayout questionImageBox;
    private ImageView questionImage;
    private LinearLayout interactionBox;
    private LinearLayout answersBox;
    private LinearLayout evaluationBox;
    private LinearLayout actionBox;
    private Button actionButton;
    private Button newRoundButton;
    private Button mainMenuButton;

    // UI Hilfliste Dynamisch erzeugte Antwort Buttons
    private List<Button> answerButtons = new ArrayList<>();

    // Ergebnistext Referenz
    private TextView resultText = null;



    // Überschreibe Methode der Oberklasse
    @Override

    // onCreate wird automatisch von Android bei Start aufgerufen. Initialisiert interne Android Dinge.
    // Methodenaufrufe und deren Reihenfolge
    // "Orchestriert"
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout setzen
        setContentView(R.layout.activity_quiz);

        // Zurück Taste blockieren
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed(){
            }
        });

        // UI initialisieren
        initViews();

        // Quiz Daten bereitstellen
        loadEngine();

        // Action Button ergeugen und Action Box zuweisen
        createActionButton();

        // Action Button Verhalten initialisieren
        setupActionButton();

        // Aktuelle Frage anzeigen
        showCurrentQuestion();


    }

    // UI Views initialisieren Methode
    // Aufbau "Welche Views gibt es und woher diese?".
    private void initViews () {

        // Views greifen (Referenzenen auf UI Elemente aus dem XML holen)
        // findViewById ist quasi Android equivalent zu new VBox / new Button etc.
        questionCounterText = findViewById(R.id.questionCounterText);
        questionIdText = findViewById(R.id.questionIdText);
        questionBox = findViewById(R.id.questionBox);
        questionTextBox = findViewById(R.id.questionTextBox);
        questionText = findViewById(R.id.questionText);
        questionImageBox = findViewById(R.id.questionImageBox);
        questionImage = findViewById(R.id.questionImage);
        interactionBox = findViewById(R.id.interactionBox);
        answersBox = findViewById(R.id.answersBox);
        evaluationBox = findViewById(R.id.evaluationBox);

        // Action Button Container
        actionBox = findViewById(R.id.actionBox);

    }

    // Quiz Engine Laden Methode
    private void loadEngine() {

        // Absichern gegen Exceptions (hier IOException)
        try {
            // Fragen aus JSON laden und
            QuizLoader loader = new QuizLoader(getAssets());

            // FragenListe erzeugen
            List<QuizQuestion> questions = loader.load("questions/lf09.json");

            // QuizEngine mit Fragenliste erzeugen
            engine = new QuizEngine(questions);

        // IOException abfangen
        } catch (IOException error) {

            // App Crash bei error
            throw new RuntimeException(error);

        }

    }

    private void createActionButton() {

        // Button erstellen und verstecken bis Antwort gewählt
        actionButton = new Button(this, null, 0, R.style.ActionButton);
        actionButton.setText("Bestätige Antwort");
        actionButton.setVisibility(View.GONE);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                dpToPx(220),
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        buttonParams.gravity = Gravity.CENTER;

        actionBox.removeAllViews();
        actionBox.addView(actionButton, buttonParams);

    }

    // Action Button Verhalten / Zustande
    private void setupActionButton() {

        // Wenn Action Button gedrückt
        actionButton.setOnClickListener(onClick -> {

            // Nichts tun bei keiner Antwort Auswahl
            if (inputAnswer == null) {
                return;
            }

            // Wenn Antwort ausgewählt und nicht im Auswertungsmodus
            if (!evaluationMode) {

                showEvaluation();

                // Im Auswertungsmodus, Nächste Frage oder Quiz bewerten.
            } else {

                // Wenn letzte Frage
                if (engine.isLastQuestion()) {
                    // Zeige Gesamtergebnis
                    showResult();

                    // Ansonsten lade und zeige nächste Frage
                } else {
                    engine.nextQuestion();
                    showCurrentQuestion();
                }

            }

        });

    }

    // Methode um aktuelle Frage anzuzeigen und UI Zustände zu ändern
    private void showCurrentQuestion(){

        // Neue Runde und Hauptmenübutton aus Endergebnis Bildschirm ausblenden
        if (newRoundButton != null) {
            newRoundButton.setVisibility(View.GONE);
            mainMenuButton.setVisibility(View.GONE);
        }

        if (resultText != null) {
            // Fragen Box und Ergebnistext entfernen
            questionBox.removeView(resultText);
            resultText = null;
        }

        // Satusbartext an
        questionCounterText.setVisibility(View.VISIBLE);
        questionIdText.setVisibility(View.VISIBLE);

        // Fragentext an;
        questionBox.setGravity(Gravity.CENTER_HORIZONTAL);
        questionTextBox.setVisibility(View.VISIBLE);
        questionText.setVisibility(View.VISIBLE);
        questionImageBox.setVisibility(View.GONE);
        questionImage.setVisibility(View.GONE);
        interactionBox.setVisibility(View.VISIBLE);
        answersBox.setVisibility(View.VISIBLE);
        evaluationBox.setVisibility(View.GONE);

        // Erste Frage ziehen (Index 0 in Fragenliste)
        QuizQuestion question = engine.getCurrentQuestion();

        // Statusbar Werte Frage x/y aktualisieren
        int currentIndex = engine.getCurrentIndex() + 1;
        int totalQuestions = engine.getQuestionCount();

        // Statusbar Frage x/y anzeigen
        questionCounterText.setText("Frage " + twoDigit(currentIndex) + "/" + twoDigit(totalQuestions));

        // Statusbar Frage ID anzeigen
        questionIdText.setText(engine.getCurrentQuestion().getId());

        // Zustände zurücksetzen
        inputAnswer = null;
        evaluationMode = false;
        actionButton.setText("Bestätige Antwort");
        actionButton.setVisibility(View.GONE);

        // Android nutzt View weil 3 Zustände (IN)VISIBLE und GONE
        evaluationBox.setVisibility(View.GONE);

        // Antwort Box wieder anzeigen Antwort Buttons zurücksetzen
        answersBox.removeAllViews();
        answersBox.setVisibility(View.VISIBLE);

        answerButtons.clear();

        // Text anzeigen
        questionText.setText(question.getQuestion());

        // Bild anzeigen falls vorhanden.
        if (question.getImage() != null && !question.getImage().isEmpty()) {

            // Absichern gegen Exceptions (hier IOException)
            try {

                InputStream imageStream = getAssets().open("questions/" + question.getImage());

                // Bitmap als Android-Standard. Hohe Kompatibilität mit verschiedenen Geräten.
                Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                questionImage.setImageBitmap(bitmap);

                // Bild anzeigen
                questionImageBox.setVisibility(View.VISIBLE);
                questionImage.setVisibility(View.VISIBLE);

            // IOException abfangen
            } catch (IOException error) {

                // App Crash bei error
                throw new RuntimeException(error);

            }

        } else {

            // Ohne Bikld Text mittig zentriert
            questionBox.setGravity(Gravity.CENTER);

        }

        if (question.getAnswers().size() > 2) {
            Collections.shuffle(question.getAnswers());
        }

        // Dynamische Antwort-Buttons
        for (QuizAnswer answer : question.getAnswers()) {

            // Button erzeugen
            Button answerButton = new Button(this, null, 0, R.style.ActionButton);

            // Antworttext zuweisen
            answerButton.setText(answer.getText());

            // Antwort Button Alpha setzen
            answerButton.setAlpha(1.0f);

            // Button merken
            answerButtons.add(answerButton);

            // Wenn geklickt
            answerButton.setOnClickListener(onClick -> {

                // Ausgewählten Button merken
                inputAnswer = answer;
                // Zeige Action Button an
                actionButton.setVisibility(View.VISIBLE);

                // Für jeden Button in AntwortButtons
                for (Button button : answerButtons) {

                    // Setze Buttons visuell auf halbe Transparenz (0.5f)
                    button.setAlpha(0.5f);

                }

                // Ausgewählten Button hervorheben mit keiner Transparenz (1.0f)
                answerButton.setAlpha(1.0f);

            });

            answersBox.addView(answerButton);

        }

    }

    private void showEvaluation() {

        // Wechsle in den Auswertungsmodus
        evaluationMode = true;

        // AntwortBox weg und AuswertungsBox aktiv
        answersBox.setVisibility(View.GONE);
        evaluationBox.setVisibility(View.VISIBLE);

        // Views aus dem XML holen (NICHT neu erzeugen!)
        TextView resultTextView = findViewById(R.id.resultText);
        TextView explanationTextView = findViewById(R.id.explanationText);

        // Ist Eingabe richtig? true oder false
        boolean correct = inputAnswer.isCorrect();
        // Setze Antwort auf true oder false
        engine.inputAnswer(correct);

        // Ausgabetexte
        resultTextView.setText(correct ? "+ RICHTIG +" : "- FALSCH -");
        explanationTextView.setText(
                engine.getCurrentQuestion().getExplanation()
        );

        // Wenn Frage die letzte Frage war
        if (engine.isLastQuestion()) {

            // Dann setze Action Button Text auf
            actionButton.setText("Quiz auswerten");

        } else {

            // Ansonsten auf
            actionButton.setText("Nächste Frage");

        }

    }

    // Endergebnis Methode
    private void showResult() {

        // Quiz UI ausblenden
        actionButton.setVisibility(View.GONE);
        answersBox.setVisibility(View.GONE);
        evaluationBox.setVisibility(View.GONE);
        questionCounterText.setVisibility(View.GONE);
        questionIdText.setVisibility(View.GONE);
        questionTextBox.setVisibility(View.GONE);
        questionText.setVisibility(View.GONE);
        questionImageBox.setVisibility(View.GONE);
        questionImage.setVisibility(View.GONE);

        // Endergebnis Text erzeugen
        resultText = new TextView(this);
        resultText.setText("Ergebnis:\n" + twoDigit(engine.getScore()) + "/" + twoDigit(engine.getQuestionCount()));
        resultText.setTextSize(28f);
        resultText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        resultText.setGravity(Gravity.CENTER);

        // Fragebox anzeigen
        questionBox.setVisibility(View.VISIBLE);

        // Ergebnis in FragenBox anzeigen
        questionBox.addView(resultText);
        questionBox.setGravity(Gravity.CENTER);

        // Check ob Buttons schon vorhanden
        if (newRoundButton == null) {

            // LayoutParameter für "Neue Runde" und "Hauptmenü" Button
            // LayoutParameter arbeiten in Pixel nicht in dp
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(

                    // Umrechnen von dp zu px
                    dpToPx(220),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            // Neue Runde Button
            newRoundButton = new Button(this, null, 0, R.style.ActionButton);
            newRoundButton.setText("Neue Runde");

            // Beim Drücken neue Runde starten
            newRoundButton.setOnClickListener(onClick -> {

                loadEngine();
                showCurrentQuestion();

            });

            // Hauptmenü Button
            mainMenuButton = new Button(this, null, 0, R.style.ActionButton);
            mainMenuButton.setText("Hauptmenü");

            // Beim Drücken zurück zum Hauptmenü
            mainMenuButton.setOnClickListener(onClick -> {

                finish();

            });

            // In Interaktionsbox Buttons zentriert anzeigen mit Button Parametern
            buttonParams.gravity = Gravity.CENTER;
            interactionBox.addView(newRoundButton, buttonParams);
            interactionBox.addView(mainMenuButton, buttonParams);

        }

        // Buttons sichtbar machen
        newRoundButton.setVisibility(View.VISIBLE);
        mainMenuButton.setVisibility(View.VISIBLE);


    }

    // Hilfmethode um auf 2 Zeichen zu erhöhen, sprich z.B. Frage 01/20 nicht 1/20
    private String twoDigit(int value) {
        return String.format("%02d", value);
    }

    // Hilfsmethode zum Umrechnen von dp zu px
    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }





}
