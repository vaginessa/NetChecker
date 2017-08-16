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
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
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

    private val inflater by lazy { LayoutInflater.from(this@MainActivity) }

    private val recyclerAdapter by lazy { RequestBodyRecyclerAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        executeButton.click = View.OnClickListener {
            Executor().execute()
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
        requestBodyListView.adapter = recyclerAdapter

        addRequestBodyButton.click = View.OnClickListener {
            val view = inflater.inflate(R.layout.add_body_dialog, null, false)
            val dialog = AlertDialog.Builder(this).setView(view).show()
            view.findViewById(R.id.done).click = View.OnClickListener {
                requestBody.add(
                        RequestBodyItem(
                                (view.findViewById(R.id.name) as TextView).text.toString(),
                                (view.findViewById(R.id.value) as TextView).text.toString(),
                                (view.findViewById(R.id.encoded) as Switch).isChecked))
                recyclerAdapter.notifyItemInserted(requestBody.size)
                dialog.dismiss()
            }
        }
    }

    private fun updateConsole(message: String?) {
        consoleView.append(message)
        consoleView.append("\n")
    }

    inner class Executor : AsyncTask<Unit, String, Unit>() {
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
            addRequestBodyButton.isEnabled = true
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
            val itemView = holder?.itemView
            if (itemView != null) {
                (itemView.findViewById(R.id.name) as TextView).text = requestBody[position].name
                (itemView.findViewById(R.id.value) as TextView).text = requestBody[position].value
                if (requestBody[position].encoded) {
                    itemView.findViewById(R.id.encoded).visibility = View.VISIBLE
                } else {
                    itemView.findViewById(R.id.encoded).visibility = View.GONE
                }
                itemView.findViewById(R.id.delete).click = View.OnClickListener {
                    requestBody.remove(requestBody[position])
                    notifyItemRemoved(position)
                }
            }
        }

        override fun getItemCount(): Int = requestBody.size

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): ViewHolder =
                ViewHolder(inflater.inflate(R.layout.request_body_item, parent, false))

    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}

data class RequestBodyItem(val name: String, val value: String, val encoded: Boolean)
