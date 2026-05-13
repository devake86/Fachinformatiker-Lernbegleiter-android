package app.projectfiqaandroid.ui;

// Da R in app.projectfiqaandroid liegt nicht in app.projectfiqaandroid.ui muss hier importiert werden
import app.projectfiqaandroid.R;

// Enthält Speicherzustand.
import android.os.Bundle;

// Basisklasse für Activities.
import androidx.appcompat.app.AppCompatActivity;

// Objekt zum Starten von unter anderem Activities
import android.content.Intent;

// View imports
import android.view.Gravity;
import android.widget.LinearLayout;
import android.view.View;

// Button stuff
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    // MenuBox
    private LinearLayout interactionBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Layout laden
        setContentView(R.layout.activity_main);

        // Menu Box erzeugen
        interactionBox = findViewById(R.id.interactionBox);
        interactionBox.setVisibility(View.VISIBLE);

        setupMenuButtons();

    }

    // Methode für Start und Beenden Button
    public void setupMenuButtons() {

        // Layout Parameter für Start und Beenden Button
        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        dpToPx(220),
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        buttonParams.gravity = Gravity.CENTER;

        // Start Button
        Button startButton = new Button(this, null, 0, R.style.ActionButton);
        startButton.setText("Start");

        // Bei Drücken von Start öffne QuizActivity
        startButton.setOnClickListener(onClick -> {
            startActivity(new Intent(this, QuizActivity.class));
        });

        // Beenden Button
        Button exitButton = new Button(this, null, 0, R.style.ActionButton);
        exitButton.setText("Beenden");

        // Bei Drücken von Beenden schließe App
        exitButton.setOnClickListener(onClick -> finish());

        interactionBox.addView(startButton, buttonParams);
        interactionBox.addView(exitButton, buttonParams);

    }

    // Hilfsmethode für dp zu px
    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources().getDisplayMetrics().density
        );
    }

}