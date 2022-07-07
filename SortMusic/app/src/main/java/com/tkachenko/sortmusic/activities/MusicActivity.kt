package com.tkachenko.sortmusic.activities

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.DocumentsContract
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tkachenko.sortmusic.R
import com.tkachenko.sortmusic.SimpleItemTouchHelperCallback
import com.tkachenko.sortmusic.adapters.MusicAdapter
import com.tkachenko.sortmusic.models.Music
import com.tkachenko.sortmusic.player.MusicFileData
import com.tkachenko.sortmusic.services.MusicPlayerService
import java.io.*


private const val TAG = "MusicActivity"

class MusicActivity: AppCompatActivity(), View.OnClickListener {
    private lateinit var btnAdd: ImageView
    private lateinit var btnClear: ImageView
    private lateinit var btnCheckMark: ImageView
    private lateinit var tvMyMusic: TextView
    private lateinit var mAdapter: MusicAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var textWelcome: TextView
    private var isPlaying = false
    private var serviceIntentPlayer: Intent? = null
    private var holder: MusicAdapter.MusicHolder? = null
    private var musicService: MusicPlayerService? = null
    private var isBound: Boolean? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
            Log.d(TAG, "onServiceConnected")
            musicService = (iBinder as MusicPlayerService.MyBinder).service
            isBound = true
            updateProgressIndicator()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "onServiceDisconnected")
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        serviceIntentPlayer = Intent(this, MusicPlayerService::class.java)

        mAdapter = MusicAdapter()
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = mAdapter
        recyclerView.itemAnimator = null

        textWelcome = findViewById(R.id.text_welcome)
        tvMyMusic = findViewById(R.id.my_music)
        btnAdd = findViewById(R.id.btn_add)
        btnClear = findViewById(R.id.btn_clear)
        btnCheckMark = findViewById(R.id.btn_check_mark)

        btnAdd.setOnClickListener(this)
        btnClear.setOnClickListener(this)
        btnCheckMark.setOnClickListener(this)

        val callback: ItemTouchHelper.Callback = SimpleItemTouchHelperCallback(mAdapter)
        val touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recyclerView)

        mAdapter.attachCallback(object: MusicAdapter.Callback {
            override fun onClickPlay(uri: Uri, title: String, artist: String, position: Int) {
                if (isPlaying) stopMusicService()
                isPlaying = true
                holder = recyclerView.findViewHolderForAdapterPosition(position) as MusicAdapter.MusicHolder
                startMusicService(uri, title, artist)
                mAdapter.startPlay(position)
                musicService?.attachCallback(object : MusicPlayerService.Callback {
                    override fun onCompletion() {
                        isPlaying = false
                        stopMusicService()
                        mAdapter.stopPlay(position)
                    }
                })
            }

            override fun onClickBtnContinue() {
                isPlaying = true
                continueMusicService()
            }

            override fun onClickBtnPause() {
                pauseMusicService()
            }

            override fun deleteMusic(isPlay: Boolean) {
                if (isPlay) {
                    isPlaying = false
                    stopMusicService()
                }
                if (mAdapter.itemCount == 0) {
                    textWelcome.visibility = View.VISIBLE
                    tvMyMusic.visibility = View.GONE
                    btnClear.visibility = View.GONE
                    btnCheckMark.visibility = View.GONE
                }
            }

            override fun progressChanged(progress: Int) {
                rewindMusicService(progress)
                Log.d(TAG, progress.toString())
            }
        })
    }

    private fun updateProgressIndicator() {
        musicService?.maxProgressLiveData?.observe(this) {
            holder?.setProgressIndicatorMax(it)
        }
        musicService?.progressLiveData?.observe(this) {
            holder?.updateProgressIndicator(it)
        }
    }

    override fun onStart() {
        super.onStart()
        bindAudioService()
    }

    override fun onStop() {
        super.onStop()
        unbindAudioService()
    }

    private fun bindAudioService() {
        bindService(serviceIntentPlayer, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindAudioService() {
        unbindService(serviceConnection)
    }

    private fun startMusicService(uri: Uri, title: String, artist: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceIntentPlayer?.action = MusicPlayerService.START_PLAY
            serviceIntentPlayer?.putExtra(MusicPlayerService.URI, uri.toString())
            serviceIntentPlayer?.putExtra(MusicPlayerService.TITLE, title)
            serviceIntentPlayer?.putExtra(MusicPlayerService.ARTIST, artist)
            startForegroundService(serviceIntentPlayer)
        } else {
            serviceIntentPlayer?.action = MusicPlayerService.START_PLAY
            serviceIntentPlayer?.putExtra(MusicPlayerService.URI, uri.toString())
            serviceIntentPlayer?.putExtra(MusicPlayerService.TITLE, title)
            startService(serviceIntentPlayer)
        }
    }

    private fun stopMusicService() {
        serviceIntentPlayer?.action = MusicPlayerService.STOP_PLAY
        startService(serviceIntentPlayer)
    }

    private fun continueMusicService() {
        serviceIntentPlayer?.action = MusicPlayerService.CONTINUE_PLAY
        startService(serviceIntentPlayer)
    }

    private fun pauseMusicService() {
        serviceIntentPlayer?.action = MusicPlayerService.PAUSE_PLAY
        startService(serviceIntentPlayer)
    }

    private fun rewindMusicService(progress: Int) {
        serviceIntentPlayer?.action = MusicPlayerService.REWIND_PLAY
        serviceIntentPlayer?.putExtra(MusicPlayerService.PROGRESS, progress)
        startService(serviceIntentPlayer)
    }

    override fun onClick(view: View?) {
        when(view?.id) {
            R.id.btn_add -> {
                openSaf()
            }
            R.id.btn_clear -> {
                onClickBtnClear()
            }
            R.id.btn_check_mark -> {
                onClickBtnCheckMark()
            }
        }
    }

    private fun openSaf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "audio/*"
        resultLauncher.launch(intent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val musicFileData = MusicFileData(this)
            val data: Intent? = result.data
            val music: MutableList<Music> = mutableListOf()
            if (data?.clipData != null) {
                val count = data.clipData?.itemCount ?: 0
                for (i in 0 until count) {
                    val uri: Uri = data.clipData?.getItemAt(i)?.uri!!
                    val path = uri.path
                    if (path != null) {
                        val image = musicFileData.getImage(uri)
                        val title = musicFileData.getTitle(uri)
                        val name = musicFileData.getName(uri)
                        val artist = musicFileData.getArtist(uri)
                        val duration = musicFileData.getDuration(uri)
                        val extension = musicFileData.getExtension(uri)
                        Log.d(TAG, path)
                        music.add(Music(image = image, title = title, name = name, artist = artist, duration = duration, uri = uri, extension = extension))
                    }
                    textWelcome.visibility = View.GONE
                    tvMyMusic.visibility = View.VISIBLE
                }
            } else if (data?.data != null) {
                val uri = data.data
                val path = uri?.path
                if (path != null) {
                    val image = musicFileData.getImage(uri)
                    val title = musicFileData.getTitle(uri)
                    val name = musicFileData.getName(uri)
                    val artist = musicFileData.getArtist(uri)
                    val duration = musicFileData.getDuration(uri)
                    val extension = musicFileData.getExtension(uri)
                    music.add(Music(image = image, title = title, name = name, artist = artist, duration = duration, uri = uri, extension = extension))
                    textWelcome.visibility = View.GONE
                    tvMyMusic.visibility = View.VISIBLE
                }
            }
            if (mAdapter.itemCount == 0) mAdapter.setData(music)
            else mAdapter.insertData(music)

            btnClear.visibility = View.VISIBLE
            btnCheckMark.visibility = View.VISIBLE
        }
    }

    private fun onClickBtnClear() {
        mAdapter.removeData()
        textWelcome.visibility = View.VISIBLE
        tvMyMusic.visibility = View.GONE
        btnClear.visibility = View.GONE
        btnCheckMark.visibility = View.GONE
        if (isPlaying) {
            isPlaying = false
            stopMusicService()
        }
    }

    private fun onClickBtnCheckMark() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher2.launch(intent)
    }

    private var resultLauncher2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val folderUri = result.data?.data
            if (folderUri != null) {
                val music = mAdapter.getData()
                for (i in music.indices) {
                    Thread {
                        copyFile(music[i].uri, folderUri,"${i}_${music[i].name}.${music[i].extension}")
                    }.start()
                }
                Toast.makeText(this, "Файлы сохранены", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyFile(src: Uri, dest: Uri, destFileName: String) {
        val docId = DocumentsContract.getTreeDocumentId(dest)
        val dirUri = DocumentsContract.buildDocumentUriUsingTree(dest, docId)
        val destUri = DocumentsContract.createDocument(contentResolver, dirUri, "audio/*", destFileName)

        val ins: InputStream? = contentResolver.openInputStream(src)
        if (ins != null) {
            val out: OutputStream? = contentResolver.openOutputStream(destUri!!, "w")
            val byte: ByteArray = ins.readBytes()
            if (byte.isNotEmpty()) {
                out?.write(byte)
            }
        }
    }
}