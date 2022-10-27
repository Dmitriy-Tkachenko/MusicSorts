package com.tkachenko.sortmusic.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.tkachenko.sortmusic.ItemTouchHelperAdapter
import com.tkachenko.sortmusic.R
import com.tkachenko.sortmusic.models.Music
import com.tkachenko.sortmusic.utils.Utils
import java.util.*


private const val TAG = "MusicAdapter"

class MusicAdapter: RecyclerView.Adapter<MusicAdapter.MusicHolder>(), ItemTouchHelperAdapter {
    interface Callback {
        fun onClickPlay(uri: Uri, title: String, artist: String, position: Int)
        fun onClickBtnContinue()
        fun onClickBtnPause()
        fun deleteMusic(isPlay: Boolean)
        fun progressChanged(progress: Int)
    }

    private var music: MutableList<Music> = mutableListOf()
    private var playingPosition: Int? = null
    private lateinit var callback: Callback

    fun attachCallback(callback: Callback) {
        this.callback = callback
    }

    fun startPlay(position: Int) {
        if (position < music.size) {
            music[position].isPlay = true
            notifyItemChanged(position)
        }
    }

    fun stopPlay(position: Int) {
        music[position].isPlay = false
        music[position].isPause = false
        notifyItemChanged(position)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(newMusic: List<Music>) {
        music.clear()
        music.addAll(newMusic)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun insertData(newMusic: List<Music>) {
        val musicSet: List<Music> = newMusic.filter {
            !music.contains(it)
        }
        for (item in musicSet) {
            Log.d(TAG, item.title)
        }
        music.addAll(musicSet)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeData() {
        music.clear()
        notifyDataSetChanged()
    }

    fun getData(): List<Music> = music

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent,false)
        return MusicHolder(view = view)
    }

    override fun onBindViewHolder(holder: MusicHolder, position: Int) {
        val music = music[position]
        holder.bind(music)
    }

    override fun getItemCount(): Int = music.size

    inner class MusicHolder(view: View): RecyclerView.ViewHolder(view) {
        private val image: ShapeableImageView = view.findViewById(R.id.img)
        private val title: TextView = view.findViewById(R.id.title)
        private val artist: TextView = view.findViewById(R.id.artist)
        private val duration: TextView = view.findViewById(R.id.duration)
        private val currentDuration: TextView = view.findViewById(R.id.current_duration)
        private val btnPlay: ImageView = view.findViewById(R.id.btn_play)
        private val btnPause: ImageView = view.findViewById(R.id.btn_pause)
        private val parent: LinearLayout = view.findViewById(R.id.parent)
        private val seekBar: SeekBar = view.findViewById(R.id.seek_bar)

        init {
            parent.apply {
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
            }
        }

        fun bind(model: Music) {
            image.setImageBitmap(model.image)
            title.text = model.title
            artist.text = model.artist
            duration.text = model.duration
            title.isSelected = true
            artist.isSelected = true

            if (model.isPlay && !model.isPause) {
                btnPlay.visibility = View.GONE
                btnPause.visibility = View.VISIBLE
                seekBar.visibility = View.VISIBLE
                currentDuration.visibility = View.VISIBLE
            } else if (model.isPause) {
                btnPlay.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
            } else {
                btnPlay.visibility = View.VISIBLE
                btnPause.visibility = View.GONE
                seekBar.visibility = View.INVISIBLE
                currentDuration.visibility = View.GONE
            }

            btnPlay.setOnClickListener {
                if (model.isPause) {
                    callback.onClickBtnContinue()
                    model.isPause = false
                    notifyItemChanged(adapterPosition)
                }
                else {
                    callback.onClickPlay(model.uri, model.title, model.artist, adapterPosition)
                    playingPosition?.let {
                        music[playingPosition!!].isPlay = false
                        music[playingPosition!!].isPause = false
                        seekBar.progress = 0
                        notifyItemChanged(playingPosition!!)
                    }
                    music[adapterPosition].isPlay = true
                    playingPosition = adapterPosition
                    notifyItemChanged(adapterPosition)
                }
            }

            btnPause.setOnClickListener {
                callback.onClickBtnPause()
                music[adapterPosition].isPause = true
                notifyItemChanged(adapterPosition)
            }

            seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekbar: SeekBar?, progress: Int, p2: Boolean) {
                    if (p2) {
                        callback.progressChanged(progress)
                    }
                }

                override fun onStartTrackingTouch(seekbar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekbar: SeekBar?) {

                }
            })
        }

        fun setProgressIndicatorMax(max: Int) {
            seekBar.max = max
        }

        @SuppressLint("SetTextI18n")
        fun updateProgressIndicator(progress: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                seekBar.setProgress(progress, true)
            } else seekBar.progress = progress
            currentDuration.text = "${Utils.getTimeStringFromInt(progress)} / "
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(music, i, i + 1)
                if (music[i + 1].isPlay) {
                    playingPosition = i + 1
                } else if (music[i].isPlay) {
                    playingPosition = i
                }
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(music, i, i - 1)
                if (music[i - 1].isPlay) {
                    playingPosition = i - 1
                } else if (music[i].isPlay) {
                    playingPosition = i
                }
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onItemDismiss(position: Int) {
        val isPlay = music[position].isPlay
        music.removeAt(position)
        callback.deleteMusic(isPlay)
        playingPosition = null
        notifyItemRemoved(position)
    }
}