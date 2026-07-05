package app.filb.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

public class ContentSubmenuActivity extends AppCompatActivity {
    private static final String SETTINGS_NAME = "settings";

    private static final String LEFT_HANDED_KEY = "leftHanded";

    private static final String INTENT_EXTRA_QUIZ_MODE = "quizMode";
    private static final String INTENT_EXTRA_JSON_LIST = "jsonList";

    private static final String HAS_LAST_QUIZ_DATA_KEY = "hasLastQuizData";
    private static final String LAST_QUIZ_MODE_KEY = "lastQuizMode";
    private static final String LAST_JSON_LIST_KEY = "lastJsonList";

    private static final String QUIZ_MODE_AP1 = "AP1";
    private static final String QUIZ_MODE_LF = "LF";

    private static final String QUIZ_MODE_LF01 = "LF-01";
    private static final String QUIZ_MODE_LF02 = "LF-02";
    private static final String QUIZ_MODE_LF03 = "LF-03";
    private static final String QUIZ_MODE_LF04 = "LF-04";
    private static final String QUIZ_MODE_LF05 = "LF-05";
    private static final String QUIZ_MODE_LF06 = "LF-06";

    // Textanhänge für quizMode
    private static final String FACTUAL_QUESTIONS_TEXT = " - Sachfragen";
    private static final String TECHNICAL_TERMS_TEXT = " - Fachbegriffe";
    private static final String ABBREVIATIONS_TEXT = " - Abkürzungen";
    private static final String SUBNETTING_TEXT = " - Subnetting";
    private static final String CODE_SNIPPETS_TEXT = " - Codeausschnitte";
    private static final String CALCULATIONS_TEXT = " - Berechnungen";

    private static final String TITLE_FACTS = "Sachfragen";
    private static final String DESCRIPTION_FACTS = "\n\nprüfen Anwendung, Verständnis und Einordnung von Themen.";

    private static final String TITLE_TERMS = "Fachbegriffe";
    private static final String DESCRIPTION_TERMS = "\n\nprüfen Definitionen und Bedeutungen zentraler Begriffe.";

    private static final String TITLE_ABBR = "Abkürzungen";
    private static final String DESCRIPTION_ABBR = "\n\nprüfen Langformen technischer Abkürzungen.";

    private static final String DESCRIPTION_EXTRA = "\n\n\nJe nach Lernfeld können zusätzliche Fragenpakete verfügbar sein.";

    private static final String QUIZ_MENU_TEXT_SPACER = "\n\n\n";
    private static final float TITLE_RELATIVE_SIZE = 1.3f;

    // 20-Fragen-Modus: Poolgröße
    private static final int LF20_QUESTIONS_SIZE = 20;

    // AP1 Gewichtung LF01-LF06: 1:3:3:1:3:1, Multiplikator 3 ergibt 36 Fragen.
    private static final int QUESTION_MULTIPLIER_AP1 = 3;
    private static final int TAKE_QUESTIONS_LF01 = 1 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF02 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF03 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF04 = 1 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF05 = 3 * QUESTION_MULTIPLIER_AP1;
    private static final int TAKE_QUESTIONS_LF06 = 1 * QUESTION_MULTIPLIER_AP1;

    private static final String ERROR_NOT_ENOUGH_QUESTIONS = "Zu wenig Fragen für diese Auswahl vorhanden.";
    private static final String ERROR_JSON_PATH = "Keine JSON vorhanden oder falscher Pfad.";
    private static final String ERROR_JSON_SYNTAX = "Fehler in der JSON-Struktur.";

    // Quiz-Vorbereitung
    private QuizLoader quizLoader;
    private Intent quizContentData;
    private QuestionProgressRepository questionProgressRepository;

    private SharedPreferences sharedPreferences;

    private String quizMode;
    private String[] jsonList;

    // XML: Hauptbuttons
    private Button factualQuestionsButton;
    private Button technicalTermsButton;
    private Button abbreviationsButton;

    // XML: optionale Buttons
    private Button subnettingButton;
    private Button codeSnippetsButton;
    private Button calculationsButton;

    // XML: Status, Beschreibung und Navigation
    private TextView statusBarText;
    private TextView lfTitleText;
    private TextView flavorText;
    private Button actionButton;

    private final List<List<QuizQuestion>> lf20QuestionsSessionPools = new ArrayList<>();

    // AP1-Fragenpools je Lernfeld für Poolgrößenprüfung.
    private final List<QuizQuestion> lf01QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf02QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf03QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf04QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf05QuizSessionPool = new ArrayList<>();
    private final List<QuizQuestion> lf06QuizSessionPool = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.content_submenu_activity);

        // quizMode aus Intent übernehmen; Fallback dient als Sicherheit für direkten Activity-Start.
        quizMode = getIntent().getStringExtra(INTENT_EXTRA_QUIZ_MODE);
        if (quizMode == null) {
            quizMode = QUIZ_MODE_AP1;
        }

        sharedPreferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);

        quizLoader = new QuizLoader(getAssets());
        questionProgressRepository = new QuestionProgressRepository(this);

        initViews();
        initUI();
        initButtonBehavior();
    }

    // XML-Views holen und Feldern zuweisen.
    private void initViews() {
        statusBarText = findViewById(R.id.statusBarText);

        lfTitleText = findViewById(R.id.lfTitleText);
        flavorText = findViewById(R.id.flavorText);

        factualQuestionsButton = findViewById(R.id.factualQuestionsButton);
        technicalTermsButton = findViewById(R.id.technicalTermsButton);
        abbreviationsButton = findViewById(R.id.abbreviationsButton);

        subnettingButton = findViewById(R.id.subnettingButton);
        codeSnippetsButton = findViewById(R.id.codeSnippetsButton);
        calculationsButton = findViewById(R.id.calculationsButton);

        actionButton = findViewById(R.id.actionButton);
    }

    // Submenu anhand des übergebenen quizMode aufbauen.
    private void initUI() {
        statusBarText.setVisibility(View.VISIBLE);

        lfTitleText.setVisibility(View.VISIBLE);
        flavorText.setVisibility(View.INVISIBLE);

        factualQuestionsButton.setVisibility(View.VISIBLE);
        technicalTermsButton.setVisibility(View.VISIBLE);
        abbreviationsButton.setVisibility(View.VISIBLE);

        subnettingButton.setVisibility(View.GONE);
        codeSnippetsButton.setVisibility(View.GONE);
        calculationsButton.setVisibility(View.GONE);

        if (QUIZ_MODE_LF02.equals(quizMode)) {
            calculationsButton.setVisibility(View.VISIBLE);
        }

        if (QUIZ_MODE_LF03.equals(quizMode)) {
            subnettingButton.setVisibility(View.VISIBLE);
            calculationsButton.setVisibility(View.VISIBLE);
        }

        if (QUIZ_MODE_LF05.equals(quizMode)) {
            codeSnippetsButton.setVisibility(View.VISIBLE);
            calculationsButton.setVisibility(View.VISIBLE);
        }

        setTextFormatForThreeTitleAndDescription(
                TITLE_FACTS,
                DESCRIPTION_FACTS,
                TITLE_TERMS,
                DESCRIPTION_TERMS,
                TITLE_ABBR,
                DESCRIPTION_ABBR,
                DESCRIPTION_EXTRA
        );

        actionButton.setVisibility(View.VISIBLE);
    }

    private void initButtonBehavior() {
        factualQuestionsButton.setOnClickListener(onClick -> addStringToQuizMode(FACTUAL_QUESTIONS_TEXT));
        technicalTermsButton.setOnClickListener(onClick -> addStringToQuizMode(TECHNICAL_TERMS_TEXT));
        abbreviationsButton.setOnClickListener(onClick -> addStringToQuizMode(ABBREVIATIONS_TEXT));
        subnettingButton.setOnClickListener(onClick -> addStringToQuizMode(SUBNETTING_TEXT));
        codeSnippetsButton.setOnClickListener(onClick -> addStringToQuizMode(CODE_SNIPPETS_TEXT));
        calculationsButton.setOnClickListener(onClick -> addStringToQuizMode(CALCULATIONS_TEXT));

        actionButton.setOnClickListener(onClick -> backToContentMenu());

        // Android Zurück-Taste blockieren, damit Navigation bewusst über den UI-Button läuft.
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    // Zurück ins ContentMenu wechseln und vorhandene ContentMenuActivity wiederverwenden.
    private void backToContentMenu() {
        Intent contentMenu = new Intent(this, ContentMenuActivity.class);

        if (QUIZ_MODE_AP1.equals(quizMode)) {
            contentMenu.putExtra(INTENT_EXTRA_QUIZ_MODE, QUIZ_MODE_AP1);
        } else {
            contentMenu.putExtra(INTENT_EXTRA_QUIZ_MODE, QUIZ_MODE_LF);
        }

        // CLEAR_TOP entfernt Activities über ContentMenuActivity, SINGLE_TOP nutzt eine vorhandene ContentMenuActivity wieder.
        contentMenu.setFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(contentMenu);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
    }

    // Fragenpaket an den Lernfeld-/AP1-Modus anhängen und finale Quiz-Auswahl vorbereiten.
    private void addStringToQuizMode(String addedString) {
        if (quizMode == null) {
            quizMode = QUIZ_MODE_AP1;
        }

        switch (quizMode) {
            case QUIZ_MODE_AP1:
            case QUIZ_MODE_LF01:
            case QUIZ_MODE_LF02:
            case QUIZ_MODE_LF03:
            case QUIZ_MODE_LF04:
            case QUIZ_MODE_LF05:
            case QUIZ_MODE_LF06:
                setupQuizMode(quizMode + addedString);
                break;

            default:
                setupQuizMode(QUIZ_MODE_AP1 + FACTUAL_QUESTIONS_TEXT);
                break;
        }
    }

    // Finale Quiz-Auswahl auf konkrete JSON-Fragenpakete abbilden.
    // REFACTOR PRIO 3: JSON Pfade kommen in mindestens 2 Activities vor -> Auslagern
    private void setupQuizMode(String quizMode) {
        if ("AP1 - Sachfragen".equals(quizMode)) {
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
        } else if ("AP1 - Fachbegriffe".equals(quizMode)) {
            jsonList = new String[]{
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
            };
        } else if ("AP1 - Abkürzungen".equals(quizMode)) {
            jsonList = new String[]{
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
            };
        } else if ("LF-01 - Sachfragen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-01/lf-01-sach-arbm.json",
                    "questions/lf-01/lf-01-sach-bwl.json",
                    "questions/lf-01/lf-01-sach-markt.json",
                    "questions/lf-01/lf-01-sach-orga.json",
                    "questions/lf-01/lf-01-sach-sozi.json",
                    "questions/lf-01/lf-01-sach-ziel.json",
            };
        } else if ("LF-01 - Fachbegriffe".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-01/lf-01-fach-arbm.json",
                    "questions/lf-01/lf-01-fach-bwl.json",
                    "questions/lf-01/lf-01-fach-markt.json",
                    "questions/lf-01/lf-01-fach-orga.json",
                    "questions/lf-01/lf-01-fach-sozi.json",
                    "questions/lf-01/lf-01-fach-ziel.json",
            };
        } else if ("LF-01 - Abkürzungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-01/lf-01-abkz.json",
            };
        } else if ("LF-02 - Sachfragen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-02/lf-02-sach-besch.json",
                    "questions/lf-02/lf-02-sach-hard.json",
                    "questions/lf-02/lf-02-sach-kommun.json",
                    "questions/lf-02/lf-02-sach-proj.json",
                    "questions/lf-02/lf-02-sach-recht.json",
                    "questions/lf-02/lf-02-sach-sich.json",
            };
        } else if ("LF-02 - Fachbegriffe".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-02/lf-02-fach-besch.json",
                    "questions/lf-02/lf-02-fach-hard.json",
                    "questions/lf-02/lf-02-fach-kalk.json",
                    "questions/lf-02/lf-02-fach-kommun.json",
                    "questions/lf-02/lf-02-fach-proj.json",
                    "questions/lf-02/lf-02-fach-recht.json",
            };
        } else if ("LF-02 - Abkürzungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-02/lf-02-abkz.json",
            };
        } else if ("LF-02 - Berechnungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-02/lf-02-rech-finanz.json",
                    "questions/lf-02/lf-02-rech-kalk.json",
                    "questions/lf-02/lf-02-rech-wirt.json",
            };
        } else if ("LF-03 - Sachfragen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-03/lf-03-sach-adrs.json",
                    "questions/lf-03/lf-03-sach-cloud.json",
                    "questions/lf-03/lf-03-sach-integ.json",
                    "questions/lf-03/lf-03-sach-komp.json",
                    "questions/lf-03/lf-03-sach-modl.json",
                    "questions/lf-03/lf-03-sach-secu.json",
                    "questions/lf-03/lf-03-sach-strv.json",
            };
        } else if ("LF-03 - Fachbegriffe".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-03/lf-03-fach-adrs.json",
                    "questions/lf-03/lf-03-fach-cloud.json",
                    "questions/lf-03/lf-03-fach-komp.json",
                    "questions/lf-03/lf-03-fach-modl.json",
                    "questions/lf-03/lf-03-fach-secu.json",
                    "questions/lf-03/lf-03-fach-strv.json",
            };
        } else if ("LF-03 - Abkürzungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-03/lf-03-abkz.json",
            };
        } else if ("LF-03 - Subnetting".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-03/lf-03-subn-ipv4.json",
            };
        } else if ("LF-03 - Berechnungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-03/lf-03-rech-netz.json",
            };
        } else if ("LF-04 - Sachfragen".equals(quizMode)) {
            jsonList = new String[]{
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
            };
        } else if ("LF-04 - Fachbegriffe".equals(quizMode)) {
            jsonList = new String[]{
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
            };
        } else if ("LF-04 - Abkürzungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-04/lf-04-abkz.json",
            };
        } else if ("LF-05 - Sachfragen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-05/lf-05-sach-data.json",
                    "questions/lf-05/lf-05-sach-db.json",
                    "questions/lf-05/lf-05-sach-dev.json",
                    "questions/lf-05/lf-05-sach-modl.json",
                    "questions/lf-05/lf-05-sach-prog.json",
            };
        } else if ("LF-05 - Fachbegriffe".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-05/lf-05-fach-data.json",
                    "questions/lf-05/lf-05-fach-db.json",
                    "questions/lf-05/lf-05-fach-dev.json",
                    "questions/lf-05/lf-05-fach-modl.json",
                    "questions/lf-05/lf-05-fach-prog.json",
            };
        } else if ("LF-05 - Abkürzungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-05/lf-05-abkz.json",
            };
        } else if ("LF-05 - Codeausschnitte".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-05/lf-05-code-py.json",
                    "questions/lf-05/lf-05-code-sql.json",
            };
        } else if ("LF-05 - Berechnungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-05/lf-05-rech-zahl.json",
            };
        } else if ("LF-06 - Sachfragen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-06/lf-06-sach-anal.json",
                    "questions/lf-06/lf-06-sach-komm.json",
                    "questions/lf-06/lf-06-sach-moni.json",
                    "questions/lf-06/lf-06-sach-serv.json",
                    "questions/lf-06/lf-06-sach-tick.json",
                    "questions/lf-06/lf-06-sach-vert.json",
                    "questions/lf-06/lf-06-sach-wart.json",
            };
        } else if ("LF-06 - Fachbegriffe".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-06/lf-06-fach-anal.json",
                    "questions/lf-06/lf-06-fach-komm.json",
                    "questions/lf-06/lf-06-fach-moni.json",
                    "questions/lf-06/lf-06-fach-serv.json",
                    "questions/lf-06/lf-06-fach-tick.json",
                    "questions/lf-06/lf-06-fach-vert.json",
                    "questions/lf-06/lf-06-fach-wart.json",
            };
        } else if ("LF-06 - Abkürzungen".equals(quizMode)) {
            jsonList = new String[]{
                    "questions/lf-06/lf-06-abkz.json",
            };
        }

        prepareQuizContentData(quizMode, jsonList);
    }

    // Schlüsselmeister: JSONs und interne Datenbank correct_answer prüfen, Quiz nur bei ausreichend Fragen starten.
    // REFACTOR PRIO 4: Ähnliche Prüfungen in mehreren Screens -> Schlüsselmeister-/Torwächterlogik auslagern.
    private void prepareQuizContentData(String quizMode, String[] jsonList) {
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
        } catch (IOException ioException) {
            flavorText.setText(ERROR_JSON_PATH);
            flavorText.setVisibility(View.VISIBLE);
            return;
        } catch (JsonSyntaxException jsonSyntaxException) {
            flavorText.setText(ERROR_JSON_SYNTAX);
            flavorText.setVisibility(View.VISIBLE);
            return;
        }

        saveLastQuizData(quizMode, jsonList);
        startQuiz(quizMode, jsonList);
    }

    // Erfolgreich geprüfte Quiz-Auswahl für den Button „Letzte Auswahl“ speichern.
    private void saveLastQuizData(String quizMode, String[] jsonList) {
        String jsonListString = new Gson().toJson(jsonList);

        sharedPreferences.edit()
                .putString(LAST_QUIZ_MODE_KEY, quizMode)
                .putString(LAST_JSON_LIST_KEY, jsonListString)
                .putBoolean(HAS_LAST_QUIZ_DATA_KEY, true)
                .apply();
    }

    // QuizActivity starten und geprüfte Quizdaten per Intent übergeben.
    private void startQuiz(String quizMode, String[] jsonList) {
        quizContentData = new Intent(this, QuizActivity.class);
        quizContentData.putExtra(INTENT_EXTRA_QUIZ_MODE, quizMode);
        quizContentData.putExtra(INTENT_EXTRA_JSON_LIST, jsonList);

        startActivity(quizContentData);
        overridePendingTransition(android.R.anim.fade_in, R.anim.no_anim);
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

                prepareQuizContentData(quizMode, jsonList);
            });
        } else {
            leftConfirmationButton.setText("Quiz starten");
            rightConfirmationButton.setText("Abbrechen");

            leftConfirmationButton.setOnClickListener(onClick -> {
                questionProgressRepository.resetCorrectAnswersForQuestionIds(selectedQuestionIds);

                dialogConfirmationWindow.dismiss();

                prepareQuizContentData(quizMode, jsonList);
            });

            rightConfirmationButton.setOnClickListener(onClick -> dialogConfirmationWindow.dismiss());
        }

        dialogConfirmationWindow.show();
    }

    // Titel fett/größer darstellen und Beschreibung normal anhängen.
    // REFACTOR PRIO 2: Ähnliche Textformat-Methoden in 3 Activities -> später in eine gemeinsame Hilfsklasse auslagern.
    private void setTextFormatForThreeTitleAndDescription(
            String titleText1,
            String descriptionText1,
            String titleText2,
            String descriptionText2,
            String titleText3,
            String descriptionText3,
            String extraDescription
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

        SpannableStringBuilder boldTitle3 = new SpannableStringBuilder(titleText3);

        boldTitle3.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                titleText3.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        boldTitle3.setSpan(
                new RelativeSizeSpan(TITLE_RELATIVE_SIZE),
                0,
                titleText3.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        SpannableStringBuilder normalDescription3 = new SpannableStringBuilder(descriptionText3);

        SpannableStringBuilder extraNormalDescription = new SpannableStringBuilder(extraDescription);

        builtString.append(boldTitle1);
        builtString.append(normalDescription1);
        builtString.append(QUIZ_MENU_TEXT_SPACER);
        builtString.append(boldTitle2);
        builtString.append(normalDescription2);
        builtString.append(QUIZ_MENU_TEXT_SPACER);
        builtString.append(boldTitle3);
        builtString.append(normalDescription3);
        builtString.append(extraNormalDescription);

        lfTitleText.setText(builtString);
    }

    // Hilfsmethode um alle gültigen IDs aus einer geladenen JSON in selectedQuestionIds zu sammeln.
    private void addQuestionIdsToSelectedQuestionIds(
            List<QuizQuestion> quizQuestions,
            List<String> selectedQuestionIds
    ) {
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

}
