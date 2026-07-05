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

public class ContentMenuActivity extends AppCompatActivity {
    private static final String QUIZ_MODE_AP1 = "AP1";
    private static final String QUIZ_MODE_LF = "LF";

    private static final String QUIZ_MODE_LF01 = "LF-01";
    private static final String QUIZ_MODE_LF02 = "LF-02";
    private static final String QUIZ_MODE_LF03 = "LF-03";
    private static final String QUIZ_MODE_LF04 = "LF-04";
    private static final String QUIZ_MODE_LF05 = "LF-05";
    private static final String QUIZ_MODE_LF06 = "LF-06";

    private static final String INTENT_EXTRA_QUIZ_MODE = "quizMode";

    private static final String TITLE_AP1 = "Abschlussprüfung 1";
    private static final String DESCRIPTION_AP1 =
            "\n\nEine prüfungsnahe Mischung aus den Lernfeldern 01 bis 06 mit 36 Fragen.";

    private static final String TITLE_LF = "Lernfeld-Vertiefung";
    private static final String DESCRIPTION_LF =
            "\n\nEine gezielte 20-Fragen-Runde zur Wiederholung und Vertiefung eines einzelnen Lernfeldes.";

    private static final float TITLE_RELATIVE_SIZE = 1.3f;

    // XML: Texte
    private TextView statusBarText;
    private TextView lfTitleText;
    private TextView flavorText;

    // XML: Auswahlbuttons
    private Button ap1Button;
    private Button lf01Button;
    private Button lf02Button;
    private Button lf03Button;
    private Button lf04Button;
    private Button lf05Button;
    private Button lf06Button;

    // XML: Action Button
    private Button actionButton;

    // Quiz-Vorbereitung
    private String quizMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_menu_activity);

        // quizMode aus Intent übernehmen; Fallback dient als Sicherheit für direkten Activity-Start.
        quizMode = getIntent().getStringExtra(INTENT_EXTRA_QUIZ_MODE);

        if (quizMode == null) {
            quizMode = QUIZ_MODE_AP1;
        }

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
        lf01Button = findViewById(R.id.lf01Button);
        lf02Button = findViewById(R.id.lf02Button);
        lf03Button = findViewById(R.id.lf03Button);
        lf04Button = findViewById(R.id.lf04Button);
        lf05Button = findViewById(R.id.lf05Button);
        lf06Button = findViewById(R.id.lf06Button);

        actionButton = findViewById(R.id.actionButton);
    }

    // ContentMenu anhand des übergebenen quizMode aufbauen.
    private void initUI() {
        statusBarText.setVisibility(View.VISIBLE);

        lfTitleText.setVisibility(View.VISIBLE);
        flavorText.setVisibility(View.VISIBLE);

        if (QUIZ_MODE_AP1.equals(quizMode)) {
            setTextFormatForTitleAndDescription(
                    TITLE_AP1,
                    DESCRIPTION_AP1
            );

            ap1Button.setVisibility(View.VISIBLE);

            lf01Button.setVisibility(View.GONE);
            lf02Button.setVisibility(View.GONE);
            lf03Button.setVisibility(View.GONE);
            lf04Button.setVisibility(View.GONE);
            lf05Button.setVisibility(View.GONE);
            lf06Button.setVisibility(View.GONE);
        } else if (QUIZ_MODE_LF.equals(quizMode)) {
            setTextFormatForTitleAndDescription(
                    TITLE_LF,
                    DESCRIPTION_LF
            );

            ap1Button.setVisibility(View.GONE);

            lf01Button.setVisibility(View.VISIBLE);
            lf02Button.setVisibility(View.VISIBLE);
            lf03Button.setVisibility(View.VISIBLE);
            lf04Button.setVisibility(View.VISIBLE);
            lf05Button.setVisibility(View.VISIBLE);
            lf06Button.setVisibility(View.VISIBLE);
        }

        actionButton.setVisibility(View.VISIBLE);
    }

    private void initButtonBehavior() {
        ap1Button.setOnClickListener(onClick -> loadContentSubmenu(QUIZ_MODE_AP1));
        lf01Button.setOnClickListener(onClick -> loadContentSubmenu(QUIZ_MODE_LF01));
        lf02Button.setOnClickListener(onClick -> loadContentSubmenu(QUIZ_MODE_LF02));
        lf03Button.setOnClickListener(onClick -> loadContentSubmenu(QUIZ_MODE_LF03));
        lf04Button.setOnClickListener(onClick -> loadContentSubmenu(QUIZ_MODE_LF04));
        lf05Button.setOnClickListener(onClick -> loadContentSubmenu(QUIZ_MODE_LF05));
        lf06Button.setOnClickListener(onClick -> loadContentSubmenu(QUIZ_MODE_LF06));

        actionButton.setOnClickListener(onClick -> backToQuizMenu());

        // Android Zurück-Taste blockieren, damit Navigation bewusst über den UI-Button läuft.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    // ContentSubmenu starten und ausgewählten Modus per Intent übergeben.
    private void loadContentSubmenu(String quizMode) {
        Intent quizContentData = new Intent(this, ContentSubmenuActivity.class);
        quizContentData.putExtra(INTENT_EXTRA_QUIZ_MODE, quizMode);

        startActivity(quizContentData);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
    }

    // Zurück ins Quiz-Auswahlmenü wechseln und vorhandene QuizMenuActivity wiederverwenden.
    private void backToQuizMenu() {
        Intent quizMenu = new Intent(this, QuizMenuActivity.class);

        // CLEAR_TOP entfernt Activities über QuizMenuActivity, SINGLE_TOP nutzt eine vorhandene QuizMenuActivity wieder.
        quizMenu.setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(quizMenu);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
    }

    // Titel fett/größer darstellen und Beschreibung normal anhängen.
    // REFACTOR PRIO 2: Ähnliche Textformat-Methoden in 3 Activities -> später in eine gemeinsame Hilfsklasse auslagern.
    private void setTextFormatForTitleAndDescription(String titleText, String descriptionText) {
        SpannableStringBuilder builtString = new SpannableStringBuilder();

        SpannableStringBuilder boldTitle = new SpannableStringBuilder(titleText);

        // SPAN_EXCLUSIVE_EXCLUSIVE begrenzt die Formatierung auf den ursprünglichen Textbereich.
        boldTitle.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                titleText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        boldTitle.setSpan(
                new RelativeSizeSpan(TITLE_RELATIVE_SIZE),
                0,
                titleText.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        SpannableStringBuilder normalDescription = new SpannableStringBuilder(descriptionText);

        builtString.append(boldTitle);
        builtString.append(normalDescription);

        lfTitleText.setText(builtString);
    }

}
