package examples.android.lib

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.Moshi

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val parent = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
    }.also { it.addView(Button(this).apply { text = "Foo!" }) }
    setContentView(parent, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    AlertDialog.Builder(this)
      .setTitle(this.getString(R.string.where_you_at))
      .setMessage("Blah blah blah? " + getString(R.string.hello))
      .show()
    // Ensure Serialization plugin has run and generated code correctly.
    Data.serializer()

    val adapter = DataJsonModelJsonAdapter(Moshi.Builder().build())
    println(adapter.toJson(DataJsonModel("foo")))

    TestKtValue.create {
      setName("Auto Value Test") // can't use property syntax, because autovalue builder's codegen
    }
  }
}
