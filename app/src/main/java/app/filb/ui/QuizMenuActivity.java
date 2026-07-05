package app.filb.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import app.filb.R;

public class QuizMenuActivity extends AppCompatActivity {
    private static final String QUIZ_MODE_AP1 = "AP1";
    private static final String QUIZ_MODE_LF = "LF";

    private static final String INTENT_EXTRA_QUIZ_MODE = "quizMode";

    private static final String TITLE_AP1 = "Abschlussprüfung";
    private static final String DESCRIPTION_AP1 = "\n\nEine Fragenmischung über mehrere Lernfelder.";

    private static final String TITLE_LF = "Lernfeld-Vertiefung";
    private static final String DESCRIPTION_LF = "\n\nEine gezielte Vertiefung eines einzelnen Lernfeldes.";

    private static final String QUIZ_MENU_TEXT_SPACER = "\n\n\n";
    private static final float TITLE_RELATIVE_SIZE = 1.3f;

    // XML: Texte
    private TextView statusBarText;
    private TextView lfTitleText;
    private TextView flavorText;

    // XML: Auswahlbuttons
    private Button ap1Button;
    private Button start20QuestionsButton;

    // XML: Action Button
    private Button actionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.quiz_menu_activity);

        initViews();
        initUI();
        initButtonBehavior();
    }

    // XML-Views holen und Feldern zuweisen.
    private void initViews() {
        statusBarText = findViewById(R.id.statusBarText);

        lfTitleText = findViewById(R.id.lfTitleText);
        flavorText = findViewById(R.id.flavorText);

        ap1Button = findViewById(R.id.ap1Button);
        start20QuestionsButton = findViewById(R.id.start20QuestionsButton);

        actionButton = findViewById(R.id.actionButton);
    }

    // Quiz-Auswahlmenü anzeigen.
    private void initUI() {
        statusBarText.setVisibility(View.VISIBLE);

        lfTitleText.setVisibility(View.VISIBLE);
        flavorText.setVisibility(View.VISIBLE);

        setTextFormatForTwoTitleAndDescription(
                TITLE_AP1,
                DESCRIPTION_AP1,
                TITLE_LF,
                DESCRIPTION_LF
        );

        ap1Button.setVisibility(View.VISIBLE);
        start20QuestionsButton.setVisibility(View.VISIBLE);

        actionButton.setVisibility(View.VISIBLE);
    }

    private void initButtonBehavior() {
        ap1Button.setOnClickListener(onClick -> loadContentMenu(QUIZ_MODE_AP1));
        start20QuestionsButton.setOnClickListener(onClick -> loadContentMenu(QUIZ_MODE_LF));

        actionButton.setOnClickListener(onClick -> backToMainMenu());

        // Android Zurück-Taste blockieren, damit Navigation bewusst über den UI-Button läuft.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    // ContentMenu starten und gewählten Hauptmodus per Intent übergeben.
    private void loadContentMenu(String quizMode) {
        Intent quizContentData = new Intent(this, ContentMenuActivity.class);
        quizContentData.putExtra(INTENT_EXTRA_QUIZ_MODE, quizMode);

        startActivity(quizContentData);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
    }

    // Zurück ins Hauptmenü wechseln und vorhandene MainActivity wiederverwenden.
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

    // Titel fett/größer darstellen und Beschreibung normal anhängen.
    // REFACTOR PRIO 2: Ähnliche Textformat-Methoden in 3 Activities -> später in eine gemeinsame Hilfsklasse auslagern.
    private void setTextFormatForTwoTitleAndDescription(
            String titleText1,
            String descriptionText1,
            String titleText2,
            String descriptionText2
    ) {
        SpannableStringBuilder builtString = new SpannableStringBuilder();

        SpannableStringBuilder boldTitle1 = new SpannableStringBuilder(titleText1);

        // SPAN_EXCLUSIVE_EXCLUSIVE begrenzt die Formatierung auf den ursprünglichen Textbereich.
        boldTitle1.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                titleText1.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        boldTitle1.setSpan(
                new RelativeSizeSpan(TITLE_RELATIVE_SIZE),
                0,
                titleText1.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        SpannableStringBuilder normalDescription1 = new SpannableStringBuilder(descriptionText1);

        SpannableStringBuilder boldTitle2 = new SpannableStringBuilder(titleText2);

        boldTitle2.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                titleText2.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        boldTitle2.setSpan(
                new RelativeSizeSpan(TITLE_RELATIVE_SIZE),
                0,
                titleText2.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        SpannableStringBuilder normalDescription2 = new SpannableStringBuilder(descriptionText2);

        builtString.append(boldTitle1);
        builtString.append(normalDescription1);
        builtString.append(QUIZ_MENU_TEXT_SPACER);
        builtString.append(boldTitle2);
        builtString.append(normalDescription2);

        lfTitleText.setText(builtString);
    }

}
