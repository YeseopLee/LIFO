package com.example.howareyou.Views.Writing

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.howareyou.Views.Detail.DetailActivity
import com.example.howareyou.Model.*
import com.example.howareyou.R
import com.example.howareyou.Util.App
import com.example.howareyou.Util.OnSingleClickListener
import com.example.howareyou.network.RetrofitClient
import com.example.howareyou.network.ServiceApi
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import gun0912.tedimagepicker.builder.TedImagePicker
import kotlinx.android.synthetic.main.activity_writing.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException

class WritingActivity : AppCompatActivity() {

    private var service: ServiceApi? = null

    private lateinit var board_id: String
    var _uriList: ArrayList<Uri> = arrayListOf()
    var uriList: ArrayList<Uri> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_writing)

        service = RetrofitClient.client!!.create(ServiceApi::class.java)

        initListener()

    }

    private fun initListener(){

        // alert dialog value
        val builder = AlertDialog.Builder(this).create()

        // button 관리
        writing_button_check.setOnClickListener (object : OnSingleClickListener(){
            override fun onSingleClick(view: View) {
                attemptPost()
            }
        })

        writing_button_imageupload.setOnClickListener {
            TedImagePicker.with(this)
                .max(5-_uriList.size, R.string.ted_image_picker_max_count)
                .startMultiImage { uriList -> showMultiImage(uriList) }
        }

        writing_button_menu.setOnClickListener (object : OnSingleClickListener(){
            override fun onSingleClick(view: View) {

                val dialogView = layoutInflater.inflate(R.layout.activity_board_menu, null)
                val Btnfree = dialogView.findViewById<Button>(R.id.boardmenu_button_free)
                val Btnqa = dialogView.findViewById<Button>(R.id.boardmenu_button_qa)
                val Btntips = dialogView.findViewById<Button>(R.id.boardmenu_button_tips)
                val Btnstudy = dialogView.findViewById<Button>(R.id.boardmenu_button_study)

                builder.setView(dialogView)
                builder.show()

                Btnfree.setOnClickListener {
                    App.prefs.myCode = App.prefs.codeFree
                    writing_textview_writing.text = "자유게시판"
                    builder.dismiss()
                }
                Btnqa.setOnClickListener {
                    App.prefs.myCode = App.prefs.codeQA
                    writing_textview_writing.text = "Q&A"
                    builder.dismiss()
                }
                Btntips.setOnClickListener {
                    App.prefs.myCode = App.prefs.codeTips
                    writing_textview_writing.text = "Tips"
                    builder.dismiss()
                }
                Btnstudy.setOnClickListener {
                    App.prefs.myCode = App.prefs.codeStudy
                    writing_textview_writing.text = "스터디/모임"
                    builder.dismiss()
                }
            }
        })

    }

    private fun showMultiImage(uriList: List<Uri>) {
        //TODO
        for (element in uriList)
        {
            _uriList.add(element)
        }

        var mAdapter = WritingAdapter(this, _uriList)
        writing_recyclerview_image.adapter = mAdapter
        val lm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        writing_recyclerview_image.layoutManager = lm
        writing_recyclerview_image.setHasFixedSize(true)

        mAdapter.notifyDataSetChanged()
        //uploadImage()
    }

    override fun onResume() {
        super.onResume()

        when(App.prefs.myCode){
            App.prefs.codeFree -> writing_textview_writing.text = "자유게시판"
            App.prefs.codeQA -> writing_textview_writing.text = "Q&A"
            App.prefs.codeTips -> writing_textview_writing.text = "Tips"
            App.prefs.codeStudy -> writing_textview_writing.text = "스터디/모임"
        }
    }

    private fun attemptPost() {
        writing_edittext_title.error = null
        writing_edittext_content.error = null
        val title: String = writing_edittext_title.text.toString()
        val content: String = writing_edittext_content.text.toString()
        var cancel = false
        var focusView: View? = null
        // 제목 유효성 검사
        if (title.isEmpty()) {
            writing_edittext_title.error = "제목을 입력하세요."
            focusView = writing_edittext_title
            cancel = true
        }
        // 본문 유효성 검사
        if (content.isEmpty()) {
            writing_edittext_content.error = "내용을 입력하세요."
            focusView = writing_edittext_content
            cancel = true
        }
        // 게시판 유효성 검사
        if (App.prefs.myCode == ""){
            Toast.makeText(this,"게시판을 선택하세요.",Toast.LENGTH_SHORT).show()
            cancel = true
        }
        if (cancel) {
            focusView?.requestFocus()
        } else {
            startPost(PostingDTO(App.prefs.myEmail,App.prefs.myId,App.prefs.myName,title,content,"header",App.prefs.myCode))
        }
    }

    private fun startPost(data: PostingDTO) {
        service?.userPost("Bearer "+App.prefs.myJwt,data)?.enqueue(object : Callback<PostingResponseDTO?> {
            override fun onResponse(
                call: Call<PostingResponseDTO?>?,
                response: Response<PostingResponseDTO?>

            ) {
                if(response.isSuccessful)
                {
                    val result: PostingResponseDTO = response.body()!!
                    board_id = result._id
                    if(_uriList.isNotEmpty())
                    {
                        uploadImage(result._id) // 이미지를 올렸다면 이미지 post를 호출한다.
                    }
                    else {
                        val intent = Intent(applicationContext, DetailActivity::class.java)
                        intent.putExtra("board_id",board_id)
                        startActivity(intent)
                        finish()
                    }

                }else {
                    // 실패시 resopnse.errorbody를 객체화
                    val gson = Gson()
                    val adapter: TypeAdapter<PostingResponseDTO> = gson.getAdapter<PostingResponseDTO>(
                        PostingResponseDTO::class.java
                    )
                    try {
                        if (response.errorBody() != null) {
                            showProgress(false)
                            val result : PostingResponseDTO = adapter.fromJson(response.errorBody()!!.string())
                            System.out.println(result.message)

                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

            }

            override fun onFailure(call: Call<PostingResponseDTO?>?, t: Throwable) {
                Log.e("error", t.message!!)
            }
        })
    }

    private fun showDiaglog(){
        val builder = AlertDialog.Builder(this)

        val dialogView = layoutInflater.inflate(R.layout.activity_board_menu, null)

        val Btnfree = dialogView.findViewById<Button>(R.id.boardmenu_button_free)
        val Btnqa = dialogView.findViewById<Button>(R.id.boardmenu_button_qa)
        val Btntips = dialogView.findViewById<Button>(R.id.boardmenu_button_tips)
        val Btnstudy = dialogView.findViewById<Button>(R.id.boardmenu_button_study)
    }

    private fun showProgress(show: Boolean) {
        writing_progressbar.visibility = (if (show) View.VISIBLE else View.GONE)
    }

    private fun uploadImage(board_id: String){
//        var file = File(uriList22[0].path)
//        var requestBody : RequestBody = RequestBody.create(MediaType.parse("image/*"),file)
//        var body : MultipartBody.Part = MultipartBody.Part.createFormData("files",file.name,requestBody)
//

        var images = ArrayList<MultipartBody.Part>()
        for (index in 0 until _uriList.size) {
            val file = File(_uriList[index].path)
            val surveyBody = RequestBody.create(MediaType.parse("image/*"), file)
            images.add(MultipartBody.Part.createFormData("files",file.name,surveyBody))
        }

        val ref = RequestBody.create(MediaType.parse("text/plain"),"board")
        val refId = RequestBody.create(MediaType.parse("text/plain"),board_id)
        val field = RequestBody.create(MediaType.parse("text/plain"),"image")


        service?.uploadFile(images,ref,refId,field)?.enqueue(object : Callback<UploadImageResponseDTO?> {
            override fun onResponse(
                call: Call<UploadImageResponseDTO?>,
                response: Response<UploadImageResponseDTO?>
            ) {
                var result : UploadImageResponseDTO = response.body()!!
                for (index in 0 until result.size){
                    System.out.println("test"+result[index]._id)
                }
                val intent = Intent(applicationContext, DetailActivity::class.java)
                intent.putExtra("board_id",board_id)
                startActivity(intent)
                finish()
            }

            override fun onFailure(call: Call<UploadImageResponseDTO?>, t: Throwable) {
                Log.d("onFailure",t.message)
            }

        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val focusView = currentFocus

        if (focusView != null) {
            val rect = Rect()
            focusView.getGlobalVisibleRect(rect)
            val x = ev!!.x.toInt()
            val y = ev.y.toInt()
            if (!rect.contains(x, y)) {
                val imm: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (imm != null) imm.hideSoftInputFromWindow(focusView.windowToken, 0)
                focusView.clearFocus()
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}