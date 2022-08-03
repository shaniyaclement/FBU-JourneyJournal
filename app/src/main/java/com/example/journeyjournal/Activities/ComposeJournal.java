package com.example.journeyjournal.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.journeyjournal.Adapters.JournalsAdapter;
import com.example.journeyjournal.ParseConnectorFiles.Journals;
import com.example.journeyjournal.R;
import com.example.journeyjournal.ShakeListener;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class ComposeJournal extends AppCompatActivity implements ShakeListener.Callback {

    public static final String TAG = "ComposeJournal";
    TextView etJournalTitle;
    TextView tvEntry;
    TextView addEntry;
    protected JournalsAdapter adapter;
    protected List<Journals> allJournals;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_journal);

        tvEntry = findViewById(R.id.etEntry);
        etJournalTitle = findViewById(R.id.etJournalTitle);
        addEntry = findViewById(R.id.addEntry);

        allJournals = new ArrayList<>();
        adapter = new JournalsAdapter(this, allJournals);

        addEntry.setOnClickListener(v -> {
            String title = etJournalTitle.getText().toString();
            String entry = tvEntry.getText().toString();
            if (title.isEmpty()) {
                Toast.makeText(ComposeJournal.this, "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (entry.isEmpty()) {
                Toast.makeText(ComposeJournal.this, "Entry cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            saveJournal(title, entry);
            finish();
        });
    }

    private void saveJournal(String title, String entry) {
        Journals journal = new Journals();
        journal.setTitle(title);
        journal.setEntry(entry);
        journal.setUser(ParseUser.getCurrentUser());
        journal.pinInBackground("Journals");
        etJournalTitle.setText("");
        tvEntry.setText("");
        journal.saveEventually();
    }

    @Override
    public void shakingStarted() {
        etJournalTitle.setText("");
        tvEntry.setText("");
    }

    @Override
    public void shakingStopped() {
        etJournalTitle.setText("");
        tvEntry.setText("");
    }
}