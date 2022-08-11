package jp.juggler.subwaytooter

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import com.jrummyapps.android.colorpicker.ColorPickerDialog
import com.jrummyapps.android.colorpicker.ColorPickerDialogListener
import jp.juggler.subwaytooter.api.entity.Acct
import jp.juggler.subwaytooter.databinding.ActNicknameBinding
import jp.juggler.subwaytooter.table.AcctColor
import jp.juggler.util.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.textColor

class ActNickname : AppCompatActivity(), View.OnClickListener, ColorPickerDialogListener {

    companion object {
        private val log = LogCategory("ActNickname")

        internal const val EXTRA_ACCT_ASCII = "acctAscii"
        internal const val EXTRA_ACCT_PRETTY = "acctPretty"
        internal const val EXTRA_SHOW_NOTIFICATION_SOUND = "show_notification_sound"

        fun createIntent(
            activity: Activity,
            fullAcct: Acct,
            bShowNotificationSound: Boolean,
        ) = Intent(activity, ActNickname::class.java).apply {
            putExtra(EXTRA_ACCT_ASCII, fullAcct.ascii)
            putExtra(EXTRA_ACCT_PRETTY, fullAcct.pretty)
            putExtra(EXTRA_SHOW_NOTIFICATION_SOUND, bShowNotificationSound)
        }
    }

    private val views by lazy {
        ActNicknameBinding.inflate(layoutInflater)
    }

    private var showNotificationSound = false
    private lateinit var acctAscii: String
    private lateinit var acctPretty: String
    private var colorFg = 0
    private var colorBg = 0
    private var notificationSoundUri: String? = null
    private var loadingBusy = false

    private val arNotificationSound = ActivityResultHandler(log) { r ->
        r.decodeRingtonePickerResult()?.let { uri ->
            notificationSoundUri = uri.toString()
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arNotificationSound.register(this)
        App1.setActivityTheme(this)

        val intent = intent
        this.acctAscii = intent.getStringExtra(EXTRA_ACCT_ASCII)!!
        this.acctPretty = intent.getStringExtra(EXTRA_ACCT_PRETTY)!!
        this.showNotificationSound = intent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION_SOUND, false)

        initUI()

        load()
    }

    private fun initUI() {

        title = getString(
            when {
                showNotificationSound -> R.string.nickname_and_color_and_notification_sound
                else -> R.string.nickname_and_color
            }
        )
        setContentView(views.root)
        App1.initEdgeToEdge(this)

        Styler.fixHorizontalPadding(findViewById(R.id.llContent))

        views.btnTextColorEdit.setOnClickListener(this)
        views.btnTextColorReset.setOnClickListener(this)
        views.btnBackgroundColorEdit.setOnClickListener(this)
        views.btnBackgroundColorReset.setOnClickListener(this)
        views.btnSave.setOnClickListener(this)
        views.btnDiscard.setOnClickListener(this)

        views.btnNotificationSoundEdit.setOnClickListener(this)
        views.btnNotificationSoundReset.setOnClickListener(this)

        views.btnNotificationSoundEdit.isEnabledAlpha = false
        views.btnNotificationSoundReset.isEnabledAlpha = false

        views.etNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int,
            ) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable) {
                show()
            }
        })
    }

    private fun load() {
        loadingBusy = true

        findViewById<View>(R.id.llNotificationSound).visibility =
            if (showNotificationSound) View.VISIBLE else View.GONE

        views.tvAcct.text = acctPretty

        val ac = AcctColor.load(acctAscii, acctPretty)
        colorBg = ac.color_bg
        colorFg = ac.color_fg
        views.etNickname.setText(ac.nickname)
        notificationSoundUri = ac.notification_sound

        loadingBusy = false
        show()
    }

    private fun save() {
        if (loadingBusy) return
        AcctColor(
            acctAscii,
            acctPretty,
            views.etNickname.text.toString().trim { it <= ' ' },
            colorFg,
            colorBg,
            notificationSoundUri
        ).save(System.currentTimeMillis())
    }

    private fun show() {
        val s = views.etNickname.text.toString().trim { it <= ' ' }
        views.tvPreview.text = s.notEmpty() ?: acctPretty
        views.tvPreview.textColor = colorFg.notZero() ?: attrColor(R.attr.colorTimeSmall)
        views.tvPreview.backgroundColor = colorBg
    }

    override fun onClick(v: View) {
        val builder: ColorPickerDialog.Builder
        when (v.id) {
            R.id.btnTextColorEdit -> {
                views.etNickname.hideKeyboard()
                builder = ColorPickerDialog.newBuilder()
                    .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                    .setAllowPresets(true)
                    .setShowAlphaSlider(false)
                    .setDialogId(1)
                if (colorFg != 0) builder.setColor(colorFg)
                builder.show(this)
            }

            R.id.btnTextColorReset -> {
                colorFg = 0
                show()
            }

            R.id.btnBackgroundColorEdit -> {
                views.etNickname.hideKeyboard()
                builder = ColorPickerDialog.newBuilder()
                    .setDialogType(ColorPickerDialog.TYPE_CUSTOM)
                    .setAllowPresets(true)
                    .setShowAlphaSlider(false)
                    .setDialogId(2)
                if (colorBg != 0) builder.setColor(colorBg)
                builder.show(this)
            }

            R.id.btnBackgroundColorReset -> {
                colorBg = 0
                show()
            }

            R.id.btnSave -> {
                save()
                setResult(Activity.RESULT_OK)
                finish()
            }

            R.id.btnDiscard -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            R.id.btnNotificationSoundEdit -> openNotificationSoundPicker()

            R.id.btnNotificationSoundReset -> notificationSoundUri = ""
        }
    }

    override fun onColorSelected(dialogId: Int, @ColorInt newColor: Int) {
        when (dialogId) {
            1 -> colorFg = -0x1000000 or newColor
            2 -> colorBg = -0x1000000 or newColor
        }
        show()
    }

    override fun onDialogDismissed(dialogId: Int) {}

    private fun openNotificationSoundPicker() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.notification_sound)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)
        notificationSoundUri.mayUri()?.let {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
        }
        val chooser = Intent.createChooser(intent, getString(R.string.notification_sound))
        arNotificationSound.launch(chooser)
    }
}
