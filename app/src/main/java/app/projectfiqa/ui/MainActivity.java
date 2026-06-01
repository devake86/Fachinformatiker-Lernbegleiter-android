package app.projectfiqa.ui;

// Da R in app.projectfiqa liegt nicht in app.projectfiqa.ui muss hier importiert werden
import app.projectfiqa.R;

// Enthält Speicherzustand.
import android.os.Bundle;

// Basisklasse für Activities.
import androidx.appcompat.app.AppCompatActivity;

// Objekt zum Starten von unter anderem Activities
import android.content.Intent;

// View imports
import android.view.View;

// Button stuff
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // Easter Egg
    private TextView versionIdText;
    private int clickCount = 0;
    private long lastClickTime = 0;
    CharSequence previousText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout laden
        setContentView(R.layout.main_activity);

        Button ap1Button = findViewById(R.id.ap1Button);
        Button start20QuestionsButton = findViewById(R.id.start20QuestionsButton);
        Button exitButton = findViewById(R.id.exitButton);




        // Extra Modi für beta2
        Button startABKZButton = findViewById(R.id.startABKZButton);
        Button startFBEGButton = findViewById(R.id.startFBEGButton);




        // Action Button für später Detailanzeige für Modes in FrageBox und/oder ErklärungBox
        Button actionButton = findViewById(R.id.actionButton);
        actionButton.setVisibility(View.INVISIBLE);

        // Bei Drücken von AP1 öffne QuizActivity
        ap1Button.setOnClickListener(onClick -> {
            Intent quizMode = new Intent(this, QuizActivity.class);
            quizMode.putExtra("quizMode", "AP1 PoC");
            startActivity(quizMode);
            // Sanfter Übergang zur nächsten Activity
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Bei Drücken von 20 Fragen öffne QuizActivity
        start20QuestionsButton.setOnClickListener(onClick -> {
            Intent quizMode = new Intent(this, QuizActivity.class);
            quizMode.putExtra("quizMode", "20 Fragen - LF-VT1");
            startActivity(quizMode);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });



        // Extra Modi für beta2
        startABKZButton.setOnClickListener(onClick -> {
            Intent quizMode = new Intent(this, QuizActivity.class);
            quizMode.putExtra("quizMode", "20 Fragen - ABKZ");
            startActivity(quizMode);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Bei Drücken von 20 Fragen öffne QuizActivity
        startFBEGButton.setOnClickListener(onClick -> {
            Intent quizMode = new Intent(this, QuizActivity.class);
            quizMode.putExtra("quizMode", "20 Fragen - FBEG");
            startActivity(quizMode);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });








        // Bei Drücken von Beenden schließe App
        exitButton.setOnClickListener(onClick -> {
            finish();
        });

        // Easter Egg
        creatorEasterEgg();

    }

    private void creatorEasterEgg(){
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

                versionIdText.postDelayed(() -> {
                    versionIdText.setText(previousText);
                }, 3000);

                clickCount = 0;
            }
        });
    }

}