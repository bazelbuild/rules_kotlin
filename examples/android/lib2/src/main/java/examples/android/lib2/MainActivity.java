package examples.android.lib2;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

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
        .setMessage(R.string.little_bat)
        .show();
  }
}
