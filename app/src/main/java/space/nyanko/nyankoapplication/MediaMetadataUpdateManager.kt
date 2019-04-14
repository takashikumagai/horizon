package space.nyanko.nyankoapplication

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.io.File
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object MediaMetadataUpdateManager {

    private const val TAG = "MetadataUpdateManager"

    const val TASK_COMPLETE = 2500
    const val TASK_INVALID_STATE = 2501

    private val taskQueue: Queue<MediaMetadataUpdateTask>
            = LinkedBlockingQueue<MediaMetadataUpdateTask>()

    private val queue: BlockingQueue<Runnable>
            = LinkedBlockingQueue<Runnable>()

    private val threadPoolExecutor: ThreadPoolExecutor = ThreadPoolExecutor(
            8, //CORE_POOL_SIZE,
            8, // MAXIMUM_POOL_SIZE,
            1, // KEEP_ALIVE_TIME,
            TimeUnit.SECONDS,// KEEP_ALIVE_TIME_UNIT,
            queue// mDownloadWorkQueue);
    )

    init {
        Log.d(TAG,"init t" + Thread.currentThread().id)
    }

    /**
     * This must run on the UI thread
     *
     *
     */
    private val handler: Handler = object: Handler(Looper.getMainLooper()) {

        override fun handleMessage(inputMessage: Message) {
            Log.d(TAG,"hM t" + Thread.currentThread().id)
            // Gets the task from the incoming message object
            val task = inputMessage.obj as MediaMetadataUpdateTask

            if(inputMessage.what == TASK_COMPLETE) {
                Log.d(TAG,"hM TASK_COMPLETE")

                val entry = task.entry
                val holder = task.holder
                if(entry != null && holder != null) {
                    task.updateMediaMetadata(entry,holder)
                }

                recycleTask(task)
            }
        }
    }

    fun handleState(task: MediaMetadataUpdateTask, state: Int) {
        Log.d(TAG,"hS t" + Thread.currentThread().id + " / state: " + state)

        if(state == TASK_COMPLETE) {
            Log.d(TAG,"hS " + state)
            // Gets a Message object, stores the state in it, and sends it to the Handler
            val completeMessage = handler.obtainMessage(state, task)
            completeMessage.sendToTarget()
        }
    }

    fun updateMediaMetadataDeferred(entry: File, holder: RecyclerViewAdapter.ViewHolder) {
        Log.d(TAG,"uMMD t" + Thread.currentThread().id + " " + entry.path)

        var task: MediaMetadataUpdateTask? = taskQueue.poll()

        if(task == null) {
            Log.d(TAG,"uMMD t" + Thread.currentThread().id + " !task")
            task = MediaMetadataUpdateTask()
        }

        task.initializeTask(entry, holder)

        threadPoolExecutor.execute(task.runnable)
    }

    fun recycleTask(task: MediaMetadataUpdateTask) {
        task.clear()
        taskQueue.offer(task)
    }
}