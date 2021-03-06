package com.example.howareyou.views.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.howareyou.App
import com.example.howareyou.R
import com.example.howareyou.util.ConvertTime
import com.example.howareyou.model.LoadPostItem
import com.example.howareyou.databinding.ItemHomePostingBinding
import com.example.howareyou.model.Code
import com.example.howareyou.views.detail.DetailActivity
import kotlinx.android.synthetic.main.item_home_posting.view.*
import java.lang.Exception
import kotlin.collections.ArrayList

class HomeAdapter(val context: Context) : RecyclerView.Adapter<CustomViewHolder>(){

    var postingDTO = ArrayList<LoadPostItem>()

    //클릭 인터페이스 정의
    interface ItemClickListener {
        fun onClick(view: View, position: Int, postArray: ArrayList<LoadPostItem>)
    }

    //클릭리스너 선언
    private lateinit var itemClickListner: ItemClickListener

    //클릭리스너 등록 매소드
    fun setItemClickListener(itemClickListener: ItemClickListener) {
        this.itemClickListner = itemClickListener
    }

    fun setItem(data: ArrayList<LoadPostItem>){
        this.postingDTO.clear()
        this.postingDTO.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {

        val binding = ItemHomePostingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return postingDTO.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {


        holder.onBind(postingDTO[position])

        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("board_id",postingDTO[position].id)
            context.startActivity(intent)
        }

        // 시간 convert
        val tempText: String
        val convertTime = ConvertTime()
        try {
            postingDTO[position].createdAt = convertTime.showTime(postingDTO[position].createdAt)

        } catch (e : Exception){

        }

        //get Code
        when(postingDTO[position].code?.id){
            App.prefs.codeFree -> postingDTO[position].code = Code("자유게시판")
            App.prefs.codeQA -> postingDTO[position].code = Code("Q&A")
            App.prefs.codeTips -> postingDTO[position].code = Code("Tips")
            App.prefs.codeStudy -> postingDTO[position].code = Code("스터디모집")
            App.prefs.codeCourse -> postingDTO[position].code = Code("진로게시판")
        }

        holder.itemView.homeposting_button_favorite.setBackgroundResource(R.drawable.ic_thumbsup_white)


//         좋아요 체크
        for ( i in  1..postingDTO[position].likeds!!.size)
        {
            if( postingDTO[position].likeds!![i-1].user_id == App.prefs.myId) holder.itemView.homeposting_button_favorite.setBackgroundResource(
                R.drawable.ic_thumbsup
            )
        }



    }

}

class CustomViewHolder(val binding : ItemHomePostingBinding) : RecyclerView.ViewHolder(binding.root) {
    fun onBind(data : LoadPostItem?){
        binding.postItem = data
    }
}