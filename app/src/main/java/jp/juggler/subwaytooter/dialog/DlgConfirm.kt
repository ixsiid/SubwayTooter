package jp.juggler.subwaytooter.dialog

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import jp.juggler.subwaytooter.R
import jp.juggler.subwaytooter.databinding.DlgConfirmBinding
import jp.juggler.util.ui.dismissSafe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object DlgConfirm {

//    interface Callback {
//        var isConfirmEnabled: Boolean
//
//        fun onOK()
//    }

//    @SuppressLint("InflateParams")
//    fun open(activity: Activity, message: String, callback: Callback): Dialog {
//
//        if (!callback.isConfirmEnabled) {
//            callback.onOK()
//            return
//        }
//
//        val view = activity.layoutInflater.inflate(R.layout.dlg_confirm, null, false)
//        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
//        val cbSkipNext = view.findViewById<CheckBox>(R.id.cbSkipNext)
//        tvMessage.text = message
//
//        AlertDialog.Builder(activity)
//            .setView(view)
//            .setCancelable(true)
//            .setNegativeButton(R.string.cancel, null)
//            .setPositiveButton(R.string.ok) { _, _ ->
//                if (cbSkipNext.isChecked) {
//                    callback.isConfirmEnabled = false
//                }
//                callback.onOK()
//            }
//            .show()
//    }

//    @SuppressLint("InflateParams")
//    fun openSimple(activity: Activity, message: String, callback: () -> Unit) {
//        val view = activity.layoutInflater.inflate(R.layout.dlg_confirm, null, false)
//        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
//        val cbSkipNext = view.findViewById<CheckBox>(R.id.cbSkipNext)
//        tvMessage.text = message
//        cbSkipNext.visibility = View.GONE
//
//        AlertDialog.Builder(activity)
//            .setView(view)
//            .setCancelable(true)
//            .setNegativeButton(R.string.cancel, null)
//            .setPositiveButton(R.string.ok) { _, _ -> callback() }
//            .show()
//    }

    @SuppressLint("InflateParams")
    suspend inline fun AppCompatActivity.confirm(
        message: String,
        isConfirmEnabled: Boolean,
        setConfirmEnabled: (newConfirmEnabled: Boolean) -> Unit,
    ) {
        if (!isConfirmEnabled) return
        val skipNext = suspendCancellableCoroutine { cont ->
            try {
                val views = DlgConfirmBinding.inflate(layoutInflater)
                views.tvMessage.text = message
                val dialog = AlertDialog.Builder(this)
                    .setView(views.root)
                    .setCancelable(true)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        if (cont.isActive) cont.resume(views.cbSkipNext.isChecked)
                    }
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog cancelled."))
                }
                dialog.show()
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
        if (skipNext) setConfirmEnabled(false)
    }

    suspend fun AppCompatActivity.confirm(@StringRes messageId: Int, vararg args: Any?) =
        confirm(getString(messageId, *args))

    suspend fun AppCompatActivity.confirm(message: CharSequence, title: CharSequence? = null) {
        suspendCancellableCoroutine { cont ->
            try {
                val views = DlgConfirmBinding.inflate(layoutInflater)
                views.tvMessage.text = message
                views.cbSkipNext.visibility = View.GONE

                val dialog = AlertDialog.Builder(this).apply {
                    setView(views.root)
                    setCancelable(true)
                    title?.let { setTitle(it) }
                    setNegativeButton(R.string.cancel, null)
                    setPositiveButton(R.string.ok) { _, _ ->
                        if (cont.isActive) cont.resume(Unit)
                    }
                }.create()
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog closed."))
                }
                dialog.show()
                cont.invokeOnCancellation { dialog.dismissSafe() }
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
    }

    suspend fun AppCompatActivity.okDialog(@StringRes messageId: Int, vararg args: Any?) =
        okDialog(getString(messageId, *args))

    suspend fun AppCompatActivity.okDialog(message: CharSequence, title: CharSequence? = null) {
        suspendCancellableCoroutine { cont ->
            try {
                val views = DlgConfirmBinding.inflate(layoutInflater)

                views.cbSkipNext.visibility = View.GONE

                views.tvMessage.apply {
                    movementMethod = LinkMovementMethod.getInstance()
                    autoLinkMask = Linkify.WEB_URLS
                    text = message
                }

                val dialog = AlertDialog.Builder(this).apply {
                    setView(views.root)
                    setCancelable(true)
                    title?.let { setTitle(it) }
                    setPositiveButton(R.string.ok) { _, _ ->
                        if (cont.isActive) cont.resume(Unit)
                    }
                }.create()
                dialog.setOnDismissListener {
                    if (cont.isActive) cont.resumeWithException(CancellationException("dialog closed."))
                }
                dialog.show()
                cont.invokeOnCancellation { dialog.dismissSafe() }
            } catch (ex: Throwable) {
                cont.resumeWithException(ex)
            }
        }
    }
}
