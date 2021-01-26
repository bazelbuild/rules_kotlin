package examples.android.lib2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    LinearLayout parent = new LinearLayout(this);
    parent.setOrientation(LinearLayout.VERTICAL);
    Button button = new Button(this);
    button.setText("Foo!");
    parent.addView(button);
    setContentView(parent, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

    new AlertDialog.Builder(this)
        .setTitle("Blah")
        .setMessage(R.string.little_bat)
        .show();
  }
}
