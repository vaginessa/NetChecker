/*
 * Copyright 2017 KoFuk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chronoscoper.android.netchecker

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import kotterknife.bindView
import okhttp3.*

class MainActivity : AppCompatActivity() {

    private val executeButton by bindView<Button>(R.id.execute)
    private val urlView by bindView<EditText>(R.id.url)
    private val addRequestBodyButton by bindView<Button>(R.id.add)
    private val requestBodyListView by bindView<RecyclerView>(R.id.request_body)
    private val dumpCookieButton by bindView<Button>(R.id.dump_cookie)
    private val clearCookieButton by bindView<Button>(R.id.clear_cookie)
    private val clearConsoleButton by bindView<Button>(R.id.clear_console)
    private val consoleView by bindView<TextView>(R.id.console)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        executeButton.click = View.OnClickListener {
            executor.execute()
        }

        dumpCookieButton.click = View.OnClickListener {
            updateConsole(cookie?.toString())
        }

        clearCookieButton.click = View.OnClickListener {
            cookie = mutableListOf()
        }

        clearConsoleButton.click = View.OnClickListener {
            consoleView.text = null
        }

        requestBodyListView.layoutManager = LinearLayoutManager(this)
        requestBodyListView.adapter = RequestBodyRecyclerAdapter()

        addRequestBodyButton.click = View.OnClickListener {
            TODO()
        }
    }

    private fun updateConsole(message: String?) {
        consoleView.append(message)
        consoleView.append("\n")
    }

    private val executor = object : AsyncTask<Unit, String, Unit>() {
        private var url: String? = null

        override fun onPreExecute() {
            super.onPreExecute()
            url = urlView.text.toString()
            executeButton.isEnabled = false
            addRequestBodyButton.isEnabled = false
        }

        override fun doInBackground(vararg p0: Unit?) {
            try {
                val client = OkHttpClient.Builder()
                        .cookieJar(cookieJar)
                        .build()

                val requestBodyBuilder = FormBody.Builder()

                requestBody.forEach {
                    if (it.encoded) {
                        requestBodyBuilder.addEncoded(it.name, it.value)
                    } else {
                        requestBodyBuilder.add(it.name, it.value)
                    }
                }

                val request = Request.Builder()
                        .url(url)
                        .post(requestBodyBuilder.build())
                        .build()

                val response = client.newCall(request).execute()

                publishProgress(
                        "Status code: ${response.code()}",
                        "Message: ${response.message()}")

                publishProgress("BODY:", response.body()?.string())
            } catch (t: Throwable) {
                publishProgress(t.toString())
            }
        }

        override fun onProgressUpdate(vararg values: String?) {
            super.onProgressUpdate(*values)
            values.forEach {
                updateConsole(it)
            }
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)
            executeButton.isEnabled = true
            addRequestBodyButton.isEnabled = false
        }
    }

    private var cookie: MutableList<Cookie>? = mutableListOf()

    private val cookieJar = object : okhttp3.CookieJar {
        override fun saveFromResponse(url: HttpUrl?, cookies: MutableList<Cookie>?) {
            cookie = cookies
        }

        override fun loadForRequest(url: HttpUrl?): MutableList<Cookie> = cookie ?: mutableListOf()
    }

    private val requestBody = arrayListOf<RequestBodyItem>()

    inner class RequestBodyRecyclerAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onBindViewHolder(holder: ViewHolder?, position: Int) {
        }

        override fun getItemCount(): Int = requestBody.size + 1

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder =
                ViewHolder(View(this@MainActivity))

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

data class RequestBodyItem(val name: String, val value: String, val encoded: Boolean)
