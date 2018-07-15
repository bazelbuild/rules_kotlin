// Copyright 2015 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.bazel.example.android.activities

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_ping) {
            object : AsyncTask<String, Void, String>() {
                val READ_TIMEOUT_MS = 5000
                val CONNECTION_TIMEOUT_MS = 2000

                @Throws(IOException::class)
                private fun inputStreamToString(stream: InputStream): String {
                    val result = StringBuilder()
                    try {
                        val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
                        var line: String = reader.readLine()
                        while ( line != null) {
                            result.append(line)
                            line = reader.readLine()
                        }
                    } finally {
                        stream.close()
                    }
                    return result.toString()
                }

                @Throws(IOException::class)
                private fun getConnection(url: String): HttpURLConnection {
                    val urlConnection = URL(url).openConnection()
                    urlConnection.connectTimeout = CONNECTION_TIMEOUT_MS
                    urlConnection.readTimeout = READ_TIMEOUT_MS
                    return urlConnection as HttpURLConnection
                }

                override fun doInBackground(vararg params: String): String? {
                    val url = params[0]
                    var connection: HttpURLConnection? = null
                    try {
                        connection = getConnection(url)
                        return JSONObject(inputStreamToString(connection.inputStream))
                                .getString("requested")
                    } catch (e: IOException) {
                        Log.e("background", "IOException", e)
                        return null
                    } catch (e: JSONException) {
                        Log.e("background", "JSONException", e)
                        return null
                    } finally {
                        if (connection != null) {
                            connection.disconnect()
                        }
                    }
                }

                override fun onPostExecute(result: String?) {
                    val textView = findViewById<View>(R.id.text_view) as TextView
                    if (result == null) {
                        Toast.makeText(
                                this@MainActivity, getString(R.string.error_sending_request), Toast.LENGTH_LONG)
                                .show()
                        textView.text = "???"
                        return
                    }
                    textView.text = result
                }
            }.execute("http://10.0.2.2:8080/boop")
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
