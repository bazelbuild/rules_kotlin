package examples.android.lib2;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import androidx.appcompat.app.AlertDialog;

public class MainActivity extends Activity {
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
        .setMessage("Blah blah blah?")
        .show();
  }
}
